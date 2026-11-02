package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.services.AiService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

@Service
public class AiServiceImpl implements AiService {

    // what we ask Gemini to do with the audio: write exactly what was said, nothing more
    private static final String TRANSCRIBE_PROMPT = """
            Transcribe this voice recording word for word.
            The speaker is a collaborator of a Moroccan commissaire judiciaire (huissier) describing what he sees on the field.
            He can speak Arabic, Moroccan Darija or French, and can mix them.
            Rules:
            - Write the text in the language that was spoken. Do not translate.
            - Do not summarize, do not add anything, do not comment.
            - Add normal punctuation.
            - Return only the transcribed text.
            - If nobody speaks in the recording, return an empty answer.
            """;

    private final ChatClient chatClient;

    public AiServiceImpl(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public String generate(String prompt) {

        return chatClient
                .prompt(prompt)
                .call()
                .content();
    }

    @Override
    public String transcribe(byte[] audio, String mimeType) {

        ByteArrayResource audioResource = new ByteArrayResource(audio);

        return chatClient
                .prompt()
                .user(user -> user
                        .text(TRANSCRIBE_PROMPT)
                        .media(MimeTypeUtils.parseMimeType(mimeType), audioResource))
                .call()
                .content();
    }
}
