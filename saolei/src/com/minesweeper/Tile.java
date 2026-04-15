package com.minesweeper;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class Tile extends JButton {
    private int x, y;
    private boolean isMine;
    private boolean isRevealed;
    private boolean isFlagged;
    private int adjacentMines;
    private boolean isExploding;

    public Tile(int x, int y) {
        this.x = x;
        this.y = y;
        this.isMine = false;
        this.isRevealed = false;
        this.isFlagged = false;
        this.adjacentMines = 0;
        this.isExploding = false;

        setupUI();
    }

    private void setupUI() {
        setFont(new Font("微软雅黑", Font.BOLD, 24));
        setFocusPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(48, 48));
        setMargin(new Insets(0, 0, 0, 0));
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        setBackground(new Color(120, 120, 135));
        setOpaque(true);
        setBorder(BorderFactory.createRaisedBevelBorder());  // 初始为凸起边框
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (isRevealed) {
            // 翻开格子的背景
            g2d.setColor(new Color(248, 248, 255));
            g2d.fillRect(0, 0, w, h);
            g2d.setColor(new Color(180, 180, 200));
            g2d.drawRect(0, 0, w - 1, h - 1);

            if (isMine) {
                // 绘制炸弹
                BombIcon.drawBomb(g2d, 0, 0, w, isExploding);
            } else if (adjacentMines > 0) {
                String text = String.valueOf(adjacentMines);
                g2d.setFont(new Font("微软雅黑", Font.BOLD, 26));
                FontMetrics fm = g2d.getFontMetrics();
                int xPos = (w - fm.stringWidth(text)) / 2;
                int yPos = (h + fm.getAscent()) / 2 - 3;
                g2d.setColor(getNumberColor());
                g2d.drawString(text, xPos, yPos);
            }
            // 移除边框，使用普通矩形
            setBorder(BorderFactory.createEmptyBorder());
        } else {
            // 未翻开格子
            GradientPaint gradient = new GradientPaint(0, 0, new Color(140, 140, 155),
                    0, h, new Color(100, 100, 115));
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, w, h);

            // 3D效果
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.drawLine(1, 1, w - 2, 1);
            g2d.drawLine(1, 1, 1, h - 2);
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.drawLine(1, h - 2, w - 2, h - 2);
            g2d.drawLine(w - 2, 1, w - 2, h - 2);

            // 旗子
            if (isFlagged) {
                drawFlag(g2d, w, h);
            }

            // 未翻开时使用凸起边框
            setBorder(BorderFactory.createRaisedBevelBorder());
        }

        g2d.dispose();
    }

    private void drawFlag(Graphics2D g2d, int w, int h) {
        g2d.setColor(new Color(60, 60, 70));
        g2d.setStroke(new BasicStroke(3f));
        g2d.drawLine(w / 2, h / 5, w / 2, h * 4 / 5);

        int[] xPoints = {w / 2, w * 3 / 4, w / 2};
        int[] yPoints = {h / 5, h / 3, h * 5 / 12};
        g2d.setColor(new Color(255, 60, 50));
        g2d.fillPolygon(xPoints, yPoints, 3);
    }

    private Color getNumberColor() {
        switch (adjacentMines) {
            case 1: return new Color(0, 100, 255);
            case 2: return new Color(0, 160, 0);
            case 3: return new Color(255, 50, 50);
            case 4: return new Color(0, 0, 200);
            case 5: return new Color(180, 0, 180);
            case 6: return new Color(0, 160, 160);
            default: return new Color(80, 80, 80);
        }
    }

    public void startExplode() {
        isExploding = true;
        repaint();
        // 300ms后停止爆炸动画并移除边框
        Timer timer = new Timer(300, e -> {
            isExploding = false;
            // 确保爆炸后边框被移除
            setBorder(BorderFactory.createEmptyBorder());
            repaint();
        });
        timer.setRepeats(false);
        timer.start();
    }

    public void reveal() {
        if (isRevealed || isFlagged) return;
        isRevealed = true;
        // 翻开时移除凸起边框
        setBorder(BorderFactory.createEmptyBorder());
        repaint();
    }

    public void reset() {
        isMine = false;
        isRevealed = false;
        isFlagged = false;
        adjacentMines = 0;
        isExploding = false;
        setEnabled(true);
        // 重置时恢复凸起边框
        setBorder(BorderFactory.createRaisedBevelBorder());
        repaint();
    }

    // Getters and Setters
    public int getTileX() { return x; }
    public int getTileY() { return y; }
    public boolean isMine() { return isMine; }
    public void setMine(boolean mine) { isMine = mine; }
    public boolean isRevealed() { return isRevealed; }
    public void setRevealed(boolean revealed) { isRevealed = revealed; }
    public boolean isFlagged() { return isFlagged; }
    public void setFlagged(boolean flagged) { isFlagged = flagged; }
    public int getAdjacentMines() { return adjacentMines; }
    public void setAdjacentMines(int mines) { adjacentMines = mines; }
}