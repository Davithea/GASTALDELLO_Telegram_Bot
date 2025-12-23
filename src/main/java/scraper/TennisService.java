package richieste;

import model.Match;
import model.Player;
import model.RankingResult;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Tennis Service IBRIDO:
 * - SCRAPING Wikipedia per classifiche (stabile e affidabile)
 * - API RapidAPI per partite live e ricerca giocatori
 */
public class TennisService {

    private final OkHttpClient client;
    private final String rapidApiKey;
    private static final String RAPID_API_HOST = "tennis-live-data.p.rapidapi.com";

    public TennisService(String rapidApiKey) {
        this.client = new OkHttpClient();
        this.rapidApiKey = rapidApiKey;
    }

    // ==================== CLASSIFICHE (SCRAPING WIKIPEDIA) ====================

    /**
     * Scraping classifiche ATP da Wikipedia
     */
    public List<Player> getTopRankings(int limit) {
        return getRankings(limit, "ATP_rankings", "atp");
    }

    /**
     * Scraping classifiche WTA da Wikipedia
     */
    public List<Player> getWTARankings(int limit) {
        return getRankings(limit, "WTA_rankings", "wta");
    }

    /**
     * Scraping classifiche ATP Doppio da Wikipedia
     */
    public List<Player> getATPDoublesRankings(int limit) {
        return getDoublesRankings(limit, "atp");
    }

    /**
     * Scraping classifiche WTA Doppio da Wikipedia
     */
    public List<Player> getWTADoublesRankings(int limit) {
        return getDoublesRankings(limit, "wta");
    }

    private RankingResult getRaceAndRanking(int limit, String wikiPage, String type) {

        List<Player> race = new ArrayList<>();
        List<Player> ranking = new ArrayList<>();

        System.out.println("🌐 Scraping Race + Ranking " + type.toUpperCase());

        try {
            String url = "https://en.wikipedia.org/wiki/" + wikiPage;

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null)
                    return new RankingResult(race, ranking);

                Document doc = Jsoup.parse(response.body().string());
                Elements tables = doc.select("table.wikitable");

                int found = 0;

                for (Element table : tables) {

                    String header = table.select("th").text().toLowerCase();

                    // ranking singolare (Race o Ranking)
                    if (!(header.contains("rank") &&
                            header.contains("player") &&
                            header.contains("points"))) {
                        continue;
                    }

                    found++;

                    List<Player> target =
                            (found == 1) ? race :
                                    (found == 2) ? ranking :
                                            null;

                    if (target == null) break;

                    System.out.println(found == 1
                            ? "🏁 Race " + type.toUpperCase()
                            : "🏆 Ranking " + type.toUpperCase());

                    Elements rows = table.select("tbody tr");

                    for (Element row : rows) {
                        if (target.size() >= limit) break;

                        Elements cells = row.select("td");
                        if (cells.size() < 3) continue;

                        try {
                            int rank = Integer.parseInt(
                                    cells.get(0).text().replaceAll("[^0-9]", "")
                            );

                            String name = cells.get(1).text().trim();
                            if (name.split("\\s+").length < 2) continue;

                            int points = 0;
                            for (int i = 2; i < cells.size(); i++) {
                                String p = cells.get(i).text().replaceAll("[^0-9]", "");
                                if (p.length() >= 3) {
                                    points = Integer.parseInt(p);
                                    break;
                                }
                            }

                            String country = "Unknown";
                            Elements imgs = row.select("img");
                            if (!imgs.isEmpty()) {
                                country = cleanCountryName(imgs.first().attr("alt"));
                            }

                            target.add(new Player(name, country, rank, points, 0));

                        } catch (Exception ignored) {}
                    }

                    if (found == 2) break;
                }
            }

        } catch (Exception e) {
            System.out.println("❌ Errore scraping ranking: " + e.getMessage());
        }

        return new RankingResult(race, ranking);
    }


    /**
     * Metodo generico per classifiche singolare (SCRAPING)
     */
    private List<Player> getRankings(int limit, String wikiPage, String type) {
        List<Player> players = new ArrayList<>();

        System.out.println("🌐 Scraping classifiche " + type.toUpperCase() + " (robusto)");

        try {
            String url = "https://en.wikipedia.org/wiki/" + wikiPage;

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return players;

                Document doc = Jsoup.parse(response.body().string());
                Elements tables = doc.select("table.wikitable");

                for (Element table : tables) {

                    String header = table.select("th").text().toLowerCase();

                    // 🔍 filtro forte: SOLO ranking singolare
                    if (!(header.contains("rank") &&
                            header.contains("player") &&
                            header.contains("points"))) {
                        continue;
                    }

                    Elements rows = table.select("tbody tr");
                    int validRows = 0;

                    for (Element row : rows) {
                        Elements cells = row.select("td");
                        if (cells.size() < 4) continue;

                        try {
                            // Rank
                            String rankText = cells.get(0).text().replaceAll("[^0-9]", "");
                            if (rankText.isEmpty()) continue;
                            int ranking = Integer.parseInt(rankText);

                            // Nome (almeno nome + cognome)
                            String name = cells.get(1).text().trim();
                            if (name.split("\\s+").length < 2) continue;

                            // Points
                            int points = 0;
                            for (int i = 2; i < cells.size(); i++) {
                                String p = cells.get(i).text().replaceAll("[^0-9]", "");
                                if (p.length() >= 3) {
                                    points = Integer.parseInt(p);
                                    break;
                                }
                            }

                            // Country
                            String country = "Unknown";
                            Elements imgs = row.select("img");
                            if (!imgs.isEmpty()) {
                                country = cleanCountryName(imgs.first().attr("alt"));
                            }

                            players.add(new Player(name, country, ranking, points, 0));
                            validRows++;

                            if (players.size() >= limit) break;

                        } catch (Exception ignored) {}
                    }

                    // ✅ se troviamo abbastanza righe valide, è la tabella giusta
                    if (validRows >= 10) {
                        System.out.println("✅ Tabella ranking corretta trovata");
                        break;
                    } else {
                        players.clear(); // era una tabella finta
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("❌ Errore scraping ranking: " + e.getMessage());
        }

        System.out.println("✅ " + players.size() + " giocatori estratti");
        return players;
    }


    /**
     * Metodo generico per classifiche doppio (SCRAPING)
     */
    private List<Player> getDoublesRankings(int limit, String type) {
        List<Player> players = new ArrayList<>();

        System.out.println("🌐 Scraping classifiche DOPPIO " + type.toUpperCase() + "...");

        try {
            String url = "https://en.wikipedia.org/wiki/" + type.toUpperCase() + "_rankings";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return players;
                }

                String html = response.body().string();
                Document doc = Jsoup.parse(html);

                Elements allTables = doc.select("table.wikitable");

                for (Element table : allTables) {
                    String caption = table.select("caption").text().toLowerCase();
                    String headerText = table.select("th").text().toLowerCase();

                    if (caption.contains("doubles") || headerText.contains("doubles")) {
                        Elements rows = table.select("tbody tr");

                        for (Element row : rows) {
                            if (players.size() >= limit) break;

                            Elements cells = row.select("td");
                            if (cells.size() < 2) continue;

                            try {
                                String rankText = cells.get(0).text().replaceAll("[^0-9]", "");
                                if (rankText.isEmpty()) continue;
                                int ranking = Integer.parseInt(rankText);

                                String teamName = "";
                                for (int i = 1; i < Math.min(cells.size(), 5); i++) {
                                    String cellText = cells.get(i).text().trim();
                                    if (cellText.contains("/") ||
                                            (cellText.split("\\s+").length >= 4 && !cellText.matches(".*\\d{3,}.*"))) {
                                        teamName = cellText;
                                        break;
                                    }
                                }

                                if (teamName.isEmpty()) continue;

                                int points = 0;
                                for (int i = 2; i < cells.size(); i++) {
                                    String pointsText = cells.get(i).text().replaceAll("[^0-9]", "");
                                    if (!pointsText.isEmpty() && pointsText.length() >= 3) {
                                        points = Integer.parseInt(pointsText);
                                        break;
                                    }
                                }

                                Elements imgs = row.select("img");
                                String country = "Unknown";

                                if (imgs.size() >= 2) {
                                    String country1 = cleanCountryName(imgs.get(0).attr("alt"));
                                    String country2 = cleanCountryName(imgs.get(1).attr("alt"));
                                    country = country1 + " / " + country2;
                                } else if (imgs.size() == 1) {
                                    country = cleanCountryName(imgs.get(0).attr("alt"));
                                }

                                players.add(new Player(teamName, country, ranking, points, 0));

                            } catch (Exception e) {
                                continue;
                            }
                        }

                        if (!players.isEmpty()) break;
                    }
                }

                System.out.println("✅ " + players.size() + " team estratti");
            }

        } catch (Exception e) {
            System.out.println("❌ Errore: " + e.getMessage());
        }

        return players;
    }

    private String cleanCountryName(String country) {
        if (country == null || country.isEmpty()) return "Unknown";

        country = country.replaceAll("(?i)flag of ", "")
                .replaceAll("(?i)flag icon ", "")
                .replaceAll("(?i)the ", "")
                .trim();

        return country;
    }

    // ==================== PARTITE LIVE & RICERCA (API) ====================

    /**
     * Partite live da RapidAPI Tennis Live Data
     */
    public List<Match> getRecentMatches() {
        List<Match> matches = new ArrayList<>();

        System.out.println("🌐 Recupero partite live da RapidAPI...");

        try {
            // Endpoint per partite live
            String url = "https://tennis-live-data.p.rapidapi.com/matches-by-date/2024-12-22";

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("X-RapidAPI-Key", rapidApiKey)
                    .addHeader("X-RapidAPI-Host", RAPID_API_HOST)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    System.out.println("❌ Errore API: " + response.code());
                    return matches;
                }

                String jsonResponse = response.body().string();
                JSONObject json = new JSONObject(jsonResponse);

                if (json.has("results")) {
                    JSONArray results = json.getJSONArray("results");

                    for (int i = 0; i < results.length(); i++) {
                        JSONObject matchData = results.getJSONObject(i);

                        // Solo partite in corso
                        String status = matchData.optString("status", "");
                        if (!status.equalsIgnoreCase("inprogress")) continue;

                        String tournament = matchData.optString("tournament", "Unknown");
                        String homePlayer = matchData.optJSONObject("home_player") != null
                                ? matchData.getJSONObject("home_player").optString("full_name", "Unknown")
                                : "Unknown";
                        String awayPlayer = matchData.optJSONObject("away_player") != null
                                ? matchData.getJSONObject("away_player").optString("full_name", "Unknown")
                                : "Unknown";

                        String score = matchData.optString("home_score", "0") + " - " +
                                matchData.optString("away_score", "0");

                        int priority = getTournamentPriority(tournament);
                        matches.add(new Match(tournament, homePlayer, awayPlayer, score, "", priority));
                    }

                    System.out.println("✅ " + matches.size() + " partite live trovate");
                } else {
                    System.out.println("ℹ️  Nessuna partita live al momento");
                }
            }

        } catch (Exception e) {
            System.out.println("❌ Errore API: " + e.getMessage());
            e.printStackTrace();
        }

        return matches;
    }

    /**
     * Cerca giocatore (prima nelle classifiche, poi via API)
     */
    public Player searchPlayer(String name) {
        System.out.println("🔍 Cercando: " + name);

        // Prima cerca nelle classifiche ATP/WTA
        List<Player> atpRankings = getTopRankings(100);
        for (Player player : atpRankings) {
            if (player.getNome().toLowerCase().contains(name.toLowerCase())) {
                System.out.println("✅ Trovato in ATP rankings");
                return player;
            }
        }

        List<Player> wtaRankings = getWTARankings(100);
        for (Player player : wtaRankings) {
            if (player.getNome().toLowerCase().contains(name.toLowerCase())) {
                System.out.println("✅ Trovato in WTA rankings");
                return player;
            }
        }

        System.out.println("❌ Giocatore non trovato nelle top 100");
        return null;
    }

    private int getTournamentPriority(String tournament) {
        String lower = tournament.toLowerCase();
        if (lower.contains("australian open") || lower.contains("roland garros") ||
                lower.contains("french open") || lower.contains("wimbledon") ||
                lower.contains("us open")) return 1;
        if (lower.contains("masters") || lower.contains("finals")) return 2;
        if (lower.contains("500")) return 3;
        if (lower.contains("250")) return 4;
        if (lower.contains("challenger")) return 5;
        return 6;
    }
}


//private RankingResult getRaceAndRanking(int limit, String wikiPage, String type) {
//
//        List<Player> race = new ArrayList<>();
//        List<Player> ranking = new ArrayList<>();
//
//        System.out.println("🌐 Scraping Race + Ranking " + type.toUpperCase());
//
//        try {
//            String url = "https://en.wikipedia.org/wiki/" + wikiPage;
//
//            Request request = new Request.Builder()
//                    .url(url)
//                    .addHeader("User-Agent", "Mozilla/5.0")
//                    .build();
//
//            try (Response response = client.newCall(request).execute()) {
//                if (!response.isSuccessful() || response.body() == null)
//                    return new RankingResult(race, ranking);
//
//                Document doc = Jsoup.parse(response.body().string());
//                Elements tables = doc.select("table.wikitable");
//
//                int found = 0;
//
//                for (Element table : tables) {
//
//                    String header = table.select("th").text().toLowerCase();
//
//                    // ranking singolare (Race o Ranking)
//                    if (!(header.contains("rank") &&
//                            header.contains("player") &&
//                            header.contains("points"))) {
//                        continue;
//                    }
//
//                    found++;
//
//                    List<Player> target =
//                            (found == 1) ? race :
//                                    (found == 2) ? ranking :
//                                            null;
//
//                    if (target == null) break;
//
//                    System.out.println(found == 1
//                            ? "🏁 Race " + type.toUpperCase()
//                            : "🏆 Ranking " + type.toUpperCase());
//
//                    Elements rows = table.select("tbody tr");
//
//                    for (Element row : rows) {
//                        if (target.size() >= limit) break;
//
//                        Elements cells = row.select("td");
//                        if (cells.size() < 3) continue;
//
//                        try {
//                            int rank = Integer.parseInt(
//                                    cells.get(0).text().replaceAll("[^0-9]", "")
//                            );
//
//                            String name = cells.get(1).text().trim();
//                            if (name.split("\\s+").length < 2) continue;
//
//                            int points = 0;
//                            for (int i = 2; i < cells.size(); i++) {
//                                String p = cells.get(i).text().replaceAll("[^0-9]", "");
//                                if (p.length() >= 3) {
//                                    points = Integer.parseInt(p);
//                                    break;
//                                }
//                            }
//
//                            String country = "Unknown";
//                            Elements imgs = row.select("img");
//                            if (!imgs.isEmpty()) {
//                                country = cleanCountryName(imgs.first().attr("alt"));
//                            }
//
//                            target.add(new Player(name, country, rank, points, 0));
//
//                        } catch (Exception ignored) {}
//                    }
//
//                    if (found == 2) break;
//                }
//            }
//
//        } catch (Exception e) {
//            System.out.println("❌ Errore scraping ranking: " + e.getMessage());
//        }
//
//        return new RankingResult(race, ranking);
//    }