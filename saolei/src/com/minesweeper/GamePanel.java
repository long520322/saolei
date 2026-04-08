package com.minesweeper;

import javax.swing.*;
import javax.swing.border.TitledBorder;  // 添加这一行
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class GamePanel extends JPanel {
    // 难度配置
    private static final int EASY_ROWS = 9;
    private static final int EASY_COLS = 9;
    private static final int EASY_MINES = 10;

    private static final int MEDIUM_ROWS = 16;
    private static final int MEDIUM_COLS = 16;
    private static final int MEDIUM_MINES = 40;

    private static final int HARD_ROWS = 16;
    private static final int HARD_COLS = 30;
    private static final int HARD_MINES = 99;

    // 游戏模式
    private enum GameMode {
        CLASSIC, VS_COMPUTER
    }

    private GameMode currentMode = GameMode.CLASSIC;

    // 当前游戏配置
    private int currentRows = EASY_ROWS;
    private int currentCols = EASY_COLS;
    private int currentMines = EASY_MINES;
    private int tileSize = 45;

    private Tile[][] board;
    private boolean gameOver;
    private boolean gameWin;
    private JLabel statusLabel;
    private JLabel minesLabel;
    private int flagsPlaced;
    private JPanel gameBoardPanel;
    private JButton easyBtn, mediumBtn, hardBtn;
    private JButton resetButton, hintButton, modeButton;
    private JPanel skillPanel;

    // 人机对战相关
    private boolean isComputerTurn = false;
    private Random random = new Random();
    private javax.swing.Timer computerTimer;

    // 技能相关
    private List<Skill> skills;
    private JButton safeRevealBtn, doubleClickBtn, mineSweeperBtn, shieldBtn;
    private boolean shieldActive = false;

    public GamePanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(240, 240, 240));

        // 顶部面板
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // 技能面板
        createSkillPanel();
        add(skillPanel, BorderLayout.WEST);

        // 游戏面板容器
        gameBoardPanel = new JPanel(new GridBagLayout());
        gameBoardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        gameBoardPanel.setBackground(new Color(240, 240, 240));
        add(gameBoardPanel, BorderLayout.CENTER);

        // 初始化技能
        initSkills();

        // 初始化游戏
        initGameBoard();
        resetGame();
    }

    private void initSkills() {
        skills = new ArrayList<>();
        skills.add(new Skill("安全探测", "随机显示一个安全格子位置", new Color(100, 200, 100)));
        skills.add(new Skill("双倍点击", "一次翻开周围所有安全格子", new Color(200, 200, 100)));
        skills.add(new Skill("雷区扫描", "显示所有地雷位置3秒", new Color(200, 100, 100)));
        skills.add(new Skill("护盾", "免疫一次踩雷伤害", new Color(100, 150, 200)));
    }

    private void createSkillPanel() {
        skillPanel = new JPanel();
        skillPanel.setLayout(new BoxLayout(skillPanel, BoxLayout.Y_AXIS));
        skillPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "✨ 技能选择 (每局限用一次)",
                TitledBorder.CENTER,
                TitledBorder.TOP,
                new Font("Microsoft YaHei", Font.BOLD, 12)
        ));
        skillPanel.setPreferredSize(new Dimension(150, 300));
        skillPanel.setBackground(new Color(250, 250, 240));
        skillPanel.setMaximumSize(new Dimension(150, 400));

        // 安全探测技能
        safeRevealBtn = createSkillButton(
                "🔍 安全探测",
                "随机显示一个安全格子位置",
                new Color(100, 200, 100),
                e -> useSafeReveal()
        );

        // 双倍点击技能
        doubleClickBtn = createSkillButton(
                "⚡ 双倍点击",
                "一次翻开周围所有安全格子",
                new Color(200, 200, 100),
                e -> useDoubleClick()
        );

        // 雷区扫描技能
        mineSweeperBtn = createSkillButton(
                "📡 雷区扫描",
                "显示所有地雷位置3秒",
                new Color(200, 100, 100),
                e -> useMineSweeper()
        );

        // 护盾技能
        shieldBtn = createSkillButton(
                "🛡️ 护盾",
                "免疫一次踩雷伤害",
                new Color(100, 150, 200),
                e -> useShield()
        );

        skillPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        skillPanel.add(safeRevealBtn);
        skillPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        skillPanel.add(doubleClickBtn);
        skillPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        skillPanel.add(mineSweeperBtn);
        skillPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        skillPanel.add(shieldBtn);
        skillPanel.add(Box.createVerticalGlue());
    }

    private JButton createSkillButton(String name, String tooltip, Color color, java.awt.event.ActionListener listener) {
        JButton button = new JButton(name);
        button.setToolTipText(tooltip);
        button.setBackground(color);
        button.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        button.setPreferredSize(new Dimension(130, 40));
        button.setMaximumSize(new Dimension(130, 40));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(listener);
        return button;
    }

    // 技能1：安全探测 - 显示一个安全格子
    private void useSafeReveal() {
        if (gameOver || gameWin) {
            JOptionPane.showMessageDialog(this, "游戏已经结束，无法使用技能！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (currentMode == GameMode.VS_COMPUTER && isComputerTurn) {
            JOptionPane.showMessageDialog(this, "现在是电脑回合，无法使用技能！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Skill skill = skills.get(0);
        if (skill.isUsed()) {
            JOptionPane.showMessageDialog(this, "本局游戏已经使用过该技能！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 寻找安全格子
        List<Point> safeMoves = new ArrayList<>();
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                Tile tile = board[i][j];
                if (!tile.isRevealed() && !tile.isFlagged() && !tile.isMine()) {
                    safeMoves.add(new Point(i, j));
                }
            }
        }

        if (safeMoves.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有安全的格子了！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        skill.setUsed(true);
        safeRevealBtn.setEnabled(false);
        safeRevealBtn.setBackground(Color.GRAY);

        Point safe = safeMoves.get(random.nextInt(safeMoves.size()));
        Tile tile = board[safe.x][safe.y];

        // 高亮显示安全格子
        final Color originalBg = tile.getBackground();
        tile.setBackground(Color.GREEN);

        javax.swing.Timer timer = new javax.swing.Timer(3000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!tile.isRevealed()) {
                    tile.setBackground(originalBg);
                }
            }
        });
        timer.setRepeats(false);
        timer.start();

        statusLabel.setText("技能：安全探测 - 第" + (safe.x + 1) + "行，第" + (safe.y + 1) + "列是安全的");

        javax.swing.Timer statusTimer = new javax.swing.Timer(3000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateStatusLabel();
            }
        });
        statusTimer.setRepeats(false);
        statusTimer.start();
    }

    // 技能2：双倍点击 - 翻开周围所有安全格子
    private void useDoubleClick() {
        if (gameOver || gameWin) {
            JOptionPane.showMessageDialog(this, "游戏已经结束，无法使用技能！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (currentMode == GameMode.VS_COMPUTER && isComputerTurn) {
            JOptionPane.showMessageDialog(this, "现在是电脑回合，无法使用技能！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Skill skill = skills.get(1);
        if (skill.isUsed()) {
            JOptionPane.showMessageDialog(this, "本局游戏已经使用过该技能！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 寻找一个已翻开的数字格子
        List<Point> revealedNumbers = new ArrayList<>();
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                Tile tile = board[i][j];
                if (tile.isRevealed() && tile.getAdjacentMines() > 0) {
                    revealedNumbers.add(new Point(i, j));
                }
            }
        }

        if (revealedNumbers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "没有可以触发双倍点击的格子！先翻开一些格子吧！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        skill.setUsed(true);
        doubleClickBtn.setEnabled(false);
        doubleClickBtn.setBackground(Color.GRAY);

        int revealed = 0;
        for (Point p : revealedNumbers) {
            List<Point> neighbors = getNeighbors(p.x, p.y);
            for (Point n : neighbors) {
                Tile neighborTile = board[n.x][n.y];
                if (!neighborTile.isRevealed() && !neighborTile.isFlagged() && !neighborTile.isMine()) {
                    revealTile(n.x, n.y);
                    revealed++;
                }
            }
        }

        statusLabel.setText("技能：双倍点击 - 翻开了 " + revealed + " 个安全格子");

        checkWin();

        javax.swing.Timer statusTimer = new javax.swing.Timer(3000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateStatusLabel();
            }
        });
        statusTimer.setRepeats(false);
        statusTimer.start();
    }

    // 技能3：雷区扫描 - 显示所有地雷位置
    private void useMineSweeper() {
        if (gameOver || gameWin) {
            JOptionPane.showMessageDialog(this, "游戏已经结束，无法使用技能！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Skill skill = skills.get(2);
        if (skill.isUsed()) {
            JOptionPane.showMessageDialog(this, "本局游戏已经使用过该技能！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        skill.setUsed(true);
        mineSweeperBtn.setEnabled(false);
        mineSweeperBtn.setBackground(Color.GRAY);

        // 记录所有地雷格子的原始背景色
        List<Tile> mineTiles = new ArrayList<>();
        List<Color> originalColors = new ArrayList<>();

        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                if (board[i][j].isMine() && !board[i][j].isRevealed()) {
                    mineTiles.add(board[i][j]);
                    originalColors.add(board[i][j].getBackground());
                    board[i][j].setBackground(Color.RED);
                }
            }
        }

        statusLabel.setText("技能：雷区扫描 - 显示 " + mineTiles.size() + " 个地雷位置");

        // 3秒后恢复
        javax.swing.Timer timer = new javax.swing.Timer(3000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < mineTiles.size(); i++) {
                    Tile t = mineTiles.get(i);
                    if (!t.isRevealed()) {
                        t.setBackground(originalColors.get(i));
                    }
                }
                updateStatusLabel();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    // 技能4：护盾 - 免疫一次踩雷
    private void useShield() {
        if (gameOver || gameWin) {
            JOptionPane.showMessageDialog(this, "游戏已经结束，无法使用技能！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Skill skill = skills.get(3);
        if (skill.isUsed()) {
            JOptionPane.showMessageDialog(this, "本局游戏已经使用过该技能！", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }

        skill.setUsed(true);
        shieldActive = true;
        shieldBtn.setEnabled(false);
        shieldBtn.setBackground(Color.GRAY);

        statusLabel.setText("技能：护盾 - 已激活，免疫下一次踩雷伤害");

        javax.swing.Timer statusTimer = new javax.swing.Timer(3000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateStatusLabel();
            }
        });
        statusTimer.setRepeats(false);
        statusTimer.start();

        // 护盾视觉效果 - 边框变蓝
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                if (!board[i][j].isRevealed()) {
                    board[i][j].setBorder(BorderFactory.createLineBorder(new Color(100, 150, 255), 2));
                }
            }
        }

        // 3秒后恢复边框
        javax.swing.Timer borderTimer = new javax.swing.Timer(3000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < currentRows; i++) {
                    for (int j = 0; j < currentCols; j++) {
                        if (!board[i][j].isRevealed()) {
                            board[i][j].setBorder(BorderFactory.createLineBorder(Color.GRAY));
                        }
                    }
                }
            }
        });
        borderTimer.setRepeats(false);
        borderTimer.start();
    }

    private void updateStatusLabel() {
        if (currentMode == GameMode.VS_COMPUTER) {
            if (isComputerTurn) {
                statusLabel.setText("人机对战模式 - 电脑思考中...");
            } else {
                statusLabel.setText("人机对战模式 - 轮到你了");
            }
        } else {
            statusLabel.setText("游戏进行中");
        }
        statusLabel.setForeground(Color.BLACK);
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 12));
        topPanel.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));
        topPanel.setBackground(new Color(240, 240, 240));

        statusLabel = new JLabel("游戏进行中");
        statusLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 18));

        minesLabel = new JLabel("剩余地雷: " + currentMines);
        minesLabel.setFont(new Font("Microsoft YaHei", Font.BOLD, 18));

        resetButton = new JButton("新游戏");
        resetButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        resetButton.setPreferredSize(new Dimension(90, 38));
        resetButton.addActionListener(e -> resetGame());

        hintButton = new JButton("💡 提示");
        hintButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        hintButton.setPreferredSize(new Dimension(90, 38));
        hintButton.setBackground(new Color(255, 255, 200));
        hintButton.addActionListener(e -> showHint());

        modeButton = new JButton("🤖 人机对战");
        modeButton.setFont(new Font("Microsoft YaHei", Font.BOLD, 14));
        modeButton.setPreferredSize(new Dimension(110, 38));
        modeButton.setBackground(new Color(200, 220, 255));
        modeButton.addActionListener(e -> toggleGameMode());

        easyBtn = new JButton("简单");
        easyBtn.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        easyBtn.setPreferredSize(new Dimension(70, 35));
        easyBtn.setBackground(new Color(200, 230, 200));
        easyBtn.addActionListener(e -> setDifficulty(EASY_ROWS, EASY_COLS, EASY_MINES));

        mediumBtn = new JButton("中等");
        mediumBtn.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        mediumBtn.setPreferredSize(new Dimension(70, 35));
        mediumBtn.setBackground(new Color(230, 230, 180));
        mediumBtn.addActionListener(e -> setDifficulty(MEDIUM_ROWS, MEDIUM_COLS, MEDIUM_MINES));

        hardBtn = new JButton("困难");
        hardBtn.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        hardBtn.setPreferredSize(new Dimension(70, 35));
        hardBtn.setBackground(new Color(230, 200, 200));
        hardBtn.addActionListener(e -> setDifficulty(HARD_ROWS, HARD_COLS, HARD_MINES));

        topPanel.add(statusLabel);
        topPanel.add(resetButton);
        topPanel.add(hintButton);
        topPanel.add(modeButton);
        topPanel.add(easyBtn);
        topPanel.add(mediumBtn);
        topPanel.add(hardBtn);
        topPanel.add(minesLabel);

        return topPanel;
    }

    private void initGameBoard() {
        gameBoardPanel.removeAll();
        adjustTileSize();

        JPanel boardContainer = new JPanel(new GridBagLayout());
        boardContainer.setBackground(new Color(240, 240, 240));

        JPanel boardPanel = new JPanel(new GridLayout(currentRows, currentCols));

        int boardWidth = currentCols * tileSize;
        int boardHeight = currentRows * tileSize;
        boardPanel.setPreferredSize(new Dimension(boardWidth, boardHeight));
        boardPanel.setMinimumSize(new Dimension(boardWidth, boardHeight));
        boardPanel.setMaximumSize(new Dimension(boardWidth, boardHeight));
        boardPanel.setBackground(Color.DARK_GRAY);

        board = new Tile[currentRows][currentCols];

        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                board[i][j] = new Tile(i, j);
                board[i][j].setPreferredSize(new Dimension(tileSize, tileSize));
                board[i][j].setMinimumSize(new Dimension(tileSize, tileSize));
                board[i][j].setMaximumSize(new Dimension(tileSize, tileSize));
                board[i][j].setFont(new Font("Segoe UI", Font.BOLD, Math.max(16, tileSize / 2)));

                final int row = i;
                final int col = j;

                board[i][j].addActionListener(e -> onTileClick(row, col));
                board[i][j].addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e)) {
                            onTileRightClick(row, col);
                        }
                    }
                });
                boardPanel.add(board[i][j]);
            }
        }

        boardContainer.add(boardPanel);
        gameBoardPanel.add(boardContainer);

        gameBoardPanel.revalidate();
        gameBoardPanel.repaint();

        SwingUtilities.invokeLater(() -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.pack();
                window.setLocationRelativeTo(null);
            }
        });
    }

    private void adjustTileSize() {
        if (currentRows <= 9 && currentCols <= 9) {
            tileSize = 45;
        } else if (currentRows <= 16 && currentCols <= 16) {
            tileSize = 38;
        } else {
            tileSize = 32;
        }
    }

    private void toggleGameMode() {
        if (currentMode == GameMode.CLASSIC) {
            currentMode = GameMode.VS_COMPUTER;
            modeButton.setText("👤 经典模式");
            modeButton.setBackground(new Color(220, 220, 200));
            statusLabel.setText("人机对战模式 - 轮到你了");
            isComputerTurn = false;
        } else {
            currentMode = GameMode.CLASSIC;
            modeButton.setText("🤖 人机对战");
            modeButton.setBackground(new Color(200, 220, 255));
            statusLabel.setText("经典模式 - 游戏进行中");
            isComputerTurn = false;
        }

        resetGame();
    }

    private void setDifficulty(int rows, int cols, int mines) {
        this.currentRows = rows;
        this.currentCols = cols;
        this.currentMines = mines;

        easyBtn.setBackground(new Color(200, 230, 200));
        mediumBtn.setBackground(new Color(230, 230, 180));
        hardBtn.setBackground(new Color(230, 200, 200));

        if (currentRows == EASY_ROWS && currentCols == EASY_COLS) {
            easyBtn.setBackground(new Color(100, 200, 100));
        } else if (currentRows == MEDIUM_ROWS && currentCols == MEDIUM_COLS) {
            mediumBtn.setBackground(new Color(200, 200, 100));
        } else if (currentRows == HARD_ROWS && currentCols == HARD_COLS) {
            hardBtn.setBackground(new Color(200, 100, 100));
        }

        initGameBoard();
        resetGame();
    }

    private void resetGameState() {
        gameOver = false;
        gameWin = false;
        flagsPlaced = 0;
        isComputerTurn = false;
        shieldActive = false;

        // 重置所有技能
        for (Skill skill : skills) {
            skill.setUsed(false);
        }
        safeRevealBtn.setEnabled(true);
        safeRevealBtn.setBackground(new Color(100, 200, 100));
        doubleClickBtn.setEnabled(true);
        doubleClickBtn.setBackground(new Color(200, 200, 100));
        mineSweeperBtn.setEnabled(true);
        mineSweeperBtn.setBackground(new Color(200, 100, 100));
        shieldBtn.setEnabled(true);
        shieldBtn.setBackground(new Color(100, 150, 200));

        if (computerTimer != null) {
            computerTimer.stop();
            computerTimer = null;
        }

        updateMinesLabel();

        if (currentMode == GameMode.VS_COMPUTER) {
            statusLabel.setText("人机对战模式 - 轮到你了");
        } else {
            statusLabel.setText("游戏进行中");
        }
        statusLabel.setForeground(Color.BLACK);
    }

    private void initGameData() {
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                board[i][j].reset();
            }
        }

        Random rand = new Random();
        int minesPlaced = 0;
        while (minesPlaced < currentMines) {
            int row = rand.nextInt(currentRows);
            int col = rand.nextInt(currentCols);
            if (!board[row][col].isMine()) {
                board[row][col].setMine(true);
                minesPlaced++;
            }
        }

        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                if (!board[i][j].isMine()) {
                    int count = 0;
                    for (int di = -1; di <= 1; di++) {
                        for (int dj = -1; dj <= 1; dj++) {
                            if (di == 0 && dj == 0) continue;
                            int ni = i + di;
                            int nj = j + dj;
                            if (ni >= 0 && ni < currentRows && nj >= 0 && nj < currentCols) {
                                if (board[ni][nj].isMine()) count++;
                            }
                        }
                    }
                    board[i][j].setAdjacentMines(count);
                }
            }
        }
    }

    private void onTileClick(int row, int col) {
        if (gameOver || gameWin) return;

        if (currentMode == GameMode.VS_COMPUTER && isComputerTurn) {
            statusLabel.setText("请等待电脑思考...");
            return;
        }

        Tile tile = board[row][col];
        if (tile.isFlagged() || tile.isRevealed()) return;

        if (tile.isMine()) {
            // 护盾技能：免疫一次踩雷
            if (shieldActive) {
                shieldActive = false;
                statusLabel.setText("护盾生效！免疫了这次地雷伤害！");

                javax.swing.Timer statusTimer = new javax.swing.Timer(2000, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        updateStatusLabel();
                    }
                });
                statusTimer.setRepeats(false);
                statusTimer.start();

                // 标记这个地雷为已处理（但游戏继续）
                tile.setFlagged(true);
                tile.setText("🛡️");
                flagsPlaced++;
                updateMinesLabel();
                return;
            }

            gameOver = true;
            if (currentMode == GameMode.VS_COMPUTER) {
                statusLabel.setText("你踩到地雷！电脑赢了！");
            } else {
                statusLabel.setText("游戏结束！你输了！");
            }
            statusLabel.setForeground(Color.RED);
            revealAllMines();
            return;
        }

        revealTile(row, col);

        if (checkWin()) return;

        if (currentMode == GameMode.VS_COMPUTER && !gameOver && !gameWin) {
            isComputerTurn = true;
            statusLabel.setText("人机对战模式 - 电脑思考中...");
            statusLabel.setForeground(new Color(0, 100, 200));
            startComputerTurn();
        }
    }

    private void revealTile(int row, int col) {
        Tile tile = board[row][col];
        if (tile.isRevealed() || tile.isFlagged()) return;

        tile.reveal();

        if (tile.getAdjacentMines() == 0) {
            for (int di = -1; di <= 1; di++) {
                for (int dj = -1; dj <= 1; dj++) {
                    if (di == 0 && dj == 0) continue;
                    int ni = row + di;
                    int nj = col + dj;
                    if (ni >= 0 && ni < currentRows && nj >= 0 && nj < currentCols) {
                        revealTile(ni, nj);
                    }
                }
            }
        }
    }

    private void revealAllMines() {
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                if (board[i][j].isMine()) {
                    board[i][j].reveal();
                }
            }
        }
    }

    private boolean checkWin() {
        int unrevealedSafe = 0;
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                if (!board[i][j].isRevealed() && !board[i][j].isMine()) {
                    unrevealedSafe++;
                }
            }
        }

        if (unrevealedSafe == 0) {
            gameWin = true;
            if (currentMode == GameMode.VS_COMPUTER) {
                statusLabel.setText("恭喜！你赢了电脑！");
            } else {
                statusLabel.setText("恭喜！你赢了！");
            }
            statusLabel.setForeground(new Color(0, 150, 0));

            for (int i = 0; i < currentRows; i++) {
                for (int j = 0; j < currentCols; j++) {
                    if (board[i][j].isMine()) {
                        board[i][j].setFlagged(true);
                        board[i][j].setText("🚩");
                    }
                }
            }
            return true;
        }
        return false;
    }

    private void onTileRightClick(int row, int col) {
        if (gameOver || gameWin) return;

        if (currentMode == GameMode.VS_COMPUTER && isComputerTurn) return;

        Tile tile = board[row][col];
        if (tile.isRevealed()) return;

        if (!tile.isFlagged() && flagsPlaced < currentMines) {
            tile.setFlagged(true);
            tile.setText("🚩");
            flagsPlaced++;
        } else if (tile.isFlagged()) {
            tile.setFlagged(false);
            tile.setText("");
            flagsPlaced--;
        }
        updateMinesLabel();
    }

    private void startComputerTurn() {
        if (gameOver || gameWin || !isComputerTurn) return;

        computerTimer = new javax.swing.Timer(800, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameOver || gameWin || !isComputerTurn) return;

                Point move = getComputerMove();

                if (move != null) {
                    Tile tile = board[move.x][move.y];
                    Color originalBg = tile.getBackground();
                    tile.setBackground(new Color(100, 200, 255));

                    javax.swing.Timer clickTimer = new javax.swing.Timer(400, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent evt) {
                            if (!gameOver && !gameWin && isComputerTurn) {
                                if (board[move.x][move.y].isMine()) {
                                    gameOver = true;
                                    statusLabel.setText("电脑踩到地雷！你赢了！");
                                    statusLabel.setForeground(Color.RED);
                                    revealAllMines();
                                } else {
                                    revealTile(move.x, move.y);
                                    checkWin();
                                }

                                isComputerTurn = false;
                                if (!gameOver && !gameWin) {
                                    statusLabel.setText("人机对战模式 - 轮到你了");
                                    statusLabel.setForeground(Color.BLACK);
                                }
                            }
                        }
                    });
                    clickTimer.setRepeats(false);
                    clickTimer.start();
                } else {
                    isComputerTurn = false;
                    if (!gameOver && !gameWin) {
                        statusLabel.setText("人机对战模式 - 轮到你了");
                        statusLabel.setForeground(Color.BLACK);
                    }
                }
            }
        });
        computerTimer.setRepeats(false);
        computerTimer.start();
    }

    private Point getComputerMove() {
        List<Point> safeMoves = new ArrayList<>();
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                Tile tile = board[i][j];
                if (!tile.isRevealed() && !tile.isFlagged() && !tile.isMine()) {
                    safeMoves.add(new Point(i, j));
                }
            }
        }

        if (!safeMoves.isEmpty()) {
            return safeMoves.get(random.nextInt(safeMoves.size()));
        }

        List<Point> allUnrevealed = new ArrayList<>();
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                if (!board[i][j].isRevealed() && !board[i][j].isFlagged()) {
                    allUnrevealed.add(new Point(i, j));
                }
            }
        }

        return allUnrevealed.isEmpty() ? null : allUnrevealed.get(0);
    }

    private List<Point> getNeighbors(int row, int col) {
        List<Point> neighbors = new ArrayList<>();
        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                if (di == 0 && dj == 0) continue;
                int ni = row + di;
                int nj = col + dj;
                if (ni >= 0 && ni < currentRows && nj >= 0 && nj < currentCols) {
                    neighbors.add(new Point(ni, nj));
                }
            }
        }
        return neighbors;
    }

    private void showHint() {
        if (gameOver || gameWin) return;

        if (currentMode == GameMode.VS_COMPUTER && isComputerTurn) return;

        List<Point> safeMoves = new ArrayList<>();
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                Tile tile = board[i][j];
                if (!tile.isRevealed() && !tile.isFlagged() && !tile.isMine()) {
                    safeMoves.add(new Point(i, j));
                }
            }
        }

        if (safeMoves.isEmpty()) return;

        Point hint = safeMoves.get(random.nextInt(safeMoves.size()));
        Tile tile = board[hint.x][hint.y];

        final Color originalBg = tile.getBackground();
        tile.setBackground(Color.YELLOW);

        javax.swing.Timer timer = new javax.swing.Timer(2000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!tile.isRevealed()) {
                    tile.setBackground(originalBg);
                }
            }
        });
        timer.setRepeats(false);
        timer.start();

        statusLabel.setText("提示：第" + (hint.x + 1) + "行，第" + (hint.y + 1) + "列是安全的");

        javax.swing.Timer statusTimer = new javax.swing.Timer(3000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateStatusLabel();
            }
        });
        statusTimer.setRepeats(false);
        statusTimer.start();
    }

    private void updateMinesLabel() {
        int remaining = currentMines - flagsPlaced;
        minesLabel.setText("剩余地雷: " + remaining);
        minesLabel.setForeground(remaining < 0 ? Color.RED : Color.BLACK);
    }

    public void resetGame() {
        if (computerTimer != null) {
            computerTimer.stop();
            computerTimer = null;
        }

        resetGameState();
        initGameData();
        gameBoardPanel.revalidate();
        gameBoardPanel.repaint();

        SwingUtilities.invokeLater(() -> {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.pack();
                window.setLocationRelativeTo(null);
            }
        });
    }
}