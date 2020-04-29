package com.abanoubhanna.ocr.arabic;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.theartofdev.edmodo.cropper.CropImage;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.os.Environment;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    public static final int IMAGE_GALLERY_REQUEST = 10;
    public static final int CAMERA_REQUEST_CODE = 20;
    Bitmap bmp;
    ImageView ocrImage;
    FloatingActionButton fab;
    boolean isDone = false;
    TextView ps, pressOCR, resultTextView;
    String[] cameraPermission;
    String currentPhotoPath;
    Uri photoURI;
    private AdView adView;
    private InterstitialAd mInterstitialAd;
    OcrAsyncTask task;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        resultTextView = findViewById(R.id.resultTextView);
        resultTextView.setMovementMethod(new ScrollingMovementMethod());
        resultTextView.setTextIsSelectable(true);

        ocrImage = findViewById(R.id.ocrImage);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isDone){
                    justCopy();
                } else {
                    //make ad visible
                    if (isNetworkAvailable()) { adView.setVisibility(View.VISIBLE); }
                    runOCR();
                    deleteAllPhotos();
                }
            }
        });

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        loadAds();
        onSharedIntent();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadAds();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAds();
    }

    private void loadAds() {
        //banner
        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();

        //interstitial
        MobileAds.initialize(this, "ca-app-pub-4971969455307153~5598283529");
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-4971969455307153/9016275042");

        //if network available, load the ad
        if (isNetworkAvailable()) {
            adView.loadAd(adRequest);
            mInterstitialAd.loadAd(new AdRequest.Builder().build());

        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        String imageFileName = "OCRit_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onBackPressed() {
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        } else {
            super.onBackPressed();
        }
    }

    void copy2Clipboard(CharSequence text){
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("copy text", text);
        if (clipboard != null){
            clipboard.setPrimaryClip(clip);
        }
        Notify(getString(R.string.copied));
    }

    private void justCopy() {
        copy2Clipboard(resultTextView.getText().toString());
    }

    private void runOCR() {
        task = new OcrAsyncTask(MainActivity.this);
        task.execute(bmp);

        //OcrManager manager = new OcrManager();
        //manager.initAPI();
        //Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.test);
        //String result = manager.startRecognize(bmp);
        resultTextView.setText(getString(R.string.identifying));
        resultTextView.setVisibility(View.VISIBLE);
        //apply the copy function instead of OCR
        isDone = true;
        //change fab icon
        fab.setImageDrawable(
                //ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_copy)
                AppCompatResources.getDrawable(MainActivity.this, R.drawable.ic_copy)
        );
        pressOCR.setText(getString(R.string.copyHint));
    }

    private void Notify(String text) {
        Snackbar.make(findViewById(R.id.fab), text, Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.camera) {
            deleteAllPhotos();
            if (checkCameraPermission()){
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                // Ensure that there's a camera activity to handle the intent
                if (cameraIntent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        Notify(ex.getMessage());
                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        photoURI = FileProvider.getUriForFile(this,
                                "com.abanoubhanna.ocr.arabic.android.fileprovider",
                                photoFile);
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
                    }
                }
            } else {
                showDialogMsg();//if yes see the permission requests
            }
            return true;
        }else if(id == R.id.gallery){
            deleteAllPhotos();
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            File pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            String pictureDirectoryPath = pictureDirectory.getPath();
            Uri data = Uri.parse(pictureDirectoryPath);
            photoPickerIntent.setDataAndType(data,"image/*");
            startActivityForResult(photoPickerIntent, IMAGE_GALLERY_REQUEST);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showDialogMsg() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage(R.string.permissionHint)
                .setPositiveButton("موافق", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User agree to accept permissions, show them permissions requests
                        requestCameraPermission();
                        //re-call the camera button
                        findViewById(R.id.camera).performClick();
                    }
                })
                .setNegativeButton("لا", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // No -> so user can not use camera to take picture of papers
                        final AlertDialog.Builder notifBuilder = new AlertDialog.Builder(MainActivity.this);
                        notifBuilder.setMessage("لن تستطيع استخدام الكاميرا لتصوير الورق والمستندات لأنك لم تعطي التطبيق الأذون والتصريحات اللازمة")
                                .setPositiveButton("تم", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        //do nothing
                                    }
                                }).show();
                    }
                }).show();
    }

    private void requestCameraPermission() {
        cameraPermission = new String[]{Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE};
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE);
    }
    private boolean checkCameraPermission(){
        boolean result0 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);
        return result0 && result1;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_GALLERY_REQUEST && data != null) {
                Uri imageUri = data.getData();
                CropImage.activity(imageUri).start(this);

            } else if (requestCode == CAMERA_REQUEST_CODE) {
                Uri camUri = Uri.fromFile(new File(currentPhotoPath));
                //Uri camUri = Uri.parse(new File(currentPhotoPath).toString());
                CropImage.activity(camUri).start(this);

                //Bitmap cameraImage = (Bitmap) data.getExtras().get("data");
                //Uri camUri = getImageUri(this, cameraImage);
                //CropImage.activity(camUri).start(this);

            } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (result != null) {
                    bmp = uriToBitmap(result.getUri());
                    hideWelcomeScreen();
                    showOCRView();
                    ocrImage.setImageBitmap(bmp);
                } else {
                    Notify("Error: Result is NULL");
                }
                isDone = false;
                //change fab icon
                fab.setImageDrawable(
                        //ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_ocr)
                        AppCompatResources.getDrawable(MainActivity.this, R.drawable.ic_ocr)
                );
                pressOCR.setText(R.string.tap_ocr_btn);
            }
        }
    }

    private void showOCRView() {
        ocrImage.setVisibility(View.VISIBLE);

        //fab.setVisibility(View.VISIBLE);
        fab.show();

        ImageView arrow2ocrbtn = findViewById(R.id.arrow2ocrbtn);
        arrow2ocrbtn.setVisibility(View.VISIBLE);

        pressOCR = findViewById(R.id.pressOcr);
        pressOCR.setVisibility(View.VISIBLE);
    }

    private void hideWelcomeScreen() {
        TextView hello = findViewById(R.id.hello);
        hello.setVisibility(View.GONE);

        TextView cameraHint = findViewById(R.id.camera_hint);
        cameraHint.setVisibility(View.GONE);

        TextView galleryHint = findViewById(R.id.gallery_hint);
        galleryHint.setVisibility(View.GONE);

        ps = findViewById(R.id.ps);
        ps.setVisibility(View.GONE);

        ImageView cameraArrow = findViewById(R.id.cameraArrow);
        cameraArrow.setVisibility(View.GONE);

        ImageView galleryArrow = findViewById(R.id.galleryArrow);
        galleryArrow.setVisibility(View.GONE);
    }

    private Bitmap uriToBitmap(Uri uri){
        InputStream inputStream = null;
        try {
            inputStream = getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return BitmapFactory.decodeStream(inputStream);
    }

//    public Uri getImageUri(Context inContext, Bitmap inImage) {
//        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
//        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
//        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
//        return Uri.parse(path);
//    }

    @Override
    protected void onDestroy() {
        deleteAllPhotos();
        if( task != null && task.getStatus() == AsyncTask.Status.RUNNING){
            task.cancel(true);
        }
        super.onDestroy();
    }

    private void deleteAllPhotos(){
        if (photoURI != null) {
            this.getContentResolver().delete(photoURI, null, null);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null){
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

//    @Override
//    protected void onStop() {
//        super.onStop();
//        MainActivity.this.finish();
//    }

    //background OCR task
    private static class OcrAsyncTask extends AsyncTask<Bitmap, Integer, String> {
        private WeakReference<MainActivity> activityWeakReference;

        OcrAsyncTask(MainActivity activity) {
            activityWeakReference = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //MainActivity activity = activityWeakReference.get();
            //if (activity == null || activity.isFinishing()) {
                //return;
            //}
        }

        @Override
        protected String doInBackground(Bitmap... bitmaps) {
            OcrManager manager = new OcrManager();
            manager.initAPI();
            return manager.startRecognize(bitmaps[0]);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            MainActivity activity = activityWeakReference.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            activity.resultTextView.setText(s);
        }
    }

    private void onSharedIntent() {
        Intent receivedIntent = getIntent();
        String receivedAction = receivedIntent.getAction();
        String receivedType = receivedIntent.getType();

        if (receivedAction != null && receivedAction.equals(Intent.ACTION_SEND) && receivedType != null && receivedType.startsWith("image/")) {
            Uri receiveUri = (Uri) receivedIntent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (receiveUri != null) {
                CropImage.activity(receiveUri).start(this);
                //Log.e(TAG,receiveUri.toString());
            }
        }
//        else if (receivedAction != null && receivedAction.equals(Intent.ACTION_MAIN)) {
//            Notify("Nothing shared");
//            //Log.e(TAG, "onSharedIntent: nothing shared" );
//        }
    }
}