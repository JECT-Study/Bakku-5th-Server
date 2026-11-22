package com.ject.user.application;

import com.ject.user.application.port.ReportRepository;
import com.ject.user.domain.domain.Report;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class FakeReportRepository implements ReportRepository {

    private final List<Report> store = new ArrayList<>();

    @Override
    public Optional<Long> saveIfAbsent(Report report) {
        boolean exists = store.stream().anyMatch(saved -> Objects.equals(saved.getReporterId(), report.getReporterId())
                && saved.getTargetType() == report.getTargetType()
                && Objects.equals(saved.getTargetId(), report.getTargetId()));
        if (exists) {
            return Optional.empty();
        }
        store.add(report);
        return Optional.of((long) store.size());
    }

    List<Report> saved() {
        return store;
    }
}
