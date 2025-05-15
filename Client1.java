import javax.swing.*;
import java.awt.*;

public class Client1 extends JFrame {
    private JTextField server_ip, room_id;
    private JButton Start, End, Create;

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
        ImageIcon img = new ImageIcon("D:\\java_demo\\586.jpg");
        JLabel background = new JLabel(img);
        this.getLayeredPane().add(background, Integer.valueOf(Integer.MIN_VALUE));
        background.setBounds(0, 0, img.getIconWidth(), img.getIconHeight());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Client1());
    }
}