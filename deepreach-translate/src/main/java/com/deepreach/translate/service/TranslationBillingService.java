package com.deepreach.translate.service;

import java.math.BigDecimal;

public interface TranslationBillingService {

    BigDecimal deduct(Long userId, long totalTokens);

    BigDecimal resolveUnitPrice();
}
