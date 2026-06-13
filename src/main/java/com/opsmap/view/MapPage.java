package com.opsmap.view;

import com.opsmap.model.*;
import com.opsmap.network.MapClient;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MapPage {

    private ListView<String> onlineUsersList;
    private final Stage stage;
    private MapClient client;
    private final User currentUser;

    private Canvas canvas;
    private GraphicsContext gc;
    private List<MapShape> shapes = new CopyOnWriteArrayList<>();

    // ابزارها
    private String currentTool = "LINE";
    private String currentColor = "#FF0000";
    private double strokeWidth = 2.0;
    private boolean isDrawing = false;
    private double startX, startY;

    // UI Elements
    private Label statusLabel;
    private Label shapesCountLabel;
    private Label usersLabel;
    private VBox chatBox;
    private TextField chatInput;
    private ScrollPane chatScrollPane;

    // برای دنبال کردن موس کاربران
    private Map<String, double[]> userCursors = new ConcurrentHashMap<>();
    private Map<String, Label> cursorLabels = new HashMap<>();
    private Pane canvasPane;
    private boolean receivingHistory = false;  // برای تشخیص تاریخچه از real-time


    // Constructor با 2 آرگومان (برای سازگاری با LoginPage)
    public MapPage(Stage stage, User user) {
        this.stage = stage;
        this.currentUser = user;
        this.client = null; // بعداً می‌توان تنظیم کرد
        show();
        initializeClient();

    }

    // Constructor با 3 آرگومان
    public MapPage(Stage stage, MapClient client, User user) {
        this.stage = stage;
        this.client = client;
        this.currentUser = user;
        show();
    }

    private void initializeClient() {
        try {
            this.client = new MapClient();
            // ⭐ اول handler تنظیم شود
            client.setOnMessageReceived(this::handleServerMessage);
            client.connect("localhost", 5555);

            // ارسال اطلاعات کاربر به سرور
            if (client.isConnected()) {
                client.sendMessage("JOIN:" + currentUser.getUsername() + ":" + currentUser.getRole());
            }
        } catch (Exception e) {
            System.err.println("⚠️ اتصال به سرور ناموفق: " + e.getMessage());
            this.client = null;
        }
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1a1a2e;");

       /* // تنظیم handler برای دریافت پیام
        if (client != null) {
            try {
                client.setOnMessageReceived(this::handleServerMessage);
            } catch (Exception e) {
                System.err.println("خطا در تنظیم message handler: " + e.getMessage());
            }
        }
*/
        root.setTop(createTopBar());
        root.setLeft(createToolPanel());

        canvasPane = new Pane();
        canvas = new Canvas(900, 600);
        gc = canvas.getGraphicsContext2D();
        clearCanvas();
        setupCanvasEvents();
        canvasPane.getChildren().add(canvas);

        ScrollPane canvasScroll = new ScrollPane(canvasPane);
        canvasScroll.setStyle("-fx-background: #2d2d44;");
        canvasScroll.setPannable(true);
        root.setCenter(canvasScroll);

        root.setRight(createChatPanel());
        root.setBottom(createStatusBar());

        Scene scene = new Scene(root, 1280, 720);
        stage.setScene(scene);
        stage.setTitle("OpsMap - " + currentUser.getUsername() + " (" + getRolePersian() + ")");
        stage.setMaximized(true);
        stage.show();

        if (client != null && client.isConnected()) {
            updateStatus("✅ متصل به سرور", Color.LIGHTGREEN);
        } else {
            updateStatus("⚠️ حالت آفلاین", Color.ORANGE);
        }
    }

    // متد کمکی برای نقش فارسی
    private String getRolePersian() {
        try {
            return currentUser.getRolePersian();
        } catch (Exception e) {
            // اگر متد وجود نداشت
            String role = currentUser.getRole().toString();
            switch (role) {
                case "COMMANDER": return "فرمانده";
                case "OFFICER": return "افسر";
                case "VIEWER": return "ناظر";
                default: return role;
            }
        }
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(15);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 20, 10, 20));
        topBar.setStyle("-fx-background-color: #16213e;");

        Label logo = new Label("🗺️ OpsMap");
        logo.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        logo.setTextFill(Color.WHITE);

        Label userLabel = new Label("👤 " + currentUser.getUsername());
        userLabel.setTextFill(Color.LIGHTBLUE);
        userLabel.setFont(Font.font(14));

        Label roleLabel = new Label("🎖️ " + getRolePersian());
        roleLabel.setTextFill(Color.GOLD);
        roleLabel.setFont(Font.font(14));

        usersLabel = new Label("👥 کاربران: 1");
        usersLabel.setTextFill(Color.LIGHTGREEN);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button saveBtn = new Button("💾 ذخیره");
        saveBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        saveBtn.setOnAction(e -> saveMap());

        Button loadBtn = new Button("📂 بارگذاری");
        loadBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        loadBtn.setOnAction(e -> loadMap());

        Button logoutBtn = new Button("🚪 خروج");
        logoutBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        logoutBtn.setOnAction(e -> logout());

        topBar.getChildren().addAll(logo, userLabel, roleLabel, usersLabel, spacer, saveBtn, loadBtn, logoutBtn);
        return topBar;
    }

    private VBox createToolPanel() {
        VBox toolPanel = new VBox(10);
        toolPanel.setPadding(new Insets(15));
        toolPanel.setStyle("-fx-background-color: #1f1f3d; -fx-min-width: 180;");

        Label toolsTitle = new Label("🛠️ ابزارها");
        toolsTitle.setTextFill(Color.WHITE);
        toolsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        ToggleGroup toolGroup = new ToggleGroup();

        RadioButton lineBtn = createToolRadio("📏 خط", "LINE", toolGroup);
        RadioButton rectBtn = createToolRadio("⬜ مستطیل", "RECT", toolGroup);
        RadioButton circleBtn = createToolRadio("⭕ دایره", "CIRCLE", toolGroup);
        RadioButton textBtn = createToolRadio("📝 متن", "TEXT", toolGroup);

        lineBtn.setSelected(true);

        Label colorLabel = new Label("🎨 رنگ:");
        colorLabel.setTextFill(Color.WHITE);

        ColorPicker colorPicker = new ColorPicker(Color.RED);
        colorPicker.setOnAction(e -> {
            Color c = colorPicker.getValue();
            currentColor = String.format("#%02X%02X%02X",
                    (int) (c.getRed() * 255),
                    (int) (c.getGreen() * 255),
                    (int) (c.getBlue() * 255));
        });

        Label widthLabel = new Label("📐 ضخامت:");
        widthLabel.setTextFill(Color.WHITE);

        Slider widthSlider = new Slider(1, 10, 2);
        widthSlider.setShowTickLabels(true);
        widthSlider.valueProperty().addListener((obs, old, val) -> strokeWidth = val.doubleValue());

        Separator sep1 = new Separator();

        Label actionsTitle = new Label("⚡ عملیات");
        actionsTitle.setTextFill(Color.WHITE);
        actionsTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        Button undoBtn = new Button("↩️ حذف آخرین");
        undoBtn.setMaxWidth(Double.MAX_VALUE);
        undoBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white;");
        undoBtn.setOnAction(e -> undoLastShape());

        Button clearMyBtn = new Button("🧹 پاک کردن شکل‌های من");
        clearMyBtn.setMaxWidth(Double.MAX_VALUE);
        clearMyBtn.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white;");
        clearMyBtn.setOnAction(e -> clearMyShapes());

        Button clearAllBtn = new Button("🗑️ پاک کردن همه");
        clearAllBtn.setMaxWidth(Double.MAX_VALUE);
        clearAllBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;");

        // فقط فرمانده می‌تواند همه را پاک کند
        if (!canDeleteAll()) {
            clearAllBtn.setDisable(true);
            clearAllBtn.setTooltip(new Tooltip("فقط فرمانده می‌تواند نقشه را پاک کند"));
        }
        clearAllBtn.setOnAction(e -> clearAllShapes());

        toolPanel.getChildren().addAll(
                toolsTitle, lineBtn, rectBtn, circleBtn, textBtn,
                new Separator(),
                colorLabel, colorPicker,
                widthLabel, widthSlider,
                sep1,
                actionsTitle, undoBtn, clearMyBtn, clearAllBtn
        );

        return toolPanel;
    }

    private RadioButton createToolRadio(String text, String tool, ToggleGroup group) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(group);
        rb.setTextFill(Color.LIGHTGRAY);
        rb.setOnAction(e -> currentTool = tool);
        return rb;
    }

    private VBox createChatPanel() {
        VBox chatPanel = new VBox(10);
        chatPanel.setPadding(new Insets(10));
        chatPanel.setStyle("-fx-background-color: #1f1f3d; -fx-min-width: 280; -fx-max-width: 280;");

        // لیست کاربران آنلاین
        Label usersTitle = new Label("👥 کاربران آنلاین");
        usersTitle.setTextFill(Color.WHITE);
        usersTitle.setFont(Font.font("Arial", FontWeight.BOLD, 12));

        onlineUsersList = new ListView<>();
        onlineUsersList.setPrefHeight(120);
        onlineUsersList.setStyle(
                "-fx-background-color: #2d2d44;" +
                        "-fx-control-inner-background: #2d2d44;" +
                        "-fx-border-color: #444;"
        );
// اضافه کردن خودمان به لیست
        onlineUsersList.getItems().add("👤 " + currentUser.getUsername() + " (من)");

        Separator usersSep = new Separator();
        usersSep.setStyle("-fx-background-color: #444;");


        Label chatTitle = new Label("💬 چت گروهی");
        chatTitle.setTextFill(Color.WHITE);
        chatTitle.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        chatBox = new VBox(5);
        chatBox.setStyle("-fx-background-color: #2d2d44; -fx-padding: 10;");

        chatScrollPane = new ScrollPane(chatBox);
        chatScrollPane.setFitToWidth(true);
        chatScrollPane.setStyle("-fx-background: #2d2d44; -fx-border-color: #444;");
        chatScrollPane.setPrefHeight(400);
        VBox.setVgrow(chatScrollPane, Priority.ALWAYS);

        HBox inputBox = new HBox(5);
        chatInput = new TextField();
        chatInput.setPromptText("پیام...");
        chatInput.setStyle("-fx-background-color: #3d3d5c; -fx-text-fill: white; -fx-prompt-text-fill: gray;");
        HBox.setHgrow(chatInput, Priority.ALWAYS);

        Button sendBtn = new Button("📤");
        sendBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");

        chatInput.setOnAction(e -> sendChatMessage());
        sendBtn.setOnAction(e -> sendChatMessage());

        inputBox.getChildren().addAll(chatInput, sendBtn);

        addChatMessage("سیستم", "خوش آمدید " + currentUser.getUsername() + "!", "#00FF00");

        chatPanel.getChildren().addAll(usersTitle, onlineUsersList, usersSep, chatTitle, chatScrollPane, inputBox);
        return chatPanel;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(20);
        statusBar.setPadding(new Insets(8, 15, 8, 15));
        statusBar.setStyle("-fx-background-color: #0f0f23;");
        statusBar.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("✅ آماده");
        statusLabel.setTextFill(Color.LIGHTGREEN);

        shapesCountLabel = new Label("📊 شکل‌ها: 0");
        shapesCountLabel.setTextFill(Color.LIGHTBLUE);

        Label coordsLabel = new Label("📍 موقعیت: -");
        coordsLabel.setTextFill(Color.GRAY);

        canvas.setOnMouseMoved(e -> {
            coordsLabel.setText(String.format("📍 موقعیت: %.0f, %.0f", e.getX(), e.getY()));

            // ارسال موقعیت موس به سرور
            if (client != null && client.isConnected()) {
                client.sendMessage("CURSOR:" + currentUser.getUsername() + ":" + (int) e.getX() + ":" + (int) e.getY());
            }
        });

        statusBar.getChildren().addAll(statusLabel, shapesCountLabel, coordsLabel);
        return statusBar;
    }

    private void setupCanvasEvents() {
        if (!canDraw()) {
            return;
        }

        canvas.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                startX = e.getX();
                startY = e.getY();
                isDrawing = true;

                if (currentTool.equals("TEXT")) {
                    showTextDialog(startX, startY);
                    isDrawing = false;
                }
            }
        });

        canvas.setOnMouseDragged(e -> {
            if (isDrawing && !currentTool.equals("TEXT")) {
                redrawCanvas();
                drawPreview(e.getX(), e.getY());
            }
        });

        canvas.setOnMouseReleased(e -> {
            if (isDrawing && !currentTool.equals("TEXT")) {
                isDrawing = false;
                createShape(e.getX(), e.getY());
            }
        });
    }

    // پیش‌نمایش با خط‌چین
    private void drawPreview(double endX, double endY) {
        gc.setStroke(Color.web(currentColor).deriveColor(0, 1, 1, 0.5));
        gc.setLineWidth(strokeWidth);
        gc.setLineDashes(5, 5); // خط‌چین

        switch (currentTool) {
            case "LINE":
                gc.strokeLine(startX, startY, endX, endY);
                break;
            case "RECT":
                double rx = Math.min(startX, endX);
                double ry = Math.min(startY, endY);
                double rw = Math.abs(endX - startX);
                double rh = Math.abs(endY - startY);
                gc.strokeRect(rx, ry, rw, rh);
                break;
            case "CIRCLE":
                double cx = (startX + endX) / 2;
                double cy = (startY + endY) / 2;
                double radius = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2)) / 2;
                gc.strokeOval(cx - radius, cy - radius, radius * 2, radius * 2);
                break;
        }

        gc.setLineDashes(0); // برگشت به حالت عادی
    }

    private void createShape(double endX, double endY) {
        MapShape shape = null;
        String owner = currentUser.getUsername();

        switch (currentTool) {
            case "LINE":
                shape = new LineShape(owner, currentColor, strokeWidth, startX, startY, endX, endY);
                break;
            case "RECT":
                double rx = Math.min(startX, endX);
                double ry = Math.min(startY, endY);
                double rw = Math.abs(endX - startX);
                double rh = Math.abs(endY - startY);
                shape = new RectangleShape(owner, currentColor, strokeWidth, rx, ry, rw, rh);
                break;
            case "CIRCLE":
                double cx = (startX + endX) / 2;
                double cy = (startY + endY) / 2;
                double radius = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2)) / 2;
                shape = new CircleShape(owner, currentColor, strokeWidth, cx, cy, radius);
                break;
        }

        if (shape != null) {
            shapes.add(shape);
            redrawCanvas();
            updateShapesCount();

            if (client != null && client.isConnected()) {
                client.sendMessage(shape.toNetworkFormat());
            }

            updateStatus("✏️ شکل ایجاد شد", Color.LIGHTGREEN);
        }
    }

    private void showTextDialog(double x, double y) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("متن جدید");
        dialog.setHeaderText("متن را وارد کنید:");
        dialog.setContentText("متن:");

        dialog.showAndWait().ifPresent(text -> {
            if (!text.trim().isEmpty()) {
                TextShape shape = new TextShape(currentUser.getUsername(), currentColor, strokeWidth, x, y, text);
                shapes.add(shape);
                redrawCanvas();
                updateShapesCount();

                if (client != null && client.isConnected()) {
                    client.sendMessage(shape.toNetworkFormat());
                }
            }
        });
    }

    private void clearCanvas() {
        gc.setFill(Color.web("#2d2d44"));
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        drawGrid();
    }

    private void drawGrid() {
        gc.setStroke(Color.web("#3d3d5c"));
        gc.setLineWidth(0.5);
        gc.setLineDashes(0);

        for (int x = 0; x < canvas.getWidth(); x += 50) {
            gc.strokeLine(x, 0, x, canvas.getHeight());
        }
        for (int y = 0; y < canvas.getHeight(); y += 50) {
            gc.strokeLine(0, y, canvas.getWidth(), y);
        }
    }

    private void redrawCanvas() {
        clearCanvas();
        gc.setLineDashes(0);
        for (MapShape shape : shapes) {
            drawShape(shape);
        }
    }

    private void drawShape(MapShape shape) {
        gc.setStroke(Color.web(shape.getColor()));
        gc.setLineWidth(shape.getStrokeWidth());
        gc.setLineDashes(0);

        if (shape instanceof LineShape) {
            LineShape line = (LineShape) shape;
            gc.strokeLine(line.getStartX(), line.getStartY(), line.getEndX(), line.getEndY());

        } else if (shape instanceof RectangleShape) {
            RectangleShape rect = (RectangleShape) shape;
            gc.strokeRect(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());

        } else if (shape instanceof CircleShape) {
            CircleShape circle = (CircleShape) shape;
            double r = circle.getRadius();
            gc.strokeOval(circle.getCenterX() - r, circle.getCenterY() - r, r * 2, r * 2);

        } else if (shape instanceof TextShape) {
            TextShape text = (TextShape) shape;
            gc.setFill(Color.web(shape.getColor()));
            double fontSize = shape.getStrokeWidth() > 0 ? shape.getStrokeWidth() * 5 : 14;
            gc.setFont(Font.font(fontSize));
            gc.fillText(text.getText(), text.getX(), text.getY());
        }
    }

    // ==================== متدهای کمکی برای دسترسی ====================

    private boolean canDraw() {
        try {
            return currentUser.canDraw();
        } catch (Exception e) {
            // اگر متد وجود نداشت، بر اساس نقش تصمیم بگیر
            String role = currentUser.getRole().toString();
            return role.equals("COMMANDER") || role.equals("OFFICER");
        }
    }

    private boolean canDeleteOwn() {
        try {
            return currentUser.canDeleteOwn();
        } catch (Exception e) {
            String role = currentUser.getRole().toString();
            return role.equals("COMMANDER") || role.equals("OFFICER");
        }
    }

    private boolean canDeleteAll() {
        try {
            return currentUser.canDeleteAll();
        } catch (Exception e) {
            String role = currentUser.getRole().toString();
            return role.equals("COMMANDER");
        }
    }

    // ==================== عملیات ====================

    private void undoLastShape() {
        if (!canDeleteOwn()) {
            showAlert("خطا", "شما دسترسی حذف ندارید!");
            return;
        }

        for (int i = shapes.size() - 1; i >= 0; i--) {
            if (shapes.get(i).getOwner().equals(currentUser.getUsername())) {
                MapShape removed = shapes.remove(i);
                redrawCanvas();
                updateShapesCount();

                if (client != null && client.isConnected()) {
                    client.sendMessage("DELETE_SHAPE:" + removed.getId());
                }
                updateStatus("↩️ شکل حذف شد", Color.ORANGE);
                return;
            }
        }
        updateStatus("⚠️ شکلی برای حذف نیست", Color.YELLOW);
    }

    private void clearMyShapes() {
        if (!canDeleteOwn()) {
            showAlert("خطا", "شما دسترسی حذف ندارید!");
            return;
        }

        // ارسال به سرور
        if (client != null && client.isConnected()) {
            try {
                client.deleteMyShapes();
            } catch (Exception e) {
                client.sendMessage("DELETE_USER_SHAPES:" + currentUser.getUsername());
            }
        }

        shapes.removeIf(s -> s.getOwner().equals(currentUser.getUsername()));
        redrawCanvas();
        updateShapesCount();
        updateStatus("🧹 شکل‌های شما پاک شد", Color.ORANGE);
    }

    private void clearAllShapes() {
        if (!canDeleteAll()) {
            showAlert("خطا", "فقط فرمانده می‌تواند نقشه را پاک کند!");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("تأیید پاک کردن");
        confirm.setHeaderText("آیا مطمئن هستید؟");
        confirm.setContentText("تمام شکل‌های نقشه پاک خواهند شد!");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            shapes.clear();
            redrawCanvas();
            updateShapesCount();

            if (client != null && client.isConnected()) {
                try {
                    client.clearAll();
                } catch (Exception e) {
                    client.sendMessage("CLEAR_ALL");
                }
            }

            addChatMessage("سیستم", "نقشه توسط " + currentUser.getUsername() + " پاک شد", "#FF9800");
            updateStatus("🗑️ نقشه پاک شد", Color.ORANGE);
        }
    }

    private void sendChatMessage() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty()) {
            // نمایش پیام خودمان فوری
            addChatMessage(currentUser.getUsername(), message, "#AAAAFF");

            // ارسال به سرور
            if (client != null && client.isConnected()) {
                try {
                    client.sendChat(message);
                } catch (Exception e) {
                    client.sendMessage("CHAT:" + currentUser.getUsername() + ":" + message);
                }
            }
            chatInput.clear();
        }
    }

    private void addChatMessage(String sender, String message, String color) {
        Platform.runLater(() -> {
            Label msgLabel = new Label(sender + ": " + message);
            msgLabel.setTextFill(Color.web(color));
            msgLabel.setWrapText(true);
            msgLabel.setMaxWidth(250);
            chatBox.getChildren().add(msgLabel);
            chatScrollPane.setVvalue(1.0);
        });
    }

    private void handleServerMessage(String message) {
        Platform.runLater(() -> {
            System.out.println("🔔 پیام دریافتی: " + message);

            try {
                // ===== شروع تاریخچه =====
                if (message.equals("HISTORY_START")) {
                    receivingHistory = true;
                    System.out.println("📜 شروع دریافت تاریخچه...");
                }

                // ===== پایان تاریخچه =====
                else if (message.equals("HISTORY_END")) {
                    receivingHistory = false;
                    redrawCanvas();
                    updateShapesCount();
                    System.out.println("📜 پایان تاریخچه - " + shapes.size() + " شکل دریافت شد");
                }

                // ===== چت =====
                else if (message.startsWith("CHAT:")) {
                    String[] parts = message.substring(5).split(":", 2);
                    if (parts.length >= 2) {
                        String sender = parts[0];
                        String chatMsg = parts[1];
                        if (!sender.equals(currentUser.getUsername())) {
                            addChatMessage(sender, chatMsg, "#FFFFFF");
                        }
                    }
                }

                // ===== موقعیت موس =====
                else if (message.startsWith("CURSOR:")) {
                    String[] parts = message.substring(7).split(":");
                    if (parts.length >= 3) {
                        String username = parts[0];
                        if (!username.equals(currentUser.getUsername())) {
                            try {
                                double x = Double.parseDouble(parts[1]);
                                double y = Double.parseDouble(parts[2]);
                                updateCursorDisplay(username, x, y);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }

                // ===== دریافت شکل =====
                else if (message.startsWith("LINE:") || message.startsWith("RECT:") ||
                        message.startsWith("CIRCLE:") || message.startsWith("TEXT:")) {
                    MapShape shape = MapShape.fromNetworkFormat(message);
                    if (shape != null) {
                        System.out.println("📥 شکل دریافت شد - ID: " + shape.getId() + " | Owner: " + shape.getOwner());

                        // اگر تاریخچه است → همه شکل‌ها را بگیر
                        // اگر real-time است → فقط شکل‌های دیگران
                        boolean shouldAdd = receivingHistory || !shape.getOwner().equals(currentUser.getUsername());

                        if (shouldAdd) {
                            boolean exists = shapes.stream().anyMatch(s -> s.getId().equals(shape.getId()));
                            if (!exists) {
                                shapes.add(shape);
                                if (!receivingHistory) {
                                    redrawCanvas();
                                    updateShapesCount();
                                }
                                System.out.println("✅ شکل اضافه شد");
                            }
                        }
                    }
                }

                // ===== حذف یک شکل =====
                else if (message.startsWith("DELETE_SHAPE:")) {
                    String shapeId = message.substring(13);
                    System.out.println("🗑️ درخواست حذف شکل با ID: " + shapeId);

                    boolean removed = shapes.removeIf(s -> s.getId().equals(shapeId));
                    if (removed) {
                        System.out.println("✅ شکل حذف شد");
                        redrawCanvas();
                        updateShapesCount();
                    }
                }

                // ===== حذف شکل‌های یک کاربر =====
                else if (message.startsWith("DELETE_USER_SHAPES:")) {
                    String username = message.substring(19);
                    shapes.removeIf(s -> s.getOwner().equals(username));
                    redrawCanvas();
                    updateShapesCount();
                }

                // ===== پاک کردن همه =====
                else if (message.equals("CLEAR_ALL")) {
                    shapes.clear();
                    redrawCanvas();
                    updateShapesCount();
                    addChatMessage("سیستم", "نقشه پاک شد", "#FF9800");
                }

                // ===== کاربر جدید =====
                else if (message.startsWith("USER_JOINED:")) {
                    String[] parts = message.substring(12).split(":");
                    String username = parts[0];
                    if (!username.equals(currentUser.getUsername())) {
                        addChatMessage("سیستم", username + " وارد شد", "#00FF00");
                    }
                }

                // ===== کاربر خارج شد =====
                else if (message.startsWith("USER_LEFT:")) {
                    String username = message.substring(10);
                    addChatMessage("سیستم", username + " خارج شد", "#FF6600");
                    removeCursorDisplay(username);
                }

                // ===== لیست کاربران (مهم!) =====
                else if (message.startsWith("USERLIST:")) {
                    String data = message.substring(9);

                    onlineUsersList.getItems().clear();

                    if (data.isEmpty() || data.trim().isEmpty()) {
                        onlineUsersList.getItems().add("👤 " + currentUser.getUsername() + " (من)");
                        usersLabel.setText("👥 کاربران: 1");
                        return;
                    }

                    String[] users = data.split(",");
                    int count = 0;

                    for (String userEntry : users) {
                        userEntry = userEntry.trim();
                        if (userEntry.isEmpty()) continue;

                        // فرمت: username:role
                        String uname;
                        String urole;

                        if (userEntry.contains(":")) {
                            String[] parts = userEntry.split(":");
                            uname = parts[0].trim();
                            urole = parts.length > 1 ? parts[1].trim() : "OPERATOR";
                        } else {
                            uname = userEntry;
                            urole = "OPERATOR";
                        }

                        if (uname.isEmpty()) continue;

                        count++;
                        String emoji = getEmojiForRole(urole);

                        if (uname.equalsIgnoreCase(currentUser.getUsername())) {
                            onlineUsersList.getItems().add(emoji + " " + uname + " (من)");
                        } else {
                            onlineUsersList.getItems().add(emoji + " " + uname);
                        }
                    }

                    usersLabel.setText("👥 کاربران: " + count);
                    System.out.println("👥 لیست کاربران بروز شد: " + count + " نفر");
                }

                // ===== خوش‌آمدگویی =====
                else if (message.startsWith("WELCOME:")) {
                    updateStatus("✅ متصل شد به سرور", Color.LIGHTGREEN);
                }

                // ===== خطا =====
                else if (message.startsWith("ERROR:")) {
                    showAlert("خطا", message.substring(6));
                }

            } catch (Exception e) {
                System.err.println("❌ خطا در پردازش پیام: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }


    private void updateCursorDisplay(String username, double x, double y) {
        Label cursorLabel = cursorLabels.get(username);

        if (cursorLabel == null) {
            cursorLabel = new Label("👆 " + username);
            cursorLabel.setStyle(
                    "-fx-background-color: rgba(0,0,0,0.7); " +
                            "-fx-text-fill: #00ff00; " +
                            "-fx-padding: 2 5; " +
                            "-fx-font-size: 10px; " +
                            "-fx-background-radius: 3;"
            );
            cursorLabels.put(username, cursorLabel);
            canvasPane.getChildren().add(cursorLabel);
        }

        cursorLabel.setLayoutX(x + 15);
        cursorLabel.setLayoutY(y + 15);
        cursorLabel.setVisible(true);
    }

    private void removeCursorDisplay(String username) {
        Label cursorLabel = cursorLabels.remove(username);
        if (cursorLabel != null) {
            canvasPane.getChildren().remove(cursorLabel);
        }
    }

    private void saveMap() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("ذخیره نقشه");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("OpsMap Files", "*.opsmap"));
        fileChooser.setInitialFileName("map_" + System.currentTimeMillis());

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                MapData mapData = new MapData("نقشه", new ArrayList<>(shapes), currentUser.getUsername());

                String filename = file.getAbsolutePath();
                if (!filename.endsWith(".opsmap")) {
                    filename += ".opsmap";
                }

                if (mapData.saveToFile(filename)) {
                    updateStatus("💾 ذخیره شد: " + file.getName(), Color.LIGHTGREEN);
                } else {
                    updateStatus("❌ خطا در ذخیره", Color.RED);
                }
            } catch (Exception e) {
                updateStatus("❌ خطا در ذخیره: " + e.getMessage(), Color.RED);
            }
        }
    }

    private void loadMap() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("بارگذاری نقشه");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("OpsMap Files", "*.opsmap"));

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                MapData mapData = MapData.loadFromFile(file.getAbsolutePath());
                if (mapData != null) {
                    shapes.clear();
                    shapes.addAll(mapData.getShapes());
                    redrawCanvas();
                    updateShapesCount();
                    updateStatus("📂 نقشه بارگذاری شد: " + mapData.getName(), Color.LIGHTGREEN);

                    // ارسال به سرور
                    if (client != null && client.isConnected()) {
                        for (MapShape shape : shapes) {
                            client.sendMessage(shape.toNetworkFormat());
                        }
                    }
                } else {
                    updateStatus("❌ خطا در بارگذاری", Color.RED);
                }
            } catch (Exception e) {
                updateStatus("❌ خطا در بارگذاری: " + e.getMessage(), Color.RED);
            }
        }
    }

    private void updateStatus(String text, Color color) {
        if (statusLabel != null) {
            statusLabel.setText(text);
            statusLabel.setTextFill(color);
        }
    }

    private void updateShapesCount() {
        if (shapesCountLabel != null) {
            int myShapes = (int) shapes.stream()
                    .filter(s -> s.getOwner().equals(currentUser.getUsername()))
                    .count();
            shapesCountLabel.setText("📊 شکل‌ها: " + shapes.size() + " (من: " + myShapes + ")");
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void logout() {
        if (client != null) {
            client.disconnect();
        }

        // تلاش برای logout از UserManager
        try {
            UserManager.getInstance().logout(currentUser.getUsername());
        } catch (Exception e) {
            // اگر UserManager وجود نداشت، مشکلی نیست
        }

        stage.close();

        // باز کردن صفحه لاگین
        try {
            Stage loginStage = new Stage();
            new LoginPage().show(loginStage);
        } catch (Exception e) {
            System.exit(0);
        }
    }

    private String getEmojiForRole(String role) {
        if (role == null || role.isEmpty()) return "👤";
        switch (role.toUpperCase()) {
            case "COMMANDER": return "👑";
            case "OPERATOR": return "🔧";
            case "OBSERVER": return "👁️";
            default: return "👤";
        }
    }

}
