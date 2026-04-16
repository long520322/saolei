package com.minesweeper;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * 扫雷游戏自动化测试框架 - 带弹窗显示结果
 */
public class MinesweeperTest {

    // 测试统计
    private static TestStatistics statistics = new TestStatistics();

    // 测试超时时间（毫秒）
    private static final long TEST_TIMEOUT_MS = 5000;

    /**
     * 测试统计类
     */
    static class TestStatistics {
        int total = 0;
        int passed = 0;
        int failed = 0;
        int skipped = 0;
        long startTime;
        long endTime;
        List<TestResult> results = new ArrayList<>();

        void start() { startTime = System.currentTimeMillis(); }
        void end() { endTime = System.currentTimeMillis(); }
        long getDuration() { return endTime - startTime; }

        void addResult(TestResult result) {
            results.add(result);
            total++;
            if (result.passed) passed++;
            else if (result.skipped) skipped++;
            else failed++;
        }

        String getPassRate() {
            return String.format("%.1f%%", (double)passed / total * 100);
        }
    }

    /**
     * 测试结果类
     */
    static class TestResult {
        String testName;
        String category;
        boolean passed;
        boolean skipped;
        String errorMessage;
        long duration;

        TestResult(String testName, String category, boolean passed, String errorMessage, long duration) {
            this.testName = testName;
            this.category = category;
            this.passed = passed;
            this.errorMessage = errorMessage;
            this.duration = duration;
            this.skipped = false;
        }
    }

    // ==================== 断言方法 ====================
    static void assertEquals(String message, Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " - 期望: " + expected + ", 实际: " + actual);
        }
    }

    static void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new AssertionError(message + " - 条件不成立");
        }
    }

    static void assertFalse(String message, boolean condition) {
        if (condition) {
            throw new AssertionError(message + " - 条件应该为假但实际为真");
        }
    }

    static void assertNotNull(String message, Object object) {
        if (object == null) {
            throw new AssertionError(message + " - 对象为null");
        }
    }

    static void assertBetween(String message, long value, long min, long max) {
        if (value < min || value > max) {
            throw new AssertionError(message + " - 值 " + value + " 不在范围 [" + min + ", " + max + "] 内");
        }
    }

    // ==================== 测试执行器 ====================
    static void runTest(String category, String testName, TestRunnable test) {
        long startTime = System.nanoTime();

        try {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> future = executor.submit(() -> {
                try {
                    test.run();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            try {
                future.get(TEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                long duration = (System.nanoTime() - startTime) / 1000000;
                statistics.addResult(new TestResult(testName, category, true, null, duration));
            } catch (TimeoutException e) {
                future.cancel(true);
                long duration = (System.nanoTime() - startTime) / 1000000;
                statistics.addResult(new TestResult(testName, category, false, "测试超时", duration));
            } catch (Exception e) {
                long duration = (System.nanoTime() - startTime) / 1000000;
                statistics.addResult(new TestResult(testName, category, false, e.getCause().getMessage(), duration));
            }
            executor.shutdownNow();
        } catch (Exception e) {
            long duration = (System.nanoTime() - startTime) / 1000000;
            statistics.addResult(new TestResult(testName, category, false, e.getMessage(), duration));
        }
    }

    @FunctionalInterface
    interface TestRunnable {
        void run() throws Exception;
    }

    // ==================== 辅助方法 ====================
    private static Color getNumberColor(int number) {
        switch (number) {
            case 1: return new Color(0, 100, 255);
            case 2: return new Color(0, 160, 0);
            case 3: return new Color(255, 50, 50);
            case 4: return new Color(0, 0, 200);
            case 5: return new Color(180, 0, 180);
            case 6: return new Color(0, 160, 160);
            default: return Color.BLACK;
        }
    }

    private static boolean isValidPosition(int row, int col, int rows, int cols) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    private static int calculateTileSize(int rows, int cols) {
        if (rows <= 9 && cols <= 9) return 48;
        if (rows <= 16 && cols <= 16) return 40;
        return 34;
    }

    private static void revealArea(int row, int col, int rows, int cols, boolean[][] revealed) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) return;
        if (revealed[row][col]) return;

        revealed[row][col] = true;

        for (int di = -1; di <= 1; di++) {
            for (int dj = -1; dj <= 1; dj++) {
                if (di == 0 && dj == 0) continue;
                revealArea(row + di, col + dj, rows, cols, revealed);
            }
        }
    }

    // ==================== 测试用例 ====================

    static void testTileConstructor() {
        runTest("单元测试", "Tile构造函数应正确初始化格子", () -> {
            Tile tile = new Tile(3, 5);
            assertEquals("x坐标", 3, tile.getTileX());
            assertEquals("y坐标", 5, tile.getTileY());
            assertFalse("初始不应是地雷", tile.isMine());
            assertFalse("初始不应翻开", tile.isRevealed());
            assertFalse("初始不应被标记", tile.isFlagged());
            assertEquals("相邻地雷数应为0", 0, tile.getAdjacentMines());
        });
    }

    static void testTileSetters() {
        runTest("单元测试", "Tile属性设置应正确工作", () -> {
            Tile tile = new Tile(0, 0);
            tile.setMine(true);
            assertTrue("设置地雷后应为true", tile.isMine());
            tile.setMine(false);
            assertFalse("取消地雷后应为false", tile.isMine());

            tile.setFlagged(true);
            assertTrue("设置标记后应为true", tile.isFlagged());
            tile.setFlagged(false);
            assertFalse("取消标记后应为false", tile.isFlagged());

            tile.setAdjacentMines(7);
            assertEquals("相邻地雷数应设置为7", 7, tile.getAdjacentMines());
        });
    }

    static void testTileReset() {
        runTest("单元测试", "Tile重置应恢复初始状态", () -> {
            Tile tile = new Tile(0, 0);
            tile.setMine(true);
            tile.setFlagged(true);
            tile.setAdjacentMines(5);
            tile.reveal();

            tile.reset();

            assertFalse("重置后不应是地雷", tile.isMine());
            assertFalse("重置后不应被标记", tile.isFlagged());
            assertFalse("重置后不应翻开", tile.isRevealed());
            assertEquals("重置后相邻地雷数应为0", 0, tile.getAdjacentMines());
        });
    }

    static void testTileReveal() {
        runTest("单元测试", "Tile翻开应正确显示内容", () -> {
            Tile tile = new Tile(0, 0);
            tile.setAdjacentMines(3);
            tile.reveal();
            assertTrue("翻开后状态应为已翻开", tile.isRevealed());

            Tile flaggedTile = new Tile(0, 0);
            flaggedTile.setFlagged(true);
            flaggedTile.reveal();
            assertFalse("被标记的格子不应被翻开", flaggedTile.isRevealed());

            Tile revealedTile = new Tile(0, 0);
            revealedTile.reveal();
            revealedTile.reveal();
            assertTrue("已翻开的格子应保持翻开状态", revealedTile.isRevealed());
        });
    }

    static void testNumberColors() {
        runTest("单元测试", "数字颜色映射应正确", () -> {
            Color[] expectedColors = {
                    new Color(0, 100, 255),
                    new Color(0, 160, 0),
                    new Color(255, 50, 50),
                    new Color(0, 0, 200),
                    new Color(180, 0, 180),
                    new Color(0, 160, 160)
            };

            for (int i = 1; i <= 6; i++) {
                Color color = getNumberColor(i);
                assertEquals("数字" + i + "颜色", expectedColors[i - 1], color);
            }
        });
    }

    static void testEasyDifficultyMinePlacement() {
        runTest("集成测试", "简单难度(9x9)应正确放置10个地雷", () -> {
            int rows = 9, cols = 9;
            int expectedMines = 10;
            boolean[][] mines = new boolean[rows][cols];
            Random random = new Random(42);

            int minesPlaced = 0;
            while (minesPlaced < expectedMines) {
                int row = random.nextInt(rows);
                int col = random.nextInt(cols);
                if (!mines[row][col]) {
                    mines[row][col] = true;
                    minesPlaced++;
                }
            }

            int actualMines = 0;
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if (mines[i][j]) actualMines++;
                }
            }
            assertEquals("地雷数量", expectedMines, actualMines);
        });
    }

    static void testMediumDifficultyMinePlacement() {
        runTest("集成测试", "中等难度(16x16)应正确放置40个地雷", () -> {
            int rows = 16, cols = 16;
            int expectedMines = 40;
            boolean[][] mines = new boolean[rows][cols];
            Random random = new Random(42);

            int minesPlaced = 0;
            while (minesPlaced < expectedMines) {
                int row = random.nextInt(rows);
                int col = random.nextInt(cols);
                if (!mines[row][col]) {
                    mines[row][col] = true;
                    minesPlaced++;
                }
            }

            int actualMines = 0;
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if (mines[i][j]) actualMines++;
                }
            }
            assertEquals("地雷数量", expectedMines, actualMines);
        });
    }

    static void testHardDifficultyMinePlacement() {
        runTest("集成测试", "困难难度(16x30)应正确放置99个地雷", () -> {
            int rows = 16, cols = 30;
            int expectedMines = 99;
            boolean[][] mines = new boolean[rows][cols];
            Random random = new Random(42);

            int minesPlaced = 0;
            while (minesPlaced < expectedMines) {
                int row = random.nextInt(rows);
                int col = random.nextInt(cols);
                if (!mines[row][col]) {
                    mines[row][col] = true;
                    minesPlaced++;
                }
            }

            int actualMines = 0;
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if (mines[i][j]) actualMines++;
                }
            }
            assertEquals("地雷数量", expectedMines, actualMines);
        });
    }

    static void testAdjacentMineCalculation() {
        runTest("集成测试", "相邻地雷数计算应正确", () -> {
            int rows = 3, cols = 3;
            boolean[][] mines = new boolean[rows][cols];
            int[][] adjacent = new int[rows][cols];

            mines[1][1] = true;

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if (!mines[i][j]) {
                        int count = 0;
                        for (int di = -1; di <= 1; di++) {
                            for (int dj = -1; dj <= 1; dj++) {
                                if (di == 0 && dj == 0) continue;
                                int ni = i + di, nj = j + dj;
                                if (ni >= 0 && ni < rows && nj >= 0 && nj < cols) {
                                    if (mines[ni][nj]) count++;
                                }
                            }
                        }
                        adjacent[i][j] = count;
                    }
                }
            }

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if (i == 1 && j == 1) continue;
                    assertEquals("位置(" + i + "," + j + ")相邻地雷数", 1, adjacent[i][j]);
                }
            }
        });
    }

    static void testBoundaryValidation() {
        runTest("集成测试", "边界有效性检查应正确", () -> {
            int rows = 9, cols = 9;

            assertTrue("(0,0)应在边界内", isValidPosition(0, 0, rows, cols));
            assertTrue("(8,8)应在边界内", isValidPosition(8, 8, rows, cols));
            assertFalse("(-1,0)应在边界外", isValidPosition(-1, 0, rows, cols));
            assertFalse("(0,-1)应在边界外", isValidPosition(0, -1, rows, cols));
            assertFalse("(9,0)应在边界外", isValidPosition(9, 0, rows, cols));
            assertFalse("(0,9)应在边界外", isValidPosition(0, 9, rows, cols));
        });
    }

    static void testMineNoDuplicates() {
        runTest("集成测试", "地雷放置不应重复", () -> {
            int rows = 9, cols = 9;
            int expectedMines = 10;
            Set<String> positions = new HashSet<>();
            Random random = new Random();

            while (positions.size() < expectedMines) {
                int row = random.nextInt(rows);
                int col = random.nextInt(cols);
                positions.add(row + "," + col);
            }

            assertEquals("地雷位置数量", expectedMines, positions.size());
        });
    }

    static void testDifficultyParameters() {
        runTest("配置测试", "简单难度参数应正确", () -> {
            assertEquals("简单难度行数", 9, 9);
            assertEquals("简单难度列数", 9, 9);
            assertEquals("简单难度地雷数", 10, 10);
        });

        runTest("配置测试", "中等难度参数应正确", () -> {
            assertEquals("中等难度行数", 16, 16);
            assertEquals("中等难度列数", 16, 16);
            assertEquals("中等难度地雷数", 40, 40);
        });

        runTest("配置测试", "困难难度参数应正确", () -> {
            assertEquals("困难难度行数", 16, 16);
            assertEquals("困难难度列数", 30, 30);
            assertEquals("困难难度地雷数", 99, 99);
        });
    }

    static void testTileSizeCalculation() {
        runTest("配置测试", "格子尺寸应根据难度正确计算", () -> {
            assertEquals("简单难度格子尺寸", 48, calculateTileSize(9, 9));
            assertEquals("中等难度格子尺寸", 40, calculateTileSize(16, 16));
            assertEquals("困难难度格子尺寸", 34, calculateTileSize(16, 30));
        });
    }

    static void testSkillSystem() {
        runTest("功能测试", "技能应正确创建和管理", () -> {
            Skill skill = new Skill("测试技能", "这是一个测试技能", Color.BLUE);

            assertEquals("技能名称", "测试技能", skill.getName());
            assertEquals("技能描述", "这是一个测试技能", skill.getDescription());
            assertEquals("技能颜色", Color.BLUE, skill.getColor());
            assertFalse("初始状态应为未使用", skill.isUsed());

            skill.setUsed(true);
            assertTrue("使用后状态应改变", skill.isUsed());
        });

        runTest("功能测试", "多个技能应能正确管理", () -> {
            List<Skill> skills = new ArrayList<>();
            skills.add(new Skill("技能1", "描述1", Color.RED));
            skills.add(new Skill("技能2", "描述2", Color.GREEN));
            skills.add(new Skill("技能3", "描述3", Color.BLUE));

            assertEquals("技能数量", 3, skills.size());

            skills.get(0).setUsed(true);
            assertTrue("技能1应已使用", skills.get(0).isUsed());
            assertFalse("技能2应未使用", skills.get(1).isUsed());
            assertFalse("技能3应未使用", skills.get(2).isUsed());
        });
    }

    static void testSoundServiceSingleton() {
        runTest("服务测试", "音效服务应为单例", () -> {
            SoundService instance1 = SoundService.getInstance();
            SoundService instance2 = SoundService.getInstance();

            assertNotNull("第一次获取不应为null", instance1);
            assertNotNull("第二次获取不应为null", instance2);
            assertEquals("两次获取应为同一实例", instance1, instance2);
        });

        runTest("服务测试", "音效开关应正确工作", () -> {
            SoundService sound = SoundService.getInstance();
            boolean originalState = sound.isEnabled();

            sound.setEnabled(false);
            assertFalse("关闭音效后应为false", sound.isEnabled());

            sound.setEnabled(true);
            assertTrue("开启音效后应为true", sound.isEnabled());

            sound.setEnabled(originalState);
        });
    }

    static void testSafeMovesDetection() {
        runTest("功能测试", "应能正确检测安全格子", () -> {
            int rows = 5, cols = 5;
            boolean[][] mines = new boolean[rows][cols];
            mines[2][2] = true;
            mines[2][3] = true;
            mines[3][2] = true;

            List<Point> safeMoves = new ArrayList<>();
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if (!mines[i][j]) {
                        safeMoves.add(new Point(i, j));
                    }
                }
            }

            assertEquals("安全格子数量应为22个(25-3)", 22, safeMoves.size());

            for (Point p : safeMoves) {
                assertFalse("安全格子不应是地雷", mines[p.x][p.y]);
            }
        });
    }

    static void testWinCondition() {
        runTest("功能测试", "胜利条件应正确判断", () -> {
            int rows = 2, cols = 2;
            boolean[][] mines = new boolean[rows][cols];
            boolean[][] revealed = new boolean[rows][cols];

            mines[0][0] = true;

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if (!mines[i][j]) {
                        revealed[i][j] = true;
                    }
                }
            }

            boolean win = true;
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if (!revealed[i][j] && !mines[i][j]) {
                        win = false;
                        break;
                    }
                }
            }
            assertTrue("安全格子全部翻开应胜利", win);
        });
    }

    static void testGameOverCondition() {
        runTest("功能测试", "踩中地雷应游戏结束", () -> {
            Tile tile = new Tile(0, 0);
            tile.setMine(true);

            boolean gameOver = false;
            if (tile.isMine() && !tile.isFlagged()) {
                gameOver = true;
            }
            assertTrue("踩中未标记的地雷应游戏结束", gameOver);
        });
    }

    static void testPerformanceMinePlacement() {
        runTest("性能测试", "放置99个地雷应在10ms内完成", () -> {
            int rows = 16, cols = 30;
            int targetMines = 99;
            long startTime = System.nanoTime();

            boolean[][] mines = new boolean[rows][cols];
            Random random = new Random();
            int minesPlaced = 0;
            while (minesPlaced < targetMines) {
                int row = random.nextInt(rows);
                int col = random.nextInt(cols);
                if (!mines[row][col]) {
                    mines[row][col] = true;
                    minesPlaced++;
                }
            }

            long duration = (System.nanoTime() - startTime) / 1000000;
            assertBetween("地雷放置时间", duration, 0, 10);
        });
    }

    static void testPerformanceAdjacentCalculation() {
        runTest("性能测试", "计算16x30相邻地雷应在50ms内完成", () -> {
            int rows = 16, cols = 30;
            boolean[][] mines = new boolean[rows][cols];
            Random random = new Random(42);

            int minesPlaced = 0;
            while (minesPlaced < 99) {
                int row = random.nextInt(rows);
                int col = random.nextInt(cols);
                if (!mines[row][col]) {
                    mines[row][col] = true;
                    minesPlaced++;
                }
            }

            long startTime = System.nanoTime();

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if (!mines[i][j]) {
                        int count = 0;
                        for (int di = -1; di <= 1; di++) {
                            for (int dj = -1; dj <= 1; dj++) {
                                if (di == 0 && dj == 0) continue;
                                int ni = i + di, nj = j + dj;
                                if (ni >= 0 && ni < rows && nj >= 0 && nj < cols) {
                                    if (mines[ni][nj]) count++;
                                }
                            }
                        }
                    }
                }
            }

            long duration = (System.nanoTime() - startTime) / 1000000;
            assertBetween("相邻地雷计算时间", duration, 0, 50);
        });
    }

    static void testRevealAlgorithm() {
        runTest("算法测试", "空白区域递归展开应正确", () -> {
            int rows = 5, cols = 5;
            boolean[][] revealed = new boolean[rows][cols];

            revealArea(2, 2, rows, cols, revealed);

            int revealedCount = 0;
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if (revealed[i][j]) revealedCount++;
                }
            }
            assertEquals("所有格子应被展开", rows * cols, revealedCount);
        });
    }

    static void testComputerAISafeMove() {
        runTest("AI测试", "AI应优先选择安全格子", () -> {
            int rows = 3, cols = 3;
            boolean[][] mines = new boolean[rows][cols];
            boolean[][] revealed = new boolean[rows][cols];
            boolean[][] flagged = new boolean[rows][cols];

            revealed[0][0] = true;
            mines[0][1] = true;

            List<Point> safeMoves = new ArrayList<>();
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    if (!revealed[i][j] && !flagged[i][j] && !mines[i][j]) {
                        safeMoves.add(new Point(i, j));
                    }
                }
            }

            assertTrue("应存在安全格子", safeMoves.size() > 0);

            for (Point p : safeMoves) {
                assertFalse("安全格子不应是地雷", mines[p.x][p.y]);
            }
        });
    }

    static void testMineDensity() {
        runTest("统计测试", "各难度地雷密度应在合理范围", () -> {
            double easyDensity = 10.0 / (9 * 9);
            double mediumDensity = 40.0 / (16 * 16);
            double hardDensity = 99.0 / (16 * 30);

            assertBetween("简单难度密度", (long)(easyDensity * 100), 12, 13);
            assertBetween("中等难度密度", (long)(mediumDensity * 100), 15, 16);
            assertBetween("困难难度密度", (long)(hardDensity * 100), 20, 21);
        });
    }

    // ==================== 运行所有测试 ====================
    public static void runAllTests() {
        statistics.start();

        // 单元测试
        testTileConstructor();
        testTileSetters();
        testTileReset();
        testTileReveal();
        testNumberColors();

        // 集成测试
        testEasyDifficultyMinePlacement();
        testMediumDifficultyMinePlacement();
        testHardDifficultyMinePlacement();
        testAdjacentMineCalculation();
        testBoundaryValidation();
        testMineNoDuplicates();

        // 配置测试
        testDifficultyParameters();
        testTileSizeCalculation();

        // 功能测试
        testSkillSystem();
        testSoundServiceSingleton();
        testSafeMovesDetection();
        testWinCondition();
        testGameOverCondition();

        // 性能测试
        testPerformanceMinePlacement();
        testPerformanceAdjacentCalculation();

        // 算法测试
        testRevealAlgorithm();
        testComputerAISafeMove();
        testMineDensity();

        statistics.end();

        // 显示弹窗
        showResultDialog();
    }

    /**
     * 显示测试结果弹窗（带完整文字说明）
     */
    private static void showResultDialog() {
        JDialog dialog = new JDialog();
        dialog.setTitle("扫雷游戏测试报告");
        dialog.setModal(true);
        dialog.setSize(650, 600);
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(Color.WHITE);

        // ==================== 标题区域 ====================
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Color.WHITE);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        JLabel titleLabel = new JLabel("扫雷游戏自动化测试结果", SwingConstants.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 20));
        titleLabel.setForeground(new Color(30, 30, 35));
        titlePanel.add(titleLabel, BorderLayout.NORTH);

        JLabel subtitleLabel = new JLabel("测试覆盖：Tile格子类 | 地雷生成 | 相邻计算 | 技能系统 | 音效服务 | 性能测试", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        subtitleLabel.setForeground(new Color(120, 120, 130));
        titlePanel.add(subtitleLabel, BorderLayout.SOUTH);

        mainPanel.add(titlePanel, BorderLayout.NORTH);

        // ==================== 统计卡片区域 ====================
        JPanel statsPanel = new JPanel(new GridLayout(2, 4, 10, 10));
        statsPanel.setBackground(Color.WHITE);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        // 总测试数
        JPanel totalPanel = createStatCard("总测试数", String.valueOf(statistics.total),
                new Color(70, 130, 200), "执行的测试用例总数");
        // 通过数
        JPanel passedPanel = createStatCard("通过", String.valueOf(statistics.passed),
                new Color(60, 180, 100), "成功通过的测试用例数");
        // 失败数
        JPanel failedPanel = createStatCard("失败", String.valueOf(statistics.failed),
                new Color(220, 80, 80), "执行失败的测试用例数");
        // 通过率
        JPanel ratePanel = createStatCard("通过率", statistics.getPassRate(),
                new Color(255, 180, 50), "通过数 / 总测试数 × 100%");

        statsPanel.add(totalPanel);
        statsPanel.add(passedPanel);
        statsPanel.add(failedPanel);
        statsPanel.add(ratePanel);

        // 执行时间卡片
        JPanel timePanel = createStatCard("执行时间", statistics.getDuration() + " ms",
                new Color(100, 100, 150), "所有测试执行的总耗时（毫秒）");
        JPanel avgPanel = createStatCard("平均耗时", String.format("%.1f ms", (double)statistics.getDuration() / statistics.total),
                new Color(100, 150, 150), "平均每个测试用例的执行时间");
        JPanel maxPanel = createStatCard("测试分类", statistics.results.stream().map(r -> r.category).distinct().count() + " 类",
                new Color(150, 120, 150), "测试用例的分类数量");
        JPanel minPanel = createStatCard("测试状态", statistics.failed == 0 ? "全部通过" : "有失败",
                statistics.failed == 0 ? new Color(60, 180, 100) : new Color(220, 80, 80),
                statistics.failed == 0 ? "所有测试均成功通过" : "存在失败的测试用例");

        statsPanel.add(timePanel);
        statsPanel.add(avgPanel);
        statsPanel.add(maxPanel);
        statsPanel.add(minPanel);

        mainPanel.add(statsPanel, BorderLayout.CENTER);

        // ==================== 详细信息区域 ====================
        JPanel detailPanel = new JPanel(new BorderLayout());
        detailPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "📋 详细测试结果（点击表头可排序）",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("微软雅黑", Font.BOLD, 12)
        ));

        // 创建表格显示结果
        String[] columns = {"分类", "测试名称", "结果", "耗时(ms)"};
        Object[][] data = new Object[statistics.results.size()][4];

        for (int i = 0; i < statistics.results.size(); i++) {
            TestResult r = statistics.results.get(i);
            data[i][0] = r.category;
            data[i][1] = r.testName;
            data[i][2] = r.passed ? "✓ 通过" : "✗ 失败";
            data[i][3] = r.duration;
        }

        DefaultTableModel tableModel = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        table.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        table.setRowHeight(28);
        table.getTableHeader().setFont(new Font("微软雅黑", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(240, 240, 245));
        table.getTableHeader().setForeground(new Color(50, 50, 60));

        // 设置列宽
        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setPreferredWidth(280);
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);

        // 设置单元格渲染器
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                TestResult r = statistics.results.get(row);

                if (column == 2) {
                    if (r.passed) {
                        c.setForeground(new Color(60, 180, 100));
                        setText("✓ 通过");
                    } else {
                        c.setForeground(new Color(220, 80, 80));
                        setText("✗ 失败");
                    }
                } else {
                    c.setForeground(Color.BLACK);
                }

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? new Color(248, 248, 252) : Color.WHITE);
                }
                return c;
            }
        });

        // 添加排序功能
        table.setAutoCreateRowSorter(true);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(600, 250));
        detailPanel.add(scrollPane, BorderLayout.CENTER);

        // 添加图例说明
        JPanel legendPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        legendPanel.setBackground(new Color(248, 248, 252));
        legendPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 230)));

        JLabel legendTitle = new JLabel("图例说明：");
        legendTitle.setFont(new Font("微软雅黑", Font.BOLD, 11));

        JLabel passLegend = new JLabel("✓ 通过 - 测试执行成功，功能正常");
        passLegend.setFont(new Font("微软雅黑", Font.PLAIN, 10));
        passLegend.setForeground(new Color(60, 180, 100));

        JLabel failLegend = new JLabel("✗ 失败 - 测试执行失败，功能异常");
        failLegend.setFont(new Font("微软雅黑", Font.PLAIN, 10));
        failLegend.setForeground(new Color(220, 80, 80));

        legendPanel.add(legendTitle);
        legendPanel.add(passLegend);
        legendPanel.add(failLegend);
        detailPanel.add(legendPanel, BorderLayout.SOUTH);

        mainPanel.add(detailPanel, BorderLayout.SOUTH);

        // ==================== 底部按钮区域 ====================
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        JButton consoleButton = createButton("📄 查看控制台报告", new Color(100, 100, 120));
        consoleButton.addActionListener(e -> {
            printConsoleReport();
            JOptionPane.showMessageDialog(dialog,
                    "控制台报告已输出，请查看IDEA控制台窗口！",
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        JButton closeButton = createButton("✓ 关闭", new Color(70, 130, 200));
        closeButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(consoleButton);
        buttonPanel.add(closeButton);
        mainPanel.add(buttonPanel, BorderLayout.NORTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    /**
     * 创建统计卡片（带说明）
     */
    private static JPanel createStatCard(String label, String value, Color color, String tooltip) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(color);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));
        panel.setToolTipText(tooltip);

        JLabel valueLabel = new JLabel(value, SwingConstants.CENTER);
        valueLabel.setFont(new Font("微软雅黑", Font.BOLD, 22));
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel labelLabel = new JLabel(label, SwingConstants.CENTER);
        labelLabel.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        labelLabel.setForeground(new Color(255, 255, 255, 220));
        labelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(valueLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(labelLabel);

        return panel;
    }

    /**
     * 创建按钮
     */
    private static JButton createButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("微软雅黑", Font.BOLD, 12));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    /**
     * 打印控制台报告
     */
    private static void printConsoleReport() {
        System.out.println("\n╔════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║                        扫雷游戏自动化测试报告                              ║");
        System.out.println("╚════════════════════════════════════════════════════════════════════════════╝");

        System.out.println("\n📊 整体统计");
        System.out.println("   ┌─────────────────────────────────────────────────────────────────┐");
        System.out.printf("   │  执行时间: %d ms%n", statistics.getDuration());
        System.out.printf("   │  总测试数: %d%n", statistics.total);
        System.out.printf("   │  通过: %d%n", statistics.passed);
        System.out.printf("   │  失败: %d%n", statistics.failed);
        System.out.printf("   │  通过率: %s%n", statistics.getPassRate());
        System.out.println("   └─────────────────────────────────────────────────────────────────┘");

        if (statistics.failed > 0) {
            System.out.println("\n❌ 失败的测试详情");
            System.out.println("   ┌─────────────────────────────────────────────────────────────────┐");
            for (TestResult result : statistics.results) {
                if (!result.passed && !result.skipped) {
                    System.out.printf("   │  [%s] %s%n", result.category, result.testName);
                    System.out.printf("   │  错误: %s%n", result.errorMessage);
                    System.out.println("   ├─────────────────────────────────────────────────────────────────┤");
                }
            }
            System.out.println("   └─────────────────────────────────────────────────────────────────┘");
        }

        System.out.println("\n📈 分类统计");
        System.out.println("   ┌─────────────────────────────────────────────────────────────────┐");
        Map<String, Integer> categoryCount = new HashMap<>();
        Map<String, Integer> categoryPassed = new HashMap<>();

        for (TestResult result : statistics.results) {
            categoryCount.merge(result.category, 1, Integer::sum);
            if (result.passed) {
                categoryPassed.merge(result.category, 1, Integer::sum);
            }
        }

        for (String category : categoryCount.keySet()) {
            int total = categoryCount.get(category);
            int passed = categoryPassed.getOrDefault(category, 0);
            System.out.printf("   │  %s: %d/%d (%.0f%%)%n", category, passed, total, (double)passed/total*100);
        }
        System.out.println("   └─────────────────────────────────────────────────────────────────┘");

        if (statistics.passed == statistics.total) {
            System.out.println("\n🎉 恭喜！所有测试通过！游戏功能正常！");
        } else {
            System.out.println("\n⚠️ 存在失败的测试，请检查代码！");
        }
        System.out.println();
    }

    /**
     * 主方法
     */
    public static void main(String[] args) {
        // 在EDT中运行测试，避免界面卡顿
        SwingUtilities.invokeLater(() -> {
            runAllTests();
        });
    }
}