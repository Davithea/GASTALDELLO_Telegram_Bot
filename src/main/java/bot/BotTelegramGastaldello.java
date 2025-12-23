package bot;

import database.DatabaseManager;
import model.Match;
import model.Player;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import scraper.TennisService;

import java.util.*;

public class BotTelegramGastaldello implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient;
    private final TennisService tennisService;
    private final DatabaseManager databaseManager;

    // Stato conversazione per comando /cerca interattivo
    private final Map<Long, String> waitingForPlayerName = new HashMap<>();

    // Mappa completa paese -> emoji
    private static final Map<String, String> COUNTRY_FLAGS = new HashMap<>();

    static {
        // Europa
        COUNTRY_FLAGS.put("ITALY", "🇮🇹");
        COUNTRY_FLAGS.put("ITA", "🇮🇹");
        COUNTRY_FLAGS.put("SPAIN", "🇪🇸");
        COUNTRY_FLAGS.put("ESP", "🇪🇸");
        COUNTRY_FLAGS.put("SERBIA", "🇷🇸");
        COUNTRY_FLAGS.put("SRB", "🇷🇸");
        COUNTRY_FLAGS.put("RUSSIA", "🇷🇺");
        COUNTRY_FLAGS.put("RUS", "🇷🇺");
        COUNTRY_FLAGS.put("FRANCE", "🇫🇷");
        COUNTRY_FLAGS.put("FRA", "🇫🇷");
        COUNTRY_FLAGS.put("GERMANY", "🇩🇪");
        COUNTRY_FLAGS.put("GER", "🇩🇪");
        COUNTRY_FLAGS.put("DEU", "🇩🇪");
        COUNTRY_FLAGS.put("GREECE", "🇬🇷");
        COUNTRY_FLAGS.put("GRE", "🇬🇷");
        COUNTRY_FLAGS.put("NORWAY", "🇳🇴");
        COUNTRY_FLAGS.put("NOR", "🇳🇴");
        COUNTRY_FLAGS.put("DENMARK", "🇩🇰");
        COUNTRY_FLAGS.put("DEN", "🇩🇰");
        COUNTRY_FLAGS.put("POLAND", "🇵🇱");
        COUNTRY_FLAGS.put("POL", "🇵🇱");
        COUNTRY_FLAGS.put("BULGARIA", "🇧🇬");
        COUNTRY_FLAGS.put("BUL", "🇧🇬");
        COUNTRY_FLAGS.put("CROATIA", "🇭🇷");
        COUNTRY_FLAGS.put("CRO", "🇭🇷");
        COUNTRY_FLAGS.put("SWITZERLAND", "🇨🇭");
        COUNTRY_FLAGS.put("SUI", "🇨🇭");
        COUNTRY_FLAGS.put("AUSTRIA", "🇦🇹");
        COUNTRY_FLAGS.put("AUT", "🇦🇹");
        COUNTRY_FLAGS.put("CZECH REPUBLIC", "🇨🇿");
        COUNTRY_FLAGS.put("CZE", "🇨🇿");
        COUNTRY_FLAGS.put("NETHERLANDS", "🇳🇱");
        COUNTRY_FLAGS.put("NED", "🇳🇱");
        COUNTRY_FLAGS.put("BELGIUM", "🇧🇪");
        COUNTRY_FLAGS.put("BEL", "🇧🇪");
        COUNTRY_FLAGS.put("SWEDEN", "🇸🇪");
        COUNTRY_FLAGS.put("SWE", "🇸🇪");
        COUNTRY_FLAGS.put("UNITED KINGDOM", "🇬🇧");
        COUNTRY_FLAGS.put("GREAT BRITAIN", "🇬🇧");
        COUNTRY_FLAGS.put("GBR", "🇬🇧");
        COUNTRY_FLAGS.put("PORTUGAL", "🇵🇹");
        COUNTRY_FLAGS.put("POR", "🇵🇹");

        // Americhe
        COUNTRY_FLAGS.put("USA", "🇺🇸");
        COUNTRY_FLAGS.put("UNITED STATES", "🇺🇸");
        COUNTRY_FLAGS.put("ARGENTINA", "🇦🇷");
        COUNTRY_FLAGS.put("ARG", "🇦🇷");
        COUNTRY_FLAGS.put("BRAZIL", "🇧🇷");
        COUNTRY_FLAGS.put("BRA", "🇧🇷");
        COUNTRY_FLAGS.put("CANADA", "🇨🇦");
        COUNTRY_FLAGS.put("CAN", "🇨🇦");
        COUNTRY_FLAGS.put("CHILE", "🇨🇱");
        COUNTRY_FLAGS.put("CHI", "🇨🇱");
        COUNTRY_FLAGS.put("MEXICO", "🇲🇽");
        COUNTRY_FLAGS.put("MEX", "🇲🇽");

        // Asia/Oceania
        COUNTRY_FLAGS.put("AUSTRALIA", "🇦🇺");
        COUNTRY_FLAGS.put("AUS", "🇦🇺");
        COUNTRY_FLAGS.put("JAPAN", "🇯🇵");
        COUNTRY_FLAGS.put("JPN", "🇯🇵");
        COUNTRY_FLAGS.put("CHINA", "🇨🇳");
        COUNTRY_FLAGS.put("CHN", "🇨🇳");
        COUNTRY_FLAGS.put("KAZAKHSTAN", "🇰🇿");
        COUNTRY_FLAGS.put("KAZ", "🇰🇿");
        COUNTRY_FLAGS.put("SOUTH KOREA", "🇰🇷");
        COUNTRY_FLAGS.put("KOR", "🇰🇷");
        COUNTRY_FLAGS.put("NEW ZEALAND", "🇳🇿");
        COUNTRY_FLAGS.put("NZL", "🇳🇿");
    }

    public BotTelegramGastaldello(String botToken, String rapidApiKey) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.tennisService = new TennisService(rapidApiKey);
        this.databaseManager = new DatabaseManager();
    }

    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText().trim();
            Long chatId = update.getMessage().getChatId();
            String username = update.getMessage().getFrom().getUserName();

            databaseManager.saveUser(chatId, username);
            databaseManager.logInteraction(chatId, messageText);

            String response;

            // Gestione stato conversazionale per /cerca
            if (waitingForPlayerName.containsKey(chatId)) {
                response = handlePlayerSearch(chatId, messageText);
            } else {
                response = processCommand(messageText, chatId);
            }

            sendMessage(chatId, response, messageText.equals("/start"));
        }
    }

    private String processCommand(String command, Long chatId) {
        try {
            if (command.equals("/start")) {
                return "🎾 Benvenuto nel Tennis Bot!\n\n" +
                        "Sono il tuo assistente personale per il tennis.\n\n" +
                        "Comandi disponibili:\n" +
                        "🏆 /classificaATP - Classifica ATP Singolare\n" +
                        "👩 /classificaWTA - Classifica WTA Singolare\n" +
                        "👥 /classificaATPDoppio - Classifica ATP Doppio\n" +
                        "👭 /classificaWTADoppio - Classifica WTA Doppio\n" +
                        "📅 /partite - Partite live\n" +
                        "🔍 /cerca - Cerca un giocatore\n" +
                        "⭐ /preferiti - I tuoi giocatori preferiti\n" +
                        "➕ /aggiungi [nome] - Aggiungi ai preferiti\n" +
                        "➖ /rimuovi [nome] - Rimuovi dai preferiti\n" +
                        "📊 /statistiche - Le tue statistiche\n" +
                        "❓ /aiuto - Mostra questo messaggio\n\n" +
                        "📡 Classifiche: Wikipedia (scraping)\n" +
                        "⚡ Live: RapidAPI Tennis";
            }

            if (command.equals("/aiuto") || command.equals("/help")) {
                return processCommand("/start", chatId);
            }

            if (command.equals("/classifiche") || command.equals("🏆 Classifiche") || command.equals("/classificaATP")) {
                List<Player> rankings = tennisService.getTopRankings(10);
                databaseManager.savePlayers(rankings);
                return formatRankings(rankings, "ATP");
            }

            if (command.equals("/classificaWTA")) {
                List<Player> rankings = tennisService.getWTARankings(10);
                databaseManager.savePlayers(rankings);
                return formatRankings(rankings, "WTA");
            }

            if (command.equals("/classificaATPDoppio")) {
                List<Player> rankings = tennisService.getATPDoublesRankings(10);
                databaseManager.savePlayers(rankings);
                return formatRankings(rankings, "ATP DOPPIO");
            }

            if (command.equals("/classificaWTADoppio")) {
                List<Player> rankings = tennisService.getWTADoublesRankings(10);
                databaseManager.savePlayers(rankings);
                return formatRankings(rankings, "WTA DOPPIO");
            }

            if (command.equals("/statistiche") || command.equals("📊 Statistiche")) {
                return databaseManager.getUserStatistics(chatId);
            }

            if (command.equals("/partite") || command.equals("📅 Partite")) {
                List<Match> matches = tennisService.getRecentMatches();
                databaseManager.saveMatches(matches);
                return formatMatches(matches);
            }

            // COMANDO /CERCA INTERATTIVO
            if (command.equals("/cerca") || command.equals("🔍 Cerca")) {
                waitingForPlayerName.put(chatId, "WAITING_PLAYER_NAME");
                return "🔍 RICERCA GIOCATORE\n\n" +
                        "Scrivi il nome del giocatore che vuoi cercare.\n\n" +
                        "Esempi:\n" +
                        "• Sinner\n" +
                        "• Djokovic\n" +
                        "• Swiatek\n" +
                        "• Alcaraz\n\n" +
                        "Digita /annulla per annullare.";
            }

            // PREFERITI
            if (command.equals("/preferiti") || command.equals("⭐ Preferiti")) {
                return databaseManager.getFavoritePlayers(chatId);
            }

            if (command.startsWith("/aggiungi ")) {
                String playerName = command.replace("/aggiungi ", "").trim();
                if (playerName.isEmpty()) {
                    return "⚠️ Usa: /aggiungi [nome giocatore]\nEsempio: /aggiungi Sinner";
                }
                return databaseManager.addFavoritePlayer(chatId, playerName);
            }

            if (command.startsWith("/rimuovi ")) {
                String playerName = command.replace("/rimuovi ", "").trim();
                if (playerName.isEmpty()) {
                    return "⚠️ Usa: /rimuovi [nome giocatore]\nEsempio: /rimuovi Sinner";
                }
                return databaseManager.removeFavoritePlayer(chatId, playerName);
            }

            if (command.equals("/annulla")) {
                waitingForPlayerName.remove(chatId);
                return "❌ Ricerca annullata.";
            }

            return "❓ Comando non riconosciuto.\nDigita /aiuto per vedere i comandi disponibili.";

        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ Si è verificato un errore. Riprova più tardi.";
        }
    }

    /**
     * Gestisce la ricerca giocatore dopo che l'utente ha digitato /cerca
     */
    private String handlePlayerSearch(Long chatId, String playerName) {
        waitingForPlayerName.remove(chatId);

        if (playerName.equalsIgnoreCase("/annulla")) {
            return "❌ Ricerca annullata.";
        }

        if (playerName.isEmpty() || playerName.length() < 2) {
            return "⚠️ Nome troppo corto. Riprova con /cerca";
        }

        System.out.println("🔍 Ricerca: " + playerName);

        Player player = tennisService.searchPlayer(playerName);

        if (player != null) {
            databaseManager.savePlayer(player);
            return formatPlayerInfo(player) + "\n\n💡 Aggiungi ai preferiti con: /aggiungi " + player.getNome();
        } else {
            return "❌ Giocatore \"" + playerName + "\" non trovato.\n\n" +
                    "Assicurati che sia tra i top 100 ATP/WTA.\n" +
                    "Riprova con /cerca";
        }
    }

    private String formatRankings(List<Player> rankings, String type) {
        if (rankings.isEmpty()) {
            return "⚠️ CLASSIFICHE " + type + " NON DISPONIBILI\n\n" +
                    "Le classifiche non possono essere recuperate.\n" +
                    "Possibili cause:\n" +
                    "- Wikipedia temporaneamente offline\n" +
                    "- Problemi di connessione\n\n" +
                    "Riprova tra qualche minuto.";
        }

        StringBuilder sb = new StringBuilder("🏆 TOP 10 CLASSIFICA " + type + "\n\n");
        for (Player player : rankings) {
            sb.append(String.format("%d. %s %s\n",
                    player.getRanking(),
                    getFlagEmoji(player.getPaese()),
                    player.getNome()));
            sb.append(String.format("   Punti: %d\n\n", player.getPunti()));
        }
        sb.append("📅 Aggiornato: ").append(new java.util.Date());
        return sb.toString();
    }

    private String formatMatches(List<Match> matches) {
        if (matches.isEmpty()) {
            return "ℹ️  NESSUNA PARTITA LIVE AL MOMENTO\n\n" +
                    "Non ci sono partite in corso.\n\n" +
                    "💡 Le partite live sono disponibili durante:\n" +
                    "- Grand Slam (Australian Open, Roland Garros, Wimbledon, US Open)\n" +
                    "- Masters 1000\n" +
                    "- ATP 500/250\n" +
                    "- WTA 1000/500/250\n\n" +
                    "Riprova più tardi!";
        }

        StringBuilder sb = new StringBuilder("🎾 PARTITE LIVE\n\n");

        for (Match match : matches) {
            String emoji = getTournamentEmoji(match.getTournament());

            sb.append(String.format("%s %s\n", emoji, match.getTournament()));
            sb.append(String.format("%s vs %s\n", match.getPlayer1(), match.getPlayer2()));
            sb.append(String.format("Score: %s\n\n", match.getScore()));
        }

        return sb.toString();
    }

    private String getTournamentEmoji(String tournament) {
        String lower = tournament.toLowerCase();
        if (lower.contains("australian open") || lower.contains("roland garros") ||
                lower.contains("french open") || lower.contains("wimbledon") ||
                lower.contains("us open")) {
            return "🏆";
        } else if (lower.contains("masters") || lower.contains("finals")) {
            return "🥇";
        } else if (lower.contains("500")) {
            return "🥈";
        } else if (lower.contains("250")) {
            return "🥉";
        } else if (lower.contains("challenger")) {
            return "🎪";
        }
        return "🎾";
    }

    private String formatPlayerInfo(Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🎾 %s\n\n", player.getNome()));
        sb.append(String.format("🌍 Nazionalità: %s %s\n", getFlagEmoji(player.getPaese()), player.getPaese()));
        sb.append(String.format("🏆 Ranking: #%d\n", player.getRanking()));
        sb.append(String.format("📊 Punti: %d\n", player.getPunti()));
        if (player.getEta() > 0) {
            sb.append(String.format("🎂 Età: %d anni\n", player.getEta()));
        }
        return sb.toString();
    }

    private String getFlagEmoji(String country) {
        if (country == null || country.isEmpty() || country.equals("Unknown")) {
            return "🌍";
        }

        country = country.trim().toUpperCase();

        if (country.contains("/")) {
            String[] countries = country.split("/");
            String flag1 = getFlagEmoji(countries[0].trim());
            String flag2 = countries.length > 1 ? getFlagEmoji(countries[1].trim()) : "";
            return flag1 + (flag2.isEmpty() ? "" : " " + flag2);
        }

        return COUNTRY_FLAGS.getOrDefault(country, "🌍");
    }

    private void sendMessage(Long chatId, String text, boolean showKeyboard) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .build();

        if (showKeyboard) {
            message.setReplyMarkup(createKeyboard());
        }

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private ReplyKeyboardMarkup createKeyboard() {
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("🏆 Classifiche");
        row1.add("📅 Partite");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("🔍 Cerca");
        row2.add("⭐ Preferiti");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("📊 Statistiche");

        keyboardRows.add(row1);
        keyboardRows.add(row2);
        keyboardRows.add(row3);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .selective(true)
                .build();
    }
}