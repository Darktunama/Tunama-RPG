<div align="center">

# ⚔️ TunamaRPG

**Un plugin RPG completo para servidores Paper Minecraft 1.21**

![Version](https://img.shields.io/badge/Versión-0.1.08-blue?style=for-the-badge)
![Paper](https://img.shields.io/badge/Paper-1.21-green?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge)
![License](https://img.shields.io/badge/Licencia-Privado-red?style=for-the-badge)

*Sistema RPG completo con clases, razas, habilidades, mascotas, clanes, misiones y mucho más.*

</div>

---

## 📋 Tabla de Contenidos

- [Características](#-características)
- [Requisitos](#-requisitos)
- [Instalación](#-instalación)
- [Clases](#-clases)
- [Razas](#-razas)
- [Sistema de Habilidades](#-sistema-de-habilidades)
- [Mascotas](#-mascotas)
- [Sistema de Clanes](#-sistema-de-clanes)
- [Comandos](#-comandos)
- [Permisos](#-permisos)
- [Configuración](#-configuración)
- [Dependencias](#-dependencias)

---

## ✨ Características

| Sistema | Descripción |
|---------|-------------|
| 🧙 **13 Clases** | Cada clase con habilidades únicas, subclases y mecánicas propias |
| 🧬 **9 Razas** | Razas jugables con bonificadores de estadísticas únicos |
| ⚡ **83 Habilidades** | Sistema de barra de habilidades con casteo estático y móvil |
| 🐾 **9 Mascotas** | Mascotas invocables que combaten junto al jugador |
| 🏰 **Sistema de Clanes** | Clanes con banco, guerras, alianzas, rangos y clasificaciones |
| 🔄 **Transformaciones** | El Druida puede transformarse en 6 formas animales distintas |
| 🐲 **Invocaciones** | Elementales de Fuego, Aire, Agua y Tierra con IA propia |
| 🎯 **Misiones** | Sistema de misiones con seguimiento y recompensas |
| 🏆 **Logros** | Sistema de logros desbloqueables |
| 📊 **Scoreboard** | Scoreboard dinámico configurable |
| 💾 **Base de datos** | Soporte para SQLite y MySQL |
| 🔌 **Placeholders** | Integración con PlaceholderAPI |

---

## 🔧 Requisitos

- **Servidor:** [Paper](https://papermc.io/) 1.21 o superior
- **Java:** 21 o superior
- **Dependencias opcionales:** Vault, Essentials, PlaceholderAPI, LuckPerms

---

## 📦 Instalación

1. Descarga el archivo `.jar` de la última [Release](https://github.com/Darktunama/Tunama-RPG/releases)
2. Colócalo en la carpeta `plugins/` de tu servidor
3. Reinicia el servidor
4. Configura `plugins/TunamaRPG/config.yml` según tus necesidades
5. Vuelve a reiniciar o ejecuta `/rpg reload`

---

## 🧙 Clases

El plugin cuenta con **13 clases jugables**, cada una con mecánicas, estadísticas y habilidades únicas. Cada clase tiene **2 subclases** para especialización avanzada.

| # | Clase | Descripción | Subclases |
|---|-------|-------------|-----------|
| 1 | ⚔️ **Paladín** | Combatiente equilibrado con capacidad de curación | Paladín Sagrado · Paladín del Caos |
| 2 | 🛡️ **Guerrero** | Maestro del combate con alta defensa | Berserker · Maestro de Armas |
| 3 | 🔮 **Mago** | Hechicero con alto daño mágico y variedad de conjuros | Elementalista · Mago de Combate |
| 4 | 🏹 **Arquero** | Combatiente a distancia con alta movilidad | Francotirador · Guardabosques |
| 5 | 🗡️ **Pícaro** | Combatiente sigiloso con alta probabilidad de crítico | Asesino · Asaltante |
| 6 | ✨ **Sacerdote** | Especialista en curación y apoyo a aliados | Primarca · Sacerdote Corrupto |
| 7 | 👊 **Monje** | Combatiente veloz con alta velocidad de movimiento | Shaolin · Maestro Zen |
| 8 | 🌿 **Druida** | Magia de la naturaleza y transformaciones animales | Licántropo · Archidruida |
| 9 | 🐉 **Evocador** | Magia de fuego y dracónica de alto impacto | Salvaguarda · Destructor |
| 10 | 🌀 **Invocador** | Conjura elementales y criaturas para que luchen por él | Brujo · Chamán |
| 11 | 💀 **Nigromante** | Magia oscura e invocación de no muertos | Lich · Caballero de la Muerte |
| 12 | 🐺 **Cazador** | Combate junto a su mascota compañera | Maestro de la Manada · Combatiente Primigenio |
| 13 | 🪤 **Trampero** | Combate basado en trampas y torretas | Maestro de las Trampas · Ingeniero |

---

## 🧬 Razas

El plugin incluye **9 razas jugables**, cada una con bonificadores de estadísticas únicos que afectan al combate y las habilidades.

| Raza | Descripción |
|------|-------------|
| 👤 **Humano** | Raza versátil equilibrada en todas las estadísticas |
| 🧝 **Elfo** | Alta agilidad e inteligencia, baja vitalidad |
| ⛏️ **Enano** | Alta resistencia y fuerza, baja velocidad |
| 💪 **Orco** | Altísima fuerza bruta, baja inteligencia |
| 💰 **Goblin** | Alta agilidad y poder corrupto |
| 🌟 **SemiElfo** | Equilibrio entre humano y elfo |
| 🔥 **Tiflyn** | Nacidos de humanos maldecidos, alto poder corrupto |
| 🐲 **Dragoneante** | Nacidos del dragón, alto poder sagrado y resistencia |
| ☠️ **No Muerto** | La muerte nunca los detuvo, resistencia y poder oscuro |

---

## ⚡ Sistema de Habilidades

Las habilidades se activan mediante una **barra de habilidades** asignada al inventario del jugador. Existen dos modos de casteo:

- **Estático** — El jugador debe permanecer quieto durante el casteo
- **Móvil** — Se puede usar en movimiento

### Daño

Las habilidades escalan con las estadísticas del jugador mediante multiplicadores configurables:

```
Daño = (Fuerza × escala) + (Inteligencia × escala) + (Agilidad × escala) + ...
```

Estadísticas disponibles: **Vida · Fuerza · Agilidad · Inteligencia · Poder Sagrado · Poder Corrupto · Poder Natural**

### Habilidades por Clase

<details>
<summary>🛡️ Guerrero — 8 habilidades</summary>

| Habilidad | Nivel | Tipo | CD | Descripción |
|-----------|-------|------|----|-------------|
| Corte Profundo | 1 | Móvil | 30s | Causa sangrado al enemigo durante 10 segundos |
| Embestida | 5 | Estático | 15s | Se lanza hacia adelante golpeando al primer enemigo |
| Romper Corazas | 10 | Móvil | 20s | Reduce la armadura del objetivo temporalmente |
| Atronar | 15 | Estático | 25s | Golpe AoE que aturde a los enemigos cercanos |
| Sed de Sangre | 20 | Móvil | 60s | Roba vida en cada golpe durante su duración |
| Torbellino Sangriento | 22 | Móvil | 20s | Giro devastador que daña a todos los enemigos cercanos |
| Ejecutar | 25 | Móvil | 180s | Daño verdadero masivo. Si mata al objetivo, reinicia el cooldown |
| Ira Furibunda | 30 | Móvil | 90s | Entra en frenesí aumentando ataque y velocidad |

</details>

<details>
<summary>🔮 Mago — 8 habilidades</summary>

| Habilidad | Nivel | Tipo | CD | Descripción |
|-----------|-------|------|----|-------------|
| Bola de Fuego | 1 | Estático | 5s | Proyectil de fuego. Si mata al objetivo, explota en área (1.1 INT) |
| Pica de Hielo | 5 | Estático | 5s | Proyectil de hielo. 3 impactos en 30s congela al objetivo 2s |
| Implosión Arcana | 10 | Móvil | 3s | Explosión arcana alrededor del mago con knockback |
| Llamarada | 15 | Móvil | 20s | Cono de fuego de 2 bloques delante del lanzador |
| Ventisca | 20 | Estático | 25s | 5 oleadas de hielo en área. 3+ impactos congela 6s |
| Sifón de Maná | 25 | Móvil | 60s | Recupera el 60% del maná máximo instantáneamente |
| Salto Dimensional | 30 | Móvil | 30s | Teletransporte horizontal hasta 10 bloques |
| Elemento Antiguo | 35 | **Pasiva** | — | Todos los hechizos de mago ganan +30% de daño |

</details>

<details>
<summary>🏹 Arquero — 7 habilidades</summary>

| Habilidad | Nivel | Descripción |
|-----------|-------|-------------|
| Flecha Rápida | 1 | Flecha a alta velocidad |
| Flecha Cargada | 5 | Flecha de alto impacto con daño aumentado |
| Flecha Penetrante | 10 | Flecha que ignora armadura |
| Multi-Disparo | 15 | Dispara 5 flechas simultáneamente |
| Flecha Rebotante | 20 | Flecha que rebota hasta 3 enemigos |
| Flecha Negra | 22 | Flecha que aplica veneno |
| Disparo al Corazón | 25 | Buff de crítico potenciado durante 30s |

</details>

<details>
<summary>🌀 Invocador — 8 habilidades</summary>

| Habilidad | Nivel | CD | Descripción |
|-----------|-------|----|-------------|
| Erupción de Fuego | 1 | 15s | DoT de fuego en área de 3 bloques durante 5s |
| Elemental de Fuego | 5 | 70s | Invoca un Blaze que combate junto al jugador (60s) |
| Trueno Primigenio | 10 | 15s | Tormenta de rayos en área de 5 bloques |
| Elemental de Aire | 15 | 90s | Invoca un Elemental de Aire que combate junto al jugador (60s) |
| Maremoto | 20 | 15s | Marea de agua en área de 6 bloques con ralentización |
| Elemental de Agua | 25 | 100s | Invoca un Elemental de Agua que combate junto al jugador (60s) |
| Vulcano | 28 | 45s | Erupción de lava en área de 6 bloques con fuego |
| Elemental de Tierra | 30 | 140s | Invoca un Golem de Hierro que combate junto al jugador (60s) |

> Los elementales atacan a quien golpee al invocador y al mismo objetivo que el invocador ataque. No se atacan entre sí ni atacan al invocador.

</details>

<details>
<summary>🐉 Evocador — 8 habilidades</summary>

| Habilidad | Descripción |
|-----------|-------------|
| Llama de los Dragones | Proyectil de fuego dracónico de alto impacto |
| Llama Interior | Buff de fuego interno que potencia el daño |
| Llama Viva | Invoca una llama que persigue y quema al enemigo |
| Vuelo del Dragón | Propulsión horizontal a alta velocidad |
| Llama Bailarina | Llamas que danzan alrededor del objetivo |
| Rugido del Dragón | AoE de intimidación que debilita enemigos cercanos |
| Rayo de Dragones Ancestrales | Rayo devastador de alto daño |
| Llamada del Último Dragón | Habilidad definitiva de máximo poder dracónico |

</details>

<details>
<summary>🌿 Druida — Transformaciones + habilidades de forma</summary>

El Druida puede transformarse en **6 formas animales**, cada una con habilidades exclusivas:

| Forma | Habilidades Exclusivas |
|-------|----------------------|
| 🐺 Lobo | Zarpazo, Mordisco Infectado, Aullido de Manada |
| 🐻 Oso | Zarpazo, Mordisco, Golpe Pesado, Rabia de Oso |
| 🕷️ Araña | Mordisco, Veneno, Telaraña, Sentido de Vibración |
| 🦊 Zorro | Zarpazo, Mordisco, Esquivar |
| 🐼 Panda | Zarpazo, Mordisco, Golpe Pesado |
| 🌿 Warden | Grito Sónico, Onda de Choque |

</details>

<details>
<summary>🐺 Cazador — 8 habilidades</summary>

| Habilidad | Descripción |
|-----------|-------------|
| Orden de Ataque | Ordena a la mascota atacar a un objetivo |
| Cura Animal | Cura a la mascota compañera |
| Resucitar Mascota | Revive a la mascota si ha muerto |
| Rabia Animal | Potencia el ataque de la mascota temporalmente |
| Potencia de la Manada | Buff de grupo si hay varias mascotas activas |
| Golpe Sombras Animales | Ataque conjunto del jugador y su mascota |
| Segunda Mascota | Permite invocar una segunda mascota simultáneamente |
| Manada Necrótíca | Invoca una manada de criaturas espectrales |

</details>

---

## 🐾 Mascotas

Sistema de mascotas disponible principalmente para la clase **Cazador**. Las mascotas combaten activamente, pueden recibir órdenes y tienen sus propias habilidades.

| Mascota | Entidad |
|---------|---------|
| 🐺 Lobo | Wolf |
| 🐻 Oso Guardián | Polar Bear |
| 🕷️ Araña Venenosa | Spider |
| 🐆 Pantera Sombría | Cat |
| 🔥 Fénix Menor | Parrot |
| 🧟 Zombie Sirviente | Zombie |
| 💀 Esqueleto Arquero | Skeleton |
| 🌿 Espíritu del Bosque | Allay |
| ⚙️ Golem de Hierro | Iron Golem |

> Las mascotas e invocaciones **no sueltan objetos** al morir y se eliminan automáticamente al reiniciar el servidor.

---

## 🏰 Sistema de Clanes

| Característica | Descripción |
|----------------|-------------|
| 📝 Creación | Nombre, etiqueta y coste en monedas configurable |
| 👑 Rangos | Líder, Oficiales y Miembros con permisos diferenciados |
| 🏦 Banco | Los miembros pueden depositar y retirar oro del banco del clan |
| ⚔️ Guerras | Sistema de declaración y seguimiento de guerras entre clanes |
| 🤝 Alianzas | Clanes aliados que no pueden atacarse entre sí |
| 🏆 Clasificaciones | Top de clanes por nivel, kills PvP y victorias en guerra |
| 💬 Chat de clan | Canal de comunicación privado |
| 📋 Registro | Historial de acciones del clan |

---

## 📜 Comandos

| Comando | Descripción |
|---------|-------------|
| `/rpg` | Menú principal del plugin |
| `/rpg reload` | Recarga la configuración *(Admin)* |
| `/clase` | Gestión de clase del personaje |
| `/raza` | Información sobre razas |
| `/subclase` | Gestión de subclase |
| `/habilidades` | Activa/desactiva la barra de habilidades |
| `/mision` | Ver misiones activas y disponibles |
| `/logro` | Ver logros desbloqueados |
| `/estadisticas [jugador]` | Ver estadísticas de un jugador |
| `/top <tipo>` | Clasificaciones globales |
| `/clan <subcomando>` | Gestión completa del clan |
| `/equipo` | Menú de equipamiento y objetos RPG |

---

## 🔑 Permisos

| Permiso | Descripción | Por defecto |
|---------|-------------|-------------|
| `rpg.admin` | Acceso a todos los comandos de administración | OP |
| `rpg.user` | Acceso básico al plugin | Todos |
| `rpg.characters.1` | Permite 1 personaje | Todos |
| `rpg.characters.3` | Permite hasta 3 personajes | — |
| `rpg.characters.5` | Permite hasta 5 personajes | — |
| `rpg.characters.10` | Permite hasta 10 personajes | — |
| `rpg.characters.20` | Permite hasta 20 personajes | — |
| `rpg.pets` | Acceso al sistema de mascotas | Todos |

---

## ⚙️ Configuración

### Base de Datos

```yaml
database:
  type: sqlite          # sqlite o mysql

  sqlite:
    file: plugins/TunamaRPG/rpg.db

  mysql:
    host: localhost
    port: 3306
    database: tunama_rpg
    username: root
    password: tu_contraseña
    useSSL: false
```

### Clanes

```yaml
clans:
  min-members: 2        # Mínimo de miembros para crear clan
  max-members: 50       # Máximo de miembros por clan
  creation-cost: 1000   # Coste en monedas para crear un clan
```

### Experiencia

```yaml
experience:
  base-multiplier: 1.0        # Multiplicador base de experiencia
  quest-multiplier: 1.5       # Multiplicador de misiones
  achievement-multiplier: 1.2 # Multiplicador de logros
```

---

## 🔌 Dependencias

| Plugin | Tipo | Función |
|--------|------|---------|
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | Opcional | Sistema de economía para el banco del clan |
| [Essentials](https://essentialsx.net/) | Opcional | Integración de chat y comandos |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Opcional | Variables para otros plugins |
| [LuckPerms](https://luckperms.net/) | Opcional | Gestión avanzada de permisos |

### Variables de PlaceholderAPI

| Variable | Descripción |
|----------|-------------|
| `%rpg_raza%` | Raza del jugador |
| `%rpg_clase%` | Clase del jugador |
| `%rpg_subclase%` | Subclase del jugador |
| `%rpg_clan%` | Nombre del clan |
| `%rpg_clantag%` | Etiqueta del clan |
| `%rpg_nivel%` | Nivel del jugador |
| `%rpg_experiencia%` | Experiencia del jugador |

---

## 🏗️ Compilación

```bash
# Clonar el repositorio
git clone https://github.com/Darktunama/Tunama-RPG.git
cd Tunama-RPG

# Compilar con Maven (requiere Java 21)
mvn clean package

# El JAR se genera en target/rpg-<version>.jar
```

---

<div align="center">

**Desarrollado por Tunama** · Paper 1.21 · Java 21

</div>
