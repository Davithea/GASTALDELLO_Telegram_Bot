import bot.BotTelegramGastaldello;
import config.MyConfiguration;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

//Classe Main per l'avvio del codice e del bot
public class Main {
    public static void main(String[] args) {
        System.out.println("🎾 Avvio Tennis Bot...");    //Stampo il messaggio che indica che sto avviando il bot
        MyConfiguration myConfiguration = MyConfiguration.getInstance();    //Prendo un'istanza del file di configurazione

        try {
            String botToken = myConfiguration.getProperty("BOT_TOKEN"); //Prelevo il BOT_TOKEN dal file config.properties
            String apiKey = myConfiguration.getProperty("API_KEY"); //Prelevo l'API_KEY dal file config.properties

            if (botToken == null || botToken.isEmpty() || botToken.equals("inserisci_qui_il_token_bot")) {    //Se viene prelevato come bot token la frase di default o non è presente
                System.err.println("❌ ERRORE: BOT_TOKEN non configurato!"); //Viene stampato il fatto che non è stato configurato alcun BOT_TOKEN
                System.err.println("Modifica il file config.properties e inserisci il token del bot"); //Viene indicato l'obbligo di inserimento
                System.exit(-1);    //Il codice termina con codice di stato -1
            }

            if (apiKey == null || apiKey.isEmpty() || apiKey.equals("inserisci_qui_l'api_key")) {   //Se viene prelevata come api key la frase di default o non è presente
                System.err.println("❌ ERRORE: API_KEY non configurata!");   //Viene stampato il fatto che non è stata configurata alcuna API_KEY
                System.err.println("Registrati su https://openweathermap.org/api"); //Istruzioni per registrarsi a Open Weather
                System.err.println("Inserisci la tua API_KEY nel file config.properties");  //Viene indicato l'obbligo di inserimento
                System.exit(-1);    //Il codice termina con codice di stato -1
            }

            TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();  //Crea l'applicazione per la gestione dei bot Telegram basata su Long Polling
            BotTelegramGastaldello bot = new BotTelegramGastaldello(botToken, apiKey);  //Crea un'istanza della classe BotTelegramGastaldello passando come parametri BOT_TOKEN e API_KEY
            botsApplication.registerBot(botToken, bot); //Registra il bot su Telegram

            //Stampo messaggi di Stato
            System.out.println("✅ Tennis Bot avviato correttamente!");
            System.out.println("📱 Il bot è ora in ascolto...");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> { //Registra un thread di shutdown che viene eseguito automaticamente quando la JVM sta per terminare l'applicazione
                System.out.println("\n🛑 Arresto Tennis Bot...");    //Stampo lo stato di arresto
            }));
        } catch(TelegramApiException e) {
            System.err.println("❌ Errore nell'avvio del bot:"); //Stampo messaggio di errore in caso di errore nell'avvio del bot
            e.printStackTrace();    //Stampo informazioni sull'errore per la gestione del debug
            System.exit(-1);    //Il codice termina con codice di stato -1
        }
    }
}
