package com.tis.nablarch.mcp.tools.handlerqueue;

import com.tis.nablarch.mcp.knowledge.model.HandlerConstraintEntry;
import com.tis.nablarch.mcp.knowledge.model.HandlerEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * ハンドラキューのトポロジカルソートを行うクラス。
 */
@Component
public class HandlerQueueSorter {
    private static final Logger log = LoggerFactory.getLogger(HandlerQueueSorter.class);

    public List<HandlerEntry> sort(List<HandlerEntry> handlers,
            List<HandlerConstraintEntry> constraints) {
        if (handlers.isEmpty()) return List.of();
        Map<String, HandlerEntry> handlerMap = new LinkedHashMap<>();
        for (HandlerEntry h : handlers) handlerMap.put(h.name, h);
        Map<String, Set<String>> graph = buildGraph(handlerMap.keySet(), constraints, handlers);
        return kahnSort(handlerMap, graph, constraints);
    }

    public List<HandlerEntry> sortWithFixes(List<HandlerEntry> handlers,
            List<HandlerConstraintEntry> constraints, List<String> errors) {
        log.info("制約違反を修正して再ソート: {} 件", errors.size());
        return sort(handlers, constraints);
    }

    private Map<String, Set<String>> buildGraph(Set<String> handlerNames,
            List<HandlerConstraintEntry> constraints, List<HandlerEntry> handlers) {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        for (String name : handlerNames) graph.put(name, new LinkedHashSet<>());
        Map<String, HandlerConstraintEntry> constraintMap = constraints.stream()
                .collect(Collectors.toMap(c -> c.handler, c -> c, (a, b) -> a));

        for (String name : handlerNames) {
            HandlerConstraintEntry c = constraintMap.get(name);
            if (c != null) {
                if (c.mustBefore != null) {
                    for (String target : c.mustBefore)
                        if (handlerNames.contains(target)) graph.get(name).add(target);
                }
                if (c.mustAfter != null) {
                    for (String target : c.mustAfter)
                        if (handlerNames.contains(target))
                            graph.computeIfAbsent(target, k -> new LinkedHashSet<>()).add(name);
                }
            }
        }
        for (HandlerEntry h : handlers) {
            if (h.constraints == null || !handlerNames.contains(h.name)) continue;
            if (h.constraints.mustBefore != null) {
                for (String target : h.constraints.mustBefore)
                    if (handlerNames.contains(target)) graph.get(h.name).add(target);
            }
            if (h.constraints.mustAfter != null) {
                for (String target : h.constraints.mustAfter)
                    if (handlerNames.contains(target))
                        graph.computeIfAbsent(target, k -> new LinkedHashSet<>()).add(h.name);
            }
        }
        return graph;
    }

    private List<HandlerEntry> kahnSort(Map<String, HandlerEntry> handlerMap,
            Map<String, Set<String>> graph, List<HandlerConstraintEntry> constraints) {
        Set<String> outerHandlers = new HashSet<>();
        Set<String> innerHandlers = new HashSet<>();
        for (HandlerConstraintEntry c : constraints) {
            if ("must_be_outer".equals(c.rule)) outerHandlers.add(c.handler);
            else if ("must_be_inner".equals(c.rule)) innerHandlers.add(c.handler);
        }

        Map<String, Integer> inDegree = new LinkedHashMap<>();
        for (String name : handlerMap.keySet()) inDegree.put(name, 0);
        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            for (String target : entry.getValue())
                if (inDegree.containsKey(target)) inDegree.merge(target, 1, Integer::sum);
        }

        PriorityQueue<String> queue = new PriorityQueue<>((a, b) -> {
            if (outerHandlers.contains(a) != outerHandlers.contains(b))
                return outerHandlers.contains(a) ? -1 : 1;
            if (innerHandlers.contains(a) != innerHandlers.contains(b))
                return innerHandlers.contains(a) ? 1 : -1;
            HandlerEntry ha = handlerMap.get(a);
            HandlerEntry hb = handlerMap.get(b);
            return Integer.compare(ha != null ? ha.order : Integer.MAX_VALUE,
                    hb != null ? hb.order : Integer.MAX_VALUE);
        });

        for (Map.Entry<String, Integer> e : inDegree.entrySet())
            if (e.getValue() == 0) queue.add(e.getKey());

        List<HandlerEntry> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            HandlerEntry h = handlerMap.get(current);
            if (h != null) result.add(h);
            for (String neighbor : graph.getOrDefault(current, Set.of())) {
                if (!inDegree.containsKey(neighbor)) continue;
                int newDegree = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, newDegree);
                if (newDegree == 0) queue.add(neighbor);
            }
        }

        if (result.size() != handlerMap.size()) {
            List<String> remaining = handlerMap.keySet().stream()
                    .filter(n -> result.stream().noneMatch(h -> h.name.equals(n)))
                    .collect(Collectors.toList());
            log.error("循環依存を検出: {}", remaining);
            remaining.stream().map(handlerMap::get).filter(Objects::nonNull)
                    .sorted(Comparator.comparingInt(h -> h.order)).forEach(result::add);
        }
        return result;
    }
}
