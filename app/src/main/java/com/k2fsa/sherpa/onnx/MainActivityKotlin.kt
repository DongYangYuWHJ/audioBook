package com.k2fsa.sherpa.onnx

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.universalchardet.UniversalDetector
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.Arrays

const val TAG = "sherpa-onnx"



class MainActivityKotlin : AppCompatActivity(), MainActivityCallback {
    private lateinit var tts: OfflineTts
//    private lateinit var text: EditText
//    private lateinit var sid: EditText
//    private lateinit var speed: EditText
//    private lateinit var generate: Button
//    private lateinit var play: Button
//    private lateinit var stop: Button
    private var stopped: Boolean = false
    private var mediaPlayer: MediaPlayer? = null


    lateinit private var txtContent: TextView
    lateinit private var touchHandler: TextTouchHandler
    lateinit var novelTitles: ArrayList<TitleViewNovelRecorded>
    lateinit var adapter: Adapter_TitleViewNovelRecorded
    lateinit private var textNovelTitleForInput: EditText
    lateinit var recyclerView: RecyclerView
    lateinit var pageNovel: View
    lateinit var currentNovelTitle: TextView

    val splitRegex: String = "[。！？,. \n]"
    private val PICK_TXT_FILE = 1 // 请求代码
    private var sentences: List<String>? = null
    var currentSentenceIndex: Int = 0
    private var readingStatus = false //true for reading now, false for not reading

    // see
    // https://developer.android.com/reference/kotlin/android/media/AudioTrack
    private lateinit var track: AudioTrack


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.i(TAG, "Start to initialize TTS")
        initTts()
        Log.i(TAG, "Finish initializing TTS")

        Log.i(TAG, "Start to initialize AudioTrack")
        initAudioTrack()
        Log.i(TAG, "Finish initializing AudioTrack")

        readingStatus = false
        val buttonReadFile = findViewById<Button>(R.id.buttonReadFile)
        val buttonSelectAlreadyReadNovel = findViewById<Button>(R.id.buttonReadStoredNovel)
        pageNovel = findViewById(R.id.read_novel_page)
        recyclerView = findViewById(R.id.recycler_view)
        val buttonNovelTitleForInput = findViewById<Button>(R.id.button_novel_title_for_input)
        val layoutNovelTitleForInput = findViewById<View>(R.id.layout_novel_title_for_input)
        textNovelTitleForInput = findViewById(R.id.novel_title_for_input)
        currentNovelTitle = findViewById(R.id.current_novel_title)

        txtContent = findViewById(R.id.txtContent)
        txtContent.setMovementMethod(ScrollingMovementMethod())

        touchHandler = TextTouchHandler(this, txtContent, sentences)
        touchHandler!!.attach()

        buttonReadFile.setOnClickListener {
            layoutNovelTitleForInput.visibility = View.VISIBLE
        }
        buttonNovelTitleForInput.setOnClickListener {
            openFilePicker()
            layoutNovelTitleForInput.visibility = View.INVISIBLE
        }
        buttonSelectAlreadyReadNovel.setOnClickListener {
            recyclerView.setVisibility(View.VISIBLE)
            pageNovel.setVisibility(View.INVISIBLE)
        }

        val buttonPause = findViewById<Button>(R.id.buttonPause)

        buttonPause.setOnClickListener { v: View? ->
            if (readingStatus) {
                onClickStop()
                readingStatus = false
            } else {
                reading()
            }
        }

        txtContent.getViewTreeObserver().addOnScrollChangedListener {
            val scrollY = txtContent.getScrollY() // 获取当前滚动位置
            val novelTitle = currentNovelTitle.getText() as String
            saveScrollPosition(novelTitle, scrollY) // 保存滚动位置
            Log.d("donghuiScroll", "check")
        }
        novelTitles = ArrayList()
        setUpNovelTitles()
        adapter = Adapter_TitleViewNovelRecorded(this, novelTitles, this)
        recyclerView.setAdapter(adapter)
        recyclerView.setLayoutManager(LinearLayoutManager(this))
    }

    private fun initAudioTrack() {
        val sampleRate = tts.sampleRate()
        val bufLength = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        Log.i(TAG, "sampleRate: $sampleRate, buffLength: $bufLength")

        val attr = AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .build()

        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .setSampleRate(sampleRate)
            .build()

        track = AudioTrack(
            attr, format, bufLength, AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        track.play()
    }

    // this function is called from C++
    private fun callback(samples: FloatArray): Int {
        if (!stopped) {
            track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            return 1
        } else {
            track.stop()
            return 0
        }
    }

    private fun onClickGenerate() {
        val sidInt = 0//sid.text.toString().toIntOrNull()
        if (sidInt == null || sidInt < 0) {
            Toast.makeText(
                applicationContext,
                "Please input a non-negative integer for speaker ID!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

//        val speedFloat = speed.text.toString().toFloatOrNull()
        val speedFloat = 1.0f
        if (speedFloat == null || speedFloat <= 0) {
            Toast.makeText(
                applicationContext,
                "Please input a positive number for speech speed!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val textStr = txtContent.text.toString().trim()
        if (textStr.isBlank() || textStr.isEmpty()) {
            Toast.makeText(applicationContext, "Please input a non-empty text!", Toast.LENGTH_SHORT)
                .show()
            return
        }

        track.pause()
        track.flush()
        track.play()

//        play.isEnabled = false
//        generate.isEnabled = false
        stopped = false
        Thread {
            val audio = tts.generateWithCallback(
                text = textStr,
                sid = sidInt,
                speed = speedFloat,
                callback = this::callback
            )

            val filename = application.filesDir.absolutePath + "/generated.wav"
            val ok = audio.samples.size > 0 && audio.save(filename)
            if (ok) {
                runOnUiThread {
//                    play.isEnabled = true
//                    generate.isEnabled = true
                    track.stop()
                }
            }
        }.start()
    }

    private fun onClickPlay() {
        val filename = application.filesDir.absolutePath + "/generated.wav"
        mediaPlayer?.stop()
        mediaPlayer = MediaPlayer.create(
            applicationContext,
            Uri.fromFile(File(filename))
        )
        mediaPlayer?.start()
    }

    private fun onClickStop() {
        stopped = true
//        play.isEnabled = true
//        generate.isEnabled = true
        track.pause()
        track.flush()
        mediaPlayer?.stop()
        mediaPlayer = null
    }

    private fun initTts() {
        var modelDir: String?
        var modelName: String?
        var acousticModelName: String?
        var vocoder: String?
        var voices: String?
        var ruleFsts: String?
        var ruleFars: String?
        var lexicon: String?
        var dataDir: String?
        var dictDir: String?
        var assets: AssetManager? = application.assets

        // The purpose of such a design is to make the CI test easier
        // Please see
        // https://github.com/k2-fsa/sherpa-onnx/blob/master/scripts/apk/generate-tts-apk-script.py

        // VITS -- begin
        modelName = null
        // VITS -- end

        // Matcha -- begin
        acousticModelName = null
        vocoder = null
        // Matcha -- end

        // For Kokoro -- begin
        voices = null
        // For Kokoro -- end


        modelDir = null
        ruleFsts = null
        ruleFars = null
        lexicon = null
        dataDir = null
        dictDir = null

        // Example 1:
        // modelDir = "vits-vctk"
        // modelName = "vits-vctk.onnx"
        // lexicon = "lexicon.txt"

        // Example 2:
        // https://github.com/k2-fsa/sherpa-onnx/releases/tag/tts-models
        // https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-en_US-amy-low.tar.bz2
        // modelDir = "vits-piper-en_US-amy-low"
        // modelName = "en_US-amy-low.onnx"
        // dataDir = "vits-piper-en_US-amy-low/espeak-ng-data"

        // Example 3:
        // https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-icefall-zh-aishell3.tar.bz2
        // modelDir = "vits-icefall-zh-aishell3"
        // modelName = "model.onnx"
        // ruleFars = "vits-icefall-zh-aishell3/rule.far"
        // lexicon = "lexicon.txt"

        // Example 4:
        // https://k2-fsa.github.io/sherpa/onnx/tts/pretrained_models/vits.html#csukuangfj-vits-zh-hf-fanchen-c-chinese-187-speakers
        // modelDir = "vits-zh-hf-fanchen-C"
        // modelName = "vits-zh-hf-fanchen-C.onnx"
        // lexicon = "lexicon.txt"
        // dictDir = "vits-zh-hf-fanchen-C/dict"

        // Example 5:
        // https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-coqui-de-css10.tar.bz2
        // modelDir = "vits-coqui-de-css10"
        // modelName = "model.onnx"

        // Example 6
        // vits-melo-tts-zh_en
        // https://k2-fsa.github.io/sherpa/onnx/tts/pretrained_models/vits.html#vits-melo-tts-zh-en-chinese-english-1-speaker
        modelDir = "vits-melo-tts-zh_en"
        modelName = "model.onnx"
        lexicon = "lexicon.txt"
        dictDir = "vits-melo-tts-zh_en/dict"

        // Example 7
        // matcha-icefall-zh-baker
        // https://k2-fsa.github.io/sherpa/onnx/tts/pretrained_models/matcha.html#matcha-icefall-zh-baker-chinese-1-female-speaker
        // modelDir = "matcha-icefall-zh-baker"
        // acousticModelName = "model-steps-3.onnx"
        // vocoder = "hifigan_v2.onnx"
        // lexicon = "lexicon.txt"
        // dictDir = "matcha-icefall-zh-baker/dict"

        // Example 8
        // matcha-icefall-en_US-ljspeech
        // https://k2-fsa.github.io/sherpa/onnx/tts/pretrained_models/matcha.html#matcha-icefall-en-us-ljspeech-american-english-1-female-speaker
        // modelDir = "matcha-icefall-en_US-ljspeech"
        // acousticModelName = "model-steps-3.onnx"
        // vocoder = "hifigan_v2.onnx"
        // dataDir = "matcha-icefall-en_US-ljspeech/espeak-ng-data"

        // Example 9
        // kokoro-en-v0_19
        // modelDir = "kokoro-en-v0_19"
        // modelName = "model.onnx"
        // voices = "voices.bin"
        // dataDir = "kokoro-en-v0_19/espeak-ng-data"

        // Example 10
        // kokoro-multi-lang-v1_0
        // modelDir = "kokoro-multi-lang-v1_0"
        // modelName = "model.onnx"
        // voices = "voices.bin"
        // dataDir = "kokoro-multi-lang-v1_0/espeak-ng-data"
        // dictDir = "kokoro-multi-lang-v1_0/dict"
        // lexicon = "kokoro-multi-lang-v1_0/lexicon-us-en.txt,kokoro-multi-lang-v1_0/lexicon-zh.txt"
        // ruleFsts = "$modelDir/phone-zh.fst,$modelDir/date-zh.fst,$modelDir/number-zh.fst"

        if (dataDir != null) {
            val newDir = copyDataDir(dataDir!!)
            dataDir = "$newDir/$dataDir"
        }

        if (dictDir != null) {
            val newDir = copyDataDir(dictDir!!)
            dictDir = "$newDir/$dictDir"
            if (ruleFsts == null) {
                ruleFsts = "$modelDir/phone.fst,$modelDir/date.fst,$modelDir/number.fst"
            }
        }

        val config = getOfflineTtsConfig(
            modelDir = modelDir!!,
            modelName = modelName ?: "",
            acousticModelName = acousticModelName ?: "",
            vocoder = vocoder ?: "",
            voices = voices ?: "",
            lexicon = lexicon ?: "",
            dataDir = dataDir ?: "",
            dictDir = dictDir ?: "",
            ruleFsts = ruleFsts ?: "",
            ruleFars = ruleFars ?: "",
        )!!

        tts = OfflineTts(assetManager = assets, config = config)
    }


    private fun copyDataDir(dataDir: String): String {
        Log.i(TAG, "data dir is $dataDir")
        copyAssets(dataDir)

        val newDataDir = application.getExternalFilesDir(null)!!.absolutePath
        Log.i(TAG, "newDataDir: $newDataDir")
        return newDataDir
    }

    private fun copyAssets(path: String) {
        val assets: Array<String>?
        try {
            assets = application.assets.list(path)
            if (assets!!.isEmpty()) {
                copyFile(path)
            } else {
                val fullPath = "${application.getExternalFilesDir(null)}/$path"
                val dir = File(fullPath)
                dir.mkdirs()
                for (asset in assets.iterator()) {
                    val p: String = if (path == "") "" else path + "/"
                    copyAssets(p + asset)
                }
            }
        } catch (ex: IOException) {
            Log.e(TAG, "Failed to copy $path. $ex")
        }
    }

    private fun copyFile(filename: String) {
        try {
            val istream = application.assets.open(filename)
            val newFilename = application.getExternalFilesDir(null).toString() + "/" + filename
            val ostream = FileOutputStream(newFilename)
            // Log.i(TAG, "Copying $filename to $newFilename")
            val buffer = ByteArray(1024)
            var read = 0
            while (read != -1) {
                ostream.write(buffer, 0, read)
                read = istream.read(buffer)
            }
            istream.close()
            ostream.flush()
            ostream.close()
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to copy $filename, $ex")
        }
    }

    override fun onDestroy() {
        // 释放 TTS 资源
        if (tts != null) {
            onClickStop()
        }
        super.onDestroy()
    }

    // 打开文件选择器
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setType("text/plain") // 限制为TXT文件
        startActivityForResult(Intent.createChooser(intent, "选择TXT文件"), PICK_TXT_FILE)
    }

    fun alreadyInputNovelButtonClicker(title: String?) {
        recyclerView!!.visibility = View.INVISIBLE
        pageNovel!!.visibility = View.VISIBLE
        selectFromAlreadyReadNovels(title)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_TXT_FILE && resultCode == RESULT_OK && data != null) {
            val uri = data.data // 获取用户选择的文件URI
            val novelTitle = textNovelTitleForInput!!.text.toString()
            readTextFileWithEncoding(uri!!, novelTitle)
            //            showFileNameInputDialog(uri);
            // 更新小说标题
            setUpNovelTitles() // 重新加载标题列表
            adapter!!.updateData(novelTitles)
        }
    }

    private fun splitTextIntoSentences(text: String): List<String> {
        return Arrays.asList(
            *text.split(("(?<=)" + splitRegex + "").toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray())
    }

    private fun updateReadingText() {
        sentences = splitTextIntoSentences(txtContent!!.text.toString())
        currentSentenceIndex = 0
        touchHandler!!.updateSentences(sentences)
    }

    protected fun showFileNameInputDialog(fileUri: Uri) {
        // 创建一个 EditText 让用户输入文件名
        val novelTitleForInput = EditText(this)
        novelTitleForInput.hint = "请输入文件名（不含扩展名）"

        AlertDialog.Builder(this)
            .setTitle("保存文件")
            .setMessage("请输入文件名")
            .setView(novelTitleForInput) // 添加输入框
            .setPositiveButton("保存") { dialog: DialogInterface?, which: Int ->
                val fileName = novelTitleForInput.text.toString().trim { it <= ' ' }
                if (!fileName.isEmpty()) {
                    // 保存文件到内部存储
                    readTextFileWithEncoding(fileUri, fileName)
                } else {
                    Toast.makeText(this, "文件名不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 录入文本至app内文件夹
     * @param fileName
     * @param content
     */
    private fun saveFileToInternalStorage(fileName: String, content: String) {
        try {
            val fos = openFileOutput(fileName, MODE_PRIVATE)
            fos.write(content.toByteArray())
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        Log.d("donghuiFile", "File stored at: " + filesDir.absolutePath)
    }

    /**
     * 读取已录入的文件
     */
    private fun loadFileFromInternalStorage(fileName: String?): String? {
        try {
            val fis = openFileInput(fileName)
            val reader = BufferedReader(InputStreamReader(fis))
            val stringBuilder = StringBuilder()
            var line: String?

            while ((reader.readLine().also { line = it }) != null) {
                stringBuilder.append(line).append("\n")
            }
            fis.close()
            return stringBuilder.toString()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * 读取文本内容
     * @param uri
     */
    private fun readTextFileWithEncoding(uri: Uri, novelTitle: String) {
        try {
            var inputStream = contentResolver.openInputStream(uri)

            // 检测编码
            val buffer = ByteArray(4096)
            var nread: Int
            val detector = UniversalDetector(null)

            while ((inputStream!!.read(buffer).also { nread = it }) > 0 && !detector.isDone) {
                detector.handleData(buffer, 0, nread)
            }
            detector.dataEnd()

            var encoding = detector.detectedCharset // 检测到的编码
            inputStream.close()

            if (encoding == null) {
                encoding = "UTF-8" // 默认回退编码
            }

            // 按检测到的编码读取文件
            inputStream = contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream, encoding))
            val stringBuilder = StringBuilder()
            var line: String?

            while ((reader.readLine().also { line = it }) != null) {
                stringBuilder.append(line).append("\n")
            }
            inputStream!!.close()

            val fileName = "$novelTitle.txt" // 自定义文件名
            saveFileToInternalStorage(fileName, stringBuilder.toString())


            // 显示文件内容
//            txtContent.setText(stringBuilder.toString());
        } catch (e: Exception) {
            e.printStackTrace()
            txtContent!!.text = "读取文件时出错！"
        }
    }

    /**
     * 包装一下
     */
    fun selectFromAlreadyReadNovels(fileName: String?) {
        val savedContent = loadFileFromInternalStorage(fileName)
        if (savedContent != null) {
            txtContent!!.text = savedContent
            updateReadingText()
        }
    }

    /**
     * 把内部储存的都setup成novel titles
     */
    fun setUpNovelTitles() {
        val internalDir = filesDir

        if (internalDir != null && internalDir.isDirectory) {
            // 遍历内部存储中的文件
            novelTitles!!.clear()
            for (file in internalDir.listFiles()) {
                if (file.isFile && file.name.endsWith(".txt")) {
                    // 添加文件名（包含扩展名）
                    novelTitles!!.add(TitleViewNovelRecorded(file.name))
                }
            }
        }
    }

    override fun onButtonClick(fileName: String) {
        // 处理按钮点击事件
        Toast.makeText(this, "Button clicked for: $fileName", Toast.LENGTH_SHORT).show()
        // 调用 MainActivity 的其他方法
        alreadyInputNovelButtonClicker(fileName)
        currentNovelTitle!!.text = fileName
        restoreScrollPosition(fileName, txtContent!!)
        restoreAudioRelated(fileName)
        currentNovelTitle!!.visibility = View.VISIBLE
        Log.d("donghuiTitleNew", "" + currentNovelTitle!!.visibility)
    }

    override fun onTtsFinishCurrentSentence() {
//        runOnUiThread {
//            Log.d("TTS", "Finished speaking: ")
//            currentSentenceIndex++
//            if (currentSentenceIndex < sentences!!.size) {
//                while (sentences!![currentSentenceIndex].length == 0) {
//                    currentSentenceIndex++
//                    //有些sentence是空的，不知道为什么，之后有时间可以看看，现在先跳过
//                    Log.d(
//                        "donghuiSpanSkip",
//                        "we are skipping " + currentSentenceIndex
//                    )
//                }
//
//                val novelTitle = currentNovelTitle!!.text as String
//                saveAudioIndex(novelTitle)
//
//                Companion.tts!!.generate(
//                    sentences!![currentSentenceIndex]
//                )
//                touchHandler!!.highlightSentence(currentSentenceIndex)
//                val newScrollY = touchHandler!!.newYScroll
//                //todo: 之后加个动画，现在一顿一顿的，太卡了
//                txtContent!!.scrollTo(0, newScrollY - txtContent!!.height / 2)
//            }
//        }
    }

    private val scrollPosition = "scroll_position"
    private fun saveScrollPosition(fileName: String, scrollY: Int) {
        val preferences = getSharedPreferences("ReadingHistory", MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putInt(fileName + scrollPosition, scrollY) // 保存滚动位置
        editor.apply()
    }

    private fun restoreScrollPosition(fileName: String, textView: TextView) {
        val preferences = getSharedPreferences("ReadingHistory", MODE_PRIVATE)
        val scrollY = preferences.getInt(fileName + scrollPosition, 0) // 默认位置为 0
        textView.post { textView.scrollTo(0, scrollY) } // 滚动到指定位置
    }

    private fun restoreAudioRelated(fileName: String) {
        restoreAudioIndex(fileName)
        touchHandler!!.highlightSentence(currentSentenceIndex)
    }

    private val audioIndex = "audio_index"
    private fun saveAudioIndex(fileName: String) {
        val preferences = getSharedPreferences("ReadingHistory", MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putInt(fileName + audioIndex, currentSentenceIndex) // 保存滚动位置
        editor.apply()
    }

    private fun restoreAudioIndex(fileName: String) {
        val preferences = getSharedPreferences("ReadingHistory", MODE_PRIVATE)
        currentSentenceIndex = preferences.getInt(fileName + audioIndex, 0) // 默认位置为 0
    }

    fun ttsReadWhenLongPress(index: Int) {
        onClickStop()
        currentSentenceIndex = index
        reading()
    }

    private fun reading() {
        if (!sentences!!.isEmpty()) {
//                tts!!.generate(sentences!![currentSentenceIndex])
            onClickGenerate()
        }
        readingStatus = true
    }
}
