package ServerSide;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ServerMain extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        System.out.println("Current directory: " + System.getProperty("user.dir"));
        System.out.println("Looking for server.fxml...");


        FXMLLoader loader = new FXMLLoader(getClass().getResource("server.fxml"));

        Parent root = loader.load();
        ServerController controller = loader.getController();

        primaryStage.setTitle("Remote Monitoring Server");
        primaryStage.setScene(new Scene(root, 900, 600));
        primaryStage.setOnCloseRequest(e -> {
            if (controller != null) {
                controller.shutdownServer();
            }
        });
        primaryStage.show();

        System.out.println("Server started successfully!");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
