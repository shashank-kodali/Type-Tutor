package com.typetutor;

import javafx.application.Application;
import javafx.stage.Stage;

public class TypeTutor extends Application {

    private DatabaseManager dbManager;
    private ThemeManager themeManager;

    @Override
    public void start(Stage primaryStage) {
        // Initialize theme manager first
        themeManager = new ThemeManager();

        // Initialize MySQL database
        dbManager = new DatabaseManager();

        // Create the Model to hold the application's data and state
        TypeTutorModel model = new TypeTutorModel();

        // Create the View with theme manager
        TypeTutorView view = new TypeTutorView(primaryStage, themeManager);

        // Create the Controller with database support
        TypeTutorController controller = new TypeTutorController(model, view, dbManager);
    }

    @Override
    public void stop() {
        // Clean up database connection when application closes
        if (dbManager != null) {
            dbManager.closeConnection();
        }
        System.out.println("Application stopped - Database connection closed.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}