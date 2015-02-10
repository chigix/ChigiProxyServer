/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chigix.bio.proxy.buffer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Administrator
 */
public class FixedBufferTest {

    public FixedBufferTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of offer method, of class FixedBuffer.
     */
    @Test
    public void testOffer() {
        System.out.println("offer");
        Object e = null;
        FixedBuffer instance = null;
        instance.offer(e);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of toArray method, of class FixedBuffer.
     */
    @Test
    public void testToArray() {
        System.out.println("toArray");
        FixedBuffer<Integer> instance = new FixedBuffer<Integer>(4);
        instance.offer(1);
        instance.offer(2);
        instance.offer(3);
        instance.offer(4);
        instance.offer(4);
        instance.offer(5);
        instance.offer(1);
        Integer[] result = instance.toArray(new Integer[4]);
        Integer[] expResult = new Integer[]{4, 4, 5, 1};
        assertArrayEquals(expResult, result);
        result = instance.toArray(new Integer[3]);
        expResult = new Integer[]{4, 4, 5};
        assertArrayEquals(expResult, result);
    }

    @Test
    public void testHTTPS() {
        System.out.println("BANKAI");
        Socket tmp_socket = null;
        try {
            tmp_socket = new Socket("jp.jfwq.net", 6002);
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
                        System.out.print((char) read);
                        System.out.print(",");
                    } catch (IOException ex) {
                        Logger.getLogger(FixedBufferTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

        }.start();
        try {
            socket.getOutputStream().write("CONNECT www.dnspod.cn:443 HTTP/1.1\nHost: www.dnspod.cn:443\n\n".getBytes());
        } catch (IOException ex) {
            Logger.getLogger(FixedBufferTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            Thread.sleep(50000);
        } catch (InterruptedException ex) {
            Logger.getLogger(FixedBufferTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Test
    public void testGoogleConnect() {
        System.out.println("BANKAI");
        Socket tmp_socket = null;
        try {
            tmp_socket = new Socket("www.google.com", 80);
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
        try {
            //socket.getOutputStream().write("GET http://www.google.com/ HTTP/1.0\r\nHost: www.google.com\r\nConnection: close\r\n\n".getBytes());
            //URI uri = new URI("/?gfe_rd=cr&ei=F07YVIjKBe_98wfq74LICw");
            URI uri = new URI("/asdfwef?");
            System.out.println(uri.getRawFragment());
            System.out.println(uri.getRawPath());
            System.out.println(uri.getRawQuery());
            System.out.println(uri.getRawSchemeSpecificPart());
            socket.getOutputStream().write(("GET / HTTP/1.0\r\nHost: www.google.com\r\nConnection: close\r\n\r\n").getBytes());
            System.out.println("REQUEST SENT");
        } catch (IOException ex) {
            Logger.getLogger(FixedBufferTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (URISyntaxException ex) {
            Logger.getLogger(FixedBufferTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            Thread.sleep(50000);
        } catch (InterruptedException ex) {
            Logger.getLogger(FixedBufferTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Test
    public void testURI() {
        try {
            URI uri = new URI("http://www.baidu.com/awefawgwage/awefwfa/wefafwef/awdfa?safwefawf=awefaf&afwef=fafwef");
            System.out.println(uri.getScheme());
            System.out.println(uri.getHost());
            System.out.println(uri.getPort());
            System.out.println(uri.getQuery());
            System.out.println(uri.getPath());
        } catch (URISyntaxException ex) {
            Logger.getLogger(FixedBufferTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        String illegalQuery = "http://sclick.baidu.com/w.gif?q=a&fm=se&T=1423492890&y=55DFFF7F&rsv_cache=0&rsv_pre=0&rsv_reh=109_130_149_109_85_195_85_85_85_85|540&rsv_scr=1899_1720_0_0_1080_1920&rsv_sid=10383_1469_12498_10902_11101_11399_11277_11241_11401_12550_11243_11403_12470&cid=0&qid=fd67eec000006821&t=1423492880700&rsv_iorr=1&rsv_tn=baidu&path=http%3A%2F%2Fwww.baidu.com%2Fs%3Fie%3Dutf-8%26f%3D8%26rsv_bp%3D1%26rsv_idx%3D1%26ch%3D%26tn%3Dbaidu%26bar%3D%26wd%3Da%26rn%3D%26rsv_pq%3Dda7dc5fb00004904%26rsv_t%3D55188AMIFp8JX4Jb3hJkfCZHYxQdZOBK%252FhV0kLFfAPijGGrceXBoFpnHzmI%26rsv_enter%3D1%26inputT%3D111";
        URI uri;
        while (true) {
            try {
                uri = new URI(illegalQuery);
            } catch (URISyntaxException ex) {
                System.out.println(illegalQuery);
                System.out.println(illegalQuery.charAt(ex.getIndex()));
                System.out.println(illegalQuery.substring(0, ex.getIndex()));
                System.out.println(illegalQuery.substring(ex.getIndex() + 1));
                try {
                    illegalQuery = illegalQuery.substring(0, ex.getIndex()) + URLEncoder.encode(String.valueOf(illegalQuery.charAt(ex.getIndex())), "utf-8") + illegalQuery.substring(ex.getIndex() + 1);
                } catch (UnsupportedEncodingException ex1) {
                }
                System.out.println(illegalQuery);
                continue;
            }
            break;
        }
        System.out.println("SCHEME: " + uri.getScheme());
        System.out.println("HOST: " + uri.getHost());
        System.out.println("path: " + uri.getRawPath());
        System.out.println("query: " + uri.getRawQuery());
        System.out.println("PORT: " + uri.getPort());
    }

}
