package ServerController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import shared.SystemInfo;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerController {
    @FXML private TableView<SystemInfo> clientsTable;
    @FXML private TableColumn<SystemInfo, String> hostnameColumn;
    @FXML private TableColumn<SystemInfo, String> osColumn;
    @FXML private TableColumn<SystemInfo, String> statusColumn;
    @FXML private TableColumn<SystemInfo, String> timestampColumn;

    @FXML private Label serverStatusLabel;
    @FXML private Label connectedClientsLabel;
    @FXML private TextArea logArea;
    @FXML private VBox chartsContainer;

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private ObservableList<SystemInfo> clientsData;
    private Map<String, XYChart.Series<Number, Number>> cpuSeriesMap;
    private Map<String, XYChart.Series<Number, Number>> memorySeriesMap;
    private int timeCounter = 0;
    private boolean isRunning = false;
    private int port = 5000;

    public void initialize() {
        setupTable();
        clientsData = FXCollections.observableArrayList();
        clientsTable.setItems(clientsData);

        cpuSeriesMap = new HashMap<>();
        memorySeriesMap = new HashMap<>();

        threadPool = Executors.newCachedThreadPool();
    }

    private void setupTable() {
        hostnameColumn.setCellValueFactory(new PropertyValueFactory<>("hostname"));
        osColumn.setCellValueFactory(new PropertyValueFactory<>("os"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        timestampColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getTimestamp().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                ));
    }

    @FXML
    private void startServer() {
        if (isRunning) return;

        threadPool.execute(() -> {
            try {
                serverSocket = new ServerSocket(port);
                isRunning = true;
                Platform.runLater(() -> {
                    serverStatusLabel.setText("Running on port " + port);
                    serverStatusLabel.setStyle("-fx-text-fill: green;");
                    log("Server started on port " + port);
                });

                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.execute(new ClientHandler(clientSocket, this));
                }
            } catch (IOException e) {
                Platform.runLater(() -> {
                    log("Server error: " + e.getMessage());
                    serverStatusLabel.setText("Stopped");
                    serverStatusLabel.setStyle("-fx-text-fill: red;");
                });
            }
        });
    }

    @FXML
    private void stopServer() {
        isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log("Error stopping server: " + e.getMessage());
        }
        Platform.runLater(() -> {
            serverStatusLabel.setText("Stopped");
            serverStatusLabel.setStyle("-fx-text-fill: red;");
            log("Server stopped");
        });
    }

    public void addOrUpdateClient(SystemInfo info) {
        Platform.runLater(() -> {
            // Remove existing entry for this host
            clientsData.removeIf(client -> client.getHostname().equals(info.getHostname()));
            clientsData.add(info);
            updateCharts(info);
            connectedClientsLabel.setText("Connected: " + clientsData.size());
            log("Updated info from " + info.getHostname());
        });
    }

    private void updateCharts(SystemInfo info) {
        String hostname = info.getHostname();

        if (!cpuSeriesMap.containsKey(hostname)) {
            createChartForClient(hostname);
        }

        // Update CPU chart
        XYChart.Series<Number, Number> cpuSeries = cpuSeriesMap.get(hostname);
        cpuSeries.getData().add(new XYChart.Data<>(timeCounter, info.getCpuUsage()));

        // Update Memory chart
        XYChart.Series<Number, Number> memorySeries = memorySeriesMap.get(hostname);
        memorySeries.getData().add(new XYChart.Data<>(timeCounter, info.getMemoryUsagePercentage()));

        // Limit data points to last 50
        if (cpuSeries.getData().size() > 50) {
            cpuSeries.getData().remove(0);
            memorySeries.getData().remove(0);
        }

        timeCounter++;
    }

    private void createChartForClient(String hostname) {
        // Create CPU chart
        LineChart<Number, Number> cpuChart = createChart("CPU Usage (%)", hostname + " - CPU Usage");
        XYChart.Series<Number, Number> cpuSeries = new XYChart.Series<>();
        cpuSeries.setName("CPU");
        cpuChart.getData().add(cpuSeries);
        cpuSeriesMap.put(hostname, cpuSeries);

        // Create Memory chart
        LineChart<Number, Number> memoryChart = createChart("Memory Usage (%)", hostname + " - Memory Usage");
        XYChart.Series<Number, Number> memorySeries = new XYChart.Series<>();
        memorySeries.setName("Memory");
        memoryChart.getData().add(memorySeries);
        memorySeriesMap.put(hostname, memorySeries);

        // Add charts to container
        HBox chartBox = new HBox(10, cpuChart, memoryChart);
        chartBox.setStyle("-fx-padding: 10;");
        Platform.runLater(() -> chartsContainer.getChildren().add(chartBox));
    }

    private LineChart<Number, Number> createChart(String yLabel, String title) {
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Time");
        xAxis.setAnimated(false);

        NumberAxis yAxis = new NumberAxis(0, 100, 10);
        yAxis.setLabel(yLabel);
        yAxis.setAnimated(false);

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(title);
        chart.setAnimated(false);
        chart.setPrefWidth(400);
        chart.setPrefHeight(250);

        return chart;
    }

    public void log(String message) {
        Platform.runLater(() -> {
            logArea.appendText(java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) +
                    " - " + message + "\n");
        });
    }

    public void removeClient(String hostname) {
        Platform.runLater(() -> {
            clientsData.removeIf(client -> client.getHostname().equals(hostname));
            connectedClientsLabel.setText("Connected: " + clientsData.size());
            log("Client disconnected: " + hostname);
        });
    }

    public void shutdownServer() {
        stopServer();
        if (threadPool != null) {
            threadPool.shutdown();
        }
    }
}