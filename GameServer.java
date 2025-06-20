import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.swing.Timer;

public class GameServer {
    private static final int PORT = 8080;
    private static final ConcurrentHashMap<Integer, RoomInfo> rooms = new ConcurrentHashMap<>();
    private static final Random random = new Random();
    private static ServerSocket serverSocket;

    static class RoomInfo {
        CopyOnWriteArrayList<String> players = new CopyOnWriteArrayList<>();
        CopyOnWriteArrayList<PrintWriter> clientWriters = new CopyOnWriteArrayList<>();
        ConcurrentHashMap<String, Boolean> readyStatus = new ConcurrentHashMap<>();
        int readyCount = 0;
        boolean isHostReady = false;
        boolean gameStarted = false;

        // 游戏轮次管理
        int currentRound = 1;
        int currentPlayerIndex = 0;
        boolean isDescribePhase = true;
        long speakStartTime = 0;
        final int SPEAK_TIME_LIMIT = 60000;
        ConcurrentHashMap<String, Boolean> hasSpoken = new ConcurrentHashMap<>();
        Timer speakTimer;

        // 投票相关字段
        ConcurrentHashMap<String, String> votes = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Integer> voteCounts = new ConcurrentHashMap<>();
        Timer votingTimer;
        final int VOTING_TIME_LIMIT = 30000;
        CopyOnWriteArrayList<String> eliminatedPlayers = new CopyOnWriteArrayList<>();

        // 投票方法
        public void resetVoting() {
            votes.clear();
            voteCounts.clear();
            for (String player : players) {
                if (!eliminatedPlayers.contains(player)) {
                    voteCounts.put(player, 0);
                }
            }
        }

        // 游戏模式和词语相关字段
        String gameMode = "system";
        String[][] wordPairs = readPairsFromFile("resources/wordPair.txt");
        String normalWord = "";
        String spyWord = "";
        ConcurrentHashMap<String, String> playerWords = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Boolean> playerRoles = new ConcurrentHashMap<>();

        private static String[][] readPairsFromFile(String fileName) {
            List<String[]> pairsList = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] pair = line.split(",");
                    if (pair.length == 2) {
                        pairsList.add(new String[] { pair[0], pair[1] });
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return pairsList.toArray(new String[0][]);
        }

        public RoomInfo(String creator) {
            players.add(creator);
            readyStatus.put(creator, false);
        }

        public void resetGame() {
            // 重置准备状态
            readyStatus.clear();
            for (String player : players) {
                readyStatus.put(player, false);
            }
            readyCount = 0;
            isHostReady = false;

            // 重置游戏状态
            gameStarted = false;
            currentRound = 1;
            currentPlayerIndex = 0;
            isDescribePhase = true;
            hasSpoken.clear();
            eliminatedPlayers.clear();

            // 重置投票状态
            votes.clear();
            voteCounts.clear();

            // 重置角色和词语
            normalWord = "";
            spyWord = "";
            playerWords.clear();
            playerRoles.clear();

            // 停止所有计时器
            if (speakTimer != null) {
                speakTimer.stop();
                speakTimer = null;
            }
            if (votingTimer != null) {
                votingTimer.stop();
                votingTimer = null;
            }
        }

        // 分配词语和角色的方法
        public void assignWordsAndRoles() {
            // 清空之前的分配
            playerWords.clear();
            playerRoles.clear();
            hasSpoken.clear();

            // 随机选择一对词语
            int randomIndex = random.nextInt(wordPairs.length);
            normalWord = wordPairs[randomIndex][0];
            spyWord = wordPairs[randomIndex][1];

            // 随机选择一名玩家作为卧底
            int spyIndex = random.nextInt(players.size());

            // 分配词语和角色
            for (int i = 0; i < players.size(); i++) {
                String player = players.get(i);
                boolean isSpy = (i == spyIndex);
                playerRoles.put(player, isSpy);
                playerWords.put(player, isSpy ? spyWord : normalWord);
                hasSpoken.put(player, false);
            }

            // 初始化游戏状态
            currentRound = 1;
            currentPlayerIndex = 0;
            isDescribePhase = true;
            speakStartTime = System.currentTimeMillis();

            System.out.println("词语分配完成:");
            System.out.println("平民词语: " + normalWord);
            System.out.println("卧底词语: " + spyWord);
            for (String player : players) {
                System.out.println(player + " -> " + (playerRoles.get(player) ? "卧底" : "平民") +
                        " (" + playerWords.get(player) + ")");
            }
        }

        // 获取当前发言玩家（跳过淘汰玩家）
        public String getCurrentSpeaker() {
            if (currentPlayerIndex < players.size()) {
                String player = players.get(currentPlayerIndex);
                return eliminatedPlayers.contains(player) ? null : player;
            }
            return null;
        }

        // 切换到下一个发言玩家（跳过淘汰玩家）
        public boolean nextPlayer() {
            if (speakTimer != null) {
                speakTimer.stop();
            }

            // 跳过所有淘汰玩家
            do {
                currentPlayerIndex++;
                if (currentPlayerIndex >= players.size()) {
                    return false;
                }
            } while (eliminatedPlayers.contains(players.get(currentPlayerIndex)));

            speakStartTime = System.currentTimeMillis();
            return true;
        }

        // 重置轮次（新一轮开始）
        public void nextRound() {
            if (speakTimer != null) {
                speakTimer.stop();
            }

            currentRound++;
            currentPlayerIndex = 0;
            isDescribePhase = true;
            speakStartTime = System.currentTimeMillis();

            // 找到第一个未被淘汰的玩家
            while (currentPlayerIndex < players.size() &&
                    eliminatedPlayers.contains(players.get(currentPlayerIndex))) {
                currentPlayerIndex++;
            }

            // 重置发言状态
            for (String player : players) {
                hasSpoken.put(player, false);
            }
        }
    }

    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("服务器已启动，监听端口：" + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler extends Thread {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private int currentRoomId = -1;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        // 投票处理
        private void handleVote(String voter, String target) {
            if (currentRoomId == -1 || !rooms.containsKey(currentRoomId)) {
                return;
            }

            RoomInfo roomInfo = rooms.get(currentRoomId);

            // 检查投票者是否被淘汰
            if (roomInfo.eliminatedPlayers.contains(voter)) {
                return;
            }

            // 确保在投票阶段
            if (!roomInfo.isDescribePhase) {
                // 检查是否已投票
                if (roomInfo.votes.containsKey(voter)) {
                    return;
                }

                // 检查目标是否有效（未被淘汰）
                if (roomInfo.players.contains(target) &&
                        !roomInfo.eliminatedPlayers.contains(target) &&
                        !voter.equals(target)) {

                    // 如果该玩家之前已经投过票，先撤销之前的投票
                    if (roomInfo.votes.containsKey(voter)) {
                        String previousVote = roomInfo.votes.get(voter);
                        int previousCount = roomInfo.voteCounts.getOrDefault(previousVote, 0) - 1;
                        roomInfo.voteCounts.put(previousVote, previousCount);
                    }

                    // 记录新投票
                    roomInfo.votes.put(voter, target);
                    int newCount = roomInfo.voteCounts.getOrDefault(target, 0) + 1;
                    roomInfo.voteCounts.put(target, newCount);

                    // 只告诉投票者他们投了谁
                    out.println("VOTE_CONFIRM:" + target);
                }
            }
        }

        // 结束投票方法（修复淘汰机制）
        private void endVoting(RoomInfo roomInfo) {
            roomInfo.votingTimer.stop();

            // 找出最高票数（只考虑未被淘汰玩家）
            int maxVotes = 0;
            List<String> candidates = new ArrayList<>();

            // 收集投票结果（只考虑未被淘汰玩家）
            Map<String, Integer> voteResults = new HashMap<>();
            for (String player : roomInfo.players) {
                if (roomInfo.eliminatedPlayers.contains(player))
                    continue;

                int votes = roomInfo.voteCounts.getOrDefault(player, 0);
                voteResults.put(player, votes);

                if (votes > maxVotes) {
                    maxVotes = votes;
                    candidates.clear();
                    candidates.add(player);
                } else if (votes == maxVotes) {
                    candidates.add(player);
                }
            }

            // 广播投票汇总
            broadcast(currentRoomId, "VOTE_SUMMARY:" + formatVoteResults(voteResults));

            // 处理淘汰
            String eliminated = null;
            if (candidates.size() == 1 && maxVotes > 0) {
                eliminated = candidates.get(0);

                // 添加到淘汰列表（不从players中移除）
                roomInfo.eliminatedPlayers.add(eliminated);

                boolean isSpy = roomInfo.playerRoles.get(eliminated);

                // 广播淘汰结果
                broadcast(currentRoomId, "ELIMINATED:" + eliminated + ":" + (isSpy ? "卧底" : "平民"));
                broadcast(currentRoomId, "PLAYER_ELIMINATED:" + eliminated);

                // 重置投票数据
                roomInfo.resetVoting();

                // 检查游戏是否结束
                checkGameEnd(roomInfo);
            } else {
                // 平票或无人投票
                broadcast(currentRoomId, "VOTING_RESULT:无人淘汰");
                roomInfo.resetVoting();
                startNextRound(roomInfo);
            }
        }

        // 辅助方法：格式化投票结果
        private String formatVoteResults(Map<String, Integer> voteResults) {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> entry : voteResults.entrySet()) {
                sb.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
            }
            return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
        }

        // 检查游戏结束条件（只考虑未被淘汰玩家）
        private void checkGameEnd(RoomInfo roomInfo) {
            int spyCount = 0;
            int civilianCount = 0;

            for (String player : roomInfo.players) {
                // 跳过淘汰玩家
                if (roomInfo.eliminatedPlayers.contains(player))
                    continue;

                if (roomInfo.playerRoles.get(player)) {
                    spyCount++;
                } else {
                    civilianCount++;
                }
            }

            if (spyCount == 0) {
                broadcast(currentRoomId, "GAME_OVER:平民胜利");
                roomInfo.resetGame();
            } else if (spyCount >= civilianCount) {
                broadcast(currentRoomId, "GAME_OVER:卧底胜利");
                roomInfo.resetGame();
            } else {
                startNextRound(roomInfo);
            }
        }

        // 开始下一轮（检查活跃玩家数量）
        private void startNextRound(RoomInfo roomInfo) {
            // 计算活跃玩家数量（未被淘汰）
            int activePlayers = roomInfo.players.size() - roomInfo.eliminatedPlayers.size();
            if (activePlayers <= 1) {
                checkGameEnd(roomInfo);
                return;
            }

            roomInfo.nextRound();
            broadcast(currentRoomId, "ROUND_START:" + roomInfo.currentRound);

            String nextSpeaker = roomInfo.getCurrentSpeaker();
            if (nextSpeaker != null) {
                broadcast(currentRoomId, "CURRENT_SPEAKER:" + nextSpeaker + ":" + roomInfo.SPEAK_TIME_LIMIT);
                startSpeakTimer(roomInfo);
            }
        }

        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String request;
                while ((request = in.readLine()) != null) {
                    String[] parts = request.split(":");
                    switch (parts[0]) {
                        case "CREATE":
                            handleCreateRoom(parts[1]);
                            break;
                        case "JOIN":
                            handleJoinRoom(Integer.parseInt(parts[1]), parts[2]);
                            break;
                        case "VALIDATE":
                            handleValidateRoom(Integer.parseInt(parts[1]));
                            break;
                        case "PREPARE":
                            System.out.print("preparemessage");
                            handlePrepare(parts[2]);
                            break;
                        case "START_GAME":
                            handleStartGame(parts[1]);
                            break;
                        case "SEND_MESSAGE":
                            handleReceiveMessage(parts[1], parts[2]);
                            break;
                        case "PLAYER_SPEAK":
                            handlePlayerSpeak(parts[1], parts[2]);
                            break;
                        case "FINISH_SPEAK":
                            handleFinishSpeak(parts[1]);
                            break;
                        case "VOTE":
                            handleVote(parts[1], parts[2]);
                            break;
                        case "EXIT":
                            handleExit();
                        case "LIST_ROOMS":
                            handleListRooms();
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                handleClientDisconnect();
            }
        }

        private void handleClientDisconnect() {
            if (currentRoomId != -1 && rooms.containsKey(currentRoomId)) {
                RoomInfo roomInfo = rooms.get(currentRoomId);
                roomInfo.players.remove(username);
                roomInfo.clientWriters.remove(out);

                if (roomInfo.readyStatus.getOrDefault(username, false)) {
                    roomInfo.readyCount--;
                    roomInfo.readyStatus.remove(username);
                }

                broadcast(currentRoomId, "PLAYER_LEFT:" + username);
                broadcast(currentRoomId, "READY_UPDATE:" + roomInfo.readyCount + ":" + roomInfo.players.size());

                if (roomInfo.players.isEmpty()) {
                    rooms.remove(currentRoomId);
                }
            }

            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void handleCreateRoom(String username) {
            this.username = username;
            int newRoomId;
            do {
                newRoomId = 1000 + random.nextInt(9000);
            } while (rooms.containsKey(newRoomId));

            RoomInfo roomInfo = new RoomInfo(username);
            roomInfo.clientWriters.add(out);
            rooms.put(newRoomId, roomInfo);
            currentRoomId = newRoomId;

            out.println("ROOM_CREATED:" + newRoomId);
            out.println("READY_UPDATE:0:" + roomInfo.players.size());
        }

        private void handleJoinRoom(int roomId, String username) {
            this.username = username;
            if (rooms.containsKey(roomId)) {
                RoomInfo roomInfo = rooms.get(roomId);
                if (roomInfo.players.size() == 3) {
                    out.println("ERROR:FULL");
                    return;
                }
                roomInfo.players.add(username);
                roomInfo.clientWriters.add(out);
                roomInfo.readyStatus.put(username, false);
                currentRoomId = roomId;

                broadcast(roomId, "PLAYER_JOINED:" + username);
                out.println("JOIN_SUCCESS:" + String.join(",", roomInfo.players));
                out.println("READY_UPDATE:" + roomInfo.readyCount + ":" + roomInfo.players.size());
                broadcast(roomId, "READY_UPDATE:" + roomInfo.readyCount + ":" + roomInfo.players.size());
            } else {
                out.println("ERROR:NO_EXIST");
            }
        }

        private void handleValidateRoom(int roomId) {
            out.println("VALIDATION_RESULT:" + rooms.containsKey(roomId));
        }

        private void handlePrepare(String username) {
            if (currentRoomId == -1 || !rooms.containsKey(currentRoomId))
                return;

            RoomInfo roomInfo = rooms.get(currentRoomId);
            boolean isCurrentlyReady = roomInfo.readyStatus.getOrDefault(username, false);
            boolean newReadyStatus = !isCurrentlyReady;
            roomInfo.readyStatus.put(username, newReadyStatus);

            if (newReadyStatus) {
                roomInfo.readyCount++;
            } else {
                roomInfo.readyCount--;
            }

            if (username.equals(roomInfo.players.get(0))) {
                roomInfo.isHostReady = newReadyStatus;
            }

            broadcast(currentRoomId, "PLAYER_READY:" + username + ":" + newReadyStatus);
            broadcast(currentRoomId, "READY_UPDATE:" + roomInfo.readyCount + ":" + roomInfo.players.size());
        }

        private void handleStartGame(String username) {
            if (currentRoomId == -1 || !rooms.containsKey(currentRoomId))
                return;

            RoomInfo roomInfo = rooms.get(currentRoomId);
            if (!username.equals(roomInfo.players.get(0))) {
                out.println("ERROR:只有房主可以开始游戏");
                return;
            }

            if (roomInfo.readyCount == roomInfo.players.size() && roomInfo.players.size() >= 3) {
                roomInfo.assignWordsAndRoles();
                roomInfo.gameStarted = true;

                broadcast(currentRoomId, "GAME_START:");

                for (String player : roomInfo.players) {
                    boolean isSpy = roomInfo.playerRoles.get(player);
                    String word = roomInfo.playerWords.get(player);
                    String role = isSpy ? "卧底" : "平民";

                    for (int i = 0; i < roomInfo.players.size(); i++) {
                        if (roomInfo.players.get(i).equals(player)) {
                            PrintWriter playerWriter = roomInfo.clientWriters.get(i);
                            playerWriter.println("WORD_ASSIGNED:" + word + ":" + role);
                            break;
                        }
                    }
                }

                String firstSpeaker = roomInfo.getCurrentSpeaker();
                if (firstSpeaker != null) {
                    broadcast(currentRoomId, "ROUND_START:" + roomInfo.currentRound);
                    broadcast(currentRoomId, "CURRENT_SPEAKER:" + firstSpeaker + ":" + roomInfo.SPEAK_TIME_LIMIT);
                    startSpeakTimer(roomInfo);
                }
            } else {
                out.println("ERROR:PREPARE");
            }
        }

        private void handleReceiveMessage(String message, String username) {
            if (currentRoomId != -1 && rooms.containsKey(currentRoomId)) {
                broadcast(currentRoomId, "SEND_MESSAGE:" + username + ":" + message);
            }
        }

        private void handlePlayerSpeak(String message, String username) {
            if (currentRoomId == -1 || !rooms.containsKey(currentRoomId)) {
                return;
            }

            RoomInfo roomInfo = rooms.get(currentRoomId);

            if (roomInfo.eliminatedPlayers.contains(username)) {
                out.println("ERROR:您已被淘汰，不能发言");
                return;
            }

            if (!roomInfo.gameStarted) {
                out.println("ERROR:游戏尚未开始");
                return;
            }

            String currentSpeaker = roomInfo.getCurrentSpeaker();
            if (!username.equals(currentSpeaker)) {
                out.println("ERROR:现在不是您的发言时间");
                return;
            }

            roomInfo.hasSpoken.put(username, true);
            message = message.replace("\n", " ").replace("\r", "").trim();
            broadcast(currentRoomId, "PLAYER_SPEAK:" + username + ":" + message);
        }

        private void handleFinishSpeak(String username) {
            if (currentRoomId == -1 || !rooms.containsKey(currentRoomId)) {
                return;
            }

            RoomInfo roomInfo = rooms.get(currentRoomId);
            if (!roomInfo.gameStarted) {
                return;
            }

            String currentSpeaker = roomInfo.getCurrentSpeaker();
            if (!username.equals(currentSpeaker)) {
                out.println("ERROR:现在不是您的发言时间");
                return;
            }

            broadcast(currentRoomId, "SPEAK_FINISHED:" + username);
            switchToNextPlayer(roomInfo);
        }

        private void switchToNextPlayer(RoomInfo roomInfo) {
            boolean hasNext = roomInfo.nextPlayer();
            if (hasNext) {
                String nextSpeaker = roomInfo.getCurrentSpeaker();
                if (nextSpeaker != null) {
                    broadcast(currentRoomId, "CURRENT_SPEAKER:" + nextSpeaker + ":" + roomInfo.SPEAK_TIME_LIMIT);
                    startSpeakTimer(roomInfo);
                }
            } else {
                roomInfo.isDescribePhase = false;
                roomInfo.resetVoting();
                broadcast(currentRoomId, "VOTING_START:" + roomInfo.currentRound);
                startVotingTimer(roomInfo);
            }
        }

        private void startSpeakTimer(RoomInfo roomInfo) {
            if (roomInfo.speakTimer != null) {
                roomInfo.speakTimer.stop();
            }

            roomInfo.speakTimer = new Timer(1000, null);
            roomInfo.speakTimer.addActionListener(e -> {
                long elapsed = System.currentTimeMillis() - roomInfo.speakStartTime;
                long remaining = roomInfo.SPEAK_TIME_LIMIT - elapsed;

                if (remaining <= 0) {
                    roomInfo.speakTimer.stop();
                    broadcast(currentRoomId, "SPEAK_TIMEOUT:" + roomInfo.getCurrentSpeaker());
                    switchToNextPlayer(roomInfo);
                } else {
                    int remainingSeconds = (int) (remaining / 1000);
                    broadcast(currentRoomId, "TIME_UPDATE:" + remainingSeconds);
                }
            });

            roomInfo.speakTimer.start();
        }

        private void startVotingTimer(RoomInfo roomInfo) {
            if (roomInfo.votingTimer != null) {
                roomInfo.votingTimer.stop();
            }

            roomInfo.votingTimer = new Timer(1000, null);
            final int[] remaining = { roomInfo.VOTING_TIME_LIMIT / 1000 };

            roomInfo.votingTimer.addActionListener(e -> {
                broadcast(currentRoomId, "VOTING_TIME:" + remaining[0]);

                if (remaining[0] <= 0) {
                    endVoting(roomInfo);
                } else {
                    remaining[0]--;
                }
            });

            roomInfo.votingTimer.start();
        }

        private void handleExit() {
            if (currentRoomId == -1 || !rooms.containsKey(currentRoomId)) {
                return;
            }

            RoomInfo roomInfo = rooms.get(currentRoomId);
            if (roomInfo == null) {
                return;
            }

            // 记录是否是房主
            boolean wasHost = !roomInfo.players.isEmpty() &&
                    username.equals(roomInfo.players.get(0));

            // 从所有玩家相关集合中移除该玩家
            roomInfo.players.remove(username);
            roomInfo.clientWriters.remove(out);

            // 更新准备状态
            if (roomInfo.readyStatus.containsKey(username)) {
                boolean wasReady = roomInfo.readyStatus.remove(username);
                if (wasReady) {
                    roomInfo.readyCount--;
                }
            }

            // 如果游戏已经开始，清理游戏相关状态
            if (roomInfo.gameStarted) {
                roomInfo.playerWords.remove(username);
                roomInfo.playerRoles.remove(username);
                roomInfo.hasSpoken.remove(username);
                roomInfo.eliminatedPlayers.remove(username);
                roomInfo.votes.remove(username);
                roomInfo.voteCounts.remove(username);

                // 如果退出的是当前发言玩家，切换到下一个玩家
                if (roomInfo.getCurrentSpeaker() != null &&
                        roomInfo.getCurrentSpeaker().equals(username)) {
                    switchToNextPlayer(roomInfo);
                }
            }

            // 检查是否是房主退出
            String newHost = null;
            if (wasHost && !roomInfo.players.isEmpty()) {
                // 转让房主给下一个玩家
                newHost = roomInfo.players.get(0);
            }

            // 广播玩家退出消息
            broadcast(currentRoomId, "PLAYER_LEFT:" + username);
            broadcast(currentRoomId, "READY_UPDATE:" + roomInfo.readyCount + ":" + roomInfo.players.size());

            // 如果房主变更，广播新房主
            if (newHost != null) {
                broadcast(currentRoomId, "NEW_HOST:" + newHost);
            }

            // 检查游戏是否因玩家退出而结束
            if (roomInfo.gameStarted) {
                checkGameEnd(roomInfo);
            }

            // 如果房间为空，移除房间
            if (roomInfo.players.isEmpty()) {
                rooms.remove(currentRoomId);
            }

            // 重置当前房间ID
            currentRoomId = -1;
        }

        private void handleListRooms() {
            StringBuilder roomList = new StringBuilder("ROOM_LIST:");
            for (Map.Entry<Integer, RoomInfo> entry : rooms.entrySet()) {
                int roomId = entry.getKey();
                int playerCount = entry.getValue().players.size();
                // 只显示未满的房间
                if (playerCount < 8) {
                    roomList.append(roomId).append(",").append(playerCount).append(";");
                }
            }
            // 移除最后一个分号
            if (roomList.charAt(roomList.length() - 1) == ';') {
                roomList.setLength(roomList.length() - 1);
            }
            out.println(roomList.toString());
        }

        // 广播方法
        private void broadcast(int roomId, String message) {
            if (rooms.containsKey(roomId)) {
                RoomInfo roomInfo = rooms.get(roomId);
                for (PrintWriter writer : roomInfo.clientWriters) {
                    try {
                        writer.println(message);
                    } catch (Exception e) {
                        // 忽略发送失败的客户端
                    }
                }
            }
        }
    }
}