package cz.muni.fi.hough;

import cz.muni.fi.hough.R;
import cz.muni.fi.hough.line.HoughLine;
import cz.muni.fi.hough.line.Line;
import cz.muni.fi.hough.pref.PreferencesActivity;
import cz.muni.fi.hough.transform.HoughCircles2D;
import cz.muni.fi.hough.transform.HoughCircles3D;
import cz.muni.fi.hough.transform.HoughLineTransform;
import cz.muni.fi.hough.transform.HoughLines;

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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

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

    //Hough transform for detecting straight lines provided by OpenCV,
    //drawing out only 2 road lanes which meet at estimate horizon
    private static final int VIEW_MODE_OPENCV_LINES_HORIZON = 9;
    
    //Application settings
    private static final int SETTINGS = 10;

    //Current screen orientation
    public static int orientation;

    //Height of image frame
    private int height;

    //Width of image frame
    private int width;

    //Current image processing mode
    private int viewMode;

    //Working matrices
    private Mat matRgba;
    private Mat matGray;
    private Mat matEdges;
    private Mat lines;
    private Mat circles;
    private Bitmap edgeBitmap;

    private CameraBridgeViewBase openCvCameraView;

    //How many votes in hough space should indicate line
    private int lineThreshold;

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
    private int circleTreshold;

    //Left and right detected road lanes
    private Line leftLane;
    private Line rightLane;

    private double midLaneX;
    private double minLaneX;
    private double maxLaneX;

    private double minLeftLaneY;
    private double minRightLaneY;
    private double maxLaneY;

    private boolean isLeftHorizontal;
    private boolean isRightHorizontal;

    /**
     * Load OpenCV Manager
     */
    private BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV library loaded successfully");
                    openCvCameraView.enableView();
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
        super();
        lineThreshold = 70;
        minLineSize = 100;
        maxLineGap = 100;
        minRadius = 40;
        maxRadius = 40;
        distanceRadius = 25;
        circleTreshold = 45;
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

        openCvCameraView = (CameraBridgeViewBase) findViewById(R.id.hough_activity_surface_view);
        openCvCameraView.setCvCameraViewListener(this);

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
        matRgba = new Mat(height, width, CvType.CV_8UC4);
        matGray = new Mat(height, width, CvType.CV_8UC1);
        matEdges = new Mat(height, width, CvType.CV_8UC1);
        edgeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        leftLane = new Line(0, height, width, 0);
        rightLane = new Line(width, height, 0, 0);

        midLaneX = width / 2;
        minLaneX = 0;
        maxLaneX = width;

        minLeftLaneY = 3 * height / 4;
        minRightLaneY = 3 * height / 4;
        maxLaneY = height;

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
            Intent i = new Intent(this, PreferencesActivity.class);
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

        lineThreshold = Integer.parseInt(sharedPrefs.getString("prefLineThresh", "70"));
        minLineSize = Integer.parseInt(sharedPrefs.getString("prefLineMinSize", "100"));
        maxLineGap = Integer.parseInt(sharedPrefs.getString("prefLineMaxGap", "100"));

        minRadius = Integer.parseInt(sharedPrefs.getString("prefMinRadius", "40"));
        maxRadius = Integer.parseInt(sharedPrefs.getString("prefMaxRadius", "40"));
        distanceRadius = Integer.parseInt(sharedPrefs.getString("prefDistanceRadius", "25"));
        circleTreshold = Integer.parseInt(sharedPrefs.getString("prefCircleThresh", "45"));

        viewMode = Integer.parseInt(sharedPrefs.getString("prefMode", "0"));
        final int mode = viewMode;
        switch (mode) {
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
            case VIEW_MODE_OPENCV_LINES_HORIZON:
                Log.i(TAG, "OpenCV lines with horizon");
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
        final int mode = viewMode;
        switch (mode) {

            case VIEW_MODE_RGBA:
                //Get input frame in RBGA
                matRgba = inputFrame.rgba();
                break;

            case VIEW_MODE_GRAY:
                //Get input frame in grayscale
                matGray = inputFrame.gray();

                matRgba = matGray;
                break;

            case VIEW_MODE_SEGMENT:
                matGray = inputFrame.gray();

                segmentation();
                matRgba = matEdges;
                break;

            case VIEW_MODE_OPENCV_LINES:
                matRgba = inputFrame.rgba();
                matGray = inputFrame.gray();

                segmentation();
                openCVLines();
                break;

            case VIEW_MODE_OPENCV_LINES_HORIZON:
                matRgba = inputFrame.rgba();
                matGray = inputFrame.gray();

                segmentation();
                openCVLinesWithHorizon();
                break;

            case VIEW_MODE_OPENCV_LINE_SEGMENTS:
                matRgba = inputFrame.rgba();
                matGray = inputFrame.gray();

                segmentation();
                openCVLineSegments();
                break;

            case VIEW_MODE_JAVA_LINES_OPTIMIZED:
                matRgba = inputFrame.rgba();
                matGray = inputFrame.gray();

                segmentation();
                javaOptimizedLines();
                break;
            case VIEW_MODE_JAVA_LINES_NAIVE:
                matRgba = inputFrame.rgba();
                matGray = inputFrame.gray();

                segmentation();
                javaNaiveLines();
                break;
            case VIEW_MODE_OPENCV_CIRCLES:
                matRgba = inputFrame.rgba();
                matGray = inputFrame.gray();

                openCVCircles();
                break;
            case VIEW_MODE_JAVA_CIRCLES_NAIVE:
                matRgba = inputFrame.rgba();
                matGray = inputFrame.gray();

                javaNaiveCircles();
                break;
        }
        return matRgba;
    }

    /**
     * Lower threshold for portrait orientation and for detecting road lanes with horizon
     *
     * @return lineThreshold
     */
    private int getLineThreshold() {
        int actualThresh = (viewMode == VIEW_MODE_OPENCV_LINES_HORIZON) ? lineThreshold - lineThreshold / 5 : lineThreshold;

        return (orientation == 1) ? actualThresh : actualThresh - actualThresh / 3;
    }

    /**
     * Image segmentation and edge detection
     *
     * @param grey greyscale image
     * @param edges binary image with segmented edges
     */
    private void segmentation() {

        //Bottom half of landscape image
        if (viewMode == VIEW_MODE_OPENCV_LINES_HORIZON || orientation == 1) {
            matGray.submat(height / 2, height, 0, width).copyTo(matEdges.submat(height / 2, height, 0, width));
        } 
        //Bottom third of portrait image
        else {
            matGray.submat(0, height, (2 * width) / 3, width).copyTo(matEdges.submat(0, height, (2 * width) / 3, width));
        }

        /* Gaussian blur
         * 
         Imgproc.GaussianBlur(matEdges, matEdges, new Size(15,15), 0.5);
         */
        
        /* Static threshold
         * 
         //Imgproc.threshold(matEdges, matEdges, 175, 255, Imgproc.THRESH_BINARY);
         //Imgproc.threshold(matEdges, matEdges, 175, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
         */
        
        /* Adaptive threshold                                                
         **/
         Imgproc.adaptiveThreshold(matEdges, matEdges, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 3, -1.5);

         //Delete noise (little white points)
         //Imgproc.dilate(matEdges, matEdges, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));
         Imgproc.erode(matEdges, matEdges, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2)));
         //Imgproc.medianBlur(matEdges, matEdges, 3);
         
        
        /* Canny edge detection
         *
        double mean = Core.mean(matGray).val[0];
        Imgproc.Canny(matEdges, matEdges, 0.66 * mean, 1.33 * mean);

        //Delete white edge at border of segmentation
        if (orientation == 1) {
            matEdges.rowRange(height / 2 - 1, height / 2 + 1).setTo(new Scalar(0));
        } else {
            matEdges.colRange(((2 * width) / 3) - 1, ((2 * width) / 3) + 1).setTo(new Scalar(0));
        }
        */
    }

    /**
     * Draw detected lines to output image from temporary matrix
     *
     * @param tmp
     */
    private void drawTmpToMRgba(Mat tmp) {
        drawBordersToMRgba();
        if (viewMode == VIEW_MODE_OPENCV_LINES_HORIZON || orientation == 1) {
            if (tmp != null) {
                tmp.submat(height / 2, height, 0, width).copyTo(matRgba.submat(height / 2, height, 0, width));
            }
        } else {
            if (tmp != null) {
                tmp.submat(0, height, (2 * width) / 3, width).copyTo(matRgba.submat(0, height, (2 * width) / 3, width));
            }
        }
    }

    /**
     * Draw segmentation borders to output image
     */
    private void drawBordersToMRgba() {
        if (viewMode == VIEW_MODE_OPENCV_LINES_HORIZON || orientation == 1) {
            Core.line(matRgba, new Point(0, height / 2 - 1), new Point(width, height / 2 - 1), new Scalar(0, 255, 0), 1);
        } else {
            Core.line(matRgba, new Point((2 * width) / 3 - 1, 0), new Point((2 * width) / 3 - 1, height), new Scalar(0, 255, 0), 1);
        }
    }

    private void openCVLines() {

        //Matrix of detected lines
        lines = new Mat();

        //Straight line detection
        Imgproc.HoughLines(matEdges, lines, 1, Math.PI / 180, getLineThreshold());

        //Draw straight lines to temporary matrix
        Mat tmp = new Mat(matRgba.rows(), matRgba.cols(), CvType.CV_8UC4);
        matRgba.copyTo(tmp);

        //Get rho and theta values for every line and convert it to cartesian space
        for (int j = 0; j < lines.cols(); j++) {
            double[] vec = lines.get(0, j);
            double rho = vec[0],
                    theta = vec[1];

            Point start, end;

            //Horizontal-ish lines conversion
            if ((theta < Math.PI / 4 || theta > 3 * Math.PI / 4)) {
                start = new Point(rho / Math.cos(theta), 0);
                end = new Point((rho - tmp.rows() * Math.sin(theta)) / Math.cos(theta), tmp.rows());
            } 
            //Vartical-ish lines conversion
            else {
                start = new Point(0, rho / Math.sin(theta));
                end = new Point(tmp.cols(), (rho - tmp.cols() * Math.cos(theta)) / Math.sin(theta));
            }

            //Draw the line
            Core.line(tmp, start, end, new Scalar(255, 0, 0), 3);
        }

        drawTmpToMRgba(tmp);

        //Cleanup
        Log.i(TAG, "lines:" + lines.cols());
        tmp.release();
        tmp = null;
        lines.release();
        lines = null;
    }

    private void openCVLinesWithHorizon() {

        //Matrix of detected lines
        lines = new Mat();

        //Straight line detection
        Imgproc.HoughLines(matEdges, lines, 1, Math.PI / 180, getLineThreshold());

        isLeftHorizontal = true;
        isRightHorizontal = true;

        minLaneX = 0;
        maxLaneX = width;
        minLeftLaneY = 3 * height / 4;
        minRightLaneY = 3 * height / 4;

        //Get rho and theta values for every line and convert it to cartesian space
        for (int j = 0; j < lines.cols(); j++) {
            double[] vec = lines.get(0, j);
            double rho = vec[0],
                    theta = vec[1];

            //Vertical-ish lines conversion
            if ((theta < Math.PI / 4 || theta > 3 * Math.PI / 4)) {

                double topX = rho / Math.cos(theta);
                double bottomX = (rho - height * Math.sin(theta)) / Math.cos(theta);

                if (minLaneX < bottomX && bottomX <= midLaneX) {
                    minLaneX = bottomX;
                    leftLane.setLine(bottomX, topX, height, 0);
                    isLeftHorizontal = false;
                }
                if (midLaneX < bottomX && bottomX < maxLaneX) {
                    maxLaneX = bottomX;
                    rightLane.setLine(bottomX, topX, height, 0);
                    isRightHorizontal = false;
                }
            } 
            //Horizontal-ish lines conversion
            else {

                double leftY = rho / Math.sin(theta);
                double rightY = (rho - width * Math.cos(theta)) / Math.sin(theta);

                if (leftY > minLeftLaneY && leftY < maxLaneY && isLeftHorizontal) {
                    minLeftLaneY = leftY;
                    leftLane.setLine(0, width, leftY, rightY);
                }
                if (rightY > minRightLaneY && rightY < maxLaneY && isRightHorizontal) {
                    minRightLaneY = rightY;
                    rightLane.setLine(width, 0, rightY, leftY);
                }
            }

        }

        //Get the horizon
        Point i = Line.getIntersectionPoint(leftLane, rightLane);

        if (i != null && i.x > 0 && i.x < width && i.y > 0 && i.y < height) {
            //Draw the lines with horizon
            Core.line(matRgba, leftLane.getStart(), i, new Scalar(255, 0, 0), 3);
            Core.line(matRgba, rightLane.getStart(), i, new Scalar(255, 0, 0), 3);
        } else {
            //Draw the lines without horizon
            Core.line(matRgba, leftLane.getStart(), leftLane.getEnd(), new Scalar(255, 0, 0), 3);
            Core.line(matRgba, rightLane.getStart(), rightLane.getEnd(), new Scalar(255, 0, 0), 3);
        }

        //Draw segmentation borders
        drawBordersToMRgba();

        //Cleanup
        Log.i(TAG, "lines:" + lines.cols());
        lines.release();
        lines = null;
    }

    private void openCVLineSegments() {

        //Matrix of detected line segments
        lines = new Mat();

        //Line segments detection
        Imgproc.HoughLinesP(matEdges, lines, 1, Math.PI / 180, getLineThreshold(), minLineSize, maxLineGap);

        //Draw line segments
        for (int i = 0; i < lines.cols(); i++) {
            double[] vec = lines.get(0, i);
            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];

            Point start = new Point(x1, y1);
            Point end = new Point(x2, y2);

            Core.line(matRgba, start, end, new Scalar(255, 0, 0), 3);
        }

        drawTmpToMRgba(null);

        //Cleanup
        Log.i(TAG, "lines:" + lines.cols());
        lines.release();
        lines = null;
    }

    private void javaOptimizedLines() {

        //Initialize Hough transform
        HoughLineTransform houghLineTransform = new HoughLineTransform(width, height);

        Utils.matToBitmap(matEdges, edgeBitmap);

        houghLineTransform.addPoints(edgeBitmap);

        Vector<HoughLine> lines = houghLineTransform.getLines(getLineThreshold());

        Mat tmp = new Mat(height, width, CvType.CV_8UC4);
        matRgba.copyTo(tmp);
        for (int j = 0; j < lines.size(); j++) {
            HoughLine line = lines.elementAt(j);
            line.draw(tmp);
        }

        drawTmpToMRgba(tmp);

        //Cleanup
        Log.i(TAG, "lines:" + lines.size());
        lines.clear();
        lines = null;
        tmp.release();
        tmp = null;
    }

    private void javaNaiveLines() {

        //Straight line detection
        HoughLines houghLines = new HoughLines(matEdges, getLineThreshold());

        //Draw lines
        Mat tmp = new Mat(height, width, CvType.CV_8UC4);
        matRgba.copyTo(tmp);
        houghLines.drawLines(tmp);

        drawTmpToMRgba(tmp);

        //Cleanup
        tmp.release();
        tmp = null;
    }

    private void openCVCircles() {

        //Matrix of detected circles
        circles = new Mat();

        //Gaussian blur						
        Imgproc.GaussianBlur(matGray, matGray, new Size(15, 15), 0.5);

        //Circle detection
        Imgproc.HoughCircles(matGray, circles, Imgproc.CV_HOUGH_GRADIENT, 1, distanceRadius);

        //Draw detected circles
        for (int i = 0; i < circles.cols(); i++) {
            double[] vec = circles.get(0, i);
            double x = vec[0],
                    y = vec[1];
            int radius = (int) vec[2];
            Point center = new Point(x, y);

            //Draw center
            Core.line(matRgba, center, center, new Scalar(255, 0, 0), 3);

            //Draw circle
            Core.circle(matRgba, center, radius, new Scalar(0, 255, 0), 3);
        }

        //Cleanup
        Log.i(TAG, "circles:" + circles.cols());
        circles.release();
        circles = null;
    }

    private void javaNaiveCircles() {

        //Segment image and detect edges
        matGray.copyTo(matEdges);
        Imgproc.adaptiveThreshold(matEdges, matEdges, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 3, -0.5);

        //Circle detection and draw result
        if (minRadius == maxRadius) {
            HoughCircles2D houghCircles2D = new HoughCircles2D(matEdges, circleTreshold, minRadius, distanceRadius);
            houghCircles2D.drawCircles(matRgba);
        } else {
            HoughCircles3D houghCircles3D = new HoughCircles3D(matEdges, circleTreshold, minRadius, maxRadius, distanceRadius);
            houghCircles3D.drawCircles(matRgba);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (openCvCameraView != null) {
            openCvCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, loaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (openCvCameraView != null) {
            openCvCameraView.disableView();
        }
    }

    public void onCameraViewStopped() {
        matRgba.release();
        matGray.release();
        matEdges.release();
        edgeBitmap.recycle();
    }
}
