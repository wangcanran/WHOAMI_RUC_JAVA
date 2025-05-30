import javax.swing.*;
import javax.swing.border.TitledBorder;

import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.*;

public class GameUI extends JFrame {
    // 新增成员变量
    private boolean isHost;  // 房主标识（测试时设为true，实际根据登录状态设置）
    private String currentGameMode = "system";  // 当前游戏模式
    private CopyOnWriteArrayList<String> players;
    private String currentPlayer;
    private JPanel playersPanel;

    
    // 原有组件
    public JTextArea messageArea;
    private JTextField inputField;


    public GameUI(boolean isHost,CopyOnWriteArrayList<String> players,String currentPlayer) {  // 修改构造函数
        this.isHost = isHost;
        this.players = players;
        this.currentPlayer = currentPlayer;
        initializeUI();
    }

    private void initializeUI() {
        //setTitle("谁是卧底 - 房间号：85672256");
        //setSize(800, 600);
        //setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        createMenuBar();
        createTopPanel();
        createGamePanel();
        createBottomPanel();
        
        //setVisible(true);
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

    // 新增房主功能控制
    private void createTopPanel() {
        JPanel topPanel = new JPanel(new GridLayout(1, 2));
        
        // 左侧房间信息
        JPanel infoPanel = new JPanel();
        infoPanel.add(new JLabel("房间号：xx"));
        infoPanel.add(new JLabel("平民：x人"));
        infoPanel.add(new JLabel("卧底：x人"));
        

        topPanel.add(infoPanel);
        add(topPanel, BorderLayout.NORTH);
    }

    private void createGamePanel() {
        JPanel gamePanel = new JPanel(new BorderLayout());
        
        // 使用JScrollPane包裹玩家面板
        playersPanel = new JPanel(new GridLayout(0, 3, 10, 10));
        playersPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        JScrollPane scrollPane = new JScrollPane(playersPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        refreshPlayersPanel();
        
        gamePanel.add(scrollPane, BorderLayout.CENTER);
        // ... 保持原有其他组件
    }

    public void refreshPlayersPanel() {
        playersPanel.removeAll();
        for (String player : players) {
            playersPanel.add(createPlayerBox(player));
            
        }
        playersPanel.revalidate();
        playersPanel.repaint();
    }

    private JPanel createPlayerBox(String playerName) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        panel.setBackground(new Color(245, 245, 245));

        // 玩家头像（示例使用文字代替）
        JLabel avatar = new JLabel("👤", SwingConstants.CENTER);
        avatar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        
        // 玩家名称
        JLabel nameLabel = new JLabel(playerName, SwingConstants.CENTER);
        nameLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));

        panel.add(avatar, BorderLayout.CENTER);
        panel.add(nameLabel, BorderLayout.SOUTH);

        // 添加点击事件
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
        endButton.setBackground(new Color(255, 99, 71));
        endButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        endButton.setVisible(isHost);

        JButton readyButton = new JButton("准备游戏");
        endButton.setBackground(new Color(255, 99, 71));
        endButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        
        buttonPanel.add(endButton);
        buttonPanel.add(readyButton);  // 原有准备按钮
        
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void createChatArea(JPanel bottomPanel) {
        // 创建聊天主面板
        JPanel chatPanel = new JPanel(new BorderLayout());
        
        // 消息显示区域
        messageArea = new JTextArea(10, 30);
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
        
}