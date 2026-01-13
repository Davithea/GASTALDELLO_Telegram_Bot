package scraper;

import model.H2HData;
import model.Match;
import model.Player;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*Classe TennisService per la gestione dello scraping delle pagine web. Tre tipi:
1. Top 10 classifiche (Wikipedia) - statico
2. Ricerca giocatori (Wikipedia) - statico
3. H2H tra giocatori (Matchstat e Wikipedia) - statico
4. Partite di oggi (Sofascore) - dinamico usando Selenium
 */
public class TennisService {
    private final OkHttpClient client;  //Dichiarazione di una variabile client OkHttpClient

    //Costruttore della classe
    public TennisService() {
        this.client = new OkHttpClient.Builder()    //Inizializzazione del client OkHttpClient
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    //==================== CLASSIFICHE (SCRAPING WIKIPEDIA) ====================
    //Metodo pubblico per prelevare n giocatori dalla classifica atp singolare
    public List<Player> getATPRankings(int limit) {
        return getRankings(limit, "ATP_rankings", "atp");   //Richiamo il metodo getRankings con le informazioni corrette
    }

    //Metodo pubblico per prelevare n giocatori dalla classifica wta singolare
    public List<Player> getWTARankings(int limit) {
        return getRankings(limit, "WTA_rankings", "wta");   //Richiamo il metodo getRankings con le informazioni corrette
    }

    //Metodo pubblico per prelevare n giocatori dalla classifica atp singolare annuale
    public List<Player> getRaceRankings(int limit) {
        return getRankings(limit, "ATP_rankings", "race");  //Richiamo il metodo getRankings con le informazioni corrette
    }

    //Metodo pubblico per prelevare n giocatori dalla classifica atp doppio
    public List<Player> getATPDoubleRankings(int limit) {
        return getRankings(limit, "ATP_rankings", "atp_doppio");    //Richiamo il metodo getRankings con le informazioni corrette
    }

    //Metodo pubblico per prelevare n giocatori dalla classifica wta doppio
    public List<Player> getWTADoubleRankings(int limit) {
        return getRankings(limit, "WTA_rankings", "wta_doppio");    //Richiamo il metodo getRankings con le informazioni corrette
    }

    //Metodo privato per la gestione dello scraping statico delle classifiche da Wikipedia
    private List<Player> getRankings(int limit, String wikiPage, String type) {
        List<Player> players = new ArrayList<>();	//Inizializzo una ArrayList che conterrà i giocatori estratti
        System.out.println("🌐 Scraping classifiche " + type.toUpperCase() + " da Wikipedia...");	//Stampo a console il tipo di scraping che sto avviando

        try {	//Avvio un blocco try per gestire eventuali eccezioni
            String url = "https://en.wikipedia.org/wiki/" + wikiPage;	//Costruisco l'URL completo della pagina Wikipedia
            Request request = new Request.Builder()	//Creo un oggetto Request per la chiamata HTTP
                    .url(url)	//Imposto l'URL della richiesta
                    .addHeader("User-Agent", "Mozilla/5.0")	//Aggiungo lo User-Agent per evitare blocchi lato server
                    .build();	//Costruisco la richiesta finale
            try (Response response = client.newCall(request).execute()) {	//Eseguo la richiesta HTTP e salvo la risposta
                if (!response.isSuccessful() || response.body() == null) {	//Controllo che la risposta sia valida
                    return players;	//Se non è valida ritorno una lista vuota
                }

                String html = response.body().string();	//Estraggo il contenuto HTML dalla risposta
                Document doc = Jsoup.parse(html);	//Parso l'HTML in un oggetto Document tramite Jsoup

                Elements allTables = doc.select("table.wikitable");	//Seleziono tutte le tabelle con classe wikitable

                int tableIndex = switch (type) {	//Decido quale tabella usare in base al tipo di classifica (uso un indice)
                    case "race", "wta_doppio" -> 1;	//Per race e doppio WTA uso la seconda tabella
                    case "atp" -> 2;	//Per ATP singolare uso la terza tabella
                    case "atp_doppio" -> 4;	//Per ATP doppio uso la quinta tabella
                    default -> 0;	//Di default uso la prima tabella
                };

                int currentTableIndex = 0;	//Inizializzo il contatore delle tabelle valide trovate

                for (Element table : allTables) {	//Itero su tutte le tabelle trovate
                    Elements headers = table.select("th");	//Estraggo le intestazioni della tabella
                    String headerText = headers.text().toLowerCase();	//Converto il testo delle intestazioni in minuscolo

                    if (headerText.contains("rank") || headerText.contains("player")) {	//Verifico che la tabella sia una classifica
                        if ((type.equals("atp") || type.equals("atp_doppio") || type.equals("wta_doppio"))
                                && currentTableIndex < tableIndex) {	//Salto le tabelle precedenti a quella desiderata
                            currentTableIndex++;	//Incremento il contatore delle tabelle
                            continue;	//Passo alla tabella successiva
                        }
                        System.out.println("✅ Usando tabella: " + table.select("caption").text());	//Stampo il nome della tabella scelta
                        Elements rows = table.select("tbody tr");	//Seleziono tutte le righe del corpo della tabella
                        int lastRanking = -1;	//Memorizzo l'ultimo ranking valido per gestire i pari merito "="
                        for (Element row : rows) {	//Itero su ogni riga della tabella
                            if (players.size() >= limit) break;	//Mi fermo se ho raggiunto il limite di giocatori
                            Elements cells = row.select("td");	//Estraggo tutte le celle della riga
                            if (cells.size() < 3) continue;	//Scarto righe non valide o incomplete
                            try {
                                //-------------------- ESTRAZIONE DEL RANKING --------------------
                                String rawRank = cells.get(0).text().trim();	//Estraggo il testo grezzo del ranking
                                int ranking;	//Dichiaro la variabile ranking

                                if (rawRank.equals("=")) {	//Se il ranking è "=" significa pari merito
                                    if (lastRanking == -1) continue;	//Se non ho un ranking precedente salto la riga
                                    ranking = lastRanking;	//Assegno lo stesso ranking precedente
                                } else {
                                    String rankText = rawRank.replaceAll("[^0-9]", "");	//Rimuovo tutti i caratteri non numerici
                                    if (rankText.isEmpty()) continue;	//Se non resta nulla salto la riga
                                    ranking = Integer.parseInt(rankText);	//Converto il ranking in intero
                                    lastRanking = ranking;	//Salvo il ranking come ultimo valido
                                }

                                //-------------------- NOME --------------------
                                String name = "";	//Inizializzo una variabile per contenere il nome del giocatore
                                for (int i = 1; i < Math.min(cells.size(), 4); i++) {	//Scorro alcune celle per trovare il nome
                                    String cellText = cells.get(i).text().trim();	//Estraggo il testo della cella
                                    if (cellText.split("\\s+").length >= 2 && !cellText.matches("^[0-9,]+$")) {	//Verifico che sembri un nome valido
                                        name = cellText;	//Assegno il nome trovato
                                        break;	//Esco dal ciclo
                                    }
                                }
                                if (name.isEmpty()) continue;	//Se non ho trovato un nome valido salto la riga

                                //-------------------- PUNTI --------------------
                                int points = 0;	//Inizializzo i punti
                                for (int i = 2; i < cells.size(); i++) {	//Scorro le celle alla ricerca dei punti
                                    String pointsText = cells.get(i).text().replaceAll("[^0-9]", "");	//Estraggo solo i numeri
                                    if (!pointsText.isEmpty() && pointsText.length() >= 3) {	//Controllo che siano plausibili
                                        points = Integer.parseInt(pointsText);	//Converto i punti in intero
                                        break;	//Esco dal ciclo
                                    }
                                }

                                //-------------------- NAZIONE --------------------
                                String country = "Unknown";	//Imposto la nazione di default
                                for (Element cell : cells) {	//Scorro tutte le celle
                                    Elements imgs = cell.select("img");	//Cerco eventuali immagini
                                    if (!imgs.isEmpty()) {	//Se trovo un'immagine
                                        String alt = imgs.first().attr("alt");	//Leggo l'attributo alt
                                        if (!alt.isEmpty() && alt.length() < 50) {	//Verifico che sia una nazione valida
                                            country = cleanCountry(alt);	//Pulisco e assegno il nome della nazione
                                            break;	//Esco dal ciclo
                                        }
                                    }
                                }
                                players.add(new Player(name, country, ranking, points, 0));	//Creo e aggiungo il Player alla lista
                            } catch (Exception e) {
                                //Salto la riga in caso di errore di parsing
                            }
                        }
                        if (!players.isEmpty()) break;	//Se ho estratto giocatori interrompo la ricerca
                    }
                }
                System.out.println("✅ " + players.size() + " giocatori estratti");	//Stampo il numero di giocatori trovati
            }
        } catch (Exception e) {
            System.out.println("❌ Errore scraping: " + e.getMessage());	//Gestisco eventuali errori generali di scraping
        }

        return players;	//Ritorno la lista finale dei giocatori
    }

    //Metodo per pulire il nome dei paesi
    private String cleanCountry(String country) {
        if (country == null || country.isEmpty()) return "Unknown"; //Se il paese non è presente, assengo il valore a 'Unkwnown'
        return country.replaceAll("(?i)flag of ", "")
                .replaceAll("(?i)flag icon ", "")
                .replaceAll("(?i)the ", "")
                .trim();    //Se invece è presente tolgo eventuali spazi prima e dopo e frasi come 'flag of', 'flag icon' e 'the'
    }

    //==================== RICERCA GIOCATORE (SCRAPING WIKIPEDIA) ====================
    //Metodo per cercare un giocatore su Wikipedia
    public Player searchPlayer(String playerName) {
        System.out.println("🔍 Cercando pagina Wikipedia per: " + playerName);	//Stampo a console il nome del giocatore che sto cercando

        try {
            String wikiName = formatWikipediaName(playerName);	//Formatto correttamente il nome per l'URL di Wikipedia
            String url = "https://it.wikipedia.org/wiki/" + wikiName;	//Costruisco l'URL della pagina Wikipedia
            System.out.println("📄 URL Wikipedia: " + url);	//Stampo l'URL che verrà interrogato
            Request request = new Request.Builder()	//Creo una nuova richiesta HTTP
                    .url(url)
                    .addHeader("User-Agent", "Mozilla/5.0")
                    .build();
            try (Response response = client.newCall(request).execute()) {	//Eseguo la richiesta e ottengo la risposta
                if (!response.isSuccessful()) {	//Controllo se la risposta non è andata a buon fine
                    System.out.println("❌ Pagina non trovata: " + response.code());	//Stampo il codice di errore HTTP
                    System.out.println("🔄 Tentativo ricerca nelle classifiche...");	//Avviso che proverò una ricerca alternativa
                    return searchInRankings(playerName);	//Cerco il giocatore nelle classifiche
                }

                String html = response.body().string();	//Estraggo il contenuto HTML della pagina
                Document doc = Jsoup.parse(html);	//Parso l'HTML in un oggetto Document con Jsoup

                if (!isTennisPlayer(doc)) {	//Verifico che la pagina appartenga a un giocatore di tennis
                    System.out.println("⚠️ Non è un giocatore di tennis");	//Stampo un avviso se non è un tennista
                    return null;	//Ritorno null perché il soggetto non è valido
                }

                Player player = extractPlayerInfo(doc, playerName);	//Estraggo le informazioni del giocatore dalla pagina

                if (player != null) {	//Controllo se l'estrazione è andata a buon fine
                    System.out.println("✅ Giocatore trovato: " + player.getNome());	//Stampo il nome del giocatore trovato
                    return player;	//Ritorno l'oggetto Player
                } else {
                    System.out.println("❌ Impossibile estrarre dati giocatore");	//Stampo errore di estrazione dati
                    return null;	//Ritorno null in caso di fallimento
                }
            }

        } catch (Exception e) {
            System.out.println("❌ Errore ricerca: " + e.getMessage());	//Gestisco eventuali errori durante la ricerca
            return searchInRankings(playerName);	//In caso di errore provo comunque la ricerca nelle classifiche
        }
    }

    //Metodo per formattare correttamente il nome di un giocatore per formare l'URL della pagina Wikipeida
    private String formatWikipediaName(String name) {
        String[] words = name.trim().split("\\s+");	//Rimuovo spazi iniziali/finali e divido il nome in parole
        StringBuilder formatted = new StringBuilder();	//Creo uno StringBuilder per costruire il nome formattato

        for (int i = 0; i < words.length; i++) {	//Itero su tutte le parole del nome
            String word = words[i];	//Salvo la parola corrente
            if (word.length() > 0) {	//Controllo che la parola non sia vuota
                formatted.append(Character.toUpperCase(word.charAt(0)));	//Rendo maiuscola la prima lettera
                formatted.append(word.substring(1).toLowerCase());	//Rendo minuscole le lettere successive

                if (i < words.length - 1) {	//Se non è l'ultima parola
                    formatted.append("_");	//Aggiungo un underscore tra le parole
                }
            }
        }

        return formatted.toString();	//Ritorno il nome formattato per l'URL di Wikipedia
    }

    //Metodo privato che verifica se la pagina appartiene a un tennista
    private boolean isTennisPlayer(Document doc) {
        String pageText = doc.text().toLowerCase();	//Estraggo tutto il testo della pagina e lo converto in minuscolo

        boolean isTennista = pageText.contains("tennis") ||	//Controllo se il testo contiene la parola tennis
                pageText.contains("atp") ||	//Controllo se viene menzionato il circuito ATP
                pageText.contains("wta") ||	//Controllo se viene menzionato il circuito WTA
                pageText.contains("grand slam") ||	//Controllo la presenza del termine Grand Slam
                pageText.contains("australian open") ||	//Controllo se è citato l'Australian Open
                pageText.contains("wimbledon") ||	//Controllo se è citato Wimbledon
                pageText.contains("us open") ||	//Controllo se è citato lo US Open
                pageText.contains("roland garros");	//Controllo se è citato il Roland Garros

        Elements infobox = doc.select("table.infobox");	//Seleziono la tabella infobox della pagina Wikipedia
        if (!infobox.isEmpty()) {	//Verifico che l'infobox sia presente
            String infoboxText = infobox.text().toLowerCase();	//Estraggo il testo dell'infobox e lo converto in minuscolo
            if (infoboxText.contains("tennis") ||	//Controllo se nell'infobox compare la parola tennis
                    infoboxText.contains("sport") && infoboxText.contains("tennis")) {	//Oppure se è indicato lo sport come tennis
                return true;	//Se una delle condizioni è vera confermo che è un tennista
            }
        }
        return isTennista;	//Ritorno il risultato basato sulla presenza di parole chiave nel testo della pagina
    }

    //Metodo privato per estrarre le informazioni su un giocatore
    private Player extractPlayerInfo(Document doc, String searchName) {
        try {
            Elements infobox = doc.select("table.infobox");	//Seleziono la tabella infobox della pagina
            if (infobox.isEmpty()) {	//Controllo se l’infobox non è presente
                System.out.println("⚠️ Infobox non trovato");	//Stampo un avviso a console
                return null;	//Ritorno null perché non posso estrarre i dati
            }
            Element table = infobox.first();	//Recupero la prima infobox trovata

            String nome = extractFromTable(table, searchName);	//Estraggo il nome del giocatore
            String nazionalita = extractNationality(table);	//Estraggo la nazionalità del giocatore
            String altezza = cleanText(extractFromTableRow(table, "Altezza"));	//Estraggo e pulisco il valore dell’altezza
            String peso = cleanText(extractFromTableRow(table, "Peso"));	//Estraggo e pulisco il valore del peso
            String datanascita = extractBirthDate(table);	//Estraggo la data di nascita
            int ranking = extractRanking(table);	//Estraggo il ranking attuale
            String migliorRanking = cleanText(extractFromTableRow(table, "Miglior ranking"));	//Estraggo il miglior ranking in carriera
            String vittorie = cleanText(extractFromTableRow(table, "Vittorie/sconfitte"));	//Estraggo il record vittorie/sconfitte
            String titoliSingolo = cleanText(extractFromTableRow(table, "Titoli vinti"));	//Estraggo il numero di titoli vinti
            String grandslamVinti = extractGrandSlams(table);	//Estraggo i Grand Slam vinti
            String manoPreferita = cleanText(extractFromTableRow(table, "Mano"));	//Estraggo la mano preferita
            String allenatore = cleanText(extractFromTableRow(table, "Allenatore"));	//Estraggo il nome dell’allenatore
            String premiMonetari = cleanText(extractFromTableRow(table, "Montepremi", "Palmares"));	//Estraggo i premi monetari guadagnati
            String imageUrl = extractImageUrl(table);	//Estraggo l’URL dell’immagine del giocatore

            int eta = calculateAge(datanascita);	//Calcolo l’età del giocatore dalla data di nascita

            StringBuilder info = new StringBuilder();	//Creo uno StringBuilder per costruire le informazioni testuali
            info.append("🎾 ").append(nome.toUpperCase()).append("\n\n");	//Aggiungo il nome del giocatore come intestazione

            if (imageUrl != null && !imageUrl.isEmpty()) {	//Controllo se l’immagine è disponibile
                info.append("📷 Foto: ").append(imageUrl).append("\n\n");	//Aggiungo il link alla foto
            }

            info.append("🌍 Nazionalità: ").append(nazionalita).append("\n");	//Aggiungo la nazionalità

            if (datanascita != null && !datanascita.isEmpty()) {	//Controllo se la data di nascita è valida
                info.append("📅 Data di nascita: ").append(datanascita);	//Aggiungo la data di nascita
                if (eta > 0) {	//Se l’età è valida
                    info.append(" (").append(eta).append(" anni)");	//Aggiungo l’età tra parentesi
                }
                info.append("\n");	//Vado a capo
            }

            if (altezza != null && !altezza.isEmpty()) {	//Controllo se l’altezza è disponibile
                info.append("📏 Altezza: ").append(altezza).append("\n");	//Aggiungo l’altezza
            }

            if (peso != null && !peso.isEmpty()) {	//Controllo se il peso è disponibile
                info.append("⚖️ Peso: ").append(peso).append("\n");	//Aggiungo il peso
            }

            if (manoPreferita != null && !manoPreferita.isEmpty()) {	//Controllo se la mano preferita è disponibile
                info.append("✋ Mano: ").append(manoPreferita).append("\n");	//Aggiungo la mano preferita
            }

            info.append("\n📊 CARRIERA\n\n");	//Aggiungo la sezione carriera

            if (ranking > 0) {	//Controllo se il ranking è valido
                info.append("🏆 Ranking attuale: #").append(ranking).append("\n");	//Aggiungo il ranking attuale
            }

            if (migliorRanking != null && !migliorRanking.isEmpty()) {	//Controllo se il miglior ranking è disponibile
                info.append("⭐ Miglior ranking: ").append(migliorRanking).append("\n");	//Aggiungo il miglior ranking
            }

            if (vittorie != null && !vittorie.isEmpty()) {	//Controllo se il record vittorie/sconfitte è disponibile
                info.append("📈 Vittorie/Sconfitte: ").append(vittorie).append("\n");	//Aggiungo il record
            }

            if (titoliSingolo != null && !titoliSingolo.isEmpty()) {	//Controllo se i titoli sono disponibili
                info.append("🏅 Titoli: ").append(titoliSingolo).append("\n");	//Aggiungo i titoli vinti
            }

            if (grandslamVinti != null && !grandslamVinti.isEmpty()) {	//Controllo se ci sono Grand Slam vinti
                info.append("\n🏆 GRAND SLAM\n\n");	//Aggiungo la sezione Grand Slam
                info.append(grandslamVinti).append("\n");	//Aggiungo i dettagli dei Grand Slam
            }

            if (allenatore != null && !allenatore.isEmpty()) {	//Controllo se l’allenatore è disponibile
                info.append("\n👨‍🏫 Allenatore: ").append(allenatore).append("\n");	//Aggiungo l’allenatore
            }

            if (premiMonetari != null && !premiMonetari.isEmpty()) {	//Controllo se i premi monetari sono disponibili
                info.append("💰 Montepremi: ").append(premiMonetari).append("\n");	//Aggiungo i premi monetari
            }

            Player player = new Player(nome, nazionalita, ranking, 0, eta);	//Creo l’oggetto Player con i dati principali
            player.setExtraInfo(info.toString());	//Imposto le informazioni testuali aggiuntive
            player.setImageUrl(imageUrl);	//Imposto l’URL dell’immagine
            player.setTennisPlayer(true);	//Imposto il flag che indica che è un tennista
            player.setAltezza(altezza);	//Imposto l’altezza del giocatore
            player.setPeso(peso);	//Imposto il peso del giocatore
            player.setMigliorRanking(migliorRanking);	//Imposto il miglior ranking
            player.setVittorieSconfitte(vittorie);	//Imposto il record vittorie/sconfitte
            player.setTitoli(titoliSingolo);	//Imposto i titoli vinti

            return player;	//Ritorno l’oggetto Player completamente popolato
        } catch (Exception e) {
            System.out.println("❌ Errore estrazione dati: " + e.getMessage());	//Stampo l’errore avvenuto durante l’estrazione
            e.printStackTrace();	//Stampo lo stack trace per debug
            return null;	//Ritorno null in caso di errore
        }
    }

    //Metodo privato per pulire il testo rimuovendo note e spazi extra
    private String cleanText(String text) {
        if (text == null) return null;	//Se il testo è null ritorno null immediatamente
        text = text.replaceAll("\\[\\d+\\]", "");	//Rimuovo riferimenti numerici tipo [1], [2], ecc.
        text = text.replaceAll("\\[nota \\d+\\]", "");	//Rimuovo note come [nota 1], [nota 2], ecc.
        text = text.replaceAll("\\s+", " ");	//Sostituisco sequenze di spazi multipli con un singolo spazio
        return text.trim();	//Rimuovo spazi iniziali e finali e ritorno il testo pulito
    }

    //Metodo privato per estrarre la data di nascita di un giocatore
    private String extractBirthDate(Element table) {
        Elements rows = table.select("tr");	//Seleziono tutte le righe della tabella
        for (Element row : rows) {	//Itero su ogni riga
            Elements th = row.select("th, td.infobox-label");	//Seleziono le intestazioni o celle con classe infobox-label
            if (th.isEmpty()) continue;	//Se non ci sono intestazioni salto la riga
            String header = th.first().text().toLowerCase();	//Estraggo il testo della prima intestazione e lo converto in minuscolo
            if ((header.contains("data di nascita") ||	//Controllo se la riga contiene "data di nascita"
                    (header.contains("nato") && !header.contains("nazionalità")))) {	//Oppure se contiene "nato" ma non "nazionalità"

                Elements td = row.select("td");	//Seleziono le celle di dati della riga
                if (!td.isEmpty()) {	//Se esiste almeno una cella
                    return cleanText(td.first().text().trim());	//Ritorno il testo pulito e senza spazi superflui
                }
            }
        }
        return null;	//Se non trovo la data ritorno null
    }

    //Metodo privato che estare il nome del giocatore dalla infobox della pagina relativa
    private String extractFromTable(Element table, String defaultName) {
        Elements title = table.select("th.infobox-title, caption");	//Seleziono l’elemento con classe infobox-title o il caption della tabella
        if (!title.isEmpty()) {	//Controllo che l’elemento esista
            String name = title.first().text().trim();	//Estraggo il testo e rimuovo spazi iniziali e finali
            if (!name.isEmpty()) {	//Se il testo non è vuoto
                return name;	//Ritorno il nome estratto
            }
        }
        return defaultName;	//Se non trovo il nome ritorno quello di default passato come parametro
    }

    //Metodo privato che estrae la nazionalità del giocatore dall’infobox
    private String extractNationality(Element table) {
        Elements rows = table.select("tr");	//Seleziono tutte le righe della tabella
        for (Element row : rows) {	//Itero su ogni riga
            if (row.text().toLowerCase().contains("nazionalità")) {	//Controllo se la riga contiene il termine "nazionalità"
                Elements links = row.select("a");	//Seleziono tutti i link presenti nella riga
                for (Element link : links) {	//Itero su ogni link
                    String text = link.text().trim();	//Estraggo il testo e rimuovo spazi superflui
                    if (!text.isEmpty() && !text.equalsIgnoreCase("nazionalità")) {	//Se il testo è valido e non è la parola "nazionalità"
                        return text;	//Ritorno il testo come nazionalità
                    }
                }
                String cellText = row.select("td").text().trim();	//Se non ci sono link validi, estraggo il testo della cella td
                if (!cellText.isEmpty() && !cellText.equalsIgnoreCase("nazionalità")) {	//Controllo che il testo sia valido
                    return cellText;	//Ritorno il testo come nazionalità
                }
            }
        }
        return "Unknown";	//Se non trovo la nazionalità ritorno "Unknown"
    }

    //Metodo privato che estrae il valore di una riga dell’infobox basandosi su parole chiave
    private String extractFromTableRow(Element table, String... keywords) {
        Elements rows = table.select("tr");	//Seleziono tutte le righe della tabella
        for (Element row : rows) {	//Itero su ogni riga
            Elements th = row.select("th, td.infobox-label");	//Seleziono le intestazioni o celle con classe infobox-label
            if (th.isEmpty()) continue;	//Se non ci sono intestazioni salto la riga
            String header = th.first().text().toLowerCase();	//Estraggo il testo della prima intestazione e lo converto in minuscolo
            for (String keyword : keywords) {	//Itero su tutte le parole chiave passate come parametro
                if (header.contains(keyword.toLowerCase())) {	//Controllo se l’intestazione contiene la parola chiave
                    Elements td = row.select("td");	//Seleziono le celle di dati della riga
                    if (!td.isEmpty()) {	//Se esiste almeno una cella
                        return td.first().text().trim();	//Ritorno il testo della cella, pulito da spazi superflui
                    }
                }
            }
        }
        return null;	//Se non trovo nulla ritorno null
    }

    //Metodo privato che estrae il ranking attuale del giocatore dall’infobox
    private int extractRanking(Element table) {
        String rankingText = extractFromTableRow(table, "Ranking attuale");	//Estraggo il testo della riga con la parola chiave "Ranking attuale"
        if (rankingText != null) {	//Se il testo è valido
            rankingText = cleanText(rankingText);	//Lo pulisco da note e spazi extra
            Pattern pattern = Pattern.compile("(\\d{1,3})(?:º|°|\\s|\\(|$)");	//Creo un pattern regex per catturare un numero da 1 a 3 cifre seguito da simboli comuni
            Matcher matcher = pattern.matcher(rankingText);	//Creo il matcher per applicare il pattern al testo
            if (matcher.find()) {	//Se trovo una corrispondenza
                try {
                    return Integer.parseInt(matcher.group(1));	//Converto il numero trovato in intero e lo ritorno
                } catch (Exception e) {	//Se c’è un errore di conversione
                    return 0;	//Ritorno 0 come valore di default
                }
            }
        }
        return 0;	//Se non trovo il ranking ritorno 0
    }

    //Metodo privato che estrae i risultati dei Grand Slam dall’infobox
    private String extractGrandSlams(Element table) {
        Map<String, StringBuilder> grandSlamResults = new LinkedHashMap<>();	//Creo una mappa ordinata per categorie di gioco
        grandSlamResults.put("Singolare", new StringBuilder());	//Inizializzo la categoria Singolare
        grandSlamResults.put("Doppio", new StringBuilder());	//Inizializzo la categoria Doppio
        grandSlamResults.put("Doppio Misto", new StringBuilder());	//Inizializzo la categoria Doppio Misto

        Elements rows = table.select("tr");	//Seleziono tutte le righe della tabella
        String currentCategory = null;	//Variabile per tenere traccia della categoria corrente

        for (Element row : rows) {	//Itero su ogni riga
            String rowText = row.text().toLowerCase();	//Estraggo il testo della riga in minuscolo

            if (rowText.contains("singolare")) {	//Se la riga indica Singolare
                currentCategory = "Singolare";	//Imposto la categoria corrente
                continue;
            } else if (rowText.contains("doppio misto")) {	//Se la riga indica Doppio Misto
                currentCategory = "Doppio Misto";	//Imposto la categoria corrente
                continue;
            } else if (rowText.contains("doppio")) {	//Se la riga indica Doppio
                currentCategory = "Doppio";	//Imposto la categoria corrente
                continue;
            }

            if (currentCategory == null) continue;	//Se non ho ancora una categoria valida salto la riga

            Elements cells = row.select("th, td");	//Seleziono tutte le celle della riga
            if (cells.size() < 2) continue;	//Se ci sono meno di due celle salto la riga

            String tournament = cells.get(0).text();	//Estraggo il nome del torneo dalla prima cella
            String result = cleanText(cells.get(1).text());	//Estraggo e pulisco il risultato dalla seconda cella
            StringBuilder sb = grandSlamResults.get(currentCategory);	//Recupero il StringBuilder della categoria corrente

            if (tournament.contains("Australian")) {	//Se il torneo è l’Australian Open
                sb.append("🇦🇺 Australian Open: ").append(result).append("\n");	//Aggiungo il risultato con l’emoji della bandiera
            } else if (tournament.contains("Roland") || tournament.contains("France")) {	//Se è Roland Garros
                sb.append("🇫🇷 Roland Garros: ").append(result).append("\n");	//Aggiungo il risultato
            } else if (tournament.contains("Wimbledon")) {	//Se è Wimbledon
                sb.append("🇬🇧 Wimbledon: ").append(result).append("\n");	//Aggiungo il risultato
            } else if (tournament.contains("US") || tournament.contains("U.S.")) {	//Se è US Open
                sb.append("🇺🇸 US Open: ").append(result).append("\n");	//Aggiungo il risultato
            }
        }

        StringBuilder finalResult = new StringBuilder();	//Creo un StringBuilder finale per concatenare tutte le categorie
        for (Map.Entry<String, StringBuilder> entry : grandSlamResults.entrySet()) {	//Itero su ogni categoria
            if (!entry.getValue().isEmpty()) {	//Se la categoria contiene risultati
                finalResult.append(entry.getKey()).append(":\n");	//Aggiungo il titolo della categoria
                finalResult.append(entry.getValue()).append("\n");	//Aggiungo i risultati
            }
        }

        return !finalResult.isEmpty() ? finalResult.toString().trim() : null;	//Ritorno il testo finale o null se vuoto
    }

    //Metodo privato che calcola l’età a partire dalla data di nascita
    private int calculateAge(String birthDate) {
        if (birthDate == null || birthDate.isEmpty()) return 0;	//Se la data è nulla o vuota ritorno 0
        try {
            Pattern pattern = Pattern.compile("(\\d{1,2})\\s+\\w+\\s+(\\d{4})");	//Creo un pattern regex per catturare giorno, mese e anno
            Matcher matcher = pattern.matcher(birthDate);	//Creo un matcher per applicare il pattern al testo della data
            if (matcher.find()) {	//Se trovo una corrispondenza
                int year = Integer.parseInt(matcher.group(2));	//Estraggo l’anno
                int currentYear = java.time.Year.now().getValue();	//Ottengo l’anno corrente
                return currentYear - year;	//Calcolo e ritorno l’età
            }
        } catch (Exception e) {	//Se si verifica un errore
            // Non faccio nulla, semplicemente ritornerò 0
        }
        return 0;	//Se non trovo l’anno o c’è un errore ritorno 0
    }

    //Metodo privato che cerca un giocatore nelle classifiche ATP e WTA come fallback
    private Player searchInRankings(String playerName) {
        System.out.println("🔄 Ricerca fallback nelle classifiche...");	//Stampo a console che sto usando la ricerca alternativa

        List<Player> atpPlayers = getATPRankings(100);	//Recupero i primi 100 giocatori ATP
        for (Player player : atpPlayers) {	//Itero su ogni giocatore ATP
            if (player.getNome().toLowerCase().contains(playerName.toLowerCase())) {	//Se il nome del giocatore contiene il testo cercato (case-insensitive)
                System.out.println("✅ Trovato in ATP rankings");	//Stampo conferma
                return player;	//Ritorno il giocatore trovato
            }
        }
        List<Player> wtaPlayers = getWTARankings(100);	//Recupero i primi 100 giocatori WTA
        for (Player player : wtaPlayers) {	//Itero su ogni giocatore WTA
            if (player.getNome().toLowerCase().contains(playerName.toLowerCase())) {	//Se il nome del giocatore contiene il testo cercato
                System.out.println("✅ Trovato in WTA rankings");	//Stampo conferma
                return player;	//Ritorno il giocatore trovato
            }
        }
        return null;	//Se non trovo nulla ritorno null
    }

    //Metodo per estrarre l'immagine di un giocatore da Wikipedia (utilizzato per ricerca giocatore e H2H)
    private String extractImageUrl(Element table) {
        Elements images = table.select("img");	//Seleziono tutte le immagini presenti nella tabella
        for (Element img : images) {	//Itero su ogni immagine
            String src = img.attr("src");	//Recupero l’attributo src dell’immagine
            if (src.contains("Flag_of") || src.contains("icon") ||
                    src.contains("logo") || src.length() < 20) {	//Se l’immagine è troppo piccola o è una bandiera/icone/loghi
                continue;	//Ignoro questa immagine e passo alla successiva
            }
            if (src.startsWith("//")) {	//Se l’URL inizia con "//" (protocol-relative)
                return "https:" + src;	//Aggiungo https davanti e ritorno
            } else if (src.startsWith("/")) {	//Se l’URL è relativo al dominio
                return "https://it.wikipedia.org" + src;	//Aggiungo il dominio italiano di Wikipedia e ritorno
            } else if (src.startsWith("http")) {	//Se è già un URL completo
                return src;	//Ritorno così com’è
            }
        }
        return null;	//Se non trovo immagini valide ritorno null
    }

    //==================== HEAD TO HEAD (SCAPRING MATCHSTAT CON IMMAGINI WIKIPEDIA) ====================
    //Metodo pubblico per ottenere dati H2H tra due giocatori
    public H2HData getH2HData(String player1, String player2) {
        H2HData h2hData = new H2HData();	//Creo un oggetto H2HData per salvare i dati

        try {	//Blocco try per gestire eventuali errori
            //OTTENGO STATISTICHE DA MATCHSTAT
            String formattedPlayer1 = formatPlayerNameForURL(player1);	//Formatto il nome del primo giocatore per l’URL
            String formattedPlayer2 = formatPlayerNameForURL(player2);	//Formatto il nome del secondo giocatore

            String url = String.format("https://matchstat.com/tennis/h2h-odds-bets/%s/%s/",
                    formattedPlayer1, formattedPlayer2);	//Costruisco l’URL H2H

            System.out.println("🔍 Recupero H2H da: " + url);	//Stampo l’URL da cui recuperare i dati

            Request request = new Request.Builder()	//Creo una richiesta HTTP
                    .url(url)	//Imposto l’URL
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")	//Imposto User-Agent
                    .build();	//Costruisco la richiesta

            try (Response response = client.newCall(request).execute()) {	//Eseguo la richiesta
                if (!response.isSuccessful() || response.body() == null) {	//Se la risposta non è valida
                    System.out.println("❌ Errore HTTP: " + response.code());	//Stampo errore
                    return null;	//Ritorno null
                }

                String html = response.body().string();	//Leggo il corpo della risposta
                Document doc = Jsoup.parse(html);	//Parso l’HTML con Jsoup

                h2hData.setPlayer1Name(player1);	//Imposto i nomi dei giocatori inizialmente
                h2hData.setPlayer2Name(player2);

                //Nomi giocatori da Matchstat
                Elements playerNames = doc.select(".table-player__name, .player-name, h2.player-name, .h2h-player-name");	//Seleziono elementi contenenti i nomi
                if (playerNames.size() >= 2) {	//Se ci sono almeno due nomi
                    String name1 = playerNames.get(0).text().trim();	//Estraggo nome giocatore 1
                    String name2 = playerNames.get(1).text().trim();	//Estraggo nome giocatore 2
                    if (!name1.isEmpty()) h2hData.setPlayer1Name(name1);	//Aggiorno il nome se valido
                    if (!name2.isEmpty()) h2hData.setPlayer2Name(name2);
                    System.out.println("✅ Giocatori trovati: " + h2hData.getPlayer1Name() + " vs " + h2hData.getPlayer2Name());
                }

                //Statistiche dalla tabella
                Elements statRows = doc.select("tr");	//Seleziono tutte le righe della tabella
                int statsFound = 0;	//Contatore delle statistiche estratte

                for (Element row : statRows) {	//Itero su ogni riga
                    Elements cells = row.select("td");	//Seleziono le celle della riga
                    if (cells.size() < 3) continue;	//Salto righe con meno di 3 celle

                    String stat1 = cells.get(0).text().trim();	//Prendo valore giocatore 1
                    String label = cells.get(1).text().trim();	//Prendo etichetta
                    String stat2 = cells.get(2).text().trim();	//Prendo valore giocatore 2

                    //Prize Money
                    if (label.contains("Career Prize Money") || label.contains("Prize Money")) {	//Se la riga riguarda i premi
                        h2hData.setPlayer1PrizeMoney(stat1);	//Imposto i valori
                        h2hData.setPlayer2PrizeMoney(stat2);
                        statsFound++;
                    }
                    //Career W/L
                    else if (label.contains("Career Total W/L") || label.contains("Career W/L") || label.contains("Total W/L")) {	//Record carriera
                        h2hData.setPlayer1WinLoss(extractWinLoss(stat1));
                        h2hData.setPlayer1WinPercentage(extractPercentage(stat1));
                        h2hData.setPlayer2WinLoss(extractWinLoss(stat2));
                        h2hData.setPlayer2WinPercentage(extractPercentage(stat2));
                        statsFound++;
                    }
                    //YTD W/L - Quando trovo questo finisco il parsing perché non mi interessano altri dati
                    else if (label.contains("YTD Win/Loss") || label.contains("YTD W/L")) {	//Se troviamo il record dell’anno corrente interrompi
                        break;
                    }
                    //Superfici
                    else if (label.equals("Clay") || label.contains("Clay")) {	//Statistiche su terra
                        h2hData.setPlayer1Clay(parseIntSafe(stat1));
                        h2hData.setPlayer2Clay(parseIntSafe(stat2));
                        statsFound++;
                    }
                    else if (label.equals("Hard") || label.contains("Hard")) {	//Statistiche su cemento
                        h2hData.setPlayer1Hard(parseIntSafe(stat1));
                        h2hData.setPlayer2Hard(parseIntSafe(stat2));
                        statsFound++;
                    }
                    else if (label.equals("Indoor") || label.contains("Indoor")) {	//Statistiche indoor
                        h2hData.setPlayer1Indoor(parseIntSafe(stat1));
                        h2hData.setPlayer2Indoor(parseIntSafe(stat2));
                        statsFound++;
                    }
                    else if (label.equals("Grass") || label.contains("Grass")) {	//Statistiche su erba
                        h2hData.setPlayer1Grass(parseIntSafe(stat1));
                        h2hData.setPlayer2Grass(parseIntSafe(stat2));
                        statsFound++;
                    }
                    //Titoli
                    else if (label.equals("Titles") || label.contains("Titles") || label.contains("Titoli")) {	//Titoli vinti
                        h2hData.setPlayer1Titles(parseIntSafe(stat1));
                        h2hData.setPlayer2Titles(parseIntSafe(stat2));
                        statsFound++;
                    }
                    //H2H Record
                    else if (label.contains("Total H2H Matches") || label.contains("H2H Matches") || label.contains("Total Matches")) {	//Record testa a testa
                        int p1Wins = parseIntSafe(stat1);
                        int p2Wins = parseIntSafe(stat2);
                        int totalH2H = p1Wins + p2Wins;
                        h2hData.setTotalH2HMatches(totalH2H);
                        h2hData.setH2hRecord(p1Wins + "-" + p2Wins);
                        statsFound++;
                        System.out.println("   ✅ H2H Record: " + h2hData.getH2hRecord());
                    }
                }
                System.out.println("✅ Statistiche estratte: " + statsFound);	//Stampo quante statistiche sono state trovate
            }

            //OTTENGO IMMAGINI DA WIKIPEDIA
            System.out.println("📸 Recupero immagini da Wikipedia...");

            String img1 = getPlayerImageFromWikipedia(player1);	//Recupero immagine giocatore 1
            String img2 = getPlayerImageFromWikipedia(player2);	//Recupero immagine giocatore 2

            if (img1 != null) {	//Se trovata
                h2hData.setPlayer1Image(img1);	//Imposto l’immagine
                System.out.println("✅ Immagine Player 1 trovata");
            } else {
                System.out.println("⚠️ Immagine Player 1 non trovata");
            }

            if (img2 != null) {	//Se trovata
                h2hData.setPlayer2Image(img2);	//Imposto l’immagine
                System.out.println("✅ Immagine Player 2 trovata");
            } else {
                System.out.println("⚠️ Immagine Player 2 non trovata");
            }

            return h2hData;	//Ritorno l’oggetto H2HData completo

        } catch (Exception e) {	//Gestione errori generali
            System.out.println("❌ Errore scraping H2H: " + e.getMessage());	//Stampo messaggio di errore
            e.printStackTrace();	//Stampo stack trace
            return null;	//Ritorno null in caso di errore
        }
    }

    //Metodo privato per recuperare l’immagine di un giocatore da Wikipedia
    private String getPlayerImageFromWikipedia(String playerName) {
        try {	//Blocco try per gestire errori
            String wikiName = formatWikipediaName(playerName);	//Formatto il nome del giocatore per l’URL di Wikipedia
            String url = "https://it.wikipedia.org/wiki/" + wikiName;	//Costruisco l’URL della pagina

            System.out.println("   📄 Tentativo Wikipedia: " + url);	//Stampo tentativo di accesso

            Request request = new Request.Builder()	//Creo richiesta HTTP
                    .url(url)	//Imposto URL
                    .addHeader("User-Agent", "Mozilla/5.0")	//Imposto User-Agent
                    .build();

            try (Response response = client.newCall(request).execute()) {	//Eseguo la richiesta
                if (!response.isSuccessful() || response.body() == null) {	//Se la risposta non è valida
                    System.out.println("   ⚠️ Pagina non trovata per: " + playerName);	//Stampo avviso
                    return null;	//Ritorno null
                }

                String html = response.body().string();	//Leggo il corpo della risposta
                Document doc = Jsoup.parse(html);	//Parso l’HTML con Jsoup

                //Verifico che sia un tennista
                if (!isTennisPlayer(doc)) {	//Controllo se la pagina appartiene a un tennista
                    System.out.println("   ⚠️ Non è un tennista: " + playerName);	//Stampo avviso
                    return null;	//Ritorno null
                }

                //Estraggo l'immagine dall'infobox
                Elements infobox = doc.select("table.infobox");	//Seleziono la tabella infobox
                if (!infobox.isEmpty()) {	//Se esiste
                    String imageUrl = extractImageUrl(infobox.first());	//Estraggo l’URL dell’immagine
                    if (imageUrl != null && !imageUrl.isEmpty()) {	//Se valida
                        System.out.println("   ✅ Immagine trovata per: " + playerName);	//Stampo conferma
                        return imageUrl;	//Ritorno l’URL dell’immagine
                    }
                }

                System.out.println("   ⚠️ Immagine non trovata nell'infobox per: " + playerName);	//Se non trovata
                return null;	//Ritorno null
            }

        } catch (Exception e) {	//Gestione errori
            System.out.println("   ❌ Errore recupero immagine per " + playerName + ": " + e.getMessage());	//Stampo errore
            return null;	//Ritorno null in caso di errore
        }
    }

    //Metodo privato per formattare il nome di un giocatore per l’URL di Matchstat
    private String formatPlayerNameForURL(String name) {
        if (name == null || name.isEmpty()) return "";	//Se il nome è null o vuoto ritorno stringa vuota
        String[] words = name.trim().split("\\s+");	//Divido il nome in parole
        StringBuilder formatted = new StringBuilder();	//Creo un StringBuilder per costruire il nome formattato
        for (int i = 0; i < words.length; i++) {	//Itero su ogni parola
            String word = words[i];
            if (!word.isEmpty()) {	//Se la parola non è vuota
                formatted.append(Character.toUpperCase(word.charAt(0)));	//Capitalizzo la prima lettera
                if (word.length() > 1) {
                    formatted.append(word.substring(1).toLowerCase());	//Aggiungo le lettere restanti in minuscolo
                }
                if (i < words.length - 1) {
                    formatted.append("%20");	//Sostituisco lo spazio con %20 tra le parole
                }
            }
        }
        return formatted.toString();	//Ritorno il nome formattato pronto per l’URL
    }

    //Metodo privato per estrarre una percentuale da un testo
    private String extractPercentage(String text) {
        if (text == null || text.isEmpty()) return "0%";	//Se il testo è null o vuoto ritorno "0%"
        Pattern pattern = Pattern.compile("(\\d+\\.?\\d*)%");	//Creo un pattern regex per catturare numeri interi o decimali seguiti dal simbolo %
        Matcher matcher = pattern.matcher(text);	//Creo il matcher sul testo
        if (matcher.find()) {	//Se trovo una corrispondenza
            return matcher.group(1) + "%";	//Ritorno il numero catturato aggiungendo il simbolo %
        }
        return "0%";	//Se non trovo nulla ritorno "0%"
    }

    //Metodo privato per estrarre il record vittorie-sconfitte da un testo
    private String extractWinLoss(String text) {
        if (text == null || text.isEmpty()) return "0-0";	//Se il testo è null o vuoto ritorno "0-0"
        Pattern pattern = Pattern.compile("\\((\\d+-\\d+)\\)");	//Creo un pattern regex per catturare il formato (vittorie-sconfitte)
        Matcher matcher = pattern.matcher(text);	//Creo il matcher sul testo
        if (matcher.find()) {	//Se trovo una corrispondenza
            return matcher.group(1);	//Ritorno il gruppo catturato (vittorie-sconfitte)
        }
        return "0-0";	//Se non trovo nulla ritorno "0-0"
    }

    //Metodo privato per convertire in intero in modo sicuro
    private int parseIntSafe(String text) {
        if (text == null || text.isEmpty()) return 0;	//Se il testo è null o vuoto ritorno 0
        try {
            text = text.replaceAll("[^0-9]", "");	//Rimuovo tutti i caratteri non numerici, lasciando solo le cifre
            return Integer.parseInt(text);	//Provo a convertire il testo in intero
        } catch (NumberFormatException e) {	//Se la conversione fallisce
            return 0;	//Ritorno 0 come valore di default
        }
    }

    //==================== PARTITE DI OGGI (SCAPRING DINAMICO SELENIUM DA SOFASCORE) ====================
    //Metodo pubblico per recuperare gli ultimi match dal sito Sofascore utilizzando Selenuim
    public List<Match> getRecentMatches() {
        List<Match> matches = new ArrayList<>();	//Creo una lista vuota per salvare i match
        ChromeOptions options = new ChromeOptions();	//Configuro le opzioni di ChromeDriver
        options.addArguments("--headless");	//Esecuzione in modalità headless (senza finestra)
        options.addArguments("--disable-blink-features=AutomationControlled");	//Disabilita rilevamento automazione
        options.addArguments("--window-size=1920,1080");	//Imposto dimensioni finestra
        WebDriver driver = new ChromeDriver(options);	//Creo l’istanza di WebDriver
        JavascriptExecutor js = (JavascriptExecutor) driver;	//Cast per eseguire JS
        Set<String> processedTexts = new HashSet<>();	//Set per evitare duplicati
        String currentTournament = "Generale";	//Torneo corrente
        String currentLocation = "";	//Luogo corrente
        boolean waitingForLocation = false;	//Flag per indicare che il prossimo testo è la location

        try {
            driver.get("https://www.sofascore.com/it/tennis");	//Accedo alla pagina dei match
            Thread.sleep(2000);	//Piccola pausa per il caricamento
            int maxScrolls = 180;	//Numero massimo di scroll per caricare contenuti
            for (int scroll = 0; scroll < maxScrolls; scroll++) {
                List<WebElement> elements = driver.findElements(By.cssSelector("a[href^='/it/tennis/']"));	//Seleziono tutti i link rilevanti
                for (WebElement el : elements) {
                    try {
                        String text = el.getText().trim();	//Recupero testo dell’elemento
                        if (text.isEmpty()) continue;	//Ignoro testo vuoto
                        //───── TITOLO TORNEO ─────
                        if (isTournamentTitle(text)) {	//Se è titolo torneo
                            currentTournament = text;	//Aggiorno torneo corrente
                            currentLocation = "";	//Resetto location
                            waitingForLocation = true;	//Flag per leggere location al prossimo testo
                            continue;
                        }
                        //───── LUOGO TORNEO (non funziona ma comunque fa una prova) ─────
                        if (waitingForLocation && currentLocation.isEmpty() && isLocationLine(text)) {	//Se attendo la location
                            currentLocation = text;	//Aggiorno location
                            waitingForLocation = false;	//Resetto flag
                            continue;
                        }
                        //───── MATCH ─────
                        if (!text.contains("\n")) continue;	//Ignoro elementi che non contengono dati match
                        if (!processedTexts.add(text)) continue;	//Ignoro duplicati
                        MatchTextData data = parseMatchText(text);	//Estraggo i dati del match
                        if (!data.isValid() || data.time.isEmpty() || !data.hasValidStatus()) continue;	//Se non valido, salto
                        //Creo il match impostando i dati corretti
                        Match match = new Match(
                                currentTournament,
                                currentLocation,
                                data.players.get(0),
                                data.players.get(1),
                                data.time,
                                data.time,
                                0
                        );
                        match.setStatus(data.status);	//Imposto lo status del match
                        //Salvo il punteggio del game corrente se LIVE
                        if (data.currentGame != null && !data.currentGame.isEmpty()) {
                            match.setCurrentGame(data.currentGame);	//Imposto punteggio game corrente
                        }
                        //Punteggio dettagliato
                        if (!data.scores.isEmpty()) {
                            String scoreString = String.join(" ", data.scores);	//Combino i set in una stringa
                            match.setDetailedScore(scoreString);	//Imposto punteggio dettagliato
                            //Se è finita, calcolo il numero di set vinti da ciascuno
                            if (data.status.equals("FINE") || data.status.equals("A tavolino")) {
                                int player1Sets = 0;
                                int player2Sets = 0;
                                for (String set : data.scores) {	//Itero sui set
                                    String[] parts = set.replaceAll("\\(\\d+\\)", "").split("-");	//Rimuovo eventuali numeri tra parentesi e split
                                    if (parts.length != 2) continue;
                                    try {
                                        int score1 = Integer.parseInt(parts[0].trim());	//Parso punteggio giocatore 1
                                        int score2 = Integer.parseInt(parts[1].trim());	//Parso punteggio giocatore 2

                                        if (score1 > score2) player1Sets++;	//Incremento set vinti
                                        else if (score2 > score1) player2Sets++;
                                    } catch (NumberFormatException e) {
                                        //Ignoro set non valido
                                    }
                                }
                                int maxSets = Math.max(player1Sets, player2Sets);
                                int minSets = Math.min(player1Sets, player2Sets);
                                match.setSetScore(maxSets + "-" + minSets);	//Imposto punteggio set
                            }
                        }
                        //Determino il vincitore se partita finita
                        if (data.status.equals("FINE") || data.status.equals("A tavolino")) {
                            String winner = determineWinner(data);	//Calcolo vincitore
                            if (winner != null) {
                                match.setWinner(winner);	//Imposto vincitore
                            }
                        }
                        matches.add(match);	//Aggiungo il match alla lista
                    } catch (StaleElementReferenceException ignored) {}	//Ignoro eccezioni di elementi non più presenti
                }
                js.executeScript("window.scrollBy(0, 400);");	//Scroll verso il basso per caricare nuovi elementi
                Thread.sleep(50);	//Piccola pausa
            }
        } catch (StopScraperException e) {	//Gestisco interruzioni personalizzate
            System.out.println("⛔ " + e.getMessage());
        } catch (Exception e) {	//Gestione errori generali
            e.printStackTrace();
        } finally {
            driver.quit();	//Chiudo il driver in ogni caso
        }
        return matches;	//Ritorno la lista di match
    }

    //Metodo statico privato per indicare se una riga rappresenta una location
    private static boolean isLocationLine(String text) {
        return text.matches("^[A-Za-z .'-]+,\\s*[A-Za-z .'-]+.*$");	//Verifica se il testo ha il formato "Città, Nazione" con eventuali caratteri speciali, spazi o punti
    }

    //Metodo privato utilizzato per determinare il vincitore di un match
    private String determineWinner(MatchTextData data) {	//Riceve un oggetto MatchTextData contenente punteggi e giocatori
        if (data.players.size() < 2 || data.scores.isEmpty()) {	//Se ci sono meno di 2 giocatori o nessun set
            return null;	//Non è possibile determinare il vincitore
        }
        int player1Sets = 0;	//Contatore set vinti dal giocatore 1
        int player2Sets = 0;	//Contatore set vinti dal giocatore 2
        //Analizzo ogni set (formato: "6-4" o "7-6(3)")
        for (String setScore : data.scores) {
            String[] parts = setScore.replaceAll("\\(\\d+\\)", "").split("-");	//Rimuovo tie-break tra parentesi e divido i punteggi
            if (parts.length != 2) continue;	//Ignoro set non validi

            try {
                int score1 = Integer.parseInt(parts[0].trim());	//Parso punteggio giocatore 1
                int score2 = Integer.parseInt(parts[1].trim());	//Parso punteggio giocatore 2
                if (score1 > score2) {	//Se giocatore 1 ha vinto il set
                    player1Sets++;
                } else if (score2 > score1) {	//Se giocatore 2 ha vinto il set
                    player2Sets++;
                }
            } catch (NumberFormatException e) {
                //Ignoro set non validi
            }
        }
        //Chi ha vinto più set ha vinto la partita
        if (player1Sets > player2Sets) {	//Giocatore 1 ha vinto più set
            return data.players.get(0);
        } else if (player2Sets > player1Sets) {	//Giocatore 2 ha vinto più set
            return data.players.get(1);
        }
        return null; //Caso di parità, ma dovrebbe essere impossibile
    }

    //Metodo statico privato che crea un oggetto MatchTextData da una Stringa ottenuta dal parsing
    private static MatchTextData parseMatchText(String text) {
        MatchTextData data = new MatchTextData();	//Creo un nuovo oggetto per memorizzare i dati del match
        String[] lines = text.split("\n");	//Divido il testo in righe, ogni riga contiene un'informazione diversa
        List<String> allNumbers = new ArrayList<>();	//Creo una lista temporanea per salvare numeri o punti live
        for (String line : lines) {
            line = line.trim();	//Rimuovo spazi iniziali e finali
            if (line.isEmpty()) continue;	//Salto righe vuote
            if (line.matches("\\d{1,2}:\\d{2}")) data.time = line;	//Se la riga è orario (HH:MM), la salvo
            else if (line.matches("LIVE|FINE|SRF|A tavolino|.*set|-|Annullata|Iniziato")) data.status = line;	//Se è status, lo salvo
            else if (line.matches("\\d{1,2}-\\d{1,2}(\\(\\d+\\))?")) data.scores.add(line);	//Se è punteggio completo, lo aggiungo
            else if (line.matches("\\d+|A")) allNumbers.add(line);	//Se è numero o "A", lo salvo in allNumbers
            else data.players.add(line);	//Altrimenti considero la riga come nome di un giocatore
        }
        //Se non ho punteggi già formattati ma ho numeri
        if (data.scores.isEmpty() && !allNumbers.isEmpty()) {
            boolean isLive = data.status.equals("LIVE") || data.status.matches("[1-5]º set");	//Controllo se il match è live
            if (isLive && allNumbers.size() >= 2) data.currentGame = allNumbers.get(0) + "-" + allNumbers.get(1);	//Salvo punteggio live dei primi due numeri
            data.scores = parseScoreNumbers(allNumbers, isLive);	//Ricostruisco i punteggi dai numeri
        }
        return data;	//Ritorno l'oggetto con tutti i dati parsati
    }

    //Metodo statico privato che da una stringa di numeri apparentemente senza significato, ritorna i set corretti (es. 40 40 6 5 3 2 1 0 --> game in corso: 40-40, set: 6-3 5-2)
    private static List<String> parseScoreNumbers(List<String> allNumbers, boolean isLive) {
        List<String> sets = new ArrayList<>();	//Creo la lista dei set finali da restituire
        //Se LIVE, scarto i primi due valori perché li ho già salvati in currentGame
        List<String> setNumbers;
        if (isLive && allNumbers.size() > 2) setNumbers = allNumbers.subList(2, allNumbers.size());
        else setNumbers = new ArrayList<>(allNumbers);	//Altrimenti uso tutti i numeri
        //Converti solo valori numerici per i set
        List<Integer> scores = new ArrayList<>();
        for (String s : setNumbers) {
            try {
                int num = Integer.parseInt(s);
                scores.add(num);	//Salvo il numero valido
            } catch (NumberFormatException e) {
                //Ignoro lettere come "A" nei punteggi dei set
            }
        }
        //Rimuovo gli ultimi due numeri se rappresentano il punteggio totale dei set
        if (scores.size() >= 2) scores = scores.subList(0, scores.size() - 2);
        //Ignoro l'ultimo numero se la lista è dispari
        if (scores.size() % 2 != 0) scores = scores.subList(0, scores.size() - 1);
        if (scores.isEmpty()) return sets;	//Se non ci sono punteggi, ritorno lista vuota
        int numSets = scores.size() / 2;	//Calcolo il numero di set
        //Ricostruisco i set: player1[i] vs player2[i]
        for (int i = 0; i < numSets; i++) {
            int score1 = scores.get(i);
            int score2 = scores.get(i + numSets);
            if (isValidTennisScore(score1, score2)) {
                String setScore = score1 + "-" + score2;
                sets.add(setScore);	//Aggiungo il set valido alla lista
                //Se il set è 7-6 o 6-7, aggiungo il tiebreak
                if ((score1 == 7 && score2 == 6) || (score1 == 6 && score2 == 7)) {
                    if (i + 1 < numSets) {
                        int nextNum = Math.min(scores.get(i + 1), scores.get(i + numSets + 1));
                        sets.set(sets.size() - 1, setScore + "(" + nextNum + ")");	//Aggiungo il tiebreak al set
                        i++;	//Salto il prossimo numero perché già usato per il tiebreak
                    }
                }
            } else {
                break;	//Se il set non è valido, interrompo il parsing
            }
        }
        return sets;	//Ritorno la lista dei set ricostruiti
    }

    //Metodo statico privato per verificare la correttezza di un set
    private static boolean isValidTennisScore(int score1, int score2) {
        return score1 >= 0 && score2 >= 0;  //Controllo che i valori dei set siano positivi
    }

    //Classe interna di supporto per salvare temporaneamente le informazioni sul Match
    private static class MatchTextData {
        String time = "";	//Salvo l’orario del match
        List<String> players = new ArrayList<>();	//Memorizzo i nomi dei giocatori
        List<String> scores = new ArrayList<>();	//Contengo i punteggi dei set
        String status = "";	//Indico lo stato del match (LIVE, FINE, ecc.)
        String currentGame = "";	//Memorizzo il punteggio del game corrente (LIVE)

        boolean isValid() {
            return players.size() >= 2;	//Controllo che ci siano almeno due giocatori
        }

        boolean hasValidStatus() {
            if (status == null || status.isEmpty()) return false;	//Scarto stati nulli o vuoti
            return status.equals("LIVE") || status.equals("FINE") || status.equals("A tavolino") ||
                    status.equals("SRF") || status.equals("Annullata") || status.equals("Iniziato") ||
                    status.equals("-") || status.contains("set");	//Verifico che lo stato sia riconosciuto
        }
    }

    //Metodo statico privato per verificare che il torneo trovato sia tra quelli validi e non sia 'poco interessante'
    private static boolean isTournamentTitle(String text) {
        String upper = text.toUpperCase();	//Converto il testo in maiuscolo per controlli case-insensitive
        if (upper.contains("ATP 125") || upper.contains("WTA 125") ||
                upper.contains("ITF") || upper.contains("CHALLENGER")) {
            throw new StopScraperException("Torneo non interessante: " + text);	//Blocco subito i tornei che non mi interessano
        }
        String[] allowedTournaments = {
                "Grande Slam", "Masters 1000", "ATP 250", "ATP 500", "WTA 250",
                "WTA 500", "WTA 1000", "United Cup"
        };	//Definisco la lista dei tornei accettati
        for (String t : allowedTournaments) {
            if (text.equals(t) || text.startsWith(t)) return true;	//Accetto il torneo se corrisponde o inizia con un nome valido
        }
        return false;	//Rifiuto tutto ciò che non rientra nei tornei consentiti
    }

    //Eccezione personalizzata per interrompere lo scraping nel caso si trovi un torneo non interessante
    private static class StopScraperException extends RuntimeException {
        public StopScraperException(String message) {
            super(message);	//Passo il messaggio all'eccezione RuntimeException
        }
    }
}