//package com.k2fsa.sherpa.onnx;
//
//import android.app.Activity;
//import android.content.Context;
//import android.content.res.AssetManager;
//import android.media.AudioAttributes;
//import android.media.AudioFormat;
//import android.media.AudioManager;
//import android.media.AudioTrack;
//import android.media.MediaPlayer;
//import android.net.Uri;
//import android.util.Log;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//
////public class TtsHandler {
////    private final Context context;
////    private MainActivityCallback mainActivity;
////    private static final String TAG = "sherpa-onnx";
////
////    private OfflineTts tts;
//////    private EditText text;
//////    private EditText sid;
//////    private EditText speed;
//////    private Button generate;
//////    private Button play;
//////    private Button stop;
////    private String text;
////    private boolean stopped = false;
////    private MediaPlayer mediaPlayer;
////    private AudioTrack track;
////
////
////    public TtsHandler(TextView textView, Context context, MainActivityCallback mainActivity) {
////        this.context = context;
////        text = textView.getText().toString().trim();
////        Log.i(TAG, "Start to initialize TTS");
////        initTts();
////        Log.i(TAG, "Finish initializing TTS");
////
////        Log.i(TAG, "Start to initialize AudioTrack");
////        initAudioTrack();
////        Log.i(TAG, "Finish initializing AudioTrack");
////
////        this.mainActivity = mainActivity;
////    }
////
////    public void updateText(TextView textView){
////        text = textView.getText().toString().trim();
////    }
////
////    public void initAudioTrack() {
////        int sampleRate = tts.sampleRate();
////        int bufLength = AudioTrack.getMinBufferSize(
////                sampleRate,
////                AudioFormat.CHANNEL_OUT_MONO,
////                AudioFormat.ENCODING_PCM_FLOAT
////        );
////        Log.i(TAG, "sampleRate: " + sampleRate + ", buffLength: " + bufLength);
////
////        AudioAttributes attr = new AudioAttributes.Builder()
////                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
////                .setUsage(AudioAttributes.USAGE_MEDIA)
////                .build();
////
////        AudioFormat format = new AudioFormat.Builder()
////                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
////                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
////                .setSampleRate(sampleRate)
////                .build();
////
////        track = new AudioTrack(
////                attr, format, bufLength, AudioTrack.MODE_STREAM,
////                AudioManager.AUDIO_SESSION_ID_GENERATE
////        );
////        track.play();
////    }
////
////    // This function is called from C++
////    private int callback(float[] samples) {
////        if (!stopped) {
////            track.write(samples, 0, samples.length, AudioTrack.WRITE_BLOCKING);
////            return 1;
////        } else {
////            track.stop();
////            return 0;
////        }
////    }
////
////    private void onSentenceFinished() {
////        mainActivity.onTtsFinishCurrentSentence();
////    }
////
////    public void generate(String textStr) {
////        Integer sidInt = null;
////        /**
////         * sidInt应该是speaker id
////         */
////        try {
//////            sidInt = Integer.parseInt(sid.getText().toString());
////            sidInt = 0;
////        } catch (NumberFormatException e) {
////            // Ignore
////        }
////        if (sidInt == null || sidInt < 0) {
////            Toast.makeText(
////                    context.getApplicationContext(),
////                    "Please input a non-negative integer for speaker ID!",
////                    Toast.LENGTH_SHORT
////            ).show();
////            return;
////        }
////
////        Float speedFloat = null;
////        try {
//////            speedFloat = Float.parseFloat(speed.getText().toString());
////            speedFloat = 1.0f;
////        } catch (NumberFormatException e) {
////            // Ignore
////        }
////        if (speedFloat == null || speedFloat <= 0) {
////            Toast.makeText(
////                    context.getApplicationContext(),
////                    "Please input a positive number for speech speed!",
////                    Toast.LENGTH_SHORT
////            ).show();
////            return;
////        }
////
////
////        if (textStr.isEmpty()) {
////            Toast.makeText(context.getApplicationContext(), "Please input a non-empty text!", Toast.LENGTH_SHORT)
////                    .show();
////            return;
////        }
////
////        track.pause();
////        track.flush();
////        track.play();
////
////        /**
////         * set UI:
////         * play.setEnabled(false);
////         * generate.setEnabled(false);
////         */
////
////        stopped = false;
////        Integer finalSidInt = sidInt;
////        Float finalSpeedFloat = speedFloat;
////        new Thread(() -> {
////            GeneratedAudio audio = tts.generateWithCallback(
////                    textStr,
////                    finalSidInt,
////                    finalSpeedFloat,
////                    this::callback
////            );
////
////            String filename = context.getFilesDir().getAbsolutePath() + "/generated1.wav";
////            boolean ok = audio.getSamples().length > 0 && audio.save(filename);
////            if (ok) {
////                ((Activity) context).runOnUiThread(() -> {
////                    /**
////                     * set UI
////                     * play.setEnabled(true);
////                     * generate.setEnabled(true);
////                     */
////                    onSentenceFinished();
////                    track.stop();
////                });
////            }
////        }).start();
////    }
////
////    public void play() {
////        String filename = context.getFilesDir().getAbsolutePath() + "/generated.wav";
////        if (mediaPlayer != null) {
////            mediaPlayer.stop();
////        }
////        mediaPlayer = MediaPlayer.create(
////                context.getApplicationContext(),
////                Uri.fromFile(new File(filename))
////        );
////        if (mediaPlayer != null) {
////            mediaPlayer.start();
////        }
////    }
////
////    public void stop() {
////        stopped = true;
////        /**
////         * set UI
////         * play.setEnabled(true);
////         * generate.setEnabled(true);
////         */
////        track.pause();
////        track.flush();
////        if (mediaPlayer != null) {
////            mediaPlayer.stop();
////            mediaPlayer = null;
////        }
////    }
////
////    public void initTts() {
////        String modelDir = null;
////        String modelName = null;
////        String acousticModelName = null;
////        String vocoder = null;
////        String voices = null;
////        String ruleFsts = null;
////        String ruleFars = null;
////        String lexicon = null;
////        String dataDir = null;
////        String dictDir = null;
////        AssetManager assets = context.getAssets();
////
////        // Example configuration for VITS model
////        modelDir = "vits-melo-tts-zh_en";
////        modelName = "model.onnx";
////        lexicon = "lexicon.txt";
////        dictDir = "vits-melo-tts-zh_en/dict";
////
////        if (dataDir != null) {
////            String newDir = copyDataDir(dataDir);
////            dataDir = newDir + "/" + dataDir;
////        }
////
////        if (dictDir != null) {
////            String newDir = copyDataDir(dictDir);
////            dictDir = newDir + "/" + dictDir;
////            ruleFsts = modelDir + "/phone.fst," + modelDir + "/date.fst," + modelDir + "/number.fst";
////        }
////
////        OfflineTtsConfig config = getOfflineTtsConfig(
////                modelDir,
////                modelName != null ? modelName : "",
////                acousticModelName != null ? acousticModelName : "",
////                vocoder != null ? vocoder : "",
////                voices != null ? voices : "",
////                lexicon != null ? lexicon : "",
////                dataDir != null ? dataDir : "",
////                dictDir != null ? dictDir : "",
////                ruleFsts != null ? ruleFsts : "",
////                ruleFars != null ? ruleFars : ""
////        );
////
////        tts = new OfflineTts(assets, config);
////    }
////
////    private String copyDataDir(String dataDir) {
////        Log.i(TAG, "data dir is " + dataDir);
////        copyAssets(dataDir);
////
////        String newDataDir = context.getExternalFilesDir(null).getAbsolutePath();
////        Log.i(TAG, "newDataDir: " + newDataDir);
////        return newDataDir;
////    }
////
////    private void copyAssets(String path) {
////        String[] assets;
////        try {
////            assets = context.getAssets().list(path);
////            if (assets == null || assets.length == 0) {
////                copyFile(path);
////            } else {
////                String fullPath = context.getExternalFilesDir(null) + "/" + path;
////                File dir = new File(fullPath);
////                dir.mkdirs();
////                for (String asset : assets) {
////                    String p = path.isEmpty() ? "" : path + "/";
////                    copyAssets(p + asset);
////                }
////            }
////        } catch (IOException ex) {
////            Log.e(TAG, "Failed to copy " + path + ". " + ex);
////        }
////    }
////
////    private void copyFile(String filename) {
////        try {
////            java.io.InputStream istream = context.getAssets().open(filename);
////            String newFilename = context.getExternalFilesDir(null) + "/" + filename;
////            java.io.OutputStream ostream = new FileOutputStream(newFilename);
////            byte[] buffer = new byte[1024];
////            int read;
////            while ((read = istream.read(buffer)) != -1) {
////                ostream.write(buffer, 0, read);
////            }
////            istream.close();
////            ostream.flush();
////            ostream.close();
////        } catch (Exception ex) {
////            Log.e(TAG, "Failed to copy " + filename + ", " + ex);
////        }
////    }
////
////    // Dummy method for getOfflineTtsConfig
////    private OfflineTtsConfig getOfflineTtsConfig(
////            String modelDir, String modelName, String acousticModelName, String vocoder,
////            String voices, String lexicon, String dataDir, String dictDir,
////            String ruleFsts, String ruleFars
////    ) {
////        // Implement this method based on your actual logic
////        return new OfflineTtsConfig();
////    }
////}
//
//
//import android.app.Activity;
//import android.content.Context;
//import android.content.res.AssetManager;
//import android.media.AudioAttributes;
//import android.media.AudioFormat;
//import android.media.AudioManager;
//import android.media.AudioTrack;
//import android.media.MediaPlayer;
//import android.net.Uri;
//import android.util.Log;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//
//public class TtsHandler {
//    private final Context context;
//    private MainActivityCallback mainActivity;
//    private static final String TAG = "sherpa-onnx";
//
//    private OfflineTts tts;
//    private String text;
//    private boolean stopped = false;
//    private MediaPlayer mediaPlayer;
//    private AudioTrack track;
//
//    public TtsHandler(TextView textView, Context context, MainActivityCallback mainActivity) {
//        Log.d("donghuiDebug", "Initializing TTS Handler");
//
//        this.context = context;
//        this.mainActivity = mainActivity;
//        text = textView.getText().toString().trim();
//
//        try {
//            Log.d("donghuiDebug", "try 1");
//            initTts();
//            Log.d("donghuiDebug", "try 2");
//        } catch (Exception e) {
//            tts = null;  // 避免后续调用 tts.sampleRate() 时报错
//        }
//
//        try {
//            initAudioTrack();
//            Log.i(TAG, "AudioTrack initialized successfully");
//        } catch (Exception e) {
//            Log.e(TAG, "initAudioTrack() crashed: " + e.getMessage(), e);
//            track = null;
//        }
//    }
//
//    public void updateText(TextView textView) {
//        text = textView.getText().toString().trim();
//    }
//
//    public void initAudioTrack() {
//        if (tts == null) {
//            Log.e(TAG, "TTS is not initialized. Skipping AudioTrack initialization.");
//            return;
//        }
//
//        int sampleRate = tts.sampleRate();
//        int bufLength = AudioTrack.getMinBufferSize(
//                sampleRate,
//                AudioFormat.CHANNEL_OUT_MONO,
//                AudioFormat.ENCODING_PCM_FLOAT
//        );
//
//        if (bufLength == AudioTrack.ERROR_BAD_VALUE) {
//            Log.e(TAG, "Invalid buffer size for AudioTrack");
//            return;
//        }
//
//        AudioAttributes attr = new AudioAttributes.Builder()
//                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
//                .setUsage(AudioAttributes.USAGE_MEDIA)
//                .build();
//
//        AudioFormat format = new AudioFormat.Builder()
//                .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
//                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
//                .setSampleRate(sampleRate)
//                .build();
//
//        track = new AudioTrack(
//                attr, format, bufLength, AudioTrack.MODE_STREAM,
//                AudioManager.AUDIO_SESSION_ID_GENERATE
//        );
//
//        if (track.getState() != AudioTrack.STATE_INITIALIZED) {
//            Log.e(TAG, "AudioTrack initialization failed.");
//            track = null;
//            return;
//        }
//
//        track.play();
//    }
//
//    private int callback(float[] samples) {
//        if (track == null || track.getState() != AudioTrack.STATE_INITIALIZED) {
//            Log.e(TAG, "AudioTrack is not initialized!");
//            return 0;
//        }
//        if (!stopped) {
//            track.write(samples, 0, samples.length, AudioTrack.WRITE_BLOCKING);
//            return 1;
//        } else {
//            track.stop();
//            return 0;
//        }
//    }
//
//    private void onSentenceFinished() {
//        if (mainActivity != null) {
//            mainActivity.onTtsFinishCurrentSentence();
//        }
//    }
//
//    public void generate(String textStr) {
//        if (tts == null) {
//            Log.e(TAG, "TTS is not initialized. Cannot generate speech.");
//            return;
//        }
//
//        if (textStr.isEmpty()) {
//            Toast.makeText(context.getApplicationContext(), "Please input a non-empty text!", Toast.LENGTH_SHORT)
//                    .show();
//            return;
//        }
//
//        if (track == null) {
//            Log.e(TAG, "AudioTrack is not initialized. Cannot play audio.");
//            return;
//        }
//
//        track.pause();
//        track.flush();
//        track.play();
//
//        stopped = false;
//        new Thread(() -> {
//            GeneratedAudio audio = tts.generateWithCallback(
//                    textStr, 0, 1.0f, this::callback
//            );
//
//            String filename = context.getFilesDir().getAbsolutePath() + "/generated1.wav";
//            boolean ok = audio.getSamples().length > 0 && audio.save(filename);
//            if (ok) {
//                ((Activity) context).runOnUiThread(() -> {
//                    onSentenceFinished();
//                    track.stop();
//                });
//            }
//        }).start();
//    }
//
//    public void play() {
//        if (track == null) {
//            Log.e(TAG, "AudioTrack is not initialized.");
//            return;
//        }
//
//        String filename = context.getFilesDir().getAbsolutePath() + "/generated.wav";
//        if (mediaPlayer != null) {
//            mediaPlayer.stop();
//        }
//        mediaPlayer = MediaPlayer.create(
//                context.getApplicationContext(),
//                Uri.fromFile(new File(filename))
//        );
//        if (mediaPlayer != null) {
//            mediaPlayer.start();
//        }
//    }
//
//    public void stop() {
//        stopped = true;
//        if (track != null && track.getState() == AudioTrack.STATE_INITIALIZED) {
//            track.pause();
//            track.flush();
//        }
//        if (mediaPlayer != null) {
//            mediaPlayer.stop();
//            mediaPlayer = null;
//        }
//    }
//
//    public void initTts() {
//        try {
//            Log.d("donghuiDebug", "try init 1");
//
//            String modelDir = "vits-melo-tts-zh_en";
//            String modelName = "model.onnx";
//            String lexicon = "lexicon.txt";
//            String dictDir = "vits-melo-tts-zh_en/dict";
//
//            OfflineTtsConfig config = getOfflineTtsConfig(
//                    modelDir, modelName, "", "", "", lexicon, "", dictDir, "", ""
//            );
//            Log.d("donghuiDebug", "try init 2");
//
//            tts = new OfflineTts(context.getAssets(), config);
//            Log.d("donghuiDebug", "try init 3");
//            if (tts == null) {
//                throw new RuntimeException("TTS initialization failed!");
//            }
//
//            Log.i(TAG, "TTS initialized successfully.");
//
//        } catch (Exception e) {
//            Log.e(TAG, "initTts() crashed: " + e.getMessage(), e);
//            tts = null;
//        }
//    }
//
//    private OfflineTtsConfig getOfflineTtsConfig(
//            String modelDir, String modelName, String acousticModelName, String vocoder,
//            String voices, String lexicon, String dataDir, String dictDir,
//            String ruleFsts, String ruleFars
//    ) {
//        return new OfflineTtsConfig();
//    }
//}
