package com.elevance.automatiam.mcp.clients.oneiam;

import com.elevance.automatiam.mcp.model.PageMetadata;
import com.elevance.automatiam.mcp.model.Pagination;

import java.util.List;

/**
 * In-memory paging for the synthetic adapters. Real adapters page upstream; thi […]
 * behaviour matches the contract rather than returning everything at once.
 */
final class MockPaging {

    private MockPaging() {
    }

    static <T> List<T> slice(List<T> all, Pagination pagination) {
        long offset = (long) pagination.page() * pagination.size();
        int from = (int) Math.min(offset, all.size());
        int to = from + Math.min(pagination.size(), all.size() - from);
        return all.subList(from, to);
    }

    static PageMetadata metadata(List<?> all, List<?> slice, Pagination pagination) {
        return PageMetadata.of(pagination, slice.size(), all.size());
    }
}
