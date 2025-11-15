package com.deepreach.web.service;

import com.deepreach.web.domain.dto.AiSuggestionRequest;
import java.util.List;

public interface CommunicateHistoryService {

    CommunicateHistorySnapshot loadSnapshot(Long userId,
                                            String contactUsername,
                                            Integer platformId);

    void mergeAndSaveAsync(CommunicateHistorySnapshot snapshot,
                           List<AiSuggestionRequest.ChatRecord> incomingRecords);

    record CommunicateHistorySnapshot(Long userId,
                                      String contactUsername,
                                      Integer platformId,
                                      String historyJson,
                                      String chatPortrait) {
    }
}
