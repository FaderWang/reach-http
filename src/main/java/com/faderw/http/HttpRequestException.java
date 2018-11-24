package com.faderw.http;

import java.io.IOException;

/**
 * @author FaderW
 * 2018/11/22
 */

public class HttpRequestException extends RuntimeException {

    public HttpRequestException(final IOException cause) {
        super(cause);
    }

    public HttpRequestException(final String message) {
        super(message);
    }

    @Override
    public IOException getCause() {
        return (IOException) super.getCause();
    }
}
