package com.typetutor;

import javafx.application.Application;
import javafx.stage.Stage;

public class TypeTutor extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. Create the Model to hold the application's data and state
        TypeTutorModel model = new TypeTutorModel();

        // 2. Create the View to manage the user interface
        TypeTutorView view = new TypeTutorView(primaryStage);

        // 3. Create the Controller to connect the Model and View
        TypeTutorController controller = new TypeTutorController(model, view);
    }

    public static void main(String[] args) {
        launch(args);
    }
}