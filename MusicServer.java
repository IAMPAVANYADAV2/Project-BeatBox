import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicServer {
    final List<ObjectOutputStream> clientOutputStream = new ArrayList<>();

    public static void main(String[] args) {
        new MusicServer().go();
    }

    public void go() {
        try {
            ServerSocket serverSocket = new ServerSocket(4242);
            ExecutorService executor = Executors.newCachedThreadPool();
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                clientOutputStream.add(out);
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                executor.execute(clientHandler);
                System.out.println("Got a Connection");
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
    public void tellEveryone(Object one, Object two){
        for(ObjectOutputStream clientOutputStreams : clientOutputStream){
            try{
                clientOutputStreams.writeObject(one);
                clientOutputStreams.writeObject(two);
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }
    class ClientHandler implements Runnable{
        private ObjectInputStream in;
        public ClientHandler(Socket socket){
            try{
                in=new ObjectInputStream(socket.getInputStream());
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
        public void run(){
            Object userName;
            Object beatSequence;
            try{
                while((userName=in.readObject()) != null){
                    beatSequence=in.readObject();
                    System.out.println("Read Two Objects");
                    tellEveryone(userName,beatSequence);
                }
            }
            catch (IOException | ClassNotFoundException e){
                e.printStackTrace();
            }
        }
    }
}
