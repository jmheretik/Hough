package com.example.hough;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;
import java.util.Vector;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.example.hough.R;
import com.example.hough.HoughLine;
import com.example.hough.HoughLineTransform;
import com.example.hough.HoughLines;

public class HoughActivity extends Activity implements CvCameraViewListener2 {

    private static final String TAG = "Hough Activity";

    //RGB image processing
    private static final int VIEW_MODE_RGBA = 0;

    //Greyscale image processing
    private static final int VIEW_MODE_GRAY = 1;

    //Image segmentation and edge detection provided by OpenCV
    private static final int VIEW_MODE_SEGMENT = 2;

    //Hough transform for detecting straight lines provided by OpenCV
    private static final int VIEW_MODE_OPENCV_LINES = 3;

    //Hough transform for detecting line segments provided by OpenCV
    private static final int VIEW_MODE_OPENCV_LINE_SEGMENTS = 4;

    //Hough transform for detecting straight lines provided by University of Essex's sample Java implementation
    private static final int VIEW_MODE_JAVA_LINES_OPTIMIZED = 5;

    //Naive Java implemetation of Hough transform for detecting straight lines
    private static final int VIEW_MODE_JAVA_LINES_NAIVE = 6;

    //Hough transform for detecting circles provided by OpenCV
    private static final int VIEW_MODE_OPENCV_CIRCLES = 7;

    //Naive Java implemetation of Hough transform for detecting circles
    private static final int VIEW_MODE_JAVA_CIRCLES_NAIVE = 8;

    //Application settings
    private static final int SETTINGS = 9;

    //Screen orientation
    public static int orientation;

    private int mViewMode;
    private Mat mRgba;
    private Mat mGray;
    private Mat mEdges;
    private Mat lines;
    private Mat circles;
    private Bitmap edgeBitmap;
    //private Bitmap mRgbaBitmap;
    private int height;
    private int width;

    private CameraBridgeViewBase mOpenCvCameraView;

    //How many votes in hough space should indicate line
    private int lineThresh;

    //Minimum line segment length
    private int minLineSize;

    //Maximum length of gap between line segments
    private int maxLineGap;

    //Smallest and biggest radius of circles to detect 
    //if equal - 2D hough space will be used which leads to better performance, otherwise 3D
    private int minRadius, maxRadius;

    //Minimum distance between radiuses of circles
    private int distanceRadius;

    //How many votes in hough space should indicate circle
    private int circleTresh;

    private Point leftStart;
    private Point leftEnd;
    private Point rightStart;
    private Point rightEnd;
    
    private double midX;
    private double minX;
    private double maxX;
    
    private double minLeftY;
    private double minRightY;
    private double maxY;
    
    private boolean isLeftHorizontal;
    private boolean isRightHorizontal;
    
    /**
     * Load OpenCV Manager
     */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV library loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    /**
     * Initialize default parameters
     */
    public HoughActivity() {
        lineThresh = 80;
        minLineSize = 100;
        maxLineGap = 100;
        minRadius = 40;
        maxRadius = 40;
        distanceRadius = 25;
        circleTresh = 45;
        orientation = 1;
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.hough_surface_view);

        showUserSettings();

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.hough_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        //Register accelerometer sensor handler for determining current screen orientation
        SensorManager sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(new SensorEventListener() {
            int orientation = -1;

            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.values[1] < 6.5 && event.values[1] > -6.5) {
                    if (orientation != 1) {
                        HoughActivity.orientation = 1;
                    }
                    orientation = 1;
                } else {
                    if (orientation != 0) {
                        HoughActivity.orientation = 0;
                    }
                    orientation = 0;
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

        }, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
    }

    /**
     * Initialise working matrices with same dimensions as image from camera
     *
     * @param width
     * @param height
     */
    public void onCameraViewStarted(int width, int height) {
    	this.width = width;
    	this.height = height;
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        mEdges = new Mat(height, width, CvType.CV_8UC1);
        edgeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        //mRgbaBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        
        leftStart = new Point(0, height);
        leftEnd = new Point(width, 0);
        rightStart = new Point(width, height);
        rightEnd = new Point(0, 0);
        
        midX = width/2;
        minX = 0;
        maxX = width;
        
        minLeftY = 3*height/4;
        minRightY = 3*height/4;
        maxY = height;
        
        isLeftHorizontal = true;
        isRightHorizontal = true;
    }

    /**
     * If 'menu' key pressed - open application settings
     *
     * @param keyCode
     * @param event
     */
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            Intent i = new Intent(this, UserSettingActivity.class);
            startActivityForResult(i, SETTINGS);
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case SETTINGS:
                showUserSettings();
                break;
        }
    }

    /**
     * Update parameters set in application settings
     */
    public void showUserSettings() {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        lineThresh = Integer.parseInt(sharedPrefs.getString("prefLineThresh", "80"));
        minLineSize = Integer.parseInt(sharedPrefs.getString("prefLineMinSize", "100"));
        maxLineGap = Integer.parseInt(sharedPrefs.getString("prefLineMaxGap", "100"));

        minRadius = Integer.parseInt(sharedPrefs.getString("prefMinRadius", "40"));
        maxRadius = Integer.parseInt(sharedPrefs.getString("prefMaxRadius", "40"));
        distanceRadius = Integer.parseInt(sharedPrefs.getString("prefDistanceRadius", "25"));
        circleTresh = Integer.parseInt(sharedPrefs.getString("prefCircleThresh", "45"));

        mViewMode = Integer.parseInt(sharedPrefs.getString("prefMode", "0"));
        final int viewMode = mViewMode;
        switch (viewMode) {
            case VIEW_MODE_RGBA:
                Log.i(TAG, "RGBA");
                break;
            case VIEW_MODE_GRAY:
                Log.i(TAG, "GRAY");
                break;
            case VIEW_MODE_SEGMENT:
                Log.i(TAG, "Segmentation");
                break;
            case VIEW_MODE_OPENCV_LINES:
                Log.i(TAG, "OpenCV lines");
                break;
            case VIEW_MODE_OPENCV_LINE_SEGMENTS:
                Log.i(TAG, "OpenCV line segments");
                break;
            case VIEW_MODE_JAVA_LINES_OPTIMIZED:
                Log.i(TAG, "Java lines - optimized");
                break;
            case VIEW_MODE_JAVA_LINES_NAIVE:
                Log.i(TAG, "Java lines - naive");
                break;
            case VIEW_MODE_OPENCV_CIRCLES:
                Log.i(TAG, "OpenCV circles");
                break;
            case VIEW_MODE_JAVA_CIRCLES_NAIVE:
                Log.i(TAG, "Java circles - naive");
                break;
        }
    }

    /**
     * Process image frames from camera in real-time and draw them back
     *
     * @param inputFrame
     */
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        final int viewMode = mViewMode;
        //mEdges = new Mat(height, width, CvType.CV_8UC1);
        switch (viewMode) {
        
            case VIEW_MODE_RGBA:
                //Get input frame in RBGA                       //15 fps
                mRgba = inputFrame.rgba();
                break;

            case VIEW_MODE_GRAY:
                //Get input frame in grayscale                  //30 fps
                mGray = inputFrame.gray();

                mRgba = mGray;
                break;

            case VIEW_MODE_SEGMENT:
                mGray = inputFrame.gray();

                segmentation();
                mRgba = mEdges;
                break;

            case VIEW_MODE_OPENCV_LINES:
                mRgba = inputFrame.rgba();
                mGray = inputFrame.gray();

                segmentation();
                openCVLines();
                break;

            case VIEW_MODE_OPENCV_LINE_SEGMENTS:
                mRgba = inputFrame.rgba();
                mGray = inputFrame.gray();

                segmentation();
                openCVLineSegments();
                break;

            case VIEW_MODE_JAVA_LINES_OPTIMIZED:
                mRgba = inputFrame.rgba();
                mGray = inputFrame.gray();

                segmentation();
                javaOptimizedLines();
                break;
            case VIEW_MODE_JAVA_LINES_NAIVE:
                mRgba = inputFrame.rgba();
                mGray = inputFrame.gray();

                segmentation();
                javaNaiveLines();
                break;
            case VIEW_MODE_OPENCV_CIRCLES:
                mRgba = inputFrame.rgba();
                mGray = inputFrame.gray();

                openCVCircles();
                break;
            case VIEW_MODE_JAVA_CIRCLES_NAIVE:
                mRgba = inputFrame.rgba();
                mGray = inputFrame.gray();

                javaNaiveCircles();
                break;
        }
        
        mEdges = Mat.zeros(height, width, CvType.CV_8UC1);
        return mRgba;
    }

    /**
	 * Lower threshold for portrait orientation 
	 * 
	 * @return lineThresh
	 */
	private int getLineThreshold() {
	    return (orientation == 1) ? lineThresh : lineThresh - lineThresh / 3;
	}

	/**
     * Image segmentation and edge detection
     *
     * @param grey greyscale image
     * @param edges binary image with segmented edges
     */
    private void segmentation() {

        //Bottom half of landscape image                                 //15fps
        if (orientation == 1) {
            mGray.submat(mGray.rows() / 2, mGray.rows(), 0, mGray.cols()).copyTo(mEdges.submat(mEdges.rows() / 2, mEdges.rows(), 0, mEdges.cols()));
        } 
        //Bottom third of portrait image
        else {
            mGray.submat(0, mGray.rows(), (2*mGray.cols())/3, mGray.cols()).copyTo(mEdges.submat(0, mEdges.rows(), (2*mEdges.cols())/3, mEdges.cols()));
        }

	//Gaussian blur                                                     //7 fps
        //Imgproc.GaussianBlur(mEdges, mEdges, new Size(15,15), 0.5);
        
    	//Static threshold
        //Imgproc.threshold(mEdges, mEdges, 100, 255, Imgproc.THRESH_BINARY);
        
        //Adaptive threshold                                                //10-12 fps
        //Imgproc.adaptiveThreshold(mEdges, mEdges, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 3, -0.5);

        //Delete noise (little white points)                                //9 fps
        //Imgproc.erode(mEdges, mEdges, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));

        //Delete white edge at border of segmentation                       //10-11 fps
        mEdges.row(mEdges.rows()/2).setTo(new Scalar(0));
        
	    //Canny edge detection                                         //6 fps
        double mean = Core.mean(mGray).val[0];
        Imgproc.Canny(mEdges, mEdges, 0.66*mean, 1.33*mean);
    }

    /**
     * Draw lines to output image from temporary matrix
     * 
     * @param tmp
     */
    private void drawToMRgba(Mat tmp) {
    	if (orientation == 1) {
        	Core.line(mRgba, new Point(0, mRgba.rows()/2 - 1), new Point(mRgba.cols(), mRgba.rows()/2 - 1), new Scalar(0, 255, 0), 1);
            if (tmp != null) tmp.submat(tmp.rows()/2, tmp.rows(), 0, tmp.cols()).copyTo(mRgba.submat(mRgba.rows()/2, mRgba.rows(), 0, mRgba.cols()));
        } else {
        	Core.line(mRgba, new Point((2*mRgba.cols())/3 - 1,  0), new Point((2*mRgba.cols())/3 - 1,  mRgba.rows()), new Scalar(0, 255, 0), 1);
        	if (tmp != null) tmp.submat(0, tmp.rows(), (2*tmp.cols())/3, tmp.cols()).copyTo(mRgba.submat(0, mRgba.rows(), (2*mRgba.cols())/3, mRgba.cols()));
        }
	}

	private void openCVLines() {

        //Matrix of detected lines
        lines = new Mat();

        //Straight line detection                                           //3-2 fps
        Imgproc.HoughLines(mEdges, lines, 1, Math.PI / 180, getLineThreshold());

        //Draw straight lines to temporary matrix
        Mat tmp = new Mat(mRgba.rows(), mRgba.cols(), CvType.CV_8UC4);
        mRgba.copyTo(tmp);
        
        boolean isLeftHorizontal = true;
        boolean isRightHorizontal = true;
        
        //Get rho and theta values for every line and convert it to cartesian space
        for (int j = 0; j < lines.cols(); j++) {
            double[] vec = lines.get(0, j);
            double rho = vec[0],
                    theta = vec[1];

            //Vertical-ish lines conversion
            if ((theta < Math.PI / 4 || theta > 3 * Math.PI / 4)) {
            	
            	double topX = rho / Math.cos(theta);
            	double bottomX = (rho - tmp.rows() * Math.sin(theta)) / Math.cos(theta);
            	
            	if (minX < bottomX && bottomX <= midX){
            		minX = bottomX;
            		leftStart.x = topX;
            		leftStart.y = 0;
            		leftEnd.x = bottomX;
            		leftEnd.y = tmp.rows();
            		isLeftHorizontal = false;
            	}
            	if (midX < bottomX && bottomX < maxX){
            		maxX = bottomX;
            		rightStart.x = topX;
            		rightStart.y = 0;
            		rightEnd.x = bottomX;
            		rightEnd.y = tmp.rows();
            		isRightHorizontal = false;
            	}
            } 
            //Horizontal-ish lines conversion
            else {
            	
            	double leftY = rho / Math.sin(theta);
            	double rightY = (rho - tmp.cols() * Math.cos(theta)) / Math.sin(theta);
            	
            	if (leftY > minLeftY && leftY < maxY  && isLeftHorizontal){
            		minLeftY = leftY;
            		leftStart.x = 0;
            		leftStart.y = leftY;
            		leftEnd.x = tmp.cols();
            		leftEnd.y = rightY;
            	}
            	if (rightY > minRightY && rightY < maxY && isRightHorizontal){
            		minRightY = rightY;
            		rightStart.x = tmp.cols();
            		rightStart.y = rightY;
            		rightEnd.x = 0;
            		rightEnd.y = leftY;
            	}
            }
            
        }
        
        //Draw the line
        Core.line(tmp, leftStart, leftEnd, new Scalar(255, 0, 0), 3);
        Core.line(tmp, rightStart, rightEnd, new Scalar(255, 0, 0), 3);
        
        drawToMRgba(tmp);

        //Cleanup
        Log.i(TAG, "lines:" + lines.cols());
        tmp.release();
        tmp = null;
        lines.release();
        lines = null;
    }

    private void openCVLineSegments() {

        //Matrix of detected line segments
        lines = new Mat();

        //Line segments detection					// 4-2 fps
        Imgproc.HoughLinesP(mEdges, lines, 1, Math.PI / 180, getLineThreshold(), minLineSize, maxLineGap);

        //Draw line segments
        for (int i = 0; i < lines.cols(); i++) {
            double[] vec = lines.get(0, i);
            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];

            Point start = new Point(x1, y1);
            Point end = new Point(x2, y2);

            Core.line(mRgba, start, end, new Scalar(255, 0, 0), 3);
        }
        
        drawToMRgba(null);

        //Cleanup
        Log.i(TAG, "lines:" + lines.cols());
        lines.release();
        lines = null;
    }

    private void javaOptimizedLines() {

        //Initialize Hough transform
        HoughLineTransform houghLineTransform = new HoughLineTransform(mEdges.cols(), mEdges.rows());

        /* Draw result to bitmap (2 additional conversions between Mat and Bitmap)
         * 0.5 fps
         *
         Utils.matToBitmap(mRgba, mRgbaBitmap);
         Utils.matToBitmap(mEdges, edgeBitmap);
	    
         houghLineTransform.addPoints(mEdges);
	    
         Vector<HoughLine> lines = houghLineTransform.getLines(getLineThreshold());
	
         for (int j = 0; j < lines.size(); j++) {
         HoughLine line = lines.elementAt(j);
         line.draw(mRgbaBitmap, Color.RED); 
         }
	    
         Utils.bitmapToMat(mRgbaBitmap, mRgba);
         */
        
        // 6 fps
        Utils.matToBitmap(mEdges, edgeBitmap);

        // 2-1 fps
        houghLineTransform.addPoints(edgeBitmap);

        Vector<HoughLine> lines = houghLineTransform.getLines(getLineThreshold());

        Mat tmp = new Mat(mRgba.rows(), mRgba.cols(), CvType.CV_8UC4);
        mRgba.copyTo(tmp);
        for (int j = 0; j < lines.size(); j++) {
            HoughLine line = lines.elementAt(j);
            line.draw(tmp);
        }

        drawToMRgba(tmp);

        //Cleanup
        Log.i(TAG, "lines:" + lines.size());
        lines.clear();
        lines = null;
        tmp.release();
        tmp = null;
    }

    private void javaNaiveLines() {

        //Straight line detection
        HoughLines houghLines = new HoughLines(mEdges, getLineThreshold());

        //Draw lines
        Mat tmp = new Mat(mRgba.rows(), mRgba.cols(), CvType.CV_8UC4);
        mRgba.copyTo(tmp);
        houghLines.drawLines(tmp);
        
        drawToMRgba(tmp);

        //cleanup
        tmp.release();
        tmp = null;
    }

    private void openCVCircles() {

        //Matrix of detected circles
        circles = new Mat();

        //Gaussian blur						
        Imgproc.GaussianBlur(mGray, mGray, new Size(15, 15), 0.5);

        //Circle detection
        Imgproc.HoughCircles(mGray, circles, Imgproc.CV_HOUGH_GRADIENT, 1, distanceRadius);

        //Draw detected circles
        for (int i = 0; i < circles.cols(); i++) {
            double[] vec = circles.get(0, i);
            double x = vec[0],
                    y = vec[1];
            int radius = (int) vec[2];
            Point center = new Point(x, y);

            //Draw center
            Core.line(mRgba, center, center, new Scalar(0, 255, 0), 3);

            //Draw circle
            Core.circle(mRgba, center, radius, new Scalar(255, 0, 0), 3);
        }

        //Cleanup
        Log.i(TAG, "circles:" + circles.cols());
        circles.release();
        circles = null;
    }

    private void javaNaiveCircles() {

        //Segment image and detect edges
        mGray.copyTo(mEdges);
        Imgproc.adaptiveThreshold(mEdges, mEdges, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 3, -0.5);

        //Circle detection and draw result
        if (minRadius == maxRadius) {
            HoughCircles2D houghCircles2D = new HoughCircles2D(mEdges, circleTresh, minRadius, distanceRadius);
            houghCircles2D.drawCircles(mRgba);

            //Detect cirlces in image on SD card
            //javaNaiveCirclesStatic();
        } else {
            HoughCircles3D houghCircles3D = new HoughCircles3D(mEdges, circleTresh, minRadius, maxRadius, distanceRadius);
            houghCircles3D.drawCircles(mRgba);
        }
    }

    private void javaNaiveCirclesStatic() {

        //Process static image from sd card.
        Bitmap copy = getImageFromSDCard("image.jpg");
        Utils.bitmapToMat(copy, mEdges);

        HoughCircles2D houghCircles2D = new HoughCircles2D(mEdges, circleTresh, minRadius, distanceRadius);
        houghCircles2D.drawCircles(mEdges);

        Utils.matToBitmap(mEdges, copy);
        saveImageToSDCard(copy);

        this.finish();
    }

    /**
     * Get image from file on SD card
     *
     * @param name
     * @return copy of bitmap image (mutable)
     */
    private Bitmap getImageFromSDCard(String name) {
        File file = new File(Environment.getExternalStorageDirectory().toString() + "/" + name);
        Bitmap image = BitmapFactory.decodeFile(file.getAbsolutePath());
        Bitmap copy = image.copy(image.getConfig(), true);
        return copy;
    }

    /**
     * Save bitmap to file on SD card
     *
     * @param copy
     */
    private void saveImageToSDCard(Bitmap copy) {
        String root = Environment.getExternalStorageDirectory().toString();
        Random generator = new Random();
        File myDir = new File(root + "/tmp");

        myDir.mkdirs();
        int n = 10000;
        n = generator.nextInt(n);

        String fname = "Image-" + n + ".jpg";
        File save = new File(myDir, fname);

        if (save.exists()) {
            save.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(save);
            copy.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.i("file", fname + " saved to: " + myDir);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        mEdges.release();
        edgeBitmap.recycle();
    }
}
