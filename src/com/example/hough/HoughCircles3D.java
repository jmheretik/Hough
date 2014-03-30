package com.example.hough;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

/**
 * Hough transform for detecting circles with unknown radius in binary image.
 *
 * @author Jakub Medvecký-Heretik
 */
public class HoughCircles3D {

    //Width of input image
    private int width;

    //Height of input image
    private int height;

    //Smallest radius of possible circles to detect
    private int minRadius;

    //Biggest radius of possible circles to detect
    private int maxRadius;

    //Minimum distance between two different radii
    private int distance;

    //Increment radius by step
    private int stepRadius;

    //Hough array [height][width][radius] for interpreting circles in polar coordinates
    private int[][][] houghSpace;

    //How many votes for point in hough array should indicate circle
    private int threshold;

    //Cached sin and cos values
    private double[] sinuses;
    private double[] cosinuses;

    public HoughCircles3D(Mat image, int threshold, int minRadius, int maxRadius, int distance) {
        width = image.cols();
        height = image.rows();
        int theta = 180;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.distance = distance;
        this.stepRadius = 10;
        this.threshold = threshold;
        sinuses = new double[theta];
        cosinuses = new double[theta];

        //Pre-compute sin and cos values for every theta
        for (int t = 0; t < theta; t++) {
            sinuses[t] = Math.sin(t);
            cosinuses[t] = Math.cos(t);
        }

        //Initialize hough array
        houghSpace = new int[width][height][maxRadius];

        //Loop through every pixel in input image
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                //If pixel is white = edge
                double[] color = image.get(y, x);
                if (color[0] != 0) {

                    //Loop through different radii of possible circles
                    for (int r = minRadius; r < maxRadius; r = r + stepRadius) {

                        /* Parametric form of circle: x = a + r*cos(theta), y = b + r*sin(theta)
                         * Conversion from cartesian to polar coordinates: 
                         * a = x - r*cos(theta), b = y - r*sin(theta)
                         */
                        for (int t = 0; t < theta; t++) {
                            int a = (int) (x - r * cosinuses[t]);
                            int b = (int) (y - r * sinuses[t]);

                            //If a and b are inside array bounds
                            if (a > 0 && a <= x && b > 0 && b <= y) {

                                //Increase vote by one
                                houghSpace[a][b][r] = houghSpace[a][b][r] + 1;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Draws detected circles to the image.
     *
     * @param image
     */
    public void drawCircles(Mat image) {

        //Loop through accumulator hough array
        for (int x = 0; x < width - distance; x++) {
            for (int y = 0; y < height - distance; y++) {
                for (int r = minRadius; r < maxRadius; r = r + stepRadius) {

                    //If point has more votes than threshold, it strongly indicates circle
                    if (houghSpace[x][y][r] > threshold) {
                        Point center = new Point(x, y);

                        //Draw the circle in red color and thickness of 3px
                        Core.circle(image, center, r, new Scalar(255, 0, 0), 3);

                        //Draw the center in green color
                        Core.line(image, center, center, new Scalar(0, 255, 0), 3);

                        //Increase coordinates to avoid drawing near local maxima 
                        //and keep minimum distance between two different radii
                        x += distance;
                        y += distance;
                    }
                }
            }
        }
    }

}
