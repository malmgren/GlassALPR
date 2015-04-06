package se.andreasmalmgren.glassalpr;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.features2d.Features2d;
import org.opencv.highgui.Highgui;

import java.io.IOException;
import java.util.ArrayList;

/**
 * An {@link Activity} showing a tuggable "Hello World!" card.
 * <p/>
 * The main content view is composed of a one-card {@link CardScrollView} that provides tugging
 * feedback to the user when swipe gestures are detected.
 * If your Glassware intends to intercept swipe gestures, you should set the content view directly
 * and use a {@link com.google.android.glass.touchpad.GestureDetector}.
 *
 * @see <a href="https://developers.google.com/glass/develop/gdk/touch">GDK Developer Guide</a>
 */
public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2{
    private JavaCameraView alprCameraView;
    private boolean captureFrame = false;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if (status == LoaderCallbackInterface.SUCCESS ) {
                //doALPR();
                //loadImage();
                //doShit();
            } else {
                super.onManagerConnected(status);
            }
        }
    };

    private void doALPR(Mat image) {
        /*try {*/
            //Mat image = Utils.loadResource(this, R.drawable.truck_normal, Highgui.CV_LOAD_IMAGE_COLOR);

            PlateDetector detector = new PlateDetector(this);

            long startTime = System.currentTimeMillis();

            detector.detectPlateFromMat(image, PlateDetector.CASCADE_CLASSIFIER);
            Mat roi = detector.getMatPlate();

            //Don't try to print if roi is null, no plate was detected
            if(roi != null){

                CharacterSegmentor segmentor = new CharacterSegmentor(roi);
                segmentor.deRotatePlateWithContours();
                segmentor.segmentUsingProjection();

                /*Mat imageWithROI = segmentor.getImageWithDrawnROI();
                Bitmap truckBitmap = Bitmap.createBitmap(imageWithROI.cols(), imageWithROI.rows(),Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(imageWithROI, truckBitmap);
                ImageView imageSurface = (ImageView) findViewById(R.id.imageView);
                imageSurface.setImageBitmap(truckBitmap);*/

                long charStartTime = System.currentTimeMillis();

                ArrayList<Mat> binaryCharMats = segmentor.getBinaryCharMats();
                String licensePlateText = "";

                if(binaryCharMats.size() == 6){
                    int charNum = 0;
                    int charType = CharacterRecognitor.OCR_ALPHANUM;
                    Debugger.log("Recognizing characters");
                    for(Mat charMat : binaryCharMats){
                        charNum++;
                        if(charNum == 4){
                            charType = CharacterRecognitor.OCR_DIGITS;
                        }
                        licensePlateText += CharacterRecognitor.tesseractAndroidOCR(charMat, charType);
                    }

                    Debugger.log(String.format("Plate was recognized as %s in %d ms", licensePlateText, System.currentTimeMillis()-charStartTime));
                    Debugger.log(String.format("The complete process took %d ms", System.currentTimeMillis()-startTime));
                }else{
                    Debugger.log("Failed to recognize 6 characters. Continuing");
                }

            }


        /*} catch (IOException e) {
            e.printStackTrace();
        }*/


    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent event) {
        if (keycode == KeyEvent.KEYCODE_CAMERA) {
            // user tapped pressed camera button
            //doALPR();
            this.captureFrame = true;

            return true;
        }

        return super.onKeyDown(keycode, event);
    }

    /**
     * {@link CardScrollView} to use as the main content view.
     */
    private CardScrollView mCardScroller;

    /**
     * "Hello World!" {@link View} generated by {@link #buildView()}.
     */
    private View mView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);
        alprCameraView = (JavaCameraView) findViewById(R.id.alpr_camera_view);
        alprCameraView.setVisibility(SurfaceView.VISIBLE);
        alprCameraView.setCvCameraViewListener(this);

        /*mView = buildView();

        mCardScroller = new CardScrollView(this);
        mCardScroller.setAdapter(new CardScrollAdapter() {
            @Override
            public int getCount() {
                return 1;
            }

            @Override
            public Object getItem(int position) {
                return mView;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return mView;
            }

            @Override
            public int getPosition(Object item) {
                if (mView.equals(item)) {
                    return 0;
                }
                return AdapterView.INVALID_POSITION;
            }
        });
        // Handle the TAP event.
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Plays disallowed sound to indicate that TAP actions are not supported.
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.playSoundEffect(Sounds.DISALLOWED);
            }
        });
        //setContentView(mCardScroller);
        setContentView(R.layout.activity_main);*/
    }

    @Override
    protected void onResume() {
        super.onResume();
        alprCameraView.enableView();
        //mCardScroller.activate();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_10, this, mLoaderCallback);
    }

    @Override
    protected void onPause() {
        //mCardScroller.deactivate();
        super.onPause();
        if (alprCameraView != null)
            alprCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (alprCameraView != null)
            alprCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
//        mDemoCameraView.setResolution();
    }

    @Override
    public void onCameraViewStopped() {
        alprCameraView.disableView();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        if(this.captureFrame){
            this.captureFrame = false;
            try{
                Mat image = Utils.loadResource(this, R.drawable.truck_normal, Highgui.CV_LOAD_IMAGE_COLOR);
                doALPR(image);
            }catch (Exception e){

            }

        }
        return inputFrame.rgba();
    }

    /**
     * Builds a Glass styled "Hello World!" view using the {@link CardBuilder} class.
     */
    private View buildView() {
        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TEXT);

        card.setText(R.string.hello_world);
        return card.getView();
    }

}
