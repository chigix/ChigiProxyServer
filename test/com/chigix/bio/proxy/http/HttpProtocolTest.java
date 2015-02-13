package com.chigix.bio.proxy.http;

import com.chigix.bio.proxy.FormatDateTime;
import com.chigix.bio.proxy.buffer.FixedBufferTest;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
 */
public class HttpProtocolTest {

    @Test
    public void testKeepAlive() throws InterruptedException {
        System.out.println("BANKAI");
        Socket tmp_socket = null;
        try {
            tmp_socket = new Socket("www.baidu.com", 80);
        } catch (IOException ex) {
            Logger.getLogger(FixedBufferTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        final Socket socket = tmp_socket;
        new Thread() {

            @Override
            public void run() {
                while (true) {
                    try {
                        int read;
                        System.out.print(read = socket.getInputStream().read());
                        System.out.print("[" + (char) read + "]");
                        System.out.print(",");
                    } catch (IOException ex) {
                        Logger.getLogger(FixedBufferTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

        }.start();
        String requestStr = "GET / HTTP/1.1\r\n"
                + "Host: www.baidu.com\r\n"
                + "Connection: keep-alive\r\n"
                + "Cache-Control: max-age=0\r\n"
                + "Accept: text/html/application/xhtml+xml,application/xml;q=0.9,image/webp,*/*,q=0.8\r\n"
                + "User-Agent: Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/35.0.1902.0 Safari/537.36\r\n"
                + "Accept-Encoding: gzip,deflate,sdch\r\n"
                + "Accept-Language: zh-CN,zh;q=0.8,en;q=0.6\r\n"
                + "Cookie: BAIDUPSID=A4D81D1F9187F9222B6C0492B0349A0D; BAIDUID=391B0B94D9202AE80B473FEEEFD539FE:FG=1; BD_CK_SAM=1; BD_HOME=0; H_PS_PSSID=11327_1429; BD_UPN=12314553\r\n"
                + "\r\n";
        try {
            socket.getOutputStream().write(requestStr.getBytes());
            System.out.println(FormatDateTime.toTimeString(new Date()) + "REQUEST SENT");
        } catch (IOException ex) {
            Logger.getLogger(FixedBufferTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Thread.sleep(6000);
        System.out.println("\nFLAG1: 6 sec~~~\n");
        try {
            socket.getOutputStream().write(requestStr.getBytes());
        } catch (IOException ex) {
            Logger.getLogger(HttpProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Thread.sleep(6000);
        System.out.println("\nFLAG2: 12 sec~~~\n");
        Thread.sleep(50000);
    }

    @Test
    public void testUriParse() {
        String uri_str = "/asdfasfd?awefawvba";
        URI uri = null;
        try {
            uri = new URI(uri_str);
        } catch (URISyntaxException ex) {
            Logger.getLogger(HttpProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        System.out.println(uri.getScheme());
        System.out.println(uri.getHost());
        System.out.println(uri.getPort());
    }

    @Test
    public void testParseUri() {
        String test_uri = "http://zn_77ycxjaq1e0122vcbs.siteintercept.qualtrics.com/WRSiteInterceptEngine/?Q_ZID=ZN_77YCxjAq1e0122V&Q_LOC=http%3A%2F%2Fwww.cbsnews.com%2Fnews%2Fobama-recruits-tech-giants-apple-intel-reveals-new-cybersecurity-information-sharing-proposals%2F";
        //test_uri = "http://zn77ycxjaq1e0122vcbs.siteintercept.qualtrics.com/WRSiteInterceptEngine/?Q_ZID";
        URI uri;
        try {
            uri = new URI(test_uri);
        } catch (URISyntaxException ex) {
            Logger.getLogger(HttpProtocolTest.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        System.out.println(uri.getHost());
        System.out.println(uri.getScheme());
        System.out.println(uri.getPath());
    }
}
