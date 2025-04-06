package com.k2fsa.sherpa.onnx

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.ScrollingMovementMethod
import android.text.style.BackgroundColorSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import org.mozilla.universalchardet.UniversalDetector
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.Arrays
import java.util.LinkedList
import java.util.Queue
import com.google.gson.Gson
import com.google.gson.GsonBuilder

const val TAG = "sherpa-onnx"



class MainActivity : AppCompatActivity(), MainActivityCallback {
    private val FILE_SUFFIX = ".json"
    private val gson = GsonBuilder().setPrettyPrinting().create()

    private lateinit var tts: OfflineTts
    private var player: ExoPlayer? = null
    private lateinit var audioLoadingProgress: ProgressBar
    private lateinit var mediaSourceFactory: ProgressiveMediaSource.Factory


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
    private var sentences: List<SentenceSegmenter.SentenceInfo>? = null
    var currentSentenceIndex: Int = 0
    private var readingStatus = false //true for reading now, false for not reading

    // see
    // https://developer.android.com/reference/kotlin/android/media/AudioTrack
    private lateinit var track: AudioTrack

    private var audioQueueRegularSize = 3
    private val audioQueueRemainder = 6
    private var audioQueueCounter = 0
//    private
    // 维护 index -> filename 的映射，限制最大大小
    private val audioIndexToFileMap = LinkedHashMap<Int, String>(audioQueueRegularSize * 4, 0.75f, true)
    private val audioIndexQueue: Queue<Int> = LinkedList()
    private val dictionaryMaxSize = audioQueueRegularSize * 4
    private var retryCount = 0
    private val maxRetry = 50 // 最多重试 50 次（5 秒）
    private var peededAudioQueueIndex: Int = 0

    private lateinit var settingsButton: ImageButton

    private var isWaitingForNextAudio = false
    private val handler = Handler(Looper.getMainLooper())
    private val checkQueueRunnable = object : Runnable {
        override fun run() {
            if (isWaitingForNextAudio) {
                if (!audioIndexQueue.isEmpty()) {
                    isWaitingForNextAudio = false
                    playNextAudio()
                } else {
                    // 如果还在等待，继续检查
                    handler.postDelayed(this, 100) // 每100ms检查一次
                }
            }
        }
    }

    private val sentencesPerPage = 50 // 每页显示的句子数
    private var currentPage = 0
    private var totalPages = 0
    private var isLoadingPage = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化 ExoPlayer
        player = ExoPlayer.Builder(this).build()
        val dataSourceFactory = DefaultDataSourceFactory(this,
            Util.getUserAgent(this, getString(R.string.app_name)))
        mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)

        // 设置播放监听器
        player?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_ENDED -> {
                        Log.d("donghuiPlaying", "ended")
                        val removedIndex = audioIndexQueue.poll()
                        audioIndexToFileMap.remove(removedIndex)
                        
                        // 如果队列为空，开始等待
                        if (audioIndexQueue.isEmpty()) {
                            Log.d("donghuiPlaying", "queue empty, waiting for next audio")
                            isWaitingForNextAudio = true
                            handler.post(checkQueueRunnable)
                        } else {
                            playNextAudio()
                        }
                    }
                }
            }
        })

        audioLoadingProgress = findViewById(R.id.audioLoadingProgress)

        Log.i(TAG, "Start to initialize TTS")
        Log.i(TAG, "joker")
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
        touchHandler!!.textTouchHandlerUpdateMainActivityCallBack(this)

        settingsButton = findViewById(R.id.settingsButton)
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

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
                buttonPause.setText(R.string.resume)
                onClickPause()
            } else {
                buttonPause.setText(R.string.pause)
                onClickResume()
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

        setupPagination()
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
//    private fun callback(samples: FloatArray): Int {
//        if (!stopped) {
//            track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
//            return 1
//        } else {
//            track.stop()
//            return 0
//        }
//    }

    private fun onClickGenerate(index: Int) {
        val sidInt = 0
        val speedFloat = 1.0f

        currentSentenceIndex = index
        
        // 显示加载动画
        runOnUiThread {
            audioLoadingProgress.visibility = View.VISIBLE
        }

        Thread {
            kotlin.synchronized(audioIndexQueue) {
                while (true) {
                    if(audioIndexQueue.size <= audioQueueRegularSize){
                        val textStr = sentences!!.getOrNull(currentSentenceIndex)!!.text ?: break
                        if (textStr.isBlank()) {
                            currentSentenceIndex++
                            continue
                        }
                        val audio = tts.generate(textStr, sid = sidInt, speed = speedFloat)
                        val filename = application.filesDir.absolutePath + "/generated${audioQueueCounter % audioQueueRemainder}.wav"
                        Log.d("donghuiGenerate", "generating: " + filename + " index: " + currentSentenceIndex)
                        val ok = audio.samples.isNotEmpty() && audio.save(filename)
                        if (ok) {
                            Thread {
                                audioIndexToFileMap[currentSentenceIndex] = filename
                                audioIndexQueue.add(currentSentenceIndex)

                                // **删除 dictionary 里最早的项，但要确保它不在 Queue 里**
                                if (audioIndexToFileMap.size > dictionaryMaxSize) {
                                    val oldestKey = audioIndexToFileMap.keys.firstOrNull()
                                    if (oldestKey != null && !audioIndexQueue.contains(oldestKey)) {
                                        audioIndexToFileMap.remove(oldestKey)
                                    }
                                }
                                Log.d("donghuiPlaying", "queue size " + audioIndexQueue.size)
                            }.start()
                        }

                        audioQueueCounter++
                        currentSentenceIndex++
                    }
                    Thread.sleep(50) // 避免生成速度过快
                }
            }
        }.start()
    }

    private fun playNextAudio() {
        Log.d("donghuiPlaying", "next audio")
        peededAudioQueueIndex = audioIndexQueue.peek()
        val filename = audioIndexToFileMap[peededAudioQueueIndex]
        peededAudioQueueIndex--

        if (filename == null) {
            Log.e("donghuiError", getString(R.string.error_file_not_found))
            onClickGenerate(peededAudioQueueIndex)
            Handler(Looper.getMainLooper()).postDelayed({ playNextAudio() }, 500)
            return
        }

        val file = File(filename)
        if (!file.exists()) {
            Log.e("donghuiError", getString(R.string.error_file_not_found))
            onClickGenerate(peededAudioQueueIndex)
            Handler(Looper.getMainLooper()).postDelayed({ playNextAudio() }, 500)
            return
        }

        // 在主线程上执行 ExoPlayer 相关操作
        runOnUiThread {
            // 创建 MediaSource
            val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
            val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)

            // 准备并播放
            player?.setMediaSource(mediaSource)
            player?.prepare()
            player?.playWhenReady = true
            player?.setPlaybackSpeed(0.85f)

            // 隐藏加载动画
            audioLoadingProgress.visibility = View.GONE

            if(touchHandler.onLongPressFirstSentence){
                touchHandler.onLongPressFirstSentence = false
            }else{
                touchHandler.highlightSentence(peededAudioQueueIndex!!)
            }
        }
    }

    private fun onClickPlay() {
        if (audioIndexQueue.isEmpty()) {
            if (retryCount < maxRetry) {
                retryCount++
                Handler(Looper.getMainLooper()).postDelayed({ onClickPlay() }, 100)
            } else {
                Log.e("donghuiError", getString(R.string.error_max_retry))
                retryCount = 0
                runOnUiThread {
                    audioLoadingProgress.visibility = View.GONE
                }
            }
            return
        }
        retryCount = 0
        playNextAudio()
    }

    private fun onClickPause() {
        runOnUiThread {
            player?.pause()
            readingStatus = false
        }
    }

    private fun onClickResume() {
        runOnUiThread {
            readingStatus = true
            player?.play()
        }
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


        // Example 6
        // vits-melo-tts-zh_en
        // https://k2-fsa.github.io/sherpa/onnx/tts/pretrained_models/vits.html#vits-melo-tts-zh-en-chinese-english-1-speaker
        modelDir = "vits-melo-tts-zh_en"
        modelName = "model.onnx"
        lexicon = "lexicon.txt"
        dictDir = "vits-melo-tts-zh_en/dict"

        val preferences = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val preferenceModel = preferences.getString("selected_model", "en")
        if(preferenceModel == "en"){
            Log.d("donghuiModel", "yingwen")
            //coqui:
            modelDir = "vits-coqui-en-ljspeech"
            modelName = "model.onnx"
            dataDir = "vits-coqui-en-ljspeech/espeak-ng-data"
        }else{
            Log.d("donghuiModel", "中文")
            // Example 6
            // vits-melo-tts-zh_en
            // https://k2-fsa.github.io/sherpa/onnx/tts/pretrained_models/vits.html#vits-melo-tts-zh-en-chinese-english-1-speaker
            modelDir = "vits-melo-tts-zh_en"
            modelName = "model.onnx"
            lexicon = "lexicon.txt"
            dictDir = "vits-melo-tts-zh_en/dict"
        }

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
            //todo: 不知道这个是否是需要的？？？好像没有也无所谓？
//            if (ruleFsts == null) {
//                ruleFsts = "$modelDir/phone.fst,$modelDir/date.fst,$modelDir/number.fst"
//            }
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
        // 停止等待检查
        isWaitingForNextAudio = false
        handler.removeCallbacks(checkQueueRunnable)
        
        // 释放 TTS 资源
        if (tts != null) {
            onClickPause()
            runOnUiThread {
                player?.release()
                player = null
            }
        }
        super.onDestroy()
    }

    // 打开文件选择器
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setType("text/plain") // 限制为TXT文件
        startActivityForResult(Intent.createChooser(intent, "选择要录入的TXT文件"), PICK_TXT_FILE)
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

    private fun updateReadingText() {
//        val sentenceSegmenter = SentenceSegmenter()
//        sentences = sentenceSegmenter.segment(txtContent.text.toString())
        
        // 使用重组后的文本更新 UI
//        val combinedText = sentenceSegmenter.combineText(sentences)
//        txtContent.text = combinedText
        
        currentSentenceIndex = 0
        touchHandler.updateSentences(sentences)
    }

    /**
     * 录入文本至app内文件夹
     * @param fileName
     * @param content
     */
    private fun saveFileToInternalStorage(fileName: String, content: String) {
        try {
            // 分割文本并保存 sentences 到 JSON 文件
            val sentenceSegmenter = SentenceSegmenter()
            val sentences = sentenceSegmenter.segment(content)
            val sentencesJson = gson.toJson(sentences)
            
            // 保存 sentences 到 JSON 文件
            val fos = openFileOutput(fileName, MODE_PRIVATE)
            fos.write(sentencesJson.toByteArray())
            fos.close()

            Log.d("donghuiFile", "File stored at: " + filesDir.absolutePath)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 读取已录入的文件
     */
    private fun loadFileFromInternalStorage(fileName: String?): List<SentenceSegmenter.SentenceInfo>? {
        try {
            val fis = openFileInput(fileName)
            val reader = BufferedReader(InputStreamReader(fis))
            val stringBuilder = StringBuilder()
            var line: String?

            while ((reader.readLine().also { line = it }) != null) {
                stringBuilder.append(line)
            }
            fis.close()
            
            // 将 JSON 字符串转换为 List<SentenceInfo>
            return gson.fromJson(stringBuilder.toString(), Array<SentenceSegmenter.SentenceInfo>::class.java).toList()
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
            val charBuffer = CharArray(8192)
            var read: Int

            // 直接读取字符，保持原始格式
            while (reader.read(charBuffer).also { read = it } != -1) {
                stringBuilder.append(charBuffer, 0, read)
            }
            inputStream!!.close()

            val fileName = "$novelTitle" + FILE_SUFFIX // 自定义文件名
            saveFileToInternalStorage(fileName, stringBuilder.toString())

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
            sentences = savedContent
            val sentenceSegmenter = SentenceSegmenter()
            val combinedText = sentenceSegmenter.combineText(sentences)
            txtContent.text = combinedText
            updateReadingText()
            
            // 设置分页
            setupPagination()
            loadPage(0) // 加载第一页
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
                if (file.isFile && file.name.endsWith(FILE_SUFFIX)) {
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

    private fun clearAllQueues() {
        // 清空音频队列
        audioIndexQueue.clear()
        audioIndexToFileMap.clear()
        
        // 停止当前播放
        runOnUiThread {
            player?.stop()
            player?.clearMediaItems()
        }
        
        // 删除所有已生成的音频文件
        for (i in 0 until audioQueueRemainder) {
            val file = File(application.filesDir.absolutePath + "/generated${i}.wav")
            if (file.exists()) {
                file.delete()
            }
        }
        
        // 重置计数器
        audioQueueCounter = 0
        peededAudioQueueIndex = 0
        retryCount = 0
        isWaitingForNextAudio = false
        handler.removeCallbacks(checkQueueRunnable)
    }

    override fun ttsReadWhenLongPress(index: Int) {
        // 清空所有队列和状态
        clearAllQueues()
        
        // 设置新的起始位置
        currentSentenceIndex = index
        
        // 开始生成和播放
        onClickGenerate(currentSentenceIndex)
        reading()
    }

    private fun reading() {
        readingStatus = true
        Thread {
            kotlin.synchronized(peededAudioQueueIndex){
                while (readingStatus) {
                    if (!sentences!!.isEmpty() && currentSentenceIndex < sentences!!.size && audioIndexQueue.size >= audioQueueRegularSize) {
                        runOnUiThread {
                            onClickPlay()
                        }
                        break
                    }
                    Thread.sleep(250)
                }
            }
        }.start()
    }

    override fun onBackPressed() {
        // 检查当前显示的页面状态
        when {
            // 如果书架列表可见，隐藏它并显示主页面
            recyclerView.visibility == View.VISIBLE -> {
                recyclerView.visibility = View.INVISIBLE
                pageNovel.visibility = View.VISIBLE
            }
            // 如果标题输入对话框可见，隐藏它并返回主页面
            findViewById<View>(R.id.layout_novel_title_for_input).visibility == View.VISIBLE -> {
                findViewById<View>(R.id.layout_novel_title_for_input).visibility = View.INVISIBLE
                textNovelTitleForInput.setText("") // 清空输入框
                pageNovel.visibility = View.VISIBLE // 确保主页面可见
            }
            // 其他情况，执行默认的返回操作
            else -> {
                super.onBackPressed()
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    private fun saveSentences(sentences: List<SentenceSegmenter.SentenceInfo>) {
        try {
            val filePath = getExternalFilesDir(null)?.absolutePath + "/sentences.json"
            File(filePath).writeText(gson.toJson(sentences))
            Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSentences(): List<SentenceSegmenter.SentenceInfo>? {
        return try {
            val filePath = getExternalFilesDir(null)?.absolutePath + "/sentences.json"
            val file = File(filePath)
            if (file.exists()) {
                gson.fromJson(file.readText(), Array<SentenceSegmenter.SentenceInfo>::class.java).toList()
            } else null
        } catch (e: Exception) {
            Toast.makeText(this, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun updateReadingText(sentenceSegmenter: SentenceSegmenter, text: String) {
        // 先尝试加载已保存的句子
        val loadedSentences = loadSentences()
        if (loadedSentences != null) {
            // 如果有保存的句子，直接使用
            val combinedText = sentenceSegmenter.combineText(loadedSentences)
            txtContent.text = combinedText
            currentSentenceIndex = 0
            touchHandler.updateSentences(loadedSentences)
            return
        }

        // 如果没有保存的句子，进行分割
        val sentences = sentenceSegmenter.segment(text)
        // 保存分割后的句子
        saveSentences(sentences)
        
        val combinedText = sentenceSegmenter.combineText(sentences)
        txtContent.text = combinedText
        currentSentenceIndex = 0
        touchHandler.updateSentences(sentences)
    }

    private fun setupPagination() {
        if (sentences == null) return
        
        // 计算总页数
        totalPages = (sentences!!.size + sentencesPerPage - 1) / sentencesPerPage
        Log.d("Pagination", "Total sentences: ${sentences!!.size}, Total pages: $totalPages")
        
        // 设置滚动监听
        txtContent.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (!isLoadingPage) {
                val layout = txtContent.layout
                if (layout != null) {
                    val firstVisibleLine = layout.getLineForVertical(scrollY)
                    val lastVisibleLine = layout.getLineForVertical(scrollY + txtContent.height)
                    
                    // 检测是否需要加载下一页
                    if (lastVisibleLine >= layout.lineCount - 10) {
                        loadNextPage()
                    }
                    
                    // 检测是否需要加载上一页
                    if (firstVisibleLine <= 10) {
                        loadPreviousPage()
                    }
                }
            }
        }
    }
    
    private fun loadPage(page: Int) {
        if (page < 0 || page >= totalPages || isLoadingPage || sentences == null) return
        
        isLoadingPage = true
        
        val startIndex = page * sentencesPerPage
        val endIndex = minOf(startIndex + sentencesPerPage, sentences!!.size)
        
        // 获取当前页的句子
        val pageSentences = sentences!!.subList(startIndex, endIndex)
        
        // 组合当前页的文本
        val sentenceSegmenter = SentenceSegmenter()
        val pageText = sentenceSegmenter.combineText(pageSentences)
        
        // 更新UI
        updatePageUI(page, pageText, pageSentences, startIndex)
        isLoadingPage = false
    }
    
    private fun updatePageUI(page: Int, text: String, sentences: List<SentenceSegmenter.SentenceInfo>, startIndex: Int) {
        val spannable = SpannableStringBuilder(text)
        var currentOffset = 0
        
        for (sentence in sentences) {
            spannable.setSpan(
                BackgroundColorSpan(Color.parseColor("#80FFA500")),
                currentOffset,
                currentOffset + sentence.text.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            currentOffset += sentence.text.length
        }
        
        // 更新当前页的文本
        txtContent.text = spannable
    }
    
    private fun loadNextPage() {
        if (currentPage < totalPages - 1) {
            currentPage++
            loadPage(currentPage)
        }
    }
    
    private fun loadPreviousPage() {
        if (currentPage > 0) {
            currentPage--
            loadPage(currentPage)
        }
    }
}