package com.deepreach.common.core.mq.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event payload describing a translation billing charge.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TranslationChargeEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier of the request/event for idempotency.
     */
    private String eventId;

    /**
     * User that initiated the translation request.
     */
    private Long requestUserId;

    /**
     * Account that will actually be charged.
     */
    private Long chargeUserId;

    /**
     * Operator user that performs the deduction (if any).
     */
    private Long operatorUserId;

    /**
     * Total token usage for the translation request.
     */
    private Long totalTokens;

    /**
     * Final amount that should be deducted.
     */
    private BigDecimal amount;

    /**
     * Billing metadata propagated to balance service.
     */
    private String billType;

    private String billingType;

    private String businessType;

    /**
     * Human readable description of the deduction.
     */
    private String description;

    private String remark;

    /**
     * Pricing mode (TOKEN / BY_TIMES / etc.).
     */
    private String pricingMode;

    /**
     * Timestamp when usage occurred.
     */
    private Instant occurredAt;

    /**
     * 已自动重试的次数（DLQ 补偿使用）
     */
    private Integer retryCount;
}
