package dev.haotangyuan.knownote.studio.agent;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Wraps LangChain4j model calls for code generation.
 */
@Component
@Slf4j
public class CodeGenAgent {

    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;

    public CodeGenAgent(
            @Qualifier("studioCodegenChatModel") ChatModel chatModel,
            @Qualifier("studioCodegenStreamingChatModel") StreamingChatModel streamingChatModel
    ) {
        this.chatModel = chatModel;
        this.streamingChatModel = streamingChatModel;
    }

    /**
     * Non-streaming call — returns the full response text.
     * Used in the Architect phase to get the file plan as JSON.
     */
    public String chat(String systemPrompt, String userMessage) {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        SystemMessage.from(systemPrompt),
                        UserMessage.from(userMessage)
                ))
                .build();
        ChatResponse response = chatModel.chat(request);
        return response.aiMessage().text();
    }

    /**
     * Streaming call — invokes onToken for each partial token.
     * Used in the Coder phase for streaming file content.
     * Blocks until the stream is complete.
     */
    public String streamChat(
            String systemPrompt,
            String userMessage,
            Consumer<String> onToken
    ) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder full = new StringBuilder();
        AtomicReference<Throwable> error = new AtomicReference<>();

        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        SystemMessage.from(systemPrompt),
                        UserMessage.from(userMessage)
                ))
                .build();

        streamingChatModel.chat(request, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String token) {
                full.append(token);
                onToken.accept(token);
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                latch.countDown();
            }

            @Override
            public void onError(Throwable err) {
                error.set(err);
                latch.countDown();
            }
        });

        latch.await();
        if (error.get() != null) throw new RuntimeException("Streaming failed", error.get());
        return full.toString();
    }
}
