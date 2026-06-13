package com.opsmap;

import com.opsmap.network.MapServer;
import com.opsmap.view.LoginPage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class Main extends Application {

    private static MapServer server;

    @Override
    public void start(Stage primaryStage) {
        showLauncher(primaryStage);
    }

    private void showLauncher(Stage stage) {
        stage.setTitle("OpsMap - راه‌اندازی");

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0f0c29, #302b63, #24243e);");

        VBox card = new VBox(25);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setMaxWidth(400);
        card.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.1);" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-color: rgba(255, 255, 255, 0.2);" +
                        "-fx-border-width: 1;"
        );
        card.setEffect(new DropShadow(30, Color.rgb(0, 0, 0, 0.5)));

        Label logo = new Label("🗺️");
        logo.setFont(Font.font(70));

        Label title = new Label("OpsMap");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 42));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("سامانه عملیاتی همکارانه");
        subtitle.setFont(Font.font("Arial", 16));
        subtitle.setTextFill(Color.LIGHTGRAY);

        Label version = new Label("نسخه 2.0 - پاییز 1404");
        version.setFont(Font.font(12));
        version.setTextFill(Color.GRAY);

        Button serverBtn = createLauncherButton("🖥️ راه‌اندازی سرور", "#9C27B0");
        serverBtn.setOnAction(e -> startServer(stage));

        Button clientBtn = createLauncherButton("👤 ورود به سامانه", "#4CAF50");
        clientBtn.setOnAction(e -> openLoginPage(stage));

        Button bothBtn = createLauncherButton("🚀 سرور + کلاینت", "#2196F3");
        bothBtn.setOnAction(e -> startBoth(stage));

        Label serverStatus = new Label("⚪ سرور غیرفعال");
        serverStatus.setTextFill(Color.GRAY);
        serverStatus.setId("serverStatus");

        Label info = new Label(
                "📋 راهنما:\n" +
                        "• سرور: اجرای سرور برای میزبانی\n" +
                        "• کلاینت: ورود به سامانه موجود\n" +
                        "• هر دو: اجرای سرور و کلاینت"
        );
        info.setTextFill(Color.LIGHTGRAY);
        info.setFont(Font.font(11));
        info.setStyle("-fx-background-color: rgba(0,0,0,0.2); -fx-padding: 10; -fx-background-radius: 5;");

        card.getChildren().addAll(
                logo, title, subtitle, version,
                new Region() {{ setMinHeight(10); }},
                serverBtn, clientBtn, bothBtn,
                serverStatus,
                info
        );

        root.getChildren().add(card);

        Scene scene = new Scene(root, 450, 600);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    private Button createLauncherButton(String text, String color) {
        Button btn = new Button(text);
        btn.setPrefWidth(280);
        btn.setPrefHeight(50);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        btn.setTextFill(Color.WHITE);
        btn.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;"
        );

        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: derive(" + color + ", 20%);" +
                        "-fx-background-radius: 10; -fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 10; -fx-cursor: hand;"
        ));

        return btn;
    }

    private void startServer(Stage stage) {
        if (server != null) {
            showAlert("سرور در حال اجراست!");
            return;
        }

        new Thread(() -> {
            server = new MapServer();
            Platform.runLater(() -> {
                Label status = (Label) stage.getScene().lookup("#serverStatus");
                if (status != null) {
                    status.setText("🟢 سرور فعال (پورت 5555)");
                    status.setTextFill(Color.LIGHTGREEN);
                }
            });
            server.start();
        }).start();

        showAlert("سرور راه‌اندازی شد!\nپورت: 5555");
    }

    private void openLoginPage(Stage stage) {
        stage.close();
        Stage loginStage = new Stage();
        new LoginPage().show(loginStage);
    }

    private void startBoth(Stage stage) {
        if (server == null) {
            new Thread(() -> {
                server = new MapServer();
                server.start();
            }).start();

            try { Thread.sleep(500); } catch (InterruptedException e) {}
        }

        openLoginPage(stage);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("اطلاعات");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
