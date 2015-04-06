package se.andreasmalmgren.glassalpr;

/*import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;*/
import android.graphics.Bitmap;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

/**
 * Created by amaxp2 on 2015-03-20.
 */
public class CharacterRecognitor {
    public static int OCR_ALPHANUM = 0;
    public static int OCR_DIGITS = 1;


    public static String tesseractDesktopOCR(Mat charMat, int charType){
        //Get tesseract instance
        /*Tesseract tesseractInstance = Tesseract.getInstance();
        tesseractInstance.setLanguage("eng");

        if(charType == OCR_ALPHANUM){
            tesseractInstance.setTessVariable("tessedit_char_whitelist", "ABCDEFGHIJKLMNOPQRESTUVWXYZ");
        }else{
            tesseractInstance.setTessVariable("tessedit_char_whitelist", "0123456789");
        }

        String result = "";
        try {
            result = tesseractInstance.doOCR(Utilities.matToBufferedImage(charMat));
            result = result.trim();
        } catch (TesseractException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;*/
        return "";
    }

    public static String tesseractAndroidOCR(Mat charMat, int charType){
        String path = "/mnt/sdcard/external_sd/tess/";

        TessBaseAPI tesseractInstance = new TessBaseAPI();
        tesseractInstance.setDebug(true);
        tesseractInstance.init(path, "eng");

        //Get tesseract instance
        /*Tesseract tesseractInstance = Tesseract.getInstance();
        tesseractInstance.setLanguage("eng");*/

        if(charType == OCR_ALPHANUM){
            tesseractInstance.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "ABCDEFGHIJKLMNOPQRESTUVWXYZ");
            tesseractInstance.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "0123456789");
        }else{
            tesseractInstance.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "ABCDEFGHIJKLMNOPQRESTUVWXYZ");
            tesseractInstance.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789");
        }

        String result = "";

        Bitmap charBitmap = Bitmap.createBitmap(charMat.cols(), charMat.rows(),Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(charMat, charBitmap);

        tesseractInstance.setImage(charBitmap);
        result = tesseractInstance.getUTF8Text();
        tesseractInstance.end();
        result = result.trim();


        return result;

    }

}
