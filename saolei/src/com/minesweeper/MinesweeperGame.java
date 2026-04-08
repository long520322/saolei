package com.minesweeper;

import javax.swing.*;

public class MinesweeperGame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("扫雷游戏 V2 - 经典扫雷 | 人机对战");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            GamePanel gamePanel = new GamePanel();
            frame.add(gamePanel);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            System.out.println("扫雷游戏 V2 启动成功！");
            System.out.println("游戏说明：");
            System.out.println("- 左键点击翻开格子");
            System.out.println("- 右键点击标记地雷");
            System.out.println("- 💡 提示按钮：自动提示一个安全格子");
            System.out.println("- 🤖 人机对战：与电脑AI对战");
        });
    }
}