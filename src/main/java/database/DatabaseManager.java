package database;

import model.Player;
import java.sql.*;
import java.util.List;

//Classe DatabaseManager per la gestione del database mysqlite con le tabelle
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:tennis_bot.db";
    private Connection connection;

    //Costruttore che inizializza la connessione al database
    public DatabaseManager() {
        try {
            connection = DriverManager.getConnection(DB_URL);	//Apro la connessione al database SQLite
            initializeDatabase();	//Inizializzo le tabelle e la struttura del database
        } catch (SQLException e) {
            e.printStackTrace();	//Stampo lo stack trace per debug
            System.err.println("❌ Errore nella connessione al database");	//Segnalo errore di connessione
        }
    }

    private void initializeDatabase() {
        try (Statement stmt = connection.createStatement()) {
            //Tabella per la gestione degli utenti
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    chat_id INTEGER PRIMARY KEY,
                    username TEXT,
                    first_interaction TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_interaction TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    total_interactions INTEGER DEFAULT 0
                )
            """);

            //Tabella per il salvataggio dei giocatori cercati (tramite comandi /cerca e /classifica...)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS players (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL,
                    country TEXT,
                    ranking INTEGER,
                    points INTEGER,
                    age INTEGER,
                    altezza TEXT,
                    peso TEXT,
                    miglior_ranking TEXT,
                    vittorie_sconfitte TEXT,
                    titoli TEXT,
                    is_tennis_player INTEGER DEFAULT 0,
                    search_count INTEGER DEFAULT 0,
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            //Tabella per il logging delle interazioni dei vari utenti con il bot (comandi inseriti)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS interactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    chat_id INTEGER,
                    command TEXT,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (chat_id) REFERENCES users(chat_id)
                )
            """);

            //Tabella per il salvataggio dei giocatori preferiti riferiti a ciascun utente
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS favorite_players (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    chat_id INTEGER NOT NULL,
                    player_name TEXT NOT NULL,
                    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE(chat_id, player_name),
                    FOREIGN KEY (chat_id) REFERENCES users(chat_id)
                )
            """);

            System.out.println("✅ Database inizializzato correttamente");
            System.out.println("📍 Percorso: " + System.getProperty("user.dir") + "/tennis_bot.db");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //==================== USERS ====================
    //Metodo pubblico per salvare o aggiornare un utente nel database
    public void saveUser(Long chatId, String username) {
        //Query SQL con gestione conflitto su chat_id
        String sql = """
		INSERT INTO users (chat_id, username, total_interactions) 
		VALUES (?, ?, 1)
		ON CONFLICT(chat_id) DO UPDATE SET 
			username = excluded.username,
			last_interaction = CURRENT_TIMESTAMP,
			total_interactions = total_interactions + 1
	""";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {	//Preparo statement SQL
            pstmt.setLong(1, chatId);	//Imposto chat_id
            pstmt.setString(2, username);	//Imposto username
            pstmt.executeUpdate();	//Eseguo insert o update
        } catch (SQLException e) {
            e.printStackTrace();	//Stampo eventuale errore SQL
        }
    }

    //==================== PLAYERS ====================
    //Metodo pubblico per salvare o aggiornare i dati di un giocatore nel database
    public void savePlayer(Player player) {
        //Query SQL con gestione conflitto su name
        String sql = """
		INSERT INTO players (name, country, ranking, points, age, altezza, peso, 
							miglior_ranking, vittorie_sconfitte, titoli, is_tennis_player, search_count) 
		VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
		ON CONFLICT(name) DO UPDATE SET 
			country = excluded.country,
			ranking = excluded.ranking,
			points = excluded.points,
			age = excluded.age,
			altezza = excluded.altezza,
			peso = excluded.peso,
			miglior_ranking = excluded.miglior_ranking,
			vittorie_sconfitte = excluded.vittorie_sconfitte,
			titoli = excluded.titoli,
			is_tennis_player = excluded.is_tennis_player,
			search_count = search_count + 1,
			last_updated = CURRENT_TIMESTAMP
	""";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {	//Preparo statement SQL
            pstmt.setString(1, player.getNome());	//Imposto nome
            pstmt.setString(2, player.getPaese());	//Imposto paese
            pstmt.setInt(3, player.getRanking());	//Imposto ranking
            pstmt.setInt(4, player.getPunti());	//Imposto punti
            pstmt.setInt(5, player.getEta());	//Imposto età
            pstmt.setString(6, player.getAltezza());	//Imposto altezza
            pstmt.setString(7, player.getPeso());	//Imposto peso
            pstmt.setString(8, player.getMigliorRanking());	//Imposto miglior ranking
            pstmt.setString(9, player.getVittorieSconfitte());	//Imposto vittorie/sconfitte
            pstmt.setString(10, player.getTitoli());	//Imposto titoli
            pstmt.setInt(11, player.isTennisPlayer() ? 1 : 0);	//Imposto flag tennis player
            pstmt.executeUpdate();	//Eseguo insert o update
        } catch (SQLException e) {
            e.printStackTrace();	//Stampo eventuale errore SQL
        }
    }

    //Metodo pubblico per salvare o aggiornare nel database più giocatori (ad esempio quando uso il comando /classifica...)
    public void savePlayers(List<Player> players) {
        for (Player player : players) {	//Scorro la lista dei giocatori
            savePlayer(player);	//Salvo ogni singolo giocatore
        }
    }

    //==================== INTERACTIONS ====================
    //Metodo pubblico per registrare le interazioni di un utente nel database
    public void logInteraction(Long chatId, String command) {
        try (PreparedStatement pstmt = connection.prepareStatement(
                "INSERT INTO interactions (chat_id, command) VALUES (?, ?)")) {	//Preparo la query
            pstmt.setLong(1, chatId);	//Imposto l'ID chat
            pstmt.setString(2, command);	//Imposto il comando inviato
            pstmt.executeUpdate();	//Eseguo l'inserimento
        } catch (SQLException e) {	//Gestisco eventuali errori
            e.printStackTrace();	//Stampo lo stack trace
        }
    }

    // ==================== FAVOURITE_PLAYERS ====================
    //Metodo pubblico per aggiungere un giocatore ai preferiti di un utente
    public String addFavoritePlayer(Long chatId, String playerName) {
        String checkSql = "SELECT is_tennis_player, name, country, altezza, peso, miglior_ranking, vittorie_sconfitte, titoli FROM players WHERE name = ?";	//Controllo se il giocatore esiste e prendo le info
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setString(1, playerName);	//Imposto il nome del giocatore
            ResultSet rs = checkStmt.executeQuery();	//Eseguo la query
            if (!rs.next()) return "❌ Giocatore \"" + playerName + "\" non trovato nel database.\n\n💡 Prima cercalo con /cerca, poi aggiungilo ai preferiti!";	//Giocatore non trovato
            int isTennisPlayer = rs.getInt("is_tennis_player");	//Controllo se è un giocatore di tennis
            if (isTennisPlayer == 0) return "❌ \"" + playerName + "\" non è un giocatore di tennis.\n\n⚠️ Solo giocatori di tennis possono essere aggiunti ai preferiti!";	//Non è un giocatore valido
            String insertSql = "INSERT INTO favorite_players (chat_id, player_name) VALUES (?, ?)";	//Query per aggiungere ai preferiti
            try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                insertStmt.setLong(1, chatId);	//Imposto ID chat
                insertStmt.setString(2, playerName);	//Imposto nome giocatore
                insertStmt.executeUpdate();	//Eseguo inserimento
                StringBuilder info = new StringBuilder();	//Costruisco messaggio di conferma
                info.append("⭐ ").append(playerName).append(" aggiunto ai preferiti!\n\n📊 INFO GIOCATORE\n\n");
                String country = rs.getString("country");
                String altezza = rs.getString("altezza");
                String peso = rs.getString("peso");
                String migliorRanking = rs.getString("miglior_ranking");
                String vittorieSconfitte = rs.getString("vittorie_sconfitte");
                String titoli = rs.getString("titoli");
                if (country != null) info.append("🌍 Nazionalità: ").append(country).append("\n");
                if (altezza != null) info.append("📏 Altezza: ").append(altezza).append("\n");
                if (peso != null) info.append("⚖️ Peso: ").append(peso).append("\n");
                if (migliorRanking != null) info.append("⭐ Miglior ranking: ").append(migliorRanking).append("\n");
                if (vittorieSconfitte != null) info.append("📈 V/S: ").append(vittorieSconfitte).append("\n");
                if (titoli != null) info.append("🏅 Titoli: ").append(titoli).append("\n");
                return info.toString();	//Ritorno le informazioni del giocatore
            }
        } catch (SQLException e) {	//Gestisco errori SQL
            if (e.getMessage().contains("UNIQUE constraint failed")) return "⚠️ " + playerName + " è già nei tuoi preferiti!";	//Giocatore già presente
            e.printStackTrace();	//Errore generico
            return "❌ Errore nell'aggiungere il giocatore.";
        }
    }

    //Metodo pubblico per rimuovere un giocatore dai preferiti di un utente
    public String removeFavoritePlayer(Long chatId, String playerName) {
        String sql = "DELETE FROM favorite_players WHERE chat_id = ? AND player_name = ?";	//Query di cancellazione

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, chatId);	//Imposto ID chat
            pstmt.setString(2, playerName);	//Imposto nome giocatore
            int deleted = pstmt.executeUpdate();	//Eseguo cancellazione

            if (deleted > 0) return "➖ " + playerName + " rimosso dai preferiti.";	//Cancellazione avvenuta
            else return "⚠️ " + playerName + " non è nei tuoi preferiti.";	//Nessun record trovato
        } catch (SQLException e) {	//Gestisco errori SQL
            e.printStackTrace();
            return "❌ Errore nella rimozione.";
        }
    }

    //Metodo pubblico per recuperare la lista dei giocatori preferiti di un utente
    public String getFavoritePlayers(Long chatId) {
        StringBuilder sb = new StringBuilder("⭐ I TUOI GIOCATORI PREFERITI\n\n");	//Intestazione messaggio
        //Query per ottenere i preferiti e info giocatori
        String sql = """
		SELECT fp.player_name, fp.added_at, p.country, p.altezza, p.peso, 
		       p.miglior_ranking, p.vittorie_sconfitte, p.titoli
		FROM favorite_players fp
		LEFT JOIN players p ON fp.player_name = p.name
		WHERE fp.chat_id = ? 
		ORDER BY fp.added_at DESC
	""";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, chatId);	//Imposto ID chat
            ResultSet rs = pstmt.executeQuery();
            int count = 0;
            while (rs.next()) {	//Itero sui risultati
                count++;
                String name = rs.getString("player_name");	//Nome giocatore
                String addedAt = rs.getString("added_at");	//Data aggiunta
                String country = rs.getString("country");
                String altezza = rs.getString("altezza");
                String peso = rs.getString("peso");
                String migliorRanking = rs.getString("miglior_ranking");
                String vittorieSconfitte = rs.getString("vittorie_sconfitte");
                String titoli = rs.getString("titoli");
                sb.append(String.format("%d. %s\n", count, name));	//Nome con numero
                if (country != null) sb.append("   🌍 ").append(country).append("\n");	//Nazionalità
                if (altezza != null && peso != null) sb.append("   📏 ").append(altezza).append(", ⚖️ ").append(peso).append("\n");	//Altezza e peso
                if (migliorRanking != null) sb.append("   ⭐ Miglior ranking: ").append(migliorRanking).append("\n");	//Ranking
                if (vittorieSconfitte != null) sb.append("   📈 V/S: ").append(vittorieSconfitte).append("\n");	//Vittorie/Sconfitte
                if (titoli != null) sb.append("   🏅 Titoli: ").append(titoli).append("\n");	//Titoli
                sb.append("   📅 Aggiunto: ").append(addedAt.substring(0, 10)).append("\n\n");	//Data aggiunta
            }
            if (count == 0) return "⭐ NON HAI ANCORA GIOCATORI PREFERITI\n\n" +	//Messaggio se lista vuota
                    "Aggiungi i tuoi giocatori preferiti:\n" +
                    "1. Cerca un giocatore con /cerca\n" +
                    "2. Aggiungilo con /aggiungi\n\n" +
                    "Esempio:\n" +
                    "/cerca\n" +
                    "→ Jannik Sinner\n" +
                    "→ /aggiungi\n" +
                    "→ Jannik Sinner";
            sb.append(String.format("📊 Totale: %d giocatori\n\n", count));	//Totale giocatori
            sb.append("➕ Aggiungi: /aggiungi\n");	//Suggerimento aggiunta
            sb.append("➖ Rimuovi: /rimuovi");	//Suggerimento rimozione
        } catch (SQLException e) {	//Gestione errori SQL
            e.printStackTrace();
            return "❌ Errore nel recupero dei preferiti.";
        }
        return sb.toString();	//Restituisco messaggio completo
    }

    // ==================== STATISTICHE ====================
    //Metodo pubblico per recuperare le statistiche personali e globali dell'utente
    public String getUserStatistics(Long chatId) {
        StringBuilder stats = new StringBuilder("📊 LE TUE STATISTICHE\n\n");	//Intestazione messaggio
        try {
            String userSql = "SELECT username, first_interaction, total_interactions FROM users WHERE chat_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(userSql)) {
                pstmt.setLong(1, chatId);	//ID chat
                ResultSet rs = pstmt.executeQuery();    //Eseguo la query
                if (rs.next()) {    //Finché trovo un altro utente
                    String username = rs.getString("username");	//Nome utente
                    String firstInteraction = rs.getString("first_interaction");	//Data prima interazione
                    int totalInteractions = rs.getInt("total_interactions");	//Numero interazioni
                    stats.append(String.format("👤 Utente: @%s\n", username != null ? username : "Unknown"));
                    stats.append(String.format("📅 Membro dal: %s\n", firstInteraction.substring(0, 10)));
                    stats.append(String.format("💬 Interazioni totali: %d\n\n", totalInteractions));
                }
            }
            //Comando più utilizzato
            String commandSql = """
			SELECT command, COUNT(*) as count 
			FROM interactions 
			WHERE chat_id = ? 
			GROUP BY command 
			ORDER BY count DESC 
			LIMIT 1
		""";
            try (PreparedStatement pstmt = connection.prepareStatement(commandSql)) {
                pstmt.setLong(1, chatId);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    stats.append(String.format("⭐ Comando preferito: %s (%d volte)\n",
                            rs.getString("command"), rs.getInt("count")));
                }
            }
            //Conto quanti giocatori ho nei preferiti per questo utente
            String favSql = "SELECT COUNT(*) as count FROM favorite_players WHERE chat_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(favSql)) {
                pstmt.setLong(1, chatId); //Imposto l'ID della chat per filtrare i preferiti
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) stats.append(String.format("⭐ Giocatori preferiti: %d\n\n", rs.getInt("count"))); //Aggiungo al report il numero di preferiti
            }
            stats.append("🌍 STATISTICHE GLOBALI\n\n");  //Aggiungo le statistiche globali del database
            String totalUsersSql = "SELECT COUNT(*) as count FROM users";   //Conto il numero totale di utenti registrati
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery(totalUsersSql);
                if (rs.next())
                    stats.append(String.format("👥 Utenti totali: %d\n", rs.getInt("count"))); //Mostro quanti utenti sono nel DB
            }
            String totalPlayersSql = "SELECT COUNT(*) as count FROM players";   //Conto il numero totale di giocatori salvati
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery(totalPlayersSql);
                if (rs.next()) stats.append(String.format("🎾 Giocatori nel database: %d\n", rs.getInt("count"))); //Mostro quanti giocatori ho nel DB
            }
        } catch (SQLException e) { //Gestisco eventuali errori SQL
            e.printStackTrace();
            return "⚠️ Errore nel recupero delle statistiche."; //Restituisco messaggio di errore
        }
        return stats.toString(); //Restituisco tutta la stringa di statistiche completa
    }
}