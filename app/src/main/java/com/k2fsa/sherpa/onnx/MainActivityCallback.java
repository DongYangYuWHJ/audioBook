package com.k2fsa.sherpa.onnx;

public interface MainActivityCallback {
    void onButtonClick(String fileName);
    void onTtsFinishGneratingCurrentSentence();
    void ttsReadWhenLongPress(int integer);
}
