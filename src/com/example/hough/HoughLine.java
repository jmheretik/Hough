package com.example.hough;

import org.opencv.core.Mat;

import android.graphics.Bitmap;

/**
 * Represents a linear line as detected by the hough transform. This line is
 * represented by an angle theta and a radius from the centre.
 *
 * @author Olly Oechsle, University of Essex, Date: 13-Mar-2008, Jakub
 * Medvecký-Heretik
 * @version 1.0
 */
public class HoughLine {

    private double theta;
    private double r;

    /**
     * Initialises the hough line
     */
    public HoughLine(double theta, double r) {
        this.theta = theta;
        this.r = r;
    }

    /**
     * Draws the line on the image of your choice with the RGB colour of your
     * choice.
     */
    public void draw(Bitmap image, int color) {

        int height = image.getHeight();
        int width = image.getWidth();

        // During processing h_h is doubled so that -ve r values 
        int houghHeight = (int) (Math.sqrt(2) * Math.max(height, width)) / 2;

        // Find edge points and vote in array 
        float centerX = width / 2;
        float centerY = height / 2;

        // Draw edges in output array 
        double tsin = Math.sin(theta);
        double tcos = Math.cos(theta);

        if (theta < Math.PI * 0.25 || theta > Math.PI * 0.75) {
            // Draw vertical-ish lines 
            for (int y = 1; y < height - 1; y++) {
                int x = (int) ((((r - houghHeight) - ((y - centerY) * tsin)) / tcos) + centerX);
                if (x < width - 1 && x >= 1) {
                    image.setPixel(x + 1, y, color);
                    image.setPixel(x - 1, y, color);
                    image.setPixel(x, y, color);
                    image.setPixel(x, y - 1, color);
                    image.setPixel(x, y + 1, color);
                }
            }
        } else {
            // Draw horizontal-ish lines 
            for (int x = 1; x < width - 1; x++) {
                int y = (int) ((((r - houghHeight) - ((x - centerX) * tcos)) / tsin) + centerY);
                if (y < height - 1 && y >= 1) {
                    image.setPixel(x + 1, y, color);
                    image.setPixel(x - 1, y, color);
                    image.setPixel(x, y, color);
                    image.setPixel(x, y - 1, color);
                    image.setPixel(x, y + 1, color);
                }
            }
        }
    }

    /**
     * Draws red line on the image (in OpenCV's org.opencv.core.Mat format).
     * Thickness of line is 3 px.
     *
     * @author Jakub Medvecký-Heretik
     */
    public void draw(Mat image) {

        int height = image.rows();
        int width = image.cols();

        int houghHeight = (int) (Math.sqrt(2) * Math.max(height, width)) / 2;

        float centerX = width / 2;
        float centerY = height / 2;

        double tsin = Math.sin(theta);
        double tcos = Math.cos(theta);

        if (theta < Math.PI * 0.25 || theta > Math.PI * 0.75) {
            for (int y = 0; y < height; y++) {
                int x = (int) ((((r - houghHeight) - ((y - centerY) * tsin)) / tcos) + centerX);
                if (x < width && x >= 0) {
                    image.put(y + 1, x, new double[]{255, 0, 0, 0});
                    image.put(y - 1, x, new double[]{255, 0, 0, 0});
                    image.put(y, x, new double[]{255, 0, 0, 0});
                    image.put(y, x - 1, new double[]{255, 0, 0, 0});
                    image.put(y, x + 1, new double[]{255, 0, 0, 0});
                }
            }
        } else {
            for (int x = 0; x < width; x++) {
                int y = (int) ((((r - houghHeight) - ((x - centerX) * tcos)) / tsin) + centerY);
                if (y < height && y >= 0) {
                    image.put(y + 1, x, new double[]{255, 0, 0, 0});
                    image.put(y - 1, x, new double[]{255, 0, 0, 0});
                    image.put(y, x, new double[]{255, 0, 0, 0});
                    image.put(y, x - 1, new double[]{255, 0, 0, 0});
                    image.put(y, x + 1, new double[]{255, 0, 0, 0});
                }
            }
        }
    }
}
