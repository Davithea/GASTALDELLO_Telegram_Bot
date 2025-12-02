import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Main {
    public static void main(String[] args) {
        MyConfiguration myConfiguration = MyConfiguration.getInstance();

        try {
            String botToken = myConfiguration.getProperty("BOT_TOKEN");
            TelegramBotsLongPollingApplication botsApplication = new TelegramBotsLongPollingApplication();
            botsApplication.registerBot(botToken, new BotTelegramGastaldello());
        } catch(TelegramApiException e){
            e.printStackTrace();
            System.exit(-1);
        }
    }
}