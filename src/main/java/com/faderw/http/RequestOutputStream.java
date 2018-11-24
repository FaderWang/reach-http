package com.faderw.http;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

/**
 * FaderW
 * created on 2018/11/25
 */
public class RequestOutputStream  extends BufferedOutputStream {

    private final CharsetEncoder encoder;

    public RequestOutputStream(final OutputStream outputStream, final String charset, final int bufferSize) {
        super(outputStream, bufferSize);
        encoder = Charset.forName(charset).newEncoder();
    }

    public RequestOutputStream write(final String value) throws IOException {
        final ByteBuffer byteBuffer = encoder.encode(CharBuffer.wrap(value));
        super.write(byteBuffer.array(), 0, byteBuffer.limit());

        return this;
    }

}