/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.chigix.bio.proxy.buffer;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    public void testURI(){
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
    }

}
