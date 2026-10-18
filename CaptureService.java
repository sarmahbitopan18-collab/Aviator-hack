package com.example.aviatorscreenreader;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.regex.*;
import java.util.Locale;

public class CaptureService extends Service {
    static final String CHANNEL="aviator_reader";
    static final ArrayDeque<String> results=new ArrayDeque<>();
    static volatile String latest=null;
    static volatile Runnable listener=null;
    MediaProjection projection; VirtualDisplay display; ImageReader reader;
    TextRecognizer recognizer; Handler handler; boolean busy=false;
    int width,height,density;
    static void addResult(Context c,double v,String src){
        synchronized(results){results.addFirst(String.format(Locale.US,"%.2fx",v));while(results.size()>100)results.removeLast();}
        latest=String.format(Locale.US,"%.2fx",v); if(listener!=null)listener.run();
    }
    static String getLatest(){return latest;}
    static String getHistoryText(){synchronized(results){if(results.isEmpty())return "none";StringBuilder b=new StringBuilder();for(String s:results){if(b.length()>0)b.append("\\n");b.append("• ").append(s);}return b.toString();}}
    static void setListener(Runnable r){listener=r;}
    @Override public void onCreate(){super.onCreate();handler=new Handler(Looper.getMainLooper());recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);createChannel();}
    void createChannel(){
        if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel(CHANNEL,"Screen reader",NotificationManager.IMPORTANCE_LOW);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);}
    }
    @Override public int onStartCommand(Intent i,int flags,int id){
        startForeground(1,new Notification.Builder(this,CHANNEL).setContentTitle("Aviator Screen Reader").setContentText("Screen capture is active; tap Stop in the app to end.").setSmallIcon(android.R.drawable.ic_menu_view).build());
        if(i==null)return START_NOT_STICKY;
        int rc=i.getIntExtra("resultCode",0); Intent data=i.getParcelableExtra("data");
        if(data==null){stopSelf();return START_NOT_STICKY;}
        MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        projection=m.getMediaProjection(rc,data);
        WindowManager wm=(WindowManager)getSystemService(WINDOW_SERVICE); DisplayMetrics dm=new DisplayMetrics(); wm.getDefaultDisplay().getRealMetrics(dm);
        width=dm.widthPixels;height=dm.heightPixels;density=dm.densityDpi;
        reader=ImageReader.newInstance(width,height,android.graphics.PixelFormat.RGBA_8888,2);
        projection.registerCallback(new MediaProjection.Callback(){@Override public void onStop(){stopSelf();}},handler);
        display=projection.createVirtualDisplay("AviatorScreenReader",width,height,density,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,handler);
        reader.setOnImageAvailableListener(r->processFrame(r),handler);
        return START_NOT_STICKY;
    }
    void processFrame(ImageReader r){
        Image im=null;
        try{
            if(busy)return;
            im=r.acquireLatestImage(); if(im==null)return;
            busy=true;
            Image.Plane p=im.getPlanes()[0]; ByteBuffer buf=p.getBuffer();
            int pix=p.getPixelStride(), row=p.getRowStride();
            Bitmap full=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
            full.copyPixelsFromBuffer(buf);
            // Crop to the central/lower area where crash-game multipliers commonly appear.
            int left=(int)(width*0.10), top=(int)(height*0.15), cw=(int)(width*0.80), ch=(int)(height*0.70);
            Bitmap crop=Bitmap.createBitmap(full,left,top,Math.min(cw,width-left),Math.min(ch,height-top)); full.recycle();
            InputImage input=InputImage.fromBitmap(crop,0);
            recognizer.process(input).addOnSuccessListener(text->{parseText(text);crop.recycle();busy=false;})
              .addOnFailureListener(e->{crop.recycle();busy=false;});
        }catch(Exception e){if(im!=null)im.close();busy=false;}
        finally{if(im!=null)im.close();}
    }
    void parseText(Text text){
        String all=text.getText();
        // Detect multiplier-like text (e.g. 1.23x); ignore numbers without x to reduce false positives.
        Pattern p=Pattern.compile("(?i)(\\d{1,4}(?:\\.\\d{1,3})?)\\s*[x×]");
        Matcher m=p.matcher(all);
        while(m.find()){
            try{double v=Double.parseDouble(m.group(1));if(v>=1.0&&v<=1000000)addResult(this,v,"ocr");}catch(Exception ignored){}
        }
    }
    @Override public void onDestroy(){
        if(reader!=null)reader.close();if(display!=null)display.release();if(projection!=null)projection.stop();if(recognizer!=null)recognizer.close();super.onDestroy();
    }
    @Override public android.os.IBinder onBind(Intent i){return null;}
}
