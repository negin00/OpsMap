package com.opsmap.view;

import com.opsmap.model.User;
import com.opsmap.model.UserManager;
import javafx.animation.*;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class LoginPage {

    private UserManager userManager = UserManager.getInstance();
    private Stage primaryStage;
    private boolean isLoginMode = true;

    public void show(Stage stage) {
        this.primaryStage = stage;
        primaryStage.setTitle("OpsMap - سامانه عملیاتی همکارانه");

        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0f0c29, #302b63, #24243e);");

        addBackgroundEffects(root);

        VBox mainCard = createMainCard();
        root.getChildren().add(mainCard);

        Scene scene = new Scene(root, 500, 650);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        playEntryAnimation(mainCard);
    }

    private void addBackgroundEffects(StackPane root) {
        for (int i = 0; i < 5; i++) {
            Circle circle = new Circle(50 + i * 30);
            circle.setFill(Color.TRANSPARENT);
            circle.setStroke(Color.rgb(255, 255, 255, 0.1));
            circle.setStrokeWidth(1);

            TranslateTransition tt = new TranslateTransition(Duration.seconds(10 + i * 2), circle);
            tt.setFromX(-200 + i * 100);
            tt.setToX(200 - i * 50);
            tt.setFromY(-100 + i * 50);
            tt.setToY(100 - i * 30);
            tt.setCycleCount(Animation.INDEFINITE);
            tt.setAutoReverse(true);
            tt.play();

            root.getChildren().add(circle);
        }
    }

    private VBox createMainCard() {
        VBox card = new VBox(20);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setMaxWidth(380);
        card.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.1);" +
                        "-fx-background-radius: 20;" +
                        "-fx-border-radius: 20;" +
                        "-fx-border-color: rgba(255, 255, 255, 0.2);" +
                        "-fx-border-width: 1;"
        );
        card.setEffect(new DropShadow(30, Color.rgb(0, 0, 0, 0.5)));

        Label logo = new Label("🗺️");
        logo.setFont(Font.font(60));

        Label title = new Label("OpsMap");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 36));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("سامانه عملیاتی همکارانه");
        subtitle.setFont(Font.font("Arial", 14));
        subtitle.setTextFill(Color.LIGHTGRAY);

        VBox fieldsBox = new VBox(15);
        fieldsBox.setAlignment(Pos.CENTER);

        TextField usernameField = createStyledTextField("👤 نام کاربری");
        PasswordField passwordField = createStyledPasswordField("🔒 رمز عبور");
        PasswordField confirmPasswordField = createStyledPasswordField("🔒 تکرار رمز عبور");
        confirmPasswordField.setVisible(false);
        confirmPasswordField.setManaged(false);

        HBox roleBox = new HBox(10);
        roleBox.setAlignment(Pos.CENTER);
        roleBox.setVisible(false);
        roleBox.setManaged(false);

        Label roleLabel = new Label("نقش:");
        roleLabel.setTextFill(Color.WHITE);

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("فرمانده", "اپراتور", "ناظر");
        roleCombo.setValue("اپراتور");
        roleCombo.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.2);" +
                        "-fx-text-fill: white; -fx-font-size: 14px;"
        );

        roleBox.getChildren().addAll(roleLabel, roleCombo);

        fieldsBox.getChildren().addAll(usernameField, passwordField, confirmPasswordField, roleBox);

        Button mainButton = createStyledButton("ورود", "#4CAF50");
        Button switchButton = createStyledButton("ثبت‌نام کاربر جدید", "transparent");
        switchButton.setStyle(switchButton.getStyle() + "-fx-border-color: white; -fx-border-width: 1;");

        Label errorLabel = new Label();
        errorLabel.setTextFill(Color.rgb(255, 100, 100));
        errorLabel.setFont(Font.font(12));
        errorLabel.setVisible(false);

        Label successLabel = new Label();
        successLabel.setTextFill(Color.rgb(100, 255, 100));
        successLabel.setFont(Font.font(12));
        successLabel.setVisible(false);

        Label infoLabel = new Label("کاربران پیش‌فرض:\nadmin / admin123 (فرمانده)\noperator1 / 1234 (اپراتور)\nobserver1 / 1234 (ناظر)");
        infoLabel.setTextFill(Color.GRAY);
        infoLabel.setFont(Font.font(10));
        infoLabel.setTextAlignment(TextAlignment.CENTER);

        
        switchButton.setOnAction(e -> {
            isLoginMode = !isLoginMode;

            if (isLoginMode) {
                mainButton.setText("ورود");
                switchButton.setText("ثبت‌نام کاربر جدید");
                confirmPasswordField.setVisible(false);
                confirmPasswordField.setManaged(false);
                roleBox.setVisible(false);
                roleBox.setManaged(false);
            } else {
                mainButton.setText("ثبت‌نام");
                switchButton.setText("بازگشت به ورود");
                confirmPasswordField.setVisible(true);
                confirmPasswordField.setManaged(true);
                roleBox.setVisible(true);
                roleBox.setManaged(true);
            }

            errorLabel.setVisible(false);
            successLabel.setVisible(false);
        });

        mainButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();

            errorLabel.setVisible(false);
            successLabel.setVisible(false);

            if (isLoginMode) {
                if (username.isEmpty() || password.isEmpty()) {
                    showError(errorLabel, "لطفاً نام کاربری و رمز عبور را وارد کنید");
                    return;
                }

                User user = userManager.login(username, password);
                if (user != null) {
                    openMapPage(user);
                } else {
                    showError(errorLabel, "نام کاربری یا رمز عبور اشتباه است");
                    shakeNode(mainButton);
                }
            } else {
                String confirmPass = confirmPasswordField.getText();

                if (username.length() < 3) {
                    showError(errorLabel, "نام کاربری باید حداقل ۳ کاراکتر باشد");
                    return;
                }
                if (password.length() < 4) {
                    showError(errorLabel, "رمز عبور باید حداقل ۴ کاراکتر باشد");
                    return;
                }
                if (!password.equals(confirmPass)) {
                    showError(errorLabel, "رمز عبور و تکرار آن یکسان نیست");
                    return;
                }
                if (userManager.userExists(username)) {
                    showError(errorLabel, "این نام کاربری قبلاً ثبت شده است");
                    return;
                }

                User.Role role;
                String selectedRole = roleCombo.getValue();
                if (selectedRole.equals("فرمانده")) {
                    role = User.Role.COMMANDER;
                } else if (selectedRole.equals("اپراتور")) {
                    role = User.Role.OPERATOR;
                } else {
                    role = User.Role.OBSERVER;
                }


                if (userManager.register(username, password, role)) {
                    showSuccess(successLabel, "ثبت‌نام موفق! حالا می‌توانید وارد شوید");
                    isLoginMode = true;
                    mainButton.setText("ورود");
                    switchButton.setText("ثبت‌نام کاربر جدید");
                    confirmPasswordField.setVisible(false);
                    confirmPasswordField.setManaged(false);
                    roleBox.setVisible(false);
                    roleBox.setManaged(false);
                    passwordField.clear();
                    confirmPasswordField.clear();
                } else {
                    showError(errorLabel, "خطا در ثبت‌نام. لطفاً دوباره تلاش کنید");
                }
            }
        });

        card.getChildren().addAll(
                logo, title, subtitle,
                new Region() {{ setMinHeight(10); }},
                fieldsBox,
                errorLabel, successLabel,
                mainButton, switchButton,
                new Region() {{ setMinHeight(10); }},
                infoLabel
        );

        return card;
    }

    private TextField createStyledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setMaxWidth(300);
        field.setPrefHeight(45);
        field.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.1);" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: rgba(255, 255, 255, 0.5);" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-color: rgba(255, 255, 255, 0.3);" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 10;"
        );

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                field.setStyle(field.getStyle().replace(
                        "rgba(255, 255, 255, 0.3)", "rgba(100, 200, 255, 0.8)"));
            } else {
                field.setStyle(field.getStyle().replace(
                        "rgba(100, 200, 255, 0.8)", "rgba(255, 255, 255, 0.3)"));
            }
        });

        return field;
    }

    private PasswordField createStyledPasswordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.setMaxWidth(300);
        field.setPrefHeight(45);
        field.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.1);" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: rgba(255, 255, 255, 0.5);" +
                        "-fx-font-size: 14px;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-color: rgba(255, 255, 255, 0.3);" +
                        "-fx-border-width: 1;" +
                        "-fx-padding: 10;"
        );

        field.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                field.setStyle(field.getStyle().replace(
                        "rgba(255, 255, 255, 0.3)", "rgba(100, 200, 255, 0.8)"));
            } else {
                field.setStyle(field.getStyle().replace(
                        "rgba(100, 200, 255, 0.8)", "rgba(255, 255, 255, 0.3)"));
            }
        });

        return field;
    }

    private Button createStyledButton(String text, String color) {
        Button button = new Button(text);
        button.setMaxWidth(300);
        button.setPrefHeight(45);
        button.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        button.setTextFill(Color.WHITE);
        button.setStyle(
                "-fx-background-color: " + color + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-cursor: hand;"
        );

        button.setOnMouseEntered(e -> {
            button.setEffect(new Glow(0.3));
            ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
            st.setToX(1.02);
            st.setToY(1.02);
            st.play();
        });

        button.setOnMouseExited(e -> {
            button.setEffect(null);
            ScaleTransition st = new ScaleTransition(Duration.millis(100), button);
            st.setToX(1);
            st.setToY(1);
            st.play();
        });

        return button;
    }

    private void showError(Label label, String message) {
        label.setText("⚠️ " + message);
        label.setVisible(true);
        shakeNode(label);
    }

    private void showSuccess(Label label, String message) {
        label.setText("✅ " + message);
        label.setVisible(true);
    }

    private void shakeNode(javafx.scene.Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(0);
        tt.setByX(10);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.play();
    }

    private void playEntryAnimation(VBox card) {
        card.setOpacity(0);
        card.setTranslateY(30);

        FadeTransition ft = new FadeTransition(Duration.millis(500), card);
        ft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(500), card);
        tt.setToY(0);

        ft.play();
        tt.play();
    }

    private void openMapPage(User user) {
        primaryStage.close();
        Stage mapStage = new Stage();
        new MapPage(mapStage, user);
    }
}
