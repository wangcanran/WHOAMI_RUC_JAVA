import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import javax.swing.*;

public class Game {
    private static Socket socket;
    private static int roomId;
    private static String username;
    private GameUI gameUI;
    private CopyOnWriteArrayList<String> players = new CopyOnWriteArrayList<>();
    private boolean isHost;
    private int readyCount;
    // 游戏相关变量
    private String myWord = "";
    private String myRole = "";
    private boolean gameStarted = false;

    public Game(Socket Socket, int RoomId, String Username, boolean isHost,
            CopyOnWriteArrayList<String> initialPlayers) {
        socket = Socket;
        roomId = RoomId;
        username = Username;
        this.isHost = isHost;
        this.players.addAll(initialPlayers);
        this.readyCount = 0;

        // 创建并显示GameUI
        SwingUtilities.invokeLater(() -> {
            gameUI = new GameUI(isHost, players, username, roomId);
            gameUI.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
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

    public static void sendPreparedmessage() {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("PREPARE:" + roomId + ":" + username);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void sendStartGame() {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("START_GAME:" + username);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void sendMessage(String message) {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("SEND_MESSAGE:" + message + ":" + username);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // 发送玩家描述
    public static void sendPlayerSpeak(String description) {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            // 保留原始的换行符
            out.println("PLAYER_SPEAK:" + description + ":" + username);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // 发送结束发言
    public static void sendFinishSpeak() {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("FINISH_SPEAK:" + username);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // 房主切换到下一个玩家
    public static void sendNextPlayer() {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("NEXT_PLAYER:" + username);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void handleError(String message) {
        String ErrorType = message.split(":")[1];
        switch (ErrorType) {
            case "PREPARE":
                JOptionPane.showMessageDialog(gameUI, "还有玩家未准备/人数不足3人");
                break;
            default:
                break;
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
        } else if (message.startsWith("PLAYER_LEFT:")) {
            String leftUser = message.split(":")[1];
            players.remove(leftUser);
            SwingUtilities.invokeLater(() -> {
                if (gameUI != null) {
                    gameUI.updatePlayers(players);
                }
            });
            SwingUtilities.invokeLater(() -> {
                if (gameUI != null) {
                    gameUI.messageArea.append(leftUser + ": 离开了房间" + "\n");
                }
            });
        } else if (message.startsWith("PLAYER_READY:")) {
            String readyUser = message.split(":")[1];
            if (gameUI != null) {
                gameUI.messageArea.append(readyUser + ":已准备" + "\n");
            }
        } else if (message.startsWith("SEND_MESSAGE:")) {
            String user = message.split(":")[1];
            String content = message.split(":")[2];
            SwingUtilities.invokeLater(() -> {
                if (gameUI != null) {
                    gameUI.messageArea.append(user + ":" + content + "\n");
                }
            });
        } else if (message.startsWith("ERROR:")) {
            handleError(message);
        } else if (message.startsWith("NEW_HOST:")) {
            String new_host = message.split(":")[1];
            if (new_host.equals(username)) {
                this.isHost = true;
                System.out.println("房主离开了");
                gameUI.setHostPrivileges(isHost);
            }
        } else if (message.startsWith("GAME_START:")) {
            SwingUtilities.invokeLater(() -> {
                if (gameUI != null) {
                    gameUI.messageArea.append("游戏开始！请等待词语分配..." + "\n");
                    gameUI.restrictPermission();
                    gameStarted = true;
                }
            });
        } else if (message.startsWith("WORD_ASSIGNED:")) {
            // 处理词语分配消息
            String[] parts = message.split(":");
            if (parts.length >= 3) {
                myWord = parts[1];
                myRole = parts[2];

                SwingUtilities.invokeLater(() -> {
                    if (gameUI != null) {
                        gameUI.showWordAssignment(myWord, myRole);
                        gameUI.messageArea.append("您的身份：" + myRole + "\n");
                        gameUI.messageArea.append("您的词语：" + myWord + "\n");
                        gameUI.messageArea.append("游戏正式开始！请等待发言轮次。\n");
                    }
                });
            }
        } else if (message.startsWith("ROUND_START:")) {
            // 处理轮次开始
            String round = message.split(":")[1];
            SwingUtilities.invokeLater(() -> {
                if (gameUI != null) {
                    gameUI.messageArea.append("=== 第" + round + "轮发言开始 ===\n");
                }
            });
        } else if (message.startsWith("CURRENT_SPEAKER:")) {
            // 处理当前发言者
            String[] parts = message.split(":");
            String speaker = parts[1];
            String timeLimit = parts[2];

            SwingUtilities.invokeLater(() -> {
                if (gameUI != null) {
                    gameUI.updateCurrentSpeaker(speaker);
                    if (speaker.equals(username)) {
                        gameUI.messageArea.append("轮到您发言了！请描述您的词语（时限" + (Integer.parseInt(timeLimit) / 1000) + "秒）\n");
                        gameUI.enableSpeakMode(true);
                    } else {
                        gameUI.messageArea
                                .append("轮到 " + speaker + " 发言（时限" + (Integer.parseInt(timeLimit) / 1000) + "秒）\n");
                        gameUI.enableSpeakMode(false);
                    }
                }
            });
        } else if (message.startsWith("PLAYER_SPEAK:")) {
            // 处理玩家发言
            String[] parts = message.split(":", 3); // 限制分割次数，以保留消息中可能的冒号
            if (parts.length >= 3) {
                String speaker = parts[1];
                final String content = parts[2];

                SwingUtilities.invokeLater(() -> {
                    if (gameUI != null) {
                        gameUI.messageArea.append("【描述】" + speaker + ": " + content + "\n");
                    }
                });
            }
        } else if (message.startsWith("VOTING_START:")) {
            String round = message.split(":")[1];
            SwingUtilities.invokeLater(() -> {
                if (gameUI != null) {
                    gameUI.enableVotingMode(true);
                    gameUI.messageArea.append("=== 第" + round + "轮投票开始（15秒） ===\n");
                    gameUI.messageArea.append("请点击要投票的玩家（只能看到自己的选择）\n");
                }
            });
        } else if (message.startsWith("GAME_RESULT:")) {
            // 处理游戏结果
            String result = message.substring("GAME_RESULT:".length());
            SwingUtilities.invokeLater(() -> {
                if (gameUI != null) {
                    gameUI.messageArea.append("=== 游戏结束 ===\n");
                    gameUI.messageArea.append(result + "\n");
                }
            });
        } else if (message.startsWith("TIME_UPDATE:")) {
            int remainingSeconds = Integer.parseInt(message.split(":")[1]);
            SwingUtilities.invokeLater(() -> {
                if (gameUI != null) {
                    gameUI.updateTimer(remainingSeconds);
                }
            });
        } else if (message.startsWith("SPEAK_TIMEOUT:")) {
            String timeoutPlayer = message.split(":")[1];
            SwingUtilities.invokeLater(() -> {
                if (gameUI != null) {
                    gameUI.messageArea.append(timeoutPlayer + " 发言时间已到！\n");
                    if (timeoutPlayer.equals(username)) {
                        gameUI.enableSpeakMode(false);
                    }
                }
            });
        } else if (message.startsWith("SPEAK_FINISHED:")) {
            String finishPlayer = message.split(":")[1];
            SwingUtilities.invokeLater(() -> {
                if (gameUI != null) {
                    gameUI.messageArea.append(finishPlayer + " 结束了发言！\n");
                    if (finishPlayer.equals(username)) {
                        gameUI.enableSpeakMode(false);
                    }
                }
            });
        } else if (message.startsWith("VOTING_START:")) {
            String round = message.split(":")[1];
            SwingUtilities.invokeLater(() -> {
                if (gameUI != null) {
                    gameUI.messageArea.append("=== 第" + round + "轮投票开始（15秒） ===\n");
                    gameUI.enableVotingMode(true);
                }
            });
        } else if (message.startsWith("VOTE_UPDATE:")) {
            // 格式: VOTE_UPDATE:玩家:票数
            String[] parts = message.split(":");
            String player = parts[1];
            int votes = Integer.parseInt(parts[2]);
            SwingUtilities.invokeLater(() -> {
                if (gameUI != null) {
                    gameUI.updateVoteCount(player, votes);
                }
            });
        } else if (message.startsWith("ELIMINATED:")) {
            // 格式: ELIMINATED:玩家:身份
            String[] parts = message.split(":");
            String player = parts[1];
            String role = parts[2];
            SwingUtilities.invokeLater(() -> {
                if (gameUI != null) {
                    gameUI.messageArea.append(player + " 被淘汰！身份：" + role + "\n");
                    gameUI.markPlayerAsEliminated(player);
                }
            });
        } else if (message.startsWith("VOTING_RESULT:")) {
            String result = message.substring("VOTING_RESULT:".length());
            SwingUtilities.invokeLater(() -> {
                if (gameUI != null) {
                    gameUI.messageArea.append(result + "\n");
                }
            });
        } else if (message.startsWith("GAME_OVER:")) {
            String result = message.substring("GAME_OVER:".length());
            SwingUtilities.invokeLater(() -> {
                if (gameUI != null) {
                    gameUI.messageArea.append("=== 游戏结束 ===\n");
                    gameUI.messageArea.append(result + "\n");
                    gameUI.restart(isHost);
                    gameUI.gameOver(result);
                }
            });
        } else if (message.startsWith("VOTING_TIME:")) {
            int remainingSeconds = Integer.parseInt(message.split(":")[1]);
            SwingUtilities.invokeLater(() -> {
                if (gameUI != null) {
                    gameUI.updateVotingTimer(remainingSeconds);
                }
            });
        }
        // 添加投票汇总消息处理
        else if (message.startsWith("VOTE_SUMMARY:")) {
            String voteData = message.substring("VOTE_SUMMARY:".length());
            Map<String, Integer> voteResults = parseVoteResults(voteData);
            SwingUtilities.invokeLater(() -> {
                if (gameUI != null) {
                    gameUI.showVotingResult(voteResults);
                }
            });
        } else if (message.startsWith("PLAYER_ELIMINATED:")) {
            // 格式: PLAYER_ELIMINATED:玩家名
            String player = message.split(":")[1];
            SwingUtilities.invokeLater(() -> {
                if (gameUI != null) {
                    gameUI.markPlayerAsEliminated(player);
                }
            });
        }

    }

    // 解析投票结果
    private Map<String, Integer> parseVoteResults(String voteData) {
        Map<String, Integer> results = new HashMap<>();
        if (!voteData.isEmpty()) {
            String[] entries = voteData.split(",");
            for (String entry : entries) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    try {
                        results.put(parts[0], Integer.parseInt(parts[1]));
                    } catch (NumberFormatException ex) {
                        // 忽略格式错误
                    }
                }
            }
        }
        return results;
    }

    // 添加发送投票的方法
    public static void sendVote(String target) {
        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("VOTE:" + username + ":" + target);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
