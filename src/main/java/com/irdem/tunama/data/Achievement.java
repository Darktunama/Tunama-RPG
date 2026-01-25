package com.irdem.tunama.data;

public class Achievement {
    private String id;
    private String name;
    private String description;
    private String category;
    private int requirement;

    public Achievement(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = "General";
        this.requirement = 0;
    }

    // Getters y Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public int getRequirement() { return requirement; }

    public void setCategory(String category) { this.category = category; }
    public void setRequirement(int requirement) { this.requirement = requirement; }
}
