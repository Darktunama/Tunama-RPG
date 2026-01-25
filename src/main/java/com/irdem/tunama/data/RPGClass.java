package com.irdem.tunama.data;

import java.util.HashSet;
import java.util.Set;

public class RPGClass {
    private String id;
    private String name;
    private String description;
    private String advantages;
    private String disadvantages;
    private Set<String> allowedRaces;
    private Set<String> subclasses;

    public RPGClass(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.allowedRaces = new HashSet<>();
        this.subclasses = new HashSet<>();
    }

    // Getters y Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getAdvantages() { return advantages; }
    public String getDisadvantages() { return disadvantages; }
    
    public void setAdvantages(String advantages) { this.advantages = advantages; }
    public void setDisadvantages(String disadvantages) { this.disadvantages = disadvantages; }
    
    public void addAllowedRace(String race) {
        allowedRaces.add(race.toLowerCase());
    }
    
    public void addSubclass(String subclass) {
        subclasses.add(subclass.toLowerCase());
    }
    
    public Set<String> getAllowedRaces() {
        return new HashSet<>(allowedRaces);
    }
    
    public Set<String> getSubclasses() {
        return new HashSet<>(subclasses);
    }
    
    public boolean isRaceAllowed(String race) {
        return allowedRaces.contains(race.toLowerCase());
    }
    
    public boolean hasSubclass(String subclass) {
        return subclasses.contains(subclass.toLowerCase());
    }
}
