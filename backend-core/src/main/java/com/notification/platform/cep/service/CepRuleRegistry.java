package com.notification.platform.cep.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notification.platform.cep.model.CepRuleDefinition;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class CepRuleRegistry {

    private static final Logger log = LoggerFactory.getLogger(CepRuleRegistry.class);

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final String rulesFilePath;

    @Getter
    private final List<CepRuleDefinition> activeRules = new CopyOnWriteArrayList<>();

    public CepRuleRegistry(
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper,
            @Value("${notification.cep.rules-file:classpath:rules-config.json}") String rulesFilePath) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.rulesFilePath = rulesFilePath;
    }

    @PostConstruct
    public void init() {
        loadRules();
    }

    public synchronized void loadRules() {
        try {
            log.info("Loading CEP rules from path: {}", rulesFilePath);
            Resource resource = resourceLoader.getResource(rulesFilePath);
            try (InputStream is = resource.getInputStream()) {
                JsonNode root = objectMapper.readTree(is);
                JsonNode rulesNode = root.get("rules");
                if (rulesNode != null && rulesNode.isArray()) {
                    List<CepRuleDefinition> parsedRules = objectMapper.convertValue(
                            rulesNode,
                            new TypeReference<List<CepRuleDefinition>>() {}
                    );
                    setRules(parsedRules);
                } else {
                    log.warn("No 'rules' array found in {}", rulesFilePath);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load CEP rules from {}: {}", rulesFilePath, e.getMessage(), e);
        }
    }

    public synchronized void setRules(List<CepRuleDefinition> newRules) {
        activeRules.clear();
        if (newRules != null) {
            List<CepRuleDefinition> sorted = new ArrayList<>(newRules);
            sorted.sort(Comparator.comparingInt(CepRuleDefinition::getPriority));
            activeRules.addAll(sorted);
        }
        log.info("Successfully loaded {} CEP rules into active registry.", activeRules.size());
    }

    public Optional<CepRuleDefinition> getRuleById(String ruleId) {
        return activeRules.stream()
                .filter(r -> r.getId().equalsIgnoreCase(ruleId))
                .findFirst();
    }
}
