package com.example.hough;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

public class MyHoughCircleTransformWithKnownRadius {

    private int width;
    private int height;
    private int radius;
    private int[][] houghSpace;
    private int threshold;
    private double[] sinuses;
    private double[] cosinuses;

    public MyHoughCircleTransformWithKnownRadius(Mat image, int threshold, int theta, int radius) {
        width = image.cols();
        height = image.rows();
        this.radius = radius;
        this.threshold = threshold;
        sinuses = new double[theta];
        cosinuses = new double[theta];

        for (int t = 0; t < theta; t++) {
            sinuses[t] = radius * Math.sin(t);
            cosinuses[t] = radius * Math.cos(t);
        }

        houghSpace = new int[height][width];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                double[] color = image.get(y, x);
                if (color[0] != 0) {
                    for (int t = 0; t < theta; t++) {
                        int a = (int) (y + cosinuses[t]);
                        int b = (int) (x + sinuses[t]);
                        if (a > 0 && a <= y && b > 0 && b <= x) {
                            houghSpace[a][b] = houghSpace[a][b] + 1;
                        }
                    }
                }
            }
        }
    }

    public void drawCircles(Mat image) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (houghSpace[y][x] > threshold) {
                    Point center = new Point(x, y);
                    Core.circle(image, center, radius, new Scalar(255, 0, 0), 3);
                    Core.circle(image, center, 1, new Scalar(0, 255, 0), 3);
                }
            }
        }
    }

}
