package game.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import game.dao.AchievementDAO;
import game.dao.CharacterDAO;
import game.dao.InventoryDAO;
import game.dao.PlayerDAO;
import game.dao.ScoreDAO;
import game.model.Achievement;
import game.model.Archer;
import game.model.Character;
import game.model.Inventory;
import game.model.LeaderboardEntry;
import game.model.Mage;
import game.model.Player;
import game.model.Warrior;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/** A small local HTTP layer over the existing JDBC DAOs for the browser UI. */
public final class ApiServer {
    private final PlayerDAO players = new PlayerDAO();
    private final CharacterDAO characters = new CharacterDAO();
    private final InventoryDAO inventory = new InventoryDAO();
    private final ScoreDAO scores = new ScoreDAO();
    private final AchievementDAO achievements = new AchievementDAO();

    public void start() throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handle);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.printf("%nArcadia is running at http://localhost:%d%nPress Ctrl+C to stop it.%n", port);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/api/")) {
                api(exchange, path);
            } else {
                staticFile(exchange, path);
            }
        } catch (SQLException e) {
            String details = e.getMessage();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                details += " [" + e.getCause().getMessage() + "]";
            }
            json(exchange, 503, "{\"error\":\"Database unavailable: " + escape(details) + "\"}");
        } catch (IllegalArgumentException e) {
            json(exchange, 400, "{\"error\":\"" + escape(e.getMessage()) + "\"}");
        } catch (Exception e) {
            json(exchange, 500, "{\"error\":\"Unexpected server error\"}");
            e.printStackTrace();
        }
    }

    private void api(HttpExchange ex, String path) throws IOException, SQLException {
        String method = ex.getRequestMethod();
        Map<String, String> params = method.equals("GET") ? query(ex.getRequestURI().getRawQuery()) : form(ex.getRequestBody());
        if (path.equals("/api/health") && method.equals("GET")) { json(ex, 200, "{\"status\":\"ok\"}"); return; }
        if (path.equals("/api/register") && method.equals("POST")) {
            require(params, "name", "email", "password");
            if (players.emailExists(params.get("email"))) { json(ex, 409, "{\"error\":\"That email is already registered.\"}"); return; }
            Player p = players.registerPlayer(new Player(params.get("name"), params.get("email"), params.get("password")));
            json(ex, 201, playerJson(p)); return;
        }
        if (path.equals("/api/login") && method.equals("POST")) {
            require(params, "email", "password");
            Player p = players.login(params.get("email"), params.get("password"));
            if (p == null) { json(ex, 401, "{\"error\":\"Invalid email or password.\"}"); return; }
            json(ex, 200, playerJson(p)); return;
        }
        if (path.equals("/api/dashboard") && method.equals("GET")) {
            int id = integer(params, "playerId"); Player p = players.getPlayerById(id);
            if (p == null) { json(ex, 404, "{\"error\":\"Player not found.\"}"); return; }
            json(ex, 200, dashboardJson(p)); return;
        }
        if (path.equals("/api/characters") && method.equals("POST")) {
            int id = integer(params, "playerId"); require(params, "name", "className");
            Character c = switch (params.get("className")) {
                case "Mage" -> new Mage(id, params.get("name"), 80, 15, 5, 1);
                case "Archer" -> new Archer(id, params.get("name"), 90, 12, 8, 1);
                case "Warrior" -> new Warrior(id, params.get("name"), 120, 12, 12, 1);
                default -> throw new IllegalArgumentException("Choose Warrior, Mage, or Archer.");
            };
            characters.addCharacter(c); json(ex, 201, "{\"id\":" + c.getCharacterId() + "}"); return;
        }
        if (path.equals("/api/inventory") && method.equals("POST")) {
            int id = integer(params, "playerId"); require(params, "name", "type", "damage", "quantity");
            String type = params.get("type"); if (!type.equals("Sword") && !type.equals("Bow")) throw new IllegalArgumentException("Choose Sword or Bow.");
            int damage = integer(params, "damage"), quantity = integer(params, "quantity");
            if (damage < 0 || quantity < 1) throw new IllegalArgumentException("Damage must be positive and quantity at least 1.");
            inventory.addItem(new Inventory(id, params.get("type") + "|" + params.get("name"), damage, quantity));
            json(ex, 201, "{\"status\":\"created\"}"); return;
        }
        if (path.equals("/api/scores") && method.equals("POST")) {
            int id = integer(params, "playerId"), score = integer(params, "score");
            if (score < 0) throw new IllegalArgumentException("Score cannot be negative.");
            scores.submitScore(id, score); Player p = players.getPlayerById(id); p.gainXp(score / 10); players.updatePlayer(p);
            json(ex, 200, dashboardJson(p)); return;
        }
        if (path.equals("/api/battle/action") && method.equals("POST")) {
            int playerId = integer(params, "playerId");
            int characterId = integer(params, "characterId");
            String action = params.getOrDefault("action", "attack").toLowerCase();
            String enemyType = params.getOrDefault("enemyType", "Goblin Scout");
            int enemyHp = integer(params, "enemyHp");
            int enemyMaxHp = integer(params, "enemyMaxHp");
            int playerHp = integer(params, "playerHp");
            int playerMaxHp = integer(params, "playerMaxHp");
            boolean isDefending = Boolean.parseBoolean(params.getOrDefault("isDefending", "false"));

            Player p = players.getPlayerById(playerId);
            if (p == null) { json(ex, 404, "{\"error\":\"Player not found.\"}"); return; }
            Character c = characters.getCharactersByPlayer(playerId).stream()
                    .filter(ch -> ch.getCharacterId() == characterId)
                    .findFirst()
                    .orElse(null);
            if (c == null) { json(ex, 404, "{\"error\":\"Character not found.\"}"); return; }

            int enemyAtk, enemyDef, rewardXp, rewardCoins, rewardScore;
            switch (enemyType) {
                case "Training Dummy" -> { enemyAtk = 6; enemyDef = 2; rewardXp = 15; rewardCoins = 10; rewardScore = 150; }
                case "Stone Golem"    -> { enemyAtk = 20; enemyDef = 12; rewardXp = 60; rewardCoins = 50; rewardScore = 750; }
                case "Dread Knight"   -> { enemyAtk = 28; enemyDef = 14; rewardXp = 100; rewardCoins = 85; rewardScore = 1400; }
                case "Archdemon"      -> { enemyAtk = 38; enemyDef = 18; rewardXp = 180; rewardCoins = 160; rewardScore = 2500; }
                default               -> { enemyAtk = 14; enemyDef = 5; rewardXp = 35; rewardCoins = 25; rewardScore = 400; }
            }

            String playerLog = "";
            boolean nextDefending = false;

            if (action.equals("attack")) {
                int base = c.calculateAttackDamage();
                int dmg = Math.max(4, base - enemyDef + (int)(Math.random() * 5));
                enemyHp = Math.max(0, enemyHp - dmg);
                playerLog = c.attack() + " [Dealt " + dmg + " DMG]";
            } else if (action.equals("defend")) {
                nextDefending = true;
                int recovery = Math.min(playerMaxHp - playerHp, 8);
                playerHp += recovery;
                playerLog = c.defend() + " [Guarded for next strike" + (recovery > 0 ? ", recovered " + recovery + " HP]" : "]");
            } else if (action.equals("special")) {
                int base = c.calculateSpecialDamage();
                int dmg = Math.max(10, base - (enemyDef / 2) + (int)(Math.random() * 6));
                enemyHp = Math.max(0, enemyHp - dmg);
                playerLog = c.specialAbility() + " [Dealt " + dmg + " critical DMG]";
            } else if (action.equals("heal")) {
                if (p.getCoins() >= 20) {
                    p.setCoins(p.getCoins() - 20);
                    int heal = Math.min(playerMaxHp - playerHp, 40);
                    playerHp += heal;
                    playerLog = c.getCharacterName() + " consumes an Elixir (+ " + heal + " HP) [-20 Coins]";
                } else {
                    playerLog = "Not enough coins for an Elixir (costs 20 coins).";
                }
            }

            String enemyLog = "";
            boolean victory = enemyHp <= 0;
            boolean defeated = false;

            if (!victory) {
                int defVal = isDefending ? c.calculateDefenseValue() * 2 : c.calculateDefenseValue();
                int rawEnemy = enemyAtk + (int)(Math.random() * 5);
                int enemyDmg = Math.max(2, rawEnemy - defVal);
                playerHp = Math.max(0, playerHp - enemyDmg);
                enemyLog = enemyType + " strikes for " + enemyDmg + " damage! (Mitigated by " + defVal + " DEF)";
                if (playerHp <= 0) {
                    defeated = true;
                    enemyLog += " " + c.getCharacterName() + " has fallen in combat!";
                }
            } else {
                enemyLog = enemyType + " was vanquished!";
                p.gainXp(rewardXp);
                p.setCoins(p.getCoins() + rewardCoins);
                List<LeaderboardEntry> board = scores.getLeaderboardEntries();
                int currentHighScore = board.stream().filter(x -> x.name().equals(p.getName())).mapToInt(LeaderboardEntry::highScore).findFirst().orElse(0);
                scores.submitScore(playerId, currentHighScore + rewardScore);
                players.updatePlayer(p);

                ensureDefaultAchievements(playerId);
                List<Achievement> currentAch = achievements.getAchievementsByPlayer(playerId);
                for (Achievement a : currentAch) {
                    if (!a.isUnlocked()) {
                        if (a.getTitle().equalsIgnoreCase("First Blood") || (enemyType.equals("Archdemon") && a.getTitle().contains("Slayer"))) {
                            achievements.unlockAchievement(a.getAchievementId());
                        }
                    }
                }
            }

            String resp = "{\"playerHp\":" + playerHp + ",\"playerMaxHp\":" + playerMaxHp +
                    ",\"enemyHp\":" + enemyHp + ",\"enemyMaxHp\":" + enemyMaxHp +
                    ",\"isDefending\":" + nextDefending +
                    ",\"victory\":" + victory + ",\"defeated\":" + defeated +
                    ",\"playerLog\":\"" + escape(playerLog) + "\",\"enemyLog\":\"" + escape(enemyLog) + "\"" +
                    ",\"reward\":{\"xp\":" + rewardXp + ",\"coins\":" + rewardCoins + ",\"score\":" + rewardScore + "}" +
                    ",\"dashboard\":" + dashboardJson(p) + "}";
            json(ex, 200, resp);
            return;
        }
        json(ex, 404, "{\"error\":\"Endpoint not found.\"}");
    }

    private void ensureDefaultAchievements(int playerId) throws SQLException {
        List<Achievement> list = achievements.getAchievementsByPlayer(playerId);
        if (list.isEmpty()) {
            achievements.addAchievement(new Achievement(playerId, "First Blood", "Win your first battle in the Arena.", false));
            achievements.addAchievement(new Achievement(playerId, "Armorer", "Add an advanced weapon to your armory.", false));
            achievements.addAchievement(new Achievement(playerId, "Demon Slayer", "Defeat the Archdemon boss.", false));
            achievements.addAchievement(new Achievement(playerId, "High Scorer", "Surpass 1,000 leaderboard points.", false));
        }
    }

    private String dashboardJson(Player p) throws SQLException {
        ensureDefaultAchievements(p.getPlayerId());
        List<Character> chars = characters.getCharactersByPlayer(p.getPlayerId());
        List<Inventory> items = inventory.getInventoryByPlayer(p.getPlayerId());
        List<LeaderboardEntry> board = scores.getLeaderboardEntries();
        List<Achievement> awards = achievements.getAchievementsByPlayer(p.getPlayerId());
        int rank = board.stream().filter(x -> x.name().equals(p.getName())).mapToInt(LeaderboardEntry::rank).findFirst().orElse(0);
        int highScore = board.stream().filter(x -> x.name().equals(p.getName())).mapToInt(LeaderboardEntry::highScore).findFirst().orElse(0);
        return "{\"player\":{" + playerFields(p) + ",\"rank\":" + rank + ",\"highScore\":" + highScore + "},\"characters\":" + charsJson(chars) + ",\"inventory\":" + inventoryJson(items) + ",\"board\":" + boardJson(board) + ",\"achievements\":" + achievementsJson(awards) + "}";
    }
    private String playerJson(Player p) { return "{\"id\":" + p.getPlayerId() + ",\"name\":\"" + escape(p.getName()) + "\"}"; }
    private String playerFields(Player p) { return "\"id\":" + p.getPlayerId() + ",\"name\":\"" + escape(p.getName()) + "\",\"level\":" + p.getLevel() + ",\"xp\":" + p.getXp() + ",\"coins\":" + p.getCoins(); }
    private String charsJson(List<Character> rows) { return rows.stream().map(c -> "{\"id\":"+c.getCharacterId()+",\"name\":\""+escape(c.getCharacterName())+"\",\"className\":\""+c.getCharacterClass()+"\",\"hp\":"+c.getHealth()+",\"atk\":"+c.getAttackStat()+",\"def\":"+c.getDefense()+"}").collect(java.util.stream.Collectors.joining(",", "[", "]")); }
    private String inventoryJson(List<Inventory> rows) { return rows.stream().map(i -> { String[] raw=i.getWeaponName().split("\\|",2); String type=raw.length==2?raw[0]:"Sword"; String name=raw.length==2?raw[1]:raw[0]; String effect=type.equals("Bow")?"Piercing shot can hit two enemies.":"Bleed damage over time."; return "{\"name\":\""+escape(name)+"\",\"type\":\""+type+"\",\"damage\":"+i.getDamage()+",\"quantity\":"+i.getQuantity()+",\"effect\":\""+effect+"\"}"; }).collect(java.util.stream.Collectors.joining(",", "[", "]")); }
    private String boardJson(List<LeaderboardEntry> rows) { return rows.stream().map(x -> "[\""+escape(x.name())+"\","+x.highScore()+"]").collect(java.util.stream.Collectors.joining(",", "[", "]")); }
    private String achievementsJson(List<Achievement> rows) { return rows.stream().map(a -> "{\"title\":\""+escape(a.getTitle())+"\",\"text\":\""+escape(a.getDescription())+"\",\"icon\":\"✦\",\"unlocked\":"+a.isUnlocked()+"}").collect(java.util.stream.Collectors.joining(",", "[", "]")); }
    private void staticFile(HttpExchange ex, String path) throws IOException {
        String cleanPath = path.equals("/") ? "/index.html" : path;
        String resource = "/web" + cleanPath;
        byte[] body = null;
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            if (in != null) {
                body = in.readAllBytes();
            }
        }
        if (body == null) {
            java.nio.file.Path filePath = java.nio.file.Path.of("web" + cleanPath);
            if (!java.nio.file.Files.exists(filePath)) {
                filePath = java.nio.file.Path.of("PlayerManagementSystem", "web" + cleanPath);
            }
            if (java.nio.file.Files.exists(filePath) && !java.nio.file.Files.isDirectory(filePath)) {
                body = java.nio.file.Files.readAllBytes(filePath);
            }
        }
        if (body == null) {
            ex.sendResponseHeaders(404, -1);
            ex.close();
            return;
        }
        try {
            ex.getResponseHeaders().set("Content-Type",
                    resource.endsWith(".css") ? "text/css; charset=utf-8" :
                    resource.endsWith(".js") ? "application/javascript; charset=utf-8" :
                    "text/html; charset=utf-8");
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
        } finally {
            ex.close();
        }
    }
    private void json(HttpExchange ex,int status,String body)throws IOException{byte[] bytes=body.getBytes(StandardCharsets.UTF_8);ex.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");ex.sendResponseHeaders(status,bytes.length);ex.getResponseBody().write(bytes);ex.close();}
    private Map<String,String> form(InputStream in)throws IOException{return query(new String(in.readAllBytes(),StandardCharsets.UTF_8));}
    private Map<String,String> query(String input){Map<String,String> result=new LinkedHashMap<>();if(input==null||input.isBlank())return result;for(String pair:input.split("&")){String[] p=pair.split("=",2);result.put(URLDecoder.decode(p[0],StandardCharsets.UTF_8),URLDecoder.decode(p.length>1?p[1]:"",StandardCharsets.UTF_8));}return result;}
    private void require(Map<String,String> p,String... keys){for(String key:keys)if(!p.containsKey(key)||p.get(key).isBlank())throw new IllegalArgumentException(key+" is required.");}
    private int integer(Map<String,String> p,String key){require(p,key);try{return Integer.parseInt(p.get(key));}catch(NumberFormatException e){throw new IllegalArgumentException(key+" must be a number.");}}
    private String escape(String value){return value==null?"":value.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");}
}
