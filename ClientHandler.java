package ServerSide;
import java.io.*;
import java.net.Socket;
import shared.SystemInfo;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ServerController controller;
    private ObjectInputStream inputStream;
    private String clientHostname;
    
    public ClientHandler(Socket socket, ServerController controller) {
        this.socket = socket;
        this.controller = controller;
    }
    
    @Override
    public void run() {
        try {
            inputStream = new ObjectInputStream(socket.getInputStream());
            clientHostname = socket.getInetAddress().getHostName();
            
            controller.log("New connection from: " + clientHostname);
            
            while (!socket.isClosed()) {
                SystemInfo info = (SystemInfo) inputStream.readObject();
                controller.addOrUpdateClient(info);
            }
            
        } catch (EOFException e) {
            // Client disconnected normally
        } catch (Exception e) {
            controller.log("Error with client " + clientHostname + ": " + e.getMessage());
        } finally {
            closeConnection();
        }
    }
    
    private void closeConnection() {
        try {
            if (inputStream != null) inputStream.close();
            if (socket != null && !socket.isClosed()) socket.close();
            controller.removeClient(clientHostname);
        } catch (IOException e) {
            controller.log("Error closing connection: " + e.getMessage());
        }
    }
}