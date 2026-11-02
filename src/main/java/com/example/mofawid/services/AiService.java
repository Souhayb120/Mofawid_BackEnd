package com.example.mofawid.services;

public interface AiService {

    String generate(String prompt);

    // turns a voice recording into text (the observation of the collaborateur)
    String transcribe(byte[] audio, String mimeType);
}
