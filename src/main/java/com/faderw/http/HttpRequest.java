package com.faderw.http;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
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

            public HttpURLConnection create(URL url) throws IOException {
                return (HttpURLConnection) url.openConnection();
            }

            public HttpURLConnection create(URL url, Proxy proxy) throws IOException {
                return (HttpURLConnection) url.openConnection(proxy);
            }
        };

    }

    private HttpURLConnection connection = null;
    private boolean form;
    private final URL url;
    private final String requestMethod;
    private static final int BUFFER_SIZE = 8132;

    private RequestOutputStream outputStream;


    private static ConnectionFactory connectionFactory = ConnectionFactory.DEFAULT;

    private HttpURLConnection createConnection() {
        final HttpURLConnection connection;
        try {
            connection = connectionFactory.create(url);
            connection.setRequestMethod(requestMethod);
            return connection;
        } catch (IOException e) {
            throw new HttpRequestException(e);
        }
    }

    public HttpURLConnection getConnection() {
        if (connection == null) {
            connection = createConnection();
        }
        return connection;
    }

    public HttpRequest openOutput() {
        if (outputStream != null) {
            return this;
        }
        getConnection().setDoOutput(true);
        final String charset = getParam(getConnection().getRequestProperty(HEADER_CONTENT_TYPE), PARAM_CHARSET);
        outputStream = new RequestOutputStream(getConnection().getOutputStream(), null, BUFFER_SIZE);
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

    public static HttpRequest post(final CharSequence baseUrl,final Map<?, ?> params, final boolean encode) {
        String url = append(url, params);
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
        if (array instanceof Object[])
          return Arrays.asList((Object[]) array);
    
        List<Object> result = new ArrayList<Object>();
        // Arrays of the primitive types can't be cast to array of Object, so this:
        if (array instanceof int[])
          for (int value : (int[]) array) result.add(value);
        else if (array instanceof boolean[])
          for (boolean value : (boolean[]) array) result.add(value);
        else if (array instanceof long[])
          for (long value : (long[]) array) result.add(value);
        else if (array instanceof float[])
          for (float value : (float[]) array) result.add(value);
        else if (array instanceof double[])
          for (double value : (double[]) array) result.add(value);
        else if (array instanceof short[])
          for (short value : (short[]) array) result.add(value);
        else if (array instanceof byte[])
          for (byte value : (byte[]) array) result.add(value);
        else if (array instanceof char[])
          for (char value : (char[]) array) result.add(value);
        return result;

    public String body(String charset) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        copy(buffer(), outputStream);

        try {
            if (charset == null || charset.length() == 0) {
                charset = "UTF-8";
            }
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
        return getConnection().getHeaderFieldInt(name, defaultValue);
    }

    public HttpRequest header(final String headerName, final String value) {
        getConnection().setRequestProperty(headerName, value);
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
            String separtaor = ";" + PARAM_CHARSET + "=";
            return header(HEADER_CONTENT_TYPE, contentType + separtaor + charset)
        }
        return header(HEADER_CONTENT_TYPE, contentType);
    }



    protected String getParam(final String value, final String paramName) {
        if (value == null || value.length() == 0)
          return null;
    
        final int length = value.length();
        int start = value.indexOf(';') + 1;
        if (start == 0 || start == length)
          return null;
    
        int end = value.indexOf(';', start);
        if (end == -1)
          end = length;
    
        while (start < end) {
          int nameEnd = value.indexOf('=', start);
          if (nameEnd != -1 && nameEnd < end
              && paramName.equals(value.substring(start, nameEnd).trim())) {
            String paramValue = value.substring(nameEnd + 1, end).trim();
            int valueLength = paramValue.length();
            if (valueLength != 0)
              if (valueLength > 2 && '"' == paramValue.charAt(0)
                  && '"' == paramValue.charAt(valueLength - 1))
                return paramValue.substring(1, valueLength - 1);
              else
                return paramValue;
          }
    
          start = end + 1;
          end = value.indexOf(';', start);
          if (end == -1)
            end = length;
        }

}
