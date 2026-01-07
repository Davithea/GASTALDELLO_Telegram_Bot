import bot.BotTelegramGastaldello;
import config.MyConfiguration;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Main {
    public static void main(String[] args) {
        System.out.println("🎾 Avvio Tennis Bot...");

        MyConfiguration myConfiguration = MyConfiguration.getInstance();

        try {
            String botToken = myConfiguration.getProperty("BOT_TOKEN");
            String rapidApiKey = myConfiguration.getProperty("RAPID_API_KEY");

            if (botToken == null || botToken.equals("inserisci_qui_il_token_bot")) {
                System.err.println("❌ ERRORE: BOT_TOKEN non configurato!");
                System.err.println("Modifica il file config.properties e inserisci il token del bot.");
                System.exit(-1);
            }

            if (rapidApiKey == null || rapidApiKey.isEmpty()) {
                System.err.println("❌ ERRORE: RAPID_API_KEY non configurata!");
                System.err.println("Registrati su https://rapidapi.com/");
                System.err.println("Inserisci la tua RapidAPI key nel file config.properties");
                System.exit(-1);
            }

            TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
            BotTelegramGastaldello bot = new BotTelegramGastaldello(botToken, rapidApiKey);
            botsApplication.registerBot(botToken, bot);

            System.out.println("✅ Tennis Bot avviato correttamente!");
            System.out.println("📱 Il bot è ora in ascolto...");
            System.out.println("📊 Classifiche: Wikipedia (scraping)");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🛑 Arresto Tennis Bot...");
            }));

        } catch(TelegramApiException e){
            System.err.println("❌ Errore nell'avvio del bot:");
            e.printStackTrace();
            System.exit(-1);
        }
    }
}