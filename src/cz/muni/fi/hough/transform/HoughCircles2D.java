package cz.muni.fi.hough.transform;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

/**
 * Hough transform for detecting circles with known radius in binary image.
 *
 * @author Jakub Medvecký-Heretik
 */
public class HoughCircles2D {

    //Width of input image
    private int width;

    //Height of input image
    private int height;

    //Radius of possible circles in image
    private int radius;

    //Minimum distance between two different radii
    private int distance;

    //Hough array [height][width] for interpreting circles in polar coordinates
    private int[][] houghSpace;

    //How many votes for point in hough array should indicate circle
    private int threshold;

    //Cached sin and cos values
    private double[] sinuses;
    private double[] cosinuses;

    public HoughCircles2D(Mat image, int threshold, int r, int distance) {
        width = image.cols();
        height = image.rows();
        int theta = 180;
        this.radius = r;
        this.distance = distance;
        this.threshold = threshold;
        sinuses = new double[theta];
        cosinuses = new double[theta];

        //Pre-compute r*sin and r*cos values for every theta and known radius
        for (int t = 0; t < theta; t++) {
            sinuses[t] = radius * Math.sin(t);
            cosinuses[t] = radius * Math.cos(t);
        }

        //Initialize hough array
        houghSpace = new int[width][height];

        //Loop through every pixel in input image
        for (int x = 0; x < width - distance; x++) {
            for (int y = 0; y < height - distance; y++) {

                //If pixel is white = edge
                double[] color = image.get(y, x);
                if (color[0] != 0) {

                    /* Parametric form of circle: x = a + r*cos(theta), y = b + r*sin(theta)
                     * Conversion from cartesian to polar coordinates: 
                     * a = x - r*cos(theta), b = y - r*sin(theta)
                     */
                    for (int t = 0; t < theta; t++) {
                        int a = (int) (x - cosinuses[t]);
                        int b = (int) (y - sinuses[t]);

                        //If a and b are inside array bounds
                        if (a > 0 && a <= x && b > 0 && b <= y) {

                            //Increase vote by one
                            houghSpace[a][b] = houghSpace[a][b] + 1;
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

                //If point has more votes than threshold, it strongly indicates circle
                if (houghSpace[x][y] > threshold) {
                    Point center = new Point(x, y);

                    //Draw the circle in green color and thickness of 3px
                    Core.circle(image, center, radius, new Scalar(0, 255, 0), 3);

                    //Draw the center in red color
                    Core.line(image, center, center, new Scalar(255, 0, 0), 3);

                    //Increase coordinates to avoid drawing near local maxima 
                    //and keep minimum distance between two different radii
                    x += distance;
                    y += distance;
                }
            }
        }
    }

}
