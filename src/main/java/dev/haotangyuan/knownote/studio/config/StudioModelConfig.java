package dev.haotangyuan.knownote.studio.config;

import java.util.concurrent.Executor;

import dev.haotangyuan.knownote.config.ResearchProperties;
import dev.haotangyuan.knownote.research.model.ModelFactory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@RequiredArgsConstructor
public class StudioModelConfig {

    private final ModelFactory modelFactory;
    private final ResearchProperties researchProperties;

    @Bean("studioCodegenChatModel")
    public ChatModel studioCodegenChatModel() {
        return modelFactory.createChatModel(researchProperties.getModel());
    }

    @Bean("studioCodegenStreamingChatModel")
    public StreamingChatModel studioCodegenStreamingChatModel() {
        return modelFactory.createStreamingChatModel(researchProperties.getModel());
    }

    @Bean("studioTaskExecutor")
    public Executor studioTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("studio-gen-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
