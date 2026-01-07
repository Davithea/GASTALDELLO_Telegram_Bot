package scraper;

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
    private final String rapidApiKey;

    public TennisService(String rapidApiKey) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        this.rapidApiKey = rapidApiKey;
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

    private String extractImageUrl(Element table) {
        Elements images = table.select("img");

        for (Element img : images) {
            String src = img.attr("src");

            if (src.contains("Flag_of") || src.contains("icon") ||
                    src.contains("logo") || src.length() < 20) {
                continue;
            }

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

    // ==================== HEAD TO HEAD ====================

    public String getH2H(String player1, String player2) {
        System.out.println("📊 H2H: " + player1 + " vs " + player2);

        return "⚔️ HEAD TO HEAD\n\n" +
                player1 + " vs " + player2 + "\n\n" +
                "ℹ️ Statistiche H2H non disponibili con scraping.\n\n" +
                "💡 Consulta:\n" +
                "- https://www.atptour.com/\n" +
                "- https://www.wtatennis.com/\n" +
                "- https://www.ultimatetennisstatistics.com/";
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

        try {
            driver.get("https://www.sofascore.com/it/tennis");
            Thread.sleep(2000);

            int maxScrolls = 180;
            for (int scroll = 0; scroll < maxScrolls; scroll++) {
                List<WebElement> elements = driver.findElements(By.cssSelector("a[href^='/it/tennis/']"));

                for (WebElement el : elements) {
                    try {
                        String text = el.getText().trim();
                        if (text.isEmpty() || !processedTexts.add(text)) continue;

                        if (isTournamentTitle(text)) {
                            currentTournament = text;
                            continue;
                        }

                        if (!text.contains("\n")) continue;

                        MatchTextData data = parseMatchText(text);
                        if (!data.isValid() || data.time.isEmpty() || !data.hasValidStatus()) continue;

                        // Creiamo la match impostando solo l'orario e la priority
                        Match match = new Match(
                                currentTournament,
                                data.players.get(0),
                                data.players.get(1),
                                data.time,
                                data.time,
                                0
                        );

                        // Impostiamo lo status corretto
                        match.setStatus(data.status);

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
                                match.setSetScore(maxSets + "-" + minSets); // es. "2-0"
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

    // ────────────── PARSE MATCH TEXT AGGIORNATO ──────────────
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
            // Numeri (potrebbero essere punteggi o altro)
            else if (line.matches("\\d+")) {
                allNumbers.add(line);
                System.out.println("  [NUM] " + line);
            }
            // Nomi giocatori
            else {
                data.players.add(line);
                System.out.println("  [PLAYER] " + line);
            }
        }

        System.out.println("  [STATUS] " + data.status);
        System.out.println("  [TIME] " + data.time);
        System.out.println("  [NUMBERS] " + allNumbers);

        // Ricostruisci i punteggi dai numeri se non già formattati
        if (data.scores.isEmpty() && !allNumbers.isEmpty()) {
            boolean isLive = data.status.equals("LIVE");
            data.scores = parseScoreNumbers(allNumbers, isLive);
        }

        return data;
    }

    // ────────────── PARSING INTELLIGENTE PUNTEGGI ──────────────
    private static List<String> parseScoreNumbers(List<String> allNumbers, boolean isLive) {
        List<String> sets = new ArrayList<>();

        // Converti ogni elemento della lista in numero intero
        List<Integer> scores = new ArrayList<>();
        for (String numStr : allNumbers) {
            try {
                int num = Integer.parseInt(numStr);
                scores.add(num);
            } catch (NumberFormatException e) {
                // Ignora elementi non numerici
                System.out.println("  [⚠️ NON-NUMERIC] Skipping: " + numStr);
            }
        }

        System.out.println("  [NUMBERS] " + scores);

        // Se LIVE, scarta le prime 4 cifre (punti del game corrente)
        if (isLive && scores.size() >= 6) {
            scores = scores.subList(4, scores.size());
            System.out.println("  [NUMBERS AFTER LIVE CUT] " + scores);
        }

        // RIMUOVI GLI ULTIMI 2 NUMERI (punteggio totale set: es. 2-0)
        if (scores.size() >= 2) {
            scores = scores.subList(0, scores.size() - 2);
            System.out.println("  [NUMBERS AFTER REMOVING SET TOTAL] " + scores);
        }

        // Se numero dispari di numeri, ignora l'ultimo numero (dato incompleto)
        if (scores.size() % 2 != 0) {
            scores = scores.subList(0, scores.size() - 1);
        }

        if (scores.isEmpty()) return sets;

        // Dividi in due metà: prima metà = giocatore1, seconda metà = giocatore2
        int numSets = scores.size() / 2;

        System.out.println("  [NUM SETS] " + numSets);

        // Ricostruisci i set: player1[i] vs player2[i]
        for (int i = 0; i < numSets; i++) {
            int score1 = scores.get(i);
            int score2 = scores.get(i + numSets);

            System.out.println("  [TRY SET " + (i+1) + "] " + score1 + "-" + score2);

            // Validazione punteggio tennis
            if (isValidTennisScore(score1, score2)) {
                String setScore = score1 + "-" + score2;
                sets.add(setScore);
                System.out.println("  [✅ VALID SET] " + setScore);

                // Se il set è 7-6, il PROSSIMO numero è il tiebreak
                if ((score1 == 7 && score2 == 6) || (score1 == 6 && score2 == 7)) {
                    if (i + 1 < numSets) {
                        int nextNum = Math.min(scores.get(i + 1), scores.get(i + numSets + 1));
                        sets.set(sets.size() - 1, setScore + "(" + nextNum + ")");
                        System.out.println("  [🎾 TIEBREAK] Added (" + nextNum + ")");
                        i++;
                    }
                }
            } else {
                System.out.println("  [❌ INVALID SET] " + score1 + "-" + score2 + " - stopping");
                break;
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

        return false;
    }

    // ────────────── CLASSE INTERNA AGGIORNATA ──────────────
    private static class MatchTextData {
        String time = "";
        List<String> players = new ArrayList<>();
        List<String> scores = new ArrayList<>(); // NUOVO: punteggi set
        String status = "";

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

        if (upper.contains("ATP 125") || upper.contains("WTA 250") ||
                upper.contains("ITF") || upper.contains("CHALLENGER")) {
            throw new StopScraperException("Torneo non interessante: " + text);
        }

        String[] allowedTournaments = {
                "Grand Slam", "Masters 1000", "ATP 250", "ATP 500",
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