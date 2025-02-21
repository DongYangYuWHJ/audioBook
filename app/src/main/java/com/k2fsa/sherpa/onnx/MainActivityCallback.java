package com.k2fsa.sherpa.onnx;

public interface MainActivityCallback {
    void onButtonClick(String fileName);
    void onTtsFinishCurrentSentence();
    void ttsReadWhenLongPress(int integer);
}
