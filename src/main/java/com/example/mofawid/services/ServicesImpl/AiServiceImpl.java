package com.example.mofawid.services.ServicesImpl;

import com.example.mofawid.services.AiService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiServiceImpl implements AiService {

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
}