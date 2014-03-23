package com.example.hough;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

public class MyHoughCircleTransformWithUnknownRadius {

    private int width;
    private int height;
    private int minRadius;
    private int maxRadius;
    private int stepRadius;
    private int[][][] houghSpace;
    private int threshold;
    private double[] sinuses;
    private double[] cosinuses;

    public MyHoughCircleTransformWithUnknownRadius(Mat image, int threshold, int theta, int minRadius, int maxRadius, int stepRadius) {
        width = image.cols();
        height = image.rows();
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.stepRadius = stepRadius;
        this.threshold = threshold;
        sinuses = new double[theta];
        cosinuses = new double[theta];

        for (int t = 0; t < theta; t++) {
            sinuses[t] = Math.sin(t);
            cosinuses[t] = Math.cos(t);
        }

        houghSpace = new int[height][width][maxRadius];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double[] color = image.get(y, x);
                if (color[0] != 0) {
                    for (int r = minRadius; r < maxRadius; r = r + stepRadius) {
                        for (int t = 0; t < theta; t++) {
                            int a = (int) (y + r * cosinuses[t]);
                            int b = (int) (x + r * sinuses[t]);
                            if (a > 0 && a <= y && b > 0 && b <= x) {
                                houghSpace[a][b][r] = houghSpace[a][b][r] + 1;
                            }
                        }
                    }
                }
            }
        }
    }

    public void drawCircles(Mat image) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int r = minRadius; r < maxRadius; r = r + stepRadius) {
                    if (houghSpace[y][x][r] > threshold) {
                        Point center = new Point(x, y);
                        Core.circle(image, center, r, new Scalar(255, 0, 0), 3);
                        Core.circle(image, center, 3, new Scalar(0, 255, 0), 3);
                    }
                }
            }
        }
    }

}
