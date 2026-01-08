package model;

/**
 * Classe per memorizzare i dati Head-to-Head tra due giocatori
 */
public class H2HData {
    private String player1Name;
    private String player2Name;
    private String player1Image;
    private String player2Image;

    // Statistiche Player 1
    private String player1PrizeMoney;
    private String player1WinLoss;
    private String player1WinPercentage;
    private int player1Grass;
    private int player1Clay;
    private int player1Hard;
    private int player1Indoor;
    private int player1Titles;
    private String player1YTDWinLoss;
    private String player1YTDPercentage;

    // Statistiche Player 2
    private String player2PrizeMoney;
    private String player2WinLoss;
    private String player2WinPercentage;
    private int player2Grass;
    private int player2Clay;
    private int player2Hard;
    private int player2Indoor;
    private int player2Titles;
    private String player2YTDWinLoss;
    private String player2YTDPercentage;

    // H2H generale
    private int totalH2HMatches;
    private String h2hRecord; // es. "3-0"

    public H2HData() {}

    // Getters e Setters

    public String getPlayer1Name() {
        return player1Name;
    }

    public void setPlayer1Name(String player1Name) {
        this.player1Name = player1Name;
    }

    public String getPlayer2Name() {
        return player2Name;
    }

    public void setPlayer2Name(String player2Name) {
        this.player2Name = player2Name;
    }

    public String getPlayer1Image() {
        return player1Image;
    }

    public void setPlayer1Image(String player1Image) {
        this.player1Image = player1Image;
    }

    public String getPlayer2Image() {
        return player2Image;
    }

    public void setPlayer2Image(String player2Image) {
        this.player2Image = player2Image;
    }

    public String getPlayer1PrizeMoney() {
        return player1PrizeMoney;
    }

    public void setPlayer1PrizeMoney(String player1PrizeMoney) {
        this.player1PrizeMoney = player1PrizeMoney;
    }

    public String getPlayer1WinLoss() {
        return player1WinLoss;
    }

    public void setPlayer1WinLoss(String player1WinLoss) {
        this.player1WinLoss = player1WinLoss;
    }

    public String getPlayer1WinPercentage() {
        return player1WinPercentage;
    }

    public void setPlayer1WinPercentage(String player1WinPercentage) {
        this.player1WinPercentage = player1WinPercentage;
    }

    public int getPlayer1Grass() {
        return player1Grass;
    }

    public void setPlayer1Grass(int player1Grass) {
        this.player1Grass = player1Grass;
    }

    public int getPlayer1Clay() {
        return player1Clay;
    }

    public void setPlayer1Clay(int player1Clay) {
        this.player1Clay = player1Clay;
    }

    public int getPlayer1Hard() {
        return player1Hard;
    }

    public void setPlayer1Hard(int player1Hard) {
        this.player1Hard = player1Hard;
    }

    public int getPlayer1Indoor() {
        return player1Indoor;
    }

    public void setPlayer1Indoor(int player1Indoor) {
        this.player1Indoor = player1Indoor;
    }

    public int getPlayer1Titles() {
        return player1Titles;
    }

    public void setPlayer1Titles(int player1Titles) {
        this.player1Titles = player1Titles;
    }

    public String getPlayer1YTDWinLoss() {
        return player1YTDWinLoss;
    }

    public void setPlayer1YTDWinLoss(String player1YTDWinLoss) {
        this.player1YTDWinLoss = player1YTDWinLoss;
    }

    public String getPlayer1YTDPercentage() {
        return player1YTDPercentage;
    }

    public void setPlayer1YTDPercentage(String player1YTDPercentage) {
        this.player1YTDPercentage = player1YTDPercentage;
    }

    public String getPlayer2PrizeMoney() {
        return player2PrizeMoney;
    }

    public void setPlayer2PrizeMoney(String player2PrizeMoney) {
        this.player2PrizeMoney = player2PrizeMoney;
    }

    public String getPlayer2WinLoss() {
        return player2WinLoss;
    }

    public void setPlayer2WinLoss(String player2WinLoss) {
        this.player2WinLoss = player2WinLoss;
    }

    public String getPlayer2WinPercentage() {
        return player2WinPercentage;
    }

    public void setPlayer2WinPercentage(String player2WinPercentage) {
        this.player2WinPercentage = player2WinPercentage;
    }

    public int getPlayer2Grass() {
        return player2Grass;
    }

    public void setPlayer2Grass(int player2Grass) {
        this.player2Grass = player2Grass;
    }

    public int getPlayer2Clay() {
        return player2Clay;
    }

    public void setPlayer2Clay(int player2Clay) {
        this.player2Clay = player2Clay;
    }

    public int getPlayer2Hard() {
        return player2Hard;
    }

    public void setPlayer2Hard(int player2Hard) {
        this.player2Hard = player2Hard;
    }

    public int getPlayer2Indoor() {
        return player2Indoor;
    }

    public void setPlayer2Indoor(int player2Indoor) {
        this.player2Indoor = player2Indoor;
    }

    public int getPlayer2Titles() {
        return player2Titles;
    }

    public void setPlayer2Titles(int player2Titles) {
        this.player2Titles = player2Titles;
    }

    public String getPlayer2YTDWinLoss() {
        return player2YTDWinLoss;
    }

    public void setPlayer2YTDWinLoss(String player2YTDWinLoss) {
        this.player2YTDWinLoss = player2YTDWinLoss;
    }

    public String getPlayer2YTDPercentage() {
        return player2YTDPercentage;
    }

    public void setPlayer2YTDPercentage(String player2YTDPercentage) {
        this.player2YTDPercentage = player2YTDPercentage;
    }

    public int getTotalH2HMatches() {
        return totalH2HMatches;
    }

    public void setTotalH2HMatches(int totalH2HMatches) {
        this.totalH2HMatches = totalH2HMatches;
    }

    public String getH2hRecord() {
        return h2hRecord;
    }

    public void setH2hRecord(String h2hRecord) {
        this.h2hRecord = h2hRecord;
    }
}