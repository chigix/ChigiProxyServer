/*
 * This file is part of the ChigiProxyTunnel package.
 * 
 * (c) Richard Lea <chigix@zoho.com>
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */
package com.chigix.buffer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Richard Lea <chigix@zoho.com>
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
        if (true) {
            return;
        }
        System.out.println("offer");
        Object e = null;
        FixedBuffer instance = null;
        instance.offer(e);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }

    /**
     * Test of toArray method, of class FixedBuffer.
     */
    @Test
    public void testToArray() {
        if (true) {
            return; 
        }
        System.out.println("toArray");
        Object[] a = null;
        FixedBuffer instance = null;
        Object[] expResult = null;
        Object[] result = instance.toArray(a);
        assertArrayEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getCapacity method, of class FixedBuffer.
     */
    @Test
    public void testGetCapacity() {
        if (true) {
            return;
        }
        System.out.println("getCapacity");
        FixedBuffer instance = null;
        int expResult = 0;
        int result = instance.getCapacity();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }
    
}
