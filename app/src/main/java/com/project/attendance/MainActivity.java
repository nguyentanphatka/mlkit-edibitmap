package com.project.attendance;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.camerakit.CameraKitView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.project.attendance.Help.GraphicOverlay;
import com.project.attendance.Help.RectOverlay;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    CameraKitView cameraView;
    GraphicOverlay graphicOverlay;
    Button btnDetect;

    AlertDialog alertDialog;

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        cameraView.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        cameraView.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Init view
        cameraView = (CameraKitView)findViewById(R.id.camera_view);
        graphicOverlay = (GraphicOverlay)findViewById(R.id.graphic_overlay);
        btnDetect = (Button)findViewById(R.id.btn_detect);
        alertDialog = new AlertDialog.Builder(this)
                .setMessage("Please wait...")
                .setCancelable(false)
                .create();
        //event
        btnDetect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //cameraView.onStart();
                cameraView.captureImage(new CameraKitView.ImageCallback() {
                    @Override
                    public void onImage(CameraKitView cameraKitView, byte[] bytes) {
                        cameraView.onStop();

                        alertDialog.show();

                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes != null ? bytes.length : 0);
                        bitmap = Bitmap.createScaledBitmap(bitmap, cameraView != null ? cameraView.getWidth() : 0, cameraView != null ? cameraView.getHeight() : 0, false);
                        runDetector(bitmap);
                    }
                });
                graphicOverlay.clear();
            }
        });
    }

    private void runDetector(Bitmap bitmap){
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionFaceDetectorOptions options = new FirebaseVisionFaceDetectorOptions.Builder()
                .build();
        FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
                .getVisionFaceDetector(options);

        detector.detectInImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionFace> faces) {
                        processFaceResult(faces,bitmap);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    private void processFaceResult(List<FirebaseVisionFace> faces ,Bitmap images) {
        int count = 0;
        for(FirebaseVisionFace face: faces)
        {
            Rect bounds = face.getBoundingBox();
            //Draw rectangle
            RectOverlay rect = new RectOverlay(graphicOverlay, bounds);
            graphicOverlay.add(rect);
            // new version
            FaceGraphic faceGraphic = new FaceGraphic(graphicOverlay);
            graphicOverlay.add(faceGraphic);
            faceGraphic.updateFace(face, frameMetadata.getCameraFacing());
            croppedImage = cropBitmap(images, face.getBoundingBox());
            //save picture => view picture imageView.setImageBitmap(croppedImage); 
            //Bitmap images = imageView.getDrawingCache();  // Gets the Bitmap
            MediaStore.Images.Media.insertImage(getContentResolver(), croppedImage,"image"+ count ,"Image Description" imageDescription);  // Saves the image.
            count ++;
        }
        alertDialog.dismiss();
        Toast.makeText(this,String.format("Detected %d faces in image",count), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        cameraView.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
    
    // new version crop face from picture
    public static Bitmap cropBitmap(Bitmap bitmap, Rect rect) {
        int w = rect.right - rect.left;
        int h = rect.bottom - rect.top;
        Bitmap ret = Bitmap.createBitmap(w, h, bitmap.getConfig());
        Canvas canvas = new Canvas(ret);
        canvas.drawBitmap(bitmap, -rect.left, -rect.top, null);
        return ret;
    }
}