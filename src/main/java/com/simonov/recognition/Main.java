package com.simonov.recognition;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class Main {
    private final static int WHITE = 0xffffffff;
    private final static int GRAY = 0xff787878;
    private final static int RED = 0xfff00000;

    private final static int CARD_MAX = 5;
    private final static int MATCH_THRESHOLD = 140;
    private final static int WIDTH = 31;
    private final static int HEIGHT = 28;
    private final static int OFFSET = 71;
    private final static int RANK_FRAME_START_X = 147;
    private final static int RANK_FRAME_START_Y = 591;
    private final static int SUIT_FRAME_START_X = 170;
    private final static int SUIT_FRAME_START_Y = 639;

    private final Map<String, int[]> rankGlyphs;
    private final Map<String, int[]> suitGlyphs;

    public Main() throws IOException {
        Path directory = Paths.get("src/main/resources/poker");

        Map<String, List<Path>> sampleRankFiles = Files.list(directory)
                .collect(Collectors.groupingBy(s -> s.getFileName().toString().split("[cdhs]")[1]));
        rankGlyphs = createGlyphs(sampleRankFiles, 217, 590);

        Map<String, List<Path>> sampleSuitFiles = Files.list(directory)
                .collect(Collectors.groupingBy(s -> s.getFileName().toString().split("[0-9JQKA]{1,2}")[1]));
        suitGlyphs = createGlyphs(sampleSuitFiles, 170, SUIT_FRAME_START_Y);
    }

    public static void main(String[] args) throws IOException {
        Main main = new Main();
        Path directory = Paths.get("src/main/resources/poker");
        Files.list(directory)
                .map(main::recognize)
                .forEach(System.out::println);
    }

    private BufferedImage read(Path imageFile) {
        try {
            return ImageIO.read(Files.newInputStream(imageFile));
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String recognize(Path imageFile) {
        StringBuilder output = new StringBuilder();
            output.append(imageFile.getFileName())
                    .append(" - ");

        BufferedImage cardImage = read(imageFile);
        for (int i = 0; i < CARD_MAX; ++ i) {
            output.append(recognizeRank(cardImage, RANK_FRAME_START_X + i * OFFSET, RANK_FRAME_START_Y));
            output.append(recognizeSuit(cardImage, SUIT_FRAME_START_X + i * OFFSET, SUIT_FRAME_START_Y));
        }

        assert output.toString().endsWith(imageFile.getFileName().toString().split("\\.")[0]) : output.toString();
        return output.toString();
    }

    private String recognizeRank(BufferedImage cardImage, int startX, int startY) {
        int[] rankPixels = adjustFrame(cardImage, startX, startY);
        return match(rankGlyphs, rankPixels);
    }

    private String recognizeSuit(BufferedImage cardImage, int startX, int startY) {
        int[] suitPixels = adjustFrame(cardImage, startX, startY);
        return match(suitGlyphs, suitPixels);
    }

    private String match(Map<String, int[]> glyphs, int[] pixels) {
        String glyphName = "";
        int minDelta = MATCH_THRESHOLD;
        for (Map.Entry<String, int[]> e : glyphs.entrySet()) {
            int delta = 0;
            for (int i = 0; i < pixels.length; ++ i) {
                if (pixels[i] != WHITE && pixels[i] != GRAY
                        && (e.getValue()[i] == WHITE || e.getValue()[i] == GRAY)
                        || (pixels[i] == WHITE || pixels[i] == GRAY)
                        && e.getValue()[i] != WHITE && e.getValue()[i] != GRAY)
                    delta += 1;
            }
            if (delta < minDelta && delta < MATCH_THRESHOLD) {
                minDelta = delta;
                glyphName = e.getKey();
            }
        }
        return glyphName;
    }

    Map<String, int[]> createGlyphs(Map<String, List<Path>> sampleFilePaths, int startX, int startY) throws IOException {
        Map<String, int[]> glyphs = new HashMap<>();
        for (Map.Entry<String, List<Path>> e : sampleFilePaths.entrySet()) {
            String glyphName = e.getKey();
            BufferedImage image = ImageIO.read(Files.newInputStream(e.getValue().get(0)));
            int[] tmp = adjustFrame(image, startX, startY);
            glyphs.put(glyphName, tmp);
        }
        return glyphs;
    }

    private int[] adjustFrame(BufferedImage frame, int startX, int startY) {
        int offsetX = 0;
        outerX: for (int i = 0; i < 10; ++ i) {
            int j;
            for (j = 0; j < HEIGHT; ++ j) {
                int pixel = frame.getRGB(startX + i, startY + j);
                if (pixel < RED && pixel != GRAY)
                    break outerX;
            }
            offsetX += 1;
        }
        int offsetY = 0;
        outerY: for (int i = 0; i < 10; ++ i) {
            int j;
            for (j = 0; j < WIDTH; ++ j) {
                int pixel = frame.getRGB(startX + j, startY + i);
                if (pixel < RED && pixel != GRAY)
                    break outerY;
            }
            offsetY += 1;
        }
        int[] pixels = new int[WIDTH * HEIGHT];
        frame.getRGB(startX + offsetX, startY + offsetY, WIDTH, HEIGHT, pixels, 0, WIDTH);
        return pixels;
    }
}
