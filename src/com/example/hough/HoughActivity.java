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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.hough.R;
import com.example.hough.HoughLine;
import com.example.hough.HoughLineTransform;
import com.example.hough.MyHoughLineTransform;

public class HoughActivity extends Activity implements CvCameraViewListener2 {
    private static final String    TAG = "Hough Activity";

    private static final int       VIEW_MODE_RGBA			= 0;
    private static final int       VIEW_MODE_GRAY			= 1;
    private static final int       VIEW_MODE_SEGMENT		= 2;
    private static final int       VIEW_MODE_OPENCV_LINES	= 3;
    private static final int       VIEW_MODE_JAVA_LINES		= 4;
    private static final int       VIEW_MODE_MY_JAVA_LINES	= 5;
    private static final int       VIEW_MODE_OPENCV_CIRCLES	= 6;
    private static final int       VIEW_MODE_MY_JAVA_CIRCLES	= 7;

    private int                    mViewMode;
    private Mat                    mRgba;
    private Mat                    mGray;
    private Mat 				   thresholdImage;
    private Mat					   lines;
    private Mat					   circles;
    private Bitmap 				   threshBitmap;
    //private Bitmap				   mRgbaBitmap;

    private MenuItem               mItemPreviewRGBA;
    private MenuItem               mItemPreviewGray;
    private MenuItem               mItemPreviewSegment;
    private MenuItem               mItemPreviewOpencvLines;
    private MenuItem               mItemPreviewJavaLines;
    private MenuItem               mItemPreviewMyJavaLines;
    private MenuItem               mItemPreviewOpencvCircles;
    private MenuItem               mItemPreviewMyJavaCircles;
    private MenuItem			   mItemLinesThreshold;
    private MenuItem			   mItemOpencvMinLineSize;
    private MenuItem			   mItemOpencvMaxLineGap;

    private CameraBridgeViewBase   mOpenCvCameraView;
    
    private int linesThreshold = 100;
    private int opencvMinLineSize = 100;
    private int opencvMaxLineGap = 10;

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
        mItemPreviewRGBA = menu.add("RGBA");
        mItemPreviewGray = menu.add("GRAY");
        mItemPreviewSegment = menu.add("Segmentation");
        mItemPreviewOpencvLines = menu.add("OpenCV lines");
        mItemPreviewJavaLines = menu.add("Java lines");
        mItemPreviewMyJavaLines = menu.add("My Java lines");
        mItemPreviewOpencvCircles = menu.add("OpenCV circles");
        mItemPreviewMyJavaCircles = menu.add("My Java circles");
        mItemLinesThreshold = menu.add("Lines threshold");
        mItemOpencvMinLineSize = menu.add("Min line size");
        mItemOpencvMaxLineGap = menu.add("Max line gap");
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
        thresholdImage = new Mat(height, width, CvType.CV_8UC1);
        threshBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        //mRgbaBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        thresholdImage.release();
        threshBitmap.recycle();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        final int viewMode = mViewMode;
        switch (viewMode) {
        case VIEW_MODE_RGBA:
            // input frame has RBGA format			//15 fps
            mRgba = inputFrame.rgba();
            break;
        case VIEW_MODE_GRAY:
            // input frame has gray scale format	//30 fps
        	Imgproc.cvtColor(inputFrame.gray(), mRgba, Imgproc.COLOR_GRAY2RGBA, 4);
            break;
        case VIEW_MODE_SEGMENT:
        	mGray = inputFrame.gray();
        	mGray.submat(mGray.rows()/2, mGray.rows(), 0, mGray.cols()).copyTo(thresholdImage.submat(thresholdImage.rows()/2, thresholdImage.rows(), 0, thresholdImage.cols()));
        	Imgproc.adaptiveThreshold(thresholdImage, mRgba, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 3, -1);
        	mRgba.row(mRgba.rows()/2).setTo(new Scalar(0));
            break;
        case VIEW_MODE_OPENCV_LINES:   
        	//input frame has gray scale format		
        	lines = new Mat();        	
        	mRgba = inputFrame.rgba();				//15 fps
        	mGray = inputFrame.gray(); 				
        	
        	//dolna polovica obrazovky   			//15fps
        	mGray.submat(mGray.rows()/2, mGray.rows(), 0, mGray.cols()).copyTo(thresholdImage.submat(thresholdImage.rows()/2, thresholdImage.rows(), 0, thresholdImage.cols()));
        	
        	//rozmazanie							//7 fps
        	//Imgproc.GaussianBlur(thresholdImage, thresholdImage, new Size(15,15), 0.5);
        	
        	//odsegmentovanie						//10-12 fps
        	Imgproc.adaptiveThreshold(thresholdImage, thresholdImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 3, -1);
        	//Imgproc.threshold(thresholdImage, mRgba, 100, 255, Imgproc.THRESH_BINARY);
        	
        	//vymazanie bieleho riadka na hranici segmentacie	//10-11 fps
        	//thresholdImage.row(thresholdImage.rows()/2).setTo(new Scalar(0));
        	
        	//vymazanie malych bodov 				//9 fps
        	Imgproc.erode(thresholdImage, thresholdImage, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,2)));
        	
        	
        	//edge detection						//6 fps
        	/*
        	double mean = Core.mean(mGray).val[0];
        	Imgproc.Canny(thresholdImage, mRgba, 0.66*mean, 1.33*mean);
        	*/
        	
        	//line segments detection					// 4-2 fps
            /*Imgproc.HoughLinesP(thresholdImage, lines, 1, Math.PI/180, linesThreshold, opencvMinLineSize, opencvMaxLineGap);
        	
        	
            //line segment draw
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
            }*/
            
        	//line detection	//3-2 fps
            Imgproc.HoughLines(thresholdImage, lines, 1, Math.PI/180, linesThreshold);
            
            //line draw
            Mat tmp = new Mat(mRgba.rows(), mRgba.cols(), CvType.CV_8UC4);
            mRgba.copyTo(tmp);
            for (int j = 0; j < lines.cols(); j++) { 
            	double[] vec = lines.get(0, j);
            	double	 rho = vec[0],
            			 theta = vec[1];
            	
            	Point 	 start, end;
            	if((theta < Math.PI/4 || theta > 3 * Math.PI/4)){
                    start = new Point(rho / Math.cos(theta), 0);
                    end = new Point( (rho - tmp.rows() * Math.sin(theta))/Math.cos(theta), tmp.rows());
                }else {
                    start = new Point(0, rho / Math.sin(theta));
                    end = new Point(tmp.cols(), (rho - tmp.cols() * Math.cos(theta))/Math.sin(theta));
                }
            	
                Core.line(tmp, start, end, new Scalar(255,0,0), 3);
            }
            tmp.submat(tmp.rows()/2, tmp.rows(), 0, tmp.cols()).copyTo(mRgba.submat(mRgba.rows()/2, mRgba.rows(), 0, mRgba.cols()));
            
            //cleanup
            Log.i(TAG, "OpenCV lines:" + lines.cols());
            tmp.release();
            tmp = null;
            lines.release();
            lines = null;
            break;
    	case VIEW_MODE_JAVA_LINES:   
    		//input frame has gray scale format     	
        	mRgba = inputFrame.rgba();			
        	mGray = inputFrame.gray();
        	mGray.submat(mGray.rows()/2, mGray.rows(), 0, mGray.cols()).copyTo(thresholdImage.submat(thresholdImage.rows()/2, thresholdImage.rows(), 0, thresholdImage.cols()));
        	Imgproc.adaptiveThreshold(thresholdImage, thresholdImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 3, -1);
        	Imgproc.erode(thresholdImage, thresholdImage, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,2)));

        	// 6 fps
        	//Utils.matToBitmap(mRgba, mRgbaBitmap);
        	Utils.matToBitmap(thresholdImage, threshBitmap);
        	
        	
            HoughLineTransform houghLineTransform = new HoughLineTransform(thresholdImage.cols(), thresholdImage.rows()); 
            
            //0.5 fps
            //houghLineTransform.addPoints(thresholdImage); 
            
            //1-2fps
            houghLineTransform.addPoints(threshBitmap);
     
            Vector<HoughLine> lines = houghLineTransform.getLines(linesThreshold); 
     
            for (int j = 0; j < lines.size(); j++) { 
                HoughLine line = lines.elementAt(j); 
                //line.draw(mRgbaBitmap, Color.RED); 
                line.draw(mRgba); 
            }    
            //Utils.bitmapToMat(threshBitmap, thresholdImage);
            
            //cleanup
            Log.i(TAG, "Java lines:" + lines.size());
            lines.clear();
            lines = null;
            break;
    	case VIEW_MODE_MY_JAVA_LINES:   
    		//input frame has gray scale format     	
        	mRgba = inputFrame.rgba();			
        	mGray = inputFrame.gray();
        	mGray.submat(mGray.rows()/2, mGray.rows(), 0, mGray.cols()).copyTo(thresholdImage.submat(thresholdImage.rows()/2, thresholdImage.rows(), 0, thresholdImage.cols()));
        	Imgproc.adaptiveThreshold(thresholdImage, thresholdImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 3, -1);
        	Imgproc.erode(thresholdImage, thresholdImage, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,2)));

            MyHoughLineTransform myHoughLineTransform = new MyHoughLineTransform(thresholdImage, linesThreshold, 180);
            
            Mat temp = new Mat(mRgba.rows(), mRgba.cols(), CvType.CV_8UC4);
            mRgba.copyTo(temp);
            myHoughLineTransform.drawLines(temp);
            temp.submat(temp.rows()/2, temp.rows(), 0, temp.cols()).copyTo(mRgba.submat(mRgba.rows()/2, mRgba.rows(), 0, mRgba.cols()));
            
            //cleanup
            Log.i(TAG, "My Java lines");
            temp.release();
            temp = null;
            break;
    	case VIEW_MODE_OPENCV_CIRCLES:   
        	//input frame has gray scale format		
        	circles = new Mat();        	
        	mRgba = inputFrame.rgba();				//15 fps
        	mGray = inputFrame.gray(); 				
        	
        	//rozmazanie							
        	Imgproc.GaussianBlur(mGray, mGray, new Size(15,15), 0.5);
        	
        	//odsegmentovanie						
        	//Imgproc.adaptiveThreshold(thresholdImage, thresholdImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 3, -1);
        	//Imgproc.threshold(thresholdImage, mRgba, 100, 255, Imgproc.THRESH_BINARY);
        	
        	//circle detection
            Imgproc.HoughCircles(mGray, circles, Imgproc.CV_HOUGH_GRADIENT, 1, 50);
            
            //circles draw
            for (int i = 0; i < circles.cols(); i++)
            {
                  double[] vec = circles.get(0, i);
                  double x = vec[0],
                         y = vec[1];
                  int radius = (int) vec[2];
                  Point center = new Point(x, y);
                  Core.circle(mRgba, center, 1, new Scalar(0,255,0), 3);
                  Core.circle(mRgba, center, radius, new Scalar(255,0,0), 3);
            }
            
            //cleanup
            Log.i(TAG, "OpenCV circles:" + circles.cols());
            circles.release();
            circles = null;
            break;
    	case VIEW_MODE_MY_JAVA_CIRCLES:   
    		//input frame has gray scale format     	
        	mRgba = inputFrame.rgba();			
        	mGray = inputFrame.gray();
        	mGray.copyTo(thresholdImage);
        	Imgproc.adaptiveThreshold(thresholdImage, thresholdImage, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 3, -1);
        	//Imgproc.erode(thresholdImage, thresholdImage, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2,2)));

            /*
            MyHoughCircleTransformWithKnownRadius myHoughCircleTransformWithKnownRadius = new MyHoughCircleTransformWithKnownRadius(thresholdImage, 27, 180, 60);
            myHoughCircleTransformWithKnownRadius.drawCircles(mRgba);
            */
        	
            
            MyHoughCircleTransformWithUnknownRadius myHoughCircleTransformWithUnknownRadius = new MyHoughCircleTransformWithUnknownRadius(thresholdImage, 27, 180, 50, 60, 5);
            myHoughCircleTransformWithUnknownRadius.drawCircles(mRgba);
            
        	
            Log.i(TAG, "My Java circles");
            break;
    		
    		/* Process static image from sd card.
    		 *
    		Bitmap copy = getImageFromSDCard("image.jpg");
    		Utils.bitmapToMat(copy, thresholdImage);
            MyHoughCircleTransformWithKnownRadius myHoughCircleTransformWithKnownRadius = new MyHoughCircleTransformWithKnownRadius(thresholdImage, 20, 180, 18);
            myHoughCircleTransformWithKnownRadius.drawCircles(thresholdImage);
            Utils.matToBitmap(thresholdImage, copy);
            saveImageToSDCard(copy);
     
        	Log.i(TAG, "Java circles");
            this.finish();
            */
        }

        return mRgba;
    }

	private void saveImageToSDCard(Bitmap copy) {
		String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/tmp");    
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-"+ n +".jpg";
        File save = new File (myDir, fname);
        Log.i("file", fname + " saved to: " + myDir);
        if (save.exists ()) save.delete (); 
        try {
               FileOutputStream out = new FileOutputStream(save);
               copy.compress(Bitmap.CompressFormat.JPEG, 90, out);
               out.flush();
               out.close();
        } catch (Exception e) {
               e.printStackTrace();
        }
	}

	private Bitmap getImageFromSDCard(String name) {
		File file = new File(Environment.getExternalStorageDirectory().toString() + "/" + name);
		Bitmap image = BitmapFactory.decodeFile(file.getAbsolutePath());
		Bitmap copy = image.copy(image.getConfig(), true);
		return copy;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

        if (item == mItemPreviewRGBA) {
            mViewMode = VIEW_MODE_RGBA;
        } else if (item == mItemPreviewGray) {
            mViewMode = VIEW_MODE_GRAY;
        } else if (item == mItemPreviewSegment) {
            mViewMode = VIEW_MODE_SEGMENT;
        } else if (item == mItemPreviewOpencvLines) {
            mViewMode = VIEW_MODE_OPENCV_LINES;
        } else if (item == mItemPreviewJavaLines) {
            mViewMode = VIEW_MODE_JAVA_LINES;   
        } else if (item == mItemPreviewMyJavaLines) {
            mViewMode = VIEW_MODE_MY_JAVA_LINES;   
        } else if (item == mItemPreviewOpencvCircles) {
            mViewMode = VIEW_MODE_OPENCV_CIRCLES;
        } else if (item == mItemPreviewMyJavaCircles) {
            mViewMode = VIEW_MODE_MY_JAVA_CIRCLES;
        } else if (item == mItemLinesThreshold) {
            if (linesThreshold < 200){
            	linesThreshold += 20;
            } else {
            	linesThreshold = 20;
            }
            Toast.makeText(HoughActivity.this,"threshold: "+linesThreshold,
                    Toast.LENGTH_SHORT).show();
        } 
        else if (item == mItemOpencvMinLineSize) {
            if (opencvMinLineSize < 200){
            	opencvMinLineSize += 20;
            } else {
            	opencvMinLineSize = 20;
            }
            Toast.makeText(HoughActivity.this,"minLineSize: "+opencvMinLineSize,
                    Toast.LENGTH_SHORT).show();
        } 
        else if (item == mItemOpencvMaxLineGap) {
            if (opencvMaxLineGap < 200){
            	opencvMaxLineGap += 20;
            } else {
            	opencvMaxLineGap = 20;
            }
            Toast.makeText(HoughActivity.this,"maxLineGap: "+opencvMaxLineGap,  
                    Toast.LENGTH_SHORT).show();
        } 

        return true;
    }
    
}
