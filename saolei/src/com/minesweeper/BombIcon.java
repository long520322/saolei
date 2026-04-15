package com.minesweeper;

import java.awt.*;
import java.awt.geom.*;

/**
 * 炸弹绘制类 - 绘制精美的炸弹图形
 */
public class BombIcon {

    public static void drawBomb(Graphics2D g2d, int x, int y, int size, boolean isExploding) {
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        int bombSize = size - 8;

        if (isExploding) {
            // 爆炸效果 - 绘制火焰
            drawExplosion(g2d, centerX, centerY, bombSize);
        } else {
            // 绘制炸弹主体
            // 炸弹圆形身体
            g2d.setColor(new Color(40, 40, 45));
            g2d.fillOval(centerX - bombSize/2, centerY - bombSize/2, bombSize, bombSize);

            // 炸弹高光
            g2d.setColor(new Color(80, 80, 90));
            g2d.fillOval(centerX - bombSize/2 + 3, centerY - bombSize/2 + 3, bombSize - 6, bombSize - 6);

            // 炸弹金属质感
            RadialGradientPaint gradient = new RadialGradientPaint(
                    centerX - bombSize/4, centerY - bombSize/4, bombSize/2,
                    new float[]{0f, 0.6f, 1f},
                    new Color[]{new Color(100, 100, 110), new Color(60, 60, 70), new Color(30, 30, 35)}
            );
            g2d.setPaint(gradient);
            g2d.fillOval(centerX - bombSize/2, centerY - bombSize/2, bombSize, bombSize);

            // 炸弹引信
            g2d.setColor(new Color(80, 60, 30));
            g2d.fillRect(centerX - 3, centerY - bombSize/2 - 6, 6, 12);

            // 引线
            g2d.setColor(new Color(60, 40, 20));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(centerX, centerY - bombSize/2 - 6, centerX, centerY - bombSize/2 - 14);

            // 火星（闪烁）
            g2d.setColor(new Color(255, 100, 50));
            g2d.fillOval(centerX - 2, centerY - bombSize/2 - 16, 4, 4);

            // 骷髅标记
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Segoe UI", Font.BOLD, bombSize - 12));
            FontMetrics fm = g2d.getFontMetrics();
            String skull = "💀";
            int textX = centerX - fm.stringWidth(skull) / 2;
            int textY = centerY + fm.getAscent() / 2 - 2;
            g2d.drawString(skull, textX, textY);
        }
    }

    private static void drawExplosion(Graphics2D g2d, int centerX, int centerY, int size) {
        // 爆炸光芒
        RadialGradientPaint explosionGrad = new RadialGradientPaint(
                centerX, centerY, size,
                new float[]{0f, 0.4f, 0.7f, 1f},
                new Color[]{new Color(255, 255, 200, 255),
                        new Color(255, 150, 50, 200),
                        new Color(255, 50, 0, 100),
                        new Color(255, 0, 0, 0)}
        );
        g2d.setPaint(explosionGrad);
        g2d.fillOval(centerX - size, centerY - size, size * 2, size * 2);

        // 爆炸火焰
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI * 2 / 8;
            int flameX = centerX + (int)(Math.cos(angle) * size * 0.8);
            int flameY = centerY + (int)(Math.sin(angle) * size * 0.8);

            g2d.setColor(new Color(255, 100 + (i * 20), 0, 180));
            g2d.fillOval(flameX - 5, flameY - 5, 10, 10);
        }
    }
}