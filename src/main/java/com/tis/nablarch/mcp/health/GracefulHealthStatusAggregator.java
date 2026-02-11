package com.tis.nablarch.mcp.health;

import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * グレースフルデグレード対応のStatusAggregator。
 *
 * <p>KnowledgeBase（コア機能）がUPであれば、DBやEmbeddingモデルが
 * DOWNでも全体ステータスをUPに維持する。
 * これにより、STDIOモード（DB不要）やONNXモデル未配置環境でも
 * ヘルスチェックが正常応答する。</p>
 *
 * <p>全インジケータがDOWNの場合のみ全体をDOWNとする。</p>
 */
@Component
public class GracefulHealthStatusAggregator implements StatusAggregator {

    @Override
    public Status getAggregateStatus(Set<Status> statuses) {
        if (statuses.stream().anyMatch(s -> Status.UP.equals(s))) {
            return Status.UP;
        }
        return Status.DOWN;
    }
}
