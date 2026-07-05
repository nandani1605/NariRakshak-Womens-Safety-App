package com.example.narirakshak;

public class Contact {
    private String id; // Firebase Document ID
    private String name;
    private String number;

    public Contact(String id, String name, String number) {
        this.id = id;
        this.name = name;
        this.number = number;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getNumber() { return number; }
}