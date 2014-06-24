package dk.netarkivet.monitor.jmx;

import javax.management.ObjectName;
import java.util.Set;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.NotImplementedException;

/** Unittest for testing the CachingProxyConnectionFactory class. */ 
public class CachingProxyConnectionFactoryTester extends TestCase {
    public CachingProxyConnectionFactoryTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }
    public void testGetConnection() throws Exception {
        JMXProxyConnectionFactory factory
                = new JMXProxyConnectionFactory() {
            public JMXProxyConnection getConnection(String server, int port, int rmiPort,
                                                 String userName, String password) {
                return connectionDescription(server, port, rmiPort, userName, password);
            }
        };

        final String server1 = "foo.dk";
        final int port1 = 1984;
        final int rmiPort1 = 1066;
        final String userName1 = "george";
        final String password1 = "godwinsson";

        assertNotSame("Two calls to factory should give different results",
                factory.getConnection(server1, port1, rmiPort1, userName1, password1),
                factory.getConnection(server1, port1, rmiPort1, userName1, password1));

        CachingProxyConnectionFactory proxy
                = new CachingProxyConnectionFactory(factory);

        JMXProxyConnection jmxProxyConnection = proxy.getConnection(server1, port1,
                rmiPort1, userName1, password1);

        assertSame("Two equals calls to cache should give same results",
                   jmxProxyConnection,
                proxy.getConnection(server1, port1, rmiPort1, userName1, password1));

        assertNotSame("Two different servers should get different results",
                proxy.getConnection(server1, port1, rmiPort1, userName1, password1),
                proxy.getConnection("bar.dk", port1, rmiPort1, userName1, password1));

        assertNotSame("Two different ports should get different results",
                proxy.getConnection(server1, port1, rmiPort1, userName1, password1),
                proxy.getConnection(server1, 1969, rmiPort1, userName1, password1));

        assertNotSame("Two different rmiports should get different results",
                proxy.getConnection(server1, port1, rmiPort1, userName1, password1),
                proxy.getConnection(server1, port1, 783, userName1, password1));

        assertNotSame("Two different users should get different results",
                proxy.getConnection(server1, port1, rmiPort1, userName1, password1),
                proxy.getConnection(server1, port1, rmiPort1, "harald", password1));

        assertNotSame("Two different passwords should get different results",
                proxy.getConnection(server1, port1, rmiPort1, userName1, password1),
                proxy.getConnection(server1, port1, rmiPort1, userName1, "orwell"));

        assertSame("Other equals values should also give same results",
                proxy.getConnection("fnord.com", port1, rmiPort1, userName1, password1),
                proxy.getConnection("fnord.com", port1, rmiPort1, userName1, password1));

        assertSame("First cached item should still exist",
                   jmxProxyConnection,
                proxy.getConnection(server1, port1, rmiPort1, userName1, password1));

    }

    private JMXProxyConnection connectionDescription(String server, int port,
                                                  int rmiPort,
                                                  String userName,
                                                  String password) {
        return new JMXProxyConnection() {
            public <T> T createProxy(ObjectName name, Class<T> intf) {
                throw new NotImplementedException("Not implemented");
            }

            public Set<ObjectName> query(String query) {
                throw new NotImplementedException("Not implemented");
            }

            public boolean isLive() {
                return true;
            }

        };
    }
}