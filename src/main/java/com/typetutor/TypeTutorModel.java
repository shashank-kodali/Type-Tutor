package com.typetutor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class TypeTutorModel {

    // --- Data Structures ---
    public static class WPMSnapshot {
        public final int second;
        public final double wpm;
        public final double rawWpm;
        public WPMSnapshot(int second, double wpm, double rawWpm) { this.second = second; this.wpm = wpm; this.rawWpm = rawWpm; }
    }

    public static class ErrorSnapshot {
        public final int second;
        public final int errors;
        public ErrorSnapshot(int second, int errors) { this.second = second; this.errors = errors; }
    }

    public enum Difficulty { BEGINNER, INTERMEDIATE, ADVANCED }

    // --- CONSTANTS FOR TEXT LOADING ---
    private static final String[] LOCAL_TEXT_FILES = {"texts/book1.txt", "texts/book2.txt", "texts/book3.txt"};
    private static final String[] GUTENBERG_URLS = {
            "https://www.gutenberg.org/files/1342/1342-0.txt",
            "https://www.gutenberg.org/files/84/84-0.txt",
            "https://www.gutenberg.org/files/11/11-0.txt"
    };
    private static final String[] BEGINNER_SENTENCES = { "the quick brown fox jumps over the lazy dog", "a journey of a thousand miles begins with a single step", "practice makes perfect", "time flies when you are having fun", "every cloud has a silver lining", "actions speak louder than words", "where there is a will there is a way", "the early bird catches the worm", "you cannot judge a book by its cover", "all good things come to those who wait"};
    private static final String[] INTERMEDIATE_SENTENCES = { "To be, or not to be, that is the question.", "All that glitters is not gold.", "Actions speak louder than words.", "Where there's a will, there's a way.", "The pen is mightier than the sword.", "Beauty is in the eye of the beholder.", "Fortune favors the bold.", "Knowledge is power.", "Time waits for no one.", "The best things in life are free."};
    private static final String[] ADVANCED_SENTENCES = { "In 2024, the GDP grew by 3.5% compared to last year's 2.8%.", "\"Success is not final, failure is not fatal,\" said Winston Churchill in 1945.", "The formula E=mc^2 was discovered by Einstein in 1905.", "Call me at 555-1234, or email: test@example.com!", "Price: $49.99 (was $79.99) - Save 37.5% today!", "The API endpoint is: https://api.example.com/v1/users?id=123", "Use JSON format: {\"name\": \"John\", \"age\": 30, \"active\": true}", "Error code #404: Resource not found at /path/to/file.html", "Temperature range: -15°C to +35°C (5°F to 95°F)", "Calculate: (25 + 17) × 3 - 8² ÷ 4 = ?"};

    // --- State Variables ---
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

    // --- Getters and Setters ---
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

    // --- Core Logic Methods ---
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

    public double calculateConsistency() {
        if (wpmHistory.size() < 2) return 100.0;
        List<Double> wpmValues = new ArrayList<>();
        for (WPMSnapshot snapshot : wpmHistory) {
            wpmValues.add(snapshot.wpm);
        }
        double mean = wpmValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = wpmValues.stream().mapToDouble(wpm -> Math.pow(wpm - mean, 2)).average().orElse(0.0);
        double stdDev = Math.sqrt(variance);
        return mean > 0 ? Math.max(0, 100 - (stdDev / mean * 100)) : 0;
    }

    // --- Text Management ---
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

    // --- ASYNC TEXT LOADING WITH FALLBACK ---
    public CompletableFuture<Void> loadTextsAsync() {
        return CompletableFuture.runAsync(() -> {
            // First, try to fetch from web URLs
            for (String urlString : GUTENBERG_URLS) {
                try {
                    String content = fetchTextFromUrl(urlString);
                    textPool.add(cleanText(content));
                    System.out.println("Successfully loaded text from URL: " + urlString);
                    return; // Exit after first success
                } catch (Exception e) {
                    System.err.println("Could not fetch from URL: " + urlString);
                }
            }

            // If web fails, try local files
            for (String filePath : LOCAL_TEXT_FILES) {
                try {
                    String content = new String(Files.readAllBytes(Paths.get(filePath)));
                    textPool.add(cleanText(content));
                    System.out.println("Successfully loaded text from local file: " + filePath);
                    return; // Exit after first success
                } catch (IOException e) {
                    System.err.println("Could not load local file: " + filePath);
                }
            }

            // FIXED: If both web and local fail, use generated samples as fallback
            if (textPool.isEmpty()) {
                textPool.add(generateSampleText());
                System.out.println("Using generated sample text as fallback.");
            }
        });
    }

    // --- HELPER METHODS ---
    private String fetchTextFromUrl(String urlString) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    private String cleanText(String text) {
        text = text.replaceAll("(?s).*?\\*\\*\\* START OF.*?\\*\\*\\*", "");
        text = text.replaceAll("(?s)\\*\\*\\* END OF.*", "");
        text = text.replaceAll("\\s+", " ");
        return text.trim();
    }

    private String generateSampleText() {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        String[] sourceArray;
        switch (currentDifficulty) {
            case ADVANCED:
                sourceArray = ADVANCED_SENTENCES;
                break;
            case INTERMEDIATE:
                sourceArray = INTERMEDIATE_SENTENCES;
                break;
            default:
                sourceArray = BEGINNER_SENTENCES;
                break;
        }

        for (int i = 0; i < 100; i++) {
            sb.append(sourceArray[random.nextInt(sourceArray.length)]).append(" ");
        }
        return sb.toString();
    }
}