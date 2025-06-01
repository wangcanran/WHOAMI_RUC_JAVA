import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    private static final int PORT = 8000;
    private static final ConcurrentHashMap<Integer, List<String>> rooms = new ConcurrentHashMap<>();
    private static final Random random = new Random();
    private static ServerSocket serverSocket;

    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("服务器已启动，监听端口：" + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler extends Thread {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String request;
                while ((request = in.readLine()) != null) {
                    String[] parts = request.split(":");
                    switch (parts[0]) {
                        case "CREATE":
                            handleCreateRoom(parts[1]);
                            break;
                        case "JOIN":
                            handleJoinRoom(Integer.parseInt(parts[1]), parts[2]);
                            break;
                        case "VALIDATE":
                            handleValidateRoom(Integer.parseInt(parts[1]));
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleCreateRoom(String username) {
            int newRoomId;
            do {
                newRoomId = 1000 + random.nextInt(9000);
            } while (rooms.containsKey(newRoomId));

            List<String> users = new CopyOnWriteArrayList<>();
            users.add(username);
            rooms.put(newRoomId, users);
            out.println("ROOM_CREATED:" + newRoomId);
        }

        private void handleJoinRoom(int roomId, String username) {
            if (rooms.containsKey(roomId)) {
                rooms.get(roomId).add(username);
                out.println("JOIN_SUCCESS:" + String.join(",", rooms.get(roomId)));
            } else {
                out.println("ERROR:房间不存在");
            }
        }

        private void handleValidateRoom(int roomId) {
            out.println("VALIDATION_RESULT:" + rooms.containsKey(roomId));
        }
    }
}