package model;

public class Player {
    private String nome;
    private String paese;
    private int ranking;
    private int punti;
    private int eta;
    private String extraInfo; // Info complete da Wikipedia
    private String imageUrl;  // URL immagine giocatore

    // Nuovi campi per preferiti
    private String altezza;
    private String peso;
    private String migliorRanking;
    private String vittorieSconfitte;
    private String titoli;
    private boolean isTennisPlayer; // Flag per verificare se è tennista

    public Player(String nome, String paese, int ranking, int punti, int eta) {
        this.nome = nome;
        this.paese = paese;
        this.ranking = ranking;
        this.punti = punti;
        this.eta = eta;
        this.isTennisPlayer = false; // Default false, sarà impostato dopo verifica
    }

    // Getters
    public String getNome() {
        return nome;
    }

    public String getPaese() {
        return paese;
    }

    public int getRanking() {
        return ranking;
    }

    public int getPunti() {
        return punti;
    }

    public int getEta() {
        return eta;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getAltezza() {
        return altezza;
    }

    public String getPeso() {
        return peso;
    }

    public String getMigliorRanking() {
        return migliorRanking;
    }

    public String getVittorieSconfitte() {
        return vittorieSconfitte;
    }

    public String getTitoli() {
        return titoli;
    }

    public boolean isTennisPlayer() {
        return isTennisPlayer;
    }

    // Setters
    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setPaese(String paese) {
        this.paese = paese;
    }

    public void setRanking(int ranking) {
        this.ranking = ranking;
    }

    public void setPunti(int punti) {
        this.punti = punti;
    }

    public void setEta(int eta) {
        this.eta = eta;
    }

    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setAltezza(String altezza) {
        this.altezza = altezza;
    }

    public void setPeso(String peso) {
        this.peso = peso;
    }

    public void setMigliorRanking(String migliorRanking) {
        this.migliorRanking = migliorRanking;
    }

    public void setVittorieSconfitte(String vittorieSconfitte) {
        this.vittorieSconfitte = vittorieSconfitte;
    }

    public void setTitoli(String titoli) {
        this.titoli = titoli;
    }

    public void setTennisPlayer(boolean tennisPlayer) {
        isTennisPlayer = tennisPlayer;
    }

    @Override
    public String toString() {
        return String.format("%d. %s (%s) - %d punti", ranking, nome, paese, punti);
    }
}