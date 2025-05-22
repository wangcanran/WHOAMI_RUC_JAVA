import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.*;

public class Client1 extends JFrame {
    private JTextField server_ip, room_id;
    private JButton Start, End, Create;
    private static final int PORT = 8000;

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
    }

    public void background() {
        ((JPanel) this.getContentPane()).setOpaque(false);
        ImageIcon img = new ImageIcon("resources/start.jpg");
        JLabel background = new JLabel(img);
        this.getLayeredPane().add(background, Integer.valueOf(Integer.MIN_VALUE));
        background.setBounds(0, 0, img.getIconWidth(), img.getIconHeight());
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

                        JOptionPane.showMessageDialog(this, "连接成功，房间创建成功！房间号为：" + roomId,
                                "连接成功", JOptionPane.INFORMATION_MESSAGE);

                        new Game(socket, roomId, username); // 传递未关闭的 Socket
                        dispose();
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
                    String joinRes = in.readLine();
                    if (joinRes.startsWith("JOIN_SUCCESS:")) {
                        new Game(socket, roomId, username); // 传递未关闭的 Socket
                        // dispose();
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
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Client1());
    }
}