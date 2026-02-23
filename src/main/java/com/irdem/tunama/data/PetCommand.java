package com.irdem.tunama.data;

/**
 * Comandos disponibles para las mascotas de combate
 */
public enum PetCommand {
    FOLLOW("Seguir", "La mascota sigue al dueño"),
    STAY("Quedarse", "La mascota se queda en su posición actual"),
    ATTACK("Atacar", "La mascota ataca al objetivo marcado"),
    DEFEND("Defender", "La mascota defiende al dueño cuando es atacado"),
    AGGRESSIVE("Agresivo", "La mascota ataca automáticamente enemigos cercanos");

    private final String displayName;
    private final String description;

    PetCommand(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
