package com.typetutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class TypeTutorModel {

    public static class WPMSnapshot {
        public final int second;
        public final double wpm;
        public final double rawWpm;
        public WPMSnapshot(int second, double wpm, double rawWpm) {
            this.second = second;
            this.wpm = wpm;
            this.rawWpm = rawWpm;
        }
    }

    public static class ErrorSnapshot {
        public final int second;
        public final int errors;
        public ErrorSnapshot(int second, int errors) {
            this.second = second;
            this.errors = errors;
        }
    }

    public enum Difficulty { BEGINNER, INTERMEDIATE, ADVANCED }

    private boolean isGameActive = false;
    private int selectedTime = 30;
    private int remainingSeconds;
    private Difficulty currentDifficulty = Difficulty.BEGINNER;
    private String currentSentence = "";
    private String typedText = "";

    private List<String> textPool = new ArrayList<>();
    private String longText = "";
    private int textPosition = 0;
    private int currentTextIndex = 0;
    private static final int TEXT_CHUNK_SIZE = 200;

    private List<WPMSnapshot> wpmHistory = new ArrayList<>();
    private List<ErrorSnapshot> errorHistory = new ArrayList<>();
    private int cumulativeCorrectChars = 0;
    private int cumulativeTotalChars = 0;
    private int cumulativeMissedChars = 0;
    private int cumulativeExtraChars = 0;

    public boolean isGameActive() { return isGameActive; }
    public void setGameActive(boolean gameActive) { isGameActive = gameActive; }
    public int getSelectedTime() { return selectedTime; }
    public void setSelectedTime(int selectedTime) { this.selectedTime = selectedTime; }
    public int getRemainingSeconds() { return remainingSeconds; }
    public Difficulty getCurrentDifficulty() { return currentDifficulty; }
    public void setCurrentDifficulty(Difficulty currentDifficulty) { this.currentDifficulty = currentDifficulty; }
    public String getCurrentSentence() { return currentSentence; }
    public String getTypedText() { return typedText; }
    public void setTypedText(String typedText) { this.typedText = typedText; }
    public List<WPMSnapshot> getWpmHistory() { return wpmHistory; }
    public List<ErrorSnapshot> getErrorHistory() { return errorHistory; }
    public int getCumulativeCorrectChars() { return cumulativeCorrectChars; }
    public int getCumulativeTotalChars() { return cumulativeTotalChars; }
    public int getCumulativeMissedChars() { return cumulativeMissedChars; }
    public int getCumulativeExtraChars() { return cumulativeExtraChars; }

    public void resetForTest() {
        this.remainingSeconds = this.selectedTime;
        this.wpmHistory.clear();
        this.errorHistory.clear();
        this.cumulativeCorrectChars = 0;
        this.cumulativeTotalChars = 0;
        this.cumulativeMissedChars = 0;
        this.cumulativeExtraChars = 0;
        this.textPosition = 0;
        this.typedText = "";
    }

    public void tick() {
        if (isGameActive) {
            remainingSeconds--;
        }
    }

    public void processCompletedSentence(int correct, int total, int missed, int extra) {
        this.cumulativeCorrectChars += correct;
        this.cumulativeTotalChars += total;
        this.cumulativeMissedChars += missed;
        this.cumulativeExtraChars += extra;
        this.textPosition += this.currentSentence.length();
        this.typedText = "";
        loadNewSentence();
    }

    public void processFinalChars(int correct, int total, int missed, int extra) {
        this.cumulativeCorrectChars += correct;
        this.cumulativeTotalChars += total;
        this.cumulativeMissedChars += missed;
        this.cumulativeExtraChars += extra;
    }

    public void loadNewSentence() {
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
    }

    public void prepareLongText() {
        if (textPool.isEmpty()) {
            longText = generateSampleText();
        } else {
            longText = textPool.get(currentTextIndex % textPool.size());
        }
        textPosition = 0;
    }

    public CompletableFuture<Void> loadTextsAsync() {
        return CompletableFuture.runAsync(() -> {
            if (textPool.isEmpty()) {
                textPool.add(generateSampleText());
            }
        });
    }

    private String generateSampleText() {
        String[] BEGINNER_SENTENCES = {
                "the quick brown fox jumps over the lazy dog", "practice makes perfect", "a journey of a thousand miles begins with a single step", "every cloud has a silver lining"
        };
        StringBuilder sb = new StringBuilder();
        Random rand = new Random();
        for(int i = 0; i < 100; i++) {
            sb.append(BEGINNER_SENTENCES[rand.nextInt(BEGINNER_SENTENCES.length)]).append(" ");
        }
        return sb.toString();
    }
}