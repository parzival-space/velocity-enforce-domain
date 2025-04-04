package space.parzival.minecraft.velocity.enforcedomain;

import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class EnforceDomainPluginTest {
    private static final Path VALID_CONFIG_PATH = Path.of("src", "test", "resources", "mock-config", "valid");
    private static final Path INVALID_CONFIG_PATH = Path.of("src", "test", "resources", "mock-config", "invalid");

    @Test
    void onProxyInitialization_shouldNotThrow() {
        ProxyServer proxyServer = mock(ProxyServer.class);

        assertDoesNotThrow(() -> {
            EnforceDomainPlugin plugin = new EnforceDomainPlugin(proxyServer, VALID_CONFIG_PATH);
            plugin.onProxyInitialization(null);
        });
    }

    @Test
    void onProxyInitialization_shouldThrowWhenConfigInvalid() {
        ProxyServer proxyServer = mock(ProxyServer.class);

        assertThrows(NullPointerException.class, () -> {
            EnforceDomainPlugin plugin = new EnforceDomainPlugin(proxyServer, INVALID_CONFIG_PATH);
            plugin.onProxyInitialization(null);
        });
    }

    @Test
    void onLogin_kickPlayerWhen_hostnameIsNull() {
        ProxyServer proxyServer = mock(ProxyServer.class);
        LoginEvent event = mock(LoginEvent.class);
        Player player = mock(Player.class);

        // prepare
        when(event.getPlayer()).thenReturn(player);
        when(player.getVirtualHost()).thenReturn(Optional.empty());

        // act
        EnforceDomainPlugin plugin = new EnforceDomainPlugin(proxyServer, VALID_CONFIG_PATH);
        plugin.onLogin(event);

        // assert
        verify(player).disconnect(any());
    }

    @Test
    void onLogin_kickPlayerWhen_localhostIsNotAllowed() {
        ProxyServer proxyServer = mock(ProxyServer.class);
        LoginEvent event = mock(LoginEvent.class);
        Player player = mock(Player.class);

        // prepare
        when(event.getPlayer()).thenReturn(player);
        when(player.getVirtualHost()).thenReturn(Optional.of(new InetSocketAddress("localhost", 25565)));

        // act
        EnforceDomainPlugin plugin = new EnforceDomainPlugin(proxyServer, VALID_CONFIG_PATH);
        plugin.onLogin(event);

        // assert
        verify(player).disconnect(any());
    }

    @Test
    void onLogin_kickPlayerWhen_subdomainsAreNotAllowed() {
        ProxyServer proxyServer = mock(ProxyServer.class);
        LoginEvent event = mock(LoginEvent.class);
        Player player = mock(Player.class);

        // prepare
        when(event.getPlayer()).thenReturn(player);
        when(player.getVirtualHost()).thenReturn(Optional.of(new InetSocketAddress("sub.example.com", 25565)));

        // act
        EnforceDomainPlugin plugin = new EnforceDomainPlugin(proxyServer, VALID_CONFIG_PATH);
        plugin.onProxyInitialization(new ProxyInitializeEvent()); // Simulate proxy initialization
        plugin.onLogin(event);

        // assert
        verify(player).disconnect(any());
    }

    @Test
    void onLogin_kickPlayerWhen_domainIsNotAllowed() {
        ProxyServer proxyServer = mock(ProxyServer.class);
        LoginEvent event = mock(LoginEvent.class);
        Player player = mock(Player.class);

        // prepare
        when(event.getPlayer()).thenReturn(player);
        when(player.getVirtualHost()).thenReturn(Optional.of(new InetSocketAddress("notallowed.com", 25565)));

        // act
        EnforceDomainPlugin plugin = new EnforceDomainPlugin(proxyServer, VALID_CONFIG_PATH);
        plugin.onLogin(event);

        // assert
        verify(player).disconnect(any());
    }

    @Test
    void getProxyServer() {
        ProxyServer proxyServer = mock(ProxyServer.class);
        EnforceDomainPlugin plugin = new EnforceDomainPlugin(proxyServer, VALID_CONFIG_PATH);

        assertEquals(proxyServer, plugin.getProxyServer());
    }

    @Test
    void getInstance() {
        ProxyServer proxyServer = mock(ProxyServer.class);
        EnforceDomainPlugin plugin = new EnforceDomainPlugin(proxyServer, VALID_CONFIG_PATH);

        assertEquals(plugin, EnforceDomainPlugin.getInstance());
    }
}