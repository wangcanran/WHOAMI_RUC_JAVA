import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.*;

public class GameUI_new extends JFrame {
    // 新增成员变量
    private boolean isHost;  // 房主标识（测试时设为true，实际根据登录状态设置）
    private String currentGameMode = "system";  // 当前游戏模式
    private CopyOnWriteArrayList<String> players;
    private String currentPlayer;
    private JPanel gamePanel;
    private JPanel playersContainer; // 新增玩家容器面板
    private Image backgroundImage;   // 背景图片
    private int room_id;
    //private JPanel playersPanel;

    
    // 原有组件
    public JTextArea messageArea;
    private JTextField inputField;


    public GameUI_new(boolean isHost,CopyOnWriteArrayList<String> players,String currentPlayer, int room_id) {  // 修改构造函数
        this.isHost = isHost;
        this.players = players;
        this.currentPlayer = currentPlayer;
        this.room_id = room_id;

        loadBackgroundImage();       
        initializeUI();
    }

    private void loadBackgroundImage() {
        try {
            backgroundImage = ImageIO.read(new File("background.jpg")); // 请替换实际图片路径
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "背景图片加载失败");
            e.printStackTrace();
        }
    }

    private void initializeUI() {
        setTitle("谁是卧底");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        createMenuBar();
        createTopPanel();
        createGamePanel();
        createBottomPanel();
        
        setVisible(true);
    }

    //菜单栏创建方法
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu modeMenu = new JMenu("游戏模式");
        
        JRadioButtonMenuItem systemMode = new JRadioButtonMenuItem("系统给词模式");
        JRadioButtonMenuItem hostMode = new JRadioButtonMenuItem("主持人给词模式");
        
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(systemMode);
        modeGroup.add(hostMode);
        systemMode.setSelected(currentGameMode.equals("system"));
        hostMode.setSelected(currentGameMode.equals("host"));

    
        // 添加权限检查
        ItemListener modeListener = e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (!isHost) {
                    JOptionPane.showMessageDialog(this, "您不是房主，无法选择游戏模式！", 
                        "权限不足", JOptionPane.WARNING_MESSAGE);
                    // 恢复之前的选择
                    if (currentGameMode.equals("system")) {
                        systemMode.setSelected(true);
                    } else {
                        hostMode.setSelected(true);
                    }
                } else {
                    currentGameMode = (e.getSource() == systemMode) ? "system" : "host";
                    messageArea.append("系统：已切换至" + 
                        ((e.getSource() == systemMode) ? "系统" : "主持人") + "给词模式\n");
                }
            }
        };

        systemMode.addItemListener(modeListener);
        hostMode.addItemListener(modeListener);

        modeMenu.add(systemMode);
        modeMenu.add(hostMode);
        menuBar.add(modeMenu);
        setJMenuBar(menuBar);
    }

    
    private void createTopPanel() {
        JPanel topPanel = new JPanel(new GridLayout(1, 2));
        
        // 左侧房间信息
        JPanel infoPanel = new JPanel();
        infoPanel.add(new JLabel("房间号:" + room_id));
        infoPanel.add(new JLabel("平民：x人"));
        infoPanel.add(new JLabel("卧底：x人"));
        

        topPanel.add(infoPanel);
        add(topPanel, BorderLayout.NORTH);
    }

    private void createGamePanel() {
        gamePanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };

        // 创建玩家容器面板（使用网格布局）
        playersContainer = new JPanel(new GridLayout(0, 4, 20, 20)); // 每行最多4个，间距20
        playersContainer.setOpaque(false); // 透明背景
        
        JScrollPane scrollPane = new JScrollPane(playersContainer);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        gamePanel.add(scrollPane, BorderLayout.CENTER);
        add(gamePanel, BorderLayout.CENTER);
        
        refreshPlayersPanel();
    }


    // 刷新玩家面板方法
    public void refreshPlayersPanel() {
        playersContainer.removeAll();
        for (String player : players) {
            JPanel playerBox = createPlayerBox(player);
            playerBox.setOpaque(false);
            playersContainer.add(playerBox);
        }
        playersContainer.revalidate();
        playersContainer.repaint();
    }

    private JPanel createPlayerBox(String playerName) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 255, 255, 150), 2),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        panel.setBackground(new Color(255, 255, 255, 80)); // 半透明背景

        // 玩家头像
        JLabel avatar = new JLabel("👤", SwingConstants.CENTER);
        avatar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        avatar.setForeground(Color.BLACK);

        // 玩家名称
        JLabel nameLabel = new JLabel(playerName, SwingConstants.CENTER);
        nameLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        nameLabel.setForeground(Color.BLACK);

        panel.add(avatar, BorderLayout.CENTER);
        panel.add(nameLabel, BorderLayout.SOUTH);

        // 点击事件
        panel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    messageArea.append("系统：已投票给玩家 " + playerName + "\n");
                }
            }
        });

        return panel;
    }

    // 更新玩家列表方法
    public void updatePlayers(CopyOnWriteArrayList<String> newPlayers) {
        this.players = newPlayers;
        refreshPlayersPanel();
    }
/* 
        
    private JPanel createKeywordPanel(String role, String word, Color color) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(color, 2), 
            role,
            TitledBorder.LEFT,
            TitledBorder.DEFAULT_POSITION,
            new Font("微软雅黑", Font.BOLD, 12),
            color));
        
        JLabel wordLabel = new JLabel(word, SwingConstants.CENTER);
        wordLabel.setFont(new Font("微软雅黑", Font.PLAIN, 24));
        wordLabel.setForeground(color.darker());
        panel.add(wordLabel);
        
        return panel;
    }
*/
    private void createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        // 聊天区域（保持原有功能）
        createChatArea(bottomPanel);
        
        // 底部按钮组
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 20));
        
        
        // 结束游戏按钮（仅房主可见）
        JButton endButton = new JButton("结束游戏");
        endButton.setBackground(Color.LIGHT_GRAY);
        endButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        endButton.setVisible(isHost);
        endButton.setOpaque(true); // 启用背景颜色显示

        JButton readyButton = new JButton("准备游戏");
        endButton.setBackground(Color.LIGHT_GRAY);
        endButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        readyButton.setOpaque(true); // 启用背景颜色显示

        readyButton.addActionListener(e -> {
                // 切换为深色模式
            readyButton.setBackground(Color.DARK_GRAY);
            readyButton.setOpaque(true); // 启用背景颜色显示
            readyButton.setForeground(Color.WHITE);
            messageArea.append(currentPlayer + "已准备就绪\n");
            readyButton.setEnabled(false);
        });
        
        buttonPanel.add(endButton);
        buttonPanel.add(readyButton);  // 原有准备按钮
        
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void createChatArea(JPanel bottomPanel) {
        // 创建聊天主面板
        JPanel chatPanel = new JPanel(new BorderLayout());
        
        // 消息显示区域
        messageArea = new JTextArea(10, 20);
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("聊天记录"));
        
        // 输入区域
        inputField = new JTextField();
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("发言"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        // 输入监听（回车发送）
        inputField.addActionListener(e -> {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                messageArea.append(currentPlayer + ": " + message + "\n");
                inputField.setText("");
            }
        });
        
        // 布局
        chatPanel.add(scrollPane, BorderLayout.CENTER);
        chatPanel.add(inputField, BorderLayout.SOUTH);
        
        // 添加到底部面板
        bottomPanel.add(chatPanel, BorderLayout.CENTER);
    }
/* 
    public static void main(String[] args) {
        CopyOnWriteArrayList<String> players = new CopyOnWriteArrayList<>();
        players.add("sutian");
        players.add("Ryan");
        players.add("Andy");
        players.add("Tina");
        players.add("Sandy");
        SwingUtilities.invokeLater(() -> new GameUI_new("sutian",players));

    }

   */ 
        
}