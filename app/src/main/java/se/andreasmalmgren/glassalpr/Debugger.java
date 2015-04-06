package se.andreasmalmgren.glassalpr;

import android.util.Log;

/**
 * Created by amaxp2 on 2015-03-12.
 */
public class Debugger {
    private static final String TAG = "OpenCVALPR::Activity";

    public static boolean isEnabled(){
        return true;
    }
    public static boolean isAndroid(){
        return true;
    }

    public static void log(Object o){
        if(Debugger.isEnabled()) {
            if(Debugger.isAndroid()){
                Log.i(TAG, o.toString());
            }else{
                System.out.println(o.toString());
            }

        }
    }

    public static void log_e(Object o){
        if(Debugger.isEnabled()) {
            if(Debugger.isAndroid()){
                Log.e(TAG, o.toString());
            }else{
                System.out.println(o.toString());
            }
        }
    }
}
