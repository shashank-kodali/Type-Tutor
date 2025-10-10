package com.typetutor;

import javafx.application.Platform;
import javafx.scene.control.ToggleButton;
import java.util.Timer;
import java.util.TimerTask;

public class TypeTutorController {

    private final TypeTutorModel model;
    private final TypeTutorView view;
    private Timer gameTimer;

    public TypeTutorController(TypeTutorModel model, TypeTutorView view) {
        this.model = model;
        this.view = view;
        attachEventHandlers();

        model.loadTextsAsync().thenRun(() -> Platform.runLater(() -> {
            model.prepareLongText();
            model.loadNewSentence();
            view.displaySentence(model.getCurrentSentence(), "");
        }));
    }

    private void attachEventHandlers() {
        view.getStartButton().setOnAction(e -> handleStartButton());

        view.getInputField().textProperty().addListener((obs, oldVal, newVal) -> {
            if (model.isGameActive()) {
                handleTextInput(newVal);
            }
        });

        view.getDifficultyGroup().selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !model.isGameActive()) {
                String difficulty = ((ToggleButton) newVal).getText();
                model.setCurrentDifficulty(TypeTutorModel.Difficulty.valueOf(difficulty.toUpperCase()));
                model.prepareLongText();
                model.loadNewSentence();
                view.displaySentence(model.getCurrentSentence(), "");
            }
        });

        view.getTimerGroup().selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !model.isGameActive()) {
                if ("custom".equals(((ToggleButton) newVal).getText())) {
                    // Custom time dialog logic would go here
                    return;
                }
                int time = Integer.parseInt(((ToggleButton) newVal).getText());
                model.setSelectedTime(time);
                view.updateTimerLabel(String.valueOf(time));
            }
        });
    }

    private void handleStartButton() {
        if (model.isGameActive()) {
            endTest();
        } else {
            startTest();
        }
    }

    private void startTest() {
        model.setGameActive(true);
        model.resetForTest();
        model.loadNewSentence();

        view.prepareForActiveTest();
        view.displaySentence(model.getCurrentSentence(), model.getTypedText());

        gameTimer = new Timer(true);
        gameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                tick();
            }
        }, 1000, 1000);
    }

    private void tick() {
        model.tick();
        Platform.runLater(() -> view.updateTimerLabel(String.valueOf(model.getRemainingSeconds())));

        if (model.getRemainingSeconds() <= 0) {
            Platform.runLater(this::endTest);
        }
    }

    private void endTest() {
        if (!model.isGameActive()) return; // Prevent multiple calls

        model.setGameActive(false);
        if (gameTimer != null) {
            gameTimer.cancel();
        }

        processCharacterComparison(model.getTypedText(), true);
        view.showResults(model);
    }

    private void handleTextInput(String typedText) {
        model.setTypedText(typedText);
        view.displaySentence(model.getCurrentSentence(), typedText);

        if (typedText.equals(model.getCurrentSentence())) {
            int[] stats = processCharacterComparison(typedText, false);
            model.processCompletedSentence(stats[0], stats[1], stats[2], stats[3]);

            Platform.runLater(() -> {
                view.getInputField().clear();
                view.displaySentence(model.getCurrentSentence(), "");
            });
        }
    }

    private int[] processCharacterComparison(String typedText, boolean isFinal) {
        int correct = 0;
        int missed = 0;
        int extra = 0;
        int total = typedText.length();
        String currentSentence = model.getCurrentSentence();

        int matchLength = Math.min(currentSentence.length(), typedText.length());
        for (int i = 0; i < matchLength; i++) {
            if (currentSentence.charAt(i) == typedText.charAt(i)) {
                correct++;
            } else {
                missed++;
            }
        }

        if (typedText.length() > currentSentence.length()) {
            extra = typedText.length() - currentSentence.length();
        }

        if (isFinal) {
            model.processFinalChars(correct, total, missed, extra);
        }

        return new int[]{correct, total, missed, extra};
    }
}