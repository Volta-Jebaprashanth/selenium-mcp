package uk.xpathy.selenium.mcp.webdriver;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserFactoryDebuggerTest {

    private final BrowserFactory factory = new BrowserFactory();

    @Test
    void normalizesDebuggerAddresses() {
        assertEquals("127.0.0.1:9222", factory.normalizeDebuggerAddress(null));
        assertEquals("127.0.0.1:9222", factory.normalizeDebuggerAddress("  "));
        assertEquals("localhost:9333", factory.normalizeDebuggerAddress("http://localhost:9333/"));
        assertEquals("127.0.0.1:9222", factory.normalizeDebuggerAddress(" 127.0.0.1:9222 "));
    }

    @Test
    void rejectsMalformedDebuggerAddresses() {
        assertThrows(IllegalArgumentException.class, () -> factory.normalizeDebuggerAddress("localhost"));
        assertThrows(IllegalArgumentException.class, () -> factory.normalizeDebuggerAddress("localhost:"));
        assertThrows(IllegalArgumentException.class, () -> factory.normalizeDebuggerAddress(":9222"));
        assertThrows(IllegalArgumentException.class, () -> factory.normalizeDebuggerAddress("localhost:abc"));
        assertThrows(IllegalArgumentException.class, () -> factory.normalizeDebuggerAddress("localhost:70000"));
    }

    @Test
    void detectsWhetherSomethingIsListening() throws IOException {
        int port;
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            port = server.getLocalPort();
            assertTrue(factory.isDebuggerListening("127.0.0.1:" + port));
        }
        assertFalse(factory.isDebuggerListening("127.0.0.1:" + port));
    }

    @Test
    void firefoxIsNotSupportedForDebuggerConnections() {
        assertThrows(IllegalArgumentException.class, () -> factory.attach("firefox", null));
        assertThrows(IllegalArgumentException.class, () -> factory.launchDebuggable("firefox", 9222, null));
    }

    @Test
    void attachFailsClearlyWhenNothingIsListening() throws IOException {
        int port;
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            port = server.getLocalPort();
        }
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> factory.attach("chrome", "127.0.0.1:" + port));
        assertTrue(e.getMessage().contains("--remote-debugging-port"));
    }

    @Test
    void launchRejectsAPortAlreadyInUse() throws IOException {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            assertThrows(IllegalStateException.class,
                    () -> factory.launchDebuggable("chrome", server.getLocalPort(), null));
        }
    }
}
