package com.notification.platform.cep.evaluator;

import com.notification.platform.cep.model.CepEvaluationResult;
import com.notification.platform.cep.model.CepRuleDefinition;
import com.notification.platform.cep.model.CepRuleType;
import com.notification.platform.domain.model.Notification;
import com.notification.platform.domain.model.UserProfile;

import java.time.Instant;

public interface CepRuleEvaluator {

    CepRuleType supportsType();
    CepEvaluationResult evaluate(Notification notification, UserProfile profile, CepRuleDefinition rule, Instant now);
}
