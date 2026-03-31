package com.hotelmanagementsystem.controller;

import com.hotelmanagementsystem.dao.HotelDAO;
import com.hotelmanagementsystem.model.Room;
import com.hotelmanagementsystem.service.HotelService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class MainController {

    // ── Manager fields ────────────────────────────────────────
    @FXML private TextField        roomNoField;
    @FXML private ComboBox<String> roomTypeBox;
    @FXML private TextField        priceField;
    @FXML private TextField        deleteRoomField;

    // ── Customer booking fields ───────────────────────────────
    @FXML private TextField customerNameField;
    @FXML private TextField contactField;
    @FXML private TextField bookRoomField;
    @FXML private TextField daysField;

    // ── Checkout fields ───────────────────────────────────────
    @FXML private TextField checkoutNameField;
    @FXML private TextField checkoutContactField;
    @FXML private TextField checkoutRoomField;

    // ── Rooms table ───────────────────────────────────────────
    @FXML private TableView<Room>            roomsTable;
    @FXML private TableColumn<Room, Integer> colRoomNo;
    @FXML private TableColumn<Room, String>  colType;
    @FXML private TableColumn<Room, Double>  colPrice;
    @FXML private TableColumn<Room, Boolean> colStatus;
    @FXML private TableColumn<Room, String>  colGuest;
    @FXML private TableColumn<Room, String>  colContact;

    private final HotelDAO     dao     = new HotelDAO();
    private final HotelService service = new HotelService();
    private static final String LOG_FILE = "hotel_log.txt";

    @FXML
    public void initialize() {
        roomTypeBox.getItems().addAll("Single", "Double", "Deluxe", "Suite");

        colRoomNo.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colType.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("pricePerDay"));
        colGuest.setCellValueFactory(new PropertyValueFactory<>("guestName"));
        colContact.setCellValueFactory(new PropertyValueFactory<>("guestContact"));

        colStatus.setCellValueFactory(new PropertyValueFactory<>("available"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean available, boolean empty) {
                super.updateItem(available, empty);
                if (empty || available == null) { setText(null); setStyle(""); return; }
                if (available) {
                    setText("✦  Available");
                    setStyle("-fx-text-fill: #6fcf97; -fx-font-weight: 500;");
                } else {
                    setText("●  Occupied");
                    setStyle("-fx-text-fill: #e57373; -fx-font-weight: 500;");
                }
            }
        });

        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) { setText(null); setStyle(""); return; }
                setText(String.format("₹%.0f", price));
                setStyle("-fx-text-fill: #c9a84c;");
            }
        });

        refreshRoomsTable();
    }

    // ── Manager actions ───────────────────────────────────────

    @FXML
    private void handleAddRoom() {
        try {
            int roomNo   = Integer.parseInt(roomNoField.getText().trim());
            String type  = roomTypeBox.getValue();
            double price = Double.parseDouble(priceField.getText().trim());

            if (type == null || type.isEmpty()) { showAlert("Please select a room type."); return; }

            dao.addRoom(roomNo, type, price);
            showAlert("Room " + roomNo + " (" + type + ") added at ₹" + (int) price + "/day.");
            roomNoField.clear(); priceField.clear(); roomTypeBox.setValue(null);
            refreshRoomsTable();

        } catch (NumberFormatException e) {
            showAlert("Invalid room number or price.");
        } catch (Exception e) {
            showAlert("Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteRoom() {
        try {
            int roomNo = Integer.parseInt(deleteRoomField.getText().trim());
            dao.deleteRoom(roomNo);
            showAlert("Room " + roomNo + " deleted successfully.");
            deleteRoomField.clear();
            refreshRoomsTable();

        } catch (NumberFormatException e) {
            showAlert("Invalid room number.");
        } catch (Exception e) {
            showAlert("Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewRooms() {
        refreshRoomsTable();
    }

    @FXML
    private void handleAnalytics() {
        try {
            HotelDAO.Analytics a = dao.getDetailedAnalytics();
            showAnalyticsDialog(a);
        } catch (Exception e) {
            showAlert("Analytics error: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewLog() {
        try {
            Path logPath = Paths.get(LOG_FILE);
            String content = Files.exists(logPath)
                    ? Files.readString(logPath)
                    : "(No activity recorded yet.)";
            showLogDialog(content);
        } catch (IOException e) {
            showAlert("Could not read log file: " + e.getMessage());
        }
    }

    // ── Customer actions ──────────────────────────────────────

    @FXML
    private void handleBookRoom() {
        try {
            String name    = customerNameField.getText().trim();
            String contact = contactField.getText().trim();
            int roomNo     = Integer.parseInt(bookRoomField.getText().trim());
            int days       = Integer.parseInt(daysField.getText().trim());

            if (name.isEmpty() || contact.isEmpty()) { showAlert("Guest name and contact are required."); return; }

            service.bookIfAvailable(name, contact, roomNo, days);
            showAlert("Room " + roomNo + " booked for " + name + " (" + days + " days).");
            customerNameField.clear(); contactField.clear(); bookRoomField.clear(); daysField.clear();
            refreshRoomsTable();

        } catch (NumberFormatException e) {
            showAlert("Invalid room number or days.");
        } catch (Exception e) {
            showAlert("Booking failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleCheckout() {
        try {
            String name    = checkoutNameField.getText().trim();
            String contact = checkoutContactField.getText().trim();
            int roomNo     = Integer.parseInt(checkoutRoomField.getText().trim());

            if (name.isEmpty() || contact.isEmpty()) { showAlert("Provide guest name and contact for verification."); return; }

            dao.checkoutWithVerification(roomNo, name, contact);
            showAlert("Checkout successful for room " + roomNo + ". Goodbye, " + name + "!");
            checkoutNameField.clear(); checkoutContactField.clear(); checkoutRoomField.clear();
            refreshRoomsTable();

        } catch (NumberFormatException e) {
            showAlert("Invalid room number.");
        } catch (Exception e) {
            showAlert("Checkout failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleBill() {
        try {
            String name    = checkoutNameField.getText().trim();
            String contact = checkoutContactField.getText().trim();
            int roomNo     = Integer.parseInt(checkoutRoomField.getText().trim());

            HotelDAO.BillDetails bill = dao.getBillDetails(roomNo, name, contact);
            showBillDialog(bill);

        } catch (NumberFormatException e) {
            showAlert("Invalid room number.");
        } catch (Exception e) {
            showAlert("Bill error: " + e.getMessage());
        }
    }

    // ── Log dialog ────────────────────────────────────────────

    private void showLogDialog(String content) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #111720; -fx-border-color: #c9a84c; -fx-border-width: 1;");
        root.setPrefWidth(680);
        root.setPrefHeight(500);

        // Header
        HBox header = new HBox();
        header.setStyle("-fx-background-color: #161b26; -fx-padding: 14 20;");
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("ACTIVITY LOG");
        title.setStyle("-fx-font-family: 'Jost'; -fx-font-size: 13px; -fx-font-weight: 500; " +
                       "-fx-text-fill: #c9a84c; -fx-letter-spacing: 0.1em;");

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Label fileLbl = new Label("hotel_log.txt");
        fileLbl.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: #4a5568;");

        Region spacer2 = new Region(); spacer2.setPrefWidth(16);

        Button close = new Button("✕");
        close.setStyle("-fx-background-color: transparent; -fx-text-fill: #6b7280; " +
                       "-fx-border-color: transparent; -fx-cursor: hand; -fx-font-size: 14px;");
        close.setOnAction(e -> dialog.close());

        header.getChildren().addAll(title, spacer, fileLbl, spacer2, close);

        // Accent line
        HBox accent = new HBox();
        accent.setPrefHeight(1);
        accent.setStyle("-fx-background-color: #2a3141;");

        // Log content
        TextArea logArea = new TextArea(content);
        logArea.setEditable(false);
        logArea.setWrapText(false);
        logArea.setStyle(
            "-fx-background-color: #0a0e17; " +
            "-fx-text-fill: #a8b4c8; " +
            "-fx-font-family: 'Courier New'; " +
            "-fx-font-size: 12px; " +
            "-fx-border-color: transparent; " +
            "-fx-background-insets: 0; " +
            "-fx-padding: 14 16;"
        );
        logArea.getStyleClass().add("log-area");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        // Color-code lines by type
        // (TextArea doesn't support per-line coloring natively — we use a ListView instead)
        root.getChildren().remove(logArea);

        ListView<String> logList = new ListView<>();
        logList.setStyle(
            "-fx-background-color: #0a0e17; " +
            "-fx-border-color: transparent; " +
            "-fx-font-family: 'Courier New'; " +
            "-fx-font-size: 12px;"
        );
        VBox.setVgrow(logList, Priority.ALWAYS);

        // Parse lines and add
        String[] lines = content.split("\n");
        for (String line : lines) {
            logList.getItems().add(line);
        }

        // Cell factory for color coding
        logList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("-fx-background-color: #0a0e17;");
                    return;
                }
                setText(item);
                String base = "-fx-font-family: 'Courier New'; -fx-font-size: 12px; " +
                              "-fx-background-color: #0a0e17; -fx-padding: 3 12;";
                if (item.contains("BOOKING")) {
                    setStyle(base + "-fx-text-fill: #6fcf97;");
                } else if (item.contains("CHECKOUT")) {
                    setStyle(base + "-fx-text-fill: #7eb8f7;");
                } else if (item.contains("BILL VIEWED")) {
                    setStyle(base + "-fx-text-fill: #c9a84c;");
                } else if (item.contains("ROOM ADDED")) {
                    setStyle(base + "-fx-text-fill: #b39ddb;");
                } else {
                    setStyle(base + "-fx-text-fill: #4a5568;");
                }
            }
        });

        // Scroll to bottom
        if (!logList.getItems().isEmpty()) {
            logList.scrollTo(logList.getItems().size() - 1);
        }

        // Footer legend
        HBox legend = new HBox(20);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setPadding(new Insets(10, 20, 12, 20));
        legend.setStyle("-fx-background-color: #0d1117; -fx-border-color: #2a3141; -fx-border-width: 1 0 0 0;");
        legend.getChildren().addAll(
            legendDot("#6fcf97", "Booking"),
            legendDot("#7eb8f7", "Checkout"),
            legendDot("#c9a84c", "Bill"),
            legendDot("#b39ddb", "Room Added")
        );

        root.getChildren().addAll(header, accent, logList, legend);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private HBox legendDot(String color, String label) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);
        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 10px;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-family: 'Jost'; -fx-font-size: 11px; -fx-text-fill: #6b7280;");
        box.getChildren().addAll(dot, lbl);
        return box;
    }

    // ── Analytics dialog ──────────────────────────────────────

    private void showAnalyticsDialog(HotelDAO.Analytics a) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #111720; -fx-border-color: #c9a84c; -fx-border-width: 1;");
        root.setPrefWidth(520);

        // Header
        HBox header = new HBox();
        header.setStyle("-fx-background-color: #161b26; -fx-padding: 16 20;");
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("HOTEL ANALYTICS");
        title.setStyle("-fx-font-family: 'Jost'; -fx-font-size: 13px; -fx-font-weight: 500; " +
                       "-fx-text-fill: #c9a84c; -fx-letter-spacing: 0.1em;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button close = new Button("✕");
        close.setStyle("-fx-background-color: transparent; -fx-text-fill: #6b7280; " +
                       "-fx-border-color: transparent; -fx-cursor: hand; -fx-font-size: 14px;");
        close.setOnAction(e -> dialog.close());
        header.getChildren().addAll(title, spacer, close);

        // Stat grid
        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20));
        grid.getColumnConstraints().addAll(colPct(50), colPct(50));

        grid.add(statCard("Total Bookings",  String.valueOf(a.totalBookings),         "#c9a84c"), 0, 0);
        grid.add(statCard("Total Revenue",   String.format("₹%.2f", a.totalRevenue), "#6fcf97"), 1, 0);
        grid.add(statCard("Rooms Occupied",  String.valueOf(a.occupiedRooms),         "#e57373"), 0, 1);
        grid.add(statCard("Rooms Available", String.valueOf(a.availableRooms),        "#7eb8f7"), 1, 1);

        // Divider
        HBox divider = new HBox();
        divider.setPrefHeight(1);
        divider.setStyle("-fx-background-color: #2a3141;");

        // Room-type breakdown
        VBox typeSection = new VBox(12);
        typeSection.setPadding(new Insets(16, 20, 24, 20));

        Label typeTitle = new Label("REVENUE BY ROOM TYPE");
        typeTitle.setStyle("-fx-font-family: 'Jost'; -fx-font-size: 11px; " +
                           "-fx-text-fill: #6b7280; -fx-font-weight: 500;");
        typeSection.getChildren().add(typeTitle);

        double maxRev = a.typeRevenues.stream().mapToDouble(Double::doubleValue).max().orElse(1);

        for (int i = 0; i < a.typeNames.size(); i++) {
            String type = a.typeNames.get(i);
            int    cnt  = a.typeCounts.get(i);
            double rev  = a.typeRevenues.get(i);
            double pct  = maxRev > 0 ? rev / maxRev : 0;

            VBox row = new VBox(5);

            HBox meta = new HBox();
            Label typeLbl = new Label(type);
            typeLbl.setStyle("-fx-font-family: 'Jost'; -fx-font-size: 13px; -fx-text-fill: #e8dcc8;");
            Region rowSpacer = new Region(); HBox.setHgrow(rowSpacer, Priority.ALWAYS);
            Label revLbl = new Label(String.format("₹%.0f  ·  %d booking%s", rev, cnt, cnt == 1 ? "" : "s"));
            revLbl.setStyle("-fx-font-family: 'Jost'; -fx-font-size: 12px; -fx-text-fill: #6b7280;");
            meta.getChildren().addAll(typeLbl, rowSpacer, revLbl);

            StackPane barBg = new StackPane();
            barBg.setStyle("-fx-background-color: #1e2536; -fx-background-radius: 3;");
            barBg.setPrefHeight(6);
            barBg.setMaxWidth(Double.MAX_VALUE);

            HBox barFill = new HBox();
            barFill.setStyle("-fx-background-color: #c9a84c; -fx-background-radius: 3;");
            barFill.setPrefHeight(6);
            barFill.setPrefWidth(pct * 460);
            StackPane.setAlignment(barFill, Pos.CENTER_LEFT);
            barBg.getChildren().add(barFill);

            row.getChildren().addAll(meta, barBg);
            typeSection.getChildren().add(row);
        }

        if (a.typeNames.isEmpty()) {
            Label empty = new Label("No booking data yet.");
            empty.setStyle("-fx-text-fill: #4a5568; -fx-font-family: 'Jost'; -fx-font-size: 12px;");
            typeSection.getChildren().add(empty);
        }

        root.getChildren().addAll(header, grid, divider, typeSection);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ── Bill dialog ───────────────────────────────────────────

    private void showBillDialog(HotelDAO.BillDetails b) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);

        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: #111720; -fx-border-color: #c9a84c; -fx-border-width: 1;");
        root.setPrefWidth(420);

        // Hotel header
        VBox billHeader = new VBox(4);
        billHeader.setAlignment(Pos.CENTER);
        billHeader.setPadding(new Insets(28, 20, 20, 20));
        billHeader.setStyle("-fx-background-color: #161b26;");
        Label hotelName = new Label("SAPHIRE CROWN");
        hotelName.setStyle("-fx-font-family: 'Jost'; -fx-font-size: 20px; -fx-font-weight: 500; " +
                           "-fx-text-fill: #c9a84c; -fx-letter-spacing: 0.15em;");
        Label hotelSub = new Label("Hotel Management  ·  Official Receipt");
        hotelSub.setStyle("-fx-font-family: 'Jost'; -fx-font-size: 11px; -fx-text-fill: #4a5568;");
        billHeader.getChildren().addAll(hotelName, hotelSub);

        HBox accentLine = new HBox();
        accentLine.setPrefHeight(1);
        accentLine.setStyle("-fx-background-color: #c9a84c; -fx-opacity: 0.5;");

        VBox guestBox = new VBox(10);
        guestBox.setPadding(new Insets(20, 24, 16, 24));
        guestBox.getChildren().addAll(
            billRow("Guest Name",  b.guestName),
            billRow("Contact",     b.guestContact),
            billRow("Room No.",    String.valueOf(b.roomNumber)),
            billRow("Room Type",   b.roomType),
            billRow("Check-in",    b.checkInDate),
            billRow("Check-out",   java.time.LocalDate.now()
                       .format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy")))
        );

        HBox dash = new HBox();
        dash.setPrefHeight(1);
        dash.setStyle("-fx-background-color: #2a3141;");

        VBox calcBox = new VBox(10);
        calcBox.setPadding(new Insets(16, 24, 16, 24));
        calcBox.getChildren().addAll(
            billRow("Room Rate", String.format("₹%.2f / day", b.pricePerDay)),
            billRow("Duration",  b.days + " day" + (b.days > 1 ? "s" : ""))
        );

        HBox totalBox = new HBox();
        totalBox.setAlignment(Pos.CENTER_LEFT);
        totalBox.setPadding(new Insets(16, 24, 16, 24));
        totalBox.setStyle("-fx-background-color: #1a2030;");
        Label totalLabel = new Label("TOTAL AMOUNT");
        totalLabel.setStyle("-fx-font-family: 'Jost'; -fx-font-size: 11px; " +
                            "-fx-text-fill: #6b7280; -fx-font-weight: 500;");
        Region tSpacer = new Region(); HBox.setHgrow(tSpacer, Priority.ALWAYS);
        Label totalAmt = new Label(String.format("₹%.2f", b.totalBill));
        totalAmt.setStyle("-fx-font-family: 'Jost'; -fx-font-size: 24px; " +
                          "-fx-font-weight: 500; -fx-text-fill: #c9a84c;");
        totalBox.getChildren().addAll(totalLabel, tSpacer, totalAmt);

        VBox footer = new VBox(10);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(18, 20, 22, 20));
        footer.setStyle("-fx-background-color: #0d1117;");
        Label thankYou = new Label("Thank you for staying with us at Saphire Crown.");
        thankYou.setStyle("-fx-font-family: 'Jost'; -fx-font-size: 11px; -fx-text-fill: #4a5568;");
        Button closeBtn = new Button("Close Receipt");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #2e3a52; " +
                          "-fx-border-radius: 5; -fx-background-radius: 5; " +
                          "-fx-text-fill: #8a94a6; -fx-font-family: 'Jost'; " +
                          "-fx-cursor: hand; -fx-padding: 7 28; -fx-font-size: 12px;");
        closeBtn.setOnAction(e -> dialog.close());
        footer.getChildren().addAll(thankYou, closeBtn);

        root.getChildren().addAll(billHeader, accentLine, guestBox, dash, calcBox, totalBox, footer);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ── Helpers ───────────────────────────────────────────────

    private HBox billRow(String label, String value) {
        HBox row = new HBox();
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-family: 'Jost'; -fx-font-size: 12px; -fx-text-fill: #6b7280;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label val = new Label(value);
        val.setStyle("-fx-font-family: 'Jost'; -fx-font-size: 12px; -fx-text-fill: #e8dcc8;");
        row.getChildren().addAll(lbl, sp, val);
        return row;
    }

    private VBox statCard(String label, String value, String color) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle("-fx-background-color: #1e2536; -fx-background-radius: 8; " +
                      "-fx-border-color: #2a3141; -fx-border-radius: 8; -fx-border-width: 1;");
        Label lbl = new Label(label.toUpperCase());
        lbl.setStyle("-fx-font-family: 'Jost'; -fx-font-size: 10px; " +
                     "-fx-text-fill: #6b7280; -fx-font-weight: 500;");
        Label val = new Label(value);
        val.setStyle("-fx-font-family: 'Jost'; -fx-font-size: 24px; " +
                     "-fx-font-weight: 500; -fx-text-fill: " + color + ";");
        card.getChildren().addAll(lbl, val);
        return card;
    }

    private ColumnConstraints colPct(double pct) {
        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(pct);
        return cc;
    }

    private void refreshRoomsTable() {
        try {
            List<Room> rooms = dao.getAllRooms();
            roomsTable.getItems().setAll(rooms);
        } catch (Exception e) {
            showAlert("Could not load rooms: " + e.getMessage());
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Saphire Crown Hotel");
        alert.getDialogPane().setStyle("-fx-background-color: #161b26; -fx-font-family: 'Jost';");
        alert.getDialogPane().lookup(".content.label").setStyle(
            "-fx-text-fill: #e8dcc8; -fx-font-family: 'Jost'; -fx-font-size: 13px;");
        alert.showAndWait();
    }
}