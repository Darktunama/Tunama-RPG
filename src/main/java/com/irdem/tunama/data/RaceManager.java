package com.irdem.tunama.data;

import org.bukkit.plugin.java.JavaPlugin;
import java.util.HashMap;
import java.util.Map;

public class RaceManager {

    private Map<String, Race> races;
    private JavaPlugin plugin;

    public RaceManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.races = new HashMap<>();
        loadRaces();
    }

    private void loadRaces() {
        // Humano
        Race humano = new Race("humano", "Humano", "Raza Versátil pero sin destacar en nada");
        humano.setAdvantages("Puede usar todas las clases excepto Evocador y Druida");
        humano.setDisadvantages("No tiene bonus de Estadísticas");
        races.put("humano", humano);

        // Elfo
        Race elfo = new Race("elfo", "Elfo", "Raza débil en vitalidad pero de alto daño");
        elfo.setAdvantages("Cada punto invertido en Agilidad o Inteligencia suma 2 en vez de 1");
        elfo.setDisadvantages("Para subir 1 punto de Poder Corrupto requiere 2 puntos, No puede ser las clases Trampero, Sacerdote ni Evocador");
        elfo.setAgilityMultiplier(2.0);
        elfo.setIntelligenceMultiplier(2.0);
        elfo.setCorruptPowerCost(2.0);
        races.put("elfo", elfo);

        // SemiElfo
        Race semiElfo = new Race("semielfo", "SemiElfo", "Es lo que pasa cuando un Elfo y un Humano se juntan");
        semiElfo.setAdvantages("Puede usar todas las clases excepto Trampero, Evocador, Monje y Nigromante");
        semiElfo.setDisadvantages("Cada punto invertido en Agilidad o Inteligencia suma 2 en vez de 1, Para subir 1 punto de Poder Corrupto requiere 2 puntos");
        semiElfo.setAgilityMultiplier(2.0);
        semiElfo.setIntelligenceMultiplier(2.0);
        semiElfo.setCorruptPowerCost(2.0);
        races.put("semielfo", semiElfo);

        // Orco
        Race orco = new Race("orco", "Orco", "Nadie donde sabe de donde vinieron estas bestias salvajes");
        orco.setAdvantages("Cada punto invertido en Vida o Fuerza suma 2 en vez de 1");
        orco.setDisadvantages("Para subir 1 punto de Agilidad Poder sagrado o Poder Naturaleza requiere 2 puntos, No puede usar las clases Pícaro y Evocador");
        orco.setLifeMultiplier(2.0);
        orco.setStrengthMultiplier(2.0);
        orco.setAgilityMultiplier(0.5);
        orco.setSagradoPowerCost(2.0);
        orco.setNaturePowerCost(2.0);
        races.put("orco", orco);

        // Tiflyn
        Race tiflyn = new Race("tiflyn", "Tiflyn", "Nacidos de humanos maldecidos por el Infierno");
        tiflyn.setAdvantages("Cada punto invertido en Vida, Fuerza, Agilidad o Poder Corrupto suma 2 en vez de 1");
        tiflyn.setDisadvantages("Cada punto Invertido en Poder Sagrado o Poder Naturaleza requiere 2 puntos, no puede usar las clases Monje, Sacerdote ni Druida");
        tiflyn.setLifeMultiplier(2.0);
        tiflyn.setStrengthMultiplier(2.0);
        tiflyn.setAgilityMultiplier(2.0);
        tiflyn.setCorruptPowerCost(0.5);
        tiflyn.setSagradoPowerCost(2.0);
        tiflyn.setNaturePowerCost(2.0);
        races.put("tiflyn", tiflyn);

        // Enano
        Race enano = new Race("enano", "Enano", "Raza muy Orgullosa, sobre todo por que le llega hasta las rodillas su barba");
        enano.setAdvantages("Cada punto Invertido en Vida y Fuerza suma 2 en vez de 1");
        enano.setDisadvantages("Cada punto invertido en Inteligencia o Agilidad requiere 2 puntos, no puede usar clases Cazador, Pícaro, Druida, Evocador");
        enano.setLifeMultiplier(2.0);
        enano.setStrengthMultiplier(2.0);
        enano.setIntelligenceMultiplier(0.5);
        enano.setAgilityMultiplier(0.5);
        races.put("enano", enano);

        // Dragoneante
        Race dragoneante = new Race("dragoneante", "Dragoneante", "Son los nacidos del Dragon");
        dragoneante.setAdvantages("Cada punto Invertido en Inteligencia, Agilidad o Poder Sagrado da 2 puntos");
        dragoneante.setDisadvantages("Cada punto invertido en Vida Poder Corrupto requiere 2 puntos, no puede usar clases Nigromante, Invocador, Paladín, Guerrero");
        dragoneante.setIntelligenceMultiplier(2.0);
        dragoneante.setAgilityMultiplier(2.0);
        dragoneante.setSagradoPowerCost(0.5);
        dragoneante.setLifeMultiplier(0.5);
        dragoneante.setCorruptPowerCost(2.0);
        races.put("dragoneante", dragoneante);

        // Goblin
        Race goblin = new Race("goblin", "Goblin", "Son grandes acaparadores de fortunas pero difíciles de tratar");
        goblin.setAdvantages("Cada punto Invertido en Agilidad, Inteligencia o Poder Corrupto dan 2 puntos");
        goblin.setDisadvantages("Cada punto invertido en vida o poder sagrado requiere 2 puntos, no puede usar clases Evocador, Paladín, Guerrero y Druida");
        goblin.setAgilityMultiplier(2.0);
        goblin.setIntelligenceMultiplier(2.0);
        goblin.setCorruptPowerCost(0.5);
        goblin.setLifeMultiplier(0.5);
        goblin.setSagradoPowerCost(2.0);
        races.put("goblin", goblin);

        // No Muerto
        Race noMuerto = new Race("nomuerto", "No Muerto", "La nunca los freno por eso siempre vuelven");
        noMuerto.setAdvantages("Cada punto Invertido en Vida o Poder Corrupto da 2 puntos");
        noMuerto.setDisadvantages("Cada punto invertido en Agilidad o Poder Naturaleza requiere 2 puntos, no puede usar clases Evocador, Sacerdote, Monje");
        noMuerto.setLifeMultiplier(2.0);
        noMuerto.setCorruptPowerCost(0.5);
        noMuerto.setAgilityMultiplier(0.5);
        noMuerto.setNaturePowerCost(2.0);
        races.put("nomuerto", noMuerto);

        plugin.getLogger().info("Se cargaron " + races.size() + " razas correctamente");
    }

    public Race getRace(String id) {
        return races.get(id.toLowerCase());
    }

    public Map<String, Race> getAllRaces() {
        return new HashMap<>(races);
    }

    public boolean isValidRace(String id) {
        return races.containsKey(id.toLowerCase());
    }
}
