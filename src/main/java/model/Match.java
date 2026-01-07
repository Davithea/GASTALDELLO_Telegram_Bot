package model;

public class Match {
    private String tournament;
    private String player1;
    private String player2;
    private String score;          // Informazioni generali o orario
    private String date;           // Ora del match
    private int priority;          // Per eventuale ordinamento
    private String winner;         // Vincitore
    private String detailedScore;  // Punteggio set dettagliato ("6-4 6-3")
    private String setScore;       // Punteggio set totale ("2-0")
    private String status;         // LIVE, FINE, A tavolino, ecc.

    public Match(String tournament, String player1, String player2,
                 String score, String date, int priority) {
        this.tournament = tournament;
        this.player1 = player1;
        this.player2 = player2;
        this.score = score;
        this.date = date;
        this.priority = priority;
        this.winner = null;
        this.detailedScore = "";
        this.setScore = null;
        this.status = ""; // Inizialmente vuoto
    }

    // ────────── GETTER ──────────
    public String getTournament() { return tournament; }
    public String getPlayer1() { return player1; }
    public String getPlayer2() { return player2; }
    public String getScore() { return score; }
    public String getDate() { return date; }
    public int getPriority() { return priority; }
    public String getWinner() { return winner; }
    public String getDetailedScore() { return detailedScore; }
    public String getSetScore() { return setScore; }
    public String getStatus() { return status; }

    // ────────── SETTER ──────────
    public void setTournament(String tournament) { this.tournament = tournament; }
    public void setPlayer1(String player1) { this.player1 = player1; }
    public void setPlayer2(String player2) { this.player2 = player2; }
    public void setScore(String score) { this.score = score; }
    public void setDate(String date) { this.date = date; }
    public void setPriority(int priority) { this.priority = priority; }
    public void setWinner(String winner) { this.winner = winner; }
    public void setDetailedScore(String detailedScore) { this.detailedScore = detailedScore; }
    public void setSetScore(String setScore) { this.setScore = setScore; }
    public void setStatus(String status) { this.status = status; }

    // ────────── METODI UTILI ──────────
    public boolean isFinished() {
        return status != null && (status.equals("FINE") || status.equals("A tavolino"));
    }

    public boolean isLive() {
        return status != null && status.equals("LIVE");
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

        sb.append(String.format(" (%s)", date));
        sb.append(String.format(" [%s]", status)); // Mostra lo status chiaramente

        return sb.toString();
    }
}
