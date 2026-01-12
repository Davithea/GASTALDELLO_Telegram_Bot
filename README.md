# 🎾 Tennis Bot Telegram

> **Bot Telegram completo per statistiche tennis in tempo reale**  
> Classifiche ATP/WTA, partite live, ricerca giocatori, Head-to-Head, meteo e molto altro!

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Telegram](https://img.shields.io/badge/Telegram-Bot%20API-blue.svg)](https://core.telegram.org/bots)

---

## 📋 Indice

- [Caratteristiche](#-caratteristiche)
- [Demo](#-demo)
- [Configurazione](#-configurazione)
- [Comandi Disponibili](#-comandi-disponibili)
- [Architettura](#-architettura)
- [Database](#-database)
- [Tecnologie Utilizzate](#-tecnologie-utilizzate)

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
- Partite terminate oggi con vincitore
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
- Meteo in tempo reale per città tornei
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
─────────────────────
👤 Jannik Sinner vs Novak Djokovic
🔴 LIVE - Game: 40-30
📊 Set: 6-4 3-2

👤 Carlos Alcaraz vs Daniil Medvedev
🏆 Alcaraz b. Medvedev 2-1
📊 Punteggio: 6-4 3-6 7-5
```

---

## ⚙️ Configurazione

### 1️⃣ Crea il file `.env` (o configura direttamente nel codice)

Crea un file `config.properties` nella root del progetto:

```properties
BOT_TOKEN=inserisci_qui_il_token_bot
API_KEY=inserisci_qui_l'api_key
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
### Struttura classi

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

### Diagramma UML
```
┌─────────────────────────────────────────────────────────────────────────┐
│                                  Main                                   │
├─────────────────────────────────────────────────────────────────────────┤
│ + main(args: String[]): void                                            │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ usa
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            MyConfiguration                              │
├─────────────────────────────────────────────────────────────────────────┤
│ - MyConfiguration()                                                     │
│ + getInstance(): MyConfiguration                                        │
│ + getProperty(key: String): String                                      │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    │ configura
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       BotTelegramGastaldello                            │
├─────────────────────────────────────────────────────────────────────────┤
│ + BotTelegramGastaldello(botToken: String, apiKey: String)              │
│ + consume(update: Update): void                                         │
│ - setupBotCommands(): void                                              │
│ - createKeyboard(): ReplyKeyboardMarkup                                 │
│ - processCommand(command: String, chatId: Long): String                 │
│ - handlePlayerSearch(chatId: Long, playerName: String): String          │
│ - handleAddFavorite(chatId: Long, playerName: String): String           │
│ - handleRemoveFavorite(chatId: Long, playerName: String): String        │
│ - handleH2HPlayer1(chatId: Long, playerName: String): String            │
│ - handleH2HPlayer2(chatId: Long, player2Name: String): String           │
│ - handleWeather(chatId: Long, city: String): String                     │
│ + getH2H(player1: String, player2: String): String                      │
│ - formatH2HData(data: H2HData): String                                  │
│ - formatRankings(rankings: List<Player>, type: String): String          │
│ - formatMatches(matches: List<Match>): String                           │
│ - getTournamentEmoji(tournament: String): String                        │
│ - formatPlayerInfo(player: Player): String                              │
│ - sendMessage(chatId: Long, text: String, showKeyboard: boolean): void  │
│ - sendPhoto(chatId: Long, photoUrl: String, caption: String): void      │
└─────────────────────────────────────────────────────────────────────────┘
                     │                    │                    │
                     │ usa                │ usa                │ usa
                     ▼                    ▼                    ▼
┌──────────────────────────┐  ┌──────────────────────────┐  ┌──────────────────────────┐
│    DatabaseManager       │  │     TennisService        │  │    WeatherService        │
├──────────────────────────┤  ├──────────────────────────┤  ├──────────────────────────┤
│ + DatabaseManager()      │  │ + TennisService()        │  │ + WeatherService(        │
│ - initializeDatabase()   │  │ + getATPRankings(        │  │     apiKey: String)      │
│ + saveUser(chatId: Long, │  │     limit: int):         │  │ + getCurrentWeather(     │
│     username: String)    │  │     List<Player>         │  │     city: String):       │
│ + savePlayer(            │  │ + getWTARankings(        │  │     String               │
│     player: Player)      │  │     limit: int):         │  │ - normalizeCity(         │
│ + savePlayers(           │  │     List<Player>         │  │     city: String):       │
│     players: List<       │  │ + getRaceRankings(       │  │     String               │
│     Player>)             │  │     limit: int):         │  │ - formatWeatherResponse( │
│ + logInteraction(        │  │     List<Player>         │  │     json: String):       │
│     chatId: Long,        │  │ + getATPDoubleRankings(  │  │     String               │
│     command: String)     │  │     limit: int):         │  │ - capitalize(            │
│ + addFavoritePlayer(     │  │     List<Player>         │  │     text: String):       │
│     chatId: Long,        │  │ + getWTADoubleRankings(  │  │     String               │
│     playerName: String): │  │     limit: int):         │  └──────────────────────────┘
│     String               │  │     List<Player>         │
│ + removeFavoritePlayer(  │  │ + searchPlayer(          │
│     chatId: Long,        │  │     playerName: String): │
│     playerName: String): │  │     Player               │
│     String               │  │ + getH2HData(            │
│ + getFavoritePlayers(    │  │     player1: String,     │
│     chatId: Long):       │  │     player2: String):    │
│     String               │  │     H2HData              │
│ + getUserStatistics(     │  │ + getRecentMatches():    │
│     chatId: Long):       │  │     List<Match>          │
│     String               │  │ - getRankings(           │
└──────────────────────────┘  │     limit: int,          │
                              │     wikiPage: String,    │
                              │     type: String):       │
                              │     List<Player>         │
                              │ - cleanCountry(          │
                              │     country: String):    │
                              │     String               │
                              │ - formatWikipediaName(   │
                              │     name: String):       │
                              │     String               │
                              │ - isTennisPlayer(        │
                              │     doc: Document):      │
                              │     boolean              │
                              │ - extractPlayerInfo(     │
                              │     doc: Document,       │
                              │     searchName: String): │
                              │     Player               │
                              │ - cleanText(             │
                              │     text: String):       │
                              │     String               │
                              │ - extractBirthDate(      │
                              │     table: Element):     │
                              │     String               │
                              │ - extractFromTable(      │
                              │     table: Element,      │
                              │     defaultName: String):│
                              │     String               │
                              │ - extractNationality(    │
                              │     table: Element):     │
                              │     String               │
                              │ - extractFromTableRow(   │
                              │     table: Element,      │
                              │     keywords: String...):│
                              │     String               │
                              │ - extractRanking(        │
                              │     table: Element): int │
                              │ - extractGrandSlams(     │
                              │     table: Element):     │
                              │     String               │
                              │ - calculateAge(          │
                              │     birthDate: String):  │
                              │     int                  │
                              │ - searchInRankings(      │
                              │     playerName: String): │
                              │     Player               │
                              │ - extractImageUrl(       │
                              │     table: Element):     │
                              │     String               │
                              │ - getPlayerImageFrom     │
                              │     Wikipedia(           │
                              │     playerName: String): │
                              │     String               │
                              │ - formatPlayerNameForURL(│
                              │     name: String):       │
                              │     String               │
                              │ - extractPercentage(     │
                              │     text: String):       │
                              │     String               │
                              │ - extractWinLoss(        │
                              │     text: String):       │
                              │     String               │
                              │ - parseIntSafe(          │
                              │     text: String): int   │
                              │ - parseMatchText(        │
                              │     text: String):       │
                              │     MatchTextData        │
                              │ - parseScoreNumbers(     │
                              │     allNumbers: List<    │
                              │     String>,             │
                              │     isLive: boolean):    │
                              │     List<String>         │
                              │ - isValidTennisScore(    │
                              │     score1: int,         │
                              │     score2: int):        │
                              │     boolean              │
                              │ - isTournamentTitle(     │
                              │     text: String):       │
                              │     boolean              │
                              │ - determineWinner(       │
                              │     data: MatchTextData):│
                              │     String               │
                              │ - isLocationLine(        │
                              │     text: String):       │
                              │     boolean              │
                              └──────────────────────────┘
                                           │
                                           │ crea/usa
                     ┌─────────────────────┼─────────────────────┐
                     │                     │                     │
                     ▼                     ▼                     ▼
┌──────────────────────────┐  ┌──────────────────────────┐  ┌──────────────────────────┐
│        Player            │  │        H2HData           │  │         Match            │
├──────────────────────────┤  ├──────────────────────────┤  ├──────────────────────────┤
│ + Player(nome: String,   │  │ + H2HData()              │  │ + Match(tournament:      │
│     paese: String,       │  │ + getPlayer1Name():      │  │     String, location:    │
│     ranking: int,        │  │     String               │  │     String, player1:     │
│     punti: int,          │  │ + getPlayer2Name():      │  │     String, player2:     │
│     eta: int)            │  │     String               │  │     String, score:       │
│ + getNome(): String      │  │ + getPlayer1Image():     │  │     String, date:        │
│ + getPaese(): String     │  │     String               │  │     String, priority:    │
│ + getRanking(): int      │  │ + getPlayer2Image():     │  │     int)                 │
│ + getPunti(): int        │  │     String               │  │ + getTournament():       │
│ + getEta(): int          │  │ + getPlayer1PrizeMoney():│  │     String               │
│ + getExtraInfo(): String │  │     String               │  │ + getPlayer1(): String   │
│ + getImageUrl(): String  │  │ + getPlayer1WinLoss():   │  │ + getPlayer2(): String   │
│ + getAltezza(): String   │  │     String               │  │ + getScore(): String     │
│ + getPeso(): String      │  │ + getPlayer1Win          │  │ + getDate(): String      │
│ + getMigliorRanking():   │  │     Percentage(): String │  │ + getWinner(): String    │
│     String               │  │ + getPlayer1Grass(): int │  │ + getDetailedScore():    │
│ + getVittorieSconfitte():│  │ + getPlayer1Clay(): int  │  │     String               │
│     String               │  │ + getPlayer1Hard(): int  │  │ + getSetScore(): String  │
│ + getTitoli(): String    │  │ + getPlayer1Indoor(): int│  │ + getStatus(): String    │
│ + isTennisPlayer():      │  │ + getPlayer1Titles(): int│  │ + getLocation(): String  │
│     boolean              │  │ + getPlayer1YTDWinLoss():│  │ + getCurrentGame():      │
│ + setNome(nome: String)  │  │     String               │  │     String               │
│ + setPaese(paese: String)│  │ + getPlayer1YTD          │  │ + getPriority(): int     │
│ + setRanking(            │  │     Percentage(): String │  │ + setTournament(         │
│     ranking: int)        │  │ + getPlayer2PrizeMoney():│  │     tournament: String)  │
│ + setPunti(punti: int)   │  │     String               │  │ + setPlayer1(            │
│ + setEta(eta: int)       │  │ + getPlayer2WinLoss():   │  │     player1: String)     │
│ + setExtraInfo(          │  │     String               │  │ + setPlayer2(            │
│     extraInfo: String)   │  │ + getPlayer2Win          │  │     player2: String)     │
│ + setImageUrl(           │  │     Percentage(): String │  │ + setScore(              │
│     imageUrl: String)    │  │ + getPlayer2Grass(): int │  │     score: String)       │
│ + setAltezza(            │  │ + getPlayer2Clay(): int  │  │ + setDate(date: String)  │
│     altezza: String)     │  │ + getPlayer2Hard(): int  │  │ + setWinner(             │
│ + setPeso(peso: String)  │  │ + getPlayer2Indoor(): int│  │     winner: String)      │
│ + setMigliorRanking(     │  │ + getPlayer2Titles(): int│  │ + setDetailedScore(      │
│     migliorRanking:      │  │ + getPlayer2YTDWinLoss():│  │     detailedScore:       │
│     String)              │  │     String               │  │     String)              │
│ + setVittorieSconfitte(  │  │ + getPlayer2YTD          │  │ + setSetScore(           │
│     vittorieSconfitte:   │  │     Percentage(): String │  │     setScore: String)    │
│     String)              │  │ + getTotalH2HMatches():  │  │ + setStatus(             │
│ + setTitoli(             │  │     int                  │  │     status: String)      │
│     titoli: String)      │  │ + getH2hRecord(): String │  │ + setLocation(           │
│ + setTennisPlayer(       │  │ + setPlayer1Name(        │  │     location: String)    │
│     tennisPlayer:        │  │     player1Name: String) │  │ + setCurrentGame(        │
│     boolean)             │  │ + setPlayer2Name(        │  │     currentGame: String) │
│ + toString(): String     │  │     player2Name: String) │  │ + setPriority(           │
└──────────────────────────┘  │ + setPlayer1Image(       │  │     priority: int)       │
                              │     player1Image: String)│  │ + isFinished(): boolean  │
                              │ + setPlayer2Image(       │  │ + isAnnullata(): boolean │
                              │     player2Image: String)│  │ + isLive(): boolean      │
                              │ + setPlayer1PrizeMoney(  │  │ + isTavolino(): boolean  │
                              │     player1PrizeMoney:   │  │ + toString(): String     │
                              │     String)              │  └──────────────────────────┘
                              │ + setPlayer1WinLoss(     │
                              │     player1WinLoss:      │
                              │     String)              │
                              │ + setPlayer1Win          │
                              │     Percentage(          │
                              │     player1WinPercentage:│
                              │     String)              │
                              │ + setPlayer1Grass(       │
                              │     player1Grass: int)   │
                              │ + setPlayer1Clay(        │
                              │     player1Clay: int)    │
                              │ + setPlayer1Hard(        │
                              │     player1Hard: int)    │
                              │ + setPlayer1Indoor(      │
                              │     player1Indoor: int)  │
                              │ + setPlayer1Titles(      │
                              │     player1Titles: int)  │
                              │ + setPlayer1YTDWinLoss(  │
                              │     player1YTDWinLoss:   │
                              │     String)              │
                              │ + setPlayer1YTD          │
                              │     Percentage(          │
                              │     player1YTDPercentage:│
                              │     String)              │
                              │ + setPlayer2PrizeMoney(  │
                              │     player2PrizeMoney:   │
                              │     String)              │
                              │ + setPlayer2WinLoss(     │
                              │     player2WinLoss:      │
                              │     String)              │
                              │ + setPlayer2Win          │
                              │     Percentage(          │
                              │     player2WinPercentage:│
                              │     String)              │
                              │ + setPlayer2Grass(       │
                              │     player2Grass: int)   │
                              │ + setPlayer2Clay(        │
                              │     player2Clay: int)    │
                              │ + setPlayer2Hard(        │
                              │     player2Hard: int)    │
                              │ + setPlayer2Indoor(      │
                              │     player2Indoor: int)  │
                              │ + setPlayer2Titles(      │
                              │     player2Titles: int)  │
                              │ + setPlayer2YTDWinLoss(  │
                              │     player2YTDWinLoss:   │
                              │     String)              │
                              │ + setPlayer2YTD          │
                              │     Percentage(          │
                              │     player2YTDPercentage:│
                              │     String)              │
                              │ + setTotalH2HMatches(    │
                              │     totalH2HMatches: int)│
                              │ + setH2hRecord(          │
                              │     h2hRecord: String)   │
                              └──────────────────────────┘
```
---

## 🗄️ Database

### Diagramma E/R
```
+-------------------+           +------------------+           +-------------------+
|     User          |1         N|  FavoritePlayer  |N         1|     Player        |
+-------------------+-----------+------------------+-----------+-------------------+
| chat_id (PK)      |           | id (PK)          |           | id (PK)           |
| username          |           | added_at         |           | name (UNIQUE)     |
| first_interaction |           +------------------+           | country           |
| last_interaction  |                                          | ranking           |
| total_interactions|                                          | points            |
+-------------------+                                          | age               |
                                                               | altezza           |
                                                               | peso              |
                                                               | miglior_ranking   |
                                                               | vittorie_sconfitte|
                                                               | titoli            |
                                                               | is_tennis_player  |
                                                               | search_count      |
                                                               | last_updated      |
                                                               +-------------------+
+----------------+
|  Interaction   |
+----------------+
| id (PK)        |
| chat_id (FK)   |
| command        |
| timestamp      |
+----------------+
```

### Modello logico-relazionale
```
User(chat_id PK, username, first_interaction, last_interaction, total_interactions)

Player(id PK, name, country, ranking, points, age, altezza, peso, miglior_ranking, vittorie_sconfitte, titoli, 
is_tennis_player, search_count, last_updated)

FavoritePlayer(id PK, chat_id, player_name, added_at, UNIQUE(chat_id, player_name))
FKs:
chat_id -> User.chat_id
player_name -> Player.name

Interaction(id PK, chat_id, command, timestamp)
FK:
chat_id -> User.chat_id
```

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
- **Java** - Linguaggio principale
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

## 👨‍💻 Autore

**Gastaldello Davide**
- GitHub: [@davithea](https://github.com/davithea)
- Telegram: [@D_G2007](https://t.me/D_G2007)