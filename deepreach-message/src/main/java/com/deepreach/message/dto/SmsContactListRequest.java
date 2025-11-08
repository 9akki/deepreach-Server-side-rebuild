package com.deepreach.message.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SmsContactListRequest {

    @NotNull
    private Long taskId;

    @Min(1)
    private Integer page = 1;

    @Min(1)
    private Integer size = 20;

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
