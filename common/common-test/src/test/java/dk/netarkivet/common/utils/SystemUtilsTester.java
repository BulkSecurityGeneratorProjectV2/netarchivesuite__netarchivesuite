package dk.netarkivet.common.utils;

import dk.netarkivet.common.Constants;
import dk.netarkivet.testutils.CollectionAsserts;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Unit tests for the class SystemUtils.
 */
public class SystemUtilsTester extends TestCase {
    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetLocalIP() {
        String ip = SystemUtils.getLocalIP();
        String[] parts = ip.split("\\.");
        assertTrue("Expected at least four parts in the IP adress " + ip,
                   parts.length >= 4);
    }

	/**
	 * 
	 * Tests getting hostname. This is nearly impossible, but we _can_ check
	 * that an IP address is not returned (note: IP number comparison disabled
	 * as the method invoked uses getCanonicalHostname which does a reverse DNS
	 * lookup which does not return a name but the IP-number if the number is
	 * not in DNS (which frequently is the case for residential networks behind
	 * a NAT-router), and that it at least does not throw an exception.
	 * 
	 * @throws Exception
	 */
    public void testGetLocalHostName() throws Exception {
        String result = SystemUtils.getLocalHostName();
        //assertFalse("Should not be an IPv4 address: " + result,
        //            Constants.IP_KEY_REGEXP.matcher(result).matches());
        //assertFalse("Should not be an IPv6 address: " + result,
        //            Constants.IPv6_KEY_REGEXP.matcher(result).matches());
        Assert.assertTrue("hostname not empty string", result.length()>0);
    }

    public void testGetCurrentClasspath() throws Exception {
        List<String> classpath = SystemUtils.getCurrentClasspath();
        String[] systemClassPath
            = System.getProperty("java.class.path").split(":");
        CollectionAsserts.assertListEquals("Should return the system"
                                           + " class path as a list",
                                           classpath,
                                           (Object[]) systemClassPath);
        // Test that some version of the standard libraries are in there
        JARS: for (String jar : new String[] {
                "commons-fileupload.*\\.jar$",
                "commons-httpclient.*\\.jar$",
                "commons-logging.*\\.jar$",
                "dom4j-.*\\.jar$",
                "jaxen-.*\\.jar$",
                "jetty-.*\\.jar$",
                "junit-.*\\.jar$",
        // Removed as not used in common.
        // "libidn-.*\\.jar$",
        // "lucene-core-.*\\.jar$"
        }) {
            Matcher m = Pattern.compile(jar).matcher("");
            for (String path : classpath) {
                if (m.reset(path).find()) {
                    continue JARS;
                }
            }
            fail("Cannot find jar " + jar + " in classpath " + classpath);
        }
    }
}
