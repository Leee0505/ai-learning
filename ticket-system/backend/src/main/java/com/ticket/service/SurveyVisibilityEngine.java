package com.ticket.service;

import com.ticket.dto.response.SurveyTemplateResponse;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SurveyVisibilityEngine {

    /**
     * Evaluate all visibility rules for a given set of answers.
     * Returns a Set of target IDs that should be HIDDEN.
     * Key format: "PAGE:123", "SECTION:456", "QUESTION:789"
     */
    public Set<String> evaluateHidden(SurveyTemplateResponse template, Map<Long, Object> answers) {
        Set<String> hidden = new HashSet<>();

        for (SurveyTemplateResponse.PageResponse page : template.getPages()) {
            if (!evaluateRules(page.getVisibilityRules(), answers)) {
                hidden.add("PAGE:" + page.getId());
            }
            for (SurveyTemplateResponse.SectionResponse section : page.getSections()) {
                if (!evaluateRules(section.getVisibilityRules(), answers)) {
                    hidden.add("SECTION:" + section.getId());
                }
                for (SurveyTemplateResponse.QuestionResponse q : section.getQuestions()) {
                    if (!evaluateRules(q.getVisibilityRules(), answers)) {
                        hidden.add("QUESTION:" + q.getId());
                    }
                }
            }
        }
        return hidden;
    }

    /**
     * Check if a group of rules is satisfied.
     * Same logicGroup → AND, different groups → OR.
     */
    private boolean evaluateRules(List<SurveyTemplateResponse.VisibilityRuleResponse> rules,
                                   Map<Long, Object> answers) {
        if (rules == null || rules.isEmpty()) return true;

        Map<Integer, List<SurveyTemplateResponse.VisibilityRuleResponse>> grouped = new HashMap<>();
        for (var r : rules) {
            grouped.computeIfAbsent(r.getLogicGroup(), k -> new ArrayList<>()).add(r);
        }

        // At least one group must be fully satisfied (OR across groups)
        for (List<SurveyTemplateResponse.VisibilityRuleResponse> group : grouped.values()) {
            boolean groupSatisfied = true;
            for (var r : group) {
                if (!evaluateSingleRule(r, answers)) {
                    groupSatisfied = false;
                    break;
                }
            }
            if (groupSatisfied) return true;
        }
        return false;
    }

    private boolean evaluateSingleRule(SurveyTemplateResponse.VisibilityRuleResponse rule,
                                        Map<Long, Object> answers) {
        Object answer = answers.get(rule.getSourceQuestionId());
        String answerStr = answer != null ? answer.toString() : "";

        // Operators that don't need a value
        switch (rule.getOp()) {
            case "answered":    return answer != null && !answerStr.isEmpty();
            case "not_answered": return answer == null || answerStr.isEmpty();
            case "is_empty":    return answerStr.isEmpty();
            case "not_empty":   return !answerStr.isEmpty();
        }

        // From here, answer must exist
        if (answerStr.isEmpty()) return false;

        String value = rule.getValue();
        if (value == null) value = "";

        switch (rule.getOp()) {
            case "eq": return answerStr.equals(value);
            case "neq": return !answerStr.equals(value);
            case "contains": return answerStr.contains(value);
            case "not_contains": return !answerStr.contains(value);
            case "in": return Arrays.asList(value.split(",")).contains(answerStr);
            case "not_in": return !Arrays.asList(value.split(",")).contains(answerStr);
            case "gt": return toDouble(answerStr) > toDouble(value);
            case "gte": return toDouble(answerStr) >= toDouble(value);
            case "lt": return toDouble(answerStr) < toDouble(value);
            case "lte": return toDouble(answerStr) <= toDouble(value);
            default: return true;
        }
    }

    private double toDouble(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
    }
}
