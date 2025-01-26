package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.UtteranceProgressListener;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.mozilla.universalchardet.UniversalDetector;
import android.speech.tts.TextToSpeech;


public class MainActivity extends AppCompatActivity implements OnButtonClickListener {
    private TextToSpeech tts;
    private static final int PICK_TXT_FILE = 1; // 请求代码
    private TextView txtContent;
    ArrayList<TitleViewNovelRecorded> novelTitles;
    Adapter_TitleViewNovelRecorded adapter;
    private EditText textNovelTitleForInput;
    RecyclerView recyclerView;
    View pageNovel;
    TextView currentNovelTitle;

    private List<String> sentences;
    private int currentSentenceIndex = 0;

    private boolean readingStatus; //true for reading now, false for not reading

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        readingStatus = false;
        Button buttonReadFile = findViewById(R.id.buttonReadFile);
        Button buttonSelectAlreadyReadNovel = findViewById(R.id.buttonReadStoredNovel);
        pageNovel = findViewById(R.id.read_novel_page);
        recyclerView = findViewById(R.id.recycler_view);
        Button buttonNovelTitleForInput = findViewById(R.id.button_novel_title_for_input);
        View layoutNovelTitleForInput = findViewById(R.id.layout_novel_title_for_input);
        textNovelTitleForInput = findViewById(R.id.novel_title_for_input);
        currentNovelTitle = findViewById(R.id.current_novel_title);

        txtContent = findViewById(R.id.txtContent);
        txtContent.setMovementMethod(new ScrollingMovementMethod());

        buttonReadFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layoutNovelTitleForInput.setVisibility(View.VISIBLE);
            }
        });
        buttonNovelTitleForInput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
                layoutNovelTitleForInput.setVisibility(View.INVISIBLE);
            }
        });
//        buttonSelectionNovel1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                recyclerView.setVisibility(View.INVISIBLE);
//                pageNovel.setVisibility(View.VISIBLE);
//                selectFromAlreadyReadNovels((String) buttonSelectionNovel1.getText());
//            }
//        });
        buttonSelectAlreadyReadNovel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recyclerView.setVisibility(View.VISIBLE);
                pageNovel.setVisibility(View.INVISIBLE);
            }
        });

        Button buttonSpeak = findViewById(R.id.buttonSpeak);
        Button buttonPause = findViewById(R.id.buttonPause);

        // 点击按钮朗读文本
        buttonSpeak.setOnClickListener(v -> {
            reading();
        });

        buttonPause.setOnClickListener(v -> {
            if(readingStatus){
                tts.stop();
                readingStatus = false;
            }else{
                reading();
            }
        });

        txtContent.getViewTreeObserver().addOnScrollChangedListener(() -> {
            int scrollY = txtContent.getScrollY(); // 获取当前滚动位置
            String novelTitle = (String)currentNovelTitle.getText();
            Log.d("donghuiTitle", novelTitle);
            Log.d("donghuiTitle", ""+currentNovelTitle.getVisibility());
            saveScrollPosition(novelTitle, scrollY); // 保存滚动位置
        });

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.CHINESE);
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        Log.d("TTS", "Started speaking: " + utteranceId);
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        Log.d("TTS", "Finished speaking: " + utteranceId);
                        currentSentenceIndex++;
                        if (currentSentenceIndex < sentences.size()) {
                            tts.speak(sentences.get(currentSentenceIndex), TextToSpeech.QUEUE_FLUSH, null, "UtteranceID_" + currentSentenceIndex);
                        }
                    }

                    @Override
                    public void onError(String utteranceId) {
                        Log.e("TTS", "Error in speaking: " + utteranceId);
                    }
                });
            }
        });

        novelTitles = new ArrayList<>();
        setUpNovelTitles();
        adapter = new Adapter_TitleViewNovelRecorded(this, novelTitles, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void reading(){
        if (!sentences.isEmpty()) {
            tts.speak(sentences.get(currentSentenceIndex), TextToSpeech.QUEUE_FLUSH, null, "UtteranceID_" + currentSentenceIndex);
        }
        readingStatus = true;
    }

    @Override
    protected void onDestroy() {
        // 释放 TTS 资源
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    // 打开文件选择器
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain"); // 限制为TXT文件
        startActivityForResult(Intent.createChooser(intent, "选择TXT文件"), PICK_TXT_FILE);
    }

    public void alreadyInputNovelButtonClicker(String title){
        recyclerView.setVisibility(View.INVISIBLE);
        pageNovel.setVisibility(View.VISIBLE);
        selectFromAlreadyReadNovels(title);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_TXT_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData(); // 获取用户选择的文件URI
            String novelTitle = String.valueOf(textNovelTitleForInput.getText());
            readTextFileWithEncoding(uri, novelTitle);
//            showFileNameInputDialog(uri);
            // 更新小说标题
            setUpNovelTitles(); // 重新加载标题列表
            adapter.updateData(novelTitles);
        }
    }


    private List<String> splitTextIntoSentences(String text) {
        return Arrays.asList(text.split("(?<=[。！？])"));
    }
    private void updateReadingText(){
        sentences = splitTextIntoSentences(txtContent.getText().toString());
        currentSentenceIndex = 0;
    }

    protected void showFileNameInputDialog(Uri fileUri) {
        // 创建一个 EditText 让用户输入文件名
        EditText novelTitleForInput = new EditText(this);
        novelTitleForInput.setHint("请输入文件名（不含扩展名）");

        new AlertDialog.Builder(this)
                .setTitle("保存文件")
                .setMessage("请输入文件名")
                .setView(novelTitleForInput) // 添加输入框
                .setPositiveButton("保存", (dialog, which) -> {
                    String fileName = novelTitleForInput.getText().toString().trim();
                    if (!fileName.isEmpty()) {
                        // 保存文件到内部存储
                        readTextFileWithEncoding(fileUri, fileName);
                    } else {
                        Toast.makeText(this, "文件名不能为空", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
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

        Log.d("donghuiFile", "File stored at: " + getFilesDir().getAbsolutePath());
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
    private void readTextFileWithEncoding(Uri uri, String novelTitle) {
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

            String fileName = novelTitle+".txt"; // 自定义文件名
            saveFileToInternalStorage(fileName, stringBuilder.toString());


            // 显示文件内容
//            txtContent.setText(stringBuilder.toString());
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
            updateReadingText();
        }
    }

    /**
     * 把内部储存的都setup成novel titles
     */
    void setUpNovelTitles(){
        File internalDir = getFilesDir();

        if (internalDir != null && internalDir.isDirectory()) {
            // 遍历内部存储中的文件
            novelTitles.clear();
            for (File file : internalDir.listFiles()) {
                if (file.isFile() && file.getName().endsWith(".txt")) {
                    // 添加文件名（包含扩展名）
                    novelTitles.add(new TitleViewNovelRecorded(file.getName()));
                }
            }
        }
    }

    @Override
    public void onButtonClick(String fileName) {
        // 处理按钮点击事件
        Toast.makeText(this, "Button clicked for: " + fileName, Toast.LENGTH_SHORT).show();
        // 调用 MainActivity 的其他方法
        alreadyInputNovelButtonClicker(fileName);
        currentNovelTitle.setText(fileName);
        restoreScrollPosition(fileName, txtContent);
        currentNovelTitle.setVisibility(View.VISIBLE);
        Log.d("donghuiTitleNew", ""+currentNovelTitle.getVisibility());
    }

    private void saveScrollPosition(String fileName, int scrollY) {
        SharedPreferences preferences = getSharedPreferences("ReadingHistory", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(fileName+"scroll_position", scrollY); // 保存滚动位置
        editor.apply();
    }

    private void restoreScrollPosition(String fileName, TextView textView) {
        SharedPreferences preferences = getSharedPreferences("ReadingHistory", MODE_PRIVATE);
        int scrollY = preferences.getInt(fileName+"scroll_position", 0); // 默认位置为 0
        textView.post(() -> textView.scrollTo(0, scrollY)); // 滚动到指定位置
    }

}