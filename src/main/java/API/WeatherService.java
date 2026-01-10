package API;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.logging.Logger;

public class WeatherService {

    private final String apiKey;
    private final OkHttpClient client;
    private final Logger logger = Logger.getLogger(WeatherService.class.getName());

    public WeatherService(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient();
    }

    public String getCurrentWeather(String city) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "❌ API KEY OpenWeather non configurata.";
        }

        try {
            // Pulisci e normalizza la città
            String normalizedCity = normalizeCity(city);

            String url = String.format(
                    "https://api.openweathermap.org/data/2.5/weather?q=%s&appid=%s&units=metric&lang=it",
                    normalizedCity, apiKey
            );

            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "WeatherBot/1.0")
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (!response.isSuccessful()) {
                    if (response.code() == 404) {
                        return "❌ Città non trovata.\nProva a specificare la provincia (es: Como,CO)";
                    }
                    logger.severe("Errore OpenWeather: " + response.code());
                    return "❌ Errore OpenWeather\nCodice: " + response.code();
                }

                String jsonResponse = response.body().string();
                return formatWeatherResponse(jsonResponse);

            }

        } catch (IOException e) {
            logger.severe("Errore connessione meteo: " + e.getMessage());
            return "⚠️ Errore di connessione al servizio meteo.";
        }
    }

    private String normalizeCity(String city) {
        city = city.trim();
        city = city.replace("/meteo", "");
        city = city.replace("'", "")
                .replace("’", "")
                .replace("à", "a")
                .replace("è", "e")
                .replace("ì", "i")
                .replace("ò", "o")
                .replace("ù", "u");
        return city;
    }

    private String formatWeatherResponse(String json) {
        JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

        String cityName = obj.get("name").getAsString();
        String description = obj.getAsJsonArray("weather")
                .get(0).getAsJsonObject()
                .get("description").getAsString();

        double temp = obj.getAsJsonObject("main").get("temp").getAsDouble();
        double feelsLike = obj.getAsJsonObject("main").get("feels_like").getAsDouble();
        int humidity = obj.getAsJsonObject("main").get("humidity").getAsInt();
        double wind = obj.getAsJsonObject("wind").get("speed").getAsDouble();

        return "🌤 METEO ATTUALE\n\n" +
                "📍 Città: " + cityName + "\n" +
                "🌡 Temperatura: " + temp + "°C\n" +
                "🤔 Percepita: " + feelsLike + "°C\n" +
                "☁️ Condizioni: " + capitalize(description) + "\n" +
                "💧 Umidità: " + humidity + "%\n" +
                "💨 Vento: " + wind + " m/s\n\n" +
                "🕒 Aggiornato in tempo reale";
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}
