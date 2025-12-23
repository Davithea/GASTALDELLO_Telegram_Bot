package database;

import model.Match;
import model.Player;

import java.sql.*;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:tennis_bot.db";
    private Connection connection;

    public DatabaseManager() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            initializeDatabase();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Errore nella connessione al database");
        }
    }

    private void initializeDatabase() {
        try (Statement stmt = connection.createStatement()) {
            // Tabella utenti
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    chat_id INTEGER PRIMARY KEY,
                    username TEXT,
                    first_interaction TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    last_interaction TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    total_interactions INTEGER DEFAULT 0
                )
            """);

            // Tabella giocatori
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS players (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT UNIQUE NOT NULL,
                    country TEXT,
                    ranking INTEGER,
                    points INTEGER,
                    age INTEGER,
                    search_count INTEGER DEFAULT 0,
                    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Tabella partite
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS matches (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    tournament TEXT NOT NULL,
                    player1 TEXT NOT NULL,
                    player2 TEXT NOT NULL,
                    score TEXT,
                    match_date TEXT,
                    saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Tabella interazioni
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS interactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    chat_id INTEGER,
                    command TEXT,
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (chat_id) REFERENCES users(chat_id)
                )
            """);

            // NUOVA TABELLA: Giocatori preferiti
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

            // NUOVA TABELLA: Preferenze utente
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user_preferences (
                    chat_id INTEGER PRIMARY KEY,
                    favorite_tournament TEXT,
                    notification_enabled INTEGER DEFAULT 0,
                    language TEXT DEFAULT 'IT',
                    FOREIGN KEY (chat_id) REFERENCES users(chat_id)
                )
            """);

            System.out.println("✅ Database inizializzato correttamente");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveUser(Long chatId, String username) {
        String sql = """
            INSERT INTO users (chat_id, username, total_interactions) 
            VALUES (?, ?, 1)
            ON CONFLICT(chat_id) DO UPDATE SET 
                username = excluded.username,
                last_interaction = CURRENT_TIMESTAMP,
                total_interactions = total_interactions + 1
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, chatId);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void savePlayer(Player player) {
        String sql = """
            INSERT INTO players (name, country, ranking, points, age, search_count) 
            VALUES (?, ?, ?, ?, ?, 1)
            ON CONFLICT(name) DO UPDATE SET 
                country = excluded.country,
                ranking = excluded.ranking,
                points = excluded.points,
                age = excluded.age,
                search_count = search_count + 1,
                last_updated = CURRENT_TIMESTAMP
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, player.getNome());
            pstmt.setString(2, player.getPaese());
            pstmt.setInt(3, player.getRanking());
            pstmt.setInt(4, player.getPunti());
            pstmt.setInt(5, player.getEta());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void savePlayers(List<Player> players) {
        for (Player player : players) {
            savePlayer(player);
        }
    }

    public void saveMatches(List<Match> matches) {
        String sql = "INSERT INTO matches (tournament, player1, player2, score, match_date) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (Match match : matches) {
                pstmt.setString(1, match.getTournament());
                pstmt.setString(2, match.getPlayer1());
                pstmt.setString(3, match.getPlayer2());
                pstmt.setString(4, match.getScore());
                pstmt.setString(5, match.getDate());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void logInteraction(Long chatId, String command) {
        String sql = "INSERT INTO interactions (chat_id, command) VALUES (?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, chatId);
            pstmt.setString(2, command);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==================== PREFERITI ====================

    public String addFavoritePlayer(Long chatId, String playerName) {
        String sql = "INSERT INTO favorite_players (chat_id, player_name) VALUES (?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, chatId);
            pstmt.setString(2, playerName);
            pstmt.executeUpdate();
            return "⭐ " + playerName + " aggiunto ai preferiti!";
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                return "⚠️ " + playerName + " è già nei tuoi preferiti!";
            }
            e.printStackTrace();
            return "❌ Errore nell'aggiungere il giocatore.";
        }
    }

    public String removeFavoritePlayer(Long chatId, String playerName) {
        String sql = "DELETE FROM favorite_players WHERE chat_id = ? AND player_name = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, chatId);
            pstmt.setString(2, playerName);
            int deleted = pstmt.executeUpdate();

            if (deleted > 0) {
                return "➖ " + playerName + " rimosso dai preferiti.";
            } else {
                return "⚠️ " + playerName + " non è nei tuoi preferiti.";
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "❌ Errore nella rimozione.";
        }
    }

    public String getFavoritePlayers(Long chatId) {
        StringBuilder sb = new StringBuilder("⭐ I TUOI GIOCATORI PREFERITI\n\n");

        String sql = "SELECT player_name, added_at FROM favorite_players WHERE chat_id = ? ORDER BY added_at DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, chatId);
            ResultSet rs = pstmt.executeQuery();

            int count = 0;
            while (rs.next()) {
                count++;
                String name = rs.getString("player_name");
                String addedAt = rs.getString("added_at");

                sb.append(String.format("%d. %s\n", count, name));
                sb.append(String.format("   Aggiunto: %s\n\n", addedAt.substring(0, 10)));
            }

            if (count == 0) {
                return "⭐ NON HAI ANCORA GIOCATORI PREFERITI\n\n" +
                        "Aggiungi i tuoi giocatori preferiti con:\n" +
                        "/aggiungi [nome]\n\n" +
                        "Esempio: /aggiungi Sinner";
            }

            sb.append(String.format("Totale: %d giocatori\n\n", count));
            sb.append("➕ Aggiungi: /aggiungi [nome]\n");
            sb.append("➖ Rimuovi: /rimuovi [nome]");

        } catch (SQLException e) {
            e.printStackTrace();
            return "❌ Errore nel recupero dei preferiti.";
        }

        return sb.toString();
    }

    // ==================== STATISTICHE ====================

    public String getUserStatistics(Long chatId) {
        StringBuilder stats = new StringBuilder("📊 LE TUE STATISTICHE\n\n");

        try {
            // Statistiche utente
            String userSql = "SELECT username, first_interaction, total_interactions FROM users WHERE chat_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(userSql)) {
                pstmt.setLong(1, chatId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    String username = rs.getString("username");
                    String firstInteraction = rs.getString("first_interaction");
                    int totalInteractions = rs.getInt("total_interactions");

                    stats.append(String.format("👤 Utente: @%s\n", username != null ? username : "Unknown"));
                    stats.append(String.format("📅 Membro dal: %s\n", firstInteraction.substring(0, 10)));
                    stats.append(String.format("💬 Interazioni totali: %d\n\n", totalInteractions));
                }
            }

            // Comando più usato
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

            // Numero preferiti
            String favSql = "SELECT COUNT(*) as count FROM favorite_players WHERE chat_id = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(favSql)) {
                pstmt.setLong(1, chatId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    stats.append(String.format("⭐ Giocatori preferiti: %d\n\n", rs.getInt("count")));
                }
            }

            // Statistiche globali
            stats.append("🌍 STATISTICHE GLOBALI\n\n");

            String totalUsersSql = "SELECT COUNT(*) as count FROM users";
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery(totalUsersSql);
                if (rs.next()) {
                    stats.append(String.format("👥 Utenti totali: %d\n", rs.getInt("count")));
                }
            }

            String totalPlayersSql = "SELECT COUNT(*) as count FROM players";
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery(totalPlayersSql);
                if (rs.next()) {
                    stats.append(String.format("🎾 Giocatori nel database: %d\n", rs.getInt("count")));
                }
            }

            String totalMatchesSql = "SELECT COUNT(*) as count FROM matches";
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery(totalMatchesSql);
                if (rs.next()) {
                    stats.append(String.format("🏆 Partite salvate: %d\n", rs.getInt("count")));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "⚠️ Errore nel recupero delle statistiche.";
        }

        return stats.toString();
    }

    public String getTop10MostSearchedPlayers() {
        StringBuilder result = new StringBuilder("🔝 TOP 10 GIOCATORI PIÙ CERCATI\n\n");

        String sql = """
            SELECT name, country, search_count 
            FROM players 
            ORDER BY search_count DESC 
            LIMIT 10
        """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            int position = 1;
            while (rs.next()) {
                result.append(String.format("%d. %s (%s) - %d ricerche\n",
                        position++,
                        rs.getString("name"),
                        rs.getString("country"),
                        rs.getInt("search_count")));
            }

            if (position == 1) {
                return "❌ Nessun giocatore ancora cercato.";
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return "⚠️ Errore nel recupero dei dati.";
        }

        return result.toString();
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✅ Connessione database chiusa");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}