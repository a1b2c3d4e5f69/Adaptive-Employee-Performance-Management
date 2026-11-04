package com.aepm.model;

import java.util.List;

public record AnonymousReportPage(List<AnonymousReport> rows, long totalCount, int limit, int offset) {
    public AnonymousReportPage { rows = List.copyOf(rows); }
}
