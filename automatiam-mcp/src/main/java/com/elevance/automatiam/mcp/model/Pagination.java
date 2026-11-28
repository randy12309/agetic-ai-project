package com.elevance.automatiam.mcp.model;

import com.elevance.automatiam.mcp.exception.InvalidToolArgumentException;

/**
 * Validated page coordinates. The maximum page size is a server-side cap, not a […]
 * it is the control that stops a tool being used for bulk enumeration.
 *
 * @param page zero-based page index
 * @param size requested page size, capped at {@link #MAX_PAGE_SIZE)
 */
public record Pagination(int page, int size) {

    public static final int MAX_PAGE_SIZE = 50;
    public static final int DEFAULT_PAGE_SIZE = 20;

    public Pagination {
        if (page < 0) {
            throw new InvalidToolArgumentException("page", "must not be negative");
        }
        if (size < 1) {
            throw new InvalidToolArgumentException("size", "must be at least 1");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new InvalidToolArgumentException("size", "must not exceed " + MAX_PAGE_SIZE);
        }
    }

    /**
     * Builds pagination from optional tool arguments, applying defaults when th […]
     *
     * @throws InvalidToolArgumentException if a supplied value is outside the p […]
     */
    public static Pagination of(Integer page, Integer size) {
        return new Pagination(
                page == null ? 0 : page,
                size == null ? DEFAULT_PAGE_SIZE : size);
    }
}
