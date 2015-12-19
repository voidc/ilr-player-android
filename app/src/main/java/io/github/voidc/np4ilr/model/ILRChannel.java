package io.github.voidc.np4ilr.model;

public class ILRChannel {
    private int id;
    private String name;
    private String description;
    private String streamURI;

    public ILRChannel(int id, String name, String description, String streamURI) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.streamURI = streamURI;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return "I \u2764 " + name;
    }

    public String getDescription() {
        return description;
    }

    public String getStreamURI() {
        return streamURI;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ILRChannel that = (ILRChannel) o;

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return getFullName();
    }

    public static String convertToFullName(String name) {
        return "I \u2764 " + name;
    }
}
