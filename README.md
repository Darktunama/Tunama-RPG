# TunameRPG - Plugin RPG para Minecraft

Un plugin RPG completo y modular para servidores de Minecraft Spigot/Paper que incluye un sistema de razas, clases, subclases, estadísticas, misiones, logros, clanes y mucho más.

## Características Principales

### 🎮 Sistema de Razas (9 razas)
- **Humano**: Versátil pero sin destacar
- **Elfo**: Débil en vitalidad pero de alto daño
- **Semi-Elfo**: Equilibrio entre Elfo y Humano
- **Orco**: Bestia salvaje con gran fuerza
- **Tiflyn**: Malditos por el Infierno
- **Enano**: Orgulloso y fuerte
- **Dragoneante**: Nacidos del Dragón
- **Goblin**: Avaricioso y astuto
- **No Muerto**: Que nunca se detienen

### 👥 Sistema de Clases (13 clases)
1. **Guerrero** - Maestro del combate con alta defensa
2. **Monje** - Luchador rápido y ágil
3. **Mago** - Hechicero con alto daño mágico
4. **Invocador** - Hechicero que invoca criaturas
5. **Arquero** - Luchador de rango con alta movilidad
6. **Pícaro** - Luchador hábil con altos críticos
7. **Paladín** - Luchador balanceado y sanador
8. **Nigromante** - Hechicero oscuro
9. **Druida** - Mago de naturaleza y transformaciones
10. **Evocador** - Mago de soporte
11. **Cazador** - Luchador con mascota
12. **Sacerdote** - Sanador especializado
13. **Trampero** - Experto en trampas

### ⚡ Sistema de Subclases (26 subclases)
Cada clase tiene 2 subclases especializadas:
- **Guerrero**: Berserker, Maestro de Armas
- **Monje**: Shaolin, Maestro Zen
- **Mago**: Elementalista, Mago de Combate
- Y muchas más...

### 📊 Estadísticas
- **Vida**: Resistencia y durabilidad
- **Fuerza**: Daño a melé
- **Agilidad**: Velocidad y críticos
- **Inteligencia**: Daño mágico
- **Poder Sagrado**: Poder divino
- **Poder Corrupto**: Poder oscuro
- **Poder Naturaleza**: Poder natural

### 🎯 Características Adicionales
- ✅ Sistema de experiencia y progresión
- ✅ Misiones y logros
- ✅ Sistema de recompensas
- ✅ Sistema de Top usuarios
- ✅ Sistema de clanes
- ✅ Compatibilidad con Vault
- ✅ Compatibilidad con EssentialsX
- ✅ Soporte para MySQL y SQLite

## Instalación

### Requisitos
- Servidor Spigot/Paper 1.19.4+
- Java 1.8+
- Maven (para compilar desde código fuente)

### Pasos de Instalación

1. **Descargar o compilar el plugin**
   ```bash
   mvn clean package
   ```

2. **Copiar el JAR al servidor**
   ```
   Copiar target/rpg-0.0.1.jar a plugins/
   ```

3. **Reiniciar el servidor**
   ```
   /reload confirm
   ```

4. **Configurar la base de datos** (Opcional)
   Editar `plugins/TunameRPG/config.yml` y establecer:
   - SQLite (por defecto) - Sin configuración adicional
   - MySQL - Proporcionar credenciales

## Configuración

### config.yml

```yaml
# Tipo de base de datos: sqlite o mysql
database:
  type: sqlite
  
  # Configuración SQLite
  sqlite:
    file: plugins/TunameRPG/rpg.db
  
  # Configuración MySQL
  mysql:
    host: localhost
    port: 3306
    database: tunama_rpg
    username: root
    password: password
```

## Comandos

### Comandos Principales
- `/rpg help` - Ver ayuda
- `/rpg razas` - Ver todas las razas
- `/rpg clases` - Ver todas las clases
- `/rpg info <raza|clase|subclase>` - Ver información detallada

### Próximos Comandos (En desarrollo)
- `/clase <subcomando>` - Gestionar tu clase
- `/mision` - Ver tus misiones
- `/logro` - Ver tus logros
- `/estadisticas [jugador]` - Ver estadísticas
- `/clan` - Gestionar tu clan

## Estructura del Proyecto

```
rpg/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/irdem/tunama/
│   │   │       ├── TunameRPG.java (Clase principal)
│   │   │       ├── config/
│   │   │       │   └── ConfigManager.java
│   │   │       ├── database/
│   │   │       │   └── DatabaseManager.java
│   │   │       ├── data/
│   │   │       │   ├── Race.java
│   │   │       │   ├── RaceManager.java
│   │   │       │   ├── RPGClass.java
│   │   │       │   ├── ClassManager.java
│   │   │       │   ├── Subclass.java
│   │   │       │   ├── SubclassManager.java
│   │   │       │   ├── PlayerData.java
│   │   │       │   └── PlayerStats.java
│   │   │       ├── commands/
│   │   │       │   └── RPGCommand.java
│   │   │       └── listeners/
│   │   │           └── PlayerListener.java
│   │   └── resources/
│   │       ├── plugin.yml
│   │       └── config.yml
│   └── test/
│       └── java/
│           └── com/irdem/tunama/
│               └── AppTest.java
└── pom.xml
```

## Base de Datos

### Tablas Principales
- `players` - Información de jugadores
- `player_stats` - Estadísticas de jugadores
- `player_quests` - Misiones completadas
- `player_achievements` - Logros completados
- `clans` - Información de clanes
- `clan_members` - Miembros de clanes

## Desarrollo

### Compilar el proyecto
```bash
mvn clean compile
```

### Crear el JAR
```bash
mvn clean package
```

### Ejecutar pruebas
```bash
mvn test
```

## Próximas Características

- [ ] Sistema de combate avanzado
- [ ] Sistema de habilidades
- [ ] Sistema de objetos y equipamiento
- [ ] Sistema de tienda
- [ ] Sistema de cofres del tesoro
- [ ] Eventos especiales
- [ ] Dungeons y mazmorras
- [ ] Sistema de mascotas
- [ ] Interfaz gráfica en juego

## Contribuciones

Las contribuciones son bienvenidas. Por favor:
1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## Licencia

Este proyecto está bajo la licencia MIT. Consulta el archivo LICENSE para más detalles.

## Autor

**Irdem Tunama**
- Email: [tu-email@ejemplo.com]
- GitHub: [tu-github]

## Soporte

Para reportar bugs o solicitar nuevas características, por favor crea un issue en el repositorio.

---

**Nota**: Este plugin está en desarrollo activo. Algunas características pueden no estar completamente implementadas.
