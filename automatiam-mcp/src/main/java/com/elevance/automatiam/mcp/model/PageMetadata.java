package com.elevance.automatiam.mcp.model;

/**
 * Pagination envelope returned with every list-shaped tool result.
 *
 * @param page          zero-based index of this page
 * @param size          number of elements requested
 * @param returnedCount number of elements actually in this page
 * @param totalElements total matching elements the caller is authorized to see […]
 * @param hasNext       whether a further page exists
 */
public record PageMetadata(int page, int size, int returnedCount, long totalElements, boolean hasNext) {

    public static PageMetadata of(Pagination pagination, int returnedCount, long totalElements) {
        long consumed = (long) pagination.page() * pagination.size() + returnedCount;
        return new PageMetadata(
                pagination.page(),
                pagination.size(),
                returnedCount,
                totalElements,
                consumed < totalElements);
    }
}
