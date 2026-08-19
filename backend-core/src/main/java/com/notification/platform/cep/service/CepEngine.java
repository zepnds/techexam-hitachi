package com.notification.platform.cep.service;

import com.notification.platform.cep.evaluator.CepRuleEvaluator;
import com.notification.platform.cep.model.CepAction;
import com.notification.platform.cep.model.CepEvaluationResult;
import com.notification.platform.cep.model.CepRuleDefinition;
import com.notification.platform.cep.model.CepRuleType;
import com.notification.platform.domain.model.Notification;
import com.notification.platform.domain.model.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class CepEngine {

    private static final Logger log = LoggerFactory.getLogger(CepEngine.class);

    private final CepRuleRegistry ruleRegistry;
    private final Map<CepRuleType, CepRuleEvaluator> evaluators = new EnumMap<>(CepRuleType.class);

    public CepEngine(CepRuleRegistry ruleRegistry, List<CepRuleEvaluator> evaluatorList) {
        this.ruleRegistry = ruleRegistry;
        for (CepRuleEvaluator evaluator : evaluatorList) {
            this.evaluators.put(evaluator.supportsType(), evaluator);
            log.info("Registered CEP evaluator for type: {}", evaluator.supportsType());
        }
    }

    public CepEvaluationResult evaluate(Notification notification, UserProfile profile, Instant now) {
        List<CepRuleDefinition> rules = ruleRegistry.getActiveRules();
        log.debug("Evaluating {} active CEP rules for notification id: {}", rules.size(), notification.getId());

        for (CepRuleDefinition rule : rules) {
            if (!rule.isEnabled()) {
                continue;
            }

            CepRuleEvaluator evaluator = evaluators.get(rule.getRuleType());
            if (evaluator == null) {
                log.warn("No evaluator registered for rule type: {}", rule.getRuleType());
                continue;
            }

            CepEvaluationResult result = evaluator.evaluate(notification, profile, rule, now);
            if (result.getAction() != CepAction.PASS) {
                log.info("Notification {} intercepted by rule '{}' [{}] with action {}",
                        notification.getId(), rule.getId(), rule.getName(), result.getAction());
                return result;
            }
        }

        return CepEvaluationResult.pass();
    }
}
