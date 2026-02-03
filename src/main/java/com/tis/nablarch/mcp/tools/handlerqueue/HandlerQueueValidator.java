package com.tis.nablarch.mcp.tools.handlerqueue;

import com.tis.nablarch.mcp.knowledge.NablarchKnowledgeBase;
import com.tis.nablarch.mcp.knowledge.model.HandlerConstraintEntry;
import com.tis.nablarch.mcp.knowledge.model.HandlerEntry;
import com.tis.nablarch.mcp.tools.handlerqueue.model.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ハンドラキュー制約検証クラス。
 */
@Component
public class HandlerQueueValidator {
    private final NablarchKnowledgeBase knowledgeBase;

    public HandlerQueueValidator(NablarchKnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    public ValidationResult validate(String appType, List<HandlerEntry> orderedHandlers) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> handlerNames = orderedHandlers.stream().map(h -> h.name).collect(Collectors.toList());

        checkRequiredHandlers(appType, handlerNames, errors);
        checkOrderingConstraints(orderedHandlers, errors);
        checkCompatibility(handlerNames, warnings);
        checkRoutingPosition(handlerNames, errors);

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    private void checkRequiredHandlers(String appType, List<String> handlerNames, List<String> errors) {
        Set<String> providedNames = new HashSet<>(handlerNames);
        for (HandlerEntry req : knowledgeBase.getRequiredHandlers(appType)) {
            if (!providedNames.contains(req.name)) {
                errors.add("必須ハンドラが不足: " + req.name + " (" + req.description + ")");
            }
        }
    }

    private void checkOrderingConstraints(List<HandlerEntry> handlers, List<String> errors) {
        Map<String, Integer> positionMap = new HashMap<>();
        for (int i = 0; i < handlers.size(); i++) positionMap.put(handlers.get(i).name, i);

        List<HandlerConstraintEntry> constraints = knowledgeBase.getAllHandlerConstraints();
        for (HandlerEntry handler : handlers) {
            Integer currentPos = positionMap.get(handler.name);
            HandlerConstraintEntry constraint = constraints.stream()
                    .filter(c -> c.handler.equals(handler.name)).findFirst().orElse(null);

            if (constraint != null) {
                checkMustBefore(handler.name, constraint.mustBefore, currentPos, positionMap, errors);
                checkMustAfter(handler.name, constraint.mustAfter, currentPos, positionMap, errors);
            }
            if (handler.constraints != null) {
                checkMustBefore(handler.name, handler.constraints.mustBefore, currentPos, positionMap, errors);
                checkMustAfter(handler.name, handler.constraints.mustAfter, currentPos, positionMap, errors);
            }
        }
    }

    private void checkMustBefore(String handlerName, List<String> mustBefore, Integer currentPos,
            Map<String, Integer> positionMap, List<String> errors) {
        if (mustBefore == null) return;
        for (String target : mustBefore) {
            Integer targetPos = positionMap.get(target);
            if (targetPos != null && currentPos >= targetPos) {
                String msg = String.format("順序違反: %s (位置%d) は %s (位置%d) より前に配置すべき",
                        handlerName, currentPos + 1, target, targetPos + 1);
                if (!errors.contains(msg)) errors.add(msg);
            }
        }
    }

    private void checkMustAfter(String handlerName, List<String> mustAfter, Integer currentPos,
            Map<String, Integer> positionMap, List<String> errors) {
        if (mustAfter == null) return;
        for (String target : mustAfter) {
            Integer targetPos = positionMap.get(target);
            if (targetPos != null && currentPos <= targetPos) {
                String msg = String.format("順序違反: %s (位置%d) は %s (位置%d) より後に配置すべき",
                        handlerName, currentPos + 1, target, targetPos + 1);
                if (!errors.contains(msg)) errors.add(msg);
            }
        }
    }

    private void checkCompatibility(List<String> handlerNames, List<String> warnings) {
        Set<String> providedNames = new HashSet<>(handlerNames);
        for (HandlerConstraintEntry c : knowledgeBase.getAllHandlerConstraints()) {
            if (!providedNames.contains(c.handler) || c.incompatibleWith == null) continue;
            for (String incompatible : c.incompatibleWith) {
                if (providedNames.contains(incompatible)) {
                    warnings.add(String.format("互換性警告: %s と %s は同時使用非推奨", c.handler, incompatible));
                }
            }
        }
    }

    private void checkRoutingPosition(List<String> handlerNames, List<String> errors) {
        int routingIndex = -1;
        for (int i = 0; i < handlerNames.size(); i++) {
            String name = handlerNames.get(i);
            if ("PackageMapping".equals(name) || "RequestPathJavaPackageMapping".equals(name))
                routingIndex = i;
        }
        if (routingIndex >= 0 && routingIndex != handlerNames.size() - 1) {
            errors.add("ルーティングハンドラは最後に配置すべきです (現在位置: "
                    + (routingIndex + 1) + "/" + handlerNames.size() + ")");
        }
    }
}
