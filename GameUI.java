import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameUI extends JFrame {
    // 成员变量
    private boolean isHost;
    private String currentGameMode = "system";
    private CopyOnWriteArrayList<String> players;
    private String currentPlayer;
    private JPanel gamePanel;
    private JPanel playersContainer;
    private Image backgroundImage;
    private int room_id;

    // UI组件成员变量
    public JTextArea messageArea;
    private JTextField inputField;
    private JButton startButton;
    private JButton endButton;
    private JButton readyButton;
    private JMenu modeMenu;
    private JRadioButtonMenuItem systemMode;
    private JRadioButtonMenuItem hostMode;

    // 新增：词语显示相关组件
    private JPanel wordPanel;
    private JLabel wordLabel;
    private JLabel roleLabel;
    private boolean gameStarted = false;

    // 新增：游戏控制组件
    private JPanel gameControlPanel;
    private JButton speakButton;
    private JButton endSpeakButton;
    private JButton nextPlayerButton;
    private JTextField speakField;
    private JLabel currentSpeakerLabel;
    private JLabel timerLabel;
    private String currentSpeaker = "";
    private boolean isMyTurn = false;
    private boolean isVotingPhase = false;

    // 投票相关成员变量
    private Map<String, JLabel> voteLabels = new HashMap<>();
    private JLabel votingTimerLabel;
    private String currentVoteTarget = null;

    public GameUI(boolean isHost, CopyOnWriteArrayList<String> players, String currentPlayer, int room_id) {
        this.isHost = isHost;
        this.players = players;
        this.currentPlayer = currentPlayer;
        this.room_id = room_id;

        loadBackgroundImage();
        initializeUI();
    }

    private void loadBackgroundImage() {
        try {
            backgroundImage = ImageIO.read(new File("resources/2.jpg"));
        } catch (IOException e) {
            System.out.println("背景图片加载失败，使用默认背景");
        }
    }

    private void initializeUI() {
        setTitle("谁是卧底 - 房间: " + room_id + " - 玩家: " + currentPlayer + " (" + players.size() + "人)");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        createMenuBar();
        createTopPanel();
        createGamePanel();
        createBottomPanel();

        setVisible(true);
    }

    // 房主权限更新方法
    public void setHostPrivileges(boolean isHost) {
        SwingUtilities.invokeLater(() -> {

            this.isHost = isHost;
            if (startButton != null) {
                startButton.setVisible(isHost);
            }
            if (endButton != null) {
                endButton.setVisible(isHost);
            }
            if (modeMenu != null) {
                modeMenu.setEnabled(isHost);
                for (Component comp : modeMenu.getMenuComponents()) {
                    comp.setEnabled(isHost);
                }
            }
            messageArea.append("系统：您现在是新房主！\n");
            revalidate();
            repaint();
        });
    }

    // 完整的restart方法 - 恢复到上一局游戏开始时的状态
    public void restart(boolean isHost) {
        SwingUtilities.invokeLater(() -> {
            // 重置游戏状态变量
            gameStarted = false;
            isVotingPhase = false;
            isMyTurn = false;
            currentVoteTarget = null;

            // 重置UI组件状态
            startButton.setEnabled(isHost);
            startButton.setVisible(isHost);

            // 重置准备按钮状态
            readyButton.setText("准备游戏");
            readyButton.setEnabled(true);
            readyButton.setBackground(new Color(70, 130, 180));

            // 隐藏游戏控制面板
            gameControlPanel.setVisible(false);

            // 禁用游戏控制组件
            speakField.setEnabled(false);
            speakButton.setEnabled(false);
            endSpeakButton.setEnabled(false);

            // 隐藏词语面板
            wordPanel.setVisible(false);

            // 重置发言者标签
            currentSpeakerLabel.setText("等待游戏开始...");
            currentSpeakerLabel.setForeground(Color.MAGENTA);

            // 重置投票标签
            votingTimerLabel.setText("");

            // 重置倒计时
            timerLabel.setText("60");
            timerLabel.setForeground(Color.BLACK);

            // 重置玩家面板到当前状态（包括被淘汰玩家）
            resetPlayersPanel();

            // 重置聊天区域
            messageArea.append("系统：游戏已重置，可以重新准备开始游戏！\n");

            // 重置菜单
            modeMenu.setEnabled(isHost);
            for (Component comp : modeMenu.getMenuComponents()) {
                comp.setEnabled(isHost);
            }

            // 刷新界面
            revalidate();
            repaint();
        });
    }

    // 重置玩家面板到当前状态（包括被淘汰玩家）
    private void resetPlayersPanel() {
        // 不重新创建玩家面板，只重置状态
        for (Component comp : playersContainer.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                String playerName = panel.getName();

                // 重置边框
                panel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(255, 255, 255, 150), 2),
                        BorderFactory.createEmptyBorder(15, 20, 15, 20)));

                // 重置背景色
                panel.setBackground(new Color(255, 255, 255, 80));

                // 重置投票标签
                JLabel voteLabel = voteLabels.get(playerName);
                if (voteLabel != null) {
                    voteLabel.setText("?票");
                    voteLabel.setVisible(false);
                    voteLabel.setForeground(Color.RED);
                }

                // 重置玩家名称标签和头像
                for (Component c : panel.getComponents()) {
                    if (c instanceof JLabel) {
                        JLabel label = (JLabel) c;
                        // 玩家名称标签
                        if (label.getText().contains(playerName)) {
                            // 重置玩家名称颜色
                            if (playerName.equals(currentPlayer)) {
                                label.setForeground(Color.BLUE);
                                label.setText(playerName + " (我)");
                            } else {
                                label.setForeground(Color.BLACK);
                                label.setText(playerName);
                            }
                        }
                        // 玩家头像标签
                        else if (label.getText().contains("👤")) {
                            label.setForeground(Color.BLACK);
                        }
                    }
                }

                // 重新添加鼠标事件监听器
                MouseListener[] listeners = panel.getMouseListeners();
                for (MouseListener listener : listeners) {
                    panel.removeMouseListener(listener);
                }

                panel.addMouseListener(createPlayerMouseListener(playerName));
            }
        }

        playersContainer.revalidate();
        playersContainer.repaint();
    }

    // 创建玩家的鼠标事件监听器
    private MouseAdapter createPlayerMouseListener(String playerName) {
        return new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (gameStarted && isVotingPhase && !playerName.equals(currentPlayer)) {
                    JLabel voteLabel = voteLabels.get(playerName);
                    if (voteLabel != null && "已淘汰".equals(voteLabel.getText())) {
                        return;
                    }
                    currentVoteTarget = playerName;
                    for (Component comp : playersContainer.getComponents()) {
                        if (comp instanceof JPanel) {
                            JPanel p = (JPanel) comp;
                            p.setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createLineBorder(new Color(255, 255, 255, 150), 2),
                                    BorderFactory.createEmptyBorder(15, 20, 15, 20)));
                        }
                    }
                    JPanel panel = (JPanel) e.getSource();
                    panel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.GREEN, 3),
                            BorderFactory.createEmptyBorder(15, 20, 15, 20)));
                    Game.sendVote(playerName);
                }
            }

            public void mouseEntered(MouseEvent e) {
                if (gameStarted && isVotingPhase && !playerName.equals(currentPlayer)) {
                    JLabel voteLabel = voteLabels.get(playerName);
                    if (voteLabel != null && "已淘汰".equals(voteLabel.getText())) {
                        return;
                    }
                    JPanel panel = (JPanel) e.getSource();
                    panel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.RED, 3),
                            BorderFactory.createEmptyBorder(15, 20, 15, 20)));
                }
            }

            public void mouseExited(MouseEvent e) {
                JPanel panel = (JPanel) e.getSource();
                JLabel voteLabel = voteLabels.get(playerName);
                if (voteLabel != null && "已淘汰".equals(voteLabel.getText())) {
                    panel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(Color.GRAY, 2),
                            BorderFactory.createEmptyBorder(15, 20, 15, 20)));
                } else {
                    panel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(255, 255, 255, 150), 2),
                            BorderFactory.createEmptyBorder(15, 20, 15, 20)));
                }
            }
        };
    }

    // 菜单栏创建方法
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        modeMenu = new JMenu("游戏模式");

        systemMode = new JRadioButtonMenuItem("系统给词模式");
        hostMode = new JRadioButtonMenuItem("主持人给词模式");

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

        // 初始设置菜单可用性
        modeMenu.setEnabled(isHost);
    }

    private void createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());

        // 左侧房间信息
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        infoPanel.add(new JLabel("房间号:" + room_id));

        // 中间当前发言者信息
        JPanel speakerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        currentSpeakerLabel = new JLabel("等待游戏开始...");
        currentSpeakerLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        currentSpeakerLabel.setForeground(Color.MAGENTA);
        speakerPanel.add(new JLabel("当前发言: "));
        speakerPanel.add(currentSpeakerLabel);
        // 投票显示
        votingTimerLabel = new JLabel("");
        votingTimerLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        votingTimerLabel.setForeground(Color.RED);
        speakerPanel.add(votingTimerLabel);

        // 右侧词语显示面板
        wordPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        wordPanel.setBorder(BorderFactory.createTitledBorder("我的信息"));

        roleLabel = new JLabel("等待游戏开始...");
        roleLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        roleLabel.setForeground(Color.BLUE);

        wordLabel = new JLabel("");
        wordLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        wordLabel.setForeground(Color.RED);

        wordPanel.add(roleLabel);
        wordPanel.add(new JLabel("  "));
        wordPanel.add(wordLabel);

        // 初始时隐藏词语面板
        wordPanel.setVisible(false);

        topPanel.add(infoPanel, BorderLayout.WEST);
        topPanel.add(speakerPanel, BorderLayout.CENTER);
        topPanel.add(wordPanel, BorderLayout.EAST);
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

        // 创建玩家容器面板
        playersContainer = new JPanel(new GridLayout(0, 4, 20, 20));
        playersContainer.setOpaque(false);

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
        voteLabels.clear();
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
        panel.setName(playerName);

        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 150), 2),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)));
        panel.setBackground(new Color(255, 255, 255, 80));

        // 投票标签
        JLabel voteLabel = new JLabel("?票");
        voteLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        voteLabel.setForeground(Color.RED);
        voteLabel.setVisible(false);
        panel.add(voteLabel, BorderLayout.NORTH);
        voteLabels.put(playerName, voteLabel);

        // 玩家头像
        JLabel avatar = new JLabel("👤", SwingConstants.CENTER);
        avatar.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        avatar.setForeground(Color.BLACK);

        // 玩家名称
        JLabel nameLabel = new JLabel(playerName, SwingConstants.CENTER);
        nameLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        nameLabel.setForeground(Color.BLACK);

        // 如果是当前玩家，添加特殊标识
        if (playerName.equals(currentPlayer)) {
            nameLabel.setText(playerName + " (我)");
            nameLabel.setForeground(Color.BLUE);
        }

        panel.add(avatar, BorderLayout.CENTER);
        panel.add(nameLabel, BorderLayout.SOUTH);

        // 添加鼠标事件监听器
        panel.addMouseListener(createPlayerMouseListener(playerName));

        return panel;
    }

    // 更新玩家列表方法
    public void updatePlayers(CopyOnWriteArrayList<String> newPlayers) {
        this.players = newPlayers;
        refreshPlayersPanel();
        setTitle("谁是卧底 - 房间: " + room_id + " - 玩家: " + currentPlayer + " (" + players.size() + "人)");
    }

    private void createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());

        // 创建游戏控制面板
        createGameControlPanel();
        bottomPanel.add(gameControlPanel, BorderLayout.NORTH);

        // 创建聊天区域
        createChatArea(bottomPanel);

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 20));

        // 结束游戏按钮（仅房主可见）
        endButton = new JButton("结束游戏");
        endButton.setBackground(Color.LIGHT_GRAY);
        endButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        endButton.setVisible(isHost);

        // 开始游戏按钮（仅房主可见）
        startButton = new JButton("开始游戏");
        startButton.setBackground(new Color(50, 150, 50));
        startButton.setFont(new Font("微软雅黑", Font.BOLD, 14));
        startButton.setForeground(Color.WHITE);
        startButton.setVisible(isHost);
        startButton.setEnabled(true);

        startButton.addActionListener(e -> {
            Game.sendStartGame();
        });

        // 准备游戏按钮（所有玩家可见）
        readyButton = new JButton("准备游戏");
        readyButton.setBackground(new Color(70, 130, 180));
        readyButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        readyButton.setForeground(Color.WHITE);

        readyButton.addActionListener(e -> {
            readyButton.setBackground(new Color(30, 80, 120));
            readyButton.setForeground(Color.WHITE);
            readyButton.setText("已准备");
            readyButton.setEnabled(false);
            Game.sendPreparedmessage();
        });

        // 添加按钮到面板
        buttonPanel.add(endButton);
        buttonPanel.add(startButton);
        buttonPanel.add(readyButton);

        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    // 创建游戏控制面板
    private void createGameControlPanel() {
        gameControlPanel = new JPanel(new BorderLayout());
        gameControlPanel.setBorder(BorderFactory.createTitledBorder("游戏控制"));
        gameControlPanel.setVisible(false);

        // 发言区域
        speakField = new JTextField();
        speakField.setEnabled(false);
        speakField.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        speakField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("我的描述 (按回车发送)"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        // 添加回车键监听
        speakField.addActionListener(e -> {
            String description = speakField.getText().trim();
            if (!description.isEmpty()) {
                Game.sendPlayerSpeak(description);
                speakField.setText("");
            } else {
                JOptionPane.showMessageDialog(GameUI.this, "请输入描述内容！");
            }
        });

        // 按钮面板
        JPanel controlButtonPanel = new JPanel(new FlowLayout());

        // 倒计时标签
        timerLabel = new JLabel("60");
        timerLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        timerLabel.setForeground(Color.BLACK);
        timerLabel.setBorder(BorderFactory.createTitledBorder("倒计时"));

        speakButton = new JButton("发言");
        speakButton.setBackground(new Color(50, 150, 50));
        speakButton.setForeground(Color.WHITE);
        speakButton.setEnabled(false);
        speakButton.addActionListener(e -> {
            String description = speakField.getText().trim();
            if (!description.isEmpty()) {
                Game.sendPlayerSpeak(description);
                speakField.setText("");
            } else {
                JOptionPane.showMessageDialog(this, "请输入描述内容！");
            }
        });

        // 添加结束发言按钮
        endSpeakButton = new JButton("结束发言");
        endSpeakButton.setBackground(new Color(150, 50, 50));
        endSpeakButton.setForeground(Color.WHITE);
        endSpeakButton.setEnabled(false);
        endSpeakButton.addActionListener(e -> {
            Game.sendFinishSpeak();
        });

        controlButtonPanel.add(timerLabel);
        controlButtonPanel.add(speakButton);
        controlButtonPanel.add(endSpeakButton);

        gameControlPanel.add(speakField, BorderLayout.CENTER);
        gameControlPanel.add(controlButtonPanel, BorderLayout.SOUTH);
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
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        // 输入监听（回车发送）
        inputField.addActionListener(e -> {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                Game.sendMessage(message);
                inputField.setText("");
            }
        });

        // 布局
        chatPanel.add(scrollPane, BorderLayout.CENTER);
        chatPanel.add(inputField, BorderLayout.SOUTH);

        // 添加到底部面板
        bottomPanel.add(chatPanel, BorderLayout.CENTER);
    }

    public void restrictPermission() {
        startButton.setEnabled(false);
        gameStarted = true;
        gameControlPanel.setVisible(true);
        revalidate();
        repaint();
    }

    // 显示词语分配的方法
    public void showWordAssignment(String word, String role) {
        SwingUtilities.invokeLater(() -> {
            roleLabel.setText("身份: " + role);
            wordLabel.setText("词语: " + word);

            if ("卧底".equals(role)) {
                roleLabel.setForeground(Color.RED);
                wordLabel.setForeground(Color.RED);
            } else {
                roleLabel.setForeground(Color.BLUE);
                wordLabel.setForeground(Color.BLUE);
            }

            wordPanel.setVisible(true);
            revalidate();
            repaint();
        });
    }

    // 更新当前发言者
    public void updateCurrentSpeaker(String speaker) {
        currentSpeaker = speaker;
        currentSpeakerLabel.setText(speaker);

        if (speaker.equals(currentPlayer)) {
            currentSpeakerLabel.setForeground(Color.RED);
            currentSpeakerLabel.setText(speaker + " (您)");
        } else {
            currentSpeakerLabel.setForeground(Color.MAGENTA);
        }
    }

    // 启用/禁用发言模式
    public void enableSpeakMode(boolean isMyTurn) {
        this.isMyTurn = isMyTurn;
        speakField.setEnabled(isMyTurn);
        speakButton.setEnabled(isMyTurn);
        endSpeakButton.setEnabled(isMyTurn);

        if (isMyTurn) {
            speakField.requestFocus();
        }
    }

    // 启用投票模式
    public void enableVotingMode(boolean enable) {
        isVotingPhase = enable;
        if (enable) {
            resetVoteSelection();
            currentSpeakerLabel.setText("投票阶段");
            currentSpeakerLabel.setForeground(Color.ORANGE);
            votingTimerLabel.setText("投票剩余: 30秒");

            for (JLabel label : voteLabels.values()) {
                label.setText("?票");
                label.setVisible(false);
            }
        } else {
            for (Component comp : playersContainer.getComponents()) {
                if (comp instanceof JPanel) {
                    JPanel panel = (JPanel) comp;
                    panel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(255, 255, 255, 150), 2),
                            BorderFactory.createEmptyBorder(15, 20, 15, 20)));
                }
            }

            votingTimerLabel.setText("");
            for (JLabel label : voteLabels.values()) {
                label.setVisible(false);
            }
        }
    }

    // 更新倒计时
    public void updateTimer(int seconds) {
        timerLabel.setText(String.valueOf(seconds));
        if (seconds <= 3) {
            timerLabel.setForeground(Color.RED);
        } else {
            timerLabel.setForeground(Color.BLACK);
        }
    }

    // 更新投票数
    public void updateVoteCount(String player, int count) {
        JLabel label = voteLabels.get(player);
        if (label != null) {
            label.setText(count + "票");
        }
        highlightCurrentVote(player);
    }

    // 标记玩家为淘汰状态
    public void markPlayerAsEliminated(String player) {
        SwingUtilities.invokeLater(() -> {
            JLabel voteLabel = voteLabels.get(player);
            if (voteLabel != null) {
                voteLabel.setForeground(Color.GRAY);
                voteLabel.setText("已淘汰");
            }

            for (Component comp : playersContainer.getComponents()) {
                if (comp instanceof JPanel) {
                    JPanel panel = (JPanel) comp;
                    if (panel.getName() != null && panel.getName().equals(player)) {
                        panel.setBackground(new Color(150, 150, 150, 100));
                        panel.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(Color.GRAY, 2),
                                BorderFactory.createEmptyBorder(15, 20, 15, 20)));

                        for (MouseListener listener : panel.getMouseListeners()) {
                            panel.removeMouseListener(listener);
                        }

                        for (Component c : panel.getComponents()) {
                            if (c instanceof JLabel) {
                                JLabel label = (JLabel) c;
                                label.setForeground(Color.GRAY);
                            }
                        }
                        break;
                    }
                }
            }
        });
    }

    // 游戏结束处理
    public void gameOver(String result) {
        isVotingPhase = false;
        gameControlPanel.setVisible(false);
        inputField.setEnabled(false);
        JOptionPane.showMessageDialog(this, result, "游戏结束", JOptionPane.INFORMATION_MESSAGE);
    }

    // 更新投票计时器
    public void updateVotingTimer(int seconds) {
        votingTimerLabel.setText("投票剩余: " + seconds + "秒");
    }

    // 显示投票结果
    public void showVotingResult(Map<String, Integer> voteResults) {
        for (Map.Entry<String, Integer> entry : voteResults.entrySet()) {
            JLabel label = voteLabels.get(entry.getKey());
            if (label != null) {
                label.setText(entry.getValue() + "票");
                label.setVisible(true);
            }
        }

        JDialog resultDialog = new JDialog(this, "投票结果", true);
        resultDialog.setSize(300, 200);
        resultDialog.setLayout(new BorderLayout());
        resultDialog.setLocationRelativeTo(this);

        JTextArea resultText = new JTextArea();
        resultText.setEditable(false);
        resultText.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        StringBuilder sb = new StringBuilder("本轮投票结果:\n\n");
        for (Map.Entry<String, Integer> entry : voteResults.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("票\n");
        }
        resultText.setText(sb.toString());

        JScrollPane scrollPane = new JScrollPane(resultText);
        resultDialog.add(scrollPane, BorderLayout.CENTER);

        JLabel countdownLabel = new JLabel("5秒后关闭", JLabel.CENTER);
        countdownLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        resultDialog.add(countdownLabel, BorderLayout.SOUTH);

        Timer countdownTimer = new Timer(1000, e -> {
            String text = countdownLabel.getText();
            int seconds = Integer.parseInt(text.substring(0, 1));
            seconds--;
            if (seconds >= 0) {
                countdownLabel.setText(seconds + "秒后关闭");
            } else {
                ((Timer) e.getSource()).stop();
                resultDialog.dispose();
                for (JLabel label : voteLabels.values()) {
                    label.setVisible(false);
                }
            }
        });
        countdownTimer.start();

        resultDialog.setVisible(true);
    }

    // 高亮当前投票选择
    public void highlightCurrentVote(String playerName) {
        for (Component comp : playersContainer.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                JLabel voteLabel = voteLabels.get(panel.getName());
                if (voteLabel != null && "已淘汰".equals(voteLabel.getText())) {
                    continue;
                }
                panel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(255, 255, 255, 150), 2),
                        BorderFactory.createEmptyBorder(15, 20, 15, 20)));
            }
        }
        if (playerName != null && playerName.equals(currentVoteTarget)) {
            for (Component comp : playersContainer.getComponents()) {
                if (comp instanceof JPanel) {
                    JPanel panel = (JPanel) comp;
                    if (panel.getName() != null && panel.getName().equals(playerName)) {
                        JLabel voteLabel = voteLabels.get(playerName);
                        if (voteLabel != null && !"已淘汰".equals(voteLabel.getText())) {
                            panel.setBorder(BorderFactory.createCompoundBorder(
                                    BorderFactory.createLineBorder(Color.GREEN, 3),
                                    BorderFactory.createEmptyBorder(15, 20, 15, 20)));
                        }
                        return;
                    }
                }
            }
        }
    }

    // 重置投票选择
    public void resetVoteSelection() {
        currentVoteTarget = null;
        for (Component comp : playersContainer.getComponents()) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                panel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(255, 255, 255, 150), 2),
                        BorderFactory.createEmptyBorder(15, 20, 15, 20)));
            }
        }
    }
}