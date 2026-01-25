# TunamaRPG Copilot Instructions

## Project Overview
TunamaRPG is a comprehensive Spigot/Paper Minecraft plugin implementing a complete RPG system with races, classes, subclasses, player statistics, missions, achievements, and clans. The architecture follows a manager-based pattern where data is centrally managed and accessed through singleton-like instances.

## Architecture & Component Structure

### Core Plugin Bootstrap ([TunamaRPG.java](src/main/java/com/irdem/tunama/TunamaRPG.java))
- Single `JavaPlugin` entry point that initializes all systems in sequence
- Initialization order is critical: config → database → all managers → commands/listeners
- All managers are stored as static fields accessible via getters
- If database connection fails, plugin disables itself automatically

### Manager Pattern (Central Organization)
The plugin uses specialized managers as singletons to handle domain logic:
- **ConfigManager**: YAML configuration loading with defaults
- **DatabaseManager**: Handles SQLite/MySQL connections
- **RaceManager, ClassManager, SubclassManager**: Load RPG definitions from YAML files
- **PlayerData + Database**: Maintains player progression state
- **MissionManager, AchievementManager, AbilityManager**: Handle progression content

Access from any class via: `plugin.getManager()` (e.g., `plugin.getClassManager()`)

### Data Layer
- **PlayerData**: Core entity holding UUID, race, class, subclass, level, exp, stats, equipment slots
- **PlayerStats**: Separate object containing derived statistics (health, damage, mana)
- **YAML Config Files**: Races and classes defined in `resources/clases/` and `resources/razas/` - loaded once at startup
- Database: SQLite (default) or MySQL for persistence

### Command & Menu System
- **RPGCommand**: Main router for all `/rpg` and related commands
- **Menu GUIs**: Custom inventory-based menus (MainMenuGUI, RaceMenu, ClassMenu) for user interaction
- Menu listeners detect inventory clicks and handle player selections
- Players without race/class are forced into selection menus on first `/rpg` command

## Critical Development Patterns

### Handling Player Data
```java
// Standard pattern: fetch from DB or create new
PlayerData data = plugin.getDatabaseManager().getOrCreatePlayerData(player.getUniqueId(), player.getName());
if (data == null) {
    player.sendMessage("§c✗ Error: No player data");
    return;
}
```

### Configuration Access
- All config options use dotted notation: `config.get("database.type")`, `config.get("stats.life-per-level")`
- Defaults are auto-created if config.yml is missing
- Spanish language preference hardcoded; change in `plugin.language` config

### YAML-Driven Content
- Race/class definitions are YAML files loaded into memory managers
- Each race file contains stat modifiers; each class file contains abilities/skills
- Adding new races/classes: create YAML, ensure files are in both `src/main/resources/` AND bundled in JAR

### Database Abstraction
- DatabaseManager handles both SQLite and MySQL transparently
- Type selected via `database.type` config (default: sqlite)
- Connection pooling not currently used; single connection pattern

## Build & Testing

### Build Command
```bash
mvn clean package
```
Produces `target/rpg-0.0.1.jar` for deployment to `plugins/` folder.

### Dependencies
- **Spigot API 1.20.1**: Provided by server at runtime (not bundled)
- **SQLite JDBC 3.44**: Bundled (SQLite driver)
- **MySQL Connector 8.2**: Bundled (MySQL driver)
- **JUnit 4.13**: Test framework (test scope)
- Java 1.8 source/target (Maven compiler config)

### Testing
Basic test structure in [src/test/java/com/irdem/tunama/AppTest.java](src/test/java/com/irdem/tunama/AppTest.java). No integration tests for plugin hooks—recommend manual testing on Paper server.

## Plugin Lifecycle & Event Handling

### Startup Flow
1. `onEnable()` is called
2. Creates plugin data folders
3. Loads config.yml (creates defaults if missing)
4. Connects to database (SQLite file or MySQL)
5. Initializes all managers (races, classes, subclasses, missions, achievements, abilities)
6. Registers commands and event listeners
7. Logs startup message with version

### Event Listeners ([listeners/](src/main/java/com/irdem/tunama/listeners/))
- **PlayerListener**: Handles join/quit events, stat syncing, respawn mechanics
- **PlayerMenuListener**: Tracks menu state per player
- **MenuClickListener**: Routes inventory click events to menu handlers

### Key External Integrations
- **Vault**: Optional permission system integration (softdepend in plugin.yml)
- **Essentials**: Optional economy integration (softdepend in plugin.yml)
- Check `config.yml` `plugins.vault.enabled` and `plugins.essentials.enabled` flags before use

## Project-Specific Conventions

### Naming & Localization
- All player-facing messages in Spanish by default
- Success messages use `§a✓`, error messages use `§c✗` prefix
- Command return `true` to suppress unknown command error; `false` to show help

### Stat Modifiers
Seven core stats computed from race+class combinations:
- Vida (health), Fuerza (strength), Agilidad (agility), Inteligencia (intelligence), Poder Sagrado, Poder Corrupto, Poder Naturaleza
- Rates applied: `life-per-level`, `strength-per-level` etc. from config for progression
- Storage: in PlayerStats object; persisted to database

### File Organization
```
src/main/
  java/com/irdem/tunama/
    commands/        # Command executors
    config/          # Configuration management
    data/            # Core data models & managers
    database/        # DB abstraction layer
    listeners/       # Bukkit event handlers
    menus/           # GUI inventory menus
  resources/
    config.yml       # Main plugin config
    plugin.yml       # Bukkit plugin metadata
    clases/          # Class YAML definitions (13 classes)
    razas/           # Race YAML definitions (9 races)
```

## Common Modification Points

### Adding a New Command
1. Implement `CommandExecutor` in [commands/](src/main/java/com/irdem/tunama/commands/)
2. Register in `registerCommands()` in TunamaRPG.java
3. Add command entry to [plugin.yml](src/main/resources/plugin.yml)
4. Update help text (Spanish)

### Adding a New RPG Race or Class
1. Create YAML file in `src/main/resources/clases/` or `src/main/resources/razas/`
2. Include stat modifier keys matching PlayerStats field names
3. Manager will auto-load on plugin startup
4. Ensure file is included in Maven build output

### Modifying Player Data Persistence
1. Update [PlayerData.java](src/main/java/com/irdem/tunama/data/PlayerData.java) model
2. Update database schema in DatabaseManager (migration logic)
3. Update save/load methods in DatabaseManager

### Adding Menu GUIs
1. Extend menu base class (see [menus/](src/main/java/com/irdem/tunama/menus/) pattern)
2. Use Bukkit InventoryView API for chest-based UI
3. Register click listener in `registerListeners()` (TunamaRPG.java)

## Known Limitations & Gaps

- No async database operations—all DB calls block main thread
- No configuration reloading; requires plugin restart
- No command permission checks visible (assumed Vault handles)
- DatabaseManager does not implement connection pooling
- App.java is a placeholder (not used)

## Before Committing Changes

1. Verify all **manager initializations** complete in TunamaRPG.onEnable()
2. Test player progression: race → class → subclass flow
3. Check Spanish message localization is consistent
4. Run `mvn clean package` and verify no build errors
5. Ensure new YAML config files are in both `src/main/resources/` and copied to target
