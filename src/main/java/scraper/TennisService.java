package scraper;

import model.H2HData;
import model.Match;
import model.Player;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.*;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tennis Service - Scraping completo
 * - Wikipedia per classifiche
 * - Wikipedia per ricerca giocatori
 * - SofaScore per partite live (Selenium)
 */
public class TennisService {

    private final OkHttpClient client;

    public TennisService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    // ==================== CLASSIFICHE (SCRAPING WIKIPEDIA) ====================

    public List<Player> getATPRankings(int limit) {
        return getRankings(limit, "ATP_rankings", "atp");
    }

    public List<Player> getWTARankings(int limit) {
        return getRankings(limit, "WTA_rankings", "wta");
    }

    public List<Player> getRaceRankings(int limit) {
        return getRankings(limit, "ATP_rankings", "race");
    }

    public List<Player> getATPDoubleRankings(int limit) {
        return getRankings(limit, "ATP_rankings", "atp_doppio");
    }

    public List<Player> getWTADoubleRankings(int limit) {
        return getRankings(limit, "WTA_rankings", "wta_doppio");
    }

    private List<Player> getRankings(int limit, String wikiPage, String type) {
        List<Player> players = new ArrayList<>();

        System.out.println("🌐 Scraping classifiche " + type.toUpperCase() + " da Wikipedia...");

        try {
            String url = "https://en.wikipedia.org/wiki/" + wikiPage;

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

                int tableIndex = switch (type) {
                    case "race", "wta_doppio" -> 1;
                    case "atp" -> 2;
                    case "atp_doppio" -> 4;
                    default -> 0;
                };

                int currentTableIndex = 0;

                for (Element table : allTables) {
                    Elements headers = table.select("th");
                    String headerText = headers.text().toLowerCase();

                    if (headerText.contains("rank") || headerText.contains("player")) {

                        if ((type.equals("atp") || type.equals("atp_doppio") || type.equals("wta_doppio"))
                                && currentTableIndex < tableIndex) {
                            currentTableIndex++;
                            continue;
                        }

                        System.out.println("✅ Usando tabella: " + table.select("caption").text());

                        Elements rows = table.select("tbody tr");

                        int lastRanking = -1; // ← per gestire "="

                        for (Element row : rows) {
                            if (players.size() >= limit) break;

                            Elements cells = row.select("td");
                            if (cells.size() < 3) continue;

                            try {
                                // ---------------- RANKING ----------------
                                String rawRank = cells.get(0).text().trim();
                                int ranking;

                                if (rawRank.equals("=")) {
                                    if (lastRanking == -1) continue;
                                    ranking = lastRanking;
                                } else {
                                    String rankText = rawRank.replaceAll("[^0-9]", "");
                                    if (rankText.isEmpty()) continue;
                                    ranking = Integer.parseInt(rankText);
                                    lastRanking = ranking;
                                }

                                // ---------------- NOME ----------------
                                String name = "";
                                for (int i = 1; i < Math.min(cells.size(), 4); i++) {
                                    String cellText = cells.get(i).text().trim();
                                    if (cellText.split("\\s+").length >= 2 &&
                                            !cellText.matches("^[0-9,]+$")) {
                                        name = cellText;
                                        break;
                                    }
                                }

                                if (name.isEmpty()) continue;

                                // ---------------- PUNTI ----------------
                                int points = 0;
                                for (int i = 2; i < cells.size(); i++) {
                                    String pointsText = cells.get(i).text().replaceAll("[^0-9]", "");
                                    if (!pointsText.isEmpty() && pointsText.length() >= 3) {
                                        points = Integer.parseInt(pointsText);
                                        break;
                                    }
                                }

                                // ---------------- NAZIONE ----------------
                                String country = "Unknown";
                                for (Element cell : cells) {
                                    Elements imgs = cell.select("img");
                                    if (!imgs.isEmpty()) {
                                        String alt = imgs.first().attr("alt");
                                        if (!alt.isEmpty() && alt.length() < 50) {
                                            country = cleanCountryName(alt);
                                            break;
                                        }
                                    }
                                }

                                players.add(new Player(name, country, ranking, points, 0));

                            } catch (Exception e) {
                                // Salta riga
                            }
                        }

                        if (!players.isEmpty()) break;
                    }
                }

                System.out.println("✅ " + players.size() + " giocatori estratti");
            }

        } catch (Exception e) {
            System.out.println("❌ Errore scraping: " + e.getMessage());
        }

        return players;
    }

    private String cleanCountryName(String country) {
        if (country == null || country.isEmpty()) return "Unknown";
        return country.replaceAll("(?i)flag of ", "")
                .replaceAll("(?i)flag icon ", "")
                .replaceAll("(?i)the ", "")
                .trim();
    }

    // ==================== RICERCA GIOCATORE (SCRAPING WIKIPEDIA) ====================

    public Player searchPlayerAPI(String playerName) {
        System.out.println("🔍 Cercando pagina Wikipedia per: " + playerName);

        try {
            String wikiName = formatWikipediaName(playerName);
            String url = "https://it.wikipedia.org/wiki/" + wikiName;

            System.out.println("📄 URL Wikipedia: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.out.println("❌ Pagina non trovata: " + response.code());
                    System.out.println("🔄 Tentativo ricerca nelle classifiche...");
                    return searchInRankings(playerName);
                }

                String html = response.body().string();
                Document doc = Jsoup.parse(html);

                if (!isTennisPlayer(doc)) {
                    System.out.println("⚠️ Non è un giocatore di tennis");
                    return null;
                }

                Player player = extractPlayerInfo(doc, playerName);

                if (player != null) {
                    System.out.println("✅ Giocatore trovato: " + player.getNome());
                    return player;
                } else {
                    System.out.println("❌ Impossibile estrarre dati giocatore");
                    return null;
                }
            }

        } catch (Exception e) {
            System.out.println("❌ Errore ricerca: " + e.getMessage());
            return searchInRankings(playerName);
        }
    }

    private String formatWikipediaName(String name) {
        String[] words = name.trim().split("\\s+");
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.length() > 0) {
                formatted.append(Character.toUpperCase(word.charAt(0)));
                formatted.append(word.substring(1).toLowerCase());

                if (i < words.length - 1) {
                    formatted.append("_");
                }
            }
        }

        return formatted.toString();
    }

    private boolean isTennisPlayer(Document doc) {
        String pageText = doc.text().toLowerCase();

        boolean hasTennisKeyword = pageText.contains("tennis") ||
                pageText.contains("atp") ||
                pageText.contains("wta") ||
                pageText.contains("grand slam") ||
                pageText.contains("australian open") ||
                pageText.contains("wimbledon") ||
                pageText.contains("us open") ||
                pageText.contains("roland garros");

        Elements infobox = doc.select("table.infobox");
        if (!infobox.isEmpty()) {
            String infoboxText = infobox.text().toLowerCase();
            if (infoboxText.contains("tennis") ||
                    infoboxText.contains("sport") && infoboxText.contains("tennis")) {
                return true;
            }
        }

        return hasTennisKeyword;
    }

    private Player extractPlayerInfo(Document doc, String searchName) {
        try {
            Elements infobox = doc.select("table.infobox");

            if (infobox.isEmpty()) {
                System.out.println("⚠️ Infobox non trovato");
                return null;
            }

            Element table = infobox.first();

            String nome = extractFromTable(table, searchName);
            String nazionalita = extractNationality(table);
            String altezza = cleanText(extractFromTableRow(table, "Altezza"));
            String peso = cleanText(extractFromTableRow(table, "Peso"));
            String datanascita = extractBirthDate(table);
            int ranking = extractRanking(table);
            String migliorRanking = cleanText(extractFromTableRow(table, "Miglior ranking"));
            String vittorie = cleanText(extractFromTableRow(table, "Vittorie/sconfitte"));
            String titoliSingolo = cleanText(extractFromTableRow(table, "Titoli vinti"));
            String grandslamVinti = extractGrandSlams(table);
            String manoPreferita = cleanText(extractFromTableRow(table, "Mano"));
            String allenatore = cleanText(extractFromTableRow(table, "Allenatore"));
            String premiMonetari = cleanText(extractFromTableRow(table, "Montepremi", "Palmares"));

            String imageUrl = extractImageUrl(table);
            int eta = calculateAge(datanascita);

            StringBuilder info = new StringBuilder();
            info.append("🎾 ").append(nome.toUpperCase()).append("\n\n");

            if (imageUrl != null && !imageUrl.isEmpty()) {
                info.append("📷 Foto: ").append(imageUrl).append("\n\n");
            }

            info.append("🌍 Nazionalità: ").append(nazionalita).append("\n");

            if (datanascita != null && !datanascita.isEmpty()) {
                info.append("📅 Data di nascita: ").append(datanascita);
                if (eta > 0) {
                    info.append(" (").append(eta).append(" anni)");
                }
                info.append("\n");
            }

            if (altezza != null && !altezza.isEmpty()) {
                info.append("📏 Altezza: ").append(altezza).append("\n");
            }

            if (peso != null && !peso.isEmpty()) {
                info.append("⚖️ Peso: ").append(peso).append("\n");
            }

            if (manoPreferita != null && !manoPreferita.isEmpty()) {
                info.append("✋ Mano: ").append(manoPreferita).append("\n");
            }

            info.append("\n📊 CARRIERA\n\n");

            if (ranking > 0) {
                info.append("🏆 Ranking attuale: #").append(ranking).append("\n");
            }

            if (migliorRanking != null && !migliorRanking.isEmpty()) {
                info.append("⭐ Miglior ranking: ").append(migliorRanking).append("\n");
            }

            if (vittorie != null && !vittorie.isEmpty()) {
                info.append("📈 Vittorie/Sconfitte: ").append(vittorie).append("\n");
            }

            if (titoliSingolo != null && !titoliSingolo.isEmpty()) {
                info.append("🏅 Titoli: ").append(titoliSingolo).append("\n");
            }

            if (grandslamVinti != null && !grandslamVinti.isEmpty()) {
                info.append("\n🏆 GRAND SLAM\n\n");
                info.append(grandslamVinti).append("\n");
            }

            if (allenatore != null && !allenatore.isEmpty()) {
                info.append("\n👨‍🏫 Allenatore: ").append(allenatore).append("\n");
            }

            if (premiMonetari != null && !premiMonetari.isEmpty()) {
                info.append("💰 Montepremi: ").append(premiMonetari).append("\n");
            }

            Player player = new Player(nome, nazionalita, ranking, 0, eta);
            player.setExtraInfo(info.toString());
            player.setImageUrl(imageUrl);
            player.setTennisPlayer(true);

            player.setAltezza(altezza);
            player.setPeso(peso);
            player.setMigliorRanking(migliorRanking);
            player.setVittorieSconfitte(vittorie);
            player.setTitoli(titoliSingolo);

            return player;

        } catch (Exception e) {
            System.out.println("❌ Errore estrazione dati: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String cleanText(String text) {
        if (text == null) return null;
        text = text.replaceAll("\\[\\d+\\]", "");
        text = text.replaceAll("\\[nota \\d+\\]", "");
        text = text.replaceAll("\\s+", " ");
        return text.trim();
    }

    private String extractBirthDate(Element table) {
        Elements rows = table.select("tr");

        for (Element row : rows) {
            Elements th = row.select("th, td.infobox-label");

            if (th.isEmpty()) continue;

            String header = th.first().text().toLowerCase();

            if ((header.contains("data di nascita") ||
                    (header.contains("nato") && !header.contains("nazionalità")))) {

                Elements td = row.select("td");
                if (!td.isEmpty()) {
                    return cleanText(td.first().text().trim());
                }
            }
        }

        return null;
    }

    private String extractFromTable(Element table, String defaultName) {
        Elements title = table.select("th.infobox-title, caption");
        if (!title.isEmpty()) {
            String name = title.first().text().trim();
            if (!name.isEmpty()) {
                return name;
            }
        }
        return defaultName;
    }

    private String extractNationality(Element table) {
        Elements rows = table.select("tr");

        for (Element row : rows) {
            if (row.text().toLowerCase().contains("nazionalità")) {
                Elements links = row.select("a");
                for (Element link : links) {
                    String text = link.text().trim();
                    if (!text.isEmpty() && !text.equalsIgnoreCase("nazionalità")) {
                        return text;
                    }
                }

                String cellText = row.select("td").text().trim();
                if (!cellText.isEmpty() && !cellText.equalsIgnoreCase("nazionalità")) {
                    return cellText;
                }
            }
        }

        return "Unknown";
    }

    private String extractFromTableRow(Element table, String... keywords) {
        Elements rows = table.select("tr");

        for (Element row : rows) {
            Elements th = row.select("th, td.infobox-label");

            if (th.isEmpty()) continue;

            String header = th.first().text().toLowerCase();

            for (String keyword : keywords) {
                if (header.contains(keyword.toLowerCase())) {
                    Elements td = row.select("td");
                    if (!td.isEmpty()) {
                        return td.first().text().trim();
                    }
                }
            }
        }

        return null;
    }

    private int extractRanking(Element table) {
        String rankingText = extractFromTableRow(table, "Ranking attuale");

        if (rankingText != null) {
            rankingText = cleanText(rankingText);

            Pattern pattern = Pattern.compile("(\\d{1,3})(?:º|°|\\s|\\(|$)");
            Matcher matcher = pattern.matcher(rankingText);

            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group(1));
                } catch (Exception e) {
                    return 0;
                }
            }
        }

        return 0;
    }

    private String extractGrandSlams(Element table) {
        Map<String, StringBuilder> grandSlamResults = new LinkedHashMap<>();
        grandSlamResults.put("Singolare", new StringBuilder());
        grandSlamResults.put("Doppio", new StringBuilder());
        grandSlamResults.put("Doppio Misto", new StringBuilder());

        Elements rows = table.select("tr");
        String currentCategory = null;

        for (Element row : rows) {
            String rowText = row.text().toLowerCase();

            if (rowText.contains("singolare")) {
                currentCategory = "Singolare";
                continue;
            } else if (rowText.contains("doppio misto")) {
                currentCategory = "Doppio Misto";
                continue;
            } else if (rowText.contains("doppio")) {
                currentCategory = "Doppio";
                continue;
            }

            if (currentCategory == null) continue;

            Elements cells = row.select("th, td");
            if (cells.size() < 2) continue;

            String tournament = cells.get(0).text();
            String result = cleanText(cells.get(1).text());
            StringBuilder sb = grandSlamResults.get(currentCategory);

            if (tournament.contains("Australian")) {
                sb.append("🇦🇺 Australian Open: ").append(result).append("\n");
            } else if (tournament.contains("Roland") || tournament.contains("France")) {
                sb.append("🇫🇷 Roland Garros: ").append(result).append("\n");
            } else if (tournament.contains("Wimbledon")) {
                sb.append("🇬🇧 Wimbledon: ").append(result).append("\n");
            } else if (tournament.contains("US") || tournament.contains("U.S.")) {
                sb.append("🇺🇸 US Open: ").append(result).append("\n");
            }
        }

        StringBuilder finalResult = new StringBuilder();
        for (Map.Entry<String, StringBuilder> entry : grandSlamResults.entrySet()) {
            if (entry.getValue().length() > 0) {
                finalResult.append(entry.getKey()).append(":\n");
                finalResult.append(entry.getValue()).append("\n");
            }
        }

        return finalResult.length() > 0 ? finalResult.toString().trim() : null;
    }


    // Metodo helper già esistente - lo riutilizziamo
    private String extractImageUrl(Element table) {
        Elements images = table.select("img");

        for (Element img : images) {
            String src = img.attr("src");

            // Ignora bandiere, icone e loghi
            if (src.contains("Flag_of") || src.contains("icon") ||
                    src.contains("logo") || src.length() < 20) {
                continue;
            }

            // Costruisci URL completo
            if (src.startsWith("//")) {
                return "https:" + src;
            } else if (src.startsWith("/")) {
                return "https://it.wikipedia.org" + src;
            } else if (src.startsWith("http")) {
                return src;
            }
        }

        return null;
    }

    private int calculateAge(String birthDate) {
        if (birthDate == null || birthDate.isEmpty()) return 0;

        try {
            Pattern pattern = Pattern.compile("(\\d{1,2})\\s+\\w+\\s+(\\d{4})");
            Matcher matcher = pattern.matcher(birthDate);

            if (matcher.find()) {
                int year = Integer.parseInt(matcher.group(2));
                int currentYear = java.time.Year.now().getValue();
                return currentYear - year;
            }
        } catch (Exception e) {
            // Ignora errori
        }

        return 0;
    }

    private Player searchInRankings(String playerName) {
        System.out.println("🔄 Ricerca fallback nelle classifiche...");

        List<Player> atpPlayers = getATPRankings(100);
        for (Player player : atpPlayers) {
            if (player.getNome().toLowerCase().contains(playerName.toLowerCase())) {
                System.out.println("✅ Trovato in ATP rankings");
                return player;
            }
        }

        List<Player> wtaPlayers = getWTARankings(100);
        for (Player player : wtaPlayers) {
            if (player.getNome().toLowerCase().contains(playerName.toLowerCase())) {
                System.out.println("✅ Trovato in WTA rankings");
                return player;
            }
        }

        return null;
    }

    // ==================== HEAD TO HEAD CON IMMAGINI WIKIPEDIA ====================

    /**
     * Recupera i dati H2H tra due giocatori da matchstat.com
     * E le immagini da Wikipedia
     *
     * @param player1 Nome completo primo giocatore (es. "Jannik Sinner")
     * @param player2 Nome completo secondo giocatore (es. "Lorenzo Musetti")
     * @return Oggetto H2HData con tutte le statistiche e immagini
     */
    public H2HData getH2HData(String player1, String player2) {
        H2HData h2hData = new H2HData();

        try {
            // 1️⃣ OTTIENI STATISTICHE DA MATCHSTAT
            String formattedPlayer1 = formatPlayerNameForURL(player1);
            String formattedPlayer2 = formatPlayerNameForURL(player2);

            String url = String.format("https://matchstat.com/tennis/h2h-odds-bets/%s/%s/",
                    formattedPlayer1, formattedPlayer2);

            System.out.println("🔍 Recupero H2H da: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    System.out.println("❌ Errore HTTP: " + response.code());
                    return null;
                }

                String html = response.body().string();
                Document doc = Jsoup.parse(html);

                h2hData.setPlayer1Name(player1);
                h2hData.setPlayer2Name(player2);

                // Nomi giocatori da Matchstat
                Elements playerNames = doc.select(".table-player__name, .player-name, h2.player-name, .h2h-player-name");
                if (playerNames.size() >= 2) {
                    String name1 = playerNames.get(0).text().trim();
                    String name2 = playerNames.get(1).text().trim();
                    if (!name1.isEmpty()) h2hData.setPlayer1Name(name1);
                    if (!name2.isEmpty()) h2hData.setPlayer2Name(name2);
                    System.out.println("✅ Giocatori trovati: " + h2hData.getPlayer1Name() + " vs " + h2hData.getPlayer2Name());
                }

                // Statistiche dalla tabella
                Elements statRows = doc.select("tr");
                int statsFound = 0;

                for (Element row : statRows) {
                    Elements cells = row.select("td");
                    if (cells.size() < 3) continue;

                    String stat1 = cells.get(0).text().trim();
                    String label = cells.get(1).text().trim();
                    String stat2 = cells.get(2).text().trim();

                    // Prize Money
                    if (label.contains("Career Prize Money") || label.contains("Prize Money")) {
                        h2hData.setPlayer1PrizeMoney(stat1);
                        h2hData.setPlayer2PrizeMoney(stat2);
                        statsFound++;
                    }
                    // Career W/L
                    else if (label.contains("Career Total W/L") || label.contains("Career W/L") || label.contains("Total W/L")) {
                        h2hData.setPlayer1WinLoss(extractWinLoss(stat1));
                        h2hData.setPlayer1WinPercentage(extractPercentage(stat1));
                        h2hData.setPlayer2WinLoss(extractWinLoss(stat2));
                        h2hData.setPlayer2WinPercentage(extractPercentage(stat2));
                        statsFound++;
                    }
                    // YTD W/L (stop parsing)
                    else if (label.contains("YTD Win/Loss") || label.contains("YTD W/L")) {
                        break;
                    }
                    // Superfici
                    else if (label.equals("Clay") || label.contains("Clay")) {
                        h2hData.setPlayer1Clay(parseIntSafe(stat1));
                        h2hData.setPlayer2Clay(parseIntSafe(stat2));
                        statsFound++;
                    }
                    else if (label.equals("Hard") || label.contains("Hard")) {
                        h2hData.setPlayer1Hard(parseIntSafe(stat1));
                        h2hData.setPlayer2Hard(parseIntSafe(stat2));
                        statsFound++;
                    }
                    else if (label.equals("Indoor") || label.contains("Indoor")) {
                        h2hData.setPlayer1Indoor(parseIntSafe(stat1));
                        h2hData.setPlayer2Indoor(parseIntSafe(stat2));
                        statsFound++;
                    }
                    else if (label.equals("Grass") || label.contains("Grass")) {
                        h2hData.setPlayer1Grass(parseIntSafe(stat1));
                        h2hData.setPlayer2Grass(parseIntSafe(stat2));
                        statsFound++;
                    }
                    // Titoli
                    else if (label.equals("Titles") || label.contains("Titles") || label.contains("Titoli")) {
                        h2hData.setPlayer1Titles(parseIntSafe(stat1));
                        h2hData.setPlayer2Titles(parseIntSafe(stat2));
                        statsFound++;
                    }
                    // H2H Record
                    else if (label.contains("Total H2H Matches") || label.contains("H2H Matches") || label.contains("Total Matches")) {
                        int p1Wins = parseIntSafe(stat1);
                        int p2Wins = parseIntSafe(stat2);
                        int totalH2H = p1Wins + p2Wins;
                        h2hData.setTotalH2HMatches(totalH2H);
                        h2hData.setH2hRecord(p1Wins + "-" + p2Wins);
                        statsFound++;
                        System.out.println("   ✅ H2H Record: " + h2hData.getH2hRecord());
                    }
                }

                System.out.println("✅ Statistiche estratte: " + statsFound);
            }

            // 2️⃣ OTTIENI IMMAGINI DA WIKIPEDIA
            System.out.println("📸 Recupero immagini da Wikipedia...");

            String img1 = getPlayerImageFromWikipedia(player1);
            String img2 = getPlayerImageFromWikipedia(player2);

            if (img1 != null) {
                h2hData.setPlayer1Image(img1);
                System.out.println("✅ Immagine Player 1 trovata");
            } else {
                System.out.println("⚠️ Immagine Player 1 non trovata");
            }

            if (img2 != null) {
                h2hData.setPlayer2Image(img2);
                System.out.println("✅ Immagine Player 2 trovata");
            } else {
                System.out.println("⚠️ Immagine Player 2 non trovata");
            }

            return h2hData;

        } catch (Exception e) {
            System.out.println("❌ Errore scraping H2H: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Recupera l'immagine di un giocatore da Wikipedia
     *
     * @param playerName Nome del giocatore (es. "Jannik Sinner")
     * @return URL dell'immagine o null se non trovata
     */
    private String getPlayerImageFromWikipedia(String playerName) {
        try {
            String wikiName = formatWikipediaName(playerName);
            String url = "https://it.wikipedia.org/wiki/" + wikiName;

            System.out.println("   📄 Tentativo Wikipedia: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    System.out.println("   ⚠️ Pagina non trovata per: " + playerName);
                    return null;
                }

                String html = response.body().string();
                Document doc = Jsoup.parse(html);

                // Verifica che sia un tennista
                if (!isTennisPlayer(doc)) {
                    System.out.println("   ⚠️ Non è un tennista: " + playerName);
                    return null;
                }

                // Estrai immagine dall'infobox
                Elements infobox = doc.select("table.infobox");
                if (!infobox.isEmpty()) {
                    String imageUrl = extractImageUrl(infobox.first());
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        System.out.println("   ✅ Immagine trovata per: " + playerName);
                        return imageUrl;
                    }
                }

                System.out.println("   ⚠️ Immagine non trovata nell'infobox per: " + playerName);
                return null;
            }

        } catch (Exception e) {
            System.out.println("   ❌ Errore recupero immagine per " + playerName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Formatta il nome del giocatore per l'URL
     * Es: "Jannik Sinner" -> "Jannik%20Sinner"
     */
    private String formatPlayerNameForURL(String name) {
        if (name == null || name.isEmpty()) return "";

        // Capitalizza ogni parola e sostituisci spazi con %20
        String[] words = name.trim().split("\\s+");
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            if (word.length() > 0) {
                formatted.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    formatted.append(word.substring(1).toLowerCase());
                }

                if (i < words.length - 1) {
                    formatted.append("%20");
                }
            }
        }

        return formatted.toString();
    }

    /**
     * Estrae la percentuale da una stringa tipo "59.38% (269-184)"
     */
    private String extractPercentage(String text) {
        if (text == null || text.isEmpty()) return "0%";

        Pattern pattern = Pattern.compile("(\\d+\\.?\\d*)%");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1) + "%";
        }

        return "0%";
    }

    /**
     * Estrae il record W-L da una stringa tipo "59.38% (269-184)"
     */
    private String extractWinLoss(String text) {
        if (text == null || text.isEmpty()) return "0-0";

        Pattern pattern = Pattern.compile("\\((\\d+-\\d+)\\)");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "0-0";
    }

    /**
     * Parse sicuro di interi
     */
    private int parseIntSafe(String text) {
        if (text == null || text.isEmpty()) return 0;
        try {
            text = text.replaceAll("[^0-9]", ""); // Solo cifre
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Metodo wrapper per compatibilità con il vecchio codice
     * Restituisce una stringa formattata con i dati H2H
     */
    public String getH2H(String player1, String player2) {
        H2HData data = getH2HData(player1, player2);

        if (data == null) {
            return "❌ Impossibile recuperare i dati H2H.\n\n" +
                    "Possibili cause:\n" +
                    "• Errore di connessione al sito\n" +
                    "• Nomi non corretti\n" +
                    "• Sito non raggiungibile\n\n" +
                    "💡 Verifica lo spelling dei nomi e riprova.";
        }

        // Anche se non abbiamo trovato tutti i dati, mostriamo quello che abbiamo
        return formatH2HData(data);
    }

    /**
     * Formatta i dati H2H per la visualizzazione
     */
    private String formatH2HData(H2HData data) {
        StringBuilder sb = new StringBuilder();

        sb.append("⚔️ HEAD TO HEAD ⚔️\n\n");
        sb.append(String.format("👤 %s vs %s\n\n",
                data.getPlayer1Name() != null ? data.getPlayer1Name() : "Player 1",
                data.getPlayer2Name() != null ? data.getPlayer2Name() : "Player 2"));

        // H2H Record
        if (data.getTotalH2HMatches() > 0 || data.getH2hRecord() != null) {
            sb.append("📊 SCONTRI DIRETTI\n");
            if (data.getTotalH2HMatches() > 0) {
                sb.append(String.format("Totale partite: %d\n", data.getTotalH2HMatches()));
            }
            if (data.getH2hRecord() != null && !data.getH2hRecord().isEmpty()) {
                sb.append(String.format("Record: %s\n", data.getH2hRecord()));
            }
            sb.append("\n");
        }

        // Statistiche Career
        boolean hasCareerStats = false;
        StringBuilder careerStats = new StringBuilder();

        if (data.getPlayer1PrizeMoney() != null || data.getPlayer2PrizeMoney() != null) {
            careerStats.append(String.format("💰 Prize Money:\n"));
            if (data.getPlayer1PrizeMoney() != null) {
                careerStats.append(String.format("   %s: %s\n",
                        data.getPlayer1Name() != null ? data.getPlayer1Name() : "Player 1",
                        data.getPlayer1PrizeMoney()));
            }
            if (data.getPlayer2PrizeMoney() != null) {
                careerStats.append(String.format("   %s: %s\n",
                        data.getPlayer2Name() != null ? data.getPlayer2Name() : "Player 2",
                        data.getPlayer2PrizeMoney()));
            }
            careerStats.append("\n");
            hasCareerStats = true;
        }

        if (data.getPlayer1WinLoss() != null || data.getPlayer2WinLoss() != null) {
            careerStats.append(String.format("📈 Win/Loss Totale:\n"));
            if (data.getPlayer1WinLoss() != null) {
                careerStats.append(String.format("   %s: %s (%s)\n",
                        data.getPlayer1Name() != null ? data.getPlayer1Name() : "Player 1",
                        data.getPlayer1WinLoss(),
                        data.getPlayer1WinPercentage() != null ? data.getPlayer1WinPercentage() : "N/A"));
            }
            if (data.getPlayer2WinLoss() != null) {
                careerStats.append(String.format("   %s: %s (%s)\n",
                        data.getPlayer2Name() != null ? data.getPlayer2Name() : "Player 2",
                        data.getPlayer2WinLoss(),
                        data.getPlayer2WinPercentage() != null ? data.getPlayer2WinPercentage() : "N/A"));
            }
            careerStats.append("\n");
            hasCareerStats = true;
        }

        if (data.getPlayer1Titles() > 0 || data.getPlayer2Titles() > 0) {
            careerStats.append(String.format("🏅 Titoli:\n"));
            careerStats.append(String.format("   %s: %d\n",
                    data.getPlayer1Name() != null ? data.getPlayer1Name() : "Player 1",
                    data.getPlayer1Titles()));
            careerStats.append(String.format("   %s: %d\n",
                    data.getPlayer2Name() != null ? data.getPlayer2Name() : "Player 2",
                    data.getPlayer2Titles()));
            careerStats.append("\n");
            hasCareerStats = true;
        }

        if (hasCareerStats) {
            sb.append("🏆 STATISTICHE CARRIERA\n\n");
            sb.append(careerStats);
        }

        // Statistiche per superficie
        if (data.getPlayer1Clay() > 0 || data.getPlayer2Clay() > 0 ||
                data.getPlayer1Hard() > 0 || data.getPlayer2Hard() > 0 ||
                data.getPlayer1Indoor() > 0 || data.getPlayer2Indoor() > 0) {

            sb.append("🎾 VITTORIE PER SUPERFICIE\n\n");

            if (data.getPlayer1Clay() > 0 || data.getPlayer2Clay() > 0) {
                sb.append(String.format("🟤 Clay:\n"));
                sb.append(String.format("   %s: %d\n",
                        data.getPlayer1Name() != null ? data.getPlayer1Name() : "Player 1",
                        data.getPlayer1Clay()));
                sb.append(String.format("   %s: %d\n\n",
                        data.getPlayer2Name() != null ? data.getPlayer2Name() : "Player 2",
                        data.getPlayer2Clay()));
            }

            if (data.getPlayer1Grass() > 0 || data.getPlayer2Grass() > 0) {
                sb.append(String.format("🟢 Grass:\n"));
                sb.append(String.format("   %s: %d\n",
                        data.getPlayer1Name() != null ? data.getPlayer1Name() : "Player 1",
                        data.getPlayer1Grass()));
                sb.append(String.format("   %s: %d\n\n",
                        data.getPlayer2Name() != null ? data.getPlayer2Name() : "Player 2",
                        data.getPlayer2Grass()));
            }

            if (data.getPlayer1Hard() > 0 || data.getPlayer2Hard() > 0) {
                sb.append(String.format("🔵 Hard:\n"));
                sb.append(String.format("   %s: %d\n",
                        data.getPlayer1Name() != null ? data.getPlayer1Name() : "Player 1",
                        data.getPlayer1Hard()));
                sb.append(String.format("   %s: %d\n\n",
                        data.getPlayer2Name() != null ? data.getPlayer2Name() : "Player 2",
                        data.getPlayer2Hard()));
            }

            if (data.getPlayer1Indoor() > 0 || data.getPlayer2Indoor() > 0) {
                sb.append(String.format("🏠 Indoor:\n"));
                sb.append(String.format("   %s: %d\n",
                        data.getPlayer1Name() != null ? data.getPlayer1Name() : "Player 1",
                        data.getPlayer1Indoor()));
                sb.append(String.format("   %s: %d\n\n",
                        data.getPlayer2Name() != null ? data.getPlayer2Name() : "Player 2",
                        data.getPlayer2Indoor()));
            }
        }

        sb.append("📊 Fonte: matchstat.com");

        return sb.toString();
    }

    // ────────────── PARTITE LIVE CON VINCITORE ──────────────
    public List<Match> getRecentMatches() {
        List<Match> matches = new ArrayList<>();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--window-size=1920,1080");

        WebDriver driver = new ChromeDriver(options);
        JavascriptExecutor js = (JavascriptExecutor) driver;

        Set<String> processedTexts = new HashSet<>();
        String currentTournament = "Generale";
        String currentLocation = "";
        boolean waitingForLocation = false;

        try {
            driver.get("https://www.sofascore.com/it/tennis");
            Thread.sleep(2000);

            int maxScrolls = 180;
            for (int scroll = 0; scroll < maxScrolls; scroll++) {
                List<WebElement> elements = driver.findElements(By.cssSelector("a[href^='/it/tennis/']"));

                for (WebElement el : elements) {
                    try {
                        String text = el.getText().trim();
                        if (text.isEmpty()) continue;

                        /* ───── TITOLO TORNEO ───── */
                        if (isTournamentTitle(text)) {
                            currentTournament = text;
                            currentLocation = "";
                            waitingForLocation = true;
                            continue;
                        }

                        /* ───── LUOGO TORNEO (subito dopo il titolo) ───── */
                        if (waitingForLocation && currentLocation.isEmpty() && isLocationLine(text)) {
                            currentLocation = text;
                            waitingForLocation = false;
                            continue;
                        }

                        /* ───── DA QUI IN POI SOLO MATCH ───── */
                        if (!text.contains("\n")) continue;
                        if (!processedTexts.add(text)) continue;

                        MatchTextData data = parseMatchText(text);
                        if (!data.isValid() || data.time.isEmpty() || !data.hasValidStatus()) continue;

                        // Creiamo la match impostando solo l'orario e la priority
                        Match match = new Match(
                                currentTournament,
                                currentLocation,
                                data.players.get(0),
                                data.players.get(1),
                                data.time,
                                data.time,
                                0
                        );

                        // Impostiamo lo status corretto
                        match.setStatus(data.status);

                        // ✅ NUOVO: Salva il punteggio del game corrente se LIVE
                        if (data.currentGame != null && !data.currentGame.isEmpty()) {
                            match.setCurrentGame(data.currentGame);
                        }

                        // Punteggio dettagliato
                        if (!data.scores.isEmpty()) {
                            String scoreString = String.join(" ", data.scores);
                            match.setDetailedScore(scoreString);

                            // Se è finita, calcoliamo il numero di set vinti da ciascuno
                            if (data.status.equals("FINE") || data.status.equals("A tavolino")) {
                                int player1Sets = 0;
                                int player2Sets = 0;

                                for (String set : data.scores) {
                                    String[] parts = set.replaceAll("\\(\\d+\\)", "").split("-");
                                    if (parts.length != 2) continue;

                                    try {
                                        int score1 = Integer.parseInt(parts[0].trim());
                                        int score2 = Integer.parseInt(parts[1].trim());

                                        if (score1 > score2) player1Sets++;
                                        else if (score2 > score1) player2Sets++;
                                    } catch (NumberFormatException e) {
                                        // ignora set non valido
                                    }
                                }

                                // Mettiamo sempre prima il numero maggiore
                                int maxSets = Math.max(player1Sets, player2Sets);
                                int minSets = Math.min(player1Sets, player2Sets);
                                match.setSetScore(maxSets + "-" + minSets);
                            }
                        }

                        // Determina il vincitore se partita finita
                        if (data.status.equals("FINE") || data.status.equals("A tavolino")) {
                            String winner = determineWinner(data);
                            if (winner != null) {
                                match.setWinner(winner);
                            }
                        }

                        matches.add(match);

                    } catch (StaleElementReferenceException ignored) {}
                }

                js.executeScript("window.scrollBy(0, 400);");
                Thread.sleep(50);
            }

        } catch (StopScraperException e) {
            System.out.println("⛔ " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }

        return matches;
    }

    private static boolean isLocationLine(String text) {
        // Esempi validi:
        // Rome, Italy
        // Doha, Qatar 🇶🇦
        // Paris, France (Indoor)
        // Melbourne, Australia • ATP

        return text.matches("^[A-Za-z .'-]+,\\s*[A-Za-z .'-]+.*$");
    }

    // ────────────── DETERMINA VINCITORE ──────────────
    private String determineWinner(MatchTextData data) {
        if (data.players.size() < 2 || data.scores.isEmpty()) {
            return null;
        }

        int player1Sets = 0;
        int player2Sets = 0;

        // Analizza ogni set (formato: "6-4" o "7-6(3)")
        for (String setScore : data.scores) {
            String[] parts = setScore.replaceAll("\\(\\d+\\)", "").split("-");
            if (parts.length != 2) continue;

            try {
                int score1 = Integer.parseInt(parts[0].trim());
                int score2 = Integer.parseInt(parts[1].trim());

                if (score1 > score2) {
                    player1Sets++;
                } else if (score2 > score1) {
                    player2Sets++;
                }
            } catch (NumberFormatException e) {
                // Ignora set non validi
            }
        }

        // Chi ha vinto più set ha vinto la partita
        if (player1Sets > player2Sets) {
            return data.players.get(0);
        } else if (player2Sets > player1Sets) {
            return data.players.get(1);
        }

        return null; // Parità (improbabile)
    }

    // ────────────── PARSE MATCH TEXT AGGIORNATO (GESTIONE LIVE CON "A") ──────────────
    private static MatchTextData parseMatchText(String text) {
        MatchTextData data = new MatchTextData();
        String[] lines = text.split("\n");

        List<String> allNumbers = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // Orario
            if (line.matches("\\d{1,2}:\\d{2}")) {
                data.time = line;
            }
            // Status
            else if (line.matches("LIVE|FINE|SRF|A tavolino|.*set|-|Annullata|Iniziato")) {
                data.status = line;
            }
            // Punteggio già formattato (es. "6-4" o "7-6(3)")
            else if (line.matches("\\d{1,2}-\\d{1,2}(\\(\\d+\\))?")) {
                data.scores.add(line);
            }
            // Numeri o lettere (punti live, punteggi parziali)
            else if (line.matches("\\d+|A")) {
                allNumbers.add(line);
            }
            // Nomi giocatori
            else {
                data.players.add(line);
            }
        }

        // Ricostruisci i punteggi dai numeri se non già formattati
        if (data.scores.isEmpty() && !allNumbers.isEmpty()) {
            boolean isLive = data.status.equals("LIVE") || data.status.matches("[1-5]º set");

            // ✅ NUOVO: Salva i primi due valori come punteggio live
            if (isLive && allNumbers.size() >= 2) {
                data.currentGame = allNumbers.get(0) + "-" + allNumbers.get(1);
            }

            data.scores = parseScoreNumbers(allNumbers, isLive);
        }

        return data;
    }

    // ────────────── PARSING INTELLIGENTE PUNTEGGI (gestione LIVE con A) ──────────────
    private static List<String> parseScoreNumbers(List<String> allNumbers, boolean isLive) {
        List<String> sets = new ArrayList<>();

        // Se LIVE, scarta i primi 2 valori dai set (già salvati in currentGame)
        List<String> setNumbers;
        if (isLive && allNumbers.size() > 2) {
            setNumbers = allNumbers.subList(2, allNumbers.size());
        } else {
            setNumbers = new ArrayList<>(allNumbers);
        }

        // Converti solo i valori numerici per il parsing dei set
        List<Integer> scores = new ArrayList<>();
        for (String s : setNumbers) {
            try {
                int num = Integer.parseInt(s);
                scores.add(num);
            } catch (NumberFormatException e) {
                // Ignora lettere (es. "A") nei punteggi dei set
            }
        }

        // Rimuovi gli ultimi 2 numeri se rappresentano il punteggio totale dei set
        if (scores.size() >= 2) {
            scores = scores.subList(0, scores.size() - 2);
        }

        // Ignora l'ultimo numero se la lista è dispari
        if (scores.size() % 2 != 0) {
            scores = scores.subList(0, scores.size() - 1);
        }

        if (scores.isEmpty()) return sets;

        int numSets = scores.size() / 2;

        // Ricostruisci i set: player1[i] vs player2[i]
        for (int i = 0; i < numSets; i++) {
            int score1 = scores.get(i);
            int score2 = scores.get(i + numSets);

            if (isValidTennisScore(score1, score2)) {
                String setScore = score1 + "-" + score2;
                sets.add(setScore);

                // Se il set è 7-6, il prossimo numero è il tiebreak
                if ((score1 == 7 && score2 == 6) || (score1 == 6 && score2 == 7)) {
                    if (i + 1 < numSets) {
                        int nextNum = Math.min(scores.get(i + 1), scores.get(i + numSets + 1));
                        sets.set(sets.size() - 1, setScore + "(" + nextNum + ")");
                        i++;
                    }
                }
            } else {
                break; // set non valido
            }
        }

        return sets;
    }

    // ────────────── VALIDAZIONE PUNTEGGIO TENNIS ──────────────
    private static boolean isValidTennisScore(int score1, int score2) {
        // Punteggi validi nel tennis
        // Normale: 6-0, 6-1, 6-2, 6-3, 6-4, 7-5, 7-6
        // Tiebreak: 7-6, 6-7
        if(score1 < 0 || score2 < 0)
            return false;

        // Casi validi
        if (score1 == 6 && score2 <= 4) return true; // 6-0, 6-1, 6-2, 6-3, 6-4
        if (score2 == 6 && score1 <= 4) return true; // 0-6, 1-6, 2-6, 3-6, 4-6
        if (score1 == 7 && (score2 == 5 || score2 == 6)) return true; // 7-5, 7-6
        if (score2 == 7 && (score1 == 5 || score1 == 6)) return true; // 5-7, 6-7
        if (score1 == 10 && score2 <= 8) return true; //per Supertiebreak normali
        if (score2 == 10 && score1 <= 8) return true;

        return true;
    }

    // ────────────── CLASSE INTERNA AGGIORNATA ──────────────
    private static class MatchTextData {
        String time = "";
        List<String> players = new ArrayList<>();
        List<String> scores = new ArrayList<>();
        String status = "";
        String currentGame = ""; // ✅ NUOVO CAMPO

        boolean isValid() { return players.size() >= 2; }
        boolean hasValidStatus() {
            if (status == null || status.isEmpty()) return false;
            return status.equals("LIVE") || status.equals("FINE") || status.equals("A tavolino") ||
                    status.equals("SRF") || status.equals("Annullata") || status.equals("Iniziato") ||
                    status.equals("-") || status.contains("set");
        }
    }

    // ────────────── UTILITY ──────────────
    private static boolean isTournamentTitle(String text) {
        String upper = text.toUpperCase();

        if (upper.contains("ATP 125") || upper.contains("WTA 125") ||
                upper.contains("ITF") || upper.contains("CHALLENGER")) {
            throw new StopScraperException("Torneo non interessante: " + text);
        }

        String[] allowedTournaments = {
                "Grand Slam", "Masters 1000", "ATP 250", "ATP 500", "WTA 250",
                "WTA 500", "WTA 1000", "United Cup"
        };

        for (String t : allowedTournaments) {
            if (text.equals(t) || text.startsWith(t)) return true;
        }
        return false;
    }

    private static class StopScraperException extends RuntimeException {
        public StopScraperException(String message) { super(message); }
    }
}