import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.*;

public class Client1 extends JFrame {
    private JTextField server_ip, room_id;
    private JButton Start, End, Create, viewRooms;
    private static final int PORT = 8080;

    public Client1() {
        super("谁是卧底-开始界面");
        background();
        setSize(450, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 窗口居中
        setVisible(true);

        server_ip = new JTextField(15);
        room_id = new JTextField(15);
        Start = new JButton("进入房间");
        End = new JButton("退出游戏");
        Create = new JButton("创建房间");
        viewRooms = new JButton("查看房间");

        layoutComponents(getContentPane());
        addListeners();
    }

    public void layoutComponents(Container c) {
        c.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // 左侧填充
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        c.add(Box.createHorizontalGlue(), gbc);

        // 右侧填充
        gbc.gridx = 3;
        gbc.gridy = 0;
        c.add(Box.createHorizontalGlue(), gbc);

        // 服务器地址标签
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.LINE_END;
        JLabel serverLabel = new JLabel("服务器地址");
        serverLabel.setFont(new Font("宋体", Font.BOLD, 16));
        serverLabel.setForeground(Color.RED);
        c.add(serverLabel, gbc);

        // 服务器地址输入框
        gbc.gridx = 2;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        c.add(server_ip, gbc);

        // 房间号标签
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.LINE_END;
        JLabel roomLabel = new JLabel("房间号");
        roomLabel.setFont(new Font("宋体", Font.BOLD, 16));
        roomLabel.setForeground(Color.RED);
        c.add(roomLabel, gbc);

        // 房间号输入框
        gbc.gridx = 2;
        gbc.anchor = GridBagConstraints.LINE_START;
        c.add(room_id, gbc);

        // 按钮布局
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        Start.setPreferredSize(new Dimension(120, 30));
        c.add(Start, gbc);

        gbc.gridy = 3;
        Create.setPreferredSize(new Dimension(120, 30));
        c.add(Create, gbc);

        gbc.gridy = 4;
        End.setPreferredSize(new Dimension(120, 30));
        c.add(End, gbc);

        gbc.gridy = 5;
        viewRooms.setPreferredSize(new Dimension(120, 30));
        c.add(viewRooms, gbc);
    }

    public void background() {
        ((JPanel) this.getContentPane()).setOpaque(false);
        ImageIcon img = new ImageIcon("resources/start.jpg");
        JLabel background = new JLabel(img);
        this.getLayeredPane().add(background, Integer.valueOf(Integer.MIN_VALUE));
        background.setBounds(0, 0, img.getIconWidth(), img.getIconHeight());
    }

    // 房间信息类
    private static class RoomInfo {
        int roomId;
        int playerCount;

        public RoomInfo(int roomId, int playerCount) {
            this.roomId = roomId;
            this.playerCount = playerCount;
        }

        public int getRoomId() {
            return roomId;
        }

        public int getPlayerCount() {
            return playerCount;
        }
    }

    private void addListeners() {
        // 退出按钮
        End.addActionListener(e -> dispose());

        // 创建房间按钮
        Create.addActionListener(e -> {
            String username = JOptionPane.showInputDialog("请输入你的昵称：");
            if (username != null && !username.trim().isEmpty()) {
                try {
                    // 手动创建 Socket，不使用 try-with-resources
                    Socket socket = new Socket(server_ip.getText(), PORT);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    // 发送创建房间请求
                    out.println("CREATE:" + username);

                    // 接收服务器响应
                    String response = in.readLine();
                    if (response.startsWith("ROOM_CREATED:")) {
                        int roomId = Integer.parseInt(response.split(":")[1]);
                        room_id.setText(String.valueOf(roomId));

                        CopyOnWriteArrayList<String> players = new CopyOnWriteArrayList<>();
                        players.add(username);

                        JOptionPane.showMessageDialog(this, "连接成功，房间创建成功！房间号为：" + roomId,
                                "连接成功", JOptionPane.INFORMATION_MESSAGE);

                        new Game(socket, roomId, username, true, players); // 传递未关闭的 Socket
                        // 关闭开始界面
                        SwingUtilities.invokeLater(() -> dispose());
                    } else {
                        // 如果创建失败，手动关闭 Socket
                        socket.close();
                    }
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "连接服务器失败");
                }
            }
        });
        // 加入房间按钮
        Start.addActionListener(e -> {
            String username = JOptionPane.showInputDialog("请输入你的昵称：");
            if (username == null || username.trim().isEmpty())
                return;

            try {
                // 手动创建 Socket，不使用 try-with-resources
                Socket socket = new Socket(server_ip.getText(), PORT);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // 验证房间号
                int roomId = Integer.parseInt(room_id.getText());
                out.println("VALIDATE:" + roomId);
                String validateRes = in.readLine();

                if ("VALIDATION_RESULT:true".equals(validateRes)) {
                    // 加入房间
                    out.println("JOIN:" + roomId + ":" + username);
                    String receive = in.readLine();
                    if (receive.equals("ERROR:FULL")) {
                        JOptionPane.showMessageDialog(this,
                                "房间已满，无法加入",
                                "错误",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String joinRes = in.readLine();
                    if (joinRes.startsWith("JOIN_SUCCESS:")) {
                        String playersStr = joinRes.substring("JOIN_SUCCESS:".length());
                        CopyOnWriteArrayList<String> players = new CopyOnWriteArrayList<>(
                                Arrays.asList(playersStr.split(",")));
                        JOptionPane.showMessageDialog(this, "加入成功！房间号为：" + roomId,
                                "加入成功", JOptionPane.INFORMATION_MESSAGE);
                        new Game(socket, roomId, username, false, players); // 传递未关闭的 Socket
                        // 关闭开始界面
                        SwingUtilities.invokeLater(() -> dispose());
                    } else {
                        JOptionPane.showMessageDialog(this, joinRes);
                        socket.close(); // 加入失败时关闭
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "房间不存在");
                    socket.close(); // 验证失败时关闭
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "--连接失败：" + ex.getMessage());
            }
        });

        viewRooms.addActionListener(e -> {
            try {
                // 连接服务器获取房间列表
                Socket listSocket = new Socket(server_ip.getText(), PORT);
                PrintWriter out = new PrintWriter(listSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(listSocket.getInputStream()));

                // 发送获取房间列表请求
                out.println("LIST_ROOMS");

                // 读取服务器响应
                String response = in.readLine();
                if (response != null && response.startsWith("ROOM_LIST:")) {
                    // 解析房间列表
                    String roomData = response.substring("ROOM_LIST:".length());
                    List<RoomInfo> rooms = new ArrayList<>();
                    if (!roomData.isEmpty()) {
                        String[] roomEntries = roomData.split(";");
                        for (String entry : roomEntries) {
                            String[] parts = entry.split(",");
                            if (parts.length == 2) {
                                int roomId = Integer.parseInt(parts[0]);
                                int playerCount = Integer.parseInt(parts[1]);
                                rooms.add(new RoomInfo(roomId, playerCount));
                            }
                        }
                    }

                    // 关闭临时Socket
                    listSocket.close();

                    // 显示房间列表对话框
                    showRoomListDialog(rooms);
                } else {
                    JOptionPane.showMessageDialog(this, "获取房间列表失败");
                    listSocket.close();
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "连接服务器失败");
            }
        });
    }

    // 显示房间列表对话框
    private void showRoomListDialog(List<RoomInfo> rooms) {
        JDialog dialog = new JDialog(this, "房间列表", true);
        dialog.setSize(300, 400);
        dialog.setLayout(new BorderLayout());

        // 创建表格模型
        String[] columnNames = { "房间号", "人数" };
        Object[][] data = new Object[rooms.size()][2];
        for (int i = 0; i < rooms.size(); i++) {
            RoomInfo room = rooms.get(i);
            data[i][0] = room.getRoomId();
            data[i][1] = room.getPlayerCount();
        }

        JTable table = new JTable(data, columnNames);
        JScrollPane scrollPane = new JScrollPane(table);
        dialog.add(scrollPane, BorderLayout.CENTER);

        // 添加加入按钮
        JButton joinButton = new JButton("加入房间");
        joinButton.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow >= 0) {
                int selectedRoomId = (int) table.getValueAt(selectedRow, 0);
                room_id.setText(String.valueOf(selectedRoomId));
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "请选择一个房间");
            }
        });

        // 添加刷新按钮
        JButton refreshButton = new JButton("刷新列表");
        refreshButton.addActionListener(e -> {
            dialog.dispose();
            viewRooms.doClick();
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(joinButton);
        buttonPanel.add(refreshButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Client1());
    }
}