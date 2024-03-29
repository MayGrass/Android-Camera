package tw.org.iii.android_imagerecognition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.File;
import java.io.Serializable;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private ImageView imageView;
    private File sdroot;
    private SwitchCompat fswitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED ) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,},
                    123);

        } else {
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        init();
    }

    private CameraManager cmgr;
    private Vibrator vibrator;
    private void init(){
        cmgr = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        sdroot = Environment.getExternalStorageDirectory();
        imageView = findViewById(R.id.img);
        fswitch = findViewById(R.id.FLswitch);
        fswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    lightOn();
                }
                else {
                    lightOff();
                }
            }
        });

        // Read from the database
        myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                Bike bike2 = (Bike)dataSnapshot.getValue(Bike.class); // Using HashMap

                Log.v("DCH", bike.name+ ":" +bike.speed);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.v("DCH", "Failed to read value.", error.toException());
            }
        });
    }

    private void lightOn() {
        try {
            cmgr.setTorchMode("0", true);
        } catch (Exception e) {

        }
    }

    private void lightOff() {
        try {
            cmgr.setTorchMode("0", false);
        } catch (Exception e) {

        }

    }

    public void StartCamera(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, 1);
    }

    private Uri photoUri;
    public void test2(View view) {
        photoUri = FileProvider.getUriForFile(
                this,
                getPackageName() +".provider",
                new File(sdroot, "iii.jpg"));
//        Uri photoUri2 = Uri.fromFile(new File(sdroot, "iii2.jpg"));
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
        startActivityForResult(intent, 2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
//            Log.v("DCH", "OK");
            Bundle bundle = data.getExtras();
//            Set<String> keys = bundle.keySet();
//            for (String key : keys) {
//                Object value = bundle.get(key);
//                //反射類別
//                Log.v("DCH", key+ ":" +value.getClass().getName());
//            }
            //抓縮圖
            Bitmap bitmap = (Bitmap) bundle.get("data");
            imageView.setImageBitmap(bitmap);
        }
        //抓原圖
        else if (requestCode == 2 && requestCode == RESULT_OK) {
            Bitmap bitmap = BitmapFactory.decodeFile(sdroot.getAbsolutePath() + "/iii.jpg");
            imageView.setImageBitmap(bitmap);
//            if (photoUri != null) imageView.setImageURI(photoUri);
            FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
            FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
                    .setWidth(480)   // 480x360 is typically sufficient for
                    .setHeight(360)  // image recognition
                    .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                    .setRotation(FirebaseVisionImageMetadata.ROTATION_0)
                    .build();
            FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance()
                    .getOnDeviceTextRecognizer();
            Task<FirebaseVisionText> task = textRecognizer.processImage(image);
            task.addOnCompleteListener(new OnCompleteListener<FirebaseVisionText>() {
                @Override
                public void onComplete(@NonNull Task<FirebaseVisionText> task) {
                    FirebaseVisionText result = task.getResult();
                    String text = result.getText();
                    Log.v("DCH", text);

                }
            });
        }
    }

    public void test3(View view) {
        //分新版舊版的東西
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(1*1000, VibrationEffect.DEFAULT_AMPLITUDE));
        }
        else {
            vibrator.vibrate(1*1000);
        }
    }
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference myRef = database.getReference("message");
    private DatabaseReference myBike = database.getReference("bike");
    private Bike bike = new Bike();
    public void databasetest(View view) {
        // Write a message to the database
        myRef.setValue("Hello, World!");
        bike.setName("DCH");
        bike.upSpeed();bike.upSpeed();bike.upSpeed();bike.upSpeed();
        myBike.setValue(bike);
    }
}
