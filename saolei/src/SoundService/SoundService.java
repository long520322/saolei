package com.minesweeper;

import javax.sound.sampled.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class SoundService {

    private static SoundService instance;
    private Map<String, Clip> sounds;
    private boolean enabled;

    public static final String SOUND_CLICK = "click";
    public static final String SOUND_EXPLODE = "explode";
    public static final String SOUND_WIN = "win";
    public static final String SOUND_FLAG = "flag";
    public static final String SOUND_GAMEOVER = "gameover";
    public static final String SOUND_SKILL = "skill";
    public static final String SOUND_COMPUTER = "computer";

    private SoundService() {
        sounds = new HashMap<>();
        enabled = true;
        createAllSounds();
    }

    public static synchronized SoundService getInstance() {
        if (instance == null) {
            instance = new SoundService();
        }
        return instance;
    }

    private void createAllSounds() {
        createClickSound();
        createExplodeSound();
        createWinSound();
        createFlagSound();
        createGameOverSound();
        createSkillSound();
        createComputerSound();
    }

    private void createClickSound() {
        try {
            float sampleRate = 44100;
            int duration = 80;
            int frequency = 1200;
            int numSamples = (int)(sampleRate * duration / 1000);
            byte[] audioData = new byte[numSamples * 2];

            for (int i = 0; i < numSamples; i++) {
                double angle = 2.0 * Math.PI * frequency * i / sampleRate;
                double envelope = Math.exp(-3.0 * i / numSamples);
                short sample = (short)(Math.sin(angle) * 30000 * envelope);
                audioData[i*2] = (byte)(sample & 0xFF);
                audioData[i*2+1] = (byte)((sample >> 8) & 0xFF);
            }

            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioStream = new AudioInputStream(bais, format, numSamples);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            sounds.put(SOUND_CLICK, clip);
        } catch (Exception e) {}
    }

    private void createExplodeSound() {
        try {
            float sampleRate = 44100;
            int duration = 600;
            int numSamples = (int)(sampleRate * duration / 1000);
            byte[] audioData = new byte[numSamples * 2];

            for (int i = 0; i < numSamples; i++) {
                double t = (double)i / sampleRate;
                double sample = 0;
                sample += Math.sin(2 * Math.PI * 80 * t) * Math.exp(-5 * t);
                sample += Math.sin(2 * Math.PI * 150 * t) * Math.exp(-4 * t);
                sample += Math.sin(2 * Math.PI * 300 * t) * Math.exp(-3 * t);
                sample += Math.random() * 0.3 * Math.exp(-2 * t);
                short output = (short)(sample * 25000);
                audioData[i*2] = (byte)(output & 0xFF);
                audioData[i*2+1] = (byte)((output >> 8) & 0xFF);
            }

            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioStream = new AudioInputStream(bais, format, numSamples);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            sounds.put(SOUND_EXPLODE, clip);
        } catch (Exception e) {}
    }

    private void createWinSound() {
        try {
            float sampleRate = 44100;
            int[] notes = {523, 587, 659, 698, 784, 880, 988, 1047};
            int durationPerNote = 150;
            int totalSamples = 0;

            for (int freq : notes) {
                totalSamples += (int)(sampleRate * durationPerNote / 1000);
            }

            byte[] audioData = new byte[totalSamples * 2];
            int sampleIndex = 0;

            for (int freq : notes) {
                int numSamples = (int)(sampleRate * durationPerNote / 1000);
                for (int i = 0; i < numSamples; i++) {
                    double angle = 2.0 * Math.PI * freq * i / sampleRate;
                    double envelope = Math.sin(Math.PI * i / numSamples);
                    short sample = (short)(Math.sin(angle) * 25000 * envelope);
                    audioData[sampleIndex*2] = (byte)(sample & 0xFF);
                    audioData[sampleIndex*2+1] = (byte)((sample >> 8) & 0xFF);
                    sampleIndex++;
                }
            }

            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioStream = new AudioInputStream(bais, format, totalSamples);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            sounds.put(SOUND_WIN, clip);
        } catch (Exception e) {}
    }

    private void createFlagSound() {
        try {
            float sampleRate = 44100;
            int duration = 120;
            int frequency = 800;
            int numSamples = (int)(sampleRate * duration / 1000);
            byte[] audioData = new byte[numSamples * 2];

            for (int i = 0; i < numSamples; i++) {
                double angle = 2.0 * Math.PI * frequency * i / sampleRate;
                double envelope = Math.sin(Math.PI * i / numSamples);
                short sample = (short)(Math.sin(angle) * 20000 * envelope);
                audioData[i*2] = (byte)(sample & 0xFF);
                audioData[i*2+1] = (byte)((sample >> 8) & 0xFF);
            }

            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioStream = new AudioInputStream(bais, format, numSamples);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            sounds.put(SOUND_FLAG, clip);
        } catch (Exception e) {}
    }

    private void createGameOverSound() {
        try {
            float sampleRate = 44100;
            int duration = 800;
            int startFreq = 400;
            int endFreq = 150;
            int numSamples = (int)(sampleRate * duration / 1000);
            byte[] audioData = new byte[numSamples * 2];

            for (int i = 0; i < numSamples; i++) {
                double t = (double)i / numSamples;
                int freq = (int)(startFreq + (endFreq - startFreq) * t);
                double angle = 2.0 * Math.PI * freq * i / sampleRate;
                double envelope = Math.exp(-3 * t);
                short sample = (short)(Math.sin(angle) * 25000 * envelope);
                audioData[i*2] = (byte)(sample & 0xFF);
                audioData[i*2+1] = (byte)((sample >> 8) & 0xFF);
            }

            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioStream = new AudioInputStream(bais, format, numSamples);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            sounds.put(SOUND_GAMEOVER, clip);
        } catch (Exception e) {}
    }

    private void createSkillSound() {
        try {
            float sampleRate = 44100;
            int duration = 400;
            int[] frequencies = {600, 800, 1000, 1200};
            int numSamples = (int)(sampleRate * duration / 1000);
            byte[] audioData = new byte[numSamples * 2];

            for (int i = 0; i < numSamples; i++) {
                double t = (double)i / numSamples;
                int freqIndex = (int)(t * frequencies.length);
                if (freqIndex >= frequencies.length) freqIndex = frequencies.length - 1;
                int freq = frequencies[freqIndex];
                double angle = 2.0 * Math.PI * freq * i / sampleRate;
                double envelope = Math.sin(Math.PI * t);
                short sample = (short)(Math.sin(angle) * 20000 * envelope);
                audioData[i*2] = (byte)(sample & 0xFF);
                audioData[i*2+1] = (byte)((sample >> 8) & 0xFF);
            }

            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioStream = new AudioInputStream(bais, format, numSamples);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            sounds.put(SOUND_SKILL, clip);
        } catch (Exception e) {}
    }

    private void createComputerSound() {
        try {
            float sampleRate = 44100;
            int duration = 100;
            int frequency = 900;
            int numSamples = (int)(sampleRate * duration / 1000);
            byte[] audioData = new byte[numSamples * 2];

            for (int i = 0; i < numSamples; i++) {
                double angle = 2.0 * Math.PI * frequency * i / sampleRate;
                short sample = (short)(Math.sin(angle) * 15000);
                audioData[i*2] = (byte)(sample & 0xFF);
                audioData[i*2+1] = (byte)((sample >> 8) & 0xFF);
            }

            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioStream = new AudioInputStream(bais, format, numSamples);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            sounds.put(SOUND_COMPUTER, clip);
        } catch (Exception e) {}
    }

    public void play(String soundName) {
        if (!enabled) return;
        Clip clip = sounds.get(soundName);
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }

    public void playClick() { play(SOUND_CLICK); }
    public void playExplode() { play(SOUND_EXPLODE); }
    public void playWin() { play(SOUND_WIN); }
    public void playFlag() { play(SOUND_FLAG); }
    public void playGameOver() { play(SOUND_GAMEOVER); }
    public void playSkill() { play(SOUND_SKILL); }
    public void playComputer() { play(SOUND_COMPUTER); }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled() { return enabled; }

    public void preload() {
        for (Clip clip : sounds.values()) {
            clip.getFrameLength();
        }
        System.out.println("音效加载完成，共 " + sounds.size() + " 个音效");
    }
}