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

//Classe BotTelegramGastaldello che gestisce il bot Telegram
public class BotTelegramGastaldello implements LongPollingSingleThreadUpdateConsumer {
    private final TelegramClient telegramClient; //Tengo il client Telegram per inviare e ricevere messaggi
    private final TennisService tennisService; //Tengo il servizio tennis per recuperare dati e risultati tramite web scraping
    private final DatabaseManager databaseManager; //Tengo il manager del database per salvare utenti e giocatori
    private final WeatherService weatherService; //Tengo il servizio meteo per fornire informazioni sul tempo tramite API
    private final Map<Long, String> userStates = new HashMap<>(); //Tengo lo stato corrente di ogni utente per gestire conversazioni
    private final Map<Long, String> h2hPlayer1 = new HashMap<>(); //Tengo il primo giocatore per il confronto testa a testa

    //Costruttore
    public BotTelegramGastaldello(String botToken, String apiKey) {
        this.telegramClient = new OkHttpTelegramClient(botToken); //Creo il client Telegram con il token del bot
        this.tennisService = new TennisService(); //Inizializzo il servizio tennis per recuperare dati e risultati
        this.weatherService = new WeatherService(apiKey); //Inizializzo il servizio meteo con la chiave API
        this.databaseManager = new DatabaseManager(); //Inizializzo il manager del database per gestire utenti e giocatori
        setupBotCommands(); //Configuro i comandi disponibili del bot
    }

    //Metodo privato per creare il menu dei comandi
    private void setupBotCommands() {
        List<BotCommand> commands = new ArrayList<>(); //Creo la lista dei comandi disponibili del bot
        commands.add(new BotCommand("start", "Avvia il bot")); //Aggiungo il comando start per avviare il bot
        commands.add(new BotCommand("classificaatp", "Top 10 ATP")); //Aggiungo il comando per la classifica ATP
        commands.add(new BotCommand("racetoturin", "Race to Turin ATP (annuale)")); //Aggiungo il comando per la Race to Turin
        commands.add(new BotCommand("classificaatpdoppio", "Top 10 ATP Doppio")); //Aggiungo il comando per la classifica ATP Doppio
        commands.add(new BotCommand("classificawta", "Top 10 WTA")); //Aggiungo il comando per la classifica WTA
        commands.add(new BotCommand("classificawtadoppio", "Top 10 WTA Doppio")); //Aggiungo il comando per la classifica WTA Doppio
        commands.add(new BotCommand("partite", "Partite di oggi")); //Aggiungo il comando per le partite del giorno
        commands.add(new BotCommand("cerca", "Cerca giocatore")); //Aggiungo il comando per cercare un giocatore
        commands.add(new BotCommand("h2h", "Head to Head tra giocatori")); //Aggiungo il comando per confrontare due giocatori
        commands.add(new BotCommand("meteo", "Meteo attuale di una città")); //Aggiungo il comando per ottenere il meteo
        commands.add(new BotCommand("preferiti", "I tuoi preferiti")); //Aggiungo il comando per vedere i preferiti
        commands.add(new BotCommand("statistiche", "Statistiche personali")); //Aggiungo il comando per le statistiche personali
        commands.add(new BotCommand("aiuto", "Mostra aiuto")); //Aggiungo il comando per visualizzare l'aiuto

        try {
            SetMyCommands setMyCommands = SetMyCommands.builder()
                    .commands(commands) //Imposto i comandi che ho appena creato
                    .scope(new BotCommandScopeDefault()) //Applico i comandi a tutti gli utenti
                    .build(); //Costruisco l'oggetto SetMyCommands
            telegramClient.execute(setMyCommands); //Invio la richiesta al bot per settare i comandi
            System.out.println("Menu comandi impostato"); //Stampo conferma sul terminale
        } catch (TelegramApiException e) {
            System.err.println("Errore impostazione menu: " + e.getMessage()); //Gestisco eventuali errori API
        }
    }

    //Metodo privato per creare una tastiera interattiva
    private ReplyKeyboardMarkup createKeyboard() {
        List<KeyboardRow> keyboardRows = new ArrayList<>();	//Creo lista di righe tastiera
        KeyboardRow row1 = new KeyboardRow();	//Prima riga
        row1.add("🏆 ATP");
        row1.add("👨👨 ATP");

        KeyboardRow row2 = new KeyboardRow();	//Seconda riga
        row2.add("👩 WTA");
        row2.add("👩👩 WTA");

        KeyboardRow row3 = new KeyboardRow();	//Terza riga
        row3.add("📅 PARTITE");
        row3.add("🔍 CERCA");

        KeyboardRow row4 = new KeyboardRow();	//Quarta riga
        row4.add("⚔️ H2H");
        row4.add("🌤 METEO");

        keyboardRows.add(row1);	//Aggiungo righe alla tastiera
        keyboardRows.add(row2);
        keyboardRows.add(row3);
        keyboardRows.add(row4);

        return ReplyKeyboardMarkup.builder()
                .keyboard(keyboardRows)
                .resizeKeyboard(true)
                .oneTimeKeyboard(false)
                .build();	//Costruisco tastiera finale
    }

    //Metodo pubblico consume obbligatorio da sovrascrivere per implementare l'interfaccia LongPollingSingleThreadUpdateConsumer
    @Override
    public void consume(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) { //Controllo se l'update contiene un messaggio di testo
            String messageText = update.getMessage().getText().trim(); //Prendo il testo del messaggio e tolgo spazi iniziali e finali
            Long chatId = update.getMessage().getChatId(); //Prendo l'ID della chat
            String username = update.getMessage().getFrom().getUserName(); //Prendo il nome utente del mittente
            databaseManager.saveUser(chatId, username); //Salvo o aggiorno l'utente nel database
            databaseManager.logInteraction(chatId, messageText); //Registro l'interazione nel database
            String response; //Dichiaro la variabile per la risposta da inviare
            String state = userStates.get(chatId); //Recupero lo stato della conversazione dell'utente

            if ("WAITING_PLAYER_NAME".equals(state)) { //Se sto aspettando il nome di un giocatore
                response = handlePlayerSearch(chatId, messageText); //Gestisco la ricerca del giocatore
            } else if ("WAITING_ADD_FAVORITE".equals(state)) { //Se sto aspettando il giocatore da aggiungere ai preferiti
                response = handleAddFavorite(chatId, messageText); //Gestisco l'aggiunta ai preferiti
            } else if ("WAITING_REMOVE_FAVORITE".equals(state)) { //Se sto aspettando il giocatore da rimuovere dai preferiti
                response = handleRemoveFavorite(chatId, messageText); //Gestisco la rimozione dai preferiti
            } else if ("WAITING_H2H_PLAYER1".equals(state)) { //Se sto aspettando il primo giocatore per H2H
                response = handleH2HPlayer1(chatId, messageText); //Gestisco il primo giocatore H2H
            } else if ("WAITING_H2H_PLAYER2".equals(state)) { //Se sto aspettando il secondo giocatore per H2H
                response = handleH2HPlayer2(chatId, messageText); //Gestisco il secondo giocatore H2H
            } else if ("WAITING_CITY_WEATHER".equals(state)) { //Se sto aspettando il nome di una città per il meteo
                response = handleWeather(chatId, messageText); //Gestisco la richiesta meteo
            } else { //Se non c'è uno stato specifico
                response = processCommand(messageText, chatId); //Processo il messaggio come comando generico
            }
            sendMessage(chatId, response, messageText.equals("/start")); //Invio la risposta all'utente
        }
    }

    private String processCommand(String command, Long chatId) {
        try {
            if (command.equals("/start")) { //Se il comando è /start
                return "🎾 Benvenuto nel Tennis Bot!\n\nSono il tuo assistente personale per il tennis.\n\n" +
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
                        "💡 Usa il menu in basso per i comandi rapidi!"; //Restituisco messaggio di benvenuto con comandi
            }

            if (command.equals("/aiuto") || command.equals("/help")) { //Se il comando è /aiuto o /help
                return processCommand("/start", chatId); //Chiamo il comando /start per mostrare guida
            }

            if (command.equals("/classificaatp") || command.equals("🏆 ATP")) { //Se comando classifica ATP singolare
                List<Player> rankings = tennisService.getATPRankings(10); //Prendo top 10 ATP
                databaseManager.savePlayers(rankings); //Salvo i giocatori nel database
                return formatRankings(rankings, "ATP"); //Restituisco testo formattato
            }

            if (command.equals("/racetoturin") || command.equals("🏁 RACE")) { //Se comando Race to Turin
                List<Player> rankings = tennisService.getRaceRankings(10); //Prendo top 10 Race
                databaseManager.savePlayers(rankings); //Salvo nel database
                return formatRankings(rankings, "RACE ATP"); //Restituisco classifica formattata
            }

            if (command.equals("/classificaatpdoppio") || command.equals("👨👨 ATP")) { //Se classifica ATP doppio
                List<Player> rankings = tennisService.getATPDoubleRankings(10); //Prendo top 10 doppio ATP
                databaseManager.savePlayers(rankings); //Salvo nel database
                return formatRankings(rankings, "DOPPIO ATP"); //Restituisco classifica
            }

            if (command.equals("/classificawta") || command.equals("👩 WTA")) { //Se classifica WTA singolare
                List<Player> rankings = tennisService.getWTARankings(10); //Prendo top 10 WTA
                databaseManager.savePlayers(rankings); //Salvo nel database
                return formatRankings(rankings, "WTA"); //Restituisco testo
            }

            if (command.equals("/classificawtadoppio") || command.equals("👩👩 WTA")) { //Se classifica WTA doppio
                List<Player> rankings = tennisService.getWTADoubleRankings(10); //Prendo top 10 doppio WTA
                databaseManager.savePlayers(rankings); //Salvo nel database
                return formatRankings(rankings, "WTA"); //Restituisco testo
            }

            if (command.equals("/partite") || command.equals("📅 PARTITE")) { //Se comando partite
                List<Match> matches = tennisService.getRecentMatches(); //Prendo partite recenti
                return formatMatches(matches); //Restituisco testo partite
            }

            if (command.equals("/cerca") || command.equals("🔍 CERCA")) { //Se comando cerca giocatore
                userStates.put(chatId, "WAITING_PLAYER_NAME"); //Imposto stato attesa nome giocatore
                return "🔍 RICERCA GIOCATORE\n\nScrivi il nome del giocatore da cercare.\n\nEsempi:\n• Jannik Sinner\n• Novak Djokovic\n• Carlos Alcaraz\n\nDigita /annulla per annullare."; //Restituisco istruzioni
            }

            if (command.equals("/h2h") || command.equals("⚔️ H2H")) { //Se comando head to head
                userStates.put(chatId, "WAITING_H2H_PLAYER1"); //Imposto stato attesa primo giocatore
                return "⚔️ HEAD TO HEAD\n\nScrivi il nome completo del PRIMO giocatore.\n\nEsempi:\n• Jannik Sinner\n• Novak Djokovic\n• Carlos Alcaraz\n\n⚠️ IMPORTANTE: Usa nome e cognome completi!\n\nDigita /annulla per annullare."; //Restituisco istruzioni
            }

            if (command.equals("/meteo") || command.equals("🌤 METEO")) { //Se comando meteo
                userStates.put(chatId, "WAITING_CITY_WEATHER"); //Imposto stato attesa città
                return "🌤 METEO\n\nScrivi il nome della città.\n\nEsempi:\n• Roma\n• Milano\n• London\n\nDigita /annulla per annullare."; //Restituisco istruzioni
            }

            if (command.equals("/preferiti") || command.equals("⭐ PREFERITI")) { //Se comando preferiti
                return databaseManager.getFavoritePlayers(chatId); //Recupero lista preferiti
            }

            if (command.equals("/aggiungi")) { //Se comando aggiungi preferito
                userStates.put(chatId, "WAITING_ADD_FAVORITE"); //Imposto stato attesa aggiunta
                return "➕ AGGIUNGI AI PREFERITI\n\nScrivi il nome del giocatore da aggiungere.\n\nEsempi:\n• Jannik Sinner\n• Novak Djokovic\n• Iga Swiatek\n\nDigita /annulla per annullare."; //Restituisco istruzioni
            }

            if (command.equals("/rimuovi")) { //Se comando rimuovi preferito
                userStates.put(chatId, "WAITING_REMOVE_FAVORITE"); //Imposto stato attesa rimozione
                return "➖ RIMUOVI DAI PREFERITI\n\nScrivi il nome del giocatore da rimuovere.\n\nDigita /annulla per annullare."; //Restituisco istruzioni
            }

            if (command.startsWith("/aggiungi ")) { //Se comando aggiungi con nome diretto
                String playerName = command.replace("/aggiungi ", "").trim(); //Estraggo nome giocatore
                if (playerName.isEmpty()) { //Se vuoto
                    return "⚠️ Usa: /aggiungi\nTi chiederò il nome dopo!"; //Messaggio errore
                }
                return databaseManager.addFavoritePlayer(chatId, playerName); //Aggiungo al database
            }

            if (command.startsWith("/rimuovi ")) { //Se comando rimuovi con nome diretto
                String playerName = command.replace("/rimuovi ", "").trim(); //Estraggo nome giocatore
                if (playerName.isEmpty()) { //Se vuoto
                    return "⚠️ Usa: /rimuovi\nTi chiederò il nome dopo!"; //Messaggio errore
                }
                return databaseManager.removeFavoritePlayer(chatId, playerName); //Rimuovo dal database
            }

            if (command.equals("/statistiche") || command.equals("📊 Stats")) { //Se comando statistiche
                return databaseManager.getUserStatistics(chatId); //Recupero statistiche utente
            }

            if (command.equals("/annulla")) { //Se comando annulla
                userStates.remove(chatId); //Resetto stato utente
                h2hPlayer1.remove(chatId); //Resetto eventuale H2H
                return "❌ Operazione annullata."; //Messaggio conferma annullamento
            }
            return "❓ Comando non riconosciuto.\nUsa /aiuto o il menu in basso."; //Messaggio per comando non valido
        } catch (Exception e) { //Gestione eccezioni generiche
            e.printStackTrace(); //Stampo stack trace
            return "⚠️ Si è verificato un errore. Riprova più tardi."; //Messaggio errore generico
        }
    }

    //==================== HANDLER STATO UTENTE ====================
    //Metodo privato per la gestione della ricerca del giocatore
    private String handlePlayerSearch(Long chatId, String playerName) {
        userStates.remove(chatId);	//Rimuovo lo stato dell'utente perché sto iniziando una nuova ricerca
        if (playerName.equalsIgnoreCase("/annulla")) {	//Controllo se l'utente vuole annullare la ricerca
            return "❌ Ricerca annullata.";	//Informo l'utente che ho annullato la ricerca
        }
        if (playerName.isEmpty() || playerName.length() < 2) {	//Verifico se il nome del giocatore è troppo corto o vuoto
            return "⚠️ Nome troppo corto. Riprova con /cerca";	//Avviso l'utente che deve inserire un nome più lungo
        }
        Player player = tennisService.searchPlayer(playerName);	//Cerco il giocatore usando il servizio tennisService
        if (player != null) {	//Se ho trovato il giocatore
            databaseManager.savePlayer(player);	//Salvo il giocatore nel database
            if (player.getExtraInfo() != null && !player.getExtraInfo().isEmpty()) {	//Se il giocatore ha informazioni extra
                String response = player.getExtraInfo();	//Uso le informazioni extra come risposta
                if (player.getImageUrl() != null && !player.getImageUrl().isEmpty()) {	//Se c'è un'immagine disponibile
                    sendPhoto(chatId, player.getImageUrl(), player.getNome());	//Invio l'immagine all'utente
                }
                response += "\n💡 Aggiungi ai preferiti con /aggiungi";	//Aggiungo suggerimento per aggiungere ai preferiti
                return response;	//Ritorno la risposta completa con info extra
            } else {	//Se non ci sono info extra
                return formatPlayerInfo(player) + "\n\n💡 Aggiungi ai preferiti con /aggiungi";	//Ritorno info base formattata con suggerimento
            }
        } else {	//Se il giocatore non viene trovato
            return "❌ Giocatore \"" + playerName + "\" non trovato.\n\n" +	//Informo l'utente che non ho trovato nulla
                    "💡 Suggerimenti:\n" +	//Fornisco alcuni suggerimenti per migliorare la ricerca
                    "• Scrivi nome e cognome (es: Jannik Sinner)\n" +	//Suggerisco di scrivere nome e cognome
                    "• Controlla lo spelling\n" +	//Suggerisco di controllare lo spelling
                    "• Prova solo il cognome (es: Sinner)\n\n" +	//Suggerisco di provare solo il cognome
                    "Riprova con /cerca";	//Indico all'utente di riprovare con il comando /cerca
        }
    }

    //Metodo privato per la gestione dell'aggiunta di un giocatore ai preferiti
    private String handleAddFavorite(Long chatId, String playerName) {
        userStates.remove(chatId);	//Rimuovo lo stato dell'utente perché sto iniziando l'aggiunta ai preferiti
        if (playerName.equalsIgnoreCase("/annulla")) {	//Controllo se l'utente vuole annullare l'operazione
            return "❌ Operazione annullata.";	//Informo l'utente che ho annullato l'aggiunta
        }
        if (playerName.isEmpty() || playerName.length() < 2) {	//Verifico se il nome del giocatore è vuoto o troppo corto
            return "⚠️ Nome troppo corto. Riprova con /aggiungi";	//Avviso l'utente di inserire un nome valido
        }
        return databaseManager.addFavoritePlayer(chatId, playerName);	//Aggiungo il giocatore ai preferiti e ritorno il risultato
    }

    //Metodo privato per la gestione della rimozione di un giocatore ai preferiti
    private String handleRemoveFavorite(Long chatId, String playerName) {
        userStates.remove(chatId);	//Rimuovo lo stato dell'utente perché sto iniziando la rimozione dai preferiti
        if (playerName.equalsIgnoreCase("/annulla")) {	//Controllo se l'utente vuole annullare l'operazione
            return "❌ Operazione annullata.";	//Informo l'utente che ho annullato la rimozione
        }
        if (playerName.isEmpty() || playerName.length() < 2) {	//Verifico se il nome del giocatore è vuoto o troppo corto
            return "⚠️ Nome troppo corto. Riprova con /rimuovi";	//Avviso l'utente di inserire un nome valido
        }
        return databaseManager.removeFavoritePlayer(chatId, playerName);	//Rimuovo il giocatore dai preferiti e ritorno il risultato
    }

    //Metodo privato per la gestione della richiesta del primo giocatore per l'H2H
    private String handleH2HPlayer1(Long chatId, String playerName) {
        if (playerName.equalsIgnoreCase("/annulla")) {	//Controllo se l'utente vuole annullare l'operazione H2H
            userStates.remove(chatId);	//Rimuovo lo stato dell'utente perché sto annullando
            return "❌ H2H annullato.";	//Informo l'utente che ho annullato la richiesta H2H
        }
        if (playerName.isEmpty() || playerName.length() < 2) {	//Verifico se il nome del giocatore è vuoto o troppo corto
            return "⚠️ Nome troppo corto. Riprova.";	//Avviso l'utente di inserire un nome valido
        }
        h2hPlayer1.put(chatId, playerName);	//Salvo il primo giocatore della sfida H2H per questo utente
        userStates.put(chatId, "WAITING_H2H_PLAYER2");	//Aggiorno lo stato dell'utente per aspettare il secondo giocatore
        return "⚔️ HEAD TO HEAD\n\n" +	//Creo il messaggio di conferma per l'utente
                "Primo giocatore: " + playerName + "\n\n" +	//Mostro il primo giocatore selezionato
                "Ora scrivi il nome del SECONDO giocatore.\n\n" +	//Istruisco l'utente su cosa fare dopo
                "Digita /annulla per annullare.";	//Ricordo come annullare l'operazione
    }

    //Metodo privato per la gestione della richiesta del secondo giocatore per l'H2H
    private String handleH2HPlayer2(Long chatId, String player2Name) {
        userStates.remove(chatId);	//Rimuovo lo stato dell'utente perché sto gestendo il secondo giocatore H2H
        if (player2Name.equalsIgnoreCase("/annulla")) {	//Controllo se l'utente vuole annullare l'H2H
            h2hPlayer1.remove(chatId);	//Rimuovo anche il primo giocatore salvato perché annullo l'operazione
            return "❌ H2H annullato.";	//Informo l'utente che l'H2H è stato annullato
        }
        String player1Name = h2hPlayer1.remove(chatId);	//Recupero e rimuovo il primo giocatore salvato
        if (player1Name == null) {	//Se non c'è un primo giocatore registrato
            return "⚠️ Errore. Riprova con /h2h";	//Avviso l'utente che qualcosa è andato storto
        }
        if (player2Name.isEmpty() || player2Name.length() < 2) {	//Verifico se il nome del secondo giocatore è troppo corto o vuoto
            return "⚠️ Nome troppo corto. Riprova con /h2h";	//Avviso l'utente di inserire un nome valido
        }
        H2HData h2hData = tennisService.getH2HData(player1Name, player2Name);	//Richiedo i dati H2H completi dai servizi tennisService
        if (h2hData != null) {	//Se i dati H2H sono stati trovati
            if (h2hData.getPlayer1Image() != null && !h2hData.getPlayer1Image().isEmpty()) {	//Se il primo giocatore ha un'immagine
                sendPhoto(chatId, h2hData.getPlayer1Image(), h2hData.getPlayer1Name());	//Invio la foto del primo giocatore
            }
            if (h2hData.getPlayer2Image() != null && !h2hData.getPlayer2Image().isEmpty()) {	//Se il secondo giocatore ha un'immagine
                sendPhoto(chatId, h2hData.getPlayer2Image(), h2hData.getPlayer2Name());	//Invio la foto del secondo giocatore
            }
            return getH2H(player1Name, player2Name);	//Formato e ritorno i dati H2H tra i due giocatori
        } else {	//Se i dati H2H non sono disponibili
            return "❌ Impossibile recuperare H2H tra " + player1Name + " e " + player2Name + ".\n\n" +	//Informo l'utente che non posso recuperare i dati
                    "Possibili cause:\n" +	//Fornisco alcune possibili cause del problema
                    "• Errore di connessione\n" +	//Problemi di connessione
                    "• Nomi non corretti (es. usa 'Jannik Sinner' invece di 'Sinner')\n" +	//Errore nello spelling o nome incompleto
                    "• Sito non raggiungibile\n\n" +	//Sito web da cui prendo i dati non disponibile
                    "💡 Suggerimenti:\n" +	//Suggerisco all'utente come risolvere
                    "• Usa nome e cognome completi\n" +	//Suggerisco di usare nome completo
                    "• Controlla lo spelling\n" +	//Suggerisco di controllare lo spelling
                    "• Verifica che i giocatori esistano\n\n" +	//Suggerisco di verificare che i giocatori siano reali
                    "Riprova con /h2h";	//Invito l'utente a riprovare con il comando /h2h
        }
    }

    //Metodo privato per ottenere le informazioni meteo su una città
    private String handleWeather(Long chatId, String city) {
        userStates.remove(chatId);	//Rimuovo stato utente
        if (city.equalsIgnoreCase("/annulla")) {	//Controllo annulla
            return "❌ Meteo annullato.";	//Avviso annullamento
        }
        if (city.isEmpty() || city.length() < 2) {	//Verifico nome città
            return "⚠️ Nome città non valido.\nRiprova con /meteo";	//Avviso nome non valido
        }
        return weatherService.getCurrentWeather(city);	//Richiedo meteo attuale
    }

    //==================== FORMATTATORI ====================
    //Metodo pubblico per ottenere il riepilogo H2H in formato testuale
    public String getH2H(String player1, String player2) {
        H2HData data = tennisService.getH2HData(player1, player2);	//Richiamo il metodo che recupera i dati H2H completi
        if (data == null) {	//Se non sono riuscito a recuperare i dati
            return "❌ Impossibile recuperare i dati H2H.\n\n" +	//Ritorno un messaggio di errore dettagliato
                    "Possibili cause:\n" +
                    "• Errore di connessione al sito\n" +
                    "• Nomi non corretti\n" +
                    "• Sito non raggiungibile\n\n" +
                    "💡 Verifica lo spelling dei nomi e riprova.";
        }
        //Anche se non abbiamo trovato tutti i dati, mostriamo quello che abbiamo
        return formatH2HData(data);	//Ritorno i dati H2H formattati tramite il metodo helper
    }

    //Metodo privato per formattare i dati dell'H2H e ritornare una stringa leggibile per l'utente
    private String formatH2HData(H2HData data) {
        StringBuilder sb = new StringBuilder();	//Creo un StringBuilder per costruire il messaggio H2H
        sb.append("⚔️ HEAD TO HEAD ⚔️\n\n");	//Aggiungo intestazione H2H
        sb.append(String.format("👤 %s vs %s\n\n",
                data.getPlayer1Name() != null ? data.getPlayer1Name() : "Player 1",
                data.getPlayer2Name() != null ? data.getPlayer2Name() : "Player 2"));	//Aggiungo i nomi dei giocatori, con fallback
        if (data.getTotalH2HMatches() > 0 || data.getH2hRecord() != null) {	//Verifico se ci sono dati H2H disponibili
            sb.append("📊 SCONTRI DIRETTI\n");	//Aggiungo titolo per gli scontri diretti
            if (data.getTotalH2HMatches() > 0) {	//Se ci sono partite totali H2H
                sb.append(String.format("Totale partite: %d\n", data.getTotalH2HMatches()));	//Mostro il totale partite
            }
            if (data.getH2hRecord() != null && !data.getH2hRecord().isEmpty()) {	//Se c'è il record H2H
                sb.append(String.format("Record: %s\n", data.getH2hRecord()));	//Mostro il record H2H
            }
            sb.append("\n");	//Aggiungo una riga vuota per separare
        }
        boolean hasCareerStats = false;	//Flag per capire se ci sono statistiche di carriera
        StringBuilder careerStats = new StringBuilder();	//Creo un StringBuilder per le statistiche di carriera
        if (data.getPlayer1PrizeMoney() != null || data.getPlayer2PrizeMoney() != null) {	//Se ci sono prize money
            careerStats.append(String.format("💰 Prize Money:\n"));	//Aggiungo titolo per i prize money
            if (data.getPlayer1PrizeMoney() != null) {	//Se il player1 ha prize money
                careerStats.append(String.format("   %s: %s\n",
                        data.getPlayer1Name() != null ? data.getPlayer1Name() : "Player 1",
                        data.getPlayer1PrizeMoney()));	//Mostro prize money del player1
            }
            if (data.getPlayer2PrizeMoney() != null) {	//Se il player2 ha prize money
                careerStats.append(String.format("   %s: %s\n",
                        data.getPlayer2Name() != null ? data.getPlayer2Name() : "Player 2",
                        data.getPlayer2PrizeMoney()));	//Mostro prize money del player2
            }
            careerStats.append("\n");	//Riga vuota dopo i prize money
            hasCareerStats = true;	//Segno che ci sono statistiche di carriera
        }
        if (data.getPlayer1WinLoss() != null || data.getPlayer2WinLoss() != null) {	//Se ci sono dati Win/Loss
            careerStats.append(String.format("📈 Win/Loss Totale:\n"));	//Aggiungo titolo Win/Loss
            if (data.getPlayer1WinLoss() != null) {	//Se player1 ha Win/Loss
                careerStats.append(String.format("   %s: %s (%s)\n",
                        data.getPlayer1Name() != null ? data.getPlayer1Name() : "Player 1",
                        data.getPlayer1WinLoss(),
                        data.getPlayer1WinPercentage() != null ? data.getPlayer1WinPercentage() : "N/A"));	//Mostro Win/Loss e percentuale player1
            }
            if (data.getPlayer2WinLoss() != null) {	//Se player2 ha Win/Loss
                careerStats.append(String.format("   %s: %s (%s)\n",
                        data.getPlayer2Name() != null ? data.getPlayer2Name() : "Player 2",
                        data.getPlayer2WinLoss(),
                        data.getPlayer2WinPercentage() != null ? data.getPlayer2WinPercentage() : "N/A"));	//Mostro Win/Loss e percentuale player2
            }
            careerStats.append("\n");	//Riga vuota dopo Win/Loss
            hasCareerStats = true;	//Segno che ci sono statistiche di carriera
        }
        if (data.getPlayer1Titles() > 0 || data.getPlayer2Titles() > 0) {	//Se ci sono titoli vinti
            careerStats.append(String.format("🏅 Titoli:\n"));	//Aggiungo titolo per i titoli
            careerStats.append(String.format("   %s: %d\n",
                    data.getPlayer1Name() != null ? data.getPlayer1Name() : "Player 1",
                    data.getPlayer1Titles()));	//Mostro titoli player1
            careerStats.append(String.format("   %s: %d\n",
                    data.getPlayer2Name() != null ? data.getPlayer2Name() : "Player 2",
                    data.getPlayer2Titles()));	//Mostro titoli player2
            careerStats.append("\n");	//Riga vuota dopo titoli
            hasCareerStats = true;	//Segno che ci sono statistiche di carriera
        }
        if (hasCareerStats) {	//Se ci sono statistiche di carriera
            sb.append("🏆 STATISTICHE CARRIERA\n\n");	//Aggiungo intestazione
            sb.append(careerStats);	//Aggiungo tutte le statistiche di carriera
        }
        if (data.getPlayer1Clay() > 0 || data.getPlayer2Clay() > 0 ||
                data.getPlayer1Hard() > 0 || data.getPlayer2Hard() > 0 ||
                data.getPlayer1Indoor() > 0 || data.getPlayer2Indoor() > 0) {	//Se ci sono vittorie per superficie
            sb.append("🎾 VITTORIE PER SUPERFICIE\n\n");	//Intestazione superficie
            if (data.getPlayer1Clay() > 0 || data.getPlayer2Clay() > 0) {	//Se ci sono vittorie su Clay
                sb.append(String.format("🟤 Clay:\n"));	//Aggiungo titolo Clay
                sb.append(String.format("   %s: %d\n",
                        data.getPlayer1Name() != null ? data.getPlayer1Name() : "Player 1",
                        data.getPlayer1Clay()));	//Mostro vittorie player1 Clay
                sb.append(String.format("   %s: %d\n\n",
                        data.getPlayer2Name() != null ? data.getPlayer2Name() : "Player 2",
                        data.getPlayer2Clay()));	//Mostro vittorie player2 Clay
            }
            if (data.getPlayer1Grass() > 0 || data.getPlayer2Grass() > 0) {	//Se ci sono vittorie su Grass
                sb.append(String.format("🟢 Grass:\n"));	//Titolo Grass
                sb.append(String.format("   %s: %d\n",
                        data.getPlayer1Name() != null ? data.getPlayer1Name() : "Player 1",
                        data.getPlayer1Grass()));	//Mostro vittorie player1 Grass
                sb.append(String.format("   %s: %d\n\n",
                        data.getPlayer2Name() != null ? data.getPlayer2Name() : "Player 2",
                        data.getPlayer2Grass()));	//Mostro vittorie player2 Grass
            }
            if (data.getPlayer1Hard() > 0 || data.getPlayer2Hard() > 0) {	//Se ci sono vittorie su Hard
                sb.append(String.format("🔵 Hard:\n"));	//Titolo Hard
                sb.append(String.format("   %s: %d\n",
                        data.getPlayer1Name() != null ? data.getPlayer1Name() : "Player 1",
                        data.getPlayer1Hard()));	//Mostro vittorie player1 Hard
                sb.append(String.format("   %s: %d\n\n",
                        data.getPlayer2Name() != null ? data.getPlayer2Name() : "Player 2",
                        data.getPlayer2Hard()));	//Mostro vittorie player2 Hard
            }
            if (data.getPlayer1Indoor() > 0 || data.getPlayer2Indoor() > 0) {	//Se ci sono vittorie su Indoor
                sb.append(String.format("🏠 Indoor:\n"));	//Titolo Indoor
                sb.append(String.format("   %s: %d\n",
                        data.getPlayer1Name() != null ? data.getPlayer1Name() : "Player 1",
                        data.getPlayer1Indoor()));	//Mostro vittorie player1 Indoor
                sb.append(String.format("   %s: %d\n\n",
                        data.getPlayer2Name() != null ? data.getPlayer2Name() : "Player 2",
                        data.getPlayer2Indoor()));	//Mostro vittorie player2 Indoor
            }
        }
        sb.append("📊 Fonte: matchstat.com");	//Aggiungo fonte dati
        return sb.toString();	//Ritorno il messaggio formattato
    }

    //Metodo privato per formattare i ranking in una stringa leggibile per l'utente
    private String formatRankings(List<Player> rankings, String type) {
        if (rankings.isEmpty()) {	//Se non ci sono dati di classifica
            return "⚠️ CLASSIFICA " + type + " NON DISPONIBILE\n\n" +
                    "Impossibile recuperare i dati.\n" +
                    "Riprova tra qualche minuto.";	//Avviso l'utente
        }
        StringBuilder sb = new StringBuilder("🏆 TOP 10 " + type + "\n\n");	//Creo intestazione classifica
        for (Player player : rankings) {	//Per ogni giocatore nella classifica
            sb.append(String.format("%d. %s\n",
                    player.getRanking(),
                    player.getNome()));	//Mostro posizione e nome
            sb.append(String.format("   Punti: %d\n\n", player.getPunti()));	//Mostro punti
        }
        sb.append("📅 ").append(new java.util.Date());	//Aggiungo data dell'ultimo aggiornamento
        return sb.toString();	//Ritorno il messaggio formattato
    }

    //Metodo privato per formattare la visualizzazione delle partite in una stringa leggibile per l'utente
    private String formatMatches(List<Match> matches) {
        if (matches.isEmpty()) {	//Se non ci sono partite
            return "ℹ️ NESSUNA PARTITA LIVE\n\n" +
                    "Non ci sono partite in corso.\n\n" +
                    "Le partite live sono disponibili durante:\n" +
                    "🏆 Grand Slam\n" +
                    "🥇 Masters 1000\n" +
                    "🥈 ATP/WTA Tour\n\n" +
                    "Riprova più tardi!";	//Avviso l'utente
        }
        StringBuilder sb = new StringBuilder("🎾 PARTITE DI OGGI\n\n");	//Intestazione partite
        String currentTournament = "";	//Variabile per tracciare torneo corrente
        String currentLocation = "";	//Variabile per tracciare location corrente
        for (Match match : matches) {	//Per ogni partita
            if (!match.getTournament().equals(currentTournament) ||
                    !match.getLocation().equals(currentLocation)) {	//Se cambia torneo o location
                currentTournament = match.getTournament();	//Aggiorno torneo corrente
                currentLocation = match.getLocation();	//Aggiorno location corrente
                String emoji = getTournamentEmoji(currentTournament);	//Ottengo emoji torneo
                sb.append(String.format("\n%s %s\n", emoji, currentTournament));	//Stampo torneo con emoji
                if (currentLocation != null && !currentLocation.isEmpty()) {	//Se location disponibile
                    sb.append(String.format("📍 %s\n", currentLocation));	//Stampo location
                }
                sb.append("─────────────────────\n");	//Linea separatrice
            }
            if (match.isFinished()) {	//Se partita finita
                sb.append(String.format("👤 %s vs %s\n", match.getPlayer1(), match.getPlayer2()));	//Stampo giocatori
                if (match.getDetailedScore() != null && !match.getDetailedScore().isEmpty()) {	//Se c'è punteggio dettagliato
                    sb.append(String.format("📊 Punteggio: %s\n", match.getDetailedScore()));	//Stampo punteggio
                }
                if (match.getWinner() != null && match.getSetScore() != null) {	//Se vincitore e set disponibili
                    String winner = match.getWinner();
                    String loser = winner.equals(match.getPlayer1()) ? match.getPlayer2() : match.getPlayer1();
                    sb.append(String.format("🏆 %s b. %s %s\n", winner, loser, match.getSetScore()));	//Stampo risultato
                }
                if (match.isTavolino())	//Se partita a tavolino
                    sb.append("🩼 A TAVOLINO\n");	//Indico partita a tavolino
            } else if (match.isLive()) {	//Se partita live
                sb.append(String.format("👤 %s vs %s\n", match.getPlayer1(), match.getPlayer2()));	//Stampo giocatori
                if (match.getCurrentGame() != null && !match.getCurrentGame().isEmpty()) {	//Se game corrente disponibile
                    sb.append(String.format("🔴 LIVE - Game: %s\n", match.getCurrentGame()));	//Stampo game corrente
                } else {
                    sb.append("🔴 LIVE\n");	//Altrimenti solo LIVE
                }
                if (match.getDetailedScore() != null && !match.getDetailedScore().isEmpty()) {	//Se set disponibili
                    sb.append(String.format("📊 Set: %s\n", match.getDetailedScore()));	//Stampo set
                }
            } else if (match.isAnnullata()) {	//Se partita annullata
                sb.append(String.format("👤 %s vs %s\n", match.getPlayer1(), match.getPlayer2()));	//Stampo giocatori
                sb.append(String.format("⏰ %s\n",
                        (match.getDate() != null ? match.getDate() : "Orario sconosiuto")));	//Stampo orario
                sb.append("🚫 ANNULLATA\n");	//Indico annullamento
            } else {	//Se partita non iniziata
                sb.append(String.format("👤 %s vs %s\n", match.getPlayer1(), match.getPlayer2()));	//Stampo giocatori
                sb.append(String.format("⏰ %s\n",
                        (match.getDate() != null ? match.getDate() : "Non iniziata")));	//Stampo orario
                sb.append("🟢 IN PROGRAMMA\n");	//Indico partita in programma
            }
            sb.append("\n");	//Riga vuota tra partite
        }
        sb.append("📅 Ultimo aggiornamento: ")
                .append(new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date()));	//Aggiungo orario ultimo aggiornamento
        return sb.toString();	//Ritorno testo formattato
    }

    //Metodo helper per associare un emoji a una specifica tipologia di torneo
    private String getTournamentEmoji(String tournament) {
        String lower = tournament.toLowerCase();	//Converto il nome in minuscolo
        if (lower.contains("grande slam") || lower.contains("roland garros") || lower.contains("australian open") ||
                lower.contains("wimbledon") || lower.contains("us open")) {	//Se torneo Grand Slam
            return "🏆";	//Emoji Grand Slam
        } else if (lower.contains("masters")) {	//Se Masters
            return "🥇";	//Emoji Masters
        } else if (lower.contains("500")) {	//Se torneo 500
            return "🥈";	//Emoji 500
        }
        return "🎾";	//Emoji default
    }

    //Metodo privato per formattare le informazioni di un giocatore in una stringa leggibile per l'utente
    private String formatPlayerInfo(Player player) {
        StringBuilder sb = new StringBuilder();	//Creo StringBuilder
        sb.append(String.format("🎾 %s\n\n", player.getNome()));	//Stampo nome giocatore
        sb.append(String.format("🏆 Ranking: #%d\n", player.getRanking()));	//Stampo ranking
        sb.append(String.format("📊 Punti: %d\n", player.getPunti()));	//Stampo punti
        if (player.getEta() > 0) {	//Se età valida
            sb.append(String.format("🎂 Età: %d anni\n", player.getEta()));	//Stampo età
        }
        return sb.toString();	//Ritorno stringa formattata
    }

    //Metodo per inviare un messaggio nella chat telegram spezzandolo eventualmente se è troppo lungo
    private void sendMessage(Long chatId, String text, boolean showKeyboard) {
        int MAX_LENGTH = 4000; //Definisco lunghezza massima messaggio
        for (int i = 0; i < text.length(); i += MAX_LENGTH) {	//Divido messaggio in più parti se necessario
            String part = text.substring(i, Math.min(text.length(), i + MAX_LENGTH));	//Estraggo parte del messaggio
            SendMessage message = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(part)
                    .build();	//Costruisco messaggio
            if (showKeyboard && i == 0) {	//Mostro tastiera solo nel primo messaggio
                message.setReplyMarkup(createKeyboard());	//Imposto tastiera
            }
            try {
                telegramClient.execute(message);	//Invio messaggio
            } catch (TelegramApiException e) {
                e.printStackTrace();	//Gestione errore
            }
        }
    }

    //Metodo privato per inviare nel bot telegram una foto
    private void sendPhoto(Long chatId, String photoUrl, String caption) {
        try {
            org.telegram.telegrambots.meta.api.methods.send.SendPhoto sendPhoto =
                    org.telegram.telegrambots.meta.api.methods.send.SendPhoto.builder()
                            .chatId(chatId.toString())
                            .photo(new org.telegram.telegrambots.meta.api.objects.InputFile(photoUrl))
                            .caption(caption)
                            .build();	//Costruisco messaggio foto
            telegramClient.execute(sendPhoto);	//Invio foto
        } catch (TelegramApiException e) {
            System.out.println("⚠️ Impossibile inviare foto: " + e.getMessage());	//Errore invio foto
        }
    }
}