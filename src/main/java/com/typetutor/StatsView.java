package com.typetutor;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Statistics Dashboard showing user progress and insights
 */
public class StatsView {

    private final DatabaseManager dbManager;
    private final ThemeManager themeManager;
    private final int userId;
    private Theme currentTheme;
    private Stage stage;

    public StatsView(DatabaseManager dbManager, ThemeManager themeManager, int userId) {
        this.dbManager = dbManager;
        this.themeManager = themeManager;
        this.currentTheme = themeManager.getCurrentTheme();
        this.userId = userId;
    }

    /**
     * Show statistics window
     */
    public void show() {
        stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Statistics Dashboard");
        stage.setAlwaysOnTop(false); // Don't force always on top

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(30));
        root.setStyle("-fx-background-color: " + currentTheme.getBgColor() + ";");

        // Top section - Title and close button
        HBox topBar = createTopBar();
        root.setTop(topBar);

        // Center section - Stats content
        VBox centerContent = createCenterContent();
        root.setCenter(centerContent);

        Scene scene = new Scene(root, 1000, 700);
        stage.setScene(scene);
        stage.show();

        // Bring to front and request focus
        Platform.runLater(() -> {
            stage.toFront();
            stage.requestFocus();
        });
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(20);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(0, 0, 20, 0));

        Label titleLabel = new Label("📊 Statistics Dashboard");
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
        VBox content = new VBox(30);
        content.setAlignment(Pos.TOP_CENTER);

        try {
            // Personal bests section
            HBox personalBests = createPersonalBests();

            // Time range selector and graph
            VBox graphSection = createGraphSection();

            content.getChildren().addAll(personalBests, graphSection);

        } catch (SQLException e) {
            Label errorLabel = new Label("Error loading statistics: " + e.getMessage());
            errorLabel.setStyle("-fx-text-fill: " + currentTheme.getErrorColor() + ";");
            content.getChildren().add(errorLabel);
        }

        return content;
    }

    private HBox createPersonalBests() throws SQLException {
        HBox bestsBox = new HBox(40);
        bestsBox.setAlignment(Pos.CENTER);
        bestsBox.setPadding(new Insets(20));
        bestsBox.setStyle("-fx-background-color: " + currentTheme.getSubAltColor() + "; -fx-background-radius: 12;");

        DatabaseManager.UserStatistics stats = dbManager.getUserStatistics(userId);

        if (stats != null) {
            // Total tests
            VBox testsBox = createStatBox("Total Tests", String.format("%d", stats.totalTests), "🎯");

            // Best WPM
            VBox bestWpmBox = createStatBox("Best WPM", String.format("%.0f", stats.bestWpm), "🏆");

            // Average WPM
            VBox avgWpmBox = createStatBox("Avg WPM", String.format("%.0f", stats.avgWpm), "📈");

            // Best Accuracy
            VBox bestAccBox = createStatBox("Best Accuracy", String.format("%.0f%%", stats.bestAccuracy), "🎯");

            // Average Accuracy
            VBox avgAccBox = createStatBox("Avg Accuracy", String.format("%.0f%%", stats.avgAccuracy), "📊");

            bestsBox.getChildren().addAll(testsBox, bestWpmBox, avgWpmBox, bestAccBox, avgAccBox);
        } else {
            Label noDataLabel = new Label("No statistics available yet. Complete a test to see your stats!");
            noDataLabel.setStyle("-fx-text-fill: " + currentTheme.getSubColor() + ";");
            bestsBox.getChildren().add(noDataLabel);
        }

        return bestsBox;
    }

    private VBox createStatBox(String label, String value, String icon) {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(15));

        Label iconLabel = new Label(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", 32));

        Label valueLabel = new Label(value);
        valueLabel.setFont(new Font("Lexend Deca", 36));
        valueLabel.setStyle("-fx-text-fill: " + currentTheme.getCaretColor() + "; -fx-font-weight: bold;");

        Label labelLabel = new Label(label);
        labelLabel.setFont(new Font("Lexend Deca", 14));
        labelLabel.setStyle("-fx-text-fill: " + currentTheme.getSubColor() + ";");

        box.getChildren().addAll(iconLabel, valueLabel, labelLabel);
        return box;
    }

    private VBox createGraphSection() throws SQLException {
        VBox graphSection = new VBox(15);
        graphSection.setAlignment(Pos.CENTER);

        // Time range selector
        HBox rangeSelector = createRangeSelector(graphSection);

        // Initial graph - store reference to update it
        LineChart<Number, Number> initialChart = createProgressChart(10);
        graphSection.getChildren().addAll(rangeSelector, initialChart);

        return graphSection;
    }

    private HBox createRangeSelector(VBox graphSection) {
        HBox selector = new HBox(10);
        selector.setAlignment(Pos.CENTER);

        Label label = new Label("Show:");
        label.setStyle("-fx-text-fill: " + currentTheme.getMainColor() + ";");
        label.setFont(new Font("Lexend Deca", 14));

        ToggleGroup rangeGroup = new ToggleGroup();

        ToggleButton last10Btn = createRangeButton("Last 10", rangeGroup);
        ToggleButton last20Btn = createRangeButton("Last 20", rangeGroup);
        ToggleButton last50Btn = createRangeButton("Last 50", rangeGroup);
        ToggleButton allBtn = createRangeButton("All", rangeGroup);

        last10Btn.setSelected(true);

        // Update graph when selection changes
        rangeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String text = ((ToggleButton) newVal).getText();
                int limit = text.equals("All") ? 1000 : Integer.parseInt(text.split(" ")[1]);
                try {
                    // Remove old chart (index 1) and add new one
                    if (graphSection.getChildren().size() > 1) {
                        graphSection.getChildren().remove(1);
                    }
                    LineChart<Number, Number> newChart = createProgressChart(limit);
                    graphSection.getChildren().add(newChart);
                    System.out.println("Chart updated to show last " + limit + " tests");
                } catch (SQLException e) {
                    System.err.println("Error updating chart: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });

        selector.getChildren().addAll(label, last10Btn, last20Btn, last50Btn, allBtn);
        return selector;
    }

    private ToggleButton createRangeButton(String text, ToggleGroup group) {
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

    private LineChart<Number, Number> createProgressChart(int limit) throws SQLException {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Test Number");
        xAxis.setStyle("-fx-tick-label-fill: " + currentTheme.getSubColor() + ";");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("WPM / Accuracy %");
        yAxis.setStyle("-fx-tick-label-fill: " + currentTheme.getSubColor() + ";");

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Progress Over Time");
        chart.setStyle("-fx-background-color: " + currentTheme.getSubAltColor() + "; -fx-font-size: 14px;");
        chart.setPrefHeight(350);
        chart.setLegendVisible(true);

        // Get test history
        List<DatabaseManager.TestResultSummary> history = dbManager.getUserTestHistory(userId, limit);

        if (!history.isEmpty()) {
            XYChart.Series<Number, Number> wpmSeries = new XYChart.Series<>();
            wpmSeries.setName("WPM");

            XYChart.Series<Number, Number> accuracySeries = new XYChart.Series<>();
            accuracySeries.setName("Accuracy %");

            // Reverse to show oldest first
            for (int i = history.size() - 1; i >= 0; i--) {
                DatabaseManager.TestResultSummary result = history.get(i);
                int testNum = history.size() - i;
                wpmSeries.getData().add(new XYChart.Data<>(testNum, result.wpm));
                accuracySeries.getData().add(new XYChart.Data<>(testNum, result.accuracy));
            }

            chart.getData().addAll(wpmSeries, accuracySeries);

            // Style the lines
            Platform.runLater(() -> {
                chart.lookupAll(".chart-series-line").forEach(node -> {
                    if (node.getStyleClass().contains("series0")) {
                        node.setStyle("-fx-stroke: " + currentTheme.getCaretColor() + "; -fx-stroke-width: 3px;");
                    } else if (node.getStyleClass().contains("series1")) {
                        node.setStyle("-fx-stroke: " + currentTheme.getMainColor() + "; -fx-stroke-width: 3px;");
                    }
                });

                if (chart.lookup(".chart-plot-background") != null) {
                    chart.lookup(".chart-plot-background").setStyle("-fx-background-color: " + currentTheme.getBgColor() + ";");
                }
            });
        } else {
            Label noDataLabel = new Label("No test history available yet.");
            noDataLabel.setStyle("-fx-text-fill: " + currentTheme.getSubColor() + ";");
        }

        return chart;
    }
}