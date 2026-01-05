
package ClientSide;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientMain extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("Current directory: " + System.getProperty("user.dir"));
        System.out.println("Looking for client.fxml...");
        
        // SIMPLE PATH - file in same directory
        FXMLLoader loader = new FXMLLoader(getClass().getResource("client.fxml"));
        
        Parent root = loader.load();
        ClientController controller = loader.getController();
        
        primaryStage.setTitle("Remote Monitoring Client");
        primaryStage.setScene(new Scene(root, 700, 500));
        primaryStage.setOnCloseRequest(e -> {
            if (controller != null) {
                controller.stopMonitoring();
            }
        });
        primaryStage.show();
        
        System.out.println("Client started successfully!");
    }

    public static void main(String[] args) {
        launch(args);
    }
}