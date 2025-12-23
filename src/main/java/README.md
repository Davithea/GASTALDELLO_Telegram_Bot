# 🎾 Tennis Telegram Bot

Bot Telegram **IBRIDO** per tennis:
- **Classifiche ATP/WTA**: Web scraping da Wikipedia (sempre disponibile)
- **Partite live**: RapidAPI Tennis Live Data
- **Preferenze personali**: Salva i tuoi giocatori preferiti

## ✨ Funzionalità

### 📊 Classifiche (Wikipedia Scraping)
- 🏆 Top 10 ATP Singolare
- 👩 Top 10 WTA Singolare
- 👥 Top 10 ATP Doppio
- 👭 Top 10 WTA Doppio

### ⚡ Partite Live (RapidAPI)
- 📅 Partite in corso in tempo reale
- 🏆 Grand Slam, Masters 1000, ATP/WTA Tour

### ⭐ Preferiti Personali
- ➕ Aggiungi giocatori preferiti
- ➖ Rimuovi dai preferiti
- 📋 Visualizza la tua lista

### 🔍 Ricerca Interattiva
- Comando `/cerca` chiede il nome
- Cerca nei top 100 ATP/WTA
- Risultati con bandiere e statistiche

### 📊 Statistiche Avanzate
- Le tue interazioni
- Comando preferito
- Statistiche globali del bot

## 🚀 Setup Completo

### 1. Prerequisiti
- Java 17+
- Maven
- Account Telegram

### 2. Ottieni le API Keys (GRATIS)

#### **Bot Token Telegram**
```
1. Apri Telegram → cerca @BotFather
2. Invia /newbot
3. Nome: "My Tennis Bot"
4. Username: "mytennisbot" (deve finire con "bot")
5. Copia il TOKEN
```

#### **RapidAPI Key**
```
1. Vai su https://rapidapi.com/
2. Registrati GRATIS (solo email, NO carta)
3. Cerca "Tennis Live Data"
   → https://rapidapi.com/sportcontentapi/api/tennis-live-data
4. Clicca "Subscribe to Test"
5. Scegli piano FREE
6. Copia "X-RapidAPI-Key" dal Code Snippets
```

**Piano FREE RapidAPI:**
- ✅ Partite live
- ✅ Nessun limite sulle classifiche (Wikipedia)
- ✅ NO carta di credito

### 3. Configurazione

Modifica `config.properties`:

```properties
BOT_TOKEN=1234567890:ABCdefGHIjklMNOpqrsTUVwxyz
RAPID_API_KEY=abcdef1234567890ghijklmnop
```

### 4. Esecuzione

```bash
# Compila
mvn clean package

# Esegui
java -jar target/tennis-telegram-bot-1.0-SNAPSHOT.jar
```

Output atteso:
```
✅ Tennis Bot avviato correttamente!
📱 Il bot è ora in ascolto...
📊 Classifiche: Wikipedia (scraping)
⚡ Live: RapidAPI Tennis Live Data
```

## 📱 Comandi Bot

### Comandi Base
- `/start` - Menu principale
- `/aiuto` - Lista comandi

### Classifiche
- `/classificaATP` - Top 10 ATP Singolare 🏆
- `/classificaWTA` - Top 10 WTA Singolare 👩
- `/classificaATPDoppio` - Top 10 ATP Doppio 👥
- `/classificaWTADoppio` - Top 10 WTA Doppio 👭

### Partite & Ricerca
- `/partite` - Partite live in corso ⚡
- `/cerca` - **INTERATTIVO**: chiede il nome dopo 🔍

### Preferiti ⭐
- `/preferiti` - Lista giocatori preferiti
- `/aggiungi [nome]` - Aggiungi ai preferiti
- `/rimuovi [nome]` - Rimuovi dai preferiti

### Statistiche
- `/statistiche` - Le tue statistiche personali

## 💡 Esempio Utilizzo Ricerca

```
👤 Tu: /cerca
🤖 Bot: "Scrivi il nome del giocatore..."

👤 Tu: Sinner
🤖 Bot: 
     🎾 Jannik Sinner
     🌍 Nazionalità: 🇮🇹 ITALY
     🏆 Ranking: #1
     📊 Punti: 11830
     
     💡 Aggiungi ai preferiti con: /aggiungi Jannik Sinner

👤 Tu: /aggiungi Jannik Sinner
🤖 Bot: "⭐ Jannik Sinner aggiunto ai preferiti!"
```

## 🔧 Struttura Progetto

```
src/
├── Main.java                          # Entry point
├── bot/
│   └── BotTelegramGastaldello.java   # Logica bot + stato conversazione
├── scraper/
│   └── TennisService.java            # Scraping Wikipedia + API RapidAPI
├── database/
│   └── DatabaseManager.java          # SQLite + preferiti
├── model/
│   ├── Player.java
│   └── Match.java
└── config/
    └── MyConfiguration.java
```

## 🗄️ Database SQLite

Tabelle create automaticamente:

1. **users** - Utenti e interazioni
2. **players** - Giocatori cercati
3. **matches** - Partite salvate
4. **interactions** - Log comandi
5. **favorite_players** - ⭐ Preferiti per utente
6. **user_preferences** - Preferenze personali

File: `tennis_bot.db`

## 🌐 Architettura Ibrida

### Perché questo approccio?

| Funzionalità | Fonte | Motivo |
|-------------|-------|--------|
| **Classifiche** | Wikipedia | Sempre disponibile, dati ufficiali, gratuito |
| **Partite Live** | RapidAPI | Dati in tempo reale, API affidabile |
| **Ricerca** | Wikipedia top 100 | Sufficiente per uso normale |

### Vantaggi
- ✅ **Classifiche sempre disponibili** (no dipendenza da API)
- ✅ **NO costi** per uso normale
- ✅ **Dati affidabili** (Wikipedia = fonte ufficiale ATP/WTA)
- ✅ **Live solo quando servono** (partite durante tornei)

## 🐛 Risoluzione Problemi

### ❌ "Classifiche non disponibili"
- Wikipedia potrebbe essere temporaneamente offline
- Controlla la connessione internet
- Riprova dopo 1-2 minuti

### ❌ "Nessuna partita live"
- **NORMALE** se non ci sono tornei
- Le partite live sono disponibili solo durante:
    - Grand Slam
    - Masters 1000
    - ATP/WTA Tour events
- Testa durante un torneo importante

### ❌ "RapidAPI Key non valida"
- Verifica key copiata senza spazi
- Controlla di aver sottoscritto il piano FREE
- Vai su RapidAPI dashboard e verifica

### ❌ "Giocatore non trovato"
- La ricerca funziona solo su **top 100 ATP/WTA**
- Verifica spelling del nome
- Prova con solo cognome (es: "Sinner" invece di "Jannik Sinner")

### ❌ "Bandiere non visibili"
- Problema risolto nel nuovo codice
- Supporta 70+ paesi + codici ISO
- Gestisce doppio (es: 🇮🇹 🇪🇸)

## 🔐 Sicurezza

**IMPORTANTE - .gitignore:**
```
config.properties
tennis_bot.db
*.log
target/
```

**NON committare mai:**
- Token bot
- API keys
- Database locale

## 📊 Limiti e Quote

### Wikipedia (Scraping)
- ✅ Illimitato
- ✅ Sempre disponibile
- ⚠️ Richiede parsing HTML (può rompersi se cambia struttura)

### RapidAPI Free Tier
- Consulta i limiti sul tuo piano
- Generalmente sufficiente per uso personale
- Le partite live usano 1 richiesta

## 🎯 Prossimi Sviluppi

- [ ] Notifiche per giocatori preferiti
- [ ] Statistiche head-to-head
- [ ] Calendario tornei
- [ ] Export preferiti
- [ ] Multi-lingua

## 🤝 Contributi

Pull request benvenute!

Per modifiche importanti:
1. Apri prima una issue
2. Descrivi il cambiamento
3. Attendi feedback

## 📝 Licenza

Progetto educativo - uso libero

## 📧 Supporto

- **Telegram Bot**: @BotFather
- **RapidAPI Docs**: [Tennis Live Data API](https://rapidapi.com/sportcontentapi/api/tennis-live-data)
- **Wikipedia ATP**: [ATP Rankings](https://en.wikipedia.org/wiki/ATP_rankings)
- **Issues**: Apri una issue su GitHub

## 🙏 Credits

- Dati classifiche: Wikipedia
- Partite live: RapidAPI
- Bot framework: TelegramBots Java Library

---

**Fatto con ❤️ e ☕ per gli appassionati di tennis**

*Ultimo aggiornamento: Dicembre 2024*