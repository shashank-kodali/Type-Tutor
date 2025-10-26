package com.typetutor;

/**
 * Theme class holds all color values for a theme
 */
public class Theme {
    private final String name;
    private final String bgColor;
    private final String subAltColor;
    private final String subColor;
    private final String mainColor;
    private final String caretColor;
    private final String textColor;
    private final String errorColor;
    private final String errorExtraColor;

    public Theme(String name, String bgColor, String subAltColor, String subColor,
                 String mainColor, String caretColor, String textColor,
                 String errorColor, String errorExtraColor) {
        this.name = name;
        this.bgColor = bgColor;
        this.subAltColor = subAltColor;
        this.subColor = subColor;
        this.mainColor = mainColor;
        this.caretColor = caretColor;
        this.textColor = textColor;
        this.errorColor = errorColor;
        this.errorExtraColor = errorExtraColor;
    }

    // Getters
    public String getName() { return name; }
    public String getBgColor() { return bgColor; }
    public String getSubAltColor() { return subAltColor; }
    public String getSubColor() { return subColor; }
    public String getMainColor() { return mainColor; }
    public String getCaretColor() { return caretColor; }
    public String getTextColor() { return textColor; }
    public String getErrorColor() { return errorColor; }
    public String getErrorExtraColor() { return errorExtraColor; }

    @Override
    public String toString() {
        return name;
    }
}