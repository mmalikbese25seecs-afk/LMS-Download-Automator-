package com.downloadc.downloadc.model;

// Abstract base class for files fetched from Moodle

public abstract class LmsResource {

    private final int id;
    private final String name;

    // Constructor
    public LmsResource(int id, String name) {
        this.id = id;

        this.name= (name == null || name.isBlank()) ? "Unnamed" : name;
    }

    public int getId(){
 return id;
 }
    public String getName() {
 return name;
 }


    // Subclasses override this method

    public abstract String getSummaryLine();
   

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getSummaryLine() + "]";
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;

        if (!(o instanceof LmsResource other)) return false;
        return id == other.id && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return 31 * id + name.hashCode();
    }
}
