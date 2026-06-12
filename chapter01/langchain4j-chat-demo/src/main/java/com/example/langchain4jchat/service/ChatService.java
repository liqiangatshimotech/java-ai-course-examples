package com.example.langchain4jchat.service;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class ChatService {

    private final ChatClientRegistry registry;

    public ChatService(ChatClientRegistry registry) {
        this.registry = registry;
    }

    public String chat(String message, String provider) {
        return registry.clients(provider).chatModel().chat(message);
    }

    public Flux<String> stream(String message, String provider) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        registry.clients(provider).streamingChatModel().chat(message, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                sink.tryEmitNext(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                sink.tryEmitComplete();
            }

            @Override
            public void onError(Throwable error) {
                sink.tryEmitError(error);
            }
        });

        return sink.asFlux();
    }

    public String assistantChat(String message, String provider) {
        return registry.clients(provider).assistant().chat(message);
    }

    public Flux<String> assistantStream(String message, String provider) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        registry.clients(provider).streamingAssistant().chat(message)
                .onPartialResponse(sink::tryEmitNext)
                .onCompleteResponse(response -> sink.tryEmitComplete())
                .onError(sink::tryEmitError)
                .start();

        return sink.asFlux();
    }
}
