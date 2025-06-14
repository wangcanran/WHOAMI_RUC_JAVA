import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.*;
import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

//import javafx.stage.WindowEvent;

public class Game {
    private final Socket socket;
    private final int roomId;
    private final String username;
    private GameUI gameUI;
    private CopyOnWriteArrayList<String> players = new CopyOnWriteArrayList<>();
    private boolean isHost;

    public Game(Socket socket, int roomId, String username, boolean isHost, CopyOnWriteArrayList<String> initialPlayers) {
        this.socket = socket;
        this.roomId = roomId;
        this.username = username;
        this.isHost = isHost;
        this.players.addAll(initialPlayers);
        
        // 创建并显示GameUI
        SwingUtilities.invokeLater(() -> {
            gameUI = new GameUI(isHost, players, username,roomId);
            
            // 添加窗口关闭监听器
            gameUI.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    confirmExit();
                }
            });
            
            gameUI.setVisible(true); // 确保窗口可见
            
        });
        
        // 启动消息监听线程
        new Thread(this::listenToServerMessages).start();
    }
    
    private void confirmExit() {
        int option = JOptionPane.showConfirmDialog(
                gameUI,
                "确认退出房间？",
                "提示",
                JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.YES_OPTION) {
            try {
                // 发送退出消息给服务器
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("EXIT:" + roomId + ":" + username);
                socket.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            gameUI.dispose();
        }
    }
    
    private void listenToServerMessages() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String message;
            while ((message = in.readLine()) != null) {
                handleServerMessage(message);
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(gameUI, "与服务器断开连接");
                gameUI.dispose();
            });
        }
    }

    private void handleServerMessage(String message) {
        if (message.startsWith("PLAYER_JOINED:")) {
            String newUser = message.split(":")[1];
            if (!players.contains(newUser)) {
                players.add(newUser);
                SwingUtilities.invokeLater(() -> {
                    if (gameUI != null) {
                        gameUI.updatePlayers(players);
                    }
                });
            }
        } 
        else if (message.startsWith("PLAYER_LEFT:")) {
            String leftUser = message.split(":")[1];
            players.remove(leftUser);
            SwingUtilities.invokeLater(() -> {
                if (gameUI != null) {
                    gameUI.updatePlayers(players);
                }
            });

        }
        // 其他消息处理...
    }
}

