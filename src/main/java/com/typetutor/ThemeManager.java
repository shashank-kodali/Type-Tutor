package com.typetutor;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * ThemeManager handles theme selection and persistence
 */
public class ThemeManager {
    private static final String THEME_PREF_KEY = "selected_theme";
    private final Preferences prefs;
    private final List<Theme> themes;
    private Theme currentTheme;

    public ThemeManager() {
        this.prefs = Preferences.userNodeForPackage(ThemeManager.class);
        this.themes = createThemes();
        this.currentTheme = loadSavedTheme();
    }

    /**
     * Create all available themes
     */
    private List<Theme> createThemes() {
        List<Theme> themeList = new ArrayList<>();

        // 1. Default (Current monkeytype-style)
        themeList.add(new Theme(
                "default",
                "#323437", "#2c2e31", "#646669",
                "#d1d0c5", "#e2b714", "#646669",
                "#ca4754", "#7e2a33"
        ));

        // 2. Light
        themeList.add(new Theme(
                "light",
                "#f5f5f5", "#e8e8e8", "#9e9e9e",
                "#424242", "#ff6b00", "#757575",
                "#e53935", "#c62828"
        ));

        // 3. Dark
        themeList.add(new Theme(
                "dark",
                "#000000", "#1a1a1a", "#4a4a4a",
                "#ffffff", "#00ff41", "#666666",
                "#ff0040", "#cc0033"
        ));

        // 4. Cyberpunk
        themeList.add(new Theme(
                "cyberpunk",
                "#0a0e27", "#1a1f3a", "#4a5578",
                "#e0e0ff", "#00ffff", "#8888ff",
                "#ff00ff", "#cc00cc"
        ));

        // 5. Forest
        themeList.add(new Theme(
                "forest",
                "#1a2421", "#0f1916", "#4a5c54",
                "#c7d7c7", "#88cc44", "#6b8e6b",
                "#ff5722", "#d84315"
        ));

        // 6. Ocean
        themeList.add(new Theme(
                "ocean",
                "#0d1b2a", "#1b263b", "#415a77",
                "#e0e1dd", "#00d4ff", "#778da9",
                "#ff6090", "#ef476f"
        ));

        // 7. Sunset
        themeList.add(new Theme(
                "sunset",
                "#2d1b2e", "#3d2b3e", "#6d4b6e",
                "#f4e8d8", "#ff7b4a", "#c89f9c",
                "#ff006e", "#d90368"
        ));

        // 8. Monokai
        themeList.add(new Theme(
                "monokai",
                "#272822", "#1e1f1c", "#75715e",
                "#f8f8f2", "#fd971f", "#75715e",
                "#f92672", "#cc1e5f"
        ));

        // 9. Dracula
        themeList.add(new Theme(
                "dracula",
                "#282a36", "#21222c", "#6272a4",
                "#f8f8f2", "#bd93f9", "#6272a4",
                "#ff5555", "#cc4444"
        ));

        // 10. Nord
        themeList.add(new Theme(
                "nord",
                "#2e3440", "#3b4252", "#4c566a",
                "#eceff4", "#88c0d0", "#d8dee9",
                "#bf616a", "#9f4d54"
        ));

        return themeList;
    }

    /**
     * Load saved theme from preferences
     */
    private Theme loadSavedTheme() {
        String savedThemeName = prefs.get(THEME_PREF_KEY, "default");
        return themes.stream()
                .filter(t -> t.getName().equals(savedThemeName))
                .findFirst()
                .orElse(themes.get(0)); // Default to first theme if not found
    }

    /**
     * Save current theme to preferences
     */
    private void saveTheme() {
        prefs.put(THEME_PREF_KEY, currentTheme.getName());
    }

    /**
     * Set current theme
     */
    public void setTheme(Theme theme) {
        this.currentTheme = theme;
        saveTheme();
    }

    /**
     * Set theme by name
     */
    public void setTheme(String themeName) {
        themes.stream()
                .filter(t -> t.getName().equals(themeName))
                .findFirst()
                .ifPresent(this::setTheme);
    }

    /**
     * Get current theme
     */
    public Theme getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Get all available themes
     */
    public List<Theme> getAllThemes() {
        return new ArrayList<>(themes);
    }

    /**
     * Get theme by name
     */
    public Theme getTheme(String name) {
        return themes.stream()
                .filter(t -> t.getName().equals(name))
                .findFirst()
                .orElse(themes.get(0));
    }
}