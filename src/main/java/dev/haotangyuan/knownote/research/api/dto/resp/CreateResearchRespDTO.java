package dev.haotangyuan.knownote.research.api.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 创建研究返回
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateResearchRespDTO {
    private List<String> researchIds;
}
