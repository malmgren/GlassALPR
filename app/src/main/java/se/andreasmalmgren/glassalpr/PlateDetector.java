package se.andreasmalmgren.glassalpr;

import android.content.Context;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by amaxp2 on 2015-03-12.
 */
public class PlateDetector {
    public static final int CASCADE_CLASSIFIER = 0;
    public static final int VERTICAL_EDGES = 1;



    private CascadeClassifier plateDetector;
    private List<RegionOfInterest> regionsOfInterest;
    private Mat originalImage;
    private Mat grayOriginalImage;
    private Context context;

    PlateDetector(Context current){
        this.context = current;

        try {
            // load cascade file from application resources
            InputStream is = this.context.getResources().openRawResource(R.raw.cascade);
            File cascadeDir = this.context.getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "cascade.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            this.plateDetector = new CascadeClassifier(cascadeFile.getAbsolutePath());
            if (this.plateDetector.empty()) {
                Debugger.log_e("Failed to load cascade classifier");
                this.plateDetector = null;
            } else
                Debugger.log("Loaded cascade classifier from " + cascadeFile.getAbsolutePath());

            cascadeDir.delete();

        } catch (IOException e) {
            e.printStackTrace();
            Debugger.log_e("Failed to load cascade. Exception thrown: " + e);
        }



        // Create a plate detector from the cascade file in the resources directory.
        //this.plateDetector = new CascadeClassifier("C:\\Dropbox\\Skola\\Exjobb\\Kod\\Classifier\\cascade_classifier\\16stages-2000pos-7000neg-extended-haar-classifier\\cascade.xml");
        this.originalImage = new Mat();
        this.grayOriginalImage = new Mat();
        this.regionsOfInterest = new ArrayList<RegionOfInterest>();
    }

    // Detect regions of interest using pre-trained cascade classifier
    private void detectROIWithCascadeClassifier(){
        Debugger.log("Running ROI detection using Cascade classifiers");
        long startTime = System.currentTimeMillis();

        //reset list with ROI
        this.regionsOfInterest = new ArrayList<RegionOfInterest>();

        // Make image gray scale
        /*Mat grayImage = new Mat();
        Imgproc.cvtColor(this.originalImage, grayImage, Imgproc.COLOR_RGB2GRAY);*/

        // Detect plates in the image.
        MatOfRect rectsOfInterest = new MatOfRect();
        this.plateDetector.detectMultiScale(this.grayOriginalImage, rectsOfInterest, 1.15, 3, 0, new Size(80, 17), new Size(240, 51));

        //Convert to RegionOfInterest
        for(Rect rect : rectsOfInterest.toList()){
            this.regionsOfInterest.add(new RegionOfInterest(rect));
        }

        Debugger.log(String.format("Detected %s ROI in %d ms", rectsOfInterest.toArray().length, System.currentTimeMillis() - startTime));
    }

    private void detectROIWithVerticalEdges(){
        Debugger.log("Running ROI detection using Vertical edges");
        long startTime = System.currentTimeMillis();

        //reset list with ROI
        this.regionsOfInterest = new ArrayList<RegionOfInterest>();

        // Make image gray scale
        Mat tempImage = this.grayOriginalImage;

        // Blur
        Imgproc.blur(tempImage, tempImage, new Size(5, 5));
        Imgproc.Sobel(tempImage, tempImage, CvType.CV_8U, 1, 0, 3, 1, 0);
        //Threshold using Otsu
        Imgproc.threshold(tempImage, tempImage, 0, 255, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY);

        //Perform morphological operation
        Mat licensePlateMorphElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(17, 3));
        Imgproc.morphologyEx(tempImage, tempImage, Imgproc.MORPH_CLOSE, licensePlateMorphElement);

        //Find contours in the image
        List<MatOfPoint> candidateContours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(tempImage, candidateContours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

        // Convert to RotatedRect
        int numCandidates = 0;
        Iterator<MatOfPoint> i = candidateContours.iterator();
        while(i.hasNext()){
            MatOfPoint2f tempCandidate = new MatOfPoint2f(i.next().toArray());
            RotatedRect candidate = Imgproc.minAreaRect(tempCandidate);

            //Verify aspect ratio and min height of ROI
            if(hasLicensePlateAspectRatio(candidate) && hasLicensePlateMinSize(candidate)){
                numCandidates++;
                this.regionsOfInterest.add(new RegionOfInterest(candidate));
            }else{
                i.remove();
            }
        }

        Debugger.log(String.format("Detected %s ROI in %d ms", numCandidates, System.currentTimeMillis() - startTime));
    }

    public int getNumOfROI(){
        return regionsOfInterest.size();
    }

    public static boolean hasLicensePlateAspectRatio(RotatedRect candidate){

        double candidateRatio =  candidate.size.width/candidate.size.height;
        if(candidateRatio >= 3 && candidateRatio <= 6 && candidate.angle < 45 && candidate.angle > -45){
            return true;
        }else{
            return false;
        }
    }

    public static boolean hasLicensePlateMinSize(RotatedRect candidate){

        double candidateRatio =  candidate.size.width/candidate.size.height;
        if(candidate.size.height >= 17 && candidate.size.width >= 80){
            return true;
        }else{
            return false;
        }
    }


    public Mat getImageWithDrawnROI(){
        Mat imageWithROI;
        imageWithROI = this.originalImage;
        for (RegionOfInterest region : this.regionsOfInterest) {
            region.drawRegionOfInterest(imageWithROI);
            //Core.rectangle(imageWithROI, new Point(region.x(), region.y()), new Point(region.x() + region.width(), region.x() + region.height()), new Scalar(0, 255, 0));
        }
        return imageWithROI;
    }

    public void detectPlateFromMat(Mat originalImage, int classifierType) {
        this.setImage(originalImage);

        if (classifierType == 0) {
            this.detectROIWithCascadeClassifier();
        }else if (classifierType == 1){
            this.detectROIWithVerticalEdges();
        }

    }

    public Mat getMatPlate(){
        if (this.regionsOfInterest.size() == 0){
            return null;
        }
        Mat matPlate = this.grayOriginalImage.submat(this.regionsOfInterest.get(0).getBoundingBoxWithTopAndBottomPadding());
        return matPlate;
    }

    public void setImage(Mat image){
        this.originalImage = image;

        if(image.channels() != 1){
            // Make image gray scale
            Imgproc.cvtColor(this.originalImage, this.grayOriginalImage, Imgproc.COLOR_RGB2GRAY);
        }else{
            this.grayOriginalImage = originalImage;
        }

    }

    public void getStraightenPlates(){
        for(RegionOfInterest roi : this.regionsOfInterest){
            if(roi.isFromCascade()){
                //Find contours
                List<MatOfPoint> candidateContours = new ArrayList<MatOfPoint>();

                //Threshold using Otsu
                Mat tempImage = new Mat();
                Imgproc.threshold(this.grayOriginalImage, tempImage, 0, 255, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY);

                Imgproc.findContours(tempImage, candidateContours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
            }
        }
    }
}
