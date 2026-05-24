package dev.haotangyuan.knownote.research.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import dev.haotangyuan.knownote.common.UserContext;
import dev.haotangyuan.knownote.common.util.CacheUtil;
import dev.haotangyuan.knownote.config.ResearchProperties;
import dev.haotangyuan.knownote.research.api.dto.req.SendMessageReqDTO;
import dev.haotangyuan.knownote.research.api.dto.resp.CreateResearchRespDTO;
import dev.haotangyuan.knownote.research.api.dto.resp.ResearchMessageRespDTO;
import dev.haotangyuan.knownote.research.api.dto.resp.ResearchStatusRespDTO;
import dev.haotangyuan.knownote.research.api.dto.resp.SendMessageRespDTO;
import dev.haotangyuan.knownote.research.data.TimelineItem;
import dev.haotangyuan.knownote.research.data.WorkflowStatus;
import dev.haotangyuan.knownote.research.domain.entity.ChatMessageDO;
import dev.haotangyuan.knownote.research.domain.entity.ResearchSessionDO;
import dev.haotangyuan.knownote.research.domain.entity.WorkflowEventDO;
import dev.haotangyuan.knownote.research.domain.mapper.ChatMessageMapper;
import dev.haotangyuan.knownote.research.domain.mapper.ResearchSessionMapper;
import dev.haotangyuan.knownote.research.exception.ResearchException;
import dev.haotangyuan.knownote.research.model.ModelHandler;
import dev.haotangyuan.knownote.research.service.ResearchService;
import dev.haotangyuan.knownote.research.state.DeepResearchState;
import dev.haotangyuan.knownote.research.workflow.AgentPipeline;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 研究服务实现
 */
@Service
@RequiredArgsConstructor
public class ResearchServiceImpl implements ResearchService {

    private final ResearchSessionMapper researchSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final AgentPipeline agentPipeline;
    private final CacheUtil cacheUtil;
    private final ModelHandler modelHandler;
    private final ResearchProperties researchProperties;

    @Override
    public CreateResearchRespDTO createResearch(Integer num) {
        Long userId = requireUserId();
        int targetNum = num == null ? 1 : Math.max(num, 1);

        LambdaQueryWrapper<ResearchSessionDO> queryWrapper = Wrappers.lambdaQuery(ResearchSessionDO.class)
                .eq(ResearchSessionDO::getUserId, userId)
                .eq(ResearchSessionDO::getStatus, WorkflowStatus.NEW);
        List<ResearchSessionDO> sessions = new ArrayList<>(researchSessionMapper.selectList(queryWrapper));
        int oldNum = sessions.size();
        if (targetNum > oldNum) {
            for (int i = 0; i < targetNum - oldNum; i++) {
                ResearchSessionDO researchSession = ResearchSessionDO.builder()
                        .userId(userId)
                        .status(WorkflowStatus.NEW)
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .build();
                researchSessionMapper.insert(researchSession);
                sessions.add(researchSession);
            }
        }
        List<String> researchIds = sessions.stream()
                .sorted((o1, o2) -> o1.getCreateTime().compareTo(o2.getCreateTime()))
                .map(ResearchSessionDO::getId)
                .limit(targetNum)
                .collect(Collectors.toList());

        for (String researchId : researchIds) {
            cacheUtil.cacheResearchOwnership(researchId, userId);
        }

        return CreateResearchRespDTO.builder()
                .researchIds(researchIds)
                .build();
    }

    @Override
    public List<ResearchStatusRespDTO> getResearchList() {
        Long userId = requireUserId();
        LambdaQueryWrapper<ResearchSessionDO> queryWrapper = Wrappers.lambdaQuery(ResearchSessionDO.class)
                .eq(ResearchSessionDO::getUserId, userId)
                .orderByDesc(ResearchSessionDO::getUpdateTime);
        List<ResearchSessionDO> sessions = researchSessionMapper.selectList(queryWrapper);

        return sessions.stream().map(session -> ResearchStatusRespDTO.builder()
            .id(session.getId())
            .status(session.getStatus())
            .title(session.getTitle())
            .modelId(session.getModelId())
            .budget(session.getBudget())
            .startTime(session.getStartTime())
            .completeTime(session.getCompleteTime())
            .totalInputTokens(session.getTotalInputTokens())
            .totalOutputTokens(session.getTotalOutputTokens())
            .build()).collect(Collectors.toList());
    }

    @Override
    public ResearchStatusRespDTO getResearchStatus(String researchId) {
        Long userId = requireUserId();
        if (!cacheUtil.verifyResearchOwnership(researchId, userId)) {
            throw new ResearchException("研究任务不存在或无权限访问");
        }

        ResearchSessionDO researchSession = researchSessionMapper.selectById(researchId);
        if (researchSession == null) {
            throw new ResearchException("研究任务不存在");
        }
        return ResearchStatusRespDTO.builder()
                .id(researchSession.getId())
                .status(researchSession.getStatus())
                .title(researchSession.getTitle())
                .modelId(researchSession.getModelId())
                .budget(researchSession.getBudget())
                .startTime(researchSession.getStartTime())
                .completeTime(researchSession.getCompleteTime())
                .totalInputTokens(researchSession.getTotalInputTokens())
                .totalOutputTokens(researchSession.getTotalOutputTokens())
                .build();
    }

    @Override
    public ResearchMessageRespDTO getResearchMessages(String researchId) {
        Long userId = requireUserId();
        if (!cacheUtil.verifyResearchOwnership(researchId, userId)) {
            throw new ResearchException("研究任务不存在或无权限访问");
        }

        ResearchSessionDO researchSession = researchSessionMapper.selectById(researchId);
        if (researchSession == null) {
            throw new ResearchException("研究任务不存在");
        }

        List<TimelineItem> timeline = cacheUtil.getTimeline(researchId, 0);
        List<ChatMessageDO> messages = timeline.stream()
                .filter(t -> "message".equals(t.getKind()))
                .map(TimelineItem::getMessage)
                .toList();
        List<WorkflowEventDO> events = timeline.stream()
                .filter(t -> "event".equals(t.getKind()))
                .map(TimelineItem::getEvent)
                .toList();

        return ResearchMessageRespDTO.builder()
                .id(researchSession.getId())
                .status(researchSession.getStatus())
                .messages(messages)
                .events(events)
                .startTime(researchSession.getStartTime())
                .updateTime(researchSession.getUpdateTime())
                .completeTime(researchSession.getCompleteTime())
                .totalInputTokens(researchSession.getTotalInputTokens())
                .totalOutputTokens(researchSession.getTotalOutputTokens())
                .build();
    }

    @Override
    public SendMessageRespDTO sendMessage(String researchId, SendMessageReqDTO sendMessageReqDTO) {
        Long userId = requireUserId();
        if (sendMessageReqDTO == null || !StringUtils.hasText(sendMessageReqDTO.getContent())) {
            throw new ResearchException("消息内容不能为空");
        }

        int affected = researchSessionMapper.casUpdateToQueue(researchId, userId);
        if (affected == 0) {
            ResearchSessionDO failedSession = researchSessionMapper.selectById(researchId);
            if (failedSession == null) {
                throw new ResearchException("研究任务不存在");
            }
            if (!userId.equals(failedSession.getUserId())) {
                throw new ResearchException("无权访问此研究");
            }
            if (!WorkflowStatus.NEW.equals(failedSession.getStatus())
                    && !WorkflowStatus.NEED_CLARIFICATION.equals(failedSession.getStatus())) {
                throw new ResearchException("研究状态不允许启动");
            }
            throw new ResearchException("启动研究异常");
        }

        ResearchSessionDO session = researchSessionMapper.selectById(researchId);
        if (session == null) {
            throw new ResearchException("研究不存在");
        }
        if (!userId.equals(session.getUserId())) {
            throw new ResearchException("无权访问此研究");
        }

        String modelId = session.getModelId();
        String budget = session.getBudget();

        if (!StringUtils.hasText(modelId)) {
            String requestedModelId = sendMessageReqDTO.getModelId();
            ResearchProperties.Model modelConfig = resolveModelConfig(requestedModelId);
            modelId = StringUtils.hasText(modelConfig.getId()) ? modelConfig.getId() : "default";

            String title = sendMessageReqDTO.getContent().length() > 20
                ? sendMessageReqDTO.getContent().substring(0, 20)
                : sendMessageReqDTO.getContent();
            budget = sendMessageReqDTO.getBudget();
            if (!StringUtils.hasText(budget)) {
                budget = "HIGH";
            }
            researchSessionMapper.setInfoIfNull(researchId, modelId, budget, title);
        }

        ResearchProperties.Model modelConfig = resolveModelConfig(modelId);
        modelHandler.addModel(researchId, modelConfig);

        ResearchProperties.BudgetLevel budgetLevel = researchProperties.getBudget().getLevel(budget);
        if (budgetLevel == null) {
            throw new ResearchException("研究预算配置不存在");
        }

        cacheUtil.saveMessage(researchId, "user", sendMessageReqDTO.getContent());

        LambdaQueryWrapper<ChatMessageDO> historyQuery = Wrappers.lambdaQuery(ChatMessageDO.class)
                .eq(ChatMessageDO::getResearchId, researchId)
                .orderByAsc(ChatMessageDO::getSequenceNo);
        List<ChatMessageDO> dbMessages = chatMessageMapper.selectList(historyQuery);

        List<dev.langchain4j.data.message.ChatMessage> chatHistory = new ArrayList<>();
        for (ChatMessageDO msg : dbMessages) {
            if ("user".equals(msg.getRole())) {
                chatHistory.add(UserMessage.from(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                chatHistory.add(AiMessage.from(msg.getContent()));
            }
        }

        DeepResearchState state = DeepResearchState.builder()
                .researchId(researchId)
                .chatHistory(chatHistory)
                .status(WorkflowStatus.QUEUE)
                .budget(budgetLevel)
                .supervisorIterations(0)
                .conductCount(0)
                .supervisorNotes(new ArrayList<>())
                .researcherIterations(0)
                .searchCount(0)
                .researcherNotes(new ArrayList<>())
                .searchResults(new HashMap<>())
                .searchNotes(new ArrayList<>())
                .totalInputTokens(0L)
                .totalOutputTokens(0L)
                .build();
        agentPipeline.run(state);

        return SendMessageRespDTO.builder()
                .id(researchId)
                .content("已接受任务")
                .build();
    }

    private Long requireUserId() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new ResearchException("未登录");
        }
        return userId;
    }

    private ResearchProperties.Model resolveModelConfig(String requestedModelId) {
        ResearchProperties.Model model = researchProperties.getModel();
        if (model == null) {
            throw new ResearchException("研究模型未配置");
        }
        String configuredId = StringUtils.hasText(model.getId()) ? model.getId() : "default";
        if (StringUtils.hasText(requestedModelId) && !StrUtil.equals(requestedModelId, configuredId)) {
            throw new ResearchException("模型不存在或未配置");
        }
        if (!StringUtils.hasText(model.getModel()) || !StringUtils.hasText(model.getApiKey())) {
            throw new ResearchException("研究模型配置不完整");
        }
        return model;
    }
}
