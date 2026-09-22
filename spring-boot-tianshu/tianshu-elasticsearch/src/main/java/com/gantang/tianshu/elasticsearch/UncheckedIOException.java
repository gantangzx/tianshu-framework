package com.gantang.tianshu.elasticsearch;

import java.io.IOException;

/**
 * Unchecked wrapper around a checked {@link IOException} raised by the Elasticsearch
 * client so the framework service can be used freely from both servlet and reactive
 * call sites.
 */
public class UncheckedIOException extends RuntimeException {

    public UncheckedIOException(IOException cause) {
        super(cause);
    }

    public UncheckedIOException(String message) {
        super(message);
    }
}
