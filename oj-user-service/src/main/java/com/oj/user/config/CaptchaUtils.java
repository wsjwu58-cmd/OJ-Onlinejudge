package com.oj.user.config;

import com.oj.user.entity.Captcha;
import org.apache.commons.lang3.RandomUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.Objects;
import java.util.Random;

public class CaptchaUtils {

    private final static String IMG_PATH = "E:/images/3.jpg";

    public static void checkCaptcha(Captcha captcha) {
        if (captcha.getCanvasWidth() == null) {
            captcha.setCanvasWidth(320);
        }
        if (captcha.getCanvasHeight() == null) {
            captcha.setCanvasHeight(155);
        }
        if (captcha.getBlockWidth() == null) {
            captcha.setBlockWidth(65);
        }
        if (captcha.getBlockHeight() == null) {
            captcha.setBlockHeight(55);
        }
        if (captcha.getBlockRadius() == null) {
            captcha.setBlockRadius(9);
        }
        if (captcha.getPlace() == null) {
            captcha.setPlace(0);
        }
    }

    public static int getNonceByRange(int start, int end) {
        Random random = new Random();
        return random.nextInt(end - start + 1) + start;
    }

    public static BufferedImage getBufferedImage(Integer place) {
        try {
            File file = new File(IMG_PATH);
            if (!file.exists()) {
                System.out.println("验证码图片文件不存在: " + file.getAbsolutePath());
                return null;
            }
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                System.out.println("验证码图片读取失败: " + file.getAbsolutePath());
                return null;
            }
            return img;
        } catch (Exception e) {
            System.out.println("获取拼图资源失败");
            return null;
        }
    }

    public static BufferedImage imageResize(BufferedImage bufferedImage, int width, int height) {
        Image image = bufferedImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resultImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = resultImage.createGraphics();
        graphics2D.drawImage(image, 0, 0, null);
        graphics2D.dispose();
        return resultImage;
    }

    public static void cutByTemplate(BufferedImage canvasImage, BufferedImage blockImage, int blockWidth, int blockHeight, int blockRadius, int blockX, int blockY) {
        BufferedImage waterImage = new BufferedImage(blockWidth, blockHeight, BufferedImage.TYPE_4BYTE_ABGR);
        int[][] blockData = getBlockData(blockWidth, blockHeight, blockRadius);
        for (int i = 0; i < blockWidth; i++) {
            for (int j = 0; j < blockHeight; j++) {
                try {
                    if (blockData[i][j] == 1) {
                        waterImage.setRGB(i, j, Color.BLACK.getRGB());
                        blockImage.setRGB(i, j, canvasImage.getRGB(blockX + i, blockY + j));
                        if (blockData[i + 1][j] == 0 || blockData[i][j + 1] == 0 || blockData[i - 1][j] == 0 || blockData[i][j - 1] == 0) {
                            blockImage.setRGB(i, j, Color.WHITE.getRGB());
                            waterImage.setRGB(i, j, Color.WHITE.getRGB());
                        }
                    } else {
                        blockImage.setRGB(i, j, Color.TRANSLUCENT);
                        waterImage.setRGB(i, j, Color.TRANSLUCENT);
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    // 防止数组下标越界异常
                }
            }
        }
        addBlockWatermark(canvasImage, waterImage, blockX, blockY);
    }

    private static int[][] getBlockData(int blockWidth, int blockHeight, int blockRadius) {
        int[][] data = new int[blockWidth][blockHeight];
        double po = Math.pow(blockRadius, 2);
        int face1 = RandomUtils.nextInt(0, 4);
        int face2;
        do {
            face2 = RandomUtils.nextInt(0, 4);
        } while (face1 == face2);
        int[] circle1 = getCircleCoords(face1, blockWidth, blockHeight, blockRadius);
        int[] circle2 = getCircleCoords(face2, blockWidth, blockHeight, blockRadius);
        int shape = getNonceByRange(0, 1);
        for (int i = 0; i < blockWidth; i++) {
            for (int j = 0; j < blockHeight; j++) {
                data[i][j] = 0;
                if ((i >= blockRadius && i <= blockWidth - blockRadius && j >= blockRadius && j <= blockHeight - blockRadius)) {
                    data[i][j] = 1;
                }
                double d1 = Math.pow(i - Objects.requireNonNull(circle1)[0], 2) + Math.pow(j - circle1[1], 2);
                double d2 = Math.pow(i - Objects.requireNonNull(circle2)[0], 2) + Math.pow(j - circle2[1], 2);
                if (d1 <= po || d2 <= po) {
                    data[i][j] = shape;
                }
            }
        }
        return data;
    }

    private static int[] getCircleCoords(int face, int blockWidth, int blockHeight, int blockRadius) {
        if (0 == face) {
            return new int[]{blockWidth / 2 - 1, blockRadius};
        } else if (1 == face) {
            return new int[]{blockRadius, blockHeight / 2 - 1};
        } else if (2 == face) {
            return new int[]{blockWidth / 2 - 1, blockHeight - blockRadius - 1};
        } else if (3 == face) {
            return new int[]{blockWidth - blockRadius - 1, blockHeight / 2 - 1};
        }
        return null;
    }

    private static void addBlockWatermark(BufferedImage canvasImage, BufferedImage blockImage, int x, int y) {
        Graphics2D graphics2D = canvasImage.createGraphics();
        graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 0.8f));
        graphics2D.drawImage(blockImage, x, y, null);
        graphics2D.dispose();
    }

    public static String toBase64(BufferedImage bufferedImage, String type) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, type, byteArrayOutputStream);
            String base64 = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
            return String.format("data:image/%s;base64,%s", type, base64);
        } catch (IOException e) {
            System.out.println("图片资源转换BASE64失败");
            return null;
        }
    }
}
