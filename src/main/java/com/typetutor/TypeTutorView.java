package com.typetutor;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class TypeTutorView {

    private ThemeManager themeManager;
    private Theme currentTheme;

    private TextFlow sentenceDisplay;
    private TextField inputField;
    private Label timerLabel;
    private Label titleLabel;
    private Button startButton;
    private ToggleGroup difficultyGroup;
    private ToggleGroup timerGroup;
    private ToggleGroup themeGroup;
    private VBox mainTypingArea;
    private VBox resultsPanel;
    private BorderPane root;
    private Scene scene;

    // Callbacks for feature buttons
    private Runnable onStatsButtonClick;
    private Runnable onLeaderboardButtonClick;
    private Runnable onShortcutsButtonClick;

    public TextField getInputField() { return inputField; }
    public Button getStartButton() { return startButton; }
    public ToggleGroup getDifficultyGroup() { return difficultyGroup; }
    public ToggleGroup getTimerGroup() { return timerGroup; }
    public ThemeManager getThemeManager() { return themeManager; }
    public Scene getScene() { return scene; }

    /**
     * Set callback for statistics button
     */
    public void setOnStatsButtonClick(Runnable callback) {
        this.onStatsButtonClick = callback;
    }

    /**
     * Set callback for leaderboard button
     */
    public void setOnLeaderboardButtonClick(Runnable callback) {
        this.onLeaderboardButtonClick = callback;
    }

    /**
     * Set callback for shortcuts button
     */
    public void setOnShortcutsButtonClick(Runnable callback) {
        this.onShortcutsButtonClick = callback;
    }

    public TypeTutorView(Stage primaryStage, ThemeManager themeManager) {
        this.themeManager = themeManager;
        this.currentTheme = themeManager.getCurrentTheme();

        root = new BorderPane();
        root.setPadding(new Insets(50, 30, 20, 30));
        applyThemeToRoot();

        VBox topSection = createTopSection();
        StackPane centerStackPane = createCenterStackPane();

        root.setTop(topSection);
        root.setCenter(centerStackPane);

        scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("Type Tutor");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Apply theme colors to root element
     */
    private void applyThemeToRoot() {
        root.setStyle("-fx-background-color: " + currentTheme.getBgColor() + ";");
    }

    /**
     * Switch to a new theme and refresh all UI elements
     */
    public void applyTheme(Theme theme) {
        this.currentTheme = theme;
        this.themeManager.setTheme(theme);

        // Refresh all UI with new theme
        applyThemeToRoot();
        refreshAllComponents();
    }

    /**
     * Refresh all UI components with current theme
     */
    private void refreshAllComponents() {
        // Refresh title
        if (titleLabel != null) {
            titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + currentTheme.getCaretColor() + ";");
        }

        // Refresh timer label
        if (timerLabel != null) {
            timerLabel.setStyle("-fx-text-fill: " + currentTheme.getCaretColor() + "; -fx-font-weight: 300;");
        }

        // Refresh input field
        if (inputField != null) {
            boolean isGameActive = startButton != null && !startButton.getText().equals("start");
            if (inputField.isFocused() && isGameActive) {
                inputField.setStyle("-fx-background-color: transparent; -fx-text-fill: " + currentTheme.getMainColor() +
                        "; -fx-prompt-text-fill: " + currentTheme.getSubColor() + "; -fx-background-radius: 8; -fx-border-color: " +
                        currentTheme.getCaretColor() + "; -fx-border-radius: 8; -fx-border-width: 2; -fx-padding: 12;");
            } else {
                inputField.setStyle("-fx-background-color: transparent; -fx-text-fill: " + currentTheme.getMainColor() +
                        "; -fx-prompt-text-fill: " + currentTheme.getSubColor() + "; -fx-background-radius: 8; -fx-border-color: " +
                        currentTheme.getSubColor() + "; -fx-border-radius: 8; -fx-border-width: 2; -fx-padding: 12;");
            }
        }

        // Refresh start button
        if (startButton != null) {
            if (startButton.getText().equals("start")) {
                startButton.setStyle("-fx-background-color: " + currentTheme.getSubColor() + "; -fx-text-fill: " +
                        currentTheme.getBgColor() + "; -fx-background-radius: 8; -fx-font-weight: 500; -fx-border-color: transparent; -fx-cursor: hand;");
            } else {
                startButton.setStyle("-fx-background-color: " + currentTheme.getErrorColor() + "; -fx-text-fill: " +
                        currentTheme.getBgColor() + "; -fx-background-radius: 8; -fx-font-weight: 500; -fx-border-color: transparent; -fx-cursor: hand;");
            }
        }

        // Refresh all toggle buttons (difficulty, time)
        if (difficultyGroup != null) {
            difficultyGroup.getToggles().forEach(toggle -> {
                ToggleButton btn = (ToggleButton) toggle;
                updateToggleButtonStyle(btn);
            });
        }

        if (timerGroup != null) {
            timerGroup.getToggles().forEach(toggle -> {
                ToggleButton btn = (ToggleButton) toggle;
                updateToggleButtonStyle(btn);
            });
        }
    }

    /**
     * Update toggle button style with current theme
     */
    private void updateToggleButtonStyle(ToggleButton btn) {
        String baseStyle = "-fx-border-color: transparent; -fx-background-radius: 6; -fx-padding: 5 14; -fx-cursor: hand; -fx-font-size: 18px;";
        if (btn.isSelected()) {
            btn.setStyle("-fx-background-color: " + currentTheme.getCaretColor() + "; -fx-text-fill: " +
                    currentTheme.getBgColor() + "; -fx-font-weight: 600; " + baseStyle);
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + currentTheme.getSubColor() + "; " + baseStyle);
        }
    }

    public void displaySentence(String sentence, String typedText) {
        Platform.runLater(() -> {
            sentenceDisplay.getChildren().clear();
            int matchLength = Math.min(sentence.length(), typedText.length());

            for (int i = 0; i < sentence.length(); i++) {
                Text charText = new Text(String.valueOf(sentence.charAt(i)));
                charText.setFont(new Font("Roboto Mono", 30));
                if (i < matchLength) {
                    if (sentence.charAt(i) == typedText.charAt(i)) {
                        charText.setFill(Color.web(currentTheme.getMainColor()));
                    } else {
                        charText.setFill(Color.web(currentTheme.getErrorColor()));
                        charText.setStyle("-fx-font-weight: bold;");
                    }
                } else if (i == matchLength && typedText.length() > 0) {
                    charText.setFill(Color.web(currentTheme.getCaretColor()));
                    charText.setStyle("-fx-underline: true; -fx-font-weight: bold;");
                } else {
                    charText.setFill(Color.web(currentTheme.getTextColor()));
                }
                sentenceDisplay.getChildren().add(charText);
            }

            if (typedText.length() > sentence.length()) {
                for (int i = sentence.length(); i < typedText.length(); i++) {
                    Text extraChar = new Text(String.valueOf(typedText.charAt(i)));
                    extraChar.setFont(new Font("Roboto Mono", 30));
                    extraChar.setFill(Color.web(currentTheme.getErrorExtraColor()));
                    extraChar.setStyle("-fx-font-weight: bold;");
                    sentenceDisplay.getChildren().add(extraChar);
                }
            }
        });
    }

    public void updateTimerLabel(String value) {
        Platform.runLater(() -> timerLabel.setText(value));
    }

    public void prepareForNewTest(int selectedTime) {
        Platform.runLater(() -> {
            resultsPanel.setVisible(false);
            mainTypingArea.setVisible(true);
            timerLabel.setText(String.valueOf(selectedTime));
            inputField.clear();
            inputField.setDisable(true);
            inputField.setPromptText("Click 'start' to begin...");
            startButton.setText("start");
            startButton.setStyle("-fx-background-color: " + currentTheme.getSubColor() + "; -fx-text-fill: " +
                    currentTheme.getBgColor() + "; -fx-background-radius: 8; -fx-font-weight: 500; -fx-border-color: transparent; -fx-cursor: hand;");
            difficultyGroup.getToggles().forEach(toggle -> ((ToggleButton) toggle).setDisable(false));
            timerGroup.getToggles().forEach(toggle -> ((ToggleButton) toggle).setDisable(false));
        });
    }

    public void prepareForActiveTest() {
        Platform.runLater(() -> {
            inputField.clear();
            inputField.setDisable(false);
            inputField.setPromptText("start typing...");
            inputField.requestFocus();
            startButton.setText("stop");
            startButton.setStyle("-fx-background-color: " + currentTheme.getErrorColor() + "; -fx-text-fill: " +
                    currentTheme.getBgColor() + "; -fx-background-radius: 8; -fx-font-weight: 500; -fx-border-color: transparent; -fx-cursor: hand;");
            difficultyGroup.getToggles().forEach(toggle -> ((ToggleButton) toggle).setDisable(true));
            timerGroup.getToggles().forEach(toggle -> ((ToggleButton) toggle).setDisable(true));
        });
    }

    public void showResults(TypeTutorModel model, EventHandler<ActionEvent> nextTestHandler) {
        Platform.runLater(() -> {
            resultsPanel.getChildren().clear();

            double elapsedSeconds = Math.max(1.0, model.getTestDurationSeconds());
            double timeInMinutes = elapsedSeconds / 60.0;
            double wpm = (model.getCumulativeCorrectChars() / 5.0) / timeInMinutes;
            double rawWpm = (model.getCumulativeTotalChars() / 5.0) / timeInMinutes;
            double accuracy = model.getCumulativeTargetChars() > 0 ?
                    (model.getCumulativeCorrectChars() * 100.0 / model.getCumulativeTargetChars()) : 0;
            double consistency = model.calculateConsistency();

            HBox mainResultsBox = new HBox(30);
            mainResultsBox.setAlignment(Pos.CENTER);
            mainResultsBox.setPadding(new Insets(5, 0, 5, 0));

            VBox leftStats = new VBox(20);
            leftStats.setAlignment(Pos.CENTER_LEFT);
            leftStats.setPadding(new Insets(10));

            VBox wpmBox = new VBox(5);
            wpmBox.setAlignment(Pos.CENTER_LEFT);
            Label wpmLabel = new Label("wpm");
            wpmLabel.setFont(new Font("Lexend Deca", 20));
            wpmLabel.setStyle("-fx-text-fill: " + currentTheme.getSubColor() + ";");
            Label wpmValue = new Label(String.format("%.0f", wpm));
            wpmValue.setFont(new Font("Lexend Deca", 60));
            wpmValue.setStyle("-fx-text-fill: " + currentTheme.getCaretColor() + "; -fx-font-weight: bold;");
            wpmBox.getChildren().addAll(wpmLabel, wpmValue);

            VBox accBox = new VBox(5);
            accBox.setAlignment(Pos.CENTER_LEFT);
            Label accLabel = new Label("acc");
            accLabel.setFont(new Font("Lexend Deca", 20));
            accLabel.setStyle("-fx-text-fill: " + currentTheme.getSubColor() + ";");
            Label accValue = new Label(String.format("%.0f%%", accuracy));
            accValue.setFont(new Font("Lexend Deca", 60));
            accValue.setStyle("-fx-text-fill: " + currentTheme.getCaretColor() + "; -fx-font-weight: bold;");
            accBox.getChildren().addAll(accLabel, accValue);

            leftStats.getChildren().addAll(wpmBox, accBox);

            LineChart<Number, Number> chart = createCombinedChart(model);
            chart.setPrefWidth(500);
            chart.setPrefHeight(280);
            chart.setMaxHeight(280);

            mainResultsBox.getChildren().addAll(leftStats, chart);

            GridPane metricsGrid = new GridPane();
            metricsGrid.setAlignment(Pos.CENTER);
            metricsGrid.setHgap(50);
            metricsGrid.setVgap(0);
            metricsGrid.setPadding(new Insets(10, 0, 10, 0));

            String diffName = model.getCurrentDifficulty().name().toLowerCase();
            addMetricToGrid(metricsGrid, 0, "test type", String.format("time %d (%d actual)\n%s",
                    model.getSelectedTime(), Math.round(elapsedSeconds), diffName));
            addMetricToGrid(metricsGrid, 1, "raw", String.format("%.0f", rawWpm));
            addMetricToGrid(metricsGrid, 2, "characters", String.format("%d/%d/%d/%d",
                    model.getCumulativeCorrectChars(),
                    model.getCumulativeMissedChars(),
                    model.getCumulativeExtraChars(),
                    model.getCumulativeTargetChars()));
            addMetricToGrid(metricsGrid, 3, "consistency", String.format("%.0f%%", consistency));
            addMetricToGrid(metricsGrid, 4, "time", String.format("%.1fs", elapsedSeconds));

            Button nextTestButton = new Button("next test");
            nextTestButton.setFont(new Font("Lexend Deca", 24));
            nextTestButton.setPrefWidth(200);
            nextTestButton.setPrefHeight(55);
            nextTestButton.setStyle("-fx-background-color: " + currentTheme.getSubColor() + "; -fx-text-fill: " +
                    currentTheme.getMainColor() + "; -fx-background-radius: 18; -fx-font-weight: 500; -fx-border-color: transparent; -fx-cursor: hand;");
            nextTestButton.setOnMouseEntered(e -> nextTestButton.setStyle("-fx-background-color: " + currentTheme.getMainColor() +
                    "; -fx-text-fill: " + currentTheme.getBgColor() + "; -fx-background-radius: 18; -fx-font-weight: 600; -fx-border-color: transparent; -fx-cursor: hand;"));
            nextTestButton.setOnMouseExited(e -> nextTestButton.setStyle("-fx-background-color: " + currentTheme.getSubColor() +
                    "; -fx-text-fill: " + currentTheme.getMainColor() + "; -fx-background-radius: 18; -fx-font-weight: 500; -fx-border-color: transparent; -fx-cursor: hand;"));
            nextTestButton.setOnAction(nextTestHandler);

            resultsPanel.getChildren().addAll(mainResultsBox, metricsGrid, nextTestButton);

            mainTypingArea.setVisible(false);
            resultsPanel.setVisible(true);
        });
    }

    private VBox createMetricBox(String label, String value) {
        VBox box = new VBox(6);
        box.setAlignment(Pos.CENTER);

        Label labelLabel = new Label(label);
        labelLabel.setFont(new Font("Lexend Deca", 14));
        labelLabel.setStyle("-fx-text-fill: " + currentTheme.getSubColor() + "; -fx-font-weight: 300;");

        Label valueLabel = new Label(value);
        valueLabel.setFont(new Font("Lexend Deca", 22));
        valueLabel.setStyle("-fx-text-fill: " + currentTheme.getCaretColor() + "; -fx-font-weight: 500;");
        valueLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        box.getChildren().addAll(labelLabel, valueLabel);
        return box;
    }

    private StackPane createCenterStackPane() {
        StackPane centerStackPane = new StackPane();
        centerStackPane.setAlignment(Pos.CENTER);
        mainTypingArea = createMainTypingArea();
        resultsPanel = createResultsPanel();
        resultsPanel.setVisible(false);
        centerStackPane.getChildren().addAll(mainTypingArea, resultsPanel);
        return centerStackPane;
    }

    private VBox createMainTypingArea() {
        timerLabel = new Label("30");
        timerLabel.setFont(new Font("Lexend Deca", 28));
        timerLabel.setStyle("-fx-text-fill: " + currentTheme.getCaretColor() + "; -fx-font-weight: 300;");

        sentenceDisplay = new TextFlow();
        sentenceDisplay.setStyle("-fx-background-color: transparent; -fx-padding: 20;");
        sentenceDisplay.setPrefHeight(120);
        sentenceDisplay.setLineSpacing(6);

        inputField = new TextField();
        inputField.setPromptText("Click 'start' to begin...");
        inputField.setFont(new Font("Roboto Mono", 15));
        inputField.setPrefHeight(45);
        inputField.setDisable(true);
        inputField.setStyle("-fx-background-color: transparent; -fx-text-fill: " + currentTheme.getMainColor() +
                "; -fx-prompt-text-fill: " + currentTheme.getSubColor() + "; -fx-background-radius: 8; -fx-border-color: " +
                currentTheme.getSubColor() + "; -fx-border-radius: 8; -fx-border-width: 2; -fx-padding: 12;");

        inputField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            boolean isGameActive = !startButton.getText().equals("start");
            if (newVal && isGameActive) {
                inputField.setStyle("-fx-background-color: transparent; -fx-text-fill: " + currentTheme.getMainColor() +
                        "; -fx-prompt-text-fill: " + currentTheme.getSubColor() + "; -fx-background-radius: 8; -fx-border-color: " +
                        currentTheme.getCaretColor() + "; -fx-border-radius: 8; -fx-border-width: 2; -fx-padding: 12;");
            } else {
                inputField.setStyle("-fx-background-color: transparent; -fx-text-fill: " + currentTheme.getMainColor() +
                        "; -fx-prompt-text-fill: " + currentTheme.getSubColor() + "; -fx-background-radius: 8; -fx-border-color: " +
                        currentTheme.getSubColor() + "; -fx-border-radius: 8; -fx-border-width: 2; -fx-padding: 12;");
            }
        });

        startButton = new Button("start");
        startButton.setFont(new Font("Lexend Deca", 18));
        startButton.setPrefWidth(120);
        startButton.setPrefHeight(40);
        startButton.setStyle("-fx-background-color: " + currentTheme.getSubColor() + "; -fx-text-fill: " +
                currentTheme.getBgColor() + "; -fx-background-radius: 8; -fx-font-weight: 500; -fx-border-color: transparent; -fx-cursor: hand;");

        startButton.setOnMouseEntered(e -> {
            if (startButton.getText().equals("start")) {
                startButton.setStyle("-fx-background-color: " + currentTheme.getMainColor() + "; -fx-text-fill: " +
                        currentTheme.getBgColor() + "; -fx-background-radius: 8; -fx-font-weight: 500; -fx-border-color: transparent; -fx-cursor: hand;");
            }
        });

        startButton.setOnMouseExited(e -> {
            if (startButton.getText().equals("start")) {
                startButton.setStyle("-fx-background-color: " + currentTheme.getSubColor() + "; -fx-text-fill: " +
                        currentTheme.getBgColor() + "; -fx-background-radius: 8; -fx-font-weight: 500; -fx-border-color: transparent; -fx-cursor: hand;");
            }
        });

        StackPane timerPane = new StackPane(timerLabel);
        StackPane sentencePane = new StackPane(sentenceDisplay);
        StackPane inputPane = new StackPane(inputField);
        StackPane buttonPane = new StackPane(startButton);

        sentencePane.setPadding(new Insets(0, 0, 30, 0));
        buttonPane.setPadding(new Insets(15, 0, 0, 0));

        VBox typingArea = new VBox(15);
        typingArea.setAlignment(Pos.CENTER);
        typingArea.setPrefWidth(750);
        typingArea.setMaxWidth(750);
        typingArea.getChildren().addAll(timerPane, sentencePane, inputPane, buttonPane);
        return typingArea;
    }

    private VBox createResultsPanel() {
        VBox panel = new VBox(20);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(10));
        return panel;
    }

    private VBox createTopSection() {
        VBox topSection = new VBox(15);
        topSection.setAlignment(Pos.CENTER);
        topSection.setPadding(new Insets(30, 0, 0, 0));

        Label titleLabel = new Label("Type Tutor");
        titleLabel.setFont(new Font("Lexend Deca", 36));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + currentTheme.getCaretColor() + ";");

        // Store reference to title for theme updates
        this.titleLabel = titleLabel;

        // Feature buttons row (above control bar)
        HBox featureButtons = createFeatureButtons();

        HBox controlBar = new HBox(30);
        controlBar.setAlignment(Pos.CENTER);
        controlBar.setPadding(new Insets(5, 0, 5, 0));

        // Difficulty section
        difficultyGroup = new ToggleGroup();
        HBox difficultySection = new HBox(8);
        difficultySection.setAlignment(Pos.CENTER);

        Label difficultyIcon = new Label("@");
        difficultyIcon.setFont(new Font("Consolas", 16));
        difficultyIcon.setStyle("-fx-text-fill: " + currentTheme.getSubColor() + ";");

        Label difficultyLabel = new Label("difficulty");
        difficultyLabel.setFont(new Font("Lexend Deca", 12));
        difficultyLabel.setStyle("-fx-text-fill: " + currentTheme.getSubColor() + ";");

        HBox difficultyButtons = new HBox(5);
        difficultyButtons.setAlignment(Pos.CENTER);

        ToggleButton beginnerBtn = createMonkeyToggleButton("beginner", difficultyGroup);
        ToggleButton intermediateBtn = createMonkeyToggleButton("intermediate", difficultyGroup);
        ToggleButton advancedBtn = createMonkeyToggleButton("advanced", difficultyGroup);
        beginnerBtn.setSelected(true);

        difficultyButtons.getChildren().addAll(beginnerBtn, intermediateBtn, advancedBtn);
        difficultySection.getChildren().addAll(difficultyIcon, difficultyLabel, difficultyButtons);

        // Timer section
        timerGroup = new ToggleGroup();
        HBox timerSection = new HBox(8);
        timerSection.setAlignment(Pos.CENTER);

        Label timerIcon = new Label("⏱");
        timerIcon.setFont(new Font("Consolas", 16));
        timerIcon.setStyle("-fx-text-fill: " + currentTheme.getSubColor() + ";");

        Label timerSectionLabel = new Label("time");
        timerSectionLabel.setFont(new Font("Lexend Deca", 12));
        timerSectionLabel.setStyle("-fx-text-fill: " + currentTheme.getSubColor() + ";");

        HBox timerButtons = new HBox(5);
        timerButtons.setAlignment(Pos.CENTER);

        ToggleButton time15Btn = createMonkeyToggleButton("15", timerGroup);
        ToggleButton time30Btn = createMonkeyToggleButton("30", timerGroup);
        ToggleButton time60Btn = createMonkeyToggleButton("60", timerGroup);
        ToggleButton time120Btn = createMonkeyToggleButton("120", timerGroup);
        ToggleButton customBtn = createMonkeyToggleButton("custom", timerGroup);
        time30Btn.setSelected(true);

        timerButtons.getChildren().addAll(time15Btn, time30Btn, time60Btn, time120Btn, customBtn);
        timerSection.getChildren().addAll(timerIcon, timerSectionLabel, timerButtons);

        // Theme section
        HBox themeSection = new HBox(8);
        themeSection.setAlignment(Pos.CENTER);

        Label themeIcon = new Label("🎨");
        themeIcon.setFont(new Font("Consolas", 16));
        themeIcon.setStyle("-fx-text-fill: " + currentTheme.getSubColor() + ";");

        Label themeSectionLabel = new Label("theme");
        themeSectionLabel.setFont(new Font("Lexend Deca", 12));
        themeSectionLabel.setStyle("-fx-text-fill: " + currentTheme.getSubColor() + ";");

        // Theme MenuButton with proper styling
        MenuButton themeMenuButton = new MenuButton(currentTheme.getName());
        themeMenuButton.setFont(new Font("Lexend Deca", 14));
        themeMenuButton.setTextFill(Color.web(currentTheme.getMainColor()));
        themeMenuButton.setStyle("-fx-background-color: " + currentTheme.getSubAltColor() +
                "; -fx-background-radius: 6; -fx-padding: 5 14; -fx-cursor: hand;");

        // Add all themes to dropdown
        for (Theme theme : themeManager.getAllThemes()) {
            MenuItem themeItem = new MenuItem(theme.getName());

            // Style menu item text
            themeItem.setStyle("-fx-text-fill: " + currentTheme.getMainColor() + ";");

            themeItem.setOnAction(e -> {
                applyTheme(theme);
                themeMenuButton.setText(theme.getName());
                themeMenuButton.setTextFill(Color.web(theme.getMainColor()));
                themeMenuButton.setStyle("-fx-background-color: " + theme.getSubAltColor() +
                        "; -fx-background-radius: 6; -fx-padding: 5 14; -fx-cursor: hand;");

                // Update all menu items with new theme colors
                themeMenuButton.getItems().forEach(item -> {
                    item.setStyle("-fx-text-fill: " + theme.getMainColor() + ";");
                });
            });
            themeMenuButton.getItems().add(themeItem);
        }

        // Style the menu popup background
        themeMenuButton.setOnShowing(e -> {
            Platform.runLater(() -> {
                // Find and style the popup
                if (themeMenuButton.getScene() != null) {
                    themeMenuButton.getScene().getRoot().lookupAll(".context-menu").forEach(node -> {
                        node.setStyle("-fx-background-color: " + currentTheme.getBgColor() + "; -fx-border-color: " +
                                currentTheme.getSubColor() + "; -fx-border-width: 1px;");
                    });
                }
            });
        });

        themeSection.getChildren().addAll(themeIcon, themeSectionLabel, themeMenuButton);

        controlBar.getChildren().addAll(difficultySection, createSeparator(), timerSection, createSeparator(), themeSection);
        topSection.getChildren().addAll(titleLabel, featureButtons, controlBar);
        return topSection;
    }

    /**
     * Create feature buttons row (Statistics, Leaderboard, Help)
     */
    private HBox createFeatureButtons() {
        HBox buttonsRow = new HBox(15);
        buttonsRow.setAlignment(Pos.CENTER);
        buttonsRow.setPadding(new Insets(10, 0, 5, 0));

        // Statistics button
        Button statsButton = new Button("📊 Statistics");
        statsButton.setFont(new Font("Lexend Deca", 14));
        statsButton.setPrefWidth(140);
        statsButton.setPrefHeight(35);
        statsButton.setStyle("-fx-background-color: " + currentTheme.getSubAltColor() + "; -fx-text-fill: " +
                currentTheme.getMainColor() + "; -fx-background-radius: 8; -fx-cursor: hand;");
        statsButton.setOnAction(e -> {
            if (onStatsButtonClick != null) {
                onStatsButtonClick.run();
            }
        });
        statsButton.setOnMouseEntered(e -> statsButton.setStyle("-fx-background-color: " + currentTheme.getCaretColor() +
                "; -fx-text-fill: " + currentTheme.getBgColor() + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold;"));
        statsButton.setOnMouseExited(e -> statsButton.setStyle("-fx-background-color: " + currentTheme.getSubAltColor() +
                "; -fx-text-fill: " + currentTheme.getMainColor() + "; -fx-background-radius: 8; -fx-cursor: hand;"));

        // Leaderboard button
        Button leaderboardButton = new Button("🏆 Leaderboard");
        leaderboardButton.setFont(new Font("Lexend Deca", 14));
        leaderboardButton.setPrefWidth(140);
        leaderboardButton.setPrefHeight(35);
        leaderboardButton.setStyle("-fx-background-color: " + currentTheme.getSubAltColor() + "; -fx-text-fill: " +
                currentTheme.getMainColor() + "; -fx-background-radius: 8; -fx-cursor: hand;");
        leaderboardButton.setOnAction(e -> {
            if (onLeaderboardButtonClick != null) {
                onLeaderboardButtonClick.run();
            }
        });
        leaderboardButton.setOnMouseEntered(e -> leaderboardButton.setStyle("-fx-background-color: " + currentTheme.getCaretColor() +
                "; -fx-text-fill: " + currentTheme.getBgColor() + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold;"));
        leaderboardButton.setOnMouseExited(e -> leaderboardButton.setStyle("-fx-background-color: " + currentTheme.getSubAltColor() +
                "; -fx-text-fill: " + currentTheme.getMainColor() + "; -fx-background-radius: 8; -fx-cursor: hand;"));

        // Keyboard Shortcuts button
        Button shortcutsButton = new Button("⌨️ Shortcuts");
        shortcutsButton.setFont(new Font("Lexend Deca", 14));
        shortcutsButton.setPrefWidth(140);
        shortcutsButton.setPrefHeight(35);
        shortcutsButton.setStyle("-fx-background-color: " + currentTheme.getSubAltColor() + "; -fx-text-fill: " +
                currentTheme.getMainColor() + "; -fx-background-radius: 8; -fx-cursor: hand;");
        shortcutsButton.setOnAction(e -> {
            if (onShortcutsButtonClick != null) {
                onShortcutsButtonClick.run();
            }
        });
        shortcutsButton.setOnMouseEntered(e -> shortcutsButton.setStyle("-fx-background-color: " + currentTheme.getCaretColor() +
                "; -fx-text-fill: " + currentTheme.getBgColor() + "; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold;"));
        shortcutsButton.setOnMouseExited(e -> shortcutsButton.setStyle("-fx-background-color: " + currentTheme.getSubAltColor() +
                "; -fx-text-fill: " + currentTheme.getMainColor() + "; -fx-background-radius: 8; -fx-cursor: hand;"));

        buttonsRow.getChildren().addAll(statsButton, leaderboardButton, shortcutsButton);
        return buttonsRow;
    }

    private ToggleButton createMonkeyToggleButton(String text, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setFont(new Font("Lexend Deca", 18));
        btn.setPrefHeight(28);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + currentTheme.getSubColor() +
                "; -fx-border-color: transparent; -fx-background-radius: 6; -fx-padding: 5 14; -fx-cursor: hand;");

        btn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            String baseStyle = "-fx-border-color: transparent; -fx-background-radius: 6; -fx-padding: 5 14; -fx-cursor: hand; -fx-font-size: 18px;";
            if (newVal) {
                btn.setStyle("-fx-background-color: " + currentTheme.getCaretColor() + "; -fx-text-fill: " +
                        currentTheme.getBgColor() + "; -fx-font-weight: 600; " + baseStyle);
            } else {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + currentTheme.getSubColor() + "; " + baseStyle);
            }
        });

        btn.setOnMouseEntered(e -> {
            if (!btn.isSelected()) {
                String baseStyle = "-fx-border-color: transparent; -fx-background-radius: 6; -fx-padding: 5 14; -fx-cursor: hand; -fx-font-size: 18px;";
                btn.setStyle("-fx-background-color: " + currentTheme.getSubAltColor() + "; -fx-text-fill: " +
                        currentTheme.getMainColor() + "; " + baseStyle);
            }
        });

        btn.setOnMouseExited(e -> {
            if (!btn.isSelected()) {
                String baseStyle = "-fx-border-color: transparent; -fx-background-radius: 6; -fx-padding: 5 14; -fx-cursor: hand; -fx-font-size: 18px;";
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + currentTheme.getSubColor() + "; " + baseStyle);
            }
        });

        return btn;
    }

    private Region createSeparator() {
        Region separator = new Region();
        separator.setPrefWidth(1);
        separator.setPrefHeight(20);
        separator.setStyle("-fx-background-color: " + currentTheme.getSubColor() + "; -fx-opacity: 0.3;");
        return separator;
    }

    private void addMetricToGrid(GridPane grid, int col, String label, String value) {
        VBox metricBox = new VBox(4);
        metricBox.setAlignment(Pos.CENTER);

        Label labelLabel = new Label(label);
        labelLabel.setFont(new Font("Lexend Deca", 16));
        labelLabel.setStyle("-fx-text-fill: " + currentTheme.getSubColor() + ";");

        Label valueLabel = new Label(value);
        valueLabel.setFont(new Font("Lexend Deca", 24));
        valueLabel.setStyle("-fx-text-fill: " + currentTheme.getCaretColor() + "; -fx-font-weight: 500;");
        valueLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        metricBox.getChildren().addAll(labelLabel, valueLabel);
        grid.add(metricBox, col, 0);
    }

    private LineChart<Number, Number> createCombinedChart(TypeTutorModel model) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("");
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(model.getSelectedTime());
        xAxis.setTickUnit(Math.max(model.getSelectedTime() / 6.0, 5));
        styleAxis(xAxis);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("");
        yAxis.setAutoRanging(true);
        styleAxis(yAxis);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setCreateSymbols(false);
        styleChart(chart);

        XYChart.Series<Number, Number> wpmSeries = new XYChart.Series<>();
        wpmSeries.setName("wpm");
        XYChart.Series<Number, Number> errorSeries = new XYChart.Series<>();
        errorSeries.setName("errors");

        for (TypeTutorModel.WPMSnapshot snapshot : model.getWpmHistory()) {
            wpmSeries.getData().add(new XYChart.Data<>(snapshot.second, snapshot.wpm));
        }
        for (TypeTutorModel.ErrorSnapshot snapshot : model.getErrorHistory()) {
            errorSeries.getData().add(new XYChart.Data<>(snapshot.second, snapshot.errors));
        }

        chart.getData().addAll(wpmSeries, errorSeries);

        Platform.runLater(() -> {
            if (chart.lookup(".chart-plot-background") != null) {
                chart.lookup(".chart-plot-background").setStyle("-fx-background-color: " + currentTheme.getSubAltColor() + ";");
            }

            chart.lookupAll(".chart-series-line").forEach(node -> {
                if (node.getStyleClass().contains("series0")) {
                    node.setStyle("-fx-stroke: " + currentTheme.getCaretColor() + "; -fx-stroke-width: 2.5px;");
                } else if (node.getStyleClass().contains("series1")) {
                    node.setStyle("-fx-stroke: " + currentTheme.getErrorColor() + "; -fx-stroke-width: 2.5px;");
                }
            });

            chart.setVerticalGridLinesVisible(false);
            chart.setHorizontalGridLinesVisible(true);
            chart.lookupAll(".chart-horizontal-grid-lines").forEach(node -> {
                node.setStyle("-fx-stroke: " + currentTheme.getSubColor() + "; -fx-stroke-width: 0.5px; -fx-opacity: 0.15;");
            });
        });

        return chart;
    }

    private void styleChart(LineChart<Number, Number> chart) {
        chart.setStyle("-fx-background-color: " + currentTheme.getBgColor() + "; -fx-font-size: 14px;");
        Platform.runLater(() -> {
            if (chart.lookup(".chart-plot-background") != null) {
                chart.lookup(".chart-plot-background").setStyle("-fx-background-color: " + currentTheme.getSubAltColor() + ";");
            }
        });
        chart.setLegendVisible(false);
    }

    private void styleAxis(NumberAxis axis) {
        axis.setStyle("-fx-tick-label-fill: " + currentTheme.getSubColor() + "; -fx-font-size: 14px;");
        axis.setTickLabelFill(Color.web(currentTheme.getSubColor()));
    }
}