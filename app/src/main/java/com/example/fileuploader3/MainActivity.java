package com.example.fileuploader3;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_FILE_REQUEST = 1;
    private Uri fileUri;
    private TextView selectedFilePath;
    private Button uploadFileBtn;
    private EditText filePathInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button browseFileBtn = findViewById(R.id.browseFileBtn);
        selectedFilePath = findViewById(R.id.selectedFilePath);
        uploadFileBtn = findViewById(R.id.uploadFileBtn);
        filePathInput = findViewById(R.id.filePathInput);

        browseFileBtn.setOnClickListener(v -> openFilePicker());

        uploadFileBtn.setOnClickListener(v -> {
            String filePath = null;
            if (fileUri != null) {
                filePath = getPathFromUri(this, fileUri);
            } else if (filePathInput.getText() != null) {
                filePath = filePathInput.getText().toString().trim();
            }

            if (filePath != null && !filePath.isEmpty()) {
                File file = new File(filePath);
                if (file.exists()) {
                    try {

                        String signedUrl = "https://ninebit.s3.ap-south-1.amazonaws.com/540.jpg?Content-Type=image%2Fjpeg&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=AKIA3D7KSU77YZMWLTYC%2F20240522%2Fap-south-1%2Fs3%2Faws4_request&X-Amz-Date=20240522T100449Z&X-Amz-Expires=600&X-Amz-Signature=a951d742d6da418176e03c2275c356d6e00845a7059a895f6bc18b19fef07b3c&X-Amz-SignedHeaders=host";
                        uploadToS3(file, signedUrl);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.e("FileUpload", "File not found: " + filePath);
                }
            } else {
                Log.e("FileUpload", "No file selected or path input");
            }
        });

    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_FILE_REQUEST);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            fileUri = data.getData();
            String path = getPathFromUri(this, fileUri);
            if (path != null && !path.isEmpty()) {
                selectedFilePath.setText(path);
                uploadFileBtn.setEnabled(true);

                loadImageIntoImageView(path);
            } else {
                Log.e("FileUpload", "Failed to retrieve file path from URI: " + fileUri);
            }
        } else {
            Log.e("FileUpload", "Failed to pick a file. Request code: " + requestCode + ", Result code: " + resultCode);
        }
    }

    private void loadImageIntoImageView(String imagePath) {
        ImageView imageView = findViewById(R.id.img);
        Log.d("FileUpload", "Image Path: " + imagePath);
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            Log.e("FileUpload", "Failed to decode Bitmap from file: " + imagePath);
        }
    }



    private void uploadToS3(File file, String signedUrl) throws IOException {
        OkHttpClient client = new OkHttpClient();

        MediaType mediaType = MediaType.parse("image/jpeg");
        RequestBody requestBody = RequestBody.create(file, mediaType);

        Request request = new Request.Builder()
                .url(signedUrl)
                .put(requestBody)
                .addHeader("Content-Type", "image/jpeg")
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                Log.e("Upload", "ERROR", e);
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.i("Upload", "SUCCESS");
                } else {
                    Log.e("Upload", "ERROR: " + response.message());
                }
            }
        });
    }

    private String getPathFromUri(Context context, Uri uri) {
        String filePath = null;
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                File tempFile = File.createTempFile("temp_image", null, context.getCacheDir());
                OutputStream outputStream = new FileOutputStream(tempFile);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                filePath = tempFile.getAbsolutePath();
                inputStream.close();
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filePath;
    }


}
