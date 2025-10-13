package com.typetutor;

import javafx.application.Platform;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

public class TypeTutorController {

    private final TypeTutorModel model;
    private final TypeTutorView view;
    private final DatabaseManager dbManager;  // NEW: Database manager
    private Timer gameTimer;
    private int currentUserId = -1;  // NEW: Track current user

    public TypeTutorController(TypeTutorModel model, TypeTutorView view, DatabaseManager dbManager) {
        this.model = model;
        this.view = view;
        this.dbManager = dbManager;  // NEW: Initialize database manager

        // NEW: Prompt for username on startup
        promptForUsername();

        attachEventHandlers();

        model.prepareLongText();
        model.loadNewSentence();
        view.displaySentence(model.getCurrentSentence(), "");

        model.loadTextsAsync().thenRun(() -> Platform.runLater(() -> {
            model.prepareLongText();
            model.loadNewSentence();
            view.displaySentence(model.getCurrentSentence(), "");
        }));
    }

    // NEW: Prompt user to enter username
    private void promptForUsername() {
        TextInputDialog dialog = new TextInputDialog("Guest");
        dialog.setTitle("Welcome to Type Tutor");
        dialog.setHeaderText("Enter your username to track your progress:");
        dialog.setContentText("Username:");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #323437;");
        dialogPane.lookup(".content.label").setStyle("-fx-text-fill: #d1d0c5;");

        dialog.showAndWait().ifPresent(username -> {
            try {
                currentUserId = dbManager.getOrCreateUser(username);
                System.out.println("Logged in as: " + username + " (ID: " + currentUserId + ")");

                // Show user statistics
                DatabaseManager.UserStatistics stats = dbManager.getUserStatistics(currentUserId);
                if (stats != null && stats.totalTests > 0) {
                    System.out.println("Your Stats: " + stats);
                }
            } catch (SQLException e) {
                System.err.println("Error creating/loading user: " + e.getMessage());
                currentUserId = -1;
            }
        });
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
                String timeStr = ((ToggleButton) newVal).getText();

                if ("custom".equals(timeStr)) {
                    TextInputDialog dialog = new TextInputDialog("45");
                    dialog.setTitle("Custom Time");
                    dialog.setHeaderText("Enter time in seconds (10-300):");
                    dialog.setContentText("Seconds:");

                    DialogPane dialogPane = dialog.getDialogPane();
                    dialogPane.setStyle("-fx-background-color: #323437;");
                    dialogPane.lookup(".content.label").setStyle("-fx-text-fill: #d1d0c5;");
                    dialogPane.lookup(".header-panel").setStyle("-fx-background-color: #2c2e31;");
                    dialogPane.lookup(".header-panel .label").setStyle("-fx-text-fill: #d1d0c5;");

                    dialog.showAndWait().ifPresent(seconds -> {
                        try {
                            int time = Integer.parseInt(seconds);
                            if (time >= 10 && time <= 300) {
                                model.setSelectedTime(time);
                                view.updateTimerLabel(String.valueOf(time));
                            } else {
                                if (oldVal != null) ((ToggleButton) oldVal).setSelected(true);
                            }
                        } catch (NumberFormatException e) {
                            if (oldVal != null) ((ToggleButton) oldVal).setSelected(true);
                        }
                    });
                } else {
                    int time = Integer.parseInt(timeStr);
                    model.setSelectedTime(time);
                    view.updateTimerLabel(String.valueOf(time));
                }
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

        System.out.println("=== Test Started ===");
        System.out.println("Duration: " + model.getSelectedTime() + " seconds");
        System.out.println("Difficulty: " + model.getCurrentDifficulty());

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

        int elapsedSeconds = model.getSelectedTime() - model.getRemainingSeconds();
        if (elapsedSeconds <= 0) return;

        int[] stats = processCharacterComparison(model.getTypedText(), false);
        int totalCorrect = model.getCumulativeCorrectChars() + stats[0];
        int totalTyped = model.getCumulativeTotalChars() + stats[1];
        int totalErrors = model.getCumulativeMissedChars() + model.getCumulativeExtraChars() + stats[2] + stats[3];
        double minutes = elapsedSeconds / 60.0;

        if (minutes > 0) {
            double wpm = (totalCorrect / 5.0) / minutes;
            double rawWpm = (totalTyped / 5.0) / minutes;
            model.getWpmHistory().add(new TypeTutorModel.WPMSnapshot(elapsedSeconds, wpm, rawWpm));
            model.getErrorHistory().add(new TypeTutorModel.ErrorSnapshot(elapsedSeconds, totalErrors));

            System.out.println("Second " + elapsedSeconds + ": WPM=" + String.format("%.1f", wpm) +
                    ", Raw=" + String.format("%.1f", rawWpm) + ", Errors=" + totalErrors);
        }

        if (model.getRemainingSeconds() <= 0) {
            Platform.runLater(this::endTest);
        }
    }

    private void endTest() {
        if (!model.isGameActive()) return;

        model.setGameActive(false);
        if (gameTimer != null) {
            gameTimer.cancel();
        }

        processCharacterComparison(model.getTypedText(), true);

        System.out.println("\n=== Test Ended ===");
        System.out.println("Final Stats:");
        System.out.println("  Cumulative correct: " + model.getCumulativeCorrectChars());
        System.out.println("  Cumulative total: " + model.getCumulativeTotalChars());
        System.out.println("  Cumulative missed: " + model.getCumulativeMissedChars());
        System.out.println("  Cumulative extra: " + model.getCumulativeExtraChars());
        System.out.println("  WPM history size: " + model.getWpmHistory().size());
        System.out.println("  Error history size: " + model.getErrorHistory().size());

        double timeInMinutes = model.getSelectedTime() / 60.0;
        double finalWpm = (model.getCumulativeCorrectChars() / 5.0) / timeInMinutes;
        double finalRawWpm = (model.getCumulativeTotalChars() / 5.0) / timeInMinutes;
        double finalAccuracy = model.getCumulativeTotalChars() > 0 ?
                (model.getCumulativeCorrectChars() * 100.0 / model.getCumulativeTotalChars()) : 0;

        System.out.println("  Final WPM: " + String.format("%.1f", finalWpm));
        System.out.println("  Final Raw WPM: " + String.format("%.1f", finalRawWpm));
        System.out.println("  Final Accuracy: " + String.format("%.1f%%", finalAccuracy));
        System.out.println("==================\n");

        // NEW: Save test result to database
        saveTestToDatabase();

        view.showResults(model, e -> handleNextTest());
    }

    // NEW: Save test result to database
    private void saveTestToDatabase() {
        if (currentUserId != -1) {
            try {
                int resultId = dbManager.saveTestResult(currentUserId, model);
                System.out.println("Test result saved to database (ID: " + resultId + ")");

                // Print updated user statistics
                DatabaseManager.UserStatistics stats = dbManager.getUserStatistics(currentUserId);
                if (stats != null) {
                    System.out.println("Updated Stats: " + stats);
                }
            } catch (SQLException e) {
                System.err.println("Error saving test result: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("No user logged in - test result not saved");
        }
    }

    private void handleNextTest() {
        System.out.println("=== Preparing Next Test ===\n");
        model.resetForTest();
        view.prepareForNewTest(model.getSelectedTime());
        model.prepareLongText();
        model.loadNewSentence();
        view.displaySentence(model.getCurrentSentence(), "");
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