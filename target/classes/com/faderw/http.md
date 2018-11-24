### URL、URI相关
- URL
```java
URL url = new URL("http://www.baidu.com/info");
        System.out.println("url.getProtocol : " + url.getProtocol());
        System.out.println("url.getHost : " + url.getHost());
        System.out.println("url.getPath : " + url.getPath());
        System.out.println("url.getFile : " + url.getFile());
        System.out.println("url.getPort : " + url.getPort());
```

运行结果
```bash
url.getProtocol : http
url.getHost : www.baidu.com
url.getPath : /info
url.getFile : /info
url.getPort : -1
```
