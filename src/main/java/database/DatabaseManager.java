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

            // Tabella giocatori - AGGIORNATA con più campi
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

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, player.getNome());
            pstmt.setString(2, player.getPaese());
            pstmt.setInt(3, player.getRanking());
            pstmt.setInt(4, player.getPunti());
            pstmt.setInt(5, player.getEta());
            pstmt.setString(6, player.getAltezza());
            pstmt.setString(7, player.getPeso());
            pstmt.setString(8, player.getMigliorRanking());
            pstmt.setString(9, player.getVittorieSconfitte());
            pstmt.setString(10, player.getTitoli());
            pstmt.setInt(11, player.isTennisPlayer() ? 1 : 0);
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
        // Verifica prima se il giocatore esiste ed è un tennista
        String checkSql = "SELECT is_tennis_player, name, country, altezza, peso, miglior_ranking, vittorie_sconfitte, titoli FROM players WHERE name = ?";

        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setString(1, playerName);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                return "❌ Giocatore \"" + playerName + "\" non trovato nel database.\n\n" +
                        "💡 Prima cercalo con /cerca, poi aggiungilo ai preferiti!";
            }

            int isTennisPlayer = rs.getInt("is_tennis_player");
            if (isTennisPlayer == 0) {
                return "❌ \"" + playerName + "\" non è un giocatore di tennis.\n\n" +
                        "⚠️ Solo giocatori di tennis possono essere aggiunti ai preferiti!";
            }

            // Giocatore valido, aggiungilo ai preferiti
            String insertSql = "INSERT INTO favorite_players (chat_id, player_name) VALUES (?, ?)";

            try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                insertStmt.setLong(1, chatId);
                insertStmt.setString(2, playerName);
                insertStmt.executeUpdate();

                // Crea messaggio con info giocatore
                StringBuilder info = new StringBuilder();
                info.append("⭐ ").append(playerName).append(" aggiunto ai preferiti!\n\n");
                info.append("📊 INFO GIOCATORE\n\n");

                String country = rs.getString("country");
                String altezza = rs.getString("altezza");
                String peso = rs.getString("peso");
                String migliorRanking = rs.getString("miglior_ranking");
                String vittorieSconfitte = rs.getString("vittorie_sconfitte");
                String titoli = rs.getString("titoli");

                if (country != null) info.append("🌍 Nazionalità: ").append(country).append("\n");
                if (altezza != null) info.append("📏 Altezza: ").append(altezza).append(" cm\n");
                if (peso != null) info.append("⚖️ Peso: ").append(peso).append(" kg\n");
                if (migliorRanking != null) info.append("⭐ Miglior ranking: ").append(migliorRanking).append("\n");
                if (vittorieSconfitte != null) info.append("📈 V/S: ").append(vittorieSconfitte).append("\n");
                if (titoli != null) info.append("🏅 Titoli: ").append(titoli).append("\n");

                return info.toString();
            }

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

        String sql = """
            SELECT fp.player_name, fp.added_at, p.country, p.altezza, p.peso, 
                   p.miglior_ranking, p.vittorie_sconfitte, p.titoli
            FROM favorite_players fp
            LEFT JOIN players p ON fp.player_name = p.name
            WHERE fp.chat_id = ? 
            ORDER BY fp.added_at DESC
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, chatId);
            ResultSet rs = pstmt.executeQuery();

            int count = 0;
            while (rs.next()) {
                count++;
                String name = rs.getString("player_name");
                String addedAt = rs.getString("added_at");
                String country = rs.getString("country");
                String altezza = rs.getString("altezza");
                String peso = rs.getString("peso");
                String migliorRanking = rs.getString("miglior_ranking");
                String vittorieSconfitte = rs.getString("vittorie_sconfitte");
                String titoli = rs.getString("titoli");

                sb.append(String.format("%d. %s\n", count, name));
                if (country != null) sb.append("   🌍 ").append(country).append("\n");
                if (altezza != null && peso != null) {
                    sb.append("   📏 ").append(altezza).append(" cm, ⚖️ ").append(peso).append(" kg\n");
                }
                if (migliorRanking != null) sb.append("   ⭐ Miglior ranking: ").append(migliorRanking).append("\n");
                if (vittorieSconfitte != null) sb.append("   📈 V/S: ").append(vittorieSconfitte).append("\n");
                if (titoli != null) sb.append("   🏅 Titoli: ").append(titoli).append("\n");
                sb.append("   📅 Aggiunto: ").append(addedAt.substring(0, 10)).append("\n\n");
            }

            if (count == 0) {
                return "⭐ NON HAI ANCORA GIOCATORI PREFERITI\n\n" +
                        "Aggiungi i tuoi giocatori preferiti:\n" +
                        "1. Cerca un giocatore con /cerca\n" +
                        "2. Aggiungilo con /aggiungi\n\n" +
                        "Esempio:\n" +
                        "/cerca\n" +
                        "→ Jannik Sinner\n" +
                        "→ /aggiungi\n" +
                        "→ Jannik Sinner";
            }

            sb.append(String.format("📊 Totale: %d giocatori\n\n", count));
            sb.append("➕ Aggiungi: /aggiungi\n");
            sb.append("➖ Rimuovi: /rimuovi");

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