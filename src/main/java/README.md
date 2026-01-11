# 🎾 Tennis Bot Telegram

> **Bot Telegram completo per statistiche tennis in tempo reale**  
> Classifiche ATP/WTA, partite live, ricerca giocatori, Head-to-Head, meteo e molto altro!

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Telegram](https://img.shields.io/badge/Telegram-Bot%20API-blue.svg)](https://core.telegram.org/bots)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

---

## 📋 Indice

- [Caratteristiche](#-caratteristiche)
- [Demo](#-demo)
- [Prerequisiti](#-prerequisiti)
- [Installazione](#-installazione)
- [Configurazione](#-configurazione)
- [Comandi Disponibili](#-comandi-disponibili)
- [Architettura](#-architettura)
- [Database](#-database)
- [Tecnologie Utilizzate](#-tecnologie-utilizzate)
- [Sviluppi Futuri](#-sviluppi-futuri)
- [Contribuire](#-contribuire)
- [Licenza](#-licenza)

---

## ✨ Caratteristiche

### 🏆 Classifiche in Tempo Reale
- **ATP Singles** - Top 10 classifica mondiale maschile
- **WTA Singles** - Top 10 classifica mondiale femminile
- **ATP Doubles** - Top 10 doppio maschile
- **WTA Doubles** - Top 10 doppio femminile
- **Race to Turin** - Classifica annuale per le ATP Finals

### 📅 Partite Live
- Monitoraggio partite in corso (LIVE)
- Punteggio aggiornato set per set
- Punteggio game corrente
- Partite terminate con vincitore
- Filtro automatico tornei rilevanti (Grand Slam, Masters 1000, ATP/WTA 500/250)

### 🔍 Ricerca Giocatori
- Ricerca dettagliata su Wikipedia
- Informazioni complete: altezza, peso, età, ranking, titoli
- Statistiche carriera (vittorie/sconfitte, montepremi)
- Palmares Grand Slam
- Foto profilo giocatore

### ⚔️ Head to Head (H2H)
- Confronto diretto tra due giocatori
- Record scontri diretti
- Statistiche per superficie (Clay, Hard, Grass, Indoor)
- Montepremi totale
- Titoli vinti
- Foto dei giocatori

### ⭐ Sistema Preferiti
- Aggiungi giocatori preferiti
- Visualizza lista personalizzata
- Informazioni dettagliate salvate

### 🌤 Meteo
- Meteo in tempo reale per città torneo
- Temperatura, umidità, vento
- Condizioni meteo aggiornate

### 📊 Statistiche Personali
- Tracking utilizzo bot
- Comando più utilizzato
- Statistiche globali database

---

## 🎬 Demo

### Menu Principale
```
🎾 Benvenuto nel Tennis Bot!

Comandi disponibili:
 🏆  /classificaatp - Top 10 ATP
 🏁  /racetoturin - Top 10 Race
👨👨 /classificaatpdoppio - Top 10 ATP doppio
 👩  /classificawta - Top 10 WTA
👩👩 /classificawtadoppio - Top 10 WTA doppio
 📅  /partite - Partite di oggi
 🔍  /cerca - Cerca giocatore
 ⚔️  /h2h - Confronta due giocatori
 ⛅  /meteo - Meteo città tornei
 ⭐  /preferiti - I tuoi preferiti
```

### Esempio Partite Live
```
🎾 PARTITE DI OGGI

🏆 Australian Open
📍 Melbourne, Australia
─────────────────────
👤 Jannik Sinner vs Novak Djokovic
🔴 LIVE - Game: 40-30
📊 Set: 6-4 3-2

👤 Carlos Alcaraz vs Daniil Medvedev
🏆 Alcaraz b. Medvedev 2-1
📊 Punteggio: 6-4 3-6 7-5
```

---

## 🔧 Prerequisiti

- **Java 17+** ([Download JDK](https://www.oracle.com/java/technologies/downloads/))
- **Maven** ([Download Maven](https://maven.apache.org/download.cgi))
- **ChromeDriver** (per scraping Selenium) ([Download ChromeDriver](https://chromedriver.chromium.org/downloads))
- **Telegram Bot Token** ([Crea bot con @BotFather](https://t.me/BotFather))
- **OpenWeather API Key** (opzionale) ([Registrati gratis](https://openweathermap.org/api))

---

## 📥 Installazione

### 1️⃣ Clona il repository
```bash
git clone https://github.com/tuo-username/tennis-bot.git
cd tennis-bot
```

### 2️⃣ Configura le dipendenze
```bash
mvn clean install
```

### 3️⃣ Installa ChromeDriver
- **Windows**: Scarica ChromeDriver e aggiungi al PATH
- **macOS**:
  ```bash
  brew install chromedriver
  ```
- **Linux**:
  ```bash
  sudo apt install chromium-chromedriver
  ```

---

## ⚙️ Configurazione

### 1️⃣ Crea il file `.env` (o configura direttamente nel codice)

Crea un file `config.properties` nella root del progetto:

```properties
# Telegram Bot Configuration
telegram.bot.token=YOUR_TELEGRAM_BOT_TOKEN

# OpenWeather API (opzionale)
openweather.api.key=YOUR_OPENWEATHER_API_KEY
```

### 2️⃣ Ottieni il Bot Token

1. Apri Telegram e cerca **@BotFather**
2. Invia `/newbot`
3. Segui le istruzioni e copia il **token**
4. Incollalo in `config.properties`

### 3️⃣ Ottieni API Key OpenWeather (opzionale)

1. Vai su [OpenWeatherMap](https://openweathermap.org/api)
2. Registrati gratuitamente
3. Copia la tua **API Key**
4. Incollala in `config.properties`

---

## 🚀 Avvio

### Avvio Manuale
```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="Main"
```

### Avvio con JAR
```bash
mvn clean package
java -jar target/tennis-bot-1.0.jar
```

### Output atteso
```
✅ Database inizializzato correttamente
📍 Percorso: /path/to/project/tennis_bot.db
✅ Menu comandi impostato
🎾 Bot avviato con successo!
```

---

## 🎮 Comandi Disponibili

| Comando | Descrizione |
|---------|-------------|
| `/start` | Avvia il bot e mostra menu principale |
| `/aiuto` | Mostra tutti i comandi disponibili |
| `/classificaatp` | Top 10 ATP Singles |
| `/classificawta` | Top 10 WTA Singles |
| `/classificaatpdoppio` | Top 10 ATP Doubles |
| `/classificawtadoppio` | Top 10 WTA Doubles |
| `/racetoturin` | Race to ATP Finals |
| `/partite` | Partite live e risultati di oggi |
| `/cerca` | Cerca un giocatore (interattivo) |
| `/h2h` | Head to Head tra 2 giocatori |
| `/meteo` | Meteo città torneo |
| `/preferiti` | Visualizza giocatori preferiti |
| `/aggiungi` | Aggiungi giocatore ai preferiti |
| `/rimuovi` | Rimuovi giocatore dai preferiti |
| `/statistiche` | Statistiche personali e globali |
| `/annulla` | Annulla operazione in corso |

---

## 🏗️ Architettura

```
tennis-bot/
│
├── src/main/java/
│   ├── bot/
│   │   └── BotTelegramGastaldello.java    # Logica principale bot
│   ├── scraper/
│   │   └── TennisService.java              # Web scraping (Wikipedia, SofaScore)
│   ├── database/
│   │   └── DatabaseManager.java            # Gestione SQLite
│   ├── model/
│   │   ├── Player.java                     # Modello giocatore
│   │   ├── Match.java                      # Modello partita
│   │   └── H2HData.java                    # Modello H2H
│   ├── API/
│   │   └── WeatherService.java             # API OpenWeather
│   └── Main.java                           # Entry point
│
├── tennis_bot.db                           # Database SQLite
├── pom.xml                                 # Maven dependencies
├── config.properties                       # Configurazione
└── README.md                               # Documentazione
```

---

## 🗄️ Database

### Schema SQLite

#### **Tabella `users`**
```sql
CREATE TABLE users (
    chat_id INTEGER PRIMARY KEY,
    username TEXT,
    first_interaction TIMESTAMP,
    last_interaction TIMESTAMP,
    total_interactions INTEGER
);
```

#### **Tabella `players`**
```sql
CREATE TABLE players (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT UNIQUE NOT NULL,
    country TEXT,
    ranking INTEGER,
    points INTEGER,
    age INTEGER,
    altezza TEXT,
    peso TEXT,
    miglior_ranking TEXT,
    vittorie_sconfitte TEXT,
    titoli TEXT,
    is_tennis_player INTEGER,
    search_count INTEGER,
    last_updated TIMESTAMP
);
```

#### **Tabella `favorite_players`**
```sql
CREATE TABLE favorite_players (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    chat_id INTEGER,
    player_name TEXT,
    added_at TIMESTAMP,
    UNIQUE(chat_id, player_name),
    FOREIGN KEY (chat_id) REFERENCES users(chat_id)
);
```

#### **Tabella `interactions`**
```sql
CREATE TABLE interactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    chat_id INTEGER,
    command TEXT,
    timestamp TIMESTAMP,
    FOREIGN KEY (chat_id) REFERENCES users(chat_id)
);
```

---

## 🛠️ Tecnologie Utilizzate

### Backend
- **Java 17** - Linguaggio principale
- **Telegram Bot API** - Integrazione Telegram
- **OkHttp** - HTTP client per API calls

### Web Scraping
- **Selenium WebDriver** - Scraping dinamico (SofaScore)
- **JSoup** - Parsing HTML (Wikipedia)
- **ChromeDriver** - Browser automation

### Database
- **SQLite** - Database embedded leggero
- **JDBC** - Connessione database

### API Esterne
- **Wikipedia** - Dati giocatori e classifiche
- **SofaScore** - Partite live
- **MatchStat** - Head to Head
- **OpenWeather** - Meteo

### Build & Dependencies
- **Maven** - Dependency management
- **Gson** - JSON parsing

---

## 📦 Dipendenze Maven

```xml
<dependencies>
    <!-- Telegram Bot API -->
    <dependency>
        <groupId>org.telegram</groupId>
        <artifactId>telegrambots-longpolling</artifactId>
        <version>8.0.0</version>
    </dependency>
    
    <!-- Selenium WebDriver -->
    <dependency>
        <groupId>org.seleniumhq.selenium</groupId>
        <artifactId>selenium-java</artifactId>
        <version>4.15.0</version>
    </dependency>
    
    <!-- JSoup HTML Parser -->
    <dependency>
        <groupId>org.jsoup</groupId>
        <artifactId>jsoup</artifactId>
        <version>1.17.1</version>
    </dependency>
    
    <!-- SQLite JDBC -->
    <dependency>
        <groupId>org.xerial</groupId>
        <artifactId>sqlite-jdbc</artifactId>
        <version>3.44.1.0</version>
    </dependency>
    
    <!-- OkHttp -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <version>4.12.0</version>
    </dependency>
    
    <!-- Gson -->
    <dependency>
        <groupId>com.google.code.gson</groupId>
        <artifactId>gson</artifactId>
        <version>2.10.1</version>
    </dependency>
</dependencies>
```

---

## 🚧 Sviluppi Futuri

- [ ] **Notifiche push** per partite giocatori preferiti
- [ ] **Calendario tornei** settimanale/mensile
- [ ] **Grafici statistiche** (win rate, ranking trends)
- [ ] **Supporto multi-lingua** (EN, ES, FR)
- [ ] **Predizioni match** con AI/ML
- [ ] **Quiz tennis** interattivi
- [ ] **Streaming live** link ufficiali
- [ ] **Deploy su cloud** (AWS/Heroku)

---

## 🤝 Contribuire

I contributi sono benvenuti! Ecco come puoi aiutare:

1. **Fork** il progetto
2. Crea un **branch** per la tua feature (`git checkout -b feature/AmazingFeature`)
3. **Commit** le modifiche (`git commit -m 'Add some AmazingFeature'`)
4. **Push** sul branch (`git push origin feature/AmazingFeature`)
5. Apri una **Pull Request**

### 🐛 Segnala Bug
Apri una [issue](https://github.com/tuo-username/tennis-bot/issues) descrivendo:
- Comportamento atteso
- Comportamento attuale
- Passi per riprodurre
- Screenshot (se applicabile)

---

## 📝 Licenza

Questo progetto è distribuito sotto licenza **MIT**.  
Vedi il file [LICENSE](LICENSE) per maggiori dettagli.

---

## 👨‍💻 Autore

**Gastaldello [Il tuo nome]**
- GitHub: [@tuo-username](https://github.com/tuo-username)
- Telegram: [@tuo_username_telegram](https://t.me/tuo_username_telegram)

---

## 🙏 Ringraziamenti

- [Telegram Bot API](https://core.telegram.org/bots) per la documentazione eccellente
- [Wikipedia](https://www.wikipedia.org/) per i dati aperti
- [SofaScore](https://www.sofascore.com/) per le partite live
- [OpenWeather](https://openweathermap.org/) per le API meteo

---

## 📞 Supporto

Hai problemi o domande?

- 📧 Email: tua-email@example.com
- 💬 Telegram: [@tuo_username](https://t.me/tuo_username)
- 🐛 Issues: [GitHub Issues](https://github.com/tuo-username/tennis-bot/issues)

---

## ⭐ Supporta il Progetto

Se questo progetto ti è stato utile, lascia una ⭐ su GitHub!

```
               🎾
        _______________
       |               |
       |   TENNIS BOT  |
       |_______________|
            |     |
           /       \
          🏆       🏆
```

**Made with ❤️ and ☕ by Gastaldello**