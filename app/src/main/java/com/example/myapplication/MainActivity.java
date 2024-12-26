package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.mozilla.universalchardet.UniversalDetector;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;


public class MainActivity extends AppCompatActivity {

    private static final int PICK_TXT_FILE = 1; // 请求代码
    private TextView txtContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnSelectFile = findViewById(R.id.btnSelectFile);
        txtContent = findViewById(R.id.txtContent);
        txtContent.setMovementMethod(new ScrollingMovementMethod());

        btnSelectFile.setOnClickListener(v -> openFilePicker());
    }

    // 打开文件选择器
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain"); // 限制为TXT文件
        startActivityForResult(Intent.createChooser(intent, "选择TXT文件"), PICK_TXT_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_TXT_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData(); // 获取用户选择的文件URI
            readTextFileWithEncoding(uri);
        }
    }

    // 读取文件内容
//    private void readTextFile(Uri uri) {
//        try {
//            InputStream inputStream = getContentResolver().openInputStream(uri);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
//            StringBuilder stringBuilder = new StringBuilder();
//            String line;
//
//            while ((line = reader.readLine()) != null) {
//                stringBuilder.append(line).append("\n");
//            }
//            inputStream.close();
//
//            // 显示文件内容
//            txtContent.setText(stringBuilder.toString());
//        } catch (Exception e) {
//            e.printStackTrace();
//            txtContent.setText("读取文件时出错！");
//        }
//    }


    private void readTextFileWithEncoding(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);

            // 检测编码
            byte[] buffer = new byte[4096];
            int nread;
            UniversalDetector detector = new UniversalDetector(null);

            while ((nread = inputStream.read(buffer)) > 0 && !detector.isDone()) {
                detector.handleData(buffer, 0, nread);
            }
            detector.dataEnd();

            String encoding = detector.getDetectedCharset(); // 检测到的编码
            inputStream.close();

            if (encoding == null) {
                encoding = "UTF-8"; // 默认回退编码
            }

            // 按检测到的编码读取文件
            inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, encoding));
            StringBuilder stringBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            inputStream.close();

            // 显示文件内容
            txtContent.setText(stringBuilder.toString());
        } catch (Exception e) {
            e.printStackTrace();
            txtContent.setText("读取文件时出错！");
        }
    }

}