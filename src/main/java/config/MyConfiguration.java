package config;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

//Classe MyConfiguration per la gestione della configurazione del bot
public class MyConfiguration {
    private static MyConfiguration instance;    //Dichiaro l'istanza statica per implementare il Singleton
    private Configurations configs = new Configurations();  //Creo l'oggetto che mi permette di caricare i file di configurazione
    private Configuration config;   //Dichiaro l'oggetto che conterrà la configurazione caricata

    //Costruttore privato usato per impedire che venga istanziata una classe direttamente
    private MyConfiguration() {
        try {   //Provo a caricare il file di configurazione
            config = configs.properties("config.properties");   //Carico il file config.properties
        } catch (ConfigurationException e) {    //Gestisco l'errore se il file non è disponibile
            System.err.println("File non disponibile"); //Stampo un messaggio di errore
            System.exit(-1);    //Termino l'applicazione con codice -1
        }
    }

    //Metodo che restituisce l'unica istanza della classe implementando il pattern Singleton
    public static MyConfiguration getInstance() {
        if(instance == null) {  //Controllo se l'istanza non è ancora stata creata
            instance = new MyConfiguration();   //Creo l'istanza della classe
        }
        return instance;    //Ritorno l'istanza esistente o appena creata
    }

    //Metodo per ottenere una proprietà tramite chiave
    public String getProperty(String key) {
        return config.getString(key);   //Ritorno il valore associato alla chiave dal file di configurazione
    }
}