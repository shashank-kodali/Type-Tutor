package com.typetutor;

import javafx.application.Application;
import javafx.stage.Stage;

public class TypeTutor extends Application {

    private DatabaseManager dbManager;  // NEW: Database manager instance

    @Override
    public void start(Stage primaryStage) {
        // NEW: Initialize database (choose "sqlite" or "mysql")
        dbManager = new DatabaseManager("mysql");  // Use SQLite for simplicity

        // Create the Model to hold the application's data and state
        TypeTutorModel model = new TypeTutorModel();

        // Create the View to manage the user interface
        TypeTutorView view = new TypeTutorView(primaryStage);

        // NEW: Create the Controller with database support
        TypeTutorController controller = new TypeTutorController(model, view, dbManager);
    }

    @Override
    public void stop() {
        // NEW: Clean up database connection when application closes
        if (dbManager != null) {
            dbManager.closeConnection();
        }
        System.out.println("Application stopped - Database connection closed.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}