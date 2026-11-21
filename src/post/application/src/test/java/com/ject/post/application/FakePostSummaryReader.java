package com.ject.post.application;

import com.ject.post.application.port.PostSummaryReader;

import java.util.List;

public class FakePostSummaryReader implements PostSummaryReader {

    private final List<Row> rows;
    public Condition receivedCondition;
    public Long receivedCursorId;
    public int receivedLimit;

    public FakePostSummaryReader(List<Row> rows) {
        this.rows = rows;
    }

    @Override
    public List<Row> findHomePage(Condition condition, Long cursorId, int limit) {
        this.receivedCondition = condition;
        this.receivedCursorId = cursorId;
        this.receivedLimit = limit;
        return rows.stream().limit(limit).toList();
    }
}
