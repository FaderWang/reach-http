package com.faderw.http;

/**
 * @author FaderW
 * 2018/11/22
 */

public interface HttpRequestConst {

    /**
     * 'GET' request method
     */
    String METHOD_GET = "GET";

    /**
     * 'POST' request method
     */
    String METHOD_POST = "POST";

    /**
     * 'Content-Length' header name
     */
    String HEADER_CONTENT_LENGTH = "Content-Length";

    /**
     * 'Content-Type' header name
     */
    String HEADER_CONTENT_TYPE = "Content-Type";

    /**
     * 'charset' param charset name
     */
    String PARAM_CHARSET = "charset";

    /**
     * 'application/x-www-form-urlencoded' content type header value
    */
    String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";

    /**
     * 'application/json' content type header value
     */
    String CONTENT_TYPE_JSON = "application/json";

    /**
     * 'application/json;charset=UTF-8' content type header value
     */
    String CONTENT_TYPE_JSON_UTF8 = "application/json;charset=UTF-8";

    /**
     * 'multipart/form-data' content type header value
     */
    String CONTENT_TYPE_MUITIPART = "multipart/form-data;boundary=";

    /**
     * 'CRLF' text
     */
    String CRLF = "\r\n";

    /**
     * 'boundary' text
     */
    String BOUNDARY = "reach0boundary0content";

}
