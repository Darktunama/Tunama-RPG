package com.irdem.tunama.data;

public class Subclass {
    private String id;
    private String name;
    private String description;
    private String advantages;
    private String disadvantages;
    private String parentClass;

    public Subclass(String id, String name, String description, String parentClass) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.parentClass = parentClass;
    }

    // Getters y Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getAdvantages() { return advantages; }
    public String getDisadvantages() { return disadvantages; }
    public String getParentClass() { return parentClass; }
    
    public void setAdvantages(String advantages) { this.advantages = advantages; }
    public void setDisadvantages(String disadvantages) { this.disadvantages = disadvantages; }
}
