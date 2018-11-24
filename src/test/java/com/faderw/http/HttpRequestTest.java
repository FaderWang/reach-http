package com.faderw.http;


import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

import static org.junit.Assert.*;

/**
 * @author FaderW
 * 2018/11/23
 */

public class HttpRequestTest {

    @Test
    public void baiduPage() throws MalformedURLException {
        String url = "http://www.baidu.com";
        String body = HttpRequest.get(new URL(url)).body("UTF-8");
        System.out.println(body);

        assertNotEquals(null, body);
    }

    @Test
    public void URI() throws MalformedURLException, URISyntaxException {
        URL url = new URL("http://www.baidu.com/info?name=wang yuxin&age=20");
        System.out.println("url.getProtocol : " + url.getProtocol());
        System.out.println("url.getHost : " + url.getHost());
        System.out.println("url.getPath : " + url.getPath());
        System.out.println("url.getFile : " + url.getFile());
        System.out.println("url.getPort : " + url.getPort());
        System.out.println(new URI(url.getProtocol(), url.getHost(), url.getPath(), url.getQuery(), null).toASCIIString());
        System.out.println(URLEncoder.encode("http://www.baidu.com/info?name=wangyuxin&age=20"));
    }


}
