package com.minesweeper;

import javax.swing.*;

public class MinesweeperGame {
    public static void main(String[] args) {
        // 使用SwingUtilities确保GUI在事件调度线程中创建
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("扫雷游戏 - 经典扫雷");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            GamePanel gamePanel = new GamePanel();
            frame.add(gamePanel);

            // 自动调整窗口大小以适应内容
            frame.pack();

            // 设置最小窗口大小
            frame.setMinimumSize(frame.getSize());

            // 居中显示
            frame.setLocationRelativeTo(null);

            // 设置窗口可见
            frame.setVisible(true);

            System.out.println("扫雷游戏启动成功！");
            System.out.println("游戏说明：");
            System.out.println("- 左键点击翻开格子");
            System.out.println("- 右键点击标记地雷");
            System.out.println("- 可选择简单、中等、困难难度");
        });
    }
}