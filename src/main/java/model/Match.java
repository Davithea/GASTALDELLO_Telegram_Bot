package model;

//Classe Match per la rappresentazione di una partita di Tennis
public class Match {
    private String tournament;	//Memorizzo il torneo
    private String player1;	//Memorizzo il primo giocatore
    private String player2;	//Memorizzo il secondo giocatore
    private String score;	//Memorizzo informazioni generali o orario
    private String date;	//Memorizzo la data/ora del match
    private String winner;	//Memorizzo il vincitore
    private String detailedScore;	//Memorizzo il punteggio dettagliato dei set
    private String setScore;	//Memorizzo il punteggio totale dei set
    private String status;	//Memorizzo lo stato del match (LIVE, FINE, A tavolino, ecc.)
    private String location;	//Memorizzo la location
    private String currentGame;	//Memorizzo il punteggio corrente del game per match LIVE
    private int priority;	//Memorizzo la priorità del match

    //Costruttore principale della classe
    public Match(String tournament, String location,
                 String player1, String player2,
                 String score, String date, int priority) {
        this.tournament = tournament;	//Inizializzo il torneo
        this.location = location;	//Inizializzo la location
        this.player1 = player1;	//Inizializzo player1
        this.player2 = player2;	//Inizializzo player2
        this.score = score;	//Inizializzo il punteggio generale
        this.date = date;	//Inizializzo la data
        this.priority = priority;	//Inizializzo la priorità
        this.status = "";	//Inizializzo lo stato vuoto
        this.currentGame = "";	//Inizializzo il game corrente vuoto
    }

    //────────── GETTER ──────────
    public String getTournament() { return tournament; }
    public String getPlayer1() { return player1; }
    public String getPlayer2() { return player2; }
    public String getScore() { return score; }
    public String getDate() { return date; }
    public String getWinner() { return winner; }
    public String getDetailedScore() { return detailedScore; }
    public String getSetScore() { return setScore; }
    public String getStatus() { return status; }
    public String getLocation() { return location; }
    public String getCurrentGame() { return currentGame; }
    public int getPriority() { return priority; }

    //────────── SETTER ──────────
    public void setTournament(String tournament) {this.tournament = tournament; }
    public void setPlayer1(String player1) {this.player1 = player1;}
    public void setPlayer2(String player2) {this.player2 = player2;}
    public void setScore(String score) {this.score = score;}
    public void setDate(String date) {this.date = date;}
    public void setWinner(String winner) { this.winner = winner; }
    public void setDetailedScore(String detailedScore) { this.detailedScore = detailedScore; }
    public void setSetScore(String setScore) { this.setScore = setScore; }
    public void setStatus(String status) { this.status = status; }
    public void setLocation(String location) { this.location = location; }
    public void setCurrentGame(String currentGame) { this.currentGame = currentGame; }
    public void setPriority(int priority) { this.priority = priority; }

    //────────── METODI UTILI ──────────
    public boolean isFinished() { return status != null && (status.equals("FINE") || status.equals("A tavolino")); }	//Controllo se il match è finito
    public boolean isAnnullata() { return status != null && status.equals("Annullata"); }	//Controllo se il match è annullato
    public boolean isLive() { return status != null && (status.equals("LIVE") || status.matches("[1-5]º set")); }	//Controllo se il match è LIVE
    public boolean isTavolino() { return status != null && status.equals("A tavolino"); }	//Controllo se il match è a tavolino

    //Metodo che ritorna una rappresentazione testuale della partita sovrascrivendo il metodo toString()
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();	//Creo un StringBuilder
        sb.append(String.format("%s: %s vs %s", tournament, player1, player2));	//Aggiungo torneo e giocatori
        if (winner != null && !winner.isEmpty()) sb.append(String.format(" - 🏆 %s", winner));	//Aggiungo vincitore se presente
        if (detailedScore != null && !detailedScore.isEmpty()) sb.append(String.format(" [%s]", detailedScore));	//Aggiungo punteggio dettagliato se presente
        else sb.append(String.format(" - %s", score));	//Altrimenti aggiungo punteggio generale
        if (currentGame != null && !currentGame.isEmpty()) sb.append(String.format(" (Game: %s)", currentGame));	//Aggiungo game corrente se presente
        sb.append(String.format(" - %s", date));	//Aggiungo data
        sb.append(String.format(" [%s]", status));	//Aggiungo stato
        return sb.toString();	//Ritorno la stringa finale
    }
}