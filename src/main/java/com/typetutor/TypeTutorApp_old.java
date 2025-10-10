package com.typetutor;

import javafx.application.Application;
import javafx.application.Platform;
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
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TypeTutorApp_old extends Application {

    private static final String BG_COLOR = "#323437";
    private static final String SUB_ALT_COLOR = "#2c2e31";
    private static final String SUB_COLOR = "#646669";
    private static final String MAIN_COLOR = "#d1d0c5";
    private static final String CARET_COLOR = "#e2b714";
    private static final String TEXT_COLOR = "#646669";
    private static final String ERROR_COLOR = "#ca4754";
    private static final String ERROR_EXTRA_COLOR = "#7e2a33";

    private enum Difficulty { BEGINNER, INTERMEDIATE, ADVANCED }

    private static final String[] LOCAL_TEXT_FILES = {"texts/book1.txt", "texts/book2.txt", "texts/book3.txt"};
    private static final String[] GUTENBERG_URLS = {
            "https://www.gutenberg.org/files/1342/1342-0.txt",
            "https://www.gutenberg.org/files/84/84-0.txt",
            "https://www.gutenberg.org/files/11/11-0.txt"
    };

    private static final String[] BEGINNER_SENTENCES = {
            "the quick brown fox jumps over the lazy dog",
            "a journey of a thousand miles begins with a single step",
            "practice makes perfect",
            "time flies when you are having fun",
            "every cloud has a silver lining",
            "actions speak louder than words",
            "where there is a will there is a way",
            "the early bird catches the worm",
            "you cannot judge a book by its cover",
            "all good things come to those who wait"
    };

    private static final String[] INTERMEDIATE_SENTENCES = {
            "To be, or not to be, that is the question.",
            "All that glitters is not gold.",
            "Actions speak louder than words.",
            "Where there's a will, there's a way.",
            "The pen is mightier than the sword.",
            "Beauty is in the eye of the beholder.",
            "Fortune favors the bold.",
            "Knowledge is power.",
            "Time waits for no one.",
            "The best things in life are free."
    };

    private static final String[] ADVANCED_SENTENCES = {
            "In 2024, the GDP grew by 3.5% compared to last year's 2.8%.",
            "\"Success is not final, failure is not fatal,\" said Winston Churchill in 1945.",
            "The formula E=mc^2 was discovered by Einstein in 1905.",
            "Call me at 555-1234, or email: test@example.com!",
            "Price: $49.99 (was $79.99) - Save 37.5% today!",
            "The API endpoint is: https://api.example.com/v1/users?id=123",
            "Use JSON format: {\"name\": \"John\", \"age\": 30, \"active\": true}",
            "Error code #404: Resource not found at /path/to/file.html",
            "Temperature range: -15°C to +35°C (5°F to 95°F)",
            "Calculate: (25 + 17) × 3 - 8² ÷ 4 = ?"
    };

    private TextFlow sentenceDisplay;
    private TextField inputField;
    private Label timerLabel;
    private Button startButton;
    private ToggleGroup difficultyGroup;
    private ToggleGroup timerGroup;
    private VBox mainTypingArea; // Renamed for clarity
    private VBox resultsPanel;
    private StackPane centerStackPane;

    private Difficulty currentDifficulty = Difficulty.BEGINNER;
    private int selectedTime = 30;
    private String currentSentence = "";
    private Timer gameTimer;
    private int remainingSeconds;
    private boolean isGameActive = false;
    private long startTime;

    private List<String> textPool = new ArrayList<>();
    private int currentTextIndex = 0;
    private String longText = "";
    private int textPosition = 0;
    private static final int TEXT_CHUNK_SIZE = 200;

    private List<WPMSnapshot> wpmHistory = new ArrayList<>();
    private List<ErrorSnapshot> errorHistory = new ArrayList<>();
    private int cumulativeCorrectChars = 0;
    private int cumulativeTotalChars = 0;
    private int cumulativeMissedChars = 0;
    private int cumulativeExtraChars = 0;

    private int currentChunkCorrect = 0;
    private int currentChunkTotal = 0;
    private int currentChunkMissed = 0;
    private int currentChunkExtra = 0;

    private static class WPMSnapshot {
        int second;
        double wpm;
        double rawWpm;
        WPMSnapshot(int second, double wpm, double rawWpm) {
            this.second = second;
            this.wpm = wpm;
            this.rawWpm = rawWpm;
        }
    }

    private static class ErrorSnapshot {
        int second;
        int errors;
        ErrorSnapshot(int second, int errors) {
            this.second = second;
            this.errors = errors;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(50, 30, 20, 30));
        root.setStyle("-fx-background-color: " + BG_COLOR + ";");

        VBox topSection = new VBox(15);
        topSection.setAlignment(Pos.CENTER);
        topSection.setPadding(new Insets(30, 0, 0, 0));

        Label titleLabel = new Label("Type Tutor");
        titleLabel.setFont(new Font("Lexend Deca", 36)); // User's custom font size
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + CARET_COLOR + ";");

        HBox controlBar = new HBox(30);
        controlBar.setAlignment(Pos.CENTER);
        controlBar.setPadding(new Insets(5, 0, 5, 0));

        HBox difficultySection = new HBox(8);
        difficultySection.setAlignment(Pos.CENTER);

        Label difficultyIcon = new Label("@");
        difficultyIcon.setFont(new Font("Consolas", 16));
        difficultyIcon.setStyle("-fx-text-fill: " + SUB_COLOR + ";");

        Label difficultyLabel = new Label("difficulty");
        difficultyLabel.setFont(new Font("Lexend Deca", 12));
        difficultyLabel.setStyle("-fx-text-fill: " + SUB_COLOR + ";");

        difficultyGroup = new ToggleGroup();
        HBox difficultyButtons = new HBox(5);
        difficultyButtons.setAlignment(Pos.CENTER);

        ToggleButton beginnerBtn = createMonkeyToggleButton("beginner", difficultyGroup);
        ToggleButton intermediateBtn = createMonkeyToggleButton("intermediate", difficultyGroup);
        ToggleButton advancedBtn = createMonkeyToggleButton("advanced", difficultyGroup);

        beginnerBtn.setSelected(true);
        difficultyButtons.getChildren().addAll(beginnerBtn, intermediateBtn, advancedBtn);
        difficultySection.getChildren().addAll(difficultyIcon, difficultyLabel, difficultyButtons);

        HBox timerSection = new HBox(8);
        timerSection.setAlignment(Pos.CENTER);

        Label timerIcon = new Label("⏱");
        timerIcon.setFont(new Font("Consolas", 16));
        timerIcon.setStyle("-fx-text-fill: " + SUB_COLOR + ";");

        Label timerSectionLabel = new Label("time");
        timerSectionLabel.setFont(new Font("Lexend Deca", 12));
        timerSectionLabel.setStyle("-fx-text-fill: " + SUB_COLOR + ";");

        timerGroup = new ToggleGroup();
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

        controlBar.getChildren().addAll(difficultySection, createSeparator(), timerSection);
        topSection.getChildren().addAll(titleLabel, controlBar);

        centerStackPane = new StackPane();
        centerStackPane.setAlignment(Pos.CENTER);

        // --- START OF MODIFIED LAYOUT CODE ---

        // Initialize components with your custom styles
        timerLabel = new Label("30");
        timerLabel.setFont(new Font("Lexend Deca", 28));
        timerLabel.setStyle("-fx-text-fill: " + CARET_COLOR + "; -fx-font-weight: 300;");

        sentenceDisplay = new TextFlow();
        sentenceDisplay.setStyle("-fx-background-color: transparent; -fx-padding: 20;");
        sentenceDisplay.setPrefHeight(120);
        sentenceDisplay.setLineSpacing(6);

        inputField = new TextField();
        inputField.setPromptText("Click 'start' to begin...");
        inputField.setFont(new Font("Roboto Mono", 15));
        inputField.setPrefHeight(45);
        inputField.setDisable(true);
        inputField.setStyle("-fx-background-color: transparent; -fx-text-fill: " + MAIN_COLOR + "; " +
                "-fx-prompt-text-fill: " + SUB_COLOR + "; -fx-background-radius: 8; " +
                "-fx-border-color: " + SUB_COLOR + "; -fx-border-radius: 8; " +
                "-fx-border-width: 2; -fx-padding: 12;");

        inputField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal && isGameActive) {
                inputField.setStyle("-fx-background-color: transparent; -fx-text-fill: " + MAIN_COLOR + "; " +
                        "-fx-prompt-text-fill: " + SUB_COLOR + "; -fx-background-radius: 8; " +
                        "-fx-border-color: " + CARET_COLOR + "; -fx-border-radius: 8; " +
                        "-fx-border-width: 2; -fx-padding: 12;");
            } else {
                inputField.setStyle("-fx-background-color: transparent; -fx-text-fill: " + MAIN_COLOR + "; " +
                        "-fx-prompt-text-fill: " + SUB_COLOR + "; -fx-background-radius: 8; " +
                        "-fx-border-color: " + SUB_COLOR + "; -fx-border-radius: 8; " +
                        "-fx-border-width: 2; -fx-padding: 12;");
            }
        });

        startButton = new Button("start");
        startButton.setFont(new Font("Lexend Deca", 18)); // User's custom font size
        startButton.setPrefWidth(120);
        startButton.setPrefHeight(40);
        startButton.setStyle("-fx-background-color: " + SUB_COLOR + "; -fx-text-fill: " + BG_COLOR + "; " +
                "-fx-background-radius: 8; -fx-font-weight: 500; " +
                "-fx-border-color: transparent; -fx-cursor: hand;");

        startButton.setOnMouseEntered(e -> {
            if (!isGameActive) {
                startButton.setStyle("-fx-background-color: " + MAIN_COLOR + "; -fx-text-fill: " + BG_COLOR + "; " +
                        "-fx-background-radius: 8; -fx-font-weight: 500; " +
                        "-fx-border-color: transparent; -fx-cursor: hand;");
            }
        });

        startButton.setOnMouseExited(e -> {
            if (!isGameActive) {
                startButton.setStyle("-fx-background-color: " + SUB_COLOR + "; -fx-text-fill: " + BG_COLOR + "; " +
                        "-fx-background-radius: 8; -fx-font-weight: 500; " +
                        "-fx-border-color: transparent; -fx-cursor: hand;");
            } else {
                startButton.setStyle("-fx-background-color: " + ERROR_COLOR + "; -fx-text-fill: " + BG_COLOR + "; " +
                        "-fx-background-radius: 8; -fx-font-weight: 500; " +
                        "-fx-border-color: transparent; -fx-cursor: hand;");
            }
        });

        // 1. Create Individual Containers for each component
        StackPane timerPane = new StackPane(timerLabel);
        StackPane sentencePane = new StackPane(sentenceDisplay);
        StackPane inputPane = new StackPane(inputField);
        StackPane buttonPane = new StackPane(startButton);

        // 2. Apply positioning with Padding
        sentencePane.setPadding(new Insets(100, 0, 200, 0)); // 80px bottom padding to push top elements up
        buttonPane.setPadding(new Insets(20, 0, 0, 0));   // 20px top padding for space above button

        // 3. Create the new parent VBox that will hold the individual containers
        mainTypingArea = new VBox(); // Spacing is now controlled by padding on the panes
        mainTypingArea.setAlignment(Pos.CENTER);
        mainTypingArea.setPrefWidth(750);
        mainTypingArea.setMaxWidth(750);

        // 4. Add the individual containers to the parent VBox
        mainTypingArea.getChildren().addAll(timerPane, sentencePane, inputPane, buttonPane);

        // 5. Add the new layout and results panel to the main screen
        resultsPanel = createResultsPanel();
        resultsPanel.setVisible(false);
        centerStackPane.getChildren().addAll(mainTypingArea, resultsPanel);

        // --- END OF MODIFIED LAYOUT CODE ---

        difficultyGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                ToggleButton btn = (ToggleButton) newVal;
                updateDifficulty(btn.getText());
            }
        });

        timerGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                ToggleButton btn = (ToggleButton) newVal;
                updateTimer(btn.getText());
            }
        });

        startButton.setOnAction(e -> startTest());

        inputField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (isGameActive) {
                updateDisplay(newVal);
            }
        });

        root.setTop(topSection);
        root.setCenter(centerStackPane);

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("Type Tutor");
        primaryStage.setScene(scene);
        primaryStage.show();

        loadTextsAsync();
        loadNewSentence();
    }

    private VBox createResultsPanel() {
        VBox panel = new VBox(45);
        panel.setAlignment(Pos.CENTER);
        panel.setPrefWidth(1050);
        panel.setMaxWidth(1050);
        panel.setPadding(new Insets(30));
        return panel;
    }

    private void showResults() {
        resultsPanel.getChildren().clear();

        double timeInMinutes = selectedTime / 60.0;
        double correctWords = cumulativeCorrectChars / 5.0;
        double totalWords = cumulativeTotalChars / 5.0;
        double wpm = correctWords / timeInMinutes;
        double rawWpm = totalWords / timeInMinutes;
        double accuracy = cumulativeTotalChars > 0 ? (cumulativeCorrectChars * 100.0 / cumulativeTotalChars) : 0;

        int skipped = 0;
        double consistency = calculateConsistency();

        HBox mainResultsBox = new HBox(60);
        mainResultsBox.setAlignment(Pos.CENTER);
        mainResultsBox.setPadding(new Insets(20));

        VBox leftStats = new VBox(45);
        leftStats.setAlignment(Pos.CENTER_LEFT);
        leftStats.setPadding(new Insets(40));

        VBox wpmBox = new VBox(12);
        wpmBox.setAlignment(Pos.CENTER_LEFT);
        Label wpmLabel = new Label("wpm");
        wpmLabel.setFont(new Font("Lexend Deca", 32));
        wpmLabel.setStyle("-fx-text-fill: " + SUB_COLOR + ";");
        Label wpmValue = new Label(String.format("%.0f", wpm));
        wpmValue.setFont(new Font("Lexend Deca", 135));
        wpmValue.setStyle("-fx-text-fill: " + CARET_COLOR + "; -fx-font-weight: bold;");
        wpmBox.getChildren().addAll(wpmLabel, wpmValue);

        VBox accBox = new VBox(12);
        accBox.setAlignment(Pos.CENTER_LEFT);
        Label accLabel = new Label("acc");
        accLabel.setFont(new Font("Lexend Deca", 32));
        accLabel.setStyle("-fx-text-fill: " + SUB_COLOR + ";");
        Label accValue = new Label(String.format("%.0f%%", accuracy));
        accValue.setFont(new Font("Lexend Deca", 135));
        accValue.setStyle("-fx-text-fill: " + CARET_COLOR + "; -fx-font-weight: bold;");
        accBox.getChildren().addAll(accLabel, accValue);

        leftStats.getChildren().addAll(wpmBox, accBox);

        LineChart<Number, Number> chart = createCombinedChart();
        chart.setPrefWidth(900);
        chart.setPrefHeight(675);
        chart.setMaxHeight(675);

        mainResultsBox.getChildren().addAll(leftStats, chart);

        GridPane metricsGrid = new GridPane();
        metricsGrid.setAlignment(Pos.CENTER);
        metricsGrid.setHgap(90);
        metricsGrid.setVgap(0);
        metricsGrid.setPadding(new Insets(20, 0, 20, 0));

        String diffName = currentDifficulty.name().toLowerCase();
        addMetricToGrid(metricsGrid, 0, "test type", String.format("time %d\n%s", selectedTime, diffName));
        addMetricToGrid(metricsGrid, 1, "raw", String.format("%.0f", rawWpm));
        addMetricToGrid(metricsGrid, 2, "characters",
                String.format("%d/%d/%d/%d", cumulativeCorrectChars, cumulativeMissedChars, cumulativeExtraChars, skipped));
        addMetricToGrid(metricsGrid, 3, "consistency", String.format("%.0f%%", consistency));
        addMetricToGrid(metricsGrid, 4, "time", String.format("%ds", selectedTime));

        Button nextButton = new Button("next test");
        nextButton.setFont(new Font("Lexend Deca", 32));
        nextButton.setPrefWidth(270);
        nextButton.setPrefHeight(90);
        nextButton.setStyle("-fx-background-color: " + SUB_COLOR + "; -fx-text-fill: " + MAIN_COLOR + "; " +
                "-fx-background-radius: 18; -fx-font-weight: 500; " +
                "-fx-border-color: transparent; -fx-cursor: hand;");
        nextButton.setOnMouseEntered(e ->
                nextButton.setStyle("-fx-background-color: " + MAIN_COLOR + "; -fx-text-fill: " + BG_COLOR + "; " +
                        "-fx-background-radius: 18; -fx-font-weight: 600; " +
                        "-fx-border-color: transparent; -fx-cursor: hand;"));
        nextButton.setOnMouseExited(e ->
                nextButton.setStyle("-fx-background-color: " + SUB_COLOR + "; -fx-text-fill: " + MAIN_COLOR + "; " +
                        "-fx-background-radius: 18; -fx-font-weight: 500; " +
                        "-fx-border-color: transparent; -fx-cursor: hand;"));
        nextButton.setOnAction(e -> prepareNextTest());

        resultsPanel.getChildren().addAll(mainResultsBox, metricsGrid, nextButton);
    }

    private void addMetricToGrid(GridPane grid, int col, String label, String value) {
        VBox metricBox = new VBox(9);
        metricBox.setAlignment(Pos.CENTER);

        Label labelLabel = new Label(label);
        labelLabel.setFont(new Font("Lexend Deca", 27));
        labelLabel.setStyle("-fx-text-fill: " + SUB_COLOR + ";");

        Label valueLabel = new Label(value);
        valueLabel.setFont(new Font("Lexend Deca", 40));
        valueLabel.setStyle("-fx-text-fill: " + MAIN_COLOR + "; -fx-font-weight: 500;");
        valueLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        metricBox.getChildren().addAll(labelLabel, valueLabel);
        grid.add(metricBox, col, 0);
    }

    private LineChart<Number, Number> createCombinedChart() {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("");
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(selectedTime);
        xAxis.setTickUnit(Math.max(selectedTime / 6.0, 5));
        styleAxis(xAxis);

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("");
        yAxis.setAutoRanging(true);
        styleAxis(yAxis);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setLegendVisible(true);
        chart.setCreateSymbols(true);
        styleChart(chart);

        XYChart.Series<Number, Number> wpmSeries = new XYChart.Series<>();
        wpmSeries.setName("wpm");

        XYChart.Series<Number, Number> errorSeries = new XYChart.Series<>();
        errorSeries.setName("errors");

        System.out.println("Creating chart with " + wpmHistory.size() + " WPM points and " + errorHistory.size() + " error points");

        for (WPMSnapshot snapshot : wpmHistory) {
            XYChart.Data<Number, Number> data = new XYChart.Data<>(snapshot.second, snapshot.wpm);
            wpmSeries.getData().add(data);
        }

        for (ErrorSnapshot snapshot : errorHistory) {
            XYChart.Data<Number, Number> data = new XYChart.Data<>(snapshot.second, snapshot.errors);
            errorSeries.getData().add(data);
        }

        chart.getData().addAll(wpmSeries, errorSeries);

        Platform.runLater(() -> {
            for (XYChart.Data<Number, Number> data : wpmSeries.getData()) {
                Circle circle = new Circle(6.75);
                circle.setFill(Color.web(CARET_COLOR));
                circle.setStroke(Color.web(CARET_COLOR));
                circle.setStrokeWidth(2.25);
                data.setNode(circle);
            }

            for (XYChart.Data<Number, Number> data : errorSeries.getData()) {
                Circle circle = new Circle(6.75);
                circle.setFill(Color.web(ERROR_COLOR));
                circle.setStroke(Color.web(ERROR_COLOR));
                circle.setStrokeWidth(2.25);
                data.setNode(circle);
            }

            chart.lookupAll(".chart-series-line").forEach(node -> {
                if (node.getStyleClass().contains("series0")) {
                    node.setStyle("-fx-stroke: " + CARET_COLOR + "; -fx-stroke-width: 4.5px;");
                } else if (node.getStyleClass().contains("series1")) {
                    node.setStyle("-fx-stroke: " + ERROR_COLOR + "; -fx-stroke-width: 4.5px;");
                }
            });
        });

        return chart;
    }

    private double calculateConsistency() {
        if (wpmHistory.size() < 2) return 100.0;

        List<Double> wpmValues = new ArrayList<>();
        for (WPMSnapshot snapshot : wpmHistory) {
            wpmValues.add(snapshot.wpm);
        }

        double mean = wpmValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = wpmValues.stream()
                .mapToDouble(wpm -> Math.pow(wpm - mean, 2))
                .average()
                .orElse(0.0);
        double stdDev = Math.sqrt(variance);

        double consistency = mean > 0 ? Math.max(0, 100 - (stdDev / mean * 100)) : 0;
        return consistency;
    }

    private void styleChart(LineChart<Number, Number> chart) {
        chart.setStyle("-fx-background-color: transparent; -fx-font-size: 22px;");
        Platform.runLater(() -> {
            if (chart.lookup(".chart-plot-background") != null) {
                chart.lookup(".chart-plot-background").setStyle("-fx-background-color: " + SUB_ALT_COLOR + ";");
            }
        });
        chart.setLegendVisible(true);
    }

    private void styleAxis(NumberAxis axis) {
        axis.setStyle("-fx-tick-label-fill: " + SUB_COLOR + "; -fx-font-size: 22px;");
        axis.setTickLabelFill(Color.web(SUB_COLOR));
    }

    private void prepareNextTest() {
        wpmHistory.clear();
        errorHistory.clear();
        cumulativeCorrectChars = 0;
        cumulativeTotalChars = 0;
        cumulativeMissedChars = 0;
        cumulativeExtraChars = 0;
        currentChunkCorrect = 0;
        currentChunkTotal = 0;
        currentChunkMissed = 0;
        currentChunkExtra = 0;
        textPosition = 0;

        resultsPanel.setVisible(false);
        mainTypingArea.setVisible(true); // MODIFIED

        timerLabel.setText(String.valueOf(selectedTime));
        inputField.clear();
        inputField.setDisable(true);
        inputField.setPromptText("Click 'start' to begin...");
        startButton.setText("start");
        startButton.setStyle("-fx-background-color: " + SUB_COLOR + "; -fx-text-fill: " + BG_COLOR + "; " +
                "-fx-background-radius: 8; -fx-font-weight: 500; " +
                "-fx-border-color: transparent; -fx-cursor: hand;");
        loadNewSentence();

        difficultyGroup.getToggles().forEach(toggle ->
                ((ToggleButton) toggle).setDisable(false));
        timerGroup.getToggles().forEach(toggle ->
                ((ToggleButton) toggle).setDisable(false));
    }

    private ToggleButton createMonkeyToggleButton(String text, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setFont(new Font("Lexend Deca", 18)); // User's custom font size
        btn.setPrefHeight(28);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + SUB_COLOR + "; " +
                "-fx-border-color: transparent; -fx-background-radius: 6; " +
                "-fx-padding: 5 14; -fx-cursor: hand;");

        btn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            String baseStyle = "-fx-border-color: transparent; -fx-background-radius: 6; -fx-padding: 5 14; -fx-cursor: hand; -fx-font-size: 18px;";
            if (newVal) {
                btn.setStyle("-fx-background-color: " + CARET_COLOR + "; -fx-text-fill: " + BG_COLOR + "; -fx-font-weight: 600; " + baseStyle);
            } else {
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + SUB_COLOR + "; " + baseStyle);
            }
        });

        btn.setOnMouseEntered(e -> {
            if (!btn.isSelected()) {
                String baseStyle = "-fx-border-color: transparent; -fx-background-radius: 6; -fx-padding: 5 14; -fx-cursor: hand; -fx-font-size: 18px;";
                btn.setStyle("-fx-background-color: " + SUB_ALT_COLOR + "; -fx-text-fill: " + MAIN_COLOR + "; " + baseStyle);
            }
        });

        btn.setOnMouseExited(e -> {
            if (!btn.isSelected()) {
                String baseStyle = "-fx-border-color: transparent; -fx-background-radius: 6; -fx-padding: 5 14; -fx-cursor: hand; -fx-font-size: 18px;";
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + SUB_COLOR + "; " + baseStyle);
            }
        });

        return btn;
    }

    private Region createSeparator() {
        Region separator = new Region();
        separator.setPrefWidth(1);
        separator.setPrefHeight(20);
        separator.setStyle("-fx-background-color: " + SUB_COLOR + "; -fx-opacity: 0.3;");
        return separator;
    }

    private void loadTextsAsync() {
        CompletableFuture.runAsync(() -> {
            for (String filePath : LOCAL_TEXT_FILES) {
                try {
                    String content = new String(Files.readAllBytes(Paths.get(filePath)));
                    textPool.add(cleanText(content));
                } catch (IOException e) {
                    System.err.println("Could not load local file: " + filePath);
                }
            }

            if (textPool.isEmpty()) {
                for (String urlString : GUTENBERG_URLS) {
                    try {
                        String content = fetchTextFromUrl(urlString);
                        textPool.add(cleanText(content));
                        break;
                    } catch (Exception e) {
                        System.err.println("Could not fetch from URL: " + urlString);
                    }
                }
            }

            if (textPool.isEmpty()) {
                textPool.add(generateSampleText());
            }

            Platform.runLater(() -> prepareLongText());
        });
    }

    private String fetchTextFromUrl(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder content = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        reader.close();

        return content.toString();
    }

    private String cleanText(String text) {
        text = text.replaceAll("(?s).*?\\*\\*\\* START OF.*?\\*\\*\\*", "");
        text = text.replaceAll("(?s)\\*\\*\\* END OF.*", "");
        text = text.replaceAll("\\s+", " ");
        text = text.trim();
        return text;
    }

    private String generateSampleText() {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 100; i++) {
            String[] currentArray;
            switch (currentDifficulty) {
                case ADVANCED:
                    currentArray = ADVANCED_SENTENCES;
                    break;
                case INTERMEDIATE:
                    currentArray = INTERMEDIATE_SENTENCES;
                    break;
                default:
                    currentArray = BEGINNER_SENTENCES;
            }
            sb.append(currentArray[random.nextInt(currentArray.length)]).append(" ");
        }

        return sb.toString();
    }

    private void prepareLongText() {
        if (textPool.isEmpty()) {
            longText = generateSampleText();
        } else {
            longText = textPool.get(currentTextIndex % textPool.size());
        }
        textPosition = 0;
    }

    private void updateDifficulty(String difficulty) {
        switch (difficulty) {
            case "beginner":
                currentDifficulty = Difficulty.BEGINNER;
                break;
            case "intermediate":
                currentDifficulty = Difficulty.INTERMEDIATE;
                break;
            case "advanced":
                currentDifficulty = Difficulty.ADVANCED;
                break;
        }
        if (!isGameActive) {
            prepareLongText();
            loadNewSentence();
        }
    }

    private void updateTimer(String time) {
        if (time.equals("custom")) {
            TextInputDialog dialog = new TextInputDialog("45");
            dialog.setTitle("Custom Time");
            dialog.setHeaderText("Enter time in seconds:");
            dialog.setContentText("Seconds:");

            DialogPane dialogPane = dialog.getDialogPane();
            dialogPane.setStyle("-fx-background-color: " + BG_COLOR + ";");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(seconds -> {
                try {
                    selectedTime = Integer.parseInt(seconds);
                    if (selectedTime < 10 || selectedTime > 300) {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Invalid Time");
                        alert.setHeaderText(null);
                        alert.setContentText("Please enter between 10 and 300 seconds.");
                        alert.showAndWait();
                        selectedTime = 30;
                        timerGroup.selectToggle(timerGroup.getToggles().get(1));
                    }
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Invalid Input");
                    alert.setHeaderText(null);
                    alert.setContentText("Invalid input. Using default 30 seconds.");
                    alert.showAndWait();
                    selectedTime = 30;
                    timerGroup.selectToggle(timerGroup.getToggles().get(1));
                }
            });
        } else {
            selectedTime = Integer.parseInt(time);
        }
        timerLabel.setText(String.valueOf(selectedTime));
    }

    private void loadNewSentence() {
        if (longText.isEmpty()) {
            prepareLongText();
        }

        if (textPosition >= longText.length() - TEXT_CHUNK_SIZE) {
            currentTextIndex++;
            prepareLongText();
        }

        int endPos = Math.min(textPosition + TEXT_CHUNK_SIZE, longText.length());
        currentSentence = longText.substring(textPosition, endPos).trim();

        int lastSpace = currentSentence.lastIndexOf(' ');
        if (lastSpace > TEXT_CHUNK_SIZE * 0.8 && endPos < longText.length()) {
            currentSentence = currentSentence.substring(0, lastSpace);
        }

        displaySentence(currentSentence, "");
    }

    private void displaySentence(String sentence, String typedText) {
        sentenceDisplay.getChildren().clear();

        int matchLength = Math.min(sentence.length(), typedText.length());

        for (int i = 0; i < sentence.length(); i++) {
            Text charText = new Text(String.valueOf(sentence.charAt(i)));
            charText.setFont(new Font("Roboto Mono", 30)); // User's custom font size

            if (i < matchLength) {
                if (sentence.charAt(i) == typedText.charAt(i)) {
                    charText.setFill(Color.web(MAIN_COLOR));
                } else {
                    charText.setFill(Color.web(ERROR_COLOR));
                    charText.setStyle("-fx-font-weight: bold;");
                }
            } else if (i == matchLength && typedText.length() > 0) {
                charText.setFill(Color.web(CARET_COLOR));
                charText.setStyle("-fx-underline: true; -fx-font-weight: bold;");
            } else {
                charText.setFill(Color.web(TEXT_COLOR));
            }

            sentenceDisplay.getChildren().add(charText);
        }

        if (typedText.length() > sentence.length()) {
            for (int i = sentence.length(); i < typedText.length(); i++) {
                Text extraChar = new Text(String.valueOf(typedText.charAt(i)));
                extraChar.setFont(new Font("Roboto Mono", 30)); // User's custom font size
                extraChar.setFill(Color.web(ERROR_EXTRA_COLOR));
                extraChar.setStyle("-fx-font-weight: bold;");
                sentenceDisplay.getChildren().add(extraChar);
            }
        }
    }

    private void updateDisplay(String typedText) {
        displaySentence(currentSentence, typedText);

        currentChunkCorrect = 0;
        currentChunkMissed = 0;
        currentChunkExtra = 0;
        currentChunkTotal = typedText.length();

        int matchLength = Math.min(currentSentence.length(), typedText.length());
        for (int i = 0; i < matchLength; i++) {
            if (currentSentence.charAt(i) == typedText.charAt(i)) {
                currentChunkCorrect++;
            } else {
                currentChunkMissed++;
            }
        }

        if (typedText.length() > currentSentence.length()) {
            currentChunkExtra = typedText.length() - currentSentence.length();
        }

        if (typedText.equals(currentSentence)) {
            cumulativeCorrectChars += currentChunkCorrect;
            cumulativeTotalChars += currentChunkTotal;
            cumulativeMissedChars += currentChunkMissed;
            cumulativeExtraChars += currentChunkExtra;

            currentChunkCorrect = 0;
            currentChunkTotal = 0;
            currentChunkMissed = 0;
            currentChunkExtra = 0;

            textPosition += currentSentence.length();
            loadNewSentence();
            inputField.clear();
        }
    }

    private void startTest() {
        if (isGameActive) {
            endTest();
            return;
        }

        isGameActive = true;
        remainingSeconds = selectedTime;
        cumulativeCorrectChars = 0;
        cumulativeTotalChars = 0;
        cumulativeMissedChars = 0;
        cumulativeExtraChars = 0;
        currentChunkCorrect = 0;
        currentChunkTotal = 0;
        currentChunkMissed = 0;
        currentChunkExtra = 0;
        wpmHistory.clear();
        errorHistory.clear();
        startTime = System.currentTimeMillis();

        inputField.clear();
        inputField.setDisable(false);
        inputField.setPromptText("start typing...");
        inputField.requestFocus();

        startButton.setText("stop");
        startButton.setStyle("-fx-background-color: " + ERROR_COLOR + "; -fx-text-fill: " + BG_COLOR + "; " +
                "-fx-background-radius: 8; -fx-font-weight: 500; " +
                "-fx-border-color: transparent; -fx-cursor: hand;");

        difficultyGroup.getToggles().forEach(toggle ->
                ((ToggleButton) toggle).setDisable(true));
        timerGroup.getToggles().forEach(toggle ->
                ((ToggleButton) toggle).setDisable(true));

        textPosition = 0;
        loadNewSentence();

        gameTimer = new Timer();
        gameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    int elapsedSeconds = selectedTime - remainingSeconds + 1;

                    int totalCorrect = cumulativeCorrectChars + currentChunkCorrect;
                    int totalTyped = cumulativeTotalChars + currentChunkTotal;
                    int totalErrors = (cumulativeMissedChars + currentChunkMissed) + (cumulativeExtraChars + currentChunkExtra);

                    double minutes = elapsedSeconds / 60.0;
                    double wpm = totalCorrect > 0 ? (totalCorrect / 5.0) / minutes : 0;
                    double rawWpm = totalTyped > 0 ? (totalTyped / 5.0) / minutes : 0;

                    wpmHistory.add(new WPMSnapshot(elapsedSeconds, wpm, rawWpm));
                    errorHistory.add(new ErrorSnapshot(elapsedSeconds, totalErrors));

                    System.out.println("Second " + elapsedSeconds + ": WPM=" + String.format("%.1f", wpm) +
                            ", Raw=" + String.format("%.1f", rawWpm) + ", Errors=" + totalErrors);

                    remainingSeconds--;
                    timerLabel.setText(String.valueOf(remainingSeconds));

                    if (remainingSeconds <= 0) {
                        endTest();
                    }
                });
            }
        }, 1000, 1000);
    }

    private void endTest() {
        isGameActive = false;

        if (gameTimer != null) {
            gameTimer.cancel();
        }

        inputField.setDisable(true);

        cumulativeCorrectChars += currentChunkCorrect;
        cumulativeTotalChars += currentChunkTotal;
        cumulativeMissedChars += currentChunkMissed;
        cumulativeExtraChars += currentChunkExtra;

        System.out.println("Test ended. Final stats:");
        System.out.println("  Cumulative correct: " + cumulativeCorrectChars);
        System.out.println("  Cumulative total: " + cumulativeTotalChars);
        System.out.println("  WPM history size: " + wpmHistory.size());
        System.out.println("  Error history size: " + errorHistory.size());

        mainTypingArea.setVisible(false); // MODIFIED
        resultsPanel.setVisible(true);
        showResults();
    }

    public static void main(String[] args) {
        launch(args);
    }
}