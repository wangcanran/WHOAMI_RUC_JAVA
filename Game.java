import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

//import javafx.stage.WindowEvent;

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
            BufferedReader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String message;
                while ((message = in.readLine()) != null) {
                    handleServerMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace(); // 打印异常日志
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(Game.this, "与服务器断开连接");
                    dispose();
                });
            } finally {
                try {
                    if (in != null)
                        in.close();
                    socket.close(); // 确保Socket关闭
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }).start();

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int option = JOptionPane.showConfirmDialog(
                        Game.this,
                        "确认退出房间？",
                        "提示",
                        JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION) {
                    try {
                        // 发送退出消息给服务器
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        out.println("EXIT:" + roomId + ":" + username);
                        // 关闭 Socket
                        socket.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    dispose();
                }
            }
        });
    }

    private void handleServerMessage(String message) {
        // 解析消息并更新界面
        if (message.startsWith("PLAYER_JOINED:")) {
            String newUser = message.split(":")[1];
            // updatePlayerList(newUser);
        }
        // 其他消息处理...
    }
}