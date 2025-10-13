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

    private static final String BG_COLOR = "#323437";
    private static final String SUB_ALT_COLOR = "#2c2e31";
    private static final String SUB_COLOR = "#646669";
    private static final String MAIN_COLOR = "#d1d0c5";
    private static final String CARET_COLOR = "#e2b714";
    private static final String TEXT_COLOR = "#646669";
    private static final String ERROR_COLOR = "#ca4754";
    private static final String ERROR_EXTRA_COLOR = "#7e2a33";

    private TextFlow sentenceDisplay;
    private TextField inputField;
    private Label timerLabel;
    private Button startButton;
    private ToggleGroup difficultyGroup;
    private ToggleGroup timerGroup;
    private VBox mainTypingArea;
    private ScrollPane resultsScrollPane;  // CHANGED: Use ScrollPane instead of VBox
    private VBox resultsPanel;

    public TextField getInputField() { return inputField; }
    public Button getStartButton() { return startButton; }
    public ToggleGroup getDifficultyGroup() { return difficultyGroup; }
    public ToggleGroup getTimerGroup() { return timerGroup; }

    public TypeTutorView(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(50, 30, 20, 30));
        root.setStyle("-fx-background-color: " + BG_COLOR + ";");

        VBox topSection = createTopSection();
        StackPane centerStackPane = createCenterStackPane();

        root.setTop(topSection);
        root.setCenter(centerStackPane);

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("Type Tutor");
        primaryStage.setScene(scene);
        primaryStage.show();
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
                    extraChar.setFont(new Font("Roboto Mono", 30));
                    extraChar.setFill(Color.web(ERROR_EXTRA_COLOR));
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
            resultsScrollPane.setVisible(false);
            mainTypingArea.setVisible(true);
            timerLabel.setText(String.valueOf(selectedTime));
            inputField.clear();
            inputField.setDisable(true);
            inputField.setPromptText("Click 'start' to begin...");
            startButton.setText("start");
            startButton.setStyle("-fx-background-color: " + SUB_COLOR + "; -fx-text-fill: " + BG_COLOR + "; -fx-background-radius: 8; -fx-font-weight: 500; -fx-border-color: transparent; -fx-cursor: hand;");
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
            startButton.setStyle("-fx-background-color: " + ERROR_COLOR + "; -fx-text-fill: " + BG_COLOR + "; -fx-background-radius: 8; -fx-font-weight: 500; -fx-border-color: transparent; -fx-cursor: hand;");
            difficultyGroup.getToggles().forEach(toggle -> ((ToggleButton) toggle).setDisable(true));
            timerGroup.getToggles().forEach(toggle -> ((ToggleButton) toggle).setDisable(true));
        });
    }

    public void showResults(TypeTutorModel model, EventHandler<ActionEvent> nextTestHandler) {
        Platform.runLater(() -> {
            resultsPanel.getChildren().clear();

            double timeInMinutes = model.getSelectedTime() / 60.0;
            double wpm = (model.getCumulativeCorrectChars() / 5.0) / timeInMinutes;
            double rawWpm = (model.getCumulativeTotalChars() / 5.0) / timeInMinutes;
            double accuracy = model.getCumulativeTotalChars() > 0 ? (model.getCumulativeCorrectChars() * 100.0 / model.getCumulativeTotalChars()) : 0;
            double consistency = model.calculateConsistency();

            HBox mainResultsBox = new HBox(40);  // Reduced from 60 to 40
            mainResultsBox.setAlignment(Pos.CENTER);
            mainResultsBox.setPadding(new Insets(10, 0, 10, 0));  // Reduced padding

            VBox leftStats = new VBox(30);  // Reduced from 45 to 30
            leftStats.setAlignment(Pos.CENTER_LEFT);
            leftStats.setPadding(new Insets(20));  // Reduced from 40 to 20

            VBox wpmBox = new VBox(8);  // Reduced from 12 to 8
            wpmBox.setAlignment(Pos.CENTER_LEFT);
            Label wpmLabel = new Label("wpm");
            wpmLabel.setFont(new Font("Lexend Deca", 24));  // Reduced from 32 to 24
            wpmLabel.setStyle("-fx-text-fill: " + SUB_COLOR + ";");
            Label wpmValue = new Label(String.format("%.0f", wpm));
            wpmValue.setFont(new Font("Lexend Deca", 70));  // Reduced from 135 to 100
            wpmValue.setStyle("-fx-text-fill: " + CARET_COLOR + "; -fx-font-weight: bold;");
            wpmBox.getChildren().addAll(wpmLabel, wpmValue);

            VBox accBox = new VBox(8);  // Reduced from 12 to 8
            accBox.setAlignment(Pos.CENTER_LEFT);
            Label accLabel = new Label("acc");
            accLabel.setFont(new Font("Lexend Deca", 24));  // Reduced from 32 to 24
            accLabel.setStyle("-fx-text-fill: " + SUB_COLOR + ";");
            Label accValue = new Label(String.format("%.0f%%", accuracy));
            accValue.setFont(new Font("Lexend Deca", 70));  // Reduced from 135 to 100
            accValue.setStyle("-fx-text-fill: " + CARET_COLOR + "; -fx-font-weight: bold;");
            accBox.getChildren().addAll(accLabel, accValue);

            leftStats.getChildren().addAll(wpmBox, accBox);

            LineChart<Number, Number> chart = createCombinedChart(model);
            chart.setPrefWidth(550);  // Reduced from 900 to 700
            chart.setPrefHeight(300);  // Reduced from 675 to 450
            chart.setMaxHeight(450);

            mainResultsBox.getChildren().addAll(leftStats, chart);

            GridPane metricsGrid = new GridPane();
            metricsGrid.setAlignment(Pos.CENTER);
            metricsGrid.setHgap(60);  // Reduced from 90 to 60
            metricsGrid.setVgap(0);
            metricsGrid.setPadding(new Insets(15, 0, 15, 0));  // Reduced from 20 to 15

            String diffName = model.getCurrentDifficulty().name().toLowerCase();
            addMetricToGrid(metricsGrid, 0, "test type", String.format("time %d\n%s", model.getSelectedTime(), diffName));
            addMetricToGrid(metricsGrid, 1, "raw", String.format("%.0f", rawWpm));
            addMetricToGrid(metricsGrid, 2, "characters", String.format("%d/%d/%d/%d", model.getCumulativeCorrectChars(), model.getCumulativeMissedChars(), model.getCumulativeExtraChars(), 0));
            addMetricToGrid(metricsGrid, 3, "consistency", String.format("%.0f%%", consistency));
            addMetricToGrid(metricsGrid, 4, "time", String.format("%ds", model.getSelectedTime()));

            Button nextTestButton = new Button("next test");
            nextTestButton.setFont(new Font("Lexend Deca", 28));  // Reduced from 32 to 28
            nextTestButton.setPrefWidth(240);  // Reduced from 270 to 240
            nextTestButton.setPrefHeight(70);  // Reduced from 90 to 70
            nextTestButton.setStyle("-fx-background-color: " + SUB_COLOR + "; -fx-text-fill: " + MAIN_COLOR + "; -fx-background-radius: 18; -fx-font-weight: 500; -fx-border-color: transparent; -fx-cursor: hand;");
            nextTestButton.setOnMouseEntered(e -> nextTestButton.setStyle("-fx-background-color: " + MAIN_COLOR + "; -fx-text-fill: " + BG_COLOR + "; -fx-background-radius: 18; -fx-font-weight: 600; -fx-border-color: transparent; -fx-cursor: hand;"));
            nextTestButton.setOnMouseExited(e -> nextTestButton.setStyle("-fx-background-color: " + SUB_COLOR + "; -fx-text-fill: " + MAIN_COLOR + "; -fx-background-radius: 18; -fx-font-weight: 500; -fx-border-color: transparent; -fx-cursor: hand;"));
            nextTestButton.setOnAction(nextTestHandler);

            resultsPanel.getChildren().addAll(mainResultsBox, metricsGrid, nextTestButton);

            // Scroll to top when results are shown
            resultsScrollPane.setVvalue(0);

            mainTypingArea.setVisible(false);
            resultsScrollPane.setVisible(true);
        });
    }

    private StackPane createCenterStackPane() {
        StackPane centerStackPane = new StackPane();
        centerStackPane.setAlignment(Pos.CENTER);
        mainTypingArea = createMainTypingArea();

        // FIXED: Wrap results panel in ScrollPane
        resultsPanel = createResultsPanel();
        resultsScrollPane = new ScrollPane(resultsPanel);
        resultsScrollPane.setStyle("-fx-background: " + BG_COLOR + "; -fx-background-color: " + BG_COLOR + ";");
        resultsScrollPane.setFitToWidth(true);
        resultsScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        resultsScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        resultsScrollPane.setVisible(false);

        centerStackPane.getChildren().addAll(mainTypingArea, resultsScrollPane);
        return centerStackPane;
    }

    private VBox createMainTypingArea() {
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
        inputField.setStyle("-fx-background-color: transparent; -fx-text-fill: " + MAIN_COLOR + "; -fx-prompt-text-fill: " + SUB_COLOR + "; -fx-background-radius: 8; -fx-border-color: " + SUB_COLOR + "; -fx-border-radius: 8; -fx-border-width: 2; -fx-padding: 12;");

        inputField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            boolean isGameActive = !startButton.getText().equals("start");
            if (newVal && isGameActive) {
                inputField.setStyle("-fx-background-color: transparent; -fx-text-fill: " + MAIN_COLOR + "; -fx-prompt-text-fill: " + SUB_COLOR + "; -fx-background-radius: 8; -fx-border-color: " + CARET_COLOR + "; -fx-border-radius: 8; -fx-border-width: 2; -fx-padding: 12;");
            } else {
                inputField.setStyle("-fx-background-color: transparent; -fx-text-fill: " + MAIN_COLOR + "; -fx-prompt-text-fill: " + SUB_COLOR + "; -fx-background-radius: 8; -fx-border-color: " + SUB_COLOR + "; -fx-border-radius: 8; -fx-border-width: 2; -fx-padding: 12;");
            }
        });

        startButton = new Button("start");
        startButton.setFont(new Font("Lexend Deca", 18));
        startButton.setPrefWidth(120);
        startButton.setPrefHeight(40);
        startButton.setStyle("-fx-background-color: " + SUB_COLOR + "; -fx-text-fill: " + BG_COLOR + "; -fx-background-radius: 8; -fx-font-weight: 500; -fx-border-color: transparent; -fx-cursor: hand;");

        startButton.setOnMouseEntered(e -> {
            if (startButton.getText().equals("start")) {
                startButton.setStyle("-fx-background-color: " + MAIN_COLOR + "; -fx-text-fill: " + BG_COLOR + "; -fx-background-radius: 8; -fx-font-weight: 500; -fx-border-color: transparent; -fx-cursor: hand;");
            }
        });

        startButton.setOnMouseExited(e -> {
            if (startButton.getText().equals("start")) {
                startButton.setStyle("-fx-background-color: " + SUB_COLOR + "; -fx-text-fill: " + BG_COLOR + "; -fx-background-radius: 8; -fx-font-weight: 500; -fx-border-color: transparent; -fx-cursor: hand;");
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
        VBox panel = new VBox(30);  // Reduced from 45 to 30
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(20));  // Reduced from 30 to 20
        return panel;
    }

    private VBox createTopSection() {
        VBox topSection = new VBox(15);
        topSection.setAlignment(Pos.CENTER);
        topSection.setPadding(new Insets(30, 0, 0, 0));

        Label titleLabel = new Label("Type Tutor");
        titleLabel.setFont(new Font("Lexend Deca", 36));
        titleLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + CARET_COLOR + ";");

        HBox controlBar = new HBox(30);
        controlBar.setAlignment(Pos.CENTER);
        controlBar.setPadding(new Insets(5, 0, 5, 0));

        difficultyGroup = new ToggleGroup();
        HBox difficultySection = new HBox(8);
        difficultySection.setAlignment(Pos.CENTER);

        Label difficultyIcon = new Label("@");
        difficultyIcon.setFont(new Font("Consolas", 16));
        difficultyIcon.setStyle("-fx-text-fill: " + SUB_COLOR + ";");

        Label difficultyLabel = new Label("difficulty");
        difficultyLabel.setFont(new Font("Lexend Deca", 12));
        difficultyLabel.setStyle("-fx-text-fill: " + SUB_COLOR + ";");

        HBox difficultyButtons = new HBox(5);
        difficultyButtons.setAlignment(Pos.CENTER);

        ToggleButton beginnerBtn = createMonkeyToggleButton("beginner", difficultyGroup);
        ToggleButton intermediateBtn = createMonkeyToggleButton("intermediate", difficultyGroup);
        ToggleButton advancedBtn = createMonkeyToggleButton("advanced", difficultyGroup);
        beginnerBtn.setSelected(true);

        difficultyButtons.getChildren().addAll(beginnerBtn, intermediateBtn, advancedBtn);
        difficultySection.getChildren().addAll(difficultyIcon, difficultyLabel, difficultyButtons);

        timerGroup = new ToggleGroup();
        HBox timerSection = new HBox(8);
        timerSection.setAlignment(Pos.CENTER);

        Label timerIcon = new Label("⏱");
        timerIcon.setFont(new Font("Consolas", 16));
        timerIcon.setStyle("-fx-text-fill: " + SUB_COLOR + ";");

        Label timerSectionLabel = new Label("time");
        timerSectionLabel.setFont(new Font("Lexend Deca", 12));
        timerSectionLabel.setStyle("-fx-text-fill: " + SUB_COLOR + ";");

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
        return topSection;
    }

    private ToggleButton createMonkeyToggleButton(String text, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setFont(new Font("Lexend Deca", 18));
        btn.setPrefHeight(28);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + SUB_COLOR + "; -fx-border-color: transparent; -fx-background-radius: 6; -fx-padding: 5 14; -fx-cursor: hand;");

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

    private void addMetricToGrid(GridPane grid, int col, String label, String value) {
        VBox metricBox = new VBox(6);  // Reduced from 9 to 6
        metricBox.setAlignment(Pos.CENTER);

        Label labelLabel = new Label(label);
        labelLabel.setFont(new Font("Lexend Deca", 20));  // Reduced from 27 to 20
        labelLabel.setStyle("-fx-text-fill: " + SUB_COLOR + ";");

        Label valueLabel = new Label(value);
        valueLabel.setFont(new Font("Lexend Deca", 30));  // Reduced from 40 to 30
        valueLabel.setStyle("-fx-text-fill: " + MAIN_COLOR + "; -fx-font-weight: 500;");
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
        chart.setLegendVisible(true);
        chart.setCreateSymbols(true);
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
            for (XYChart.Data<Number, Number> data : wpmSeries.getData()) {
                Circle circle = new Circle(5);  // Reduced from 6.75 to 5
                circle.setFill(Color.web(CARET_COLOR));
                data.setNode(circle);
            }
            for (XYChart.Data<Number, Number> data : errorSeries.getData()) {
                Circle circle = new Circle(5);  // Reduced from 6.75 to 5
                circle.setFill(Color.web(ERROR_COLOR));
                data.setNode(circle);
            }
            chart.lookupAll(".chart-series-line").forEach(node -> {
                if (node.getStyleClass().contains("series0")) {
                    node.setStyle("-fx-stroke: " + CARET_COLOR + "; -fx-stroke-width: 3.5px;");  // Reduced from 4.5px
                } else if (node.getStyleClass().contains("series1")) {
                    node.setStyle("-fx-stroke: " + ERROR_COLOR + "; -fx-stroke-width: 3.5px;");  // Reduced from 4.5px
                }
            });
        });
        return chart;
    }

    private void styleChart(LineChart<Number, Number> chart) {
        chart.setStyle("-fx-background-color: transparent; -fx-font-size: 18px;");  // Reduced from 22px to 18px
        Platform.runLater(() -> {
            if (chart.lookup(".chart-plot-background") != null) {
                chart.lookup(".chart-plot-background").setStyle("-fx-background-color: " + SUB_ALT_COLOR + ";");
            }
        });
        chart.setLegendVisible(true);
    }

    private void styleAxis(NumberAxis axis) {
        axis.setStyle("-fx-tick-label-fill: " + SUB_COLOR + "; -fx-font-size: 18px;");  // Reduced from 22px to 18px
        axis.setTickLabelFill(Color.web(SUB_COLOR));
    }
}