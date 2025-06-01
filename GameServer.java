import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    private static final int PORT = 8000;
    private static final ConcurrentHashMap<Integer, RoomInfo> rooms = new ConcurrentHashMap<>();
    private static final Random random = new Random();
    private static ServerSocket serverSocket;


    static class RoomInfo {
        CopyOnWriteArrayList<String> players = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<PrintWriter> clientWriters = new CopyOnWriteArrayList<>();
        
        public RoomInfo(String creator) {
            players.add(creator);
        }
    }

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
        private int currentRoomId = -1;
        private String username;

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
            this.username = username;
            int newRoomId;
            do {
                newRoomId = 1000 + random.nextInt(9000);
            } while (rooms.containsKey(newRoomId));

            RoomInfo roomInfo = new RoomInfo(username);
            roomInfo.clientWriters.add(out);
            rooms.put(newRoomId, roomInfo);
            currentRoomId = newRoomId;

            out.println("ROOM_CREATED:" + newRoomId);
        }

        private void handleJoinRoom(int roomId, String username) {
            if (rooms.containsKey(roomId)) {
                RoomInfo roomInfo = rooms.get(roomId);
                roomInfo.players.add(username);
                roomInfo.clientWriters.add(out);
                currentRoomId = roomId;

                 // 广播新玩家加入
                broadcast(roomId, "PLAYER_JOINED:" + username);
                out.println("JOIN_SUCCESS:" + String.join(",", roomInfo.players));
            } else {
                out.println("ERROR:房间不存在");
            }
        }

        private void handleValidateRoom(int roomId) {
            out.println("VALIDATION_RESULT:" + rooms.containsKey(roomId));
        }


        // 添加广播方法
        private void broadcast(int roomId, String message) {
            if (rooms.containsKey(roomId)) {
                RoomInfo roomInfo = rooms.get(roomId);
                for (PrintWriter writer : roomInfo.clientWriters) {
                    writer.println(message);
                }
            }
        }
    }
}