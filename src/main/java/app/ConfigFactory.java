package app;

import app.ConfigurationTypes.BulkAlgo;
import app.ConfigurationTypes.Extension;
import app.ConfigurationTypes.HashAlgo;
import app.ConfigurationTypes.KeyExchange;
import app.ConfigurationTypes.ServerAuth;
import app.ConfigurationTypes.TlsVersion;
import de.rub.nds.tlsattacker.core.config.Config;
import java.io.File;
import java.util.Vector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigFactory {
    private static final Logger LOGGER = LogManager.getLogger();

    public static Config getConfig(
            TlsVersion version,
            KeyExchange keyExchange,
            ServerAuth serverAuth,
            HashAlgo hashAlgo,
            BulkAlgo bulkAlgo,
            Vector<Extension> extensions) {

        File configFile = new File("");

        if (version == TlsVersion.TLS12
                && keyExchange == KeyExchange.ECDHE
                && serverAuth == ServerAuth.RSA
                && hashAlgo == HashAlgo.SHA384
                && bulkAlgo == BulkAlgo.AES_256_GCM
                && extensions.isEmpty()) {
            configFile =
                    new File(
                            "/Users/lth/Library/Mobile Documents/com~apple~CloudDocs/Zweitstudium/Module/00_Masterarbeit/Netzwerk/Bearbeitung/TLS-Attacker/TLS-Attacker/Zusatzzeug/tls12_ECDHE_short.config");
        } else if (version == TlsVersion.TLS12
                && keyExchange == KeyExchange.ECDHE
                && serverAuth == ServerAuth.RSA
                && hashAlgo == HashAlgo.SHA384
                && bulkAlgo == BulkAlgo.AES_128_GCM
                && extensions.isEmpty()) {
            configFile =
                    new File(
                            "/Users/lth/Library/Mobile Documents/com~apple~CloudDocs/Zweitstudium/Module/00_Masterarbeit/Netzwerk/Bearbeitung/TLS-Attacker/TLS-Attacker/Zusatzzeug/tls12_ECDHE_RSA_SHA256_short.config");
        } else if (version == TlsVersion.TLS12
                && keyExchange == KeyExchange.DHE
                && serverAuth == ServerAuth.RSA
                && hashAlgo == HashAlgo.SHA384
                && bulkAlgo == BulkAlgo.AES_256_GCM
                && extensions.isEmpty()) {
            configFile =
                    new File(
                            "/Users/lth/Library/Mobile Documents/com~apple~CloudDocs/Zweitstudium/Module/00_Masterarbeit/Netzwerk/Bearbeitung/TLS-Attacker/TLS-Attacker/Zusatzzeug/tls12_DHE_short.config");
        } else if (version == TlsVersion.TLS12
                && keyExchange == KeyExchange.DHE
                && serverAuth == ServerAuth.RSA
                && hashAlgo == HashAlgo.SHA384
                && bulkAlgo == BulkAlgo.AES_256_GCM
                && extensions.size() == 1
                && extensions.contains(Extension.SESSION_RESUMPTION)) {
            configFile =
                    new File(
                            "/Users/lth/Library/Mobile Documents/com~apple~CloudDocs/Zweitstudium/Module/00_Masterarbeit/Netzwerk/Bearbeitung/TLS-Attacker/TLS-Attacker/Zusatzzeug/tls12_dhe_ticket-resumption_short.config");
        } else if (version == TlsVersion.TLS12
                && keyExchange == KeyExchange.ECDHE
                && serverAuth == ServerAuth.RSA
                && hashAlgo == HashAlgo.SHA384
                && bulkAlgo == BulkAlgo.AES_256_GCM
                && extensions.size() == 1
                && extensions.contains(Extension.SESSION_RESUMPTION)) {
            configFile =
                    new File(
                            "/Users/lth/Library/Mobile Documents/com~apple~CloudDocs/Zweitstudium/Module/00_Masterarbeit/Netzwerk/Bearbeitung/TLS-Attacker/TLS-Attacker/Zusatzzeug/tls12_ecdhe_ticket-resumption_short.config");
        } else if (version == TlsVersion.TLS12
                && keyExchange == KeyExchange.RSA
                && serverAuth == ServerAuth.RSA
                && hashAlgo == HashAlgo.SHA384
                && bulkAlgo == BulkAlgo.AES_256_GCM
                && extensions.size() == 1
                && extensions.contains(Extension.SESSION_RESUMPTION)) {
            configFile =
                    new File(
                            "/Users/lth/Library/Mobile Documents/com~apple~CloudDocs/Zweitstudium/Module/00_Masterarbeit/Netzwerk/Bearbeitung/TLS-Attacker/TLS-Attacker/Zusatzzeug/tls12_resumption_short.config");
        } else if (version == TlsVersion.TLS12
                && keyExchange == KeyExchange.RSA
                && serverAuth == ServerAuth.RSA
                && hashAlgo == HashAlgo.SHA384
                && bulkAlgo == BulkAlgo.AES_256_GCM
                && extensions.isEmpty()) {
            configFile =
                    new File(
                            "/Users/lth/Library/Mobile Documents/com~apple~CloudDocs/Zweitstudium/Module/00_Masterarbeit/Netzwerk/Bearbeitung/TLS-Attacker/TLS-Attacker/Zusatzzeug/tls12_RSA_short.config");
        } else if (version == TlsVersion.TLS13
                && keyExchange == KeyExchange.DHE
                && serverAuth == ServerAuth.ECDSA
                && hashAlgo == HashAlgo.SHA384
                && bulkAlgo == BulkAlgo.AES_256_GCM
                && extensions.isEmpty()) {
            configFile =
                    new File(
                            "/Users/lth/Library/Mobile Documents/com~apple~CloudDocs/Zweitstudium/Module/00_Masterarbeit/Netzwerk/Bearbeitung/TLS-Attacker/TLS-Attacker/Zusatzzeug/tls13_DHE_ECDSA_short.config");
        } else if (version == TlsVersion.TLS13
                && keyExchange == KeyExchange.ECDHE
                && serverAuth == ServerAuth.ECDSA
                && hashAlgo == HashAlgo.SHA384
                && bulkAlgo == BulkAlgo.AES_128_GCM
                && extensions.isEmpty()) {
            configFile =
                    new File(
                            "/Users/lth/Library/Mobile Documents/com~apple~CloudDocs/Zweitstudium/Module/00_Masterarbeit/Netzwerk/Bearbeitung/TLS-Attacker/TLS-Attacker/Zusatzzeug/tls13_ECDHE_ECDSA-SHA256_short.config");
        }

        System.out.println("ConfigFile: " + configFile);
        return Config.createConfig(configFile);
    }
}
