package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.speech.tts.TextToSpeech;
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
    private List<String> sentences;
    private final TextToSpeech tts;

    public TextTouchHandler(Context context, TextView textView, List<String> sentences, TextToSpeech tts) {
        this.textView = textView;
        this.sentences = sentences;
        this.tts = tts;
        this.gestureDetector = new GestureDetector(context, this);
    }
    public void updateSentences(List<String> sentences){
        this.sentences = sentences;
    }

    @SuppressLint("ClickableViewAccessibility")
    public void attach() {
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
        int sentenceIndex = getSentenceIndexFromOffset(textView, sentences, offset);
        Log.d("donghuiSentenceIndex", ""+sentenceIndex);
        if (sentenceIndex != -1) {
            highlightSentence(textView, sentenceIndex);
//            startListeningFrom(sentenceIndex);
        }
    }

    private int getSentenceIndexFromOffset(TextView textView, List<String> sentences, int offset) {
        String fullText = textView.getText().toString();
        int charCount = 0;

        if(sentences != null){
            for (int i = 0; i < sentences.size(); i++) {
                charCount += sentences.get(i).length()+1;//1是分隔符的数量
                if (offset <= charCount) {
                    return i;
                }
            }
        }
        return -1;
    }



    private void highlightSentence(TextView textView, int sentenceIndex) {
        Spannable spannable = new SpannableString(textView.getText());
        int start = 0;
        for(int i = 0; i < sentenceIndex; i++){
            start += sentences.get(i).length()+1;//add the separator param
        }
        start--;
        int end = start + sentences.get(sentenceIndex).length()+1;

        if (start >= 0) {
            spannable.setSpan(new BackgroundColorSpan(0xFFFFFF00), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            textView.setText(spannable);
        }
    }

    private void startListeningFrom(int startIndex) {
        for (int i = startIndex; i < sentences.size(); i++) {
            String sentence = sentences.get(i).trim();
            int finalI = i;

            tts.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, "UtteranceID_" + i);
            break;
        }
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

