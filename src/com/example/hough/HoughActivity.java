package com.example.hough;

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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.hough.R;
import com.example.hough.HoughLine;
import com.example.hough.HoughTransform;

public class HoughActivity extends Activity implements CvCameraViewListener2 {
    private static final String    TAG = "Hough Activity";

    private static final int       VIEW_MODE_RGBA		= 0;
    private static final int       VIEW_MODE_GRAY		= 1;
    private static final int       VIEW_MODE_ERODE		= 2;
    private static final int       VIEW_MODE_HOUGH		= 3;
    private static final int       VIEW_MODE_JAVA		= 4;

    private int                    mViewMode;
    private Mat                    mRgba;
    private Mat                    mGray;
    private Mat					   lines;
    private Mat 				   thresholdImage;

    private MenuItem               mItemPreviewRGBA;
    private MenuItem               mItemPreviewGray;
    private MenuItem               mItemPreviewErode;
    private MenuItem               mItemPreviewHough;
    private MenuItem               mItemPreviewJava;
    private MenuItem			   mItemThreshold;
    private MenuItem			   mItemLineSize;
    private MenuItem			   mItemLineGap;

    private CameraBridgeViewBase   mOpenCvCameraView;
    
    private int threshold = 20;
    private int minLineSize = 10;
    private int maxLineGap = 200;
    //private double mean;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public HoughActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.hough_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.hough_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemPreviewRGBA = menu.add("Preview RGBA");
        mItemPreviewGray = menu.add("Preview GRAY");
        mItemPreviewErode = menu.add("Erode");
        mItemPreviewHough = menu.add("Hough Lines");
        mItemPreviewJava = menu.add("Java");
        mItemThreshold = menu.add("Hough Threshold");
        mItemLineSize = menu.add("Hough LineSize");
        mItemLineGap = menu.add("Hough LineGap");
        return true;
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        final int viewMode = mViewMode;
        switch (viewMode) {
        case VIEW_MODE_GRAY:
            // input frame has gray scale format	//30 fps
            Imgproc.cvtColor(inputFrame.gray(), mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
            break;
        case VIEW_MODE_RGBA:
            // input frame has RBGA format			//15 fps
            mRgba = inputFrame.rgba();
            break;
        case VIEW_MODE_ERODE:
        	mRgba = inputFrame.rgba();
        	mGray = inputFrame.gray();
        	thresholdImage = new Mat(mGray.rows(), mGray.cols(), CvType.CV_8U);
        	mGray.submat(mGray.rows()/2, mGray.rows(), 0, mGray.cols()).copyTo(thresholdImage.submat(thresholdImage.rows()/2, thresholdImage.rows(), 0, thresholdImage.cols()));
        	Imgproc.adaptiveThreshold(thresholdImage, thresholdImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 3, -1);
        	Imgproc.erode(thresholdImage, mRgba, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,2)));
        	thresholdImage.release();
            thresholdImage = null;
            break;
        case VIEW_MODE_HOUGH:   
        	//input frame has gray scale format		//30 fps
        	lines = new Mat();        	
        	mRgba = inputFrame.rgba();				//15 fps
        	mGray = inputFrame.gray(); 				//30 fps
        	thresholdImage = new Mat(mGray.rows(), mGray.cols(), CvType.CV_8U);
        	//Imgproc.cvtColor(mGray, mRgba, Imgproc.COLOR_RGBA2GRAY, 4);
        	
        	//dolna polovica obrazovky   			//15fps
        	mGray.submat(mGray.rows()/2, mGray.rows(), 0, mGray.cols()).copyTo(thresholdImage.submat(thresholdImage.rows()/2, thresholdImage.rows(), 0, thresholdImage.cols()));
        	
        	//rozmazanie							//7 fps
        	//Imgproc.GaussianBlur(thresholdImage, thresholdImage, new Size(15,15), 0.5);
        	
        	//odsegmentovanie						//10 fps
        	Imgproc.adaptiveThreshold(thresholdImage, thresholdImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 3, -1);
        	//Imgproc.threshold(thresholdImage, mRgba, 100, 255, Imgproc.THRESH_BINARY);
        	
        	//vymazanie malych bodov 				//10-9 fps
        	Imgproc.erode(thresholdImage, thresholdImage, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,2)));
        	
        	
        	//edge detection						//6 fps
        	/*
        	mean = Core.mean(mGray).val[0];
        	Imgproc.Canny(thresholdImage, mRgba, 0.66*mean, 1.33*mean);
        	*/
        	
         
        	//line detection						// 5-3 fps
            Imgproc.HoughLinesP(thresholdImage, lines, 1, Math.PI/180, threshold, minLineSize, maxLineGap);
         
            //line draw
            for (int i = 0; i < lines.cols(); i++)
            {
                  double[] vec = lines.get(0, i);
                  double x1 = vec[0],
                         y1 = vec[1],
                         x2 = vec[2],
                         y2 = vec[3];
                  Point start = new Point(x1, y1);
                  Point end = new Point(x2, y2);
         
                  Core.line(mRgba, start, end, new Scalar(255,0,0), 3);
            }
            
            //cleanup
            Log.i(TAG, "OpenCV lines:" + lines.cols());
            lines.release();
            lines = null;
            thresholdImage.release();
            thresholdImage = null;
            break;
    	case VIEW_MODE_JAVA:   
    		//input frame has gray scale format     	
        	mRgba = inputFrame.rgba();			
        	mGray = inputFrame.gray(); 			
        	thresholdImage = new Mat(mGray.rows(), mGray.cols(), CvType.CV_8U);
        	mGray.submat(mGray.rows()/2, mGray.rows(), 0, mGray.cols()).copyTo(thresholdImage.submat(thresholdImage.rows()/2, thresholdImage.rows(), 0, thresholdImage.cols()));
        	Imgproc.adaptiveThreshold(thresholdImage, thresholdImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 3, -1);
        	Imgproc.erode(thresholdImage, thresholdImage, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,2)));

        	/*     6 fps */
        	Bitmap threshBitmap = Bitmap.createBitmap(thresholdImage.cols(), thresholdImage.rows(), Bitmap.Config.ARGB_8888);
        	Utils.matToBitmap(thresholdImage, threshBitmap);
        	//Bitmap mRgbaBitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
        	//Utils.matToBitmap(mRgba, mRgbaBitmap);
        	
            HoughTransform h = new HoughTransform(thresholdImage.cols(), thresholdImage.rows()); 
            
            //0.5 fps
            //h.addPoints(thresholdImage); 
            
            //1-2fps
            h.addPoints(threshBitmap);
     
            Vector<HoughLine> lines = h.getLines(25); 
     
            for (int j = 0; j < lines.size(); j++) { 
                HoughLine line = lines.elementAt(j); 
                line.draw(mRgba, Color.RED); 
            }    
            //Utils.bitmapToMat(mRgbaBitmap, mRgba);
            
            //cleanup
            Log.i(TAG, "Java lines:" + lines.size());
            /*
            mRgbaBitmap.recycle();
            mRgbaBitmap = null;
            */
            threshBitmap.recycle();
            threshBitmap = null;
            thresholdImage.release();
            thresholdImage = null;
            break;
        }

        return mRgba;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mItemPreviewRGBA) {
            mViewMode = VIEW_MODE_RGBA;
        } else if (item == mItemPreviewGray) {
            mViewMode = VIEW_MODE_GRAY;
        } else if (item == mItemPreviewErode) {
            mViewMode = VIEW_MODE_ERODE;
        } else if (item == mItemPreviewHough) {
            mViewMode = VIEW_MODE_HOUGH;
        }else if (item == mItemPreviewJava) {
            mViewMode = VIEW_MODE_JAVA;
        } else if (item == mItemThreshold) {
            if (threshold < 200){
            	threshold += 20;
            } else {
            	threshold = 20;
            }
            Toast.makeText(HoughActivity.this,"threshold: "+threshold,
                    Toast.LENGTH_SHORT).show();
        } 
        else if (item == mItemLineSize) {
            if (minLineSize < 200){
            	minLineSize += 20;
            } else {
            	minLineSize = 20;
            }
            Toast.makeText(HoughActivity.this,"minLineSize: "+minLineSize,
                    Toast.LENGTH_SHORT).show();
        } 
        else if (item == mItemLineGap) {
            if (maxLineGap < 200){
            	maxLineGap += 20;
            } else {
            	maxLineGap = 20;
            }
            Toast.makeText(HoughActivity.this,"lineGap: "+maxLineGap,  
                    Toast.LENGTH_SHORT).show();
        } 

        return true;
    }
}
