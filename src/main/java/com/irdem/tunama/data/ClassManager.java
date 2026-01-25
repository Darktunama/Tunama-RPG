package com.irdem.tunama.data;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.Map;

public class ClassManager {

    private Map<String, RPGClass> classes;
    private JavaPlugin plugin;

    public ClassManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.classes = new HashMap<>();
        loadClasses();
    }

    private void loadClasses() {
        // Guerrero
        RPGClass guerrero = new RPGClass("guerrero", "Guerrero", "Un maestro en el arte del combate con alta defensa");
        guerrero.setAdvantages("Buena defensa y vida, fuerte daño a melé, puede absorber una parte del daño (absorbe el 10% del daño recibido como curación)");
        guerrero.setDisadvantages("Movimiento lento, débil en habilidades de rango");
        guerrero.addSubclass("bersker");
        guerrero.addSubclass("maestro-de-armas");
        classes.put("guerrero", guerrero);

        // Monje
        RPGClass monje = new RPGClass("monje", "Monje", "Un luchador experimentado con mucha velocidad");
        monje.setAdvantages("Movimiento alto, fuerte daño a melé, puede stunear enemigos (5% Probabilidad)");
        monje.setDisadvantages("Defensa baja, débil en habilidades de rango tanto magias, trampas y arcos");
        monje.addSubclass("shaolin");
        monje.addSubclass("maestro-zen");
        classes.put("monje", monje);

        // Mago
        RPGClass mago = new RPGClass("mago", "Mago", "Poderoso hechicero con alto daño mágico");
        mago.setAdvantages("Alto daño mágico, variedad de hechizos de diferentes elementos, ataca a distancia");
        mago.setDisadvantages("Baja defensa, débil en combate cuerpo a cuerpo");
        mago.addSubclass("elementalista");
        mago.addSubclass("mago-de-combate");
        classes.put("mago", mago);

        // Invocador
        RPGClass invocador = new RPGClass("invocador", "Invocador", "Hechicero que invoca criaturas para que luchen por el");
        invocador.setAdvantages("Invoca Criaturas, las criaturas pelean por ti, puede invocar varias criaturas a la vez");
        invocador.setDisadvantages("Débil en habilidades melé, defensa baja, débil en habilidades de distancia");
        invocador.addSubclass("brujo");
        invocador.addSubclass("chaman");
        classes.put("invocador", invocador);

        // Arquero
        RPGClass arquero = new RPGClass("arquero", "Arquero", "Un preciso luchador a distancia con alta movilidad");
        arquero.setAdvantages("Alta velocidad de movimiento, daño fuerte a distancia, puede golpear a los enemigos a distancia con su arco");
        arquero.setDisadvantages("Baja defensa y débil en habilidad a melé");
        arquero.addSubclass("francotirador");
        arquero.addSubclass("guardabosques");
        classes.put("arquero", arquero);

        // Pícaro
        RPGClass picaro = new RPGClass("picaro", "Pícaro", "Luchador hábil con alta posibilidad de hacer golpes críticos y gran movilidad");
        picaro.setAdvantages("Alta posibilidad de critico, alta movilidad, hace mas daño si golpea por la espalda");
        picaro.setDisadvantages("Baja defensa y nula habilidad a distancia ( no puede usar arcos, armas a distancia)");
        picaro.addSubclass("asesino");
        picaro.addSubclass("asaltante");
        classes.put("picaro", picaro);

        // Paladín
        RPGClass paladin = new RPGClass("paladin", "Paladín", "Luchador balanceado en combate que puede curar tanto a si mismo como a aliados");
        paladin.setAdvantages("Alta defensa y vida, curaciones y habilidades variadas");
        paladin.setDisadvantages("Débil tanto en habilidades melé como distancia");
        paladin.addSubclass("paladin-sagrado");
        paladin.addSubclass("paladin-del-caos");
        classes.put("paladin", paladin);

        // Nigromante
        RPGClass nigromante = new RPGClass("nigromante", "Nigromante", "Hechicero Oscuro con invocación de muertos y usa habilidades de putrefacción");
        nigromante.setAdvantages("Invoca no muertos, Alto daño mágico, debilita a los enemigos con su putrefacción");
        nigromante.setDisadvantages("Baja defensa, Débil en habilidades a melé");
        nigromante.addSubclass("lich");
        nigromante.addSubclass("caballero-de-la-muerte");
        classes.put("nigromante", nigromante);

        // Druida
        RPGClass druida = new RPGClass("druida", "Druida", "Usa magia de Naturaleza y transformaciones");
        druida.setAdvantages("Se transforma en animales, se puede curar a si mismo, magia de naturaleza");
        druida.setDisadvantages("Baja defensa en formas de animales y Débil en habilidades a melé fuera de forma de animales");
        druida.addSubclass("licantropo");
        druida.addSubclass("archidruida");
        classes.put("druida", druida);

        // Evocador
        RPGClass evocador = new RPGClass("evocador", "Evocador", "Usa magia su magia apoyar a los aliados y puede hacer algo de daño");
        evocador.setAdvantages("Magias de soporte y decente daño a distancia");
        evocador.setDisadvantages("Baja vida y débil daño a melé");
        evocador.addSubclass("salvaguarda");
        evocador.addSubclass("destructor");
        classes.put("evocador", evocador);

        // Cazador
        RPGClass cazador = new RPGClass("cazador", "Cazador", "Pelea junto a su mascota compañera");
        cazador.setAdvantages("Disponibilidad de mascota y buena velocidad de movimiento");
        cazador.setDisadvantages("Baja defensa y vida");
        cazador.addSubclass("maestro-de-la-manada");
        cazador.addSubclass("combatiente-primigenio");
        classes.put("cazador", cazador);

        // Sacerdote
        RPGClass sacerdote = new RPGClass("sacerdote", "Sacerdote", "Enfocado a curar a tus compañeros a cambio de tener poco daño");
        sacerdote.setAdvantages("Alta curación propia a y a los aliados y habilidades de escudo");
        sacerdote.setDisadvantages("No tiene daño a melé y tiene a distancia débil");
        sacerdote.addSubclass("primarca");
        sacerdote.addSubclass("sacerdote-corrupto");
        classes.put("sacerdote", sacerdote);

        // Trampero
        RPGClass trampero = new RPGClass("trampero", "Trampero", "Pelea basándose en sus trampas");
        trampero.setAdvantages("Alto daño en trampas y torretas");
        trampero.setDisadvantages("No puede usar combate cuerpo a cuerpo ni a distancia");
        trampero.addSubclass("maestro-de-las-trampas");
        trampero.addSubclass("ingeniero");
        classes.put("trampero", trampero);

        plugin.getLogger().info("Se cargaron " + classes.size() + " clases correctamente");
    }

    public RPGClass getClass(String id) {
        return classes.get(id.toLowerCase());
    }

    public Map<String, RPGClass> getAllClasses() {
        return new HashMap<>(classes);
    }

    public boolean isValidClass(String id) {
        return classes.containsKey(id.toLowerCase());
    }
}
