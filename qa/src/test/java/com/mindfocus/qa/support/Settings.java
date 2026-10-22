package com.mindfocus.qa.support;

/** Where the system under test runs. Override with environment variables. */
public final class Settings {
    private Settings() {}

    public static final String API_URL = env("API_URL", "/api");
    public static final String BASE_URL = env("BASE_URL", "http://localhost:4200");
    public static final boolean HEADLESS = !"0".equals(env("HEADLESS", "1"));
    public static final String PASSWORD = "password1";

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
