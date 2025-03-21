package com.k2fsa.sherpa.onnx;

//import static com.k2fsa.sherpa.onnx.MainActivityKotlin.ttsReadWhenLongPress;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
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
    private Spannable spannable;
    private int sentenceIndex;
    private Object highlightBackground;
    private int offset;
    private Context context;
    private MainActivityCallback callback;
    public boolean onLongPressFirstSentence;

    public TextTouchHandler(Context context, TextView textView, List<SentenceSegmenter.SentenceInfo> sentences) {
        Log.d("donghuiFatal", "进来了吗？？？");
        this.textView = textView;
        this.sentences = sentences;
        this.gestureDetector = new GestureDetector(context, this);
        spannable = new SpannableString(textView.getText());
        highlightBackground = new BackgroundColorSpan(Color.YELLOW);
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
        spannable = new SpannableString(textView.getText());
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
        if (layout == null) return; // 避免 NullPointerException

        // 计算文本的行号（全局滚动影响）
        int line = layout.getLineForVertical((int) (y + textView.getScrollY()));
        Log.d("donghuiLine", "line: " + line);

        // 计算该行的字符索引
        int offset = layout.getOffsetForHorizontal(line, x);
        Log.d("donghuiOffset", ""+offset);
        // 找到句子索引
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
                if (offset <= sentences.get(i).startPos+sentences.get(i).text.length()) {
                    return i;
                }
            }
        }
        return -1;
    }



    public void highlightSentence(int sentenceIndex) {
        String fulltext = textView.getText().toString();

        // 获取所有的高亮 `BackgroundColorSpan`
//        Log.d("donghuiSpan", "spanning at " + sentenceIndex);
        BackgroundColorSpan[] spans = spannable.getSpans(0, spannable.length(), BackgroundColorSpan.class);
        // 移除所有高亮
        for (BackgroundColorSpan span : spans) {
            spannable.removeSpan(span);
        }

        textView.setText(spannable); // 更新 `TextView`
        int start = sentences.get(sentenceIndex).startPos;
        while(fulltext.charAt(start) == ' '){
            start++;
        }
        start--;
        int end = start + sentences.get(sentenceIndex).text.length();

        if (start >= 0) {
            spannable.setSpan(highlightBackground, start+1, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(spannable);
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

