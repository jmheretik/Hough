package com.example.hough;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

public class MyHoughLineTransform {
	private int width;
	private int height;
	private int diagonal;
	private int[][] houghSpace;
	private int theta;
	private double thetaStep;
	private int threshold;
	private double[] sinuses; 
    private double[] cosinuses; 
    

	public MyHoughLineTransform(Mat image, int threshold, int theta) {
		width = image.cols();
		height = image.rows();
		this.theta = theta;
		thetaStep = Math.PI/theta;
		this.threshold = threshold;
		sinuses = new double[theta]; 
        cosinuses = new double[theta]; 
        for (int t = 0; t < theta; t++) { 
            double realTheta = t * thetaStep; 
            sinuses[t] = Math.sin(realTheta); 
            cosinuses[t] = Math.cos(realTheta); 
        } 
		
		diagonal = (int) Math.sqrt(width*width+height*height);
		houghSpace = new int[theta][diagonal*2];
		
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				double[] color = image.get(y, x);
    			if (color[0] != 0) { 
					for (int t = 0; t < theta; t++){
						int r = (int)(x * cosinuses[t] + y * sinuses[t]) + diagonal;
						houghSpace[t][r] = houghSpace[t][r] + 1;
					}
				}
			}
		}
	}
	
	public void drawLines(Mat image) {
		for (int t = 0; t < theta; t++) {
			for	(int r = 0; r < diagonal*2; r++) {
				if (houghSpace[t][r] > threshold) {
					double rho = r - diagonal;
					Point start, end;
					
	            	if(((t*thetaStep) < Math.PI/4 || (t*thetaStep) > 3 * Math.PI/4)){
	            		start = new Point(rho / cosinuses[t], 0);
	                    end = new Point( (rho - image.rows() * sinuses[t])/cosinuses[t], image.rows());
	                }else {
	                	start = new Point(0, rho / sinuses[t]);
	                    end = new Point(image.cols(), (rho - image.cols() * cosinuses[t])/sinuses[t]);
	                }
	            	
	                Core.line(image, start, end, new Scalar(255,0,0), 3);
				}
			}
		}
	}
	
}
