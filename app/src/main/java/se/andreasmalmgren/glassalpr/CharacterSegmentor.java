package se.andreasmalmgren.glassalpr;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by andreas on 15/03/15.
 */
public class CharacterSegmentor {
    private static final int CV_INTER_CUBIC = 2;
    private Mat plateToSegment;
    private Mat deRotatedPlate;
    private Mat deRotatedCropedPlate;
    private ArrayList<Rect> characterRects;

    public CharacterSegmentor(Mat plate){
        this.plateToSegment = plate;
        //make image binary
        this.deRotatedPlate = new Mat();
        Imgproc.threshold(this.plateToSegment, this.deRotatedPlate, 0, 255, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY);
        this.deRotatedCropedPlate = new Mat();
        this.characterRects = new ArrayList<Rect>();
    }

    public void segmentUsingProjection(){

        Debugger.log("Running segmentation with projection");
        long startTime = System.currentTimeMillis();

        //make image binary
        //Mat binaryImg = new Mat();
        //Imgproc.threshold(this.plateToSegment, binaryImg, 200, 255, Imgproc.THRESH_OTSU + Imgproc.THRESH_BINARY);

        //Find vertical segment and crop
        Mat verticalHistogram = verticalProjection(this.deRotatedPlate);
        Rect verticalArea = getCharactersVerticalPositionRect(verticalHistogram);
        this.deRotatedCropedPlate = this.deRotatedPlate.submat(verticalArea);

        //Find horizontal segments (characters)
        Mat horizontalHistogram = horizontalProjection(this.deRotatedCropedPlate);
        this.characterRects = getHorizontalSegmentRects(horizontalHistogram, this.deRotatedCropedPlate.rows());

        //findCharactersFromHistorgram(binaryImg, verticalHistogram, horizontalHistogram);

        Debugger.log(String.format("Detected %s characters in %d ms", this.characterRects.size(), System.currentTimeMillis() - startTime));

    }

    public void deRotatePlateWithContours(){
        Debugger.log("Derotating with contours");
        long startTime = System.currentTimeMillis();

        //Find contours
        List<MatOfPoint> candidateContours = new ArrayList<MatOfPoint>();
        Mat contourImage = this.deRotatedPlate.clone();
        Imgproc.findContours(contourImage, candidateContours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);


        //Filter regions that does not match license plates dimensions
        List<RotatedRect> candidateRectangles = new ArrayList<RotatedRect>();
        Iterator<MatOfPoint> i = candidateContours.iterator();
        while(i.hasNext()){
            MatOfPoint2f tempCandidate = new MatOfPoint2f(i.next().toArray());
            RotatedRect candidate = Imgproc.minAreaRect(tempCandidate);

            if(PlateDetector.hasLicensePlateAspectRatio(candidate) && PlateDetector.hasLicensePlateMinSize(candidate)){
                Debugger.log("Candidate angle: " + candidate.angle);
                candidateRectangles.add(candidate);
                //Rect rect = candidate.boundingRect();
                //Core.rectangle(origialImg, rect.tl(), rect.br(), new Scalar(0, 255, 0),3, 8,0);
            }else{
                i.remove();
            }
        }

        for(RotatedRect rect : candidateRectangles){
            //get rotation matrix
            Mat rotationMatrix= Imgproc.getRotationMatrix2D(rect.center, rect.angle, 1);

            //Create and rotate image
            Mat rotatedPlate = new Mat();
            Imgproc.warpAffine(this.deRotatedPlate, rotatedPlate, rotationMatrix, this.deRotatedPlate.size(), CV_INTER_CUBIC);
            this.deRotatedPlate = rotatedPlate;
            //Utilities.displayMat(this.deRotatedPlate);
        }

        Debugger.log(String.format("Detected ROI in %d ms", System.currentTimeMillis() - startTime));
    }

    private boolean isMatDimentionsLargeEnoughForCharOCR(Mat image){
        return (image.rows() > 16 && image.cols() > 80);
    }

    private Mat horizontalProjection(Mat binaryPlate){
        Mat horizontalProjection = new Mat(new Size(1, binaryPlate.cols()), CvType.CV_64FC1);
        Core.reduce(binaryPlate, horizontalProjection, 0, Core.REDUCE_SUM, CvType.CV_64FC1);

        int size = (int) horizontalProjection.total();
        double[] temp = new double[size]; // use double[] instead of byte[]
        horizontalProjection.get(0, 0, temp);
        for (int i = 0; i < size; i++){
            temp[i] = (temp[i] / 255);  // no more casting required.
        }

        horizontalProjection.put(0, 0, temp);

        return horizontalProjection;
    }

    private Mat verticalProjection(Mat binaryPlate){
        Mat verticalProjection = new Mat(new Size(binaryPlate.rows(), 1), CvType.CV_64FC1);
        Core.reduce(binaryPlate, verticalProjection, 1, Core.REDUCE_SUM, CvType.CV_64FC1);

        int size = (int) verticalProjection.total();
        double[] temp = new double[size];
        verticalProjection.get(0, 0, temp);
        for (int i = 0; i < size; i++){
            temp[i] = (temp[i] / 255);  // no more casting required.
        }
        verticalProjection.put(0, 0, temp);

        return verticalProjection;
    }

    private Rect getCharactersVerticalPositionRect(Mat verticalHistogram){
        int size = (int) verticalHistogram.total();
        double[] temp = new double[size];
        verticalHistogram.get(0, 0, temp);

        int topMaxPosition = 0;
        int bottomMaxPosition = 0;
        double topMax = 0;
        double bottomMax = 0;
        for(int i = 0; i < size; i++){
            if(i < (size/2)){
                if(temp[i] > topMax){
                    topMax = temp[i];
                    topMaxPosition = i;
                }
            }else{
                if(temp[i] >= bottomMax){
                    bottomMax = temp[i];
                    bottomMaxPosition = i;
                }
            }
        }

        return new Rect(new Point(0, topMaxPosition), new Point(this.plateToSegment.cols(), bottomMaxPosition));
    }

    private ArrayList<Rect> getHorizontalSegmentRects(Mat horizontalHistogram, int imageHeight){
        ArrayList<Rect> characterSegments = new ArrayList<Rect>();

        int size = (int) horizontalHistogram.total();
        double[] temp = new double[size];
        horizontalHistogram.get(0, 0, temp);

        double previousValue = 0;
        double segmentStart = -1;
        imageHeight--; //Allow the colum to have one black pixel when segmenting
        for(int i = 0; i < size; i++){
            //if there are only white pixels along the vertical
            if(temp[i] < imageHeight && previousValue >= imageHeight){
                segmentStart = i;
            }else if(temp[i] >= imageHeight && previousValue < imageHeight && segmentStart != -1){
                characterSegments.add(new Rect(new Point(segmentStart, 0), new Point(i, imageHeight))); //character height should be 1.6 times the width
            }
            previousValue = temp[i];
        }

        return characterSegments;
    }

    //draw character position rects on image
    public Mat getImageWithDrawnROI(){
        Mat deRotadedCropedWithROI = this.deRotatedCropedPlate.clone();
        Imgproc.cvtColor(deRotadedCropedWithROI, deRotadedCropedWithROI, Imgproc.COLOR_GRAY2RGB);
        for(Rect rect : this.characterRects){
            Core.rectangle(deRotadedCropedWithROI, rect.tl(), rect.br(), new Scalar(255, 0, 0));
        }

        return deRotadedCropedWithROI;
    }

    public ArrayList<Mat> getBinaryCharMats(){
        ArrayList<Mat> grayCharMats = new ArrayList<Mat>();
        for(Rect rect : this.characterRects){
            grayCharMats.add(this.deRotatedCropedPlate.submat(rect));
        }
        return grayCharMats;
    }

    public Mat getImage(){
        return this.deRotatedCropedPlate;
    }

    private void findCharactersFromHistorgram(Mat binaryImg, Mat verticalHistogram, Mat horizontalHistogram){
        Rect verticalArea = getCharactersVerticalPositionRect(verticalHistogram);
        Mat verticalCropedImg = binaryImg.submat(verticalArea);



        //Utilities.displayImage(Utilities.matToBufferedImageArrayCopy(verticalCropedImg));

    }

}
