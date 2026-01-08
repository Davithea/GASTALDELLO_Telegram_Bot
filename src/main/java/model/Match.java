package model;

public class Match {
    private String tournament;
    private String player1;
    private String player2;
    private String score;          // Informazioni generali o orario
    private String date;           // Ora del match
    private String winner;         // Vincitore
    private String detailedScore;  // Punteggio set dettagliato ("6-4 6-3")
    private String setScore;       // Punteggio set totale ("2-0")
    private String status;         // LIVE, FINE, A tavolino, ecc.
    private String location;
    private String currentGame;    // Punteggio game corrente per partite LIVE (es. "30-15")
    private int priority;

    public Match(String tournament, String location,
                 String player1, String player2,
                 String score, String date, int priority) {

        this.tournament = tournament;
        this.location = location;
        this.player1 = player1;
        this.player2 = player2;
        this.score = score;
        this.date = date;
        this.priority = priority;
        this.status = "";
        this.currentGame = "";
    }

    // ────────── GETTER ──────────
    public String getTournament() { return tournament; }
    public String getPlayer1() { return player1; }
    public String getPlayer2() { return player2; }
    public String getScore() { return score; }
    public String getDate() { return date; }
    public String getWinner() { return winner; }
    public String getDetailedScore() { return detailedScore; }
    public String getSetScore() { return setScore; }
    public String getLocation() { return location; }
    public String getCurrentGame() { return currentGame; }

    // ────────── SETTER ──────────
    public void setWinner(String winner) { this.winner = winner; }
    public void setDetailedScore(String detailedScore) { this.detailedScore = detailedScore; }
    public void setSetScore(String setScore) { this.setScore = setScore; }
    public void setStatus(String status) { this.status = status; }
    public void setCurrentGame(String currentGame) { this.currentGame = currentGame; }

    // ────────── METODI UTILI ──────────
    public boolean isFinished() {
        return status != null && (status.equals("FINE") || status.equals("A tavolino"));
    }

    public boolean isAnnullata() {
        return status != null && status.equals("Annullata");
    }

    public boolean isLive() {
        return status != null && (status.equals("LIVE") ||  status.matches("[1-5]º set"));
    }

    public boolean isTavolino() {
        return status != null && status.equals("A tavolino");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s: %s vs %s", tournament, player1, player2));

        if (winner != null && !winner.isEmpty()) {
            sb.append(String.format(" - 🏆 %s", winner));
        }

        if (detailedScore != null && !detailedScore.isEmpty()) {
            sb.append(String.format(" [%s]", detailedScore));
        } else {
            sb.append(String.format(" - %s", score));
        }

        if (currentGame != null && !currentGame.isEmpty()) {
            sb.append(String.format(" (Game: %s)", currentGame));
        }

        sb.append(String.format(" - %s", date));
        sb.append(String.format(" [%s]", status));

        return sb.toString();
    }
}