package se.andreasmalmgren.glassalpr;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

/**
 * Created by amaxp2 on 2015-03-12.
 */
public class RegionOfInterest {
    private Rect rectRegion;
    private RotatedRect rotatedRectRegion;
    private boolean regionIsFromCascade;

    RegionOfInterest(Rect region){
        //this.rectRegion = region;
        Point rectCenter = new Point(region.tl().x+(region.width/2),region.tl().y+(region.height/2));
        this.rotatedRectRegion = new RotatedRect(rectCenter, new Size(region.width, region.height), 0);
        this.regionIsFromCascade = true;
    }

    RegionOfInterest(RotatedRect region){
        this.rotatedRectRegion = region;
        this.regionIsFromCascade = false;
        //this.rectRegion = rotatedRectRegion.boundingRect();
    }

    public boolean isFromCascade(){
        return this.regionIsFromCascade;
    }

    public double width(){
        return rotatedRectRegion.size.width;
    }

    public double height(){
        return rotatedRectRegion.size.height;
    }

    public Rect getBoundingBox(){
        return this.rotatedRectRegion.boundingRect();
    }

    public Rect getBoundingBoxWithTopAndBottomPadding(){
        Rect roiRect = this.rotatedRectRegion.boundingRect();
        roiRect.y = (int) (roiRect.y - roiRect.height*0.25);
        roiRect.height = (int) (roiRect.height*1.5);
        return roiRect;
    }

    public void drawRegionOfInterest(Mat imageToDrawOn){
        if(isFromCascade()){
            Rect boundingBox = this.getBoundingBoxWithTopAndBottomPadding();
            Core.rectangle(imageToDrawOn, new Point(boundingBox.x, boundingBox.y), new Point(boundingBox.x + boundingBox.width, boundingBox.y + boundingBox.height), new Scalar(255, 0, 0));
        }else{
            Point[] rect_points = new Point[4];
            this.rotatedRectRegion.points(rect_points);
            for( int j = 0; j < 4; j++ )
                Core.line(imageToDrawOn, rect_points[j], rect_points[(j + 1) % 4], new Scalar(255, 0, 0), 2, 8, 0);
        }

    }

}
