package com.example.lightweb20;

public class Tab {
    private String name;
    private String url;

    public Tab(String name, String url) {
        this.name = name;
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return name;
    }
}
