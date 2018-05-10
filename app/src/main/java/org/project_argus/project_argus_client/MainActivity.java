package org.project_argus.project_argus_client;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import yuku.ambilwarna.AmbilWarnaDialog;

@SuppressWarnings("deprecation")
public class MainActivity extends AppCompatActivity {

    private PaintView paintView;

    private FrameLayout cameraView;

    private CameraPreview cameraPreview;

    private Camera camera;

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camera = getCameraInstance();
        paintView = (PaintView) findViewById(R.id.paintView);
        cameraView = (FrameLayout) findViewById(R.id.cameraView);
        cameraPreview = new CameraPreview(this, camera);
        cameraView.addView(cameraPreview);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        paintView.init(metrics);
        paintView.bringToFront();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.normal:
                paintView.normal();
                return true;
            case R.id.emboss:
                paintView.emboss();
                return true;
            case R.id.blur:
                paintView.blur();
                return true;
            case R.id.clear:
                paintView.clear();
                return true;
            case R.id.color:
                openColorPicker();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void openColorPicker() {
        AmbilWarnaDialog colorPicker = new AmbilWarnaDialog(this, PaintView.DEFAULT_COLOR,
                                            new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {

            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                paintView.setCurrentColor(color);
            }
        });
        colorPicker.show();
    }

    /** A safe way to get an instance of the Camera object. */
    public Camera getCameraInstance(){
        Camera camera = null;
        try {
            camera = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return camera; // returns null if camera is unavailable
    }

    public void saveDrawing(View view) {
        Bitmap bitmap  = paintView.getmBitmap();
        final String encodedImage = encodeToBase64(bitmap);
        new Thread(new Runnable(){
            @Override
            public void run() {
                sendDataToServer(encodedImage);
            }
        }).start();
    }

    private String encodeToBase64(Bitmap image) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        boolean result = image.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray,Base64.NO_WRAP);
    }

    private String sendDataToServer(String imageData) {
        String url = "http://192.168.0.103:8080/api/save-image";
        RequestBody body = RequestBody.create(JSON, createJson(imageData));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        OkHttpClient client = new OkHttpClient();
        System.out.println("running");
        try (Response response = client.newCall(request).execute()) {
            return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "FAIL";
    }

    private String createJson(String imageData) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode();
        result.put("userName", "admin");
        result.put("imageData", imageData);
        return result.toString();
    }


}
