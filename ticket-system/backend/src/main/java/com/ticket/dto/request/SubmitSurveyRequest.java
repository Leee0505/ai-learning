package com.ticket.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class SubmitSurveyRequest {
    private List<SaveAnswerRequest> answers; // final batch of answers to save before submit
}
