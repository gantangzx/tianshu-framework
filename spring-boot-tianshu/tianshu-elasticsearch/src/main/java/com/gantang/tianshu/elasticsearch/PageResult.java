package com.gantang.tianshu.elasticsearch;

import java.util.List;

/**
 * A page of Elasticsearch results.
 *
 * @param list     documents on the current page
 * @param total    total number of matching documents
 * @param pageNo   zero-based page index that was requested
 * @param pageSize page size that was requested
 * @param <T>      document type
 */
public record PageResult<T>(List<T> list, long total, int pageNo, int pageSize) {
}
