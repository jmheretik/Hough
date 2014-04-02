package cz.muni.fi.hough.transform;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

/**
 * Hough transform for detecting straight lines in binary image.
 *
 * @author Jakub Medvecký-Heretik
 */
public class HoughLines {

    //Width of input image
    private int width;

    //Height of input image
    private int height;

    //Maximum value of rho that hough array needs to have
    private int diagonal;

    //Hough array [theta][rho] for interpreting straight lines in polar coordinates
    private int[][] houghSpace;

    //Discrete values of theta that we'll check
    private int theta;

    //Get real theta value by multiplying theta by step of theta
    private double thetaStep;

    //How many votes for point in hough array should indicate line
    private int threshold;

    //Cached sin and cos values
    private double[] sinuses;
    private double[] cosinuses;

    public HoughLines(Mat image, int threshold) {
        width = image.cols();
        height = image.rows();
        theta = 180;
        thetaStep = Math.PI / theta;
        this.threshold = threshold;
        sinuses = new double[theta];
        cosinuses = new double[theta];

        //Pre-compute sin and cos values for every theta
        for (int t = 0; t < theta; t++) {
            double realTheta = t * thetaStep;
            sinuses[t] = Math.sin(realTheta);
            cosinuses[t] = Math.cos(realTheta);
        }

        diagonal = (int) Math.sqrt(width * width + height * height);

        //Initialize hough array
        houghSpace = new int[theta][diagonal * 2];

        //Loop through every pixel in bottom half of the input image
        for (int x = 0; x < width; x++) {
            for (int y = height / 2; y < height; y++) {

                //If pixel is white = edge
                double[] color = image.get(y, x);
                if (color[0] != 0) {

                    //Compute rho for every theta and add diagonal in case its negative number
                    for (int t = 0; t < theta; t++) {

                        //Conversion from cartesian to polar coordinates: rho = x*cos(theta) + y*sin(theta)
                        //and add 'diagonal' to store negative values of rho
                        int r = (int) (x * cosinuses[t] + y * sinuses[t]) + diagonal;

                        //Increase vote by one
                        houghSpace[t][r] = houghSpace[t][r] + 1;
                    }
                }
            }
        }
    }

    /**
     * Draws detected lines to the image.
     *
     * @param image
     */
    public void drawLines(Mat image) {

        //Loop through accumulator hough array
        for (int t = 0; t < theta; t++) {
            for (int r = 0; r < diagonal * 2; r++) {

                //If point has more votes than threshold, it strongly indicates line
                if (houghSpace[t][r] > threshold) {

                    //Compute real rho and starting and ending point of detected line
                    double rho = r - diagonal;
                    Point start, end;

                    //Vertical-ish lines conversion
                    if (((t * thetaStep) < Math.PI / 4 || (t * thetaStep) > 3 * Math.PI / 4)) {
                        start = new Point(rho / cosinuses[t], 0);
                        end = new Point((rho - image.rows() * sinuses[t]) / cosinuses[t], image.rows());
                    } //Horizontal-ish lines conversion
                    else {
                        start = new Point(0, rho / sinuses[t]);
                        end = new Point(image.cols(), (rho - image.cols() * cosinuses[t]) / sinuses[t]);
                    }

                    //Draw the line in red color and thickness of 3px
                    Core.line(image, start, end, new Scalar(255, 0, 0), 3);
                }
            }
        }
    }

}
