package dev.haotangyuan.knownote.research.service;

import dev.haotangyuan.knownote.research.api.dto.req.SendMessageReqDTO;
import dev.haotangyuan.knownote.research.api.dto.resp.CreateResearchRespDTO;
import dev.haotangyuan.knownote.research.api.dto.resp.ResearchMessageRespDTO;
import dev.haotangyuan.knownote.research.api.dto.resp.ResearchStatusRespDTO;
import dev.haotangyuan.knownote.research.api.dto.resp.SendMessageRespDTO;

import java.util.List;

/**
 * 研究服务接口
 */
public interface ResearchService {

    CreateResearchRespDTO createResearch(Integer num);

    List<ResearchStatusRespDTO> getResearchList();

    ResearchStatusRespDTO getResearchStatus(String researchId);

    ResearchMessageRespDTO getResearchMessages(String researchId);

    SendMessageRespDTO sendMessage(String researchId, SendMessageReqDTO sendMessageReqDTO);
}
