package com.deepreach.translate.service;

import com.deepreach.translate.dto.OriginalTextRequest;
import com.deepreach.translate.dto.OriginalTextResponse;
import com.deepreach.translate.dto.TranslateRequest;
import com.deepreach.translate.dto.TranslateResponse;
import java.util.Optional;

public interface TranslationService {

    TranslateResponse translate(TranslateRequest request);

    Optional<OriginalTextResponse> getOriginalText(OriginalTextRequest request);
}
