package app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import app.ConfigurationTypes.BulkAlgo;
import app.ConfigurationTypes.Extension;
import app.ConfigurationTypes.HashAlgo;
import app.ConfigurationTypes.KeyExchange;
import app.ConfigurationTypes.KeyExchangeGroup;
import app.ConfigurationTypes.ServerAuth;
import app.ConfigurationTypes.TlsVersion;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.constants.CertificateStatusRequestType;
import de.rub.nds.tlsattacker.core.constants.CipherSuite;
import de.rub.nds.tlsattacker.core.constants.NamedGroup;
import de.rub.nds.tlsattacker.core.constants.ProtocolVersion;
import de.rub.nds.tlsattacker.core.constants.PskKeyExchangeMode;
import de.rub.nds.tlsattacker.core.constants.SignatureAndHashAlgorithm;

public class ConfigFactory {
    private static final Logger LOGGER = LogManager.getLogger();

    public static Config getConfig(
            TlsVersion version,
            KeyExchange keyExchange,
            KeyExchangeGroup keyExchangeGroup,
            ServerAuth serverAuth,
            HashAlgo hashAlgo,
            BulkAlgo bulkAlgo,
            Vector<Extension> extensions) {

        ConfigError configValidity = validateConfigCombi(
                version,
                keyExchange,
                keyExchangeGroup,
                serverAuth,
                hashAlgo,
                bulkAlgo,
                extensions);
        if (configValidity != ConfigError.NO_ERROR) {
            throw new Error("Configuration is invalid (" + configValidity 
                + "):\n\tVersion: " + version 
                + "\n\tKey ex: " + keyExchange 
                + "\n\tServer auth: " + serverAuth 
                + "\n\tHash: " + hashAlgo 
                + "\n\tBulk: " + bulkAlgo
                + "\n\tExtensions: " + extensions.toString());
        }
    
        //Config myConfig = Config.createEmptyConfig();
        Config myConfig = Config.createConfig();
        
        // set TLS version
        switch (version) {
            case TLS12:
                myConfig.setHighestProtocolVersion(ProtocolVersion.TLS12);
                myConfig.setSupportedVersions(ProtocolVersion.TLS12);
                myConfig.setDefaultSelectedProtocolVersion(ProtocolVersion.TLS12);
                break;
            case TLS13:
                myConfig.setHighestProtocolVersion(ProtocolVersion.TLS13);
                myConfig.setSupportedVersions(ProtocolVersion.TLS13);
                myConfig.setDefaultSelectedProtocolVersion(ProtocolVersion.TLS13);

                // other TLS1.3 specific extensions
                myConfig.setAddKeyShareExtension(true);
                myConfig.setAddSupportedVersionsExtension(true);
                myConfig.setAddSignatureAndHashAlgorithmsExtension(true);        
                break;
            default:
                throw new Error("TLS version not supported: " + version);
        }

        // set signature and hash algorithm
        switch (ConfigurationTypes.combineAuthWithHash(serverAuth, hashAlgo)) {
            case DSA_SHA256:
                myConfig.setDefaultClientSupportedSignatureAndHashAlgorithms(SignatureAndHashAlgorithm.DSA_SHA256);
                break;
            case DSA_SHA384:
                myConfig.setDefaultClientSupportedSignatureAndHashAlgorithms(SignatureAndHashAlgorithm.DSA_SHA384);
                break;
            case ECDSA_SHA256:
                myConfig.setDefaultClientSupportedSignatureAndHashAlgorithms(SignatureAndHashAlgorithm.ECDSA_SHA256);
                break;
            case ECDSA_SHA384:
                myConfig.setDefaultClientSupportedSignatureAndHashAlgorithms(SignatureAndHashAlgorithm.ECDSA_SHA384);
                break;
            case RSA_SHA256:
                myConfig.setDefaultClientSupportedSignatureAndHashAlgorithms(SignatureAndHashAlgorithm.RSA_SHA256);
                break;
            case RSA_SHA384:
                myConfig.setDefaultClientSupportedSignatureAndHashAlgorithms(SignatureAndHashAlgorithm.RSA_SHA384);
                break;
            default:
                throw new Error("SignatureAndHashAlgorithm Scheme is not supported.");
        }

        // set cipher suite
        CipherSuite cipherSuite = matchCipher(version, keyExchange, serverAuth, bulkAlgo, hashAlgo);
        myConfig.setDefaultClientSupportedCipherSuites(cipherSuite);
        myConfig.setDefaultSelectedCipherSuite(cipherSuite);

        if (keyExchange == KeyExchange.ECDHE) {
            myConfig.setAddECPointFormatExtension(true);
        } else {
            myConfig.setAddECPointFormatExtension(false);
        }

        if (keyExchange == KeyExchange.ECDHE || keyExchange == KeyExchange.DHE) {
            myConfig.setAddEllipticCurveExtension(true);
        } else {
            myConfig.setAddEllipticCurveExtension(false);
        }

        // set key exchange group
        if (keyExchange != KeyExchange.RSA) { // meaning (EC)DH(E)
            NamedGroup namedGroup = getNamedGroup(keyExchangeGroup);
            myConfig.setDefaultClientNamedGroups(namedGroup);
            myConfig.setDefaultSelectedNamedGroup(namedGroup);
            myConfig.setDefaultClientKeyShareNamedGroups(namedGroup);
        }

        // add needed extensions
        // session resumption with ticket
        if (extensions.contains(Extension.RESUMPTION_SESSION_TICKET)) {
            myConfig.setAddSessionTicketTLSExtension(true);
            if (version == TlsVersion.TLS13) {
                List<PskKeyExchangeMode> pskList = new ArrayList<>();
                    pskList.add(PskKeyExchangeMode.PSK_DHE_KE);
                    myConfig.setPSKKeyExchangeModes(pskList);
                    myConfig.setAddPSKKeyExchangeModesExtension(true);
                    myConfig.setAddPreSharedKeyExtension(true);
            }
        }
        // session resumption based on session id
        else if (extensions.contains(Extension.RESUMPTION_SESSION_ID)) {
            myConfig.setAddSessionTicketTLSExtension(false);
        } else {
            myConfig.setAddSessionTicketTLSExtension(false);
            myConfig.setAddPSKKeyExchangeModesExtension(false);
            myConfig.setAddPreSharedKeyExtension(false);
        }

        // Early Data / 0RTT
        if (extensions.contains(Extension.ZERO_RTT)) {
            switch (version) {
                case TLS12:
                    throw new Error("ZERO_RTT is not supported for TLS1.2");
                case TLS13:
                    // early data extension
                    myConfig.setAddEarlyDataExtension(true);
                    myConfig.setEarlyData(new byte[]{0x54, 0x4C, 0x53, 0x2D, 0x41, 0x74, 0x74, 0x61, 0x63, 0x6B, 0x65, 0x72, 0x0A});

                    // pre-shared key extension
                    List<PskKeyExchangeMode> pskList = new ArrayList<>();
                    pskList.add(PskKeyExchangeMode.PSK_KE);
                    myConfig.setPSKKeyExchangeModes(pskList);
                    myConfig.setAddPSKKeyExchangeModesExtension(true);
                    myConfig.setAddPreSharedKeyExtension(true);
                    break;
            }
        } else {
            myConfig.setAddEarlyDataExtension(false);
        }
        
        // OCSP
        if (extensions.contains(Extension.OCSP)) {
            myConfig.setAddCertificateStatusRequestExtension(true);
            myConfig.setCertificateStatusRequestExtensionRequestType(CertificateStatusRequestType.OCSP);
            //<certificateStatusRequestExtensionRequestType>OCSP</certificateStatusRequestExtensionRequestType>
        } else {
            myConfig.setAddCertificateStatusRequestExtension(false);
        }

        // Misc
        myConfig.setAddRenegotiationInfoExtension(false);

        System.out.println(getConfigOverview(myConfig));
        return myConfig;
    }

    private enum ConfigError {
        NO_ERROR,
        TLS13_WITH_STATIC_KX,
        TLS13_WITH_SESSION_ID_RESUMPTION,
        AMBIGIOUS_RESUMPTION,
        HASH_MISMATCHING_BULK,
    }

    private static ConfigError validateConfigCombi(
            TlsVersion version,
            KeyExchange keyExchange,
            KeyExchangeGroup keyExchangeGroup,
            ServerAuth serverAuth,
            HashAlgo hashAlgo,
            BulkAlgo bulkAlgo,
            Vector<Extension> extensions) {
        if (version == TlsVersion.TLS13 && keyExchange == KeyExchange.RSA
                || version == TlsVersion.TLS13 && keyExchange == KeyExchange.DH) {
            return ConfigError.TLS13_WITH_STATIC_KX;
        }

        if (version == TlsVersion.TLS13 && extensions.contains(Extension.RESUMPTION_SESSION_ID)){
            return ConfigError.TLS13_WITH_SESSION_ID_RESUMPTION;
        }

        if (extensions.contains(Extension.RESUMPTION_SESSION_ID) && extensions.contains(Extension.RESUMPTION_SESSION_TICKET)){
            return ConfigError.AMBIGIOUS_RESUMPTION;
        }          

        if (
            (bulkAlgo == BulkAlgo.AES_256_GCM && hashAlgo != HashAlgo.SHA384)
            || (bulkAlgo == BulkAlgo.AES_128_GCM && hashAlgo != HashAlgo.SHA256)
            || (bulkAlgo == BulkAlgo.AES_256_CBC && hashAlgo != HashAlgo.SHA384)
            || (bulkAlgo == BulkAlgo.AES_128_CBC && hashAlgo != HashAlgo.SHA256))
        {
            return ConfigError.HASH_MISMATCHING_BULK;
        }

        if (keyExchange == KeyExchange.ECDHE) {
            if (
                keyExchangeGroup != KeyExchangeGroup.SECP256R1
                && keyExchangeGroup != KeyExchangeGroup.SECP384R1
                && keyExchangeGroup != KeyExchangeGroup.SECP521R1
                && keyExchangeGroup != KeyExchangeGroup.X25519
                && keyExchangeGroup != KeyExchangeGroup.X448) {
                    throw new Error("KeyExchange is ECDHE but KeyExchangeGroup is non elliptic: " + keyExchangeGroup);
                }
        }
        if ((keyExchange == KeyExchange.DH) || (keyExchange == KeyExchange.DHE)) {
            if (
                keyExchangeGroup != KeyExchangeGroup.FFDHE2048
                && keyExchangeGroup != KeyExchangeGroup.FFDHE3072
                && keyExchangeGroup != KeyExchangeGroup.FFDHE4096
                && keyExchangeGroup != KeyExchangeGroup.FFDHE6144
                && keyExchangeGroup != KeyExchangeGroup.FFDHE8192) {
                    throw new Error("KeyExchange is DH(E) but KeyExchangeGroup is non finite group: " + keyExchangeGroup);
                }
        }
        if (keyExchange == KeyExchange.RSA) {
            if (keyExchangeGroup != KeyExchangeGroup.NONE) {
                    throw new Error("KeyExchange is RSA but KeyExchangeGroup is set: " + keyExchangeGroup);
                }
        }

        return ConfigError.NO_ERROR;
    }

    private static NamedGroup getNamedGroup(KeyExchangeGroup keyExchangeGroup) {
        switch (keyExchangeGroup) {
            case SECP256R1:
                return NamedGroup.SECP256R1;
            case SECP384R1:
                return NamedGroup.SECP384R1;
            case SECP521R1:
                return NamedGroup.SECP521R1;
            case X25519:
                return NamedGroup.ECDH_X25519;
            case X448:
                return NamedGroup.ECDH_X448;
            case FFDHE2048:
                return NamedGroup.FFDHE2048;
            case FFDHE3072:
                return NamedGroup.FFDHE3072;
            case FFDHE4096:
                return NamedGroup.FFDHE4096;
            case FFDHE6144:
                return NamedGroup.FFDHE6144;
            case FFDHE8192:
                return NamedGroup.FFDHE8192;
             default:
                throw new Error("There is no corresponding NamedGroup for the KeyExchangeGroup: " + keyExchangeGroup);
        }
    }

    private static class CipherDetails {
        CipherSuite cipher;
        TlsVersion version;
        KeyExchange keyExchange;
        ServerAuth serverAuth;
        BulkAlgo bulkAlgo;
        HashAlgo hashAlgo;
        
        CipherDetails(CipherSuite myCipher, TlsVersion myVersion, KeyExchange myKeyExchange, ServerAuth myServerAuth, BulkAlgo myBulkAlgo, HashAlgo myHashAlgo) {
            cipher = myCipher;
            version = myVersion;
            keyExchange = myKeyExchange;
            serverAuth = myServerAuth;
            bulkAlgo = myBulkAlgo;
            hashAlgo = myHashAlgo;
        }
    }

    private static CipherSuite matchCipher(TlsVersion version, KeyExchange keyExchange, ServerAuth serverAuth, BulkAlgo bulkAlgo, HashAlgo hashAlgo) {
        ArrayList<CipherDetails> ciphersOverview = new ArrayList<CipherDetails>();

        // TLS1.2 ciphers
        ciphersOverview.add(new CipherDetails(CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384, TlsVersion.TLS12, KeyExchange.RSA, ServerAuth.RSA, BulkAlgo.AES_256_GCM, HashAlgo.SHA384));
        ciphersOverview.add(new CipherDetails(CipherSuite.TLS_DHE_RSA_WITH_AES_256_GCM_SHA384, TlsVersion.TLS12, KeyExchange.DHE, ServerAuth.RSA, BulkAlgo.AES_256_GCM, HashAlgo.SHA384));
        ciphersOverview.add(new CipherDetails(CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256, TlsVersion.TLS12, KeyExchange.ECDHE, ServerAuth.RSA, BulkAlgo.AES_128_GCM, HashAlgo.SHA256));
        ciphersOverview.add(new CipherDetails(CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384, TlsVersion.TLS12, KeyExchange.ECDHE, ServerAuth.RSA, BulkAlgo.AES_256_GCM, HashAlgo.SHA384));
        ciphersOverview.add(new CipherDetails(CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256, TlsVersion.TLS12, KeyExchange.ECDHE, ServerAuth.RSA, BulkAlgo.AES_128_CBC, HashAlgo.SHA256));
        ciphersOverview.add(new CipherDetails(CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384, TlsVersion.TLS12, KeyExchange.ECDHE, ServerAuth.RSA, BulkAlgo.AES_256_CBC, HashAlgo.SHA384));
        ciphersOverview.add(new CipherDetails(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384, TlsVersion.TLS12, KeyExchange.ECDHE, ServerAuth.ECDSA, BulkAlgo.AES_256_GCM, HashAlgo.SHA384));
        ciphersOverview.add(new CipherDetails(CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256, TlsVersion.TLS12, KeyExchange.ECDHE, ServerAuth.ECDSA, BulkAlgo.AES_128_GCM, HashAlgo.SHA256));
        
        // TLS1.3 ciphers
        ciphersOverview.add(new CipherDetails(CipherSuite.TLS_AES_128_GCM_SHA256, TlsVersion.TLS13, null, null, BulkAlgo.AES_128_GCM, HashAlgo.SHA256));
        ciphersOverview.add(new CipherDetails(CipherSuite.TLS_AES_256_GCM_SHA384, TlsVersion.TLS13, null, null, BulkAlgo.AES_256_GCM, HashAlgo.SHA384));
        
        for (CipherDetails cipherOverview: ciphersOverview) {
            // shared checks between TLS1.2 and TLS1.3
            if (cipherOverview.version != version) { continue; }
            if (cipherOverview.bulkAlgo != bulkAlgo) { continue; }
            if (cipherOverview.hashAlgo != hashAlgo) { continue; }        

            // differentiated checks for TLS1.2 and TLS1.3
            switch (cipherOverview.version) {
                case TLS12:
                    if (cipherOverview.keyExchange != keyExchange) { continue; }
                    if (cipherOverview.serverAuth != serverAuth) { continue; }
                    return cipherOverview.cipher;
                case TLS13:
                    if (cipherOverview.keyExchange != null) { continue; }
                    if (cipherOverview.serverAuth != null) { continue; }
                    return cipherOverview.cipher;
            }
        }
        
        throw new Error("No matching cipher suite found for:"
            + "\n\tversion: " + version
            + "\n\tkeyExchange: " + keyExchange
            + "\n\tServerAuth: " + serverAuth
            + "\n\tBulkAlgo: " + bulkAlgo
            + "\n\tHashAlgo: " + hashAlgo);
    }

    public static String getConfigOverview(Config config) {
        String configDescription = new String();

        configDescription = "Config\n";
        configDescription += "\nHighest TLS Version: " + config.getHighestProtocolVersion();
        configDescription += "\nCipher Suite: " + config.getDefaultSelectedCipherSuite();
        // TODO: ggf. deepstring nutzen, falls mehrere einträge möglich
        configDescription += "\nSig and Hash Algo: " + config.getDefaultClientSupportedSignatureAndHashAlgorithms();

        configDescription += "\nEC Point Extension: " + config.isAddECPointFormatExtension();
        configDescription += "\nEC Extension: " + config.isAddEllipticCurveExtension();
        
        configDescription += "\nNamed Group: " + config.getDefaultSelectedNamedGroup();
        configDescription += "\nKey Share: " + config.getDefaultClientKeyShareNamedGroups();
        
        configDescription += "\nOCSP: " + config.isAddCertificateStatusRequestExtension();

        configDescription += "\nSession Ticket Extension: " + config.isAddSessionTicketTLSExtension(); 
        if (config.getHighestProtocolVersion() == ProtocolVersion.TLS13) {
            configDescription += "\nPSK Extension (TLS1.3): " + config.isAddPSKKeyExchangeModesExtension();
            configDescription += "\nPSK Extension (TLS1.3): " + config.isAddPreSharedKeyExtension();
            if (config.isAddPSKKeyExchangeModesExtension()) {
                configDescription += "\nPSK Exchange Modes (TLS1.3): " + config.getPSKKeyExchangeModes();
            }
            
            configDescription += "\nEarly Data Extension (TLS1.3): " + config.isAddEarlyDataExtension();
            if (config.isAddEarlyDataExtension()) {
                configDescription += "\nEarly Data (TLS1.3): " + Arrays.toString(config.getEarlyData());
            }
        }

        return configDescription;
    }
}
