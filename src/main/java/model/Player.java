package model;

public class Player {
    private String nome;
    private String paese;
    private int ranking;
    private int punti;
    private int eta;

    public Player(String nome, String paese, int ranking, int punti, int eta) {
        this.nome = nome;
        this.paese = paese;
        this.ranking = ranking;
        this.punti = punti;
        this.eta = eta;
    }

    // Getter methods
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

    // Setter methods
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

    @Override
    public String toString() {
        return String.format("%s (%s) - Ranking: #%d, Punti: %d",
                nome, paese, ranking, punti);
    }
}