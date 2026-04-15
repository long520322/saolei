package com.minesweeper;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

public class GamePanel extends JPanel {
    // 难度配置
    private static final int EASY_ROWS = 9, EASY_COLS = 9, EASY_MINES = 10;
    private static final int MEDIUM_ROWS = 16, MEDIUM_COLS = 16, MEDIUM_MINES = 40;
    private static final int HARD_ROWS = 16, HARD_COLS = 30, HARD_MINES = 99;

    private enum GameMode { CLASSIC, VS_COMPUTER }
    private enum DifficultyLevel { EASY, MEDIUM, HARD }

    private GameMode currentMode = GameMode.CLASSIC;
    private DifficultyLevel currentDifficulty = DifficultyLevel.EASY;
    private int currentRows = EASY_ROWS, currentCols = EASY_COLS;
    private int currentMines = EASY_MINES, tileSize = 45;
    private Tile[][] board;
    private boolean gameOver, gameWin;
    private JLabel statusLabel, minesLabel, timerLabel;
    private int flagsPlaced, elapsedSeconds;
    private javax.swing.Timer gameTimer;
    private javax.swing.Timer computerTimer;
    private Random random = new Random();
    private boolean isComputerTurn = false;
    private boolean shieldActive = false;
    private List<Skill> skills;
    private JButton safeRevealBtn, doubleClickBtn, mineSweeperBtn, shieldBtn;
    private JButton modeBtn, easyBtn, mediumBtn, hardBtn, soundBtn;
    private JPanel boardContainer;  // 游戏面板容器
    private SoundService soundService;

    // 特效系统
    private java.util.List<Particle> particles;
    private java.util.List<Shockwave> shockwaves;
    private javax.swing.Timer particleTimer;
    private List<Point> chainExplosions;
    private javax.swing.Timer chainTimer;
    private int currentChainIndex;

    // 冲击波类
    private class Shockwave {
        int x, y;
        float radius;
        int maxRadius;
        int alpha;

        Shockwave(int x, int y) {
            this.x = x;
            this.y = y;
            this.radius = 5;
            this.maxRadius = 100;
            this.alpha = 200;
        }

        void update() {
            radius += 10;
            alpha -= 20;
        }

        void draw(Graphics2D g2d) {
            if (alpha > 0) {
                g2d.setColor(new Color(255, 100, 50, Math.min(255, alpha)));
                g2d.setStroke(new BasicStroke(3));
                g2d.drawOval(x - (int)radius, y - (int)radius, (int)radius * 2, (int)radius * 2);

                g2d.setColor(new Color(255, 200, 50, Math.min(255, alpha / 2)));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawOval(x - (int)(radius * 0.6f), y - (int)(radius * 0.6f),
                        (int)(radius * 1.2f), (int)(radius * 1.2f));
            }
        }

        boolean isAlive() { return alpha > 0 && radius < maxRadius; }
    }

    // 粒子类
    private class Particle {
        int x, y;
        int vx, vy;
        int size;
        Color color;
        int life;
        int maxLife;
        boolean isSpark;

        Particle(int x, int y, Color color, boolean isSpark) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.isSpark = isSpark;
            this.size = isSpark ? 2 + random.nextInt(4) : 4 + random.nextInt(6);
            this.vx = -6 + random.nextInt(13);
            this.vy = -8 + random.nextInt(17);
            this.maxLife = isSpark ? 20 + random.nextInt(15) : 35 + random.nextInt(25);
            this.life = this.maxLife;
        }

        void update() {
            x += vx;
            y += vy;
            vy += 1;
            life--;
        }

        void draw(Graphics2D g2d) {
            int alpha = (int)(255 * ((float)life / maxLife));
            g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.min(255, alpha)));
            if (isSpark) {
                g2d.fillRect(x, y, size, size);
            } else {
                g2d.fillOval(x, y, size, size);
            }
        }

        boolean isAlive() { return life > 0; }
    }

    public GamePanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(30, 30, 35));

        // 初始化音效
        soundService = SoundService.getInstance();
        soundService.preload();

        particles = new ArrayList<>();
        shockwaves = new ArrayList<>();
        chainExplosions = new ArrayList<>();
        initSkills();
        initUI();
        initGame();

        // 粒子动画定时器
        particleTimer = new javax.swing.Timer(16, e -> {
            for (Particle p : particles) p.update();
            particles.removeIf(p -> !p.isAlive());
            for (Shockwave s : shockwaves) s.update();
            shockwaves.removeIf(s -> !s.isAlive());
            repaint();
        });
        particleTimer.start();
    }

    private void initSkills() {
        skills = new ArrayList<>();
        skills.add(new Skill("安全探测", "显示一个安全格子", new Color(100, 200, 100)));
        skills.add(new Skill("双倍点击", "翻开周围安全格子", new Color(200, 200, 100)));
        skills.add(new Skill("雷区扫描", "显示所有地雷", new Color(200, 100, 100)));
        skills.add(new Skill("护盾", "免疫一次踩雷", new Color(100, 150, 200)));
    }

    private void initUI() {
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        JPanel leftPanel = createSkillPanel();
        add(leftPanel, BorderLayout.WEST);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(new Color(30, 30, 35));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        boardContainer = new JPanel(new GridBagLayout());
        boardContainer.setBackground(new Color(30, 30, 35));
        centerPanel.add(boardContainer);
        add(centerPanel, BorderLayout.CENTER);

        createBoardPanel();
    }

    private void createBoardPanel() {
        boardContainer.removeAll();
        adjustTileSize();

        JPanel boardPanel = new JPanel(new GridLayout(currentRows, currentCols, 2, 2));
        boardPanel.setBackground(new Color(50, 50, 55));
        boardPanel.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 65), 2));

        board = new Tile[currentRows][currentCols];
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                board[i][j] = new Tile(i, j);
                final int row = i, col = j;
                board[i][j].addActionListener(e -> onTileClick(row, col));
                board[i][j].addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e)) onTileRightClick(row, col);
                    }
                });
                boardPanel.add(board[i][j]);
            }
        }

        boardContainer.add(boardPanel);
        boardContainer.revalidate();
        boardContainer.repaint();
    }

    private void recreateBoardPanel() {
        boardContainer.removeAll();
        adjustTileSize();

        JPanel boardPanel = new JPanel(new GridLayout(currentRows, currentCols, 2, 2));
        boardPanel.setBackground(new Color(50, 50, 55));
        boardPanel.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 65), 2));

        board = new Tile[currentRows][currentCols];
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                board[i][j] = new Tile(i, j);
                final int row = i, col = j;
                board[i][j].addActionListener(e -> onTileClick(row, col));
                board[i][j].addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (SwingUtilities.isRightMouseButton(e)) onTileRightClick(row, col);
                    }
                });
                boardPanel.add(board[i][j]);
            }
        }

        boardContainer.add(boardPanel);
        boardContainer.revalidate();
        boardContainer.repaint();

        // 调整窗口大小
        SwingUtilities.invokeLater(() -> {
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w != null) {
                w.pack();
                w.setLocationRelativeTo(null);
            }
        });
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 240, 245));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        leftPanel.setOpaque(false);

        statusLabel = new JLabel("🎮 游戏中");
        statusLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        statusLabel.setForeground(Color.BLACK);

        timerLabel = new JLabel("00:00");
        timerLabel.setFont(new Font("Consolas", Font.BOLD, 18));
        timerLabel.setForeground(new Color(0, 100, 0));

        leftPanel.add(statusLabel);
        leftPanel.add(Box.createHorizontalStrut(15));
        leftPanel.add(timerLabel);

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        centerPanel.setOpaque(false);

        JLabel difficultyLabel = new JLabel("难度:");
        difficultyLabel.setFont(new Font("微软雅黑", Font.BOLD, 13));
        difficultyLabel.setForeground(Color.BLACK);

        easyBtn = createDifficultyButton("简 单", new Color(180, 230, 180), DifficultyLevel.EASY);
        mediumBtn = createDifficultyButton("中 等", new Color(230, 230, 180), DifficultyLevel.MEDIUM);
        hardBtn = createDifficultyButton("困 难", new Color(230, 180, 180), DifficultyLevel.HARD);

        centerPanel.add(difficultyLabel);
        centerPanel.add(easyBtn);
        centerPanel.add(mediumBtn);
        centerPanel.add(hardBtn);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        minesLabel = new JLabel("💣 " + currentMines);
        minesLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        minesLabel.setForeground(Color.BLACK);

        JButton resetBtn = createActionButton("新游戏", new Color(200, 220, 240));
        resetBtn.addActionListener(e -> {
            soundService.playClick();
            resetGame();
        });

        JButton hintBtn = createActionButton("提 示", new Color(240, 220, 180));
        hintBtn.addActionListener(e -> {
            soundService.playClick();
            showHint();
        });

        modeBtn = createActionButton("人机对战", new Color(200, 220, 240));
        modeBtn.addActionListener(e -> {
            soundService.playClick();
            toggleGameMode();
        });

        soundBtn = createActionButton("🔊 音效", new Color(200, 220, 240));
        soundBtn.addActionListener(e -> {
            boolean enabled = soundService.isEnabled();
            soundService.setEnabled(!enabled);
            soundBtn.setText(enabled ? "🔇 静音" : "🔊 音效");
            if (!enabled) soundService.playClick();
        });

        rightPanel.add(minesLabel);
        rightPanel.add(resetBtn);
        rightPanel.add(hintBtn);
        rightPanel.add(modeBtn);
        rightPanel.add(soundBtn);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    private JButton createDifficultyButton(String text, Color bgColor, DifficultyLevel level) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("微软雅黑", Font.BOLD, 12));
        btn.setForeground(Color.BLACK);
        btn.setBackground(bgColor);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(60, 30));
        btn.addActionListener(e -> {
            soundService.playClick();
            setDifficulty(level);
        });
        return btn;
    }

    private JButton createActionButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("微软雅黑", Font.BOLD, 12));
        btn.setForeground(Color.BLACK);
        btn.setBackground(bgColor);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(75, 30));
        return btn;
    }

    private void setDifficulty(DifficultyLevel level) {
        currentDifficulty = level;
        switch (level) {
            case EASY:
                currentRows = EASY_ROWS;
                currentCols = EASY_COLS;
                currentMines = EASY_MINES;
                break;
            case MEDIUM:
                currentRows = MEDIUM_ROWS;
                currentCols = MEDIUM_COLS;
                currentMines = MEDIUM_MINES;
                break;
            case HARD:
                currentRows = HARD_ROWS;
                currentCols = HARD_COLS;
                currentMines = HARD_MINES;
                break;
        }

        easyBtn.setBackground(new Color(180, 230, 180));
        mediumBtn.setBackground(new Color(230, 230, 180));
        hardBtn.setBackground(new Color(230, 180, 180));

        switch (level) {
            case EASY: easyBtn.setBackground(new Color(100, 200, 100)); break;
            case MEDIUM: mediumBtn.setBackground(new Color(200, 200, 100)); break;
            case HARD: hardBtn.setBackground(new Color(200, 100, 100)); break;
        }

        // 重新创建游戏板
        recreateBoardPanel();

        // 重置游戏数据
        resetGameData();
    }

    private JPanel createSkillPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(245, 245, 250));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                "✨ 技 能 ✨",
                TitledBorder.CENTER,
                TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 12),
                Color.BLACK
        ));
        panel.setPreferredSize(new Dimension(150, 300));

        safeRevealBtn = createSkillButton("安全探测", new Color(180, 230, 180), e -> {
            soundService.playSkill();
            useSafeReveal();
        });
        doubleClickBtn = createSkillButton("双倍点击", new Color(230, 230, 180), e -> {
            soundService.playSkill();
            useDoubleClick();
        });
        mineSweeperBtn = createSkillButton("雷区扫描", new Color(230, 180, 180), e -> {
            soundService.playSkill();
            useMineSweeper();
        });
        shieldBtn = createSkillButton("护 盾", new Color(180, 200, 230), e -> {
            soundService.playSkill();
            useShield();
        });

        panel.add(Box.createVerticalStrut(15));
        panel.add(safeRevealBtn);
        panel.add(Box.createVerticalStrut(10));
        panel.add(doubleClickBtn);
        panel.add(Box.createVerticalStrut(10));
        panel.add(mineSweeperBtn);
        panel.add(Box.createVerticalStrut(10));
        panel.add(shieldBtn);
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    private JButton createSkillButton(String text, Color bgColor, ActionListener listener) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("微软雅黑", Font.BOLD, 11));
        btn.setForeground(Color.BLACK);
        btn.setBackground(bgColor);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(130, 35));
        btn.setPreferredSize(new Dimension(130, 35));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(listener);
        return btn;
    }

    private void adjustTileSize() {
        if (currentRows <= 9) tileSize = 48;
        else if (currentRows <= 16) tileSize = 40;
        else tileSize = 34;
    }

    private void resetGameData() {
        gameOver = false;
        gameWin = false;
        flagsPlaced = 0;
        isComputerTurn = false;
        shieldActive = false;

        if (chainTimer != null) chainTimer.stop();
        chainExplosions.clear();
        particles.clear();
        shockwaves.clear();

        if (gameTimer != null) gameTimer.stop();
        if (computerTimer != null) computerTimer.stop();

        // 初始化游戏数据
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                board[i][j].reset();
            }
        }

        // 放置地雷
        int minesPlaced = 0;
        while (minesPlaced < currentMines) {
            int row = random.nextInt(currentRows);
            int col = random.nextInt(currentCols);
            if (!board[row][col].isMine()) {
                board[row][col].setMine(true);
                minesPlaced++;
            }
        }

        // 计算相邻地雷数
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                if (!board[i][j].isMine()) {
                    int count = 0;
                    for (int di = -1; di <= 1; di++) {
                        for (int dj = -1; dj <= 1; dj++) {
                            if (di == 0 && dj == 0) continue;
                            int ni = i + di, nj = j + dj;
                            if (ni >= 0 && ni < currentRows && nj >= 0 && nj < currentCols) {
                                if (board[ni][nj].isMine()) count++;
                            }
                        }
                    }
                    board[i][j].setAdjacentMines(count);
                }
            }
        }

        // 重启计时器
        elapsedSeconds = 0;
        gameTimer = new javax.swing.Timer(1000, e -> {
            elapsedSeconds++;
            timerLabel.setText(String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60));
        });
        gameTimer.start();

        updateMinesLabel();

        if (currentMode == GameMode.VS_COMPUTER) {
            statusLabel.setText("🎮 人机对战 - 你的回合");
        } else {
            statusLabel.setText("🎮 游戏中");
        }
        statusLabel.setForeground(Color.BLACK);

        refreshBoard();
    }

    private void refreshBoard() {
        if (board == null) return;
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                if (board[i][j] != null) {
                    board[i][j].repaint();
                }
            }
        }
        if (boardContainer != null) {
            boardContainer.revalidate();
            boardContainer.repaint();
        }
    }

    private void initGame() {
        resetGameData();
    }

    // 单个地雷爆炸特效
    private void createSingleExplosion(int row, int col) {
        Tile tile = board[row][col];
        tile.startExplode();

        Point location = tile.getLocationOnScreen();
        Point panelLocation = getLocationOnScreen();

        int centerX = location.x + tile.getWidth() / 2 - panelLocation.x;
        int centerY = location.y + tile.getHeight() / 2 - panelLocation.y;

        shockwaves.add(new Shockwave(centerX, centerY));

        for (int i = 0; i < 60; i++) {
            particles.add(new Particle(centerX, centerY, new Color(255, 80 + random.nextInt(80), 30), false));
            particles.add(new Particle(centerX, centerY, new Color(255, 200 + random.nextInt(55), 30), true));
            particles.add(new Particle(centerX, centerY, new Color(80 + random.nextInt(40), 60 + random.nextInt(30), 40), false));
        }

        Component parent = getParent();
        Point originalPos = parent.getLocation();
        javax.swing.Timer shakeTimer = new javax.swing.Timer(16, new ActionListener() {
            int count = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (count < 8) {
                    int offsetX = random.nextInt(6) - 3;
                    int offsetY = random.nextInt(6) - 3;
                    parent.setLocation(originalPos.x + offsetX, originalPos.y + offsetY);
                    count++;
                } else {
                    parent.setLocation(originalPos);
                    ((javax.swing.Timer)e.getSource()).stop();
                }
            }
        });
        shakeTimer.start();

        tile.repaint();
    }

    // 链式爆炸
    private void startChainExplosion() {
        chainExplosions.clear();
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                if (board[i][j].isMine() && !board[i][j].isRevealed()) {
                    chainExplosions.add(new Point(i, j));
                }
            }
        }

        currentChainIndex = 0;

        if (chainTimer != null) chainTimer.stop();
        chainTimer = new javax.swing.Timer(150, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentChainIndex < chainExplosions.size()) {
                    Point p = chainExplosions.get(currentChainIndex);
                    board[p.x][p.y].reveal();
                    createSingleExplosion(p.x, p.y);
                    soundService.playExplode();
                    currentChainIndex++;

                    if (currentChainIndex % 3 == 0) {
                        Component parent = getParent();
                        Point originalPos = parent.getLocation();
                        javax.swing.Timer shakeTimer = new javax.swing.Timer(16, new ActionListener() {
                            int count = 0;
                            @Override
                            public void actionPerformed(ActionEvent evt) {
                                if (count < 3) {
                                    int offsetX = random.nextInt(4) - 2;
                                    int offsetY = random.nextInt(4) - 2;
                                    parent.setLocation(originalPos.x + offsetX, originalPos.y + offsetY);
                                    count++;
                                } else {
                                    parent.setLocation(originalPos);
                                    ((javax.swing.Timer)evt.getSource()).stop();
                                }
                            }
                        });
                        shakeTimer.start();
                    }
                } else {
                    chainTimer.stop();
                    statusLabel.setText("💀 游戏结束");
                    statusLabel.setForeground(Color.RED);
                    refreshBoard();
                    soundService.playGameOver();
                }
            }
        });
        chainTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Shockwave s : shockwaves) {
            s.draw(g2d);
        }

        for (Particle p : particles) {
            p.draw(g2d);
        }

        g2d.dispose();
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
            soundService.playExplode();

            if (shieldActive) {
                shieldActive = false;
                statusLabel.setText("🛡️ 护盾生效！");
                tile.setFlagged(true);
                flagsPlaced++;
                updateMinesLabel();
                tile.repaint();
                return;
            }

            gameOver = true;
            if (gameTimer != null) gameTimer.stop();

            createSingleExplosion(row, col);
            tile.reveal();

            javax.swing.Timer startChain = new javax.swing.Timer(500, e -> {
                startChainExplosion();
            });
            startChain.setRepeats(false);
            startChain.start();
            return;
        }

        soundService.playClick();

        revealTile(row, col);
        refreshBoard();
        checkWin();

        if (currentMode == GameMode.VS_COMPUTER && !gameOver && !gameWin) {
            isComputerTurn = true;
            statusLabel.setText("🤖 电脑思考中...");
            statusLabel.setForeground(new Color(0, 0, 150));
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
                    int ni = row + di, nj = col + dj;
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

    private void checkWin() {
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                if (!board[i][j].isRevealed() && !board[i][j].isMine()) return;
            }
        }

        gameWin = true;
        statusLabel.setText("🏆 胜利！");
        statusLabel.setForeground(new Color(0, 100, 0));
        if (gameTimer != null) gameTimer.stop();

        soundService.playWin();

        for (int i = 0; i < 150; i++) {
            int row = random.nextInt(currentRows);
            int col = random.nextInt(currentCols);
            Tile t = board[row][col];
            Point loc = t.getLocationOnScreen();
            Point panelLoc = getLocationOnScreen();
            particles.add(new Particle(loc.x + t.getWidth()/2 - panelLoc.x,
                    loc.y + t.getHeight()/2 - panelLoc.y,
                    new Color(255, 215, 0), false));
        }

        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                if (board[i][j].isMine()) {
                    board[i][j].setFlagged(true);
                }
            }
        }
        refreshBoard();
    }

    private void onTileRightClick(int row, int col) {
        if (gameOver || gameWin) return;
        if (currentMode == GameMode.VS_COMPUTER && isComputerTurn) return;

        Tile tile = board[row][col];
        if (tile.isRevealed()) return;

        if (!tile.isFlagged() && flagsPlaced < currentMines) {
            tile.setFlagged(true);
            flagsPlaced++;
            soundService.playFlag();
        } else if (tile.isFlagged()) {
            tile.setFlagged(false);
            flagsPlaced--;
            soundService.playFlag();
        }
        tile.repaint();
        updateMinesLabel();
    }

    private void startComputerTurn() {
        if (gameOver || gameWin || !isComputerTurn) return;

        soundService.playComputer();

        computerTimer = new javax.swing.Timer(600, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameOver || gameWin || !isComputerTurn) {
                    computerTimer.stop();
                    return;
                }

                List<Point> safe = new ArrayList<>();
                for (int i = 0; i < currentRows; i++) {
                    for (int j = 0; j < currentCols; j++) {
                        Tile t = board[i][j];
                        if (!t.isRevealed() && !t.isFlagged() && !t.isMine()) {
                            safe.add(new Point(i, j));
                        }
                    }
                }

                if (!safe.isEmpty()) {
                    Point p = safe.get(random.nextInt(safe.size()));
                    Tile tile = board[p.x][p.y];
                    Color originalBg = tile.getBackground();

                    tile.setBackground(new Color(100, 200, 255));
                    tile.repaint();

                    javax.swing.Timer clickTimer = new javax.swing.Timer(400, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent evt) {
                            if (!gameOver && !gameWin && isComputerTurn) {
                                if (board[p.x][p.y].isMine()) {
                                    soundService.playExplode();
                                    createSingleExplosion(p.x, p.y);
                                    gameOver = true;
                                    statusLabel.setText("💀 电脑踩到地雷！你赢了！");
                                    statusLabel.setForeground(Color.RED);
                                    if (gameTimer != null) gameTimer.stop();
                                    revealAllMines();
                                    refreshBoard();
                                    soundService.playGameOver();
                                } else {
                                    soundService.playClick();
                                    revealTile(p.x, p.y);
                                    refreshBoard();
                                    checkWin();
                                }
                            }
                            if (!tile.isRevealed()) {
                                tile.setBackground(originalBg);
                                tile.repaint();
                            }
                            isComputerTurn = false;
                            if (!gameOver && !gameWin) {
                                statusLabel.setText("🎮 轮到你了");
                                statusLabel.setForeground(Color.BLACK);
                            }
                            computerTimer.stop();
                        }
                    });
                    clickTimer.setRepeats(false);
                    clickTimer.start();
                } else {
                    isComputerTurn = false;
                    if (!gameOver && !gameWin) {
                        statusLabel.setText("🎮 轮到你了");
                        statusLabel.setForeground(Color.BLACK);
                    }
                    computerTimer.stop();
                }
            }
        });
        computerTimer.setRepeats(false);
        computerTimer.start();
    }

    private void useSafeReveal() {
        if (gameOver || gameWin) return;
        if (currentMode == GameMode.VS_COMPUTER && isComputerTurn) {
            statusLabel.setText("电脑回合无法使用技能");
            return;
        }
        showHint();
    }

    private void useDoubleClick() {
        if (gameOver || gameWin) return;
        if (currentMode == GameMode.VS_COMPUTER && isComputerTurn) {
            statusLabel.setText("电脑回合无法使用技能");
            return;
        }

        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                Tile t = board[i][j];
                if (t.isRevealed() && t.getAdjacentMines() > 0) {
                    for (int di = -1; di <= 1; di++) {
                        for (int dj = -1; dj <= 1; dj++) {
                            int ni = i + di, nj = j + dj;
                            if (ni >= 0 && ni < currentRows && nj >= 0 && nj < currentCols) {
                                Tile neighbor = board[ni][nj];
                                if (!neighbor.isRevealed() && !neighbor.isFlagged() && !neighbor.isMine()) {
                                    revealTile(ni, nj);
                                }
                            }
                        }
                    }
                }
            }
        }
        refreshBoard();
        checkWin();
        statusLabel.setText("⚡ 双倍点击已使用");
    }

    private void useMineSweeper() {
        if (gameOver || gameWin) return;

        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                if (board[i][j].isMine() && !board[i][j].isRevealed()) {
                    board[i][j].setBackground(new Color(255, 100, 100));
                    board[i][j].repaint();
                }
            }
        }

        javax.swing.Timer timer = new javax.swing.Timer(2000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < currentRows; i++) {
                    for (int j = 0; j < currentCols; j++) {
                        if (!board[i][j].isRevealed()) {
                            board[i][j].setBackground(null);
                            board[i][j].repaint();
                        }
                    }
                }
            }
        });
        timer.setRepeats(false);
        timer.start();
        statusLabel.setText("📡 雷区扫描");
    }

    private void useShield() {
        if (gameOver || gameWin) return;
        if (currentMode == GameMode.VS_COMPUTER && isComputerTurn) {
            statusLabel.setText("电脑回合无法使用技能");
            return;
        }

        shieldActive = true;
        statusLabel.setText("🛡️ 护盾已激活");

        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                if (!board[i][j].isRevealed()) {
                    board[i][j].setBorder(BorderFactory.createLineBorder(new Color(100, 150, 255), 2));
                }
            }
        }

        javax.swing.Timer timer = new javax.swing.Timer(3000, new ActionListener() {
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
        timer.setRepeats(false);
        timer.start();
    }

    private void showHint() {
        List<Point> safe = new ArrayList<>();
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                Tile t = board[i][j];
                if (!t.isRevealed() && !t.isFlagged() && !t.isMine()) {
                    safe.add(new Point(i, j));
                }
            }
        }

        if (safe.isEmpty()) return;

        Point p = safe.get(random.nextInt(safe.size()));
        Tile t = board[p.x][p.y];
        Color orig = t.getBackground();
        t.setBackground(Color.YELLOW);
        t.repaint();

        javax.swing.Timer timer = new javax.swing.Timer(2000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!t.isRevealed()) {
                    t.setBackground(orig);
                    t.repaint();
                }
            }
        });
        timer.setRepeats(false);
        timer.start();

        statusLabel.setText("💡 第" + (p.x + 1) + "行,第" + (p.y + 1) + "列安全");
    }

    private void toggleGameMode() {
        if (currentMode == GameMode.CLASSIC) {
            currentMode = GameMode.VS_COMPUTER;
            modeBtn.setText("经典模式");
            statusLabel.setText("🎮 人机对战 - 你的回合");
        } else {
            currentMode = GameMode.CLASSIC;
            modeBtn.setText("人机对战");
            statusLabel.setText("🎮 游戏中");
        }
        resetGame();
    }

    private void updateMinesLabel() {
        minesLabel.setText("💣 " + (currentMines - flagsPlaced));
    }

    private void resetGame() {
        resetGameData();
        refreshBoard();

        SwingUtilities.invokeLater(() -> {
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w != null) {
                w.pack();
                w.setLocationRelativeTo(null);
            }
        });
    }
}