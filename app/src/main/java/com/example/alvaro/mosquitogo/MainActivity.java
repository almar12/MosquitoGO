package com.example.alvaro.mosquitogo;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import java.lang.Math;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener{
    private static final String TAG = "AndroidCameraApi";
    //Variable Sensor
    MediaPlayer mp;


    private SensorManager sSensorManager;
    private Sensor sSensor;
    private Sensor sSensorG;
    private Sensor sSensorR;
    public float angle = 0;
    public float posR =0;
    public float ea=0;
   // public float ra=0;
    public boolean pv = true;

    private static final float NS2S = 1.0f / 1000000000.0f;
   // private final float[] deltaRotationVector = new float[4]();
    private float timestamp;


    private float[] mRotationMatrix = new float[16];

    private float[] mOrientation = new float[9];

    private float[] history = new float[2];

    private float mHeading;

    private float mPitch;

    int PosXMosq[];
    int PosYMosq[];
    ImageView mosquitos[] ;
    ImageView matamosq;

    //RelativeLayout RL;
    RelativeLayout.LayoutParams params[];
    RelativeLayout.LayoutParams paramsmm;
    private TextureView textureView;

    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;

    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;


    private static final int REQUEST_CAMERA_PERMISSION = 200;

    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Sensor
        sSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
       sSensor = sSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
       // sSensorG = sSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sSensorR = sSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        //TextureView
        setContentView(R.layout.activity_main);
        textureView = (TextureView) findViewById(R.id.texture);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
        //RL = (RelativeLayout) findViewById(R.id.RL);


        mosquitos = new ImageView[3];
        params = new RelativeLayout.LayoutParams[3];
        mosquitos[0] = (ImageView) findViewById(R.id.m1);
        mosquitos[1] = (ImageView) findViewById(R.id.m2);
        mosquitos[2] = (ImageView) findViewById(R.id.m3);
        matamosq = (ImageView) findViewById(R.id.mm);
        Random r = new Random();

        PosXMosq = new int[3];
        PosYMosq = new int[3];
        for(int i =0;i<3;i++)
        {
            PosXMosq[i]= r.nextInt(360);
            PosYMosq[i]= r.nextInt(50)+2;
            params[i] = new RelativeLayout.LayoutParams(100, 100);
        }

        paramsmm = new RelativeLayout.LayoutParams(300, 300);
        paramsmm.topMargin = 700;
        paramsmm.leftMargin = 428;
        matamosq.setLayoutParams(paramsmm);


        mp = MediaPlayer.create(this,R.raw.clap);
       //Drawable myDrawable = getResources().getDrawable(R.drawable.mosq,null);
        //imageview1.setImageResource(R.drawable.mosq);
        //params.leftMargin = 100; //pixeles de derecha a izquierda.
        //params.topMargin = 100; //pixeles de arriba a bajo.
        //RL.addView(imageview, params);

    }
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //This is called when the camera is open
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
            createCameraPreview();
        }
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
        }
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }
    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback(){
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(MainActivity.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Comprobacion de permisos para uso de la camara
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                return;
            }
            manager.openCamera(cameraId, stateCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }
    protected void updatePreview() {
        if(null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                // close the app
                Toast.makeText(MainActivity.this, "Sorry!!!, you can't use this app without granting permission", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        sSensorManager.registerListener(this, sSensor, SensorManager.SENSOR_DELAY_NORMAL);
      //  sSensorManager.registerListener(this, sSensorG, SensorManager.SENSOR_DELAY_GAME);
        sSensorManager.registerListener(this, sSensorR, SensorManager.SENSOR_DELAY_NORMAL);
        //sSensorManager.registerListener(this, sSensor, Sensor.TYPE_ACCELEROMETER);
        //sSensorManager.registerListener(this, sSensorG, Sensor.TYPE_GYROSCOPE);
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCamera();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }
    }
    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        sSensorManager.unregisterListener(this);
        //sSensorManager.unregisterListener(this, sSensorG);
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    public void onSensorChanged(SensorEvent event) {
        // TODO Auto-generated method stub



            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

               // float aX = event.values[0];
                float aY = event.values[1];
                float aZ = event.values[2];

                angle = 90-(int) (Math.atan2(aY, -aZ)/(Math.PI/180));

               // ((TextView) findViewById(R.id.elevacion)).setText("Elevacion(grados): " + Float.toString(angle));
                ((TextView) findViewById(R.id.elevacion)).setText(Integer.toString(PosXMosq[0])+";"+Integer.toString(PosYMosq[0])+".."+Integer.toString(PosXMosq[1])+";"+Integer.toString(PosYMosq[1])+".."+Integer.toString(PosXMosq[2])+";"+Integer.toString(PosYMosq[2]));

            }
            else{
                SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
                SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_X,
                        SensorManager.AXIS_Z, mRotationMatrix);
                SensorManager.getOrientation(mRotationMatrix, mOrientation);

                mHeading = (float) Math.toDegrees(mOrientation[0]);
                mPitch = (float) Math.toDegrees(mOrientation[1]);

                float xDelta = history[0] - mHeading;  // Currently unused
                float yDelta = history[1] - mPitch;

                history[0] = mHeading;
                history[1] = mPitch;



                // Make sure value is between 0-360
                mHeading = ((int)mHeading%(360.0f)+(360.0f))%(360.0f);
                //(a % b + b) % b
                posR = mHeading;
                ((TextView) findViewById(R.id.rotacion)).setText("Rotacion(grados): " + Float.toString(posR));


            }

       moverImage(posR,angle,0);
        moverImage(posR,angle,1);
        moverImage(posR,angle,2);
        //ea =angle;


    }

    public void moverImage(float r,float e,int n)
    {
        //Calculo poscion relativa en x sobre la pantalla
        //PosYMosq[0]= 20;
        //PosXMosq[0]=90;
        float x = 0;
        float dif;
        float xmin = r - 22;
        float xmax = r + 22;
        if (xmin < 0) {
            xmin += 360;
        }
        if (xmax > 360) {
            xmax -= 360;
        }
        if (PosXMosq[n] > xmin && PosXMosq[n] < xmax) {
            dif = PosXMosq[n] - xmin;
            if (dif < 0) {
                dif += 360;
            }

            x = ((dif) * textureView.getMeasuredWidth()) / 44;
        } else {
            x = textureView.getMeasuredWidth() + 100;
        }
        params[n].leftMargin = (int) x; //pixeles de derecha a izquierda.




        //Calculo poscion relativa en y sobre la pantalla
        //if(Math.abs(e-ea)>1) {
            float y = 0;
            //float dif;
            float ymin = e - 28;
            float ymax = e + 28;
            if (PosYMosq[n] > ymin && PosYMosq[n] < ymax) {
                y = ((ymax - PosYMosq[n]) * textureView.getMeasuredHeight()) / 56;
            } else {
                y = textureView.getMeasuredHeight() + 50;

            }

            params[n].topMargin = (int) y; //pixeles de arriba a bajo.
        //}






        mosquitos[n].setLayoutParams(params[n]);
    }

    public synchronized void MatarMosquito(View v)
    {
        mp.start();
        for(int i=0;i<3;i++)
        {
            if (Math.abs(PosXMosq[i]-posR) < 6 && Math.abs(PosYMosq[i]-angle) < 6)
            {
                mosquitos[i].setVisibility(View.INVISIBLE);

            }

        }
    }

}