package com.irdem.tunama.data;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.Map;

public class SubclassManager {

    private Map<String, Subclass> subclasses;
    private JavaPlugin plugin;

    public SubclassManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.subclasses = new HashMap<>();
        loadSubclasses();
    }

    private void loadSubclasses() {
        // Guerrero - Bersker
        Subclass bersker = new Subclass("bersker", "Bersker", "Un maestro en el arte del combate que decidió abandonar toda armadura a cambio de pegar mas fuerte", "guerrero");
        bersker.setAdvantages("Gran cantidad vida, Gran daño a melé, puede absorber una parte del daño (absorbe el 10% del daño recibido como curación)");
        bersker.setDisadvantages("Tiene 0 de Defensa Fija, débil en habilidades de rango");
        subclasses.put("bersker", bersker);

        // Guerrero - Maestro de Armas
        Subclass maestroArmas = new Subclass("maestro-de-armas", "Maestro de Armas", "Un maestro en el arte del combate con alta defensa que perdió la capacidad de absorber el daño", "guerrero");
        maestroArmas.setAdvantages("Buena defensa y vida, fuerte daño a melé");
        maestroArmas.setDisadvantages("Movimiento lento, débil en habilidades de rango");
        subclasses.put("maestro-de-armas", maestroArmas);

        // Monje - Shaolin
        Subclass shaolin = new Subclass("shaolin", "Shaolin", "Un luchador experimentado que abandona su movilidad a cambio de ganar mas daño", "monje");
        shaolin.setAdvantages("Gran daño a melé, puede stunear enemigos (5% Probabilidad)");
        shaolin.setDisadvantages("Defensa baja, débil en habilidades de rango tanto magias, trampas y arcos");
        subclasses.put("shaolin", shaolin);

        // Monje - Maestro Zen
        Subclass maestroZen = new Subclass("maestro-zen", "Maestro Zen", "Un luchador experimentado con mucha velocidad pero perdió la capacidad de stunear a los enemigos a cambio de mas daño", "monje");
        maestroZen.setAdvantages("Movimiento alto, Gran daño a melé");
        maestroZen.setDisadvantages("Defensa baja, débil en habilidades de rango tanto magias, trampas y arcos");
        subclasses.put("maestro-zen", maestroZen);

        // Mago - Elementalista
        Subclass elementalista = new Subclass("elementalista", "Elementalista", "Poderoso hechicero con alto daño mágico que se especializo en el dominio de los elementos", "mago");
        elementalista.setAdvantages("Alto daño mágico, variedad de hechizos de diferentes elementos, ataca a distancia");
        elementalista.setDisadvantages("Baja defensa, débil en combate cuerpo a cuerpo");
        subclasses.put("elementalista", elementalista);

        // Mago - Mago de Combate
        Subclass magoCombate = new Subclass("mago-de-combate", "Mago de Combate", "Poderoso hechicero con alto daño mágico cuerpo a cuerpo", "mago");
        magoCombate.setAdvantages("Alto daño mágico con poderes concentrado en sus armas cuerpo a cuerpo");
        magoCombate.setDisadvantages("Nula capacitad de combate a distancia");
        subclasses.put("mago-de-combate", magoCombate);

        // Invocador - Brujo
        Subclass brujo = new Subclass("brujo", "Brujo", "Hechicero que invoca demonios que luchen por el", "invocador");
        brujo.setAdvantages("Invoca demonios, las demonios pelean por ti, puede invocar varias demonios a la vez");
        brujo.setDisadvantages("Débil en habilidades melé, defensa baja, débil en habilidades de distancia");
        subclasses.put("brujo", brujo);

        // Invocador - Chamán
        Subclass chaman = new Subclass("chaman", "Chamán", "Hechicero que invoca elementales para que luchen por el", "invocador");
        chaman.setAdvantages("Invoca elementales, las elementales pelean por ti, puede invocar varias elementales a la vez");
        chaman.setDisadvantages("Débil en habilidades melé, defensa baja, débil en habilidades de distancia");
        subclasses.put("chaman", chaman);

        // Arquero - Francotirador
        Subclass francotirador = new Subclass("francotirador", "Francotirador", "Un preciso luchador a distancia con alto critico que sacrifico su movilidad para ello", "arquero");
        francotirador.setAdvantages("Alta cantidad de critico, daño fuerte a distancia, puede golpear a los enemigos a distancia con su arco");
        francotirador.setDisadvantages("Baja defensa y débil en habilidad a melé");
        subclasses.put("francotirador", francotirador);

        // Arquero - Guardabosques
        Subclass guardabosques = new Subclass("guardabosques", "Guardabosques", "Un preciso luchador a distancia con alta movilidad", "arquero");
        guardabosques.setAdvantages("Alta velocidad de movimiento, daño fuerte a distancia, puede golpear a los enemigos a distancia con su arco");
        guardabosques.setDisadvantages("Baja defensa y débil en habilidad a melé");
        subclasses.put("guardabosques", guardabosques);

        // Pícaro - Asesino
        Subclass asesino = new Subclass("asesino", "Asesino", "Luchador hábil con alta posibilidad de hacer golpes críticos y gran movilidad", "picaro");
        asesino.setAdvantages("Alta posibilidad de critico, alta movilidad, hace mas daño si golpea por la espalda");
        asesino.setDisadvantages("Baja defensa y nula habilidad a distancia ( no puede usar arcos, armas a distancia)");
        subclasses.put("asesino", asesino);

        // Pícaro - Asaltante
        Subclass asaltante = new Subclass("asaltante", "Asaltante", "Luchador hábil con alta posibilidad de hacer golpes críticos y gran movilidad, que perdió la capacidad de hacerse invisible para pegar mas fuerte", "picaro");
        asaltante.setAdvantages("Alta posibilidad de critico, alto daño, alta movilidad, hace mas daño si golpea por la espalda");
        asaltante.setDisadvantages("Nula habilidad a distancia ( no puede usar arcos, armas a distancia), ya no puede hacerse invisible");
        subclasses.put("asaltante", asaltante);

        // Paladín - Paladín Sagrado
        Subclass paladinSagrado = new Subclass("paladin-sagrado", "Paladín Sagrado", "Luchador balanceado en combate que puede curar tanto a si mismo como a aliados", "paladin");
        paladinSagrado.setAdvantages("Alta defensa y vida, curaciones y habilidades variadas");
        paladinSagrado.setDisadvantages("Débil tanto en habilidades melé como distancia");
        subclasses.put("paladin-sagrado", paladinSagrado);

        // Paladín - Paladín del Caos
        Subclass paladinCaos = new Subclass("paladin-del-caos", "Paladín del Caos", "Luchador que decidió abandonar a sus dioses y abrazar el caos para hacer daño en vez de proteger", "paladin");
        paladinCaos.setAdvantages("Alta defensa, vida, daño, y habilidades variadas");
        paladinCaos.setDisadvantages("Débil tanto en habilidades distancia");
        subclasses.put("paladin-del-caos", paladinCaos);

        // Nigromante - Lich
        Subclass lich = new Subclass("lich", "Lich", "Hechicero Oscuro con invocación de muertos y usa habilidades de putrefacción", "nigromante");
        lich.setAdvantages("Invoca no muertos, Alto daño mágico, debilita a los enemigos con su putrefacción");
        lich.setDisadvantages("Baja defensa, Débil en habilidades a melé");
        subclasses.put("lich", lich);

        // Nigromante - Caballero de la Muerte
        Subclass caballerMuerte = new Subclass("caballero-de-la-muerte", "Caballero de la Muerte", "Hechicero Oscuro que decidió pelear cuerpo a cuerpo con invocación de muertos y usa habilidades de putrefacción", "nigromante");
        caballerMuerte.setAdvantages("Invoca no muertos, debilita a los enemigos con su putrefacción");
        caballerMuerte.setDisadvantages("Débil en habilidades a distancia");
        subclasses.put("caballero-de-la-muerte", caballerMuerte);

        // Druida - Licántropo
        Subclass licantropo = new Subclass("licantropo", "Licántropo", "Se convirtió tantas veces en lobo que perdió el resto de transformaciones", "druida");
        licantropo.setAdvantages("Se transforma en animales, se puede curar a si mismo, magia de naturaleza");
        licantropo.setDisadvantages("Baja defensa en formas de animales y Débil en habilidades a melé fuera de forma de animales");
        subclasses.put("licantropo", licantropo);

        // Druida - Archidruida
        Subclass archidruida = new Subclass("archidruida", "Archidruida", "Usa magia de Naturaleza y transformaciones", "druida");
        archidruida.setAdvantages("Se transforma en animales, se puede curar a si mismo, magia de naturaleza");
        archidruida.setDisadvantages("Baja defensa en formas de animales y Débil en habilidades a melé fuera de forma de animales");
        subclasses.put("archidruida", archidruida);

        // Evocador - Salvaguarda
        Subclass salvaguarda = new Subclass("salvaguarda", "Salvaguarda", "Usa magia su magia apoyar a los aliados y puede hacer algo de daño", "evocador");
        salvaguarda.setAdvantages("Magias de soporte y decente daño a distancia");
        salvaguarda.setDisadvantages("Baja vida y débil daño a melé");
        subclasses.put("salvaguarda", salvaguarda);

        // Evocador - Destructor
        Subclass destructor = new Subclass("destructor", "Destructor", "Abandono su magia de apoyo para hacer daño al resto", "evocador");
        destructor.setAdvantages("Alto daño a distancia");
        destructor.setDisadvantages("Baja vida y débil daño a melé");
        subclasses.put("destructor", destructor);

        // Cazador - Maestro de la Manada
        Subclass maestroManada = new Subclass("maestro-de-la-manada", "Maestro de la Manada", "Pelea junto a su mascota compañera a distancia", "cazador");
        maestroManada.setAdvantages("Disponibilidad de mascota y buena velocidad de movimiento");
        maestroManada.setDisadvantages("Baja defensa y vida, nula capacidad de combate cuerpo a cuerpo");
        subclasses.put("maestro-de-la-manada", maestroManada);

        // Cazador - Combatiente Primigenio
        Subclass combatientePrimigenio = new Subclass("combatiente-primigenio", "Combatiente Primigenio", "Pelea junto a su mascota compañera de manera cuerpo a cuerpo", "cazador");
        combatientePrimigenio.setAdvantages("Disponibilidad de mascota y buena velocidad de movimiento");
        combatientePrimigenio.setDisadvantages("Baja vida, nula capacidad de combate a distancia");
        subclasses.put("combatiente-primigenio", combatientePrimigenio);

        // Sacerdote - Primarca
        Subclass primarca = new Subclass("primarca", "Primarca", "Enfocado a curar a tus compañeros a cambio de tener poco daño", "sacerdote");
        primarca.setAdvantages("Alta curación propia a y a los aliados y habilidades de escudo");
        primarca.setDisadvantages("No tiene daño a melé y tiene a distancia débil");
        subclasses.put("primarca", primarca);

        // Sacerdote - Sacerdote Corrupto
        Subclass sacerdoteCorrupto = new Subclass("sacerdote-corrupto", "Sacerdote Corrupto", "Abandona todas las curaciones a cambio de tener mas daño", "sacerdote");
        sacerdoteCorrupto.setAdvantages("Buen daño");
        sacerdoteCorrupto.setDisadvantages("No tiene daño a melé y no puede curar");
        subclasses.put("sacerdote-corrupto", sacerdoteCorrupto);

        // Trampero - Maestro de las Trampas
        Subclass maestroTrampas = new Subclass("maestro-de-las-trampas", "Maestro de las Trampas", "Pelea basándose en sus trampas", "trampero");
        maestroTrampas.setAdvantages("Alto daño en trampas");
        maestroTrampas.setDisadvantages("No puede usar combate cuerpo a cuerpo ni a distancia");
        subclasses.put("maestro-de-las-trampas", maestroTrampas);

        // Trampero - Ingeniero
        Subclass ingeniero = new Subclass("ingeniero", "Ingeniero", "Pelea basándose en sus torretas", "trampero");
        ingeniero.setAdvantages("Alto daño en torretas");
        ingeniero.setDisadvantages("No puede usar combate cuerpo a cuerpo ni a distancia");
        subclasses.put("ingeniero", ingeniero);

        plugin.getLogger().info("Se cargaron " + subclasses.size() + " subclases correctamente");
    }

    public Subclass getSubclass(String id) {
        return subclasses.get(id.toLowerCase());
    }

    public Map<String, Subclass> getAllSubclasses() {
        return new HashMap<>(subclasses);
    }

    public boolean isValidSubclass(String id) {
        return subclasses.containsKey(id.toLowerCase());
    }
}
