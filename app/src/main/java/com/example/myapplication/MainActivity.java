package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.mozilla.universalchardet.UniversalDetector;


public class MainActivity extends AppCompatActivity {

    private static final int PICK_TXT_FILE = 1; // 请求代码
    private TextView txtContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button buttonReadFile = findViewById(R.id.buttonReadFile);
        Button buttonSelectAlreadyReadNovel = findViewById(R.id.buttonReadStoredNovel);
        ScrollView alreadyReadNovelSelectionList = findViewById(R.id.read_novel_selection_list);
        Button buttonSelectionNovel1 = findViewById(R.id.button_test);

        txtContent = findViewById(R.id.txtContent);
        txtContent.setMovementMethod(new ScrollingMovementMethod());

        buttonReadFile.setOnClickListener(v -> openFilePicker());
        buttonSelectAlreadyReadNovel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFromAlreadyReadNovels((String) buttonSelectionNovel1.getText());
            }
        });
        buttonSelectAlreadyReadNovel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alreadyReadNovelSelectionList.setVisibility(View.VISIBLE);
            }
        });
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

    /**
     * 录入文本至app内文件夹
     * @param fileName
     * @param content
     */
    private void saveFileToInternalStorage(String fileName, String content) {
        try {
            FileOutputStream fos = openFileOutput(fileName, MODE_PRIVATE);
            fos.write(content.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取已录入的文件
     */
    private String loadFileFromInternalStorage(String fileName) {
        try {
            FileInputStream fis = openFileInput(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder stringBuilder = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            fis.close();
            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * 读取文本内容
     * @param uri
     */
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

            String fileName = "mybook1.txt"; // 自定义文件名
            saveFileToInternalStorage(fileName, stringBuilder.toString());


            // 显示文件内容
            txtContent.setText(stringBuilder.toString());
        } catch (Exception e) {
            e.printStackTrace();
            txtContent.setText("读取文件时出错！");
        }
    }

    /**
     * 包装一下
     */
    public void selectFromAlreadyReadNovels(String fileName){
        String savedContent = loadFileFromInternalStorage(fileName);
        if (savedContent != null) {
            txtContent.setText(savedContent);
        }
    }

}