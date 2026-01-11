package bot;

import database.DatabaseManager;
import model.H2HData;
import model.Match;
import model.Player;
import API.WeatherService;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
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
    private final WeatherService weatherService;

    // Stati conversazione
    private final Map<Long, String> userStates = new HashMap<>();
    private final Map<Long, String> h2hPlayer1 = new HashMap<>();

    public BotTelegramGastaldello(String botToken, String apiKey) {
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.tennisService = new TennisService();
        this.weatherService = new WeatherService(apiKey);
        this.databaseManager = new DatabaseManager();

        // Imposta menu comandi
        setupBotCommands();
    }

    /**
     * Imposta il menu dei comandi visibile in tutti i client Telegram
     */
    private void setupBotCommands() {
        List<BotCommand> commands = new ArrayList<>();
        commands.add(new BotCommand("start", "Avvia il bot"));
        commands.add(new BotCommand("classificaatp", "Top 10 ATP"));
        commands.add(new BotCommand("racetoturin", "Race to Turin ATP (annuale)"));
        commands.add(new BotCommand("classificaatpdoppio", "Top 10 ATP Doppio"));
        commands.add(new BotCommand("classificawta", "Top 10 WTA"));
        commands.add(new BotCommand("classificawtadoppio", "Top 10 WTA Doppio"));
        commands.add(new BotCommand("partite", "Partite di oggi"));
        commands.add(new BotCommand("cerca", "Cerca giocatore"));
        commands.add(new BotCommand("h2h", "Head to Head tra giocatori"));
        commands.add(new BotCommand("meteo", "Meteo attuale di una città"));
        commands.add(new BotCommand("preferiti", "I tuoi preferiti"));
        commands.add(new BotCommand("statistiche", "Statistiche personali"));
        commands.add(new BotCommand("aiuto", "Mostra aiuto"));

        try {
            SetMyCommands setMyCommands = SetMyCommands.builder()
                    .commands(commands)
                    .scope(new BotCommandScopeDefault())
                    .build();

            telegramClient.execute(setMyCommands);
            System.out.println("✅ Menu comandi impostato");
        } catch (TelegramApiException e) {
            System.err.println("❌ Errore impostazione menu: " + e.getMessage());
        }
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

            // Gestione stati conversazionali
            String state = userStates.get(chatId);

            if ("WAITING_PLAYER_NAME".equals(state)) {
                response = handlePlayerSearch(chatId, messageText);
            } else if ("WAITING_ADD_FAVORITE".equals(state)) {
                response = handleAddFavorite(chatId, messageText);
            } else if ("WAITING_REMOVE_FAVORITE".equals(state)) {
                response = handleRemoveFavorite(chatId, messageText);
            } else if ("WAITING_H2H_PLAYER1".equals(state)) {
                response = handleH2HPlayer1(chatId, messageText);
            } else if ("WAITING_H2H_PLAYER2".equals(state)) {
                response = handleH2HPlayer2(chatId, messageText);
            } else if ("WAITING_CITY_WEATHER".equals(state)) {
                response = handleWeather(chatId, messageText);
            } else {
                response = processCommand(messageText, chatId);
            }

            sendLongMessage(chatId, response, messageText.equals("/start"));
        }
    }

    private String processCommand(String command, Long chatId) {
        try {
            if (command.equals("/start")) {
                return "🎾 Benvenuto nel Tennis Bot!\n\n" +
                        "Sono il tuo assistente personale per il tennis.\n\n" +
                        "Comandi disponibili:\n" +
                        " 🏆  /classificaatp - Top 10 ATP\n" +
                        " 🏁  /racetoturin - Top 10 Race\n" +
                        "👨👨 /classificaatpdoppio - Top 10 ATP doppio\n" +
                        " 👩  /classificawta - Top 10 WTA\n" +
                        "👩👩 /classificawtadoppio - Top 10 WTA doppio\n" +
                        " 📅  /partite - Partite di oggi\n" +
                        " 🔍  /cerca - Cerca giocatore\n" +
                        " ⚔️  /h2h - Confronta due giocatori\n" +
                        " ⛅  /meteo - Trova il meteo delle città dove si svolgono i tornei\n" +
                        " ⭐  /preferiti - I tuoi preferiti\n" +
                        " ➕  /aggiungi - Aggiungi preferito\n" +
                        " ➖  /rimuovi - Rimuovi preferito\n" +
                        " 📊  /statistiche - Le tue statistiche\n" +
                        " ❓  /aiuto - Mostra questo messaggio\n\n" +
                        "💡 Usa il menu in basso per i comandi rapidi!";
            }

            if (command.equals("/aiuto") || command.equals("/help")) {
                return processCommand("/start", chatId);
            }

            // CLASSIFICHE
            if (command.equals("/classificaatp") || command.equals("🏆 ATP")) {
                List<Player> rankings = tennisService.getATPRankings(10);
                databaseManager.savePlayers(rankings);
                return formatRankings(rankings, "ATP");
            }

            if (command.equals("/racetoturin") || command.equals("🏁 RACE")) {
                List<Player> rankings = tennisService.getRaceRankings(10);
                databaseManager.savePlayers(rankings);
                return formatRankings(rankings, "RACE ATP");
            }

            if (command.equals("/classificaatpdoppio") || command.equals("👨👨 ATP")) {
                List<Player> rankings = tennisService.getATPDoubleRankings(10);
                databaseManager.savePlayers(rankings);
                return formatRankings(rankings, "DOPPIO ATP");
            }

            if (command.equals("/classificawta") || command.equals("👩 WTA")) {
                List<Player> rankings = tennisService.getWTARankings(10);
                databaseManager.savePlayers(rankings);
                return formatRankings(rankings, "WTA");
            }

            if (command.equals("/classificawtadoppio") || command.equals("👩👩 WTA")) {
                List<Player> rankings = tennisService.getWTADoubleRankings(10);
                databaseManager.savePlayers(rankings);
                return formatRankings(rankings, "WTA");
            }

            // PARTITE LIVE
            if (command.equals("/partite") || command.equals("📅 PARTITE")) {
                List<Match> matches = tennisService.getRecentMatches();
                return formatMatches(matches);
            }

            // CERCA INTERATTIVO
            if (command.equals("/cerca") || command.equals("🔍 CERCA")) {
                userStates.put(chatId, "WAITING_PLAYER_NAME");
                return "🔍 RICERCA GIOCATORE\n\n" +
                        "Scrivi il nome del giocatore da cercare.\n\n" +
                        "Esempi:\n" +
                        "• Jannik Sinner\n" +
                        "• Novak Djokovic\n" +
                        "• Carlos Alcaraz\n\n" +
                        "Digita /annulla per annullare.";
            }

            // Cerca questa sezione nel metodo processCommand e decommentala:
            if (command.equals("/h2h") || command.equals("⚔️ H2H")) {
                userStates.put(chatId, "WAITING_H2H_PLAYER1");
                return "⚔️ HEAD TO HEAD\n\n" +
                        "Scrivi il nome completo del PRIMO giocatore.\n\n" +
                        "Esempi:\n" +
                        "• Jannik Sinner\n" +
                        "• Novak Djokovic\n" +
                        "• Carlos Alcaraz\n\n" +
                        "⚠️ IMPORTANTE: Usa nome e cognome completi!\n\n" +
                        "Digita /annulla per annullare.";
            }

            if (command.equals("/meteo") || command.equals("🌤 METEO")) {
                userStates.put(chatId, "WAITING_CITY_WEATHER");
                return "🌤 METEO\n\n" +
                        "Scrivi il nome della città.\n\n" +
                        "Esempi:\n" +
                        "• Roma\n" +
                        "• Milano\n" +
                        "• London\n\n" +
                        "Digita /annulla per annullare.";
            }

            // PREFERITI INTERATTIVI
            if (command.equals("/preferiti") || command.equals("⭐ PREFERITI")) {
                return databaseManager.getFavoritePlayers(chatId);
            }

            // AGGIUNGI - INTERATTIVO
            if (command.equals("/aggiungi")) {
                userStates.put(chatId, "WAITING_ADD_FAVORITE");
                return "➕ AGGIUNGI AI PREFERITI\n\n" +
                        "Scrivi il nome del giocatore da aggiungere.\n\n" +
                        "Esempi:\n" +
                        "• Jannik Sinner\n" +
                        "• Novak Djokovic\n" +
                        "• Iga Swiatek\n\n" +
                        "Digita /annulla per annullare.";
            }

            // RIMUOVI - INTERATTIVO
            if (command.equals("/rimuovi")) {
                userStates.put(chatId, "WAITING_REMOVE_FAVORITE");
                return "➖ RIMUOVI DAI PREFERITI\n\n" +
                        "Scrivi il nome del giocatore da rimuovere.\n\n" +
                        "Digita /annulla per annullare.";
            }

            // VECCHI COMANDI (retrocompatibilità)
            if (command.startsWith("/aggiungi ")) {
                String playerName = command.replace("/aggiungi ", "").trim();
                if (playerName.isEmpty()) {
                    return "⚠️ Usa: /aggiungi\nTi chiederò il nome dopo!";
                }
                return databaseManager.addFavoritePlayer(chatId, playerName);
            }

            if (command.startsWith("/rimuovi ")) {
                String playerName = command.replace("/rimuovi ", "").trim();
                if (playerName.isEmpty()) {
                    return "⚠️ Usa: /rimuovi\nTi chiederò il nome dopo!";
                }
                return databaseManager.removeFavoritePlayer(chatId, playerName);
            }

            // STATISTICHE
            if (command.equals("/statistiche") || command.equals("📊 Stats")) {
                return databaseManager.getUserStatistics(chatId);
            }

            // ANNULLA
            if (command.equals("/annulla")) {
                userStates.remove(chatId);
                h2hPlayer1.remove(chatId);
                return "❌ Operazione annullata.";
            }

            return "❓ Comando non riconosciuto.\nUsa /aiuto o il menu in basso.";

        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ Si è verificato un errore. Riprova più tardi.";
        }
    }

// ==================== GESTORI STATI ====================

    private String handlePlayerSearch(Long chatId, String playerName) {
        userStates.remove(chatId);

        if (playerName.equalsIgnoreCase("/annulla")) {
            return "❌ Ricerca annullata.";
        }

        if (playerName.isEmpty() || playerName.length() < 2) {
            return "⚠️ Nome troppo corto. Riprova con /cerca";
        }

        Player player = tennisService.searchPlayerAPI(playerName);

        if (player != null) {
            databaseManager.savePlayer(player);

            // Se ha info estese da Wikipedia, usale
            if (player.getExtraInfo() != null && !player.getExtraInfo().isEmpty()) {
                String response = player.getExtraInfo();

                // Se c'è immagine, invia separatamente
                if (player.getImageUrl() != null && !player.getImageUrl().isEmpty()) {
                    sendPhoto(chatId, player.getImageUrl(), player.getNome());
                }

                response += "\n💡 Aggiungi ai preferiti con /aggiungi";
                return response;
            } else {
                // Info base (fallback)
                return formatPlayerInfo(player) + "\n\n💡 Aggiungi ai preferiti con /aggiungi";
            }
        } else {
            return "❌ Giocatore \"" + playerName + "\" non trovato.\n\n" +
                    "💡 Suggerimenti:\n" +
                    "• Scrivi nome e cognome (es: Jannik Sinner)\n" +
                    "• Controlla lo spelling\n" +
                    "• Prova solo il cognome (es: Sinner)\n\n" +
                    "Riprova con /cerca";
        }
    }

    private String handleAddFavorite(Long chatId, String playerName) {
        userStates.remove(chatId);

        if (playerName.equalsIgnoreCase("/annulla")) {
            return "❌ Operazione annullata.";
        }

        if (playerName.isEmpty() || playerName.length() < 2) {
            return "⚠️ Nome troppo corto. Riprova con /aggiungi";
        }

        return databaseManager.addFavoritePlayer(chatId, playerName);
    }

    private String handleRemoveFavorite(Long chatId, String playerName) {
        userStates.remove(chatId);

        if (playerName.equalsIgnoreCase("/annulla")) {
            return "❌ Operazione annullata.";
        }

        if (playerName.isEmpty() || playerName.length() < 2) {
            return "⚠️ Nome troppo corto. Riprova con /rimuovi";
        }

        return databaseManager.removeFavoritePlayer(chatId, playerName);
    }

    private String handleH2HPlayer1(Long chatId, String playerName) {
        if (playerName.equalsIgnoreCase("/annulla")) {
            userStates.remove(chatId);
            return "❌ H2H annullato.";
        }

        if (playerName.isEmpty() || playerName.length() < 2) {
            return "⚠️ Nome troppo corto. Riprova.";
        }

        h2hPlayer1.put(chatId, playerName);
        userStates.put(chatId, "WAITING_H2H_PLAYER2");

        return "⚔️ HEAD TO HEAD\n\n" +
                "Primo giocatore: " + playerName + "\n\n" +
                "Ora scrivi il nome del SECONDO giocatore.\n\n" +
                "Digita /annulla per annullare.";
    }

// Sostituisci il metodo handleH2HPlayer2 nel BotTelegramGastaldello con questo:

    private String handleH2HPlayer2(Long chatId, String player2Name) {
        userStates.remove(chatId);

        if (player2Name.equalsIgnoreCase("/annulla")) {
            h2hPlayer1.remove(chatId);
            return "❌ H2H annullato.";
        }

        String player1Name = h2hPlayer1.remove(chatId);

        if (player1Name == null) {
            return "⚠️ Errore. Riprova con /h2h";
        }

        if (player2Name.isEmpty() || player2Name.length() < 2) {
            return "⚠️ Nome troppo corto. Riprova con /h2h";
        }

        // Ottieni i dati H2H completi
        H2HData h2hData = tennisService.getH2HData(player1Name, player2Name);

        if (h2hData != null) {
            // Invia le foto dei giocatori se disponibili
            if (h2hData.getPlayer1Image() != null && !h2hData.getPlayer1Image().isEmpty()) {
                sendPhoto(chatId, h2hData.getPlayer1Image(), h2hData.getPlayer1Name());
            }

            if (h2hData.getPlayer2Image() != null && !h2hData.getPlayer2Image().isEmpty()) {
                sendPhoto(chatId, h2hData.getPlayer2Image(), h2hData.getPlayer2Name());
            }

            // Formatta e restituisci i dati H2H
            return tennisService.getH2H(player1Name, player2Name);
        } else {
            return "❌ Impossibile recuperare H2H tra " + player1Name + " e " + player2Name + ".\n\n" +
                    "Possibili cause:\n" +
                    "• Errore di connessione\n" +
                    "• Nomi non corretti (es. usa 'Jannik Sinner' invece di 'Sinner')\n" +
                    "• Sito non raggiungibile\n\n" +
                    "💡 Suggerimenti:\n" +
                    "• Usa nome e cognome completi\n" +
                    "• Controlla lo spelling\n" +
                    "• Verifica che i giocatori esistano\n\n" +
                    "Riprova con /h2h";
        }
    }


// ==================== FORMATTATORI ====================

    private String formatRankings(List<Player> rankings, String type) {
        if (rankings.isEmpty()) {
            return "⚠️ CLASSIFICA " + type + " NON DISPONIBILE\n\n" +
                    "Impossibile recuperare i dati.\n" +
                    "Riprova tra qualche minuto.";
        }

        StringBuilder sb = new StringBuilder("🏆 TOP 10 " + type + "\n\n");
        for (Player player : rankings) {
            sb.append(String.format("%d. %s\n",
                    player.getRanking(),
                    player.getNome()));
            sb.append(String.format("   Punti: %d\n\n", player.getPunti()));
        }
        sb.append("📅 ").append(new java.util.Date());
        return sb.toString();
    }

// Sostituisci il metodo formatMatches nel BotTelegramGastaldello

    // Sostituisci il metodo formatMatches nel BotTelegramGastaldello

// Sostituisci il metodo formatMatches con questa versione corretta:

    private String formatMatches(List<Match> matches) {
        if (matches.isEmpty()) {
            return "ℹ️ NESSUNA PARTITA LIVE\n\n" +
                    "Non ci sono partite in corso.\n\n" +
                    "Le partite live sono disponibili durante:\n" +
                    "🏆 Grand Slam\n" +
                    "🥇 Masters 1000\n" +
                    "🥈 ATP/WTA Tour\n\n" +
                    "Riprova più tardi!";
        }

        StringBuilder sb = new StringBuilder("🎾 PARTITE DI OGGI\n\n");
        String currentTournament = "";
        String currentLocation = "";

        for (Match match : matches) {
            // ✅ FIX 1: Mostra il torneo E il luogo ogni volta che cambiano
            if (!match.getTournament().equals(currentTournament) ||
                    !match.getLocation().equals(currentLocation)) {

                currentTournament = match.getTournament();
                currentLocation = match.getLocation();

                String emoji = getTournamentEmoji(currentTournament);
                sb.append(String.format("\n%s %s\n", emoji, currentTournament));

                // ✅ FIX: Stampa sempre il luogo se presente
                if (currentLocation != null && !currentLocation.isEmpty()) {
                    sb.append(String.format("📍 %s\n", currentLocation));
                }

                sb.append("─────────────────────\n");
            }

            if (match.isFinished()) {
                // Partita FINITA
                sb.append(String.format("👤 %s vs %s\n", match.getPlayer1(), match.getPlayer2()));

                // Punteggio dettagliato
                if (match.getDetailedScore() != null && !match.getDetailedScore().isEmpty()) {
                    sb.append(String.format("📊 Punteggio: %s\n", match.getDetailedScore()));
                }

                // Vincitore con set vinti
                if (match.getWinner() != null && match.getSetScore() != null) {
                    String winner = match.getWinner();
                    String loser = winner.equals(match.getPlayer1()) ? match.getPlayer2() : match.getPlayer1();
                    sb.append(String.format("🏆 %s b. %s %s\n", winner, loser, match.getSetScore()));
                }

                if (match.isTavolino())
                    sb.append("🩼 A TAVOLINO\n");
            } else if (match.isLive()) {
                // Partita LIVE
                sb.append(String.format("👤 %s vs %s\n", match.getPlayer1(), match.getPlayer2()));

                // ✅ FIX 2: Mostra il punteggio del game corrente
                if (match.getCurrentGame() != null && !match.getCurrentGame().isEmpty()) {
                    sb.append(String.format("🔴 LIVE - Game: %s\n", match.getCurrentGame()));
                } else {
                    sb.append("🔴 LIVE\n");
                }
                // Punteggio dei set
                if (match.getDetailedScore() != null && !match.getDetailedScore().isEmpty()) {
                    sb.append(String.format("📊 Set: %s\n", match.getDetailedScore()));
                }
            } else if (match.isAnnullata()) {
                // Partita Annullata
                sb.append(String.format("👤 %s vs %s\n", match.getPlayer1(), match.getPlayer2()));
                sb.append(String.format("⏰ %s\n",
                        (match.getDate() != null ? match.getDate() : "Orario sconosiuto")));
                sb.append("🚫 ANNULLATA\n");
            } else {
                // Partita NON INIZIATA
                sb.append(String.format("👤 %s vs %s\n", match.getPlayer1(), match.getPlayer2()));
                sb.append(String.format("⏰ %s\n",
                        (match.getDate() != null ? match.getDate() : "Non iniziata")));
                sb.append("🟢 IN PROGRAMMA\n");
            }

            sb.append("\n");
        }

        sb.append("📅 Ultimo aggiornamento: ")
                .append(new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date()));

        return sb.toString();
    }

    private String getTournamentEmoji(String tournament) {
        String lower = tournament.toLowerCase();
        if (lower.contains("australian open") || lower.contains("roland garros") ||
                lower.contains("wimbledon") || lower.contains("us open")) {
            return "🏆";
        } else if (lower.contains("masters")) {
            return "🥇";
        } else if (lower.contains("500")) {
            return "🥈";
        }
        return "🎾";
    }

    private String formatPlayerInfo(Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("🎾 %s\n\n", player.getNome()));
        sb.append(String.format("🏆 Ranking: #%d\n", player.getRanking()));
        sb.append(String.format("📊 Punti: %d\n", player.getPunti()));
        if (player.getEta() > 0) {
            sb.append(String.format("🎂 Età: %d anni\n", player.getEta()));
        }
        return sb.toString();
    }

    private String handleWeather(Long chatId, String city) {
        userStates.remove(chatId);

        if (city.equalsIgnoreCase("/annulla")) {
            return "❌ Meteo annullato.";
        }

        if (city.isEmpty() || city.length() < 2) {
            return "⚠️ Nome città non valido.\nRiprova con /meteo";
        }

        return weatherService.getCurrentWeather(city);
    }

    private void sendLongMessage(Long chatId, String text, boolean showKeyboard) {
        int MAX_LENGTH = 4000; // margine di sicurezza

        for (int i = 0; i < text.length(); i += MAX_LENGTH) {
            String part = text.substring(i, Math.min(text.length(), i + MAX_LENGTH));

            SendMessage message = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(part)
                    .build();

            // Mostra la tastiera SOLO nel primo messaggio
            if (showKeyboard && i == 0) {
                message.setReplyMarkup(createKeyboard());
            }

            try {
                telegramClient.execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
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

    private void sendPhoto(Long chatId, String photoUrl, String caption) {
        try {
            org.telegram.telegrambots.meta.api.methods.send.SendPhoto sendPhoto =
                    org.telegram.telegrambots.meta.api.methods.send.SendPhoto.builder()
                            .chatId(chatId.toString())
                            .photo(new org.telegram.telegrambots.meta.api.objects.InputFile(photoUrl))
                            .caption(caption)
                            .build();

            telegramClient.execute(sendPhoto);
        } catch (TelegramApiException e) {
            System.out.println("⚠️ Impossibile inviare foto: " + e.getMessage());
            // Non blocca l'esecuzione, continua senza foto
        }
    }

    private ReplyKeyboardMarkup createKeyboard() {
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("🏆 ATP");
        row1.add("👨👨 ATP");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("👩 WTA");
        row2.add("👩👩 WTA");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("📅 PARTITE");
        row3.add("🔍 CERCA");

        KeyboardRow row4 = new KeyboardRow();
        row4.add("⚔️ H2H");
        row4.add("🌤 METEO");

        keyboardRows.add(row1);
        keyboardRows.add(row2);
        keyboardRows.add(row3);
        keyboardRows.add(row4);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .build();
    }
}