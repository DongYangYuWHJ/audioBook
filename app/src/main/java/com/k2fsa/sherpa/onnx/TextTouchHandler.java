package com.k2fsa.sherpa.onnx;

//import static com.k2fsa.sherpa.onnx.MainActivityKotlin.ttsReadWhenLongPress;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;

import java.util.List;

public class TextTouchHandler implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    private final GestureDetector gestureDetector;
    private final TextView textView;
    private List<SentenceSegmenter.SentenceInfo> sentences;
    private SpannableStringBuilder spannable;
    private int sentenceIndex;
    private Object highlightBackground;
    private int offset;
    private Context context;
    private MainActivityCallback callback;
    public boolean onLongPressFirstSentence;
    private int currentHighlightedIndex = -1;

    public TextTouchHandler(Context context, TextView textView, List<SentenceSegmenter.SentenceInfo> sentences) {
        Log.d("donghuiFatal", "进来了吗？？？");
        this.textView = textView;
        this.sentences = sentences;
        this.gestureDetector = new GestureDetector(context, this);
        spannable = new SpannableStringBuilder(textView.getText());
        highlightBackground = new BackgroundColorSpan(Color.parseColor("#80FFA500")); // 半透明暗橘色
        this.context = context;
    }
    public void textTouchHandlerUpdateMainActivityCallBack(MainActivityCallback callback){
        this.callback = callback;
    }
    public void textTouchHandlerLog(){
        Log.d("donghuiFatal", "咩咩咩咩吗");
    }
    public void updateSentences(List<SentenceSegmenter.SentenceInfo> sentences){
        this.sentences = sentences;
        spannable = new SpannableStringBuilder(textView.getText());
        Log.d("DonghuiLoadClip", "文本具体修改位置");
        Log.d("DonghuiLoadClip", "" + textView.getText());
    }

    @SuppressLint("ClickableViewAccessibility")
    public void attach() {
        Log.d("donghuiFatal", "attach了吗?");
        textView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        // 处理轻触（单击），但不触发高光
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();

        Layout layout = textView.getLayout();
        if (layout == null) return;

        int line = layout.getLineForVertical((int) (y + textView.getScrollY()));
        Log.d("donghuiLine", "line: " + line);

        int offset = layout.getOffsetForHorizontal(line, x);
        Log.d("donghuiOffset", ""+offset);
        
        sentenceIndex = getSentenceIndexFromOffset(textView, sentences, offset);
        Log.d("donghuiSentenceIndex", ""+sentenceIndex);
        if (sentenceIndex != -1) {
            highlightSentence(sentenceIndex);
            startListeningFrom(sentenceIndex);
        }
        onLongPressFirstSentence = true;
    }

    public int getSentenceIndex() {
        return sentenceIndex;
    }

    private int getSentenceIndexFromOffset(TextView textView, List<SentenceSegmenter.SentenceInfo> sentences, int offset) {
        if(sentences != null){
            for (int i = 0; i < sentences.size(); i++) {
                SentenceSegmenter.SentenceInfo sentence = sentences.get(i);
                // 使用 actualStartPos 和 actualLength 来判断范围
                if (offset >= sentence.startPos && offset < sentence.startPos + sentence.text.length()) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void highlightSentence(int sentenceIndex) {
        if (sentenceIndex < 0 || sentenceIndex >= sentences.size()) {
            return;
        }

        // 获取当前句子的信息
        SentenceSegmenter.SentenceInfo sentence = sentences.get(sentenceIndex);
        
        // 只清除当前高亮的句子
        if (currentHighlightedIndex != -1) {
            SentenceSegmenter.SentenceInfo oldSentence = sentences.get(currentHighlightedIndex);
            BackgroundColorSpan[] spans = spannable.getSpans(
                oldSentence.startPos, 
                oldSentence.startPos + oldSentence.text.length(), 
                BackgroundColorSpan.class);
            for (BackgroundColorSpan span : spans) {
                spannable.removeSpan(span);
            }
        }

        // 使用实际的文本位置和长度
        int start = sentence.startPos;
        int end = start + sentence.text.length();
        
        // 添加日志以便调试
        Log.d("donghuiSpan", "Highlighting from " + start + " to " + end);
        Log.d("donghuiSpan", "Text: [" + spannable.subSequence(start, end) + "]");
        Log.d("donghuiSpan", "Real Text: [" + sentence.text + "]");

        // 设置新的高亮
        if (start >= 0 && end <= spannable.length()) {
            spannable.setSpan(highlightBackground, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(spannable);
            currentHighlightedIndex = sentenceIndex;
        }
    }

    private void startListeningFrom(int startIndex) {
        if(callback == null){
            Log.d("donghuiFatal", "???");
        }else{
            callback.ttsReadWhenLongPress(startIndex);
        }
    }

    public int getNewYScroll(){
        Layout layout = textView.getLayout();
        int line = layout.getLineForOffset(offset); // 计算句子所在行
        int y = layout.getLineTop(line);
        return y;
    }

    @Override public boolean onDown(MotionEvent e) { return false; }
    @Override public void onShowPress(MotionEvent e) {}
    @Override public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) { return false; }
//    @Override public void onLongPress(MotionEvent e) {}
    @Override public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) { return false; }
    @Override public boolean onDoubleTap(MotionEvent e) { return false; }
    @Override public boolean onDoubleTapEvent(MotionEvent e) { return false; }
    @Override public boolean onSingleTapConfirmed(MotionEvent e) { return false; }
}

