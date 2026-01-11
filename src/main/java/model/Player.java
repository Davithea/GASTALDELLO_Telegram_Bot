package model;

//Classe Player per la rappresentazione di un giocatore di Tennis
public class Player {
    private String nome;	//Memorizzo il nome del giocatore
    private String paese;	//Memorizzo il paese del giocatore
    private int ranking;	//Memorizzo il ranking attuale
    private int punti;	//Memorizzo i punti ATP del giocatore
    private int eta;	//Memorizzo l'età del giocatore
    private String extraInfo;	//Memorizzo informazioni complete da Wikipedia
    private String imageUrl;	//Memorizzo l'URL dell'immagine del giocatore
    private String altezza;	//Memorizzo l'altezza del giocatore
    private String peso;	//Memorizzo il peso del giocatore
    private String migliorRanking;	//Memorizzo il miglior ranking raggiunto
    private String vittorieSconfitte;	//Memorizzo record vittorie/sconfitte
    private String titoli;	//Memorizzo il numero di titoli vinti
    private boolean isTennisPlayer;	//Indico se il giocatore è un tennista verificato

    //Costruttore principale della classe
    public Player(String nome, String paese, int ranking, int punti, int eta) {
        this.nome = nome;	//Inizializzo il nome
        this.paese = paese;	//Inizializzo il paese
        this.ranking = ranking;	//Inizializzo il ranking
        this.punti = punti;	//Inizializzo i punti
        this.eta = eta;	//Inizializzo l'età
        this.isTennisPlayer = false;	//Imposto di default isTennisPlayer a false, sarà aggiornato dopo verifica
    }

    //────────── GETTER ──────────
    public String getNome() { return nome; }
    public String getPaese() { return paese; }
    public int getRanking() { return ranking; }
    public int getPunti() { return punti; }
    public int getEta() { return eta; }
    public String getExtraInfo() { return extraInfo; }
    public String getImageUrl() { return imageUrl; }
    public String getAltezza() { return altezza; }
    public String getPeso() { return peso; }
    public String getMigliorRanking() { return migliorRanking; }
    public String getVittorieSconfitte() { return vittorieSconfitte; }
    public String getTitoli() { return titoli; }
    public boolean isTennisPlayer() { return isTennisPlayer; }

    //────────── SETTER ──────────
    public void setNome(String nome) { this.nome = nome; }
    public void setPaese(String paese) { this.paese = paese; }
    public void setRanking(int ranking) { this.ranking = ranking; }
    public void setPunti(int punti) { this.punti = punti; }
    public void setEta(int eta) { this.eta = eta; }
    public void setExtraInfo(String extraInfo) { this.extraInfo = extraInfo; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setAltezza(String altezza) { this.altezza = altezza; }
    public void setPeso(String peso) { this.peso = peso; }
    public void setMigliorRanking(String migliorRanking) { this.migliorRanking = migliorRanking; }
    public void setVittorieSconfitte(String vittorieSconfitte) { this.vittorieSconfitte = vittorieSconfitte; }
    public void setTitoli(String titoli) { this.titoli = titoli; }
    public void setTennisPlayer(boolean tennisPlayer) { isTennisPlayer = tennisPlayer; }

    //Metodo che ritorna una rappresentazione testuale del giocatore sovrascrivendo il metodo toString()
    @Override
    public String toString() {
        return String.format("%d. %s (%s) - %d punti", ranking, nome, paese, punti);
    }
}