package com.faderw.http;


import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static com.faderw.http.HttpRequestConst.*;

/**
 * @author FaderW
 * 2018/11/22
 */

public class HttpRequest {


    public HttpRequest(final URL url, final String requestMethod) {
        this.url = url;
        this.requestMethod = requestMethod;
    }

    public HttpRequest(final CharSequence url, final String requestMethod) {
        try {
            this.url = new URL(url.toString());
        } catch (MalformedURLException e) {
            throw new HttpRequestException(e);
        }

        this.requestMethod = requestMethod;
    }

    public interface ConnectionFactory {
        /**
         * open urlConnection{@link HttpURLConnection} for special url{@link URL}
         */
        HttpURLConnection create(URL url) throws IOException;

        /**
         * open urlConnection{@link HttpURLConnection} for special url{@link URL}
         * and proxy{@link Proxy}
         */
        HttpURLConnection create(URL url, Proxy proxy) throws IOException;


        ConnectionFactory DEFAULT = new ConnectionFactory() {

            @Override
            public HttpURLConnection create(URL url) throws IOException {
                return (HttpURLConnection) url.openConnection();
            }

            @Override
            public HttpURLConnection create(URL url, Proxy proxy) throws IOException {
                return (HttpURLConnection) url.openConnection(proxy);
            }
        };

    }

    private static final int BUFFER_SIZE = 8132;
    private static final String DEFAULT_CHARSET = "UTF-8";
    private final URL url;
    private final String requestMethod;
    private HttpURLConnection connection = null;
    private boolean form;
    private boolean multipart;
    private boolean unprogress;
    private boolean ignoreCloseException = true;


    private String proxyHostName;
    private Integer proxyHostPort;

    private RequestOutputStream outputStream;


    private static ConnectionFactory connectionFactory = ConnectionFactory.DEFAULT;

    private HttpURLConnection createConnection() {
        final HttpURLConnection connection;
        try {
            if (proxyHostName != null && proxyHostPort != null) {
                connection = connectionFactory.create(url, new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHostName, proxyHostPort)));
            } else {
                connection = connectionFactory.create(url);
            }
            connection.setRequestMethod(requestMethod);
            return connection;
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    private String getValidCharset(final String charset) {
        if (charset != null && charset.length() > 0) {
            return charset;
        } else {
            return DEFAULT_CHARSET;
        }
    }

    public HttpURLConnection getConnection() {
        if (connection == null) {
            connection = createConnection();
        }
        return connection;
    }

    public HttpRequest userProxy(String proxyHostName, Integer proxyHostPort) {
        this.proxyHostName = proxyHostName;
        this.proxyHostPort = proxyHostPort;
        return this;
    }

    public HttpRequest openOutput() {
        if (outputStream != null) {
            return this;
        }
        getConnection().setDoOutput(true);
        final String charset = getParam(getConnection().getRequestProperty(HEADER_CONTENT_TYPE), PARAM_CHARSET);
        try {
            outputStream = new RequestOutputStream(getConnection().getOutputStream(), charset, BUFFER_SIZE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public static HttpRequest get(final CharSequence url) {
        return new HttpRequest(url, METHOD_GET);
    }

    public static HttpRequest get(final URL url) {
        return new HttpRequest(url, METHOD_GET);
    }

    public static HttpRequest post(final URL url) {
        return new HttpRequest(url, METHOD_POST);
    }

    public static HttpRequest post(final CharSequence url) {
        return new HttpRequest(url, METHOD_POST);
    }

    public static HttpRequest post(final CharSequence baseUrl,final Map<?, ?> params, final boolean encode) {
        String url = append(baseUrl, params);
        return post(encode ? encode(url) : url);
    }

    public static String encode(final CharSequence url) {
        URL parsed;
        try {
            parsed = new URL(url.toString());
        } catch (MalformedURLException e) {
            throw new HttpRequestException(e);
        }
        String host = parsed.getHost();
        int port = parsed.getPort();
        if (port != -1) {
            host = host + ':' + Integer.toString(port);
        }
        try {
            String encoded = new URI(parsed.getProtocol(), host, parsed.getPath(), parsed.getQuery(), null)
                    .toASCIIString();
            int paramStart = encoded.indexOf("?");
            if (paramStart > 0 && paramStart + 1 < encoded.length()) {
                encoded = encoded.substring(0, paramStart + 1)
                    + encoded.substring(paramStart + 1).replace("+", "%20");
            }

            return encoded;
        } catch (URISyntaxException e) {
            IOException io = new IOException("Parsing URI failed");
            io.initCause(e);
            throw new HttpRequestException(io);
		}
    }

    public HttpRequest form(Map<?, ?> params) {
        return form(params, DEFAULT_CHARSET);
    }

    public HttpRequest json(final String json) {
        contentType(CONTENT_TYPE_JSON, DEFAULT_CHARSET);
        send(json);
        return this;
    }

    public HttpRequest send(final CharSequence charSequence) {
        openOutput();
        try {
            outputStream.write(charSequence.toString());
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return this;
    }

    public HttpRequest part(final String name, final String part) {
        return part(name, null, null, part);
    }

    public HttpRequest part(final String name, final String filename, File file) {
        return this;
    }

    public HttpRequest part(final String name, final String filename, final String part) {
        return part(name, filename, null, part);
    }

    public HttpRequest part(final String name, final String filename, final String contentType, final String part) {
        try {
            startPart();
            writePartHeader(name, filename, contentType);
            outputStream.write(part);
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return this;
    }


    protected HttpRequest startPart() throws IOException {
        if (!multipart) {
            contentType(CONTENT_TYPE_MUITIPART + BOUNDARY, null);
            openOutput();
            outputStream.write("--" + BOUNDARY + CRLF);
        } else {
            outputStream.write("--"+ BOUNDARY + CRLF);
        }
        return this;
    }

    protected HttpRequest writePartHeader(final String name, final String filename, final String contentType) {
        final StringBuilder partBuilder = new StringBuilder();
        partBuilder.append("form-data;name=\"").append(name).append("\"");
        if (filename != null) {
            partBuilder.append(";filename=\"").append(filename).append("\"");
        }
        partHeader("Content-Disposition", partBuilder.toString());
        if (contentType != null) {
            partHeader(HEADER_CONTENT_TYPE, contentType);
        }
        return send(CRLF);
    }

    protected HttpRequest partHeader(final String header, final String value) {
        return send(header).send(":").send(value).send(CRLF);
    }



    public HttpRequest form(Map<?, ?> params, String charset) {
        if (params == null || params.isEmpty()) {
            return this;
        }
        charset = getValidCharset(charset);
        for (Entry<?, ?> entry : params.entrySet()) {
            form(entry.getKey(), entry.getValue(), charset);
        }
        return this;
    }

    public HttpRequest form(final Object key, final Object value, final String charset) {
        boolean first = !form;
        if (first) {
            contentType(CONTENT_TYPE_FORM, charset);
            form = true;
        }

        try {
            openOutput();
            if (!first) {
                outputStream.write('&');
            }
            outputStream.write(URLEncoder.encode(value.toString(), charset));
            outputStream.write("=");
            if (value != null) {
                outputStream.write(URLEncoder.encode(value.toString(), charset));
            }

        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return this;
    }

    public static String append(final CharSequence url, Map<?, ?> params) {
        String baseUrl = url.toString();
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }
        StringBuilder result = new StringBuilder();
        addPathSeparator(baseUrl, result);
        addParamPrefix(baseUrl, result);

        Entry<?, ?> entry;
        Iterator iterator = params.entrySet().iterator();
        entry = (Entry<?, ?>) iterator.next();
        addParam(entry.getKey(), entry.getValue(), result);
        
        while (iterator.hasNext()) {
            result.append("&");
            entry = (Entry<?, ?>) iterator.next();
            addParam(entry.getKey(), entry.getValue(), result);
        }

        return result.toString();
    }

    private static StringBuilder addPathSeparator(final String baseUrl, final StringBuilder result) {
        if (baseUrl.indexOf(":") + 2 == baseUrl.lastIndexOf("/")) {
            result.append("/");
        }

        return result;
    }

    private static StringBuilder addParamPrefix(final String baseUrl, final StringBuilder result) {
        int paramStart = baseUrl.indexOf("?");
        int length = baseUrl.length();
        if(paramStart < 0) {
            result.append("?");
        } else if (paramStart < length -1 && baseUrl.charAt(length - 1) != '&') {
            result.append("&");
        }
        return result;
    }

    private static StringBuilder addParam(Object key, Object value, final StringBuilder result) {
        if (value.getClass().isArray()) {
            value = arrayToList(value);
        }

        if (value instanceof Iterable) {
            Iterator<?> iterator = ((Iterable) value).iterator();
            while (iterator.hasNext()) {
                result.append(key);
                result.append("[]=");
                Object element = iterator.next();
                if (null != element) {
                    result.append(element);
                }
                if (iterator.hasNext()) {
                    result.append("&");
                }
            }
        } else {
            result.append(key);
            result.append("=");
            if (null != value) {
                result.append(value);
            }
        }

        return result;
    }

    private static List<Object> arrayToList(final Object array) {
        if (array instanceof Object[]) {
            return Arrays.asList((Object[]) array);

        }
        List<Object> result = new ArrayList<>();
        // Arrays of the primitive types can't be cast to array of Object, so this:
        if (array instanceof int[]) {
            for (int value : (int[]) array) {
                result.add(value);
            }
        } else if (array instanceof boolean[]) {
            for (boolean value : (boolean[]) array) {
                result.add(value);
            }
        } else if (array instanceof long[]) {
            for (long value : (long[]) array) {
                result.add(value);
            }
        } else if (array instanceof float[]) {
            for (float value : (float[]) array) {
                result.add(value);
            }
        } else if (array instanceof double[]) {
            for (double value : (double[]) array) {
                result.add(value);
            }
        } else if (array instanceof short[]) {
            for (short value : (short[]) array) {
                result.add(value);
            }
        } else if (array instanceof byte[]) {
            for (byte value : (byte[]) array) {
                result.add(value);
            }
        } else if (array instanceof char[]) {
            for (char value : (char[]) array) {
                result.add(value);
            }
        }

        return result;
    }

    public String body(String charset) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        copy(buffer(), outputStream);

        try {
            charset = getValidCharset(charset);
            return outputStream.toString(charset);
        } catch (UnsupportedEncodingException e) {
            throw new HttpRequestException(e);
        }
    }

    protected void copy(BufferedInputStream inputStream, final OutputStream outputStream) {
        byte[] bytes = new byte[BUFFER_SIZE];
        try {
            int read;
            while ((read = inputStream.read(bytes)) > 0) {
                outputStream.write(bytes, 0, read);
            }
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    public BufferedInputStream buffer() {
        return new BufferedInputStream(stream());
    }

    public InputStream stream() {
        InputStream stream;
        try {
            if (getConnection().getResponseCode() < 400) {
                stream = getConnection().getInputStream();
            } else {
                stream = getConnection().getErrorStream();
            }
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }

        return stream;
    }

    public int intHeader(final String name) {
        return intHeader(name, -1);
    }

    public int intHeader(final String name, final int defaultValue) {
        //todo closeOutput
        closeOutputQuietly();
        return getConnection().getHeaderFieldInt(name, defaultValue);
    }

    public HttpRequest header(final String headerName, final String value) {
        getConnection().setRequestProperty(headerName, value);
        return this;
    }
    /**
     * Get header 'Content-Length' from the response
     * @return
     */
    public int contentLength() {
        return intHeader(HEADER_CONTENT_LENGTH);
    }

    public HttpRequest contentType(final String contentType, final String charset) {
        if (charset != null && charset.length() == 0) {
            String separator = ";" + PARAM_CHARSET + "=";
            return header(HEADER_CONTENT_TYPE, contentType + separator + charset);
        }
        return header(HEADER_CONTENT_TYPE, contentType);
    }


    protected ByteArrayOutputStream byteStream() {
        final int size = contentLength();
        if (size > 0) {
            return new ByteArrayOutputStream(size);
        } else {
            return new ByteArrayOutputStream();
        }
    }

    protected HttpRequest closeOutputQuietly() {
        try {
            closeOutput();
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
        return this;
    }

    protected HttpRequest closeOutput() throws IOException {
        //todo progress upload
        if (outputStream == null) {
            return this;
        }
        if (multipart) {
            outputStream.write(CRLF + "--" + BOUNDARY + "--" + CRLF);
        }
        if (ignoreCloseException) {
            try {
                outputStream.close();
            } catch (Exception e) {

            }
        } else {
            outputStream.close();
        }
        return this;
    }

    protected String getParam(final String value, final String paramName) {
        if (value == null || value.length() == 0) {
            return null;
        }

        final int length = value.length();
        int start = value.indexOf(';') + 1;
        if (start == 0 || start == length) {
            return null;
        }
        int end = value.indexOf(';', start);
        if (end == -1) {
            end = length;
        }

        while (start < end) {
            int nameEnd = value.indexOf('=', start);
            if (nameEnd != -1 && nameEnd < end
                    && paramName.equals(value.substring(start, nameEnd).trim())) {
                String paramValue = value.substring(nameEnd + 1, end).trim();
                int valueLength = paramValue.length();
                if (valueLength != 0) {
                    if (valueLength > 2 && '"' == paramValue.charAt(0)
                            && '"' == paramValue.charAt(valueLength - 1)) {
                        return paramValue.substring(1, valueLength - 1);
                    } else {
                        return paramValue;
                    }
                }

            }

            start = end + 1;
            end = value.indexOf(';', start);
            if (end == -1) {
                end = length;
            }
        }

        return null;
    }

}
