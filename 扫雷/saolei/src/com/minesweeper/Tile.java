package com.minesweeper;

import javax.swing.*;
import java.awt.*;

public class Tile extends JButton {
    private int x, y;
    private boolean isMine;
    private boolean isRevealed;
    private boolean isFlagged;
    private int adjacentMines;

    public Tile(int x, int y) {
        this.x = x;
        this.y = y;
        this.isMine = false;
        this.isRevealed = false;
        this.isFlagged = false;
        this.adjacentMines = 0;

        // 设置字体和样式
        setFont(new Font("Segoe UI", Font.BOLD, 20));
        setFocusPainted(false);
        setMargin(new Insets(0, 0, 0, 0));
        setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // 确保文字居中
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);

        // 设置背景色
        setBackground(new Color(200, 200, 200));
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isMine() { return isMine; }
    public void setMine(boolean isMine) { this.isMine = isMine; }
    public boolean isRevealed() { return isRevealed; }
    public void setRevealed(boolean revealed) { isRevealed = revealed; }
    public boolean isFlagged() { return isFlagged; }
    public void setFlagged(boolean flagged) { isFlagged = flagged; }
    public int getAdjacentMines() { return adjacentMines; }
    public void setAdjacentMines(int adjacentMines) { this.adjacentMines = adjacentMines; }

    public void reveal() {
        if (isRevealed || isFlagged) return;

        isRevealed = true;

        if (isMine) {
            setText("💣");
            setBackground(Color.RED);
            setForeground(Color.BLACK);
        } else if (adjacentMines > 0) {
            setText(String.valueOf(adjacentMines));
            setForeground(getColorForNumber(adjacentMines));
            setBackground(new Color(220, 220, 220));
        } else {
            setText("");
            setBackground(new Color(210, 210, 210));
        }

        setBorder(BorderFactory.createLoweredBevelBorder());
        setEnabled(false);
    }

    private Color getColorForNumber(int number) {
        switch (number) {
            case 1: return new Color(0, 0, 255);
            case 2: return new Color(0, 100, 0);
            case 3: return Color.RED;
            case 4: return new Color(0, 0, 139);
            case 5: return new Color(139, 0, 0);
            case 6: return new Color(0, 139, 139);
            default: return Color.BLACK;
        }
    }

    public void reset() {
        isMine = false;
        isRevealed = false;
        isFlagged = false;
        adjacentMines = 0;
        setText("");
        setBackground(new Color(200, 200, 200));
        setEnabled(true);
        setForeground(Color.BLACK);
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
    }
}