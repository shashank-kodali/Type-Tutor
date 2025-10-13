package com.typetutor;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseManager handles all database operations for Type Tutor
 * Supports both SQLite (embedded) and MySQL (server-based)
 */
public class DatabaseManager {

    private Connection connection;
    private final String dbType; // "sqlite" or "mysql"

    // SQLite Configuration
    private static final String SQLITE_URL = "jdbc:sqlite:typetutor.db";

    // MySQL Configuration (modify these for your setup)
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/typetutor_db";
    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASSWORD = "Shasi$2006";

    /**
     * Constructor - Initialize database connection
     * @param dbType "sqlite" or "mysql"
     */
    public DatabaseManager(String dbType) {
        this.dbType = dbType.toLowerCase();
        try {
            connectToDatabase();
            createTables();
            System.out.println("Database initialized successfully: " + dbType);
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Establish database connection
     */
    private void connectToDatabase() throws SQLException {
        if (dbType.equals("sqlite")) {
            // SQLite - No external server needed
            connection = DriverManager.getConnection(SQLITE_URL);
        } else if (dbType.equals("mysql")) {
            // MySQL - Requires MySQL server running
            connection = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
        } else {
            throw new SQLException("Unsupported database type: " + dbType);
        }
    }

    /**
     * Create necessary tables if they don't exist
     */
    private void createTables() throws SQLException {
        Statement stmt = connection.createStatement();

        // Users table
        String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                "user_id INTEGER PRIMARY KEY " + (dbType.equals("sqlite") ? "AUTOINCREMENT" : "AUTO_INCREMENT") + ", " +
                "username VARCHAR(50) UNIQUE NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        // Test results table
        String createResultsTable = "CREATE TABLE IF NOT EXISTS test_results (" +
                "result_id INTEGER PRIMARY KEY " + (dbType.equals("sqlite") ? "AUTOINCREMENT" : "AUTO_INCREMENT") + ", " +
                "user_id INTEGER, " +
                "test_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "difficulty VARCHAR(20), " +
                "duration_seconds INTEGER, " +
                "wpm DECIMAL(5,2), " +
                "raw_wpm DECIMAL(5,2), " +
                "accuracy DECIMAL(5,2), " +
                "consistency DECIMAL(5,2), " +
                "correct_chars INTEGER, " +
                "total_chars INTEGER, " +
                "missed_chars INTEGER, " +
                "extra_chars INTEGER, " +
                "FOREIGN KEY(user_id) REFERENCES users(user_id)" +
                ")";

        // WPM history table (for graph data)
        String createWpmHistoryTable = "CREATE TABLE IF NOT EXISTS wpm_history (" +
                "history_id INTEGER PRIMARY KEY " + (dbType.equals("sqlite") ? "AUTOINCREMENT" : "AUTO_INCREMENT") + ", " +
                "result_id INTEGER, " +
                "second INTEGER, " +
                "wpm DECIMAL(5,2), " +
                "raw_wpm DECIMAL(5,2), " +
                "FOREIGN KEY(result_id) REFERENCES test_results(result_id)" +
                ")";

        // Error history table
        String createErrorHistoryTable = "CREATE TABLE IF NOT EXISTS error_history (" +
                "error_id INTEGER PRIMARY KEY " + (dbType.equals("sqlite") ? "AUTOINCREMENT" : "AUTO_INCREMENT") + ", " +
                "result_id INTEGER, " +
                "second INTEGER, " +
                "error_count INTEGER, " +
                "FOREIGN KEY(result_id) REFERENCES test_results(result_id)" +
                ")";

        stmt.execute(createUsersTable);
        stmt.execute(createResultsTable);
        stmt.execute(createWpmHistoryTable);
        stmt.execute(createErrorHistoryTable);
        stmt.close();
    }

    /**
     * Add or get user by username
     * @return user_id
     */
    public int getOrCreateUser(String username) throws SQLException {
        // Check if user exists
        String checkSql = "SELECT user_id FROM users WHERE username = ?";
        PreparedStatement checkStmt = connection.prepareStatement(checkSql);
        checkStmt.setString(1, username);
        ResultSet rs = checkStmt.executeQuery();

        if (rs.next()) {
            int userId = rs.getInt("user_id");
            rs.close();
            checkStmt.close();
            return userId;
        }
        rs.close();
        checkStmt.close();

        // Create new user
        String insertSql = "INSERT INTO users (username) VALUES (?)";
        PreparedStatement insertStmt = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
        insertStmt.setString(1, username);
        insertStmt.executeUpdate();

        ResultSet generatedKeys = insertStmt.getGeneratedKeys();
        int userId = -1;
        if (generatedKeys.next()) {
            userId = generatedKeys.getInt(1);
        }
        generatedKeys.close();
        insertStmt.close();

        return userId;
    }

    /**
     * Save a complete test result to database
     */
    public int saveTestResult(int userId, TypeTutorModel model) throws SQLException {
        double timeInMinutes = model.getSelectedTime() / 60.0;
        double wpm = (model.getCumulativeCorrectChars() / 5.0) / timeInMinutes;
        double rawWpm = (model.getCumulativeTotalChars() / 5.0) / timeInMinutes;
        double accuracy = model.getCumulativeTotalChars() > 0 ?
                (model.getCumulativeCorrectChars() * 100.0 / model.getCumulativeTotalChars()) : 0;
        double consistency = model.calculateConsistency();

        String sql = "INSERT INTO test_results " +
                "(user_id, difficulty, duration_seconds, wpm, raw_wpm, accuracy, consistency, " +
                "correct_chars, total_chars, missed_chars, extra_chars) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        stmt.setInt(1, userId);
        stmt.setString(2, model.getCurrentDifficulty().name());
        stmt.setInt(3, model.getSelectedTime());
        stmt.setDouble(4, wpm);
        stmt.setDouble(5, rawWpm);
        stmt.setDouble(6, accuracy);
        stmt.setDouble(7, consistency);
        stmt.setInt(8, model.getCumulativeCorrectChars());
        stmt.setInt(9, model.getCumulativeTotalChars());
        stmt.setInt(10, model.getCumulativeMissedChars());
        stmt.setInt(11, model.getCumulativeExtraChars());

        stmt.executeUpdate();

        ResultSet generatedKeys = stmt.getGeneratedKeys();
        int resultId = -1;
        if (generatedKeys.next()) {
            resultId = generatedKeys.getInt(1);
        }
        generatedKeys.close();
        stmt.close();

        // Save WPM history
        saveWpmHistory(resultId, model.getWpmHistory());

        // Save error history
        saveErrorHistory(resultId, model.getErrorHistory());

        return resultId;
    }

    /**
     * Save WPM history for a test result
     */
    private void saveWpmHistory(int resultId, List<TypeTutorModel.WPMSnapshot> history) throws SQLException {
        String sql = "INSERT INTO wpm_history (result_id, second, wpm, raw_wpm) VALUES (?, ?, ?, ?)";
        PreparedStatement stmt = connection.prepareStatement(sql);

        for (TypeTutorModel.WPMSnapshot snapshot : history) {
            stmt.setInt(1, resultId);
            stmt.setInt(2, snapshot.second);
            stmt.setDouble(3, snapshot.wpm);
            stmt.setDouble(4, snapshot.rawWpm);
            stmt.addBatch();
        }

        stmt.executeBatch();
        stmt.close();
    }

    /**
     * Save error history for a test result
     */
    private void saveErrorHistory(int resultId, List<TypeTutorModel.ErrorSnapshot> history) throws SQLException {
        String sql = "INSERT INTO error_history (result_id, second, error_count) VALUES (?, ?, ?)";
        PreparedStatement stmt = connection.prepareStatement(sql);

        for (TypeTutorModel.ErrorSnapshot snapshot : history) {
            stmt.setInt(1, resultId);
            stmt.setInt(2, snapshot.second);
            stmt.setInt(3, snapshot.errors);
            stmt.addBatch();
        }

        stmt.executeBatch();
        stmt.close();
    }

    /**
     * Get user's test history
     */
    public List<TestResultSummary> getUserTestHistory(int userId, int limit) throws SQLException {
        List<TestResultSummary> results = new ArrayList<>();

        String sql = "SELECT result_id, test_date, difficulty, duration_seconds, wpm, " +
                "raw_wpm, accuracy, consistency FROM test_results " +
                "WHERE user_id = ? ORDER BY test_date DESC LIMIT ?";

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, userId);
        stmt.setInt(2, limit);

        ResultSet rs = stmt.executeQuery();
        while (rs.next()) {
            TestResultSummary summary = new TestResultSummary(
                    rs.getInt("result_id"),
                    rs.getTimestamp("test_date").toLocalDateTime(),
                    rs.getString("difficulty"),
                    rs.getInt("duration_seconds"),
                    rs.getDouble("wpm"),
                    rs.getDouble("raw_wpm"),
                    rs.getDouble("accuracy"),
                    rs.getDouble("consistency")
            );
            results.add(summary);
        }

        rs.close();
        stmt.close();
        return results;
    }

    /**
     * Get user statistics
     */
    public UserStatistics getUserStatistics(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) as total_tests, " +
                "AVG(wpm) as avg_wpm, MAX(wpm) as best_wpm, " +
                "AVG(accuracy) as avg_accuracy, MAX(accuracy) as best_accuracy " +
                "FROM test_results WHERE user_id = ?";

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, userId);

        ResultSet rs = stmt.executeQuery();
        UserStatistics stats = null;

        if (rs.next()) {
            stats = new UserStatistics(
                    rs.getInt("total_tests"),
                    rs.getDouble("avg_wpm"),
                    rs.getDouble("best_wpm"),
                    rs.getDouble("avg_accuracy"),
                    rs.getDouble("best_accuracy")
            );
        }

        rs.close();
        stmt.close();
        return stats;
    }

    /**
     * Get leaderboard (top performers)
     */
    public List<LeaderboardEntry> getLeaderboard(int limit) throws SQLException {
        List<LeaderboardEntry> leaderboard = new ArrayList<>();

        String sql = "SELECT u.username, MAX(r.wpm) as best_wpm, MAX(r.accuracy) as best_accuracy " +
                "FROM users u JOIN test_results r ON u.user_id = r.user_id " +
                "GROUP BY u.user_id, u.username " +
                "ORDER BY best_wpm DESC LIMIT ?";

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, limit);

        ResultSet rs = stmt.executeQuery();
        int rank = 1;
        while (rs.next()) {
            LeaderboardEntry entry = new LeaderboardEntry(
                    rank++,
                    rs.getString("username"),
                    rs.getDouble("best_wpm"),
                    rs.getDouble("best_accuracy")
            );
            leaderboard.add(entry);
        }

        rs.close();
        stmt.close();
        return leaderboard;
    }

    /**
     * Close database connection
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database: " + e.getMessage());
        }
    }

    // ===== Data Classes =====

    public static class TestResultSummary {
        public final int resultId;
        public final LocalDateTime testDate;
        public final String difficulty;
        public final int durationSeconds;
        public final double wpm;
        public final double rawWpm;
        public final double accuracy;
        public final double consistency;

        public TestResultSummary(int resultId, LocalDateTime testDate, String difficulty,
                                 int durationSeconds, double wpm, double rawWpm,
                                 double accuracy, double consistency) {
            this.resultId = resultId;
            this.testDate = testDate;
            this.difficulty = difficulty;
            this.durationSeconds = durationSeconds;
            this.wpm = wpm;
            this.rawWpm = rawWpm;
            this.accuracy = accuracy;
            this.consistency = consistency;
        }

        @Override
        public String toString() {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            return String.format("[%s] %s (%ds): WPM=%.1f, Acc=%.1f%%",
                    testDate.format(formatter), difficulty, durationSeconds, wpm, accuracy);
        }
    }

    public static class UserStatistics {
        public final int totalTests;
        public final double avgWpm;
        public final double bestWpm;
        public final double avgAccuracy;
        public final double bestAccuracy;

        public UserStatistics(int totalTests, double avgWpm, double bestWpm,
                              double avgAccuracy, double bestAccuracy) {
            this.totalTests = totalTests;
            this.avgWpm = avgWpm;
            this.bestWpm = bestWpm;
            this.avgAccuracy = avgAccuracy;
            this.bestAccuracy = bestAccuracy;
        }

        @Override
        public String toString() {
            return String.format("Tests: %d | Avg WPM: %.1f | Best WPM: %.1f | Avg Acc: %.1f%% | Best Acc: %.1f%%",
                    totalTests, avgWpm, bestWpm, avgAccuracy, bestAccuracy);
        }
    }

    public static class LeaderboardEntry {
        public final int rank;
        public final String username;
        public final double bestWpm;
        public final double bestAccuracy;

        public LeaderboardEntry(int rank, String username, double bestWpm, double bestAccuracy) {
            this.rank = rank;
            this.username = username;
            this.bestWpm = bestWpm;
            this.bestAccuracy = bestAccuracy;
        }

        @Override
        public String toString() {
            return String.format("#%d: %s - WPM: %.1f, Acc: %.1f%%",
                    rank, username, bestWpm, bestAccuracy);
        }
    }
}