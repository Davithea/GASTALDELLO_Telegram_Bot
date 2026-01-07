package model;

import java.util.List;

public class Match {
    private String tournament;
    private String player1;
    private String player2;
    private String score;  // es. LIVE, FINE, punteggio
    private String date;   // ora del match
    private int priority;  // per ordinamento eventuale

    public Match(String tournament, String player1, String player2,
                     String score, String date, int priority) {
        this.tournament = tournament;
        this.player1 = player1;
        this.player2 = player2;
        this.score = score;
        this.date = date;
        this.priority = priority;
    }

    public String getTournament() { return tournament; }
    public String getPlayer1() { return player1; }
    public String getPlayer2() { return player2; }
    public String getScore() { return score; }
    public String getDate() { return date; }
    public int getPriority() { return priority; }

    public void setTournament(String tournament) { this.tournament = tournament; }
    public void setPlayer1(String player1) { this.player1 = player1; }
    public void setPlayer2(String player2) { this.player2 = player2; }
    public void setScore(String score) { this.score = score; }
    public void setDate(String date) { this.date = date; }
    public void setPriority(int priority) { this.priority = priority; }

    @Override
    public String toString() {
        return String.format("%s: %s vs %s - %s (%s)",
                tournament, player1, player2, score, date);
    }
}
