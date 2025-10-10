package com.typetutor;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
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

    private BorderPane root;
    private TextFlow sentenceDisplay;
    private TextField inputField;
    private Label timerLabel;
    private Button startButton;
    private ToggleGroup difficultyGroup;
    private ToggleGroup timerGroup;
    private VBox mainTypingArea;
    private VBox resultsPanel;

    public TextField getInputField() { return inputField; }
    public Button getStartButton() { return startButton; }
    public ToggleGroup getDifficultyGroup() { return difficultyGroup; }
    public ToggleGroup getTimerGroup() { return timerGroup; }

    public TypeTutorView(Stage primaryStage) {
        root = new BorderPane();
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

    private StackPane createCenterStackPane() {
        StackPane centerStackPane = new StackPane();
        centerStackPane.setAlignment(Pos.CENTER);
        mainTypingArea = createMainTypingArea();
        resultsPanel = createResultsPanel();
        resultsPanel.setVisible(false);
        centerStackPane.getChildren().addAll(mainTypingArea, resultsPanel);
        return centerStackPane;
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
                    }
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
            startButton.setStyle("-fx-background-color: " + SUB_COLOR + "; -fx-text-fill: " + BG_COLOR + "; -fx-font-size: 18px; -fx-background-radius: 8; -fx-font-weight: 500; -fx-border-color: transparent; -fx-cursor: hand;");
        });
    }

    public void prepareForActiveTest() {
        Platform.runLater(() -> {
            inputField.clear();
            inputField.setDisable(false);
            inputField.setPromptText("start typing...");
            inputField.requestFocus();
            startButton.setText("stop");
            startButton.setStyle("-fx-background-color: " + ERROR_COLOR + "; -fx-text-fill: " + BG_COLOR + "; -fx-font-size: 18px; -fx-background-radius: 8; -fx-font-weight: 500; -fx-border-color: transparent; -fx-cursor: hand;");
        });
    }

    public void showResults(TypeTutorModel model) {
        Platform.runLater(() -> {
            resultsPanel.getChildren().clear();
            double timeInMinutes = model.getSelectedTime() / 60.0;
            double wpm = (model.getCumulativeCorrectChars() / 5.0) / timeInMinutes;
            double accuracy = model.getCumulativeTotalChars() > 0 ? (model.getCumulativeCorrectChars() * 100.0 / model.getCumulativeTotalChars()) : 0;

            Label wpmLabel = new Label("WPM: " + String.format("%.0f", wpm));
            wpmLabel.setFont(new Font("Lexend Deca", 48));
            wpmLabel.setTextFill(Color.web(CARET_COLOR));

            Label accLabel = new Label("Accuracy: " + String.format("%.1f", accuracy) + "%");
            accLabel.setFont(new Font("Lexend Deca", 24));
            accLabel.setTextFill(Color.web(MAIN_COLOR));

            resultsPanel.getChildren().addAll(wpmLabel, accLabel);
            mainTypingArea.setVisible(false);
            resultsPanel.setVisible(true);
        });
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

        startButton = new Button("start");
        startButton.setFont(new Font("Lexend Deca", 18));
        startButton.setPrefWidth(120);
        startButton.setPrefHeight(40);
        startButton.setStyle("-fx-background-color: " + SUB_COLOR + "; -fx-text-fill: " + BG_COLOR + "; -fx-background-radius: 8; -fx-font-weight: 500; -fx-border-color: transparent; -fx-cursor: hand;");

        StackPane timerPane = new StackPane(timerLabel);
        StackPane sentencePane = new StackPane(sentenceDisplay);
        StackPane inputPane = new StackPane(inputField);
        StackPane buttonPane = new StackPane(startButton);

        sentencePane.setPadding(new Insets(100, 0, 200, 0));
        buttonPane.setPadding(new Insets(20, 0, 0, 0));

        VBox typingArea = new VBox();
        typingArea.setAlignment(Pos.CENTER);
        typingArea.setPrefWidth(750);
        typingArea.setMaxWidth(750);
        typingArea.getChildren().addAll(timerPane, sentencePane, inputPane, buttonPane);
        return typingArea;
    }

    private VBox createResultsPanel() {
        VBox panel = new VBox(45);
        panel.setAlignment(Pos.CENTER);
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
}