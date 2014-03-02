package com.example.hough;

import org.opencv.core.Mat;

public class HoughLine { 
 
    private double theta; 
    private double r; 
 
    public HoughLine(double theta, double r) { 
        this.theta = theta; 
        this.r = r; 
    } 
 
    public void draw(Mat image, int color) { 
 
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
                    image.put(y, x, new double[]{255, 0, 0, 0});
                } 
            } 
        } else { 
            for (int x = 0; x < width; x++) { 
                int y = (int) ((((r - houghHeight) - ((x - centerX) * tcos)) / tsin) + centerY); 
                if (y < height && y >= 0) {  
                    image.put(y, x, new double[]{255, 0, 0, 0});
                } 
            } 
        } 
    } 
} 