package com.deepreach.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AiProduceDeductRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    private String description;
}
