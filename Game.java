import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
public class Game extends JFrame {
    private final Socket socket;
    private final int roomId;
    private final String username;

    public Game(Socket socket, int roomId, String username) {
        this.socket = socket;
        this.roomId = roomId;
        this.username = username;
        
        // 初始化界面
        setTitle("房间号：" + roomId + " - 玩家：" + username);
        setSize(800, 600);
        // 添加游戏逻辑组件...
        
        // 启动消息监听线程
        new Thread(() -> {
            try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))) {
                String message;
                while ((message = in.readLine()) != null) {
                    // 处理服务器推送的消息
                    handleServerMessage(message);
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "与服务器断开连接");
                dispose();
            }
        }).start();
    }

    private void handleServerMessage(String message) {
        // 解析消息并更新界面
        if (message.startsWith("PLAYER_JOINED:")) {
            String newUser = message.split(":")[1];
            updatePlayerList(newUser);
        }
        // 其他消息处理...
    }
}