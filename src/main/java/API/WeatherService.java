package API;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.util.logging.Logger;

//Classe WeatherService per la gestione delle richieste all'API per ottenere le informazioni meteo della città
public class WeatherService {
    private final String apiKey;	//Memorizzo la chiave API per OpenWeather
    private final OkHttpClient client;	//Creo il client HTTP per fare richieste
    private final Logger logger = Logger.getLogger(WeatherService.class.getName());	//Creo un logger per loggare informazioni

    //Costruttore che riceve la chiave API
    public WeatherService(String apiKey) {
        this.apiKey = apiKey;	//Inizializzo la chiave API
        this.client = new OkHttpClient();	//Inizializzo il client HTTP
    }

    //Metodo per ottenere il meteo attuale di una città
    public String getCurrentWeather(String city) {
        if (apiKey == null || apiKey.isEmpty()) {	//Controllo se la chiave API è assente
            return "❌ API KEY OpenWeather non configurata.";	//Ritorno messaggio di errore se manca la chiave
        }

        try {
            String normalizedCity = normalizeCity(city);	//Normalizzo il nome della città (rimuovo caratteri speciali, spazi ecc.)

            String url = String.format(	//Costruisco l'URL per la richiesta OpenWeather
                    "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric&lang=it",
                    normalizedCity, apiKey
            );

            Request request = new Request.Builder()	//Creo la richiesta HTTP
                    .url(url)	//Imposto l'URL
                    .header("User-Agent", "WeatherBot/1.0")	//Imposto l'header User-Agent
                    .build();

            try (Response response = client.newCall(request).execute()) {	//Eseguo la richiesta e ottengo la risposta
                if (!response.isSuccessful()) {	//Controllo se la risposta non è positiva
                    if (response.code() == 404) {	//Se il codice è 404
                        return "❌ Città non trovata.\nProva a specificare la provincia (es: Como,CO)";	//Ritorno messaggio città non trovata
                    }
                    logger.severe("Errore OpenWeather: " + response.code());	//Loggo errore con codice risposta
                    return "❌ Errore OpenWeather\nCodice: " + response.code();	//Ritorno messaggio di errore generico
                }
                String jsonResponse = response.body().string();	//Leggo il corpo della risposta come stringa
                return formatWeatherResponse(jsonResponse);	//Formatto e ritorno la risposta meteo
            }

        } catch (IOException e) {	//Gestisco eccezioni di connessione
            logger.severe("Errore connessione meteo: " + e.getMessage());	//Loggo il messaggio di errore
            return "⚠️ Errore di connessione al servizio meteo.";	//Ritorno messaggio di errore
        }
    }

    //Metodo privato per normalizzare il nome della città
    private String normalizeCity(String city) {
        city = city.trim();	//Rimuovo spazi iniziali/finali
        city = city.replace("/meteo", "");	//Rimuovo eventuale comando /meteo
        city = city.replace("'", "")	//Rimuovo apostrofi singoli
                .replace("’", "")	//Rimuovo apostrofi speciali
                .replace("à", "a")	//Sostituisco lettere accentate
                .replace("è", "e")
                .replace("ì", "i")
                .replace("ò", "o")
                .replace("ù", "u");
        return city;	//Ritorno la città normalizzata
    }

    //Metodo privato per formattare la risposta JSON
    private String formatWeatherResponse(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();	//Parso la stringa JSON in oggetto

        String cityName = obj.get("name").getAsString();	//Estraggo il nome della città
        String description = obj.getAsJsonArray("weather")	//Estraggo la descrizione meteo
                .get(0).getAsJsonObject()
                .get("description").getAsString();

        double temp = obj.getAsJsonObject("main").get("temp").getAsDouble();	//Estraggo temperatura attuale
        double feelsLike = obj.getAsJsonObject("main").get("feels_like").getAsDouble();	//Estraggo temperatura percepita
        int humidity = obj.getAsJsonObject("main").get("humidity").getAsInt();	//Estraggo umidità
        double wind = obj.getAsJsonObject("wind").get("speed").getAsDouble();	//Estraggo velocità vento

        return "🌤 METEO ATTUALE\n\n" +	//Costruisco stringa finale formattata
                "📍 Città: " + cityName + "\n" +
                "🌡 Temperatura: " + temp + "°C\n" +
                "🤔 Percepita: " + feelsLike + "°C\n" +
                "☁️ Condizioni: " + capitalize(description) + "\n" +
                "💧 Umidità: " + humidity + "%\n" +
                "💨 Vento: " + wind + " m/s\n\n" +
                "🕒 Aggiornato in tempo reale";
    }

    private String capitalize(String text) {	//Metodo per capitalizzare la prima lettera di una stringa
        if (text == null || text.isEmpty()) return text;	//Se testo vuoto o null, ritorno così com'è
        return text.substring(0, 1).toUpperCase() + text.substring(1);	//Capitalizzo la prima lettera e ritorno
    }
}