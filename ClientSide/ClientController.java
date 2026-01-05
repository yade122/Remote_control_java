package ClientSide;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import shared.SystemInfo;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ClientController {
    @FXML private Label hostnameLabel;
    @FXML private Label osLabel;
    @FXML private Label cpuCoresLabel;
    @FXML private Label cpuUsageLabel;
    @FXML private Label memoryUsageLabel;
    @FXML private Label statusLabel;
    @FXML private Label timestampLabel;
    
    @FXML private LineChart<Number, Number> cpuChart;
    @FXML private LineChart<Number, Number> memoryChart;
    
    @FXML private TextField serverIPField;
    @FXML private TextField serverPortField;
    @FXML private Button connectButton;
    @FXML private Button disconnectButton;
    @FXML private TextArea logArea;
    
    private ScheduledExecutorService scheduler;
    private ObjectOutputStream outputStream;
    private Socket socket;
    private boolean isConnected = false;
    private String serverIP = "localhost";
    private int serverPort = 5000;
    private int timeCounter = 0;
    
    private XYChart.Series<Number, Number> cpuSeries;
    private XYChart.Series<Number, Number> memorySeries;
    
    public void initialize() {
        serverIPField.setText(serverIP);
        serverPortField.setText(String.valueOf(serverPort));
        
        setupCharts();
        updateLocalInfo();
        
        // Update local info every second
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::updateLocalInfo, 0, 1, TimeUnit.SECONDS);
    }
    
    private void setupCharts() {
        // Check if charts are defined in FXML
        if (cpuChart == null || memoryChart == null) {
            // Create charts programmatically if not in FXML
            createChartsProgrammatically();
        } else {
            // Use charts from FXML
            setupChartSeries();
        }
    }
    
    private void createChartsProgrammatically() {
        // Create CPU Chart
        NumberAxis cpuXAxis = new NumberAxis();
        cpuXAxis.setLabel("Time (seconds)");
        NumberAxis cpuYAxis = new NumberAxis(0, 100, 10);
        cpuYAxis.setLabel("CPU Usage (%)");
        
        cpuChart = new LineChart<>(cpuXAxis, cpuYAxis);
        cpuChart.setTitle("CPU Usage Over Time");
        cpuChart.setPrefSize(350, 250);
        cpuChart.setAnimated(false);
        
        // Create Memory Chart
        NumberAxis memoryXAxis = new NumberAxis();
        memoryXAxis.setLabel("Time (seconds)");
        NumberAxis memoryYAxis = new NumberAxis(0, 100, 10);
        memoryYAxis.setLabel("Memory Usage (%)");
        
        memoryChart = new LineChart<>(memoryXAxis, memoryYAxis);
        memoryChart.setTitle("Memory Usage Over Time");
        memoryChart.setPrefSize(350, 250);
        memoryChart.setAnimated(false);
        
        setupChartSeries();
        
        // Add charts to a container (you'll need a container in FXML with fx:id="chartsContainer")
        // If you have a container in FXML, add them there
        // Platform.runLater(() -> {
        //     if (chartsContainer != null) {
        //         HBox chartBox = new HBox(10, cpuChart, memoryChart);
        //         chartsContainer.getChildren().add(chartBox);
        //     }
        // });
    }
    
    private void setupChartSeries() {
        cpuSeries = new XYChart.Series<>();
        cpuSeries.setName("CPU");
        if (cpuChart != null) {
            cpuChart.getData().add(cpuSeries);
        }
        
        memorySeries = new XYChart.Series<>();
        memorySeries.setName("Memory");
        if (memoryChart != null) {
            memoryChart.getData().add(memorySeries);
        }
    }
    
    private void updateLocalInfo() {
        Platform.runLater(() -> {
            try {
                OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
                com.sun.management.OperatingSystemMXBean sunOsBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                
                String hostname = java.net.InetAddress.getLocalHost().getHostName();
                String os = System.getProperty("os.name") + " " + System.getProperty("os.version");
                int cores = Runtime.getRuntime().availableProcessors();
                double cpuUsage = sunOsBean.getSystemCpuLoad() * 100;
                long totalMemory = sunOsBean.getTotalPhysicalMemorySize();
                long freeMemory = sunOsBean.getFreePhysicalMemorySize();
                long usedMemory = totalMemory - freeMemory;
                
                // Update labels
                hostnameLabel.setText(hostname);
                osLabel.setText(os);
                cpuCoresLabel.setText(cores + " cores");
                cpuUsageLabel.setText(String.format("%.1f%%", cpuUsage));
                memoryUsageLabel.setText(String.format("%.1f GB / %.1f GB (%.1f%%)",
                    bytesToGB(usedMemory), bytesToGB(totalMemory),
                    (usedMemory * 100.0 / totalMemory)));
                timestampLabel.setText(LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("HH:mm:ss")));
                
                // Update charts if they exist
                if (cpuSeries != null && memorySeries != null) {
                    cpuSeries.getData().add(new XYChart.Data<>(timeCounter, cpuUsage));
                    memorySeries.getData().add(new XYChart.Data<>(timeCounter, 
                        (usedMemory * 100.0 / totalMemory)));
                    
                    // Limit data points to last 50
                    if (cpuSeries.getData().size() > 50) {
                        cpuSeries.getData().remove(0);
                        memorySeries.getData().remove(0);
                    }
                    
                    timeCounter++;
                }
                
                // Send to server if connected
                if (isConnected && outputStream != null) {
                    SystemInfo info = new SystemInfo(hostname, os, cores, cpuUsage,
                        totalMemory, usedMemory, "Connected");
                    outputStream.writeObject(info);
                    outputStream.flush();
                }
                
            } catch (Exception e) {
                log("Error updating system info: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    private double bytesToGB(long bytes) {
        return bytes / (1024.0 * 1024.0 * 1024.0);
    }
    
    @FXML
    private void connectToServer() {
        if (isConnected) return;
        
        try {
            serverIP = serverIPField.getText();
            serverPort = Integer.parseInt(serverPortField.getText());
            
            socket = new Socket(serverIP, serverPort);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            isConnected = true;
            
            Platform.runLater(() -> {
                statusLabel.setText("Connected to server");
                statusLabel.setStyle("-fx-text-fill: green;");
                connectButton.setDisable(true);
                disconnectButton.setDisable(false);
                log("Connected to server at " + serverIP + ":" + serverPort);
            });
            
        } catch (Exception e) {
            Platform.runLater(() -> {
                statusLabel.setText("Connection failed");
                statusLabel.setStyle("-fx-text-fill: red;");
                log("Failed to connect: " + e.getMessage());
            });
        }
    }
    
    @FXML
    private void disconnectFromServer() {
        if (!isConnected) return;
        
        try {
            isConnected = false;
            if (outputStream != null) outputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
            
            Platform.runLater(() -> {
                statusLabel.setText("Disconnected");
                statusLabel.setStyle("-fx-text-fill: orange;");
                connectButton.setDisable(false);
                disconnectButton.setDisable(true);
                log("Disconnected from server");
            });
            
        } catch (IOException e) {
            log("Error disconnecting: " + e.getMessage());
        }
    }
    
    private void log(String message) {
        Platform.runLater(() -> {
            logArea.appendText(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("HH:mm:ss")) + " - " + message + "\n");
        });
    }
    
    @FXML
    private void clearLog() {
        logArea.clear();
    }
    
    public void stopMonitoring() {
        disconnectFromServer();
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}