# Player Account & Game Management System

A console-based Java + MySQL application implementing player accounts, characters,
inventory, a leaderboard, and achievements, built around the OOP pillars:
encapsulation, inheritance, polymorphism, and abstraction.

## Project structure

```
PlayerManagementSystem/
├── sql/
│   └── schema.sql              # run once to create the database + 5 tables
├── src/game/
│   ├── Main.java
│   ├── db/
│   │   └── DatabaseConnection.java
│   ├── model/
│   │   ├── Player.java
│   │   ├── Character.java      # abstract
│   │   ├── Warrior.java
│   │   ├── Mage.java
│   │   ├── Archer.java
│   │   ├── Weapon.java         # abstract
│   │   ├── Sword.java
│   │   ├── Bow.java
│   │   ├── Inventory.java
│   │   └── Achievement.java
│   ├── dao/
│   │   ├── PlayerDAO.java
│   │   ├── CharacterDAO.java
│   │   ├── InventoryDAO.java
│   │   ├── ScoreDAO.java
│   │   └── AchievementDAO.java
│   └── manager/
│       └── GameManager.java    # console menu / workflow
```

## 1. Set up MySQL

Start MySQL (via XAMPP or a local install), then run:

```bash
mysql -u root -p < sql/schema.sql
```

This creates the `game_db` database with the `Players`, `Characters`,
`Inventory`, `Scores`, and `Achievements` tables, related by `PlayerID`.

## 2. Get the MySQL JDBC driver

Download `mysql-connector-j-<version>.jar` from
https://dev.mysql.com/downloads/connector/j/ (or your Maven/Gradle cache)
and place it in a `lib/` folder in this project.

## 3. Configure the connection

Edit `src/game/db/DatabaseConnection.java` if your MySQL user/password/port
differ from the defaults (`root` / empty password / `localhost:3306`).

## 4. Compile

```bash
javac -d bin $(find src -name "*.java")
```

## 5. Run

```bash
java -cp "bin:lib/mysql-connector-j-<version>.jar" game.Main
```

(On Windows, use `;` instead of `:` in the classpath.)

## What it does

- **Register / Login / Edit Profile / Delete Player** — `PlayerDAO`
- **Choose or create a character** (Warrior, Mage, Archer) — `CharacterDAO`,
  each subclass overrides `attack()` differently (polymorphism)
- **Manage inventory** — add Swords/Bows, each with its own `specialEffect()`
- **Submit a score** — `ScoreDAO` keeps the highest score per player and
  recalculates the `Rank` column for everyone after each submission
- **View achievements** — `AchievementDAO`
- **Leaderboard** — ranked list joined across `Scores` and `Players`

Everything is wired together console-first in `GameManager`, matching the
workflow: Login → Dashboard → Choose Character → Manage Inventory →
Update Score → Save to Database → Leaderboard.

## Deployment

There are three levels here, pick the one that matches what you need.

### Option A — Quick local run (what you already have)

Compile with `javac` and run with `java -cp`, as in the Compile/Run steps
above. Good for testing on your own machine.

### Option B — One runnable jar (recommended for handing in / running anywhere)

This project now includes a `pom.xml` that bundles the MySQL driver into a
single jar, so there's no classpath juggling.

```bash
mvn clean package
java -jar target/player-management-system-1.0.0.jar
```

By default it connects to `jdbc:mysql://localhost:3306/game_db` as `root`
with no password (same as before). To point it at a different database
without touching code, set environment variables before running:

```bash
export DB_URL="jdbc:mysql://<host>:3306/game_db?useSSL=false&serverTimezone=UTC"
export DB_USER="myuser"
export DB_PASSWORD="mypassword"
java -jar target/player-management-system-1.0.0.jar
```

### Option C — Docker (app + MySQL together, one command)

The project also includes a `Dockerfile` and `docker-compose.yml`. This
spins up MySQL, loads `sql/schema.sql` automatically, and runs the app —
no local Java or MySQL install needed at all.

```bash
docker compose run --build app
```

(`run` instead of `up` because this is an interactive console app that
reads from stdin — `up` won't let you type into the menu.)

To tear it down: `docker compose down` (add `-v` to also wipe the database
volume).

### Option D — A real server (VPS / cloud)

If you want this reachable beyond your laptop:

1. **Database**: either install MySQL on the server, or use a managed
   MySQL (AWS RDS, DigitalOcean Managed MySQL, PlanetScale, Railway, etc.)
   and run `sql/schema.sql` against it once.
2. **App**: copy the fat jar from Option B onto the server (`scp` or a CI
   pipeline), set `DB_URL` / `DB_USER` / `DB_PASSWORD` as environment
   variables pointing at that database, and run it.
3. **Keep it running** (since it's a long-lived console session, not a web
   service): either connect over SSH and run it in a `tmux`/`screen`
   session, or convert it into a background service. A minimal systemd
   unit looks like:

   ```ini
   # /etc/systemd/system/game-management.service
   [Unit]
   Description=Player Account & Game Management System

   [Service]
   Environment=DB_URL=jdbc:mysql://localhost:3306/game_db?useSSL=false&serverTimezone=UTC
   Environment=DB_USER=root
   Environment=DB_PASSWORD=yourpassword
   ExecStart=/usr/bin/java -jar /opt/game-management/player-management-system-1.0.0.jar
   Restart=on-failure

   [Install]
   WantedBy=multi-user.target
   ```

   Note this app is a console menu, not a network service — running it
   under systemd only makes sense if you also add remote input (e.g. wrap
   it in an SSH session or convert the menu to a simple TCP/HTTP interface
   later). For a class project, Option B or C is usually the right amount
   of "deployed."

### Which one should you actually use?

- **Submitting for a course / demoing locally** → Option A or B.
- **Want a teammate to run it without installing MySQL themselves** → Option C.
- **Want it live on the internet for others to use** → Option D, and at
  that point you'd eventually want to swap the console `Scanner` UI for a
  web or API front end, since SSH-only access doesn't scale well.


- All DB access uses `PreparedStatement`s to prevent SQL injection.
- Passwords are stored as given here for simplicity — for anything beyond a
  class project, hash them (e.g. with BCrypt) before saving.
- The code compiles clean under JDK 21; no MySQL server was available in
  this environment to run it end-to-end, so test the full flow against your
  own MySQL instance after following the steps above.
