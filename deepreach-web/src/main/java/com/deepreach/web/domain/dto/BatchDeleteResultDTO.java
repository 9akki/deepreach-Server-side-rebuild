package com.deepreach.web.domain.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.ArrayList;

/**
 * 批量删除结果DTO
 *
 * 用于返回批量操作的详细结果，包括成功和失败的记录
 *
 * @author DeepReach Team
 * @version 1.0
 * @since 2025-10-31
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchDeleteResultDTO {

    /**
     * 总数量
     */
    private int totalCount;

    /**
     * 成功数量
     */
    private int successCount;

    /**
     * 失败数量
     */
    private int failureCount;

    /**
     * 成功删除的ID列表
     */
    private List<Long> successIds;

    /**
     * 失败的ID和原因列表
     */
    private List<DeleteFailureInfo> failures;

    /**
     * 操作是否全部成功
     */
    public boolean isAllSuccess() {
        return failureCount == 0;
    }

    /**
     * 操作是否有部分成功
     */
    public boolean isPartialSuccess() {
        return successCount > 0 && failureCount > 0;
    }

    /**
     * 操作是否完全失败
     */
    public boolean isAllFailure() {
        return successCount == 0 && failureCount > 0;
    }

    /**
     * 删除失败信息
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DeleteFailureInfo {
        /**
         * 失败的ID
         */
        private Long id;

        /**
         * 失败原因
         */
        private String reason;
    }

    /**
     * 创建成功的批量删除结果
     */
    public static BatchDeleteResultDTO success(int totalCount, List<Long> successIds) {
        BatchDeleteResultDTO result = new BatchDeleteResultDTO();
        result.setTotalCount(totalCount);
        result.setSuccessCount(successIds.size());
        result.setFailureCount(0);
        result.setSuccessIds(successIds);
        result.setFailures(new ArrayList<>());
        return result;
    }

    /**
     * 创建部分成功的批量删除结果
     */
    public static BatchDeleteResultDTO partial(int totalCount, List<Long> successIds, List<DeleteFailureInfo> failures) {
        BatchDeleteResultDTO result = new BatchDeleteResultDTO();
        result.setTotalCount(totalCount);
        result.setSuccessCount(successIds.size());
        result.setFailureCount(failures.size());
        result.setSuccessIds(successIds);
        result.setFailures(failures);
        return result;
    }
}