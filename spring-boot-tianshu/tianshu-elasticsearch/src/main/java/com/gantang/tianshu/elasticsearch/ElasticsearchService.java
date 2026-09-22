package com.gantang.tianshu.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.ObjectBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * Thin, stack-agnostic wrapper around the official synchronous
 * {@link ElasticsearchClient} providing common single-document and paged operations.
 * Checked {@link IOException IOExceptions} are rethrown as {@link UncheckedIOException}.
 */
public class ElasticsearchService {

    private final ElasticsearchClient client;

    public ElasticsearchService(ElasticsearchClient client) {
        this.client = client;
    }

    /**
     * @return the underlying official client for operations not covered here
     */
    public ElasticsearchClient client() {
        return this.client;
    }

    /**
     * Indexes a document under an explicit id.
     */
    public <T> void index(String index, String id, T document) {
        try {
            this.client.index(b -> b.index(index).id(id).document(document));
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Retrieves a document by id.
     *
     * @return the document, or {@code null} if absent
     */
    public <T> T get(String index, String id, Class<T> type) {
        try {
            GetResponse<T> response = this.client.get(g -> g.index(index).id(id), type);
            return response.found() ? response.source() : null;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @return whether a document with the given id exists
     */
    public boolean exists(String index, String id) {
        try {
            return this.client.exists(e -> e.index(index).id(id)).value();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Removes a document by id.
     */
    public boolean delete(String index, String id) {
        try {
            return this.client.delete(d -> d.index(index).id(id)).result().name().equals("Deleted");
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Indexes a batch of documents.
     *
     * @param documents documents to index
     */
    public <T> void bulkIndex(String index, Collection<T> documents) {
        try {
            BulkResponse response = this.client.bulk(b -> {
                for (T document : documents) {
                    b.operations(o -> o.index(i -> i.index(index).document(document)));
                }
                return b;
            });
            if (response.errors()) {
                throw new UncheckedIOException(
                        new IOException("Bulk index completed with errors: " + response));
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Searches with a caller-built query.
     *
     * @param queryFn query builder function, e.g. {@code q -> q.matchAll(m -> m)}
     */
    public <T> PageResult<T> search(String index, int pageNo, int pageSize,
            Function<Query.Builder, ObjectBuilder<Query>> queryFn, Class<T> type) {
        try {
            SearchResponse<T> response = this.client.search(s -> s
                    .index(index)
                    .from(pageNo * pageSize)
                    .size(pageSize)
                    .query(queryFn), type);
            return toPage(response, pageNo, pageSize);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Returns every document in the index for the given page.
     */
    public <T> PageResult<T> findAll(String index, int pageNo, int pageSize, Class<T> type) {
        return search(index, pageNo, pageSize, q -> q.matchAll(m -> m), type);
    }

    /**
     * Runs a query string search against the index.
     */
    public <T> PageResult<T> searchByQueryString(String index, String queryString,
            int pageNo, int pageSize, Class<T> type) {
        return search(index, pageNo, pageSize,
                q -> q.queryString(qs -> qs.query(queryString)), type);
    }

    private <T> PageResult<T> toPage(SearchResponse<T> response, int pageNo, int pageSize) {
        List<T> documents = new ArrayList<>();
        for (Hit<T> hit : response.hits().hits()) {
            documents.add(hit.source());
        }
        long total = response.hits().total() != null ? response.hits().total().value() : 0L;
        return new PageResult<>(documents, total, pageNo, pageSize);
    }
}
