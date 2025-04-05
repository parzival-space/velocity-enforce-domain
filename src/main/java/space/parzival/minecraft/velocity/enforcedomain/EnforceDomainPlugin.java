package space.parzival.minecraft.velocity.enforcedomain;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import com.velocitypowered.api.plugin.Plugin;
import net.kyori.adventure.text.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Plugin(id = "enforcedomain", name = "EnforceDomain", version = "1.0", description = "Enforce a domain for all connections", authors = {"Parzival"})
public class EnforceDomainPlugin {
    private final Path dataDirectory;

    @Getter
    private final ProxyServer proxyServer;

    @Getter
    private static EnforceDomainPlugin instance;

    // config
    private List<String> domains = List.of( "example.com");
    private boolean allowLocalhost = false;

    @Inject
    public EnforceDomainPlugin(ProxyServer proxyServer, @DataDirectory Path dataDirectory) {
        instance = this; // NOSONAR - this is a singleton
        this.proxyServer = proxyServer;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // load config
        Toml config = loadConfig("config.toml");

        this.domains = config.getList("domains", this.domains);
        this.allowLocalhost = config.getBoolean("allowLocalhost", this.allowLocalhost);
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        String hostname = player.getVirtualHost().map(InetSocketAddress::getHostName).orElse(null);

        // kick player if hostname is null
        if (hostname == null) {
            Component message = Component.empty()
                    .content("Your client is not sending a valid hostname. Please check your client settings.")
                    .asComponent();

            player.disconnect(message);
            event.setResult(ResultedEvent.ComponentResult.denied(message));

            log.info("Player {} tried to connect without providing a hostname.", player.getUsername());
            return;
        }

        // check domain validity
        boolean isLocalhost = hostname.equals(InetAddress.getLoopbackAddress().getHostName())
                || hostname.equals("127.0.0.1")
                || hostname.equals("::1");
        boolean isAllowed = (isLocalhost && this.allowLocalhost)
                || this.domains.stream().anyMatch(domain ->
                    domain.startsWith("*")
                            ? hostname.endsWith(domain.substring(1))
                            : hostname.equals(domain));

        if (!isAllowed) {
            Component message = Component.empty()
                    .content("Direct connections to this server are not allowed.")
                    .asComponent();

            player.disconnect(message);
            event.setResult(ResultedEvent.ComponentResult.denied(message));

            log.info("Player {} tried to connect with invalid domain/ip: {}", player.getUsername(), hostname);
        }
    }

    private Toml loadConfig(String configFileName) {
        File configFile = new File(this.dataDirectory.toFile(), configFileName);
        if (!configFile.getParentFile().exists())
            configFile.getParentFile().mkdirs();

        if (!configFile.exists()) {
            try (InputStream configFileStream = getClass().getResourceAsStream("/" + configFileName)) {
                if (configFileStream != null)
                    Files.copy(configFileStream, configFile.toPath());
                else
                    configFile.createNewFile();
            } catch (IOException e) {
                log.error("Failed to create configuration file.", e);
            }
        }
        return new Toml().read(configFile);
    }
}
