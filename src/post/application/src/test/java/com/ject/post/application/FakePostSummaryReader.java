package com.ject.post.application;

import com.ject.post.application.port.PostSummaryReader;

import java.util.List;

public class FakePostSummaryReader implements PostSummaryReader {

    private final List<Row> rows;
    private final List<Brief> briefs;
    public Condition receivedCondition;
    public Long receivedCursorId;
    public int receivedLimit;
    public List<Long> receivedIds;
    public int findBriefsCalls;

    public FakePostSummaryReader(List<Row> rows) {
        this(rows, List.of());
    }

    public FakePostSummaryReader(List<Row> rows, List<Brief> briefs) {
        this.rows = rows;
        this.briefs = briefs;
    }

    @Override
    public List<Row> findHomePage(Condition condition, Long cursorId, int limit) {
        this.receivedCondition = condition;
        this.receivedCursorId = cursorId;
        this.receivedLimit = limit;
        return rows.stream().limit(limit).toList();
    }

    @Override
    public List<Brief> findBriefs(List<Long> ids) {
        this.findBriefsCalls++;
        this.receivedIds = ids;
        return briefs.stream()
                .filter(brief -> ids.contains(brief.id()))
                .toList();
    }
}
