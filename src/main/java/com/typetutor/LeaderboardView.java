package com.typetutor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

/**
 * Leaderboard showing top performers
 */
public class LeaderboardView {

    private final DatabaseManager dbManager;
    private final ThemeManager themeManager;
    private final int currentUserId;
    private Theme currentTheme;
    private Stage stage;
    private VBox leaderboardList;

    public LeaderboardView(DatabaseManager dbManager, ThemeManager themeManager, int currentUserId) {
        this.dbManager = dbManager;
        this.themeManager = themeManager;
        this.currentTheme = themeManager.getCurrentTheme();
        this.currentUserId = currentUserId;
    }

    /**
     * Show leaderboard window
     */
    public void show() {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Leaderboard");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + currentTheme.getBgColor() + ";");

        // Top section
        HBox topBar = createTopBar();
        root.setTop(topBar);

        // Center section - Leaderboard
        VBox centerContent = createCenterContent();
        root.setCenter(centerContent);

        Scene scene = new Scene(root, 800, 700);
        stage.setScene(scene);
        stage.show();
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 20, 0));

        Label titleLabel = new Label("🏆 Leaderboard");
        titleLabel.setFont(new Font("Lexend Deca", 32));
        titleLabel.setStyle("-fx-text-fill: " + currentTheme.getCaretColor() + "; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeButton = new Button("✕");
        closeButton.setFont(new Font("Lexend Deca", 20));
        closeButton.setStyle("-fx-background-color: " + currentTheme.getSubColor() + "; -fx-text-fill: " +
                currentTheme.getBgColor() + "; -fx-background-radius: 8; -fx-cursor: hand;");
        closeButton.setPrefWidth(40);
        closeButton.setPrefHeight(40);
        closeButton.setOnAction(e -> stage.close());

        topBar.getChildren().addAll(titleLabel, spacer, closeButton);
        return topBar;
    }

    private VBox createCenterContent() {
        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);

        // Filter buttons
        HBox filters = createFilters(content);
        content.getChildren().add(filters);

        // Leaderboard list
        leaderboardList = new VBox(0);
        leaderboardList.setAlignment(Pos.TOP_CENTER);

        ScrollPane scrollPane = new ScrollPane(leaderboardList);
        scrollPane.setStyle("-fx-background: " + currentTheme.getBgColor() + "; -fx-background-color: transparent;");
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500);

        content.getChildren().add(scrollPane);

        // Load default leaderboard
        try {
            loadLeaderboard(50);
        } catch (SQLException e) {
            Label errorLabel = new Label("Error loading leaderboard: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: " + currentTheme.getErrorColor() + ";");
            leaderboardList.getChildren().add(errorLabel);
        }

        return content;
    }

    private HBox createFilters(VBox content) {
        HBox filters = new HBox(10);
        filters.setAlignment(Pos.CENTER);

        Label label = new Label("Show:");
        label.setStyle("-fx-text-fill: " + currentTheme.getMainColor() + ";");
        label.setFont(new Font("Lexend Deca", 14));

        ToggleGroup filterGroup = new ToggleGroup();

        ToggleButton top10Btn = createFilterButton("Top 10", filterGroup);
        ToggleButton top25Btn = createFilterButton("Top 25", filterGroup);
        ToggleButton top50Btn = createFilterButton("Top 50", filterGroup);
        ToggleButton top100Btn = createFilterButton("Top 100", filterGroup);

        top50Btn.setSelected(true);

        // Update leaderboard when selection changes
        filterGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String text = ((ToggleButton) newVal).getText();
                int limit = Integer.parseInt(text.split(" ")[1]);
                try {
                    loadLeaderboard(limit);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });

        filters.getChildren().addAll(label, top10Btn, top25Btn, top50Btn, top100Btn);
        return filters;
    }

    private ToggleButton createFilterButton(String text, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setFont(new Font("Lexend Deca", 14));
        btn.setStyle("-fx-background-color: " + currentTheme.getSubAltColor() + "; -fx-text-fill: " +
                currentTheme.getMainColor() + "; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;");

        btn.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                btn.setStyle("-fx-background-color: " + currentTheme.getCaretColor() + "; -fx-text-fill: " +
                        currentTheme.getBgColor() + "; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand; -fx-font-weight: bold;");
            } else {
                btn.setStyle("-fx-background-color: " + currentTheme.getSubAltColor() + "; -fx-text-fill: " +
                        currentTheme.getMainColor() + "; -fx-background-radius: 6; -fx-padding: 8 16; -fx-cursor: hand;");
            }
        });

        return btn;
    }

    private void loadLeaderboard(int limit) throws SQLException {
        leaderboardList.getChildren().clear();

        // Add header
        HBox header = createLeaderboardHeader();
        leaderboardList.getChildren().add(header);

        // Get leaderboard data
        List<DatabaseManager.LeaderboardEntry> entries = dbManager.getLeaderboard(limit);

        if (entries.isEmpty()) {
            Label noDataLabel = new Label("No leaderboard data available yet.");
            noDataLabel.setStyle("-fx-text-fill: " + currentTheme.getSubColor() + "; -fx-padding: 20;");
            leaderboardList.getChildren().add(noDataLabel);
        } else {
            for (DatabaseManager.LeaderboardEntry entry : entries) {
                HBox entryBox = createLeaderboardEntry(entry);
                leaderboardList.getChildren().add(entryBox);
            }
        }
    }

    private HBox createLeaderboardHeader() {
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(15, 20, 15, 20));
        header.setStyle("-fx-background-color: " + currentTheme.getSubAltColor() + "; -fx-border-color: " +
                currentTheme.getSubColor() + "; -fx-border-width: 0 0 2 0;");

        Label rankLabel = new Label("Rank");
        rankLabel.setFont(new Font("Lexend Deca", 14));
        rankLabel.setStyle("-fx-text-fill: " + currentTheme.getSubColor() + "; -fx-font-weight: bold;");
        rankLabel.setPrefWidth(80);

        Label nameLabel = new Label("Username");
        nameLabel.setFont(new Font("Lexend Deca", 14));
        nameLabel.setStyle("-fx-text-fill: " + currentTheme.getSubColor() + "; -fx-font-weight: bold;");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        Label wpmLabel = new Label("Best WPM");
        wpmLabel.setFont(new Font("Lexend Deca", 14));
        wpmLabel.setStyle("-fx-text-fill: " + currentTheme.getSubColor() + "; -fx-font-weight: bold;");
        wpmLabel.setPrefWidth(120);
        wpmLabel.setAlignment(Pos.CENTER_RIGHT);

        Label accLabel = new Label("Accuracy");
        accLabel.setFont(new Font("Lexend Deca", 14));
        accLabel.setStyle("-fx-text-fill: " + currentTheme.getSubColor() + "; -fx-font-weight: bold;");
        accLabel.setPrefWidth(120);
        accLabel.setAlignment(Pos.CENTER_RIGHT);

        header.getChildren().addAll(rankLabel, nameLabel, wpmLabel, accLabel);
        return header;
    }

    private HBox createLeaderboardEntry(DatabaseManager.LeaderboardEntry entry) {
        // Check if this is the current user
        boolean isCurrentUser = false;
        try {
            DatabaseManager.UserStatistics userStats = dbManager.getUserStatistics(currentUserId);
            if (userStats != null && userStats.bestWpm == entry.bestWpm) {
                // Simple check - could be improved with username comparison
                isCurrentUser = true;
            }
        } catch (SQLException e) {
            // Ignore
        }

        HBox entryBox = new HBox(20);
        entryBox.setAlignment(Pos.CENTER_LEFT);
        entryBox.setPadding(new Insets(15, 20, 15, 20));

        String bgColor = isCurrentUser ? currentTheme.getSubAltColor() : "transparent";
        String borderColor = isCurrentUser ? currentTheme.getCaretColor() : currentTheme.getSubColor();
        String borderWidth = isCurrentUser ? "2" : "1";

        entryBox.setStyle("-fx-background-color: " + bgColor + "; -fx-border-color: " + borderColor +
                "; -fx-border-width: 0 0 " + borderWidth + " 0;");

        // Rank with medal for top 3
        Label rankLabel = new Label(getRankDisplay(entry.rank));
        rankLabel.setFont(new Font("Lexend Deca", 18));
        rankLabel.setStyle("-fx-text-fill: " + getRankColor(entry.rank) + "; -fx-font-weight: bold;");
        rankLabel.setPrefWidth(80);

        // Username
        Label nameLabel = new Label(entry.username);
        nameLabel.setFont(new Font("Lexend Deca", 16));
        String nameColor = isCurrentUser ? currentTheme.getCaretColor() : currentTheme.getMainColor();
        String nameWeight = isCurrentUser ? "bold" : "normal";
        nameLabel.setStyle("-fx-text-fill: " + nameColor + "; -fx-font-weight: " + nameWeight + ";");
        HBox.setHgrow(nameLabel, Priority.ALWAYS);

        // WPM
        Label wpmLabel = new Label(String.format("%.0f WPM", entry.bestWpm));
        wpmLabel.setFont(new Font("Lexend Deca", 16));
        wpmLabel.setStyle("-fx-text-fill: " + currentTheme.getCaretColor() + "; -fx-font-weight: bold;");
        wpmLabel.setPrefWidth(120);
        wpmLabel.setAlignment(Pos.CENTER_RIGHT);

        // Accuracy
        Label accLabel = new Label(String.format("%.0f%%", entry.bestAccuracy));
        accLabel.setFont(new Font("Lexend Deca", 16));
        accLabel.setStyle("-fx-text-fill: " + currentTheme.getMainColor() + ";");
        accLabel.setPrefWidth(120);
        accLabel.setAlignment(Pos.CENTER_RIGHT);

        entryBox.getChildren().addAll(rankLabel, nameLabel, wpmLabel, accLabel);
        return entryBox;
    }

    private String getRankDisplay(int rank) {
        switch (rank) {
            case 1: return "🥇 1";
            case 2: return "🥈 2";
            case 3: return "🥉 3";
            default: return "#" + rank;
        }
    }

    private String getRankColor(int rank) {
        switch (rank) {
            case 1: return "#FFD700"; // Gold
            case 2: return "#C0C0C0"; // Silver
            case 3: return "#CD7F32"; // Bronze
            default: return currentTheme.getMainColor();
        }
    }
}