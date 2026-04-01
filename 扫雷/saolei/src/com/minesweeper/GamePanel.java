package com.minesweeper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;

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

    // 当前游戏配置
    private int currentRows = EASY_ROWS;
    private int currentCols = EASY_COLS;
    private int currentMines = EASY_MINES;
    private int tileSize = 45; // 每个格子的大小（像素）

    private Tile[][] board;
    private boolean gameOver;
    private boolean gameWin;
    private JLabel statusLabel;
    private JLabel minesLabel;
    private int flagsPlaced;
    private JPanel gameBoardPanel;
    private JButton easyBtn, mediumBtn, hardBtn;
    private JButton resetButton;
    private JPanel boardPanel; // 保存游戏面板的引用

    public GamePanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(240, 240, 240));

        // 顶部面板 - 增加高度和间距
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
        resetButton.setPreferredSize(new Dimension(100, 38));
        resetButton.setFocusPainted(false);
        resetButton.addActionListener(e -> resetGame());

        // 难度选择按钮
        easyBtn = new JButton("简单 9x9");
        easyBtn.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        easyBtn.setPreferredSize(new Dimension(85, 35));
        easyBtn.setBackground(new Color(200, 230, 200));
        easyBtn.setFocusPainted(false);
        easyBtn.addActionListener(e -> setDifficulty(EASY_ROWS, EASY_COLS, EASY_MINES));

        mediumBtn = new JButton("中等 16x16");
        mediumBtn.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        mediumBtn.setPreferredSize(new Dimension(95, 35));
        mediumBtn.setBackground(new Color(230, 230, 180));
        mediumBtn.setFocusPainted(false);
        mediumBtn.addActionListener(e -> setDifficulty(MEDIUM_ROWS, MEDIUM_COLS, MEDIUM_MINES));

        hardBtn = new JButton("困难 16x30");
        hardBtn.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        hardBtn.setPreferredSize(new Dimension(95, 35));
        hardBtn.setBackground(new Color(230, 200, 200));
        hardBtn.setFocusPainted(false);
        hardBtn.addActionListener(e -> setDifficulty(HARD_ROWS, HARD_COLS, HARD_MINES));

        // 添加组件到顶部面板
        topPanel.add(statusLabel);
        topPanel.add(resetButton);
        topPanel.add(easyBtn);
        topPanel.add(mediumBtn);
        topPanel.add(hardBtn);
        topPanel.add(minesLabel);

        add(topPanel, BorderLayout.NORTH);

        // 游戏面板容器 - 添加边框和间距
        gameBoardPanel = new JPanel();
        gameBoardPanel.setLayout(new BorderLayout());
        gameBoardPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        gameBoardPanel.setBackground(new Color(240, 240, 240));
        add(gameBoardPanel, BorderLayout.CENTER);

        // 初始化游戏
        createGameBoard();
        initGame();

        // 强制刷新显示
        SwingUtilities.invokeLater(() -> {
            revalidate();
            repaint();
        });
    }

    private void createGameBoard() {
        // 清除旧的面板
        gameBoardPanel.removeAll();

        // 根据难度调整格子大小
        adjustTileSize();

        // 创建居中对齐的容器
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(new Color(240, 240, 240));

        // 创建游戏面板
        boardPanel = new JPanel(new GridLayout(currentRows, currentCols));
        int boardWidth = currentCols * tileSize;
        int boardHeight = currentRows * tileSize;
        boardPanel.setPreferredSize(new Dimension(boardWidth, boardHeight));
        boardPanel.setMaximumSize(new Dimension(boardWidth, boardHeight));
        boardPanel.setBackground(Color.DARK_GRAY);
        boardPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));

        board = new Tile[currentRows][currentCols];

        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                board[i][j] = new Tile(i, j);
                board[i][j].setPreferredSize(new Dimension(tileSize, tileSize));
                board[i][j].setFont(new Font("Segoe UI", Font.BOLD, Math.max(16, tileSize / 2)));
                board[i][j].addActionListener(new TileClickListener(i, j));
                board[i][j].addMouseListener(new TileMouseListener(i, j));
                boardPanel.add(board[i][j]);
            }
        }

        centerPanel.add(boardPanel);
        gameBoardPanel.add(centerPanel, BorderLayout.CENTER);

        // 强制刷新
        boardPanel.revalidate();
        boardPanel.repaint();
        centerPanel.revalidate();
        centerPanel.repaint();
        gameBoardPanel.revalidate();
        gameBoardPanel.repaint();
    }

    private void adjustTileSize() {
        // 根据难度和屏幕大小调整格子尺寸
        if (currentRows <= 9 && currentCols <= 9) {
            tileSize = 45; // 简单难度使用大格子
        } else if (currentRows <= 16 && currentCols <= 16) {
            tileSize = 38; // 中等难度使用中等格子
        } else {
            tileSize = 32; // 困难难度使用小格子
        }
    }

    private void setDifficulty(int rows, int cols, int mines) {
        this.currentRows = rows;
        this.currentCols = cols;
        this.currentMines = mines;

        // 更新按钮样式
        updateDifficultyButtonStyle();

        // 重新创建游戏板
        createGameBoard();

        // 重置游戏状态
        resetGameState();

        // 初始化游戏逻辑
        initGame();

        // 强制刷新显示
        SwingUtilities.invokeLater(() -> {
            revalidate();
            repaint();
            // 调整窗口大小
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.pack();
                window.setLocationRelativeTo(null);
                window.setMinimumSize(window.getSize());
            }
        });
    }

    private void updateDifficultyButtonStyle() {
        // 重置所有按钮样式
        easyBtn.setBackground(new Color(200, 230, 200));
        mediumBtn.setBackground(new Color(230, 230, 180));
        hardBtn.setBackground(new Color(230, 200, 200));

        // 高亮当前选中的难度
        if (currentRows == EASY_ROWS && currentCols == EASY_COLS) {
            easyBtn.setBackground(new Color(100, 200, 100));
        } else if (currentRows == MEDIUM_ROWS && currentCols == MEDIUM_COLS) {
            mediumBtn.setBackground(new Color(200, 200, 100));
        } else if (currentRows == HARD_ROWS && currentCols == HARD_COLS) {
            hardBtn.setBackground(new Color(200, 100, 100));
        }
    }

    private void resetGameState() {
        gameOver = false;
        gameWin = false;
        flagsPlaced = 0;
        updateMinesLabel();
        statusLabel.setText("游戏进行中");
        statusLabel.setForeground(Color.BLACK);
    }

    private void initGame() {
        // 清除所有格子的状态
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                board[i][j].reset();
            }
        }

        // 随机放置地雷
        placeMines();

        // 计算相邻地雷数
        calculateAdjacentMines();

        // 强制刷新所有格子的显示
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                board[i][j].revalidate();
                board[i][j].repaint();
            }
        }

        // 刷新整个游戏面板
        if (boardPanel != null) {
            boardPanel.revalidate();
            boardPanel.repaint();
        }

        // 刷新顶层容器
        revalidate();
        repaint();
    }

    private void placeMines() {
        Random random = new Random();
        int minesPlaced = 0;

        while (minesPlaced < currentMines) {
            int row = random.nextInt(currentRows);
            int col = random.nextInt(currentCols);

            if (!board[row][col].isMine()) {
                board[row][col].setMine(true);
                minesPlaced++;
            }
        }
    }

    private void calculateAdjacentMines() {
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
                                if (board[ni][nj].isMine()) {
                                    count++;
                                }
                            }
                        }
                    }

                    board[i][j].setAdjacentMines(count);
                }
            }
        }
    }

    private void revealTile(int row, int col) {
        if (gameOver || gameWin) return;

        Tile tile = board[row][col];

        if (tile.isFlagged() || tile.isRevealed()) return;

        if (tile.isMine()) {
            gameOver = true;
            statusLabel.setText("游戏结束！你输了！");
            statusLabel.setForeground(Color.RED);
            revealAllMines();
            return;
        }

        revealEmptyTiles(row, col);
        checkWin();
    }

    private void revealEmptyTiles(int row, int col) {
        Tile tile = board[row][col];

        if (tile.isRevealed() || tile.isFlagged()) return;

        tile.reveal();

        if (tile.getAdjacentMines() == 0 && !tile.isMine()) {
            for (int di = -1; di <= 1; di++) {
                for (int dj = -1; dj <= 1; dj++) {
                    if (di == 0 && dj == 0) continue;

                    int ni = row + di;
                    int nj = col + dj;

                    if (ni >= 0 && ni < currentRows && nj >= 0 && nj < currentCols) {
                        revealEmptyTiles(ni, nj);
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
        int unrevealedCount = 0;

        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                if (!board[i][j].isRevealed() && !board[i][j].isMine()) {
                    unrevealedCount++;
                }
            }
        }

        if (unrevealedCount == 0) {
            gameWin = true;
            statusLabel.setText("恭喜！你赢了！");
            statusLabel.setForeground(new Color(0, 150, 0));

            for (int i = 0; i < currentRows; i++) {
                for (int j = 0; j < currentCols; j++) {
                    if (board[i][j].isMine()) {
                        board[i][j].setFlagged(true);
                        board[i][j].setText("🚩");
                    }
                }
            }
        }
    }

    private void toggleFlag(int row, int col) {
        if (gameOver || gameWin) return;

        Tile tile = board[row][col];

        if (!tile.isRevealed()) {
            if (!tile.isFlagged() && flagsPlaced < currentMines) {
                tile.setFlagged(true);
                tile.setText("🚩");
                flagsPlaced++;
                updateMinesLabel();
            } else if (tile.isFlagged()) {
                tile.setFlagged(false);
                tile.setText("");
                flagsPlaced--;
                updateMinesLabel();
            }
            // 刷新这个格子的显示
            tile.revalidate();
            tile.repaint();
        }
    }

    private void updateMinesLabel() {
        int remainingMines = currentMines - flagsPlaced;
        minesLabel.setText("剩余地雷: " + remainingMines);
        if (remainingMines < 0) {
            minesLabel.setForeground(Color.RED);
        } else {
            minesLabel.setForeground(Color.BLACK);
        }
    }

    public void resetGame() {
        // 重置游戏状态
        resetGameState();

        // 重新初始化游戏（不清除面板）
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                board[i][j].reset();
            }
        }

        // 重新放置地雷和计算数字
        placeMines();
        calculateAdjacentMines();

        // 强制刷新所有格子
        for (int i = 0; i < currentRows; i++) {
            for (int j = 0; j < currentCols; j++) {
                board[i][j].revalidate();
                board[i][j].repaint();
            }
        }

        // 刷新整个游戏面板
        if (boardPanel != null) {
            boardPanel.revalidate();
            boardPanel.repaint();
        }

        // 刷新顶层容器
        revalidate();
        repaint();
    }

    private class TileClickListener implements ActionListener {
        private int row, col;

        public TileClickListener(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            revealTile(row, col);
        }
    }

    private class TileMouseListener extends MouseAdapter {
        private int row, col;

        public TileMouseListener(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                toggleFlag(row, col);
            }
        }
    }
}