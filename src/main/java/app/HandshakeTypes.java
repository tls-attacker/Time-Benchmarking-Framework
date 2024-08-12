package app;

public class HandshakeTypes {
    public enum HandshakeType {
        TLS12_EPHEMERAL_WITHOUT_CLIENTAUTH,
        TLS12_EPHEMERAL_WITHOUT_CLIENTAUTH_WITH_RESUMPTION,
        TLS12_EPHEMERAL_WITH_CLIENTAUTH,
        TLS12_EPHEMERAL_WITH_CLIENTAUTH_WITH_ALERT_CERT,
        TLS12_EPHEMERAL_WITH_CLIENTAUTH_WITH_ALERT_CERT_VERIFY,
        TLS12_EPHEMERAL_WITH_CLIENTAUTH_WITH_RESUMPTION,
        TLS12_STATIC_WITHOUT_CLIENTAUTH,
        TLS12_STATIC_WITHOUT_CLIENTAUTH_WITH_RESUMPTION,
        TLS12_STATIC_WITH_CLIENTAUTH,
        TLS12_STATIC_WITH_CLIENTAUTH_WITH_ALERT_CERT,
        TLS12_STATIC_WITH_CLIENTAUTH_WITH_ALERT_CERT_VERIFY,
        TLS12_STATIC_WITH_CLIENTAUTH_WITH_RESUMPTION,
        TLS13_WITHOUT_CLIENTAUTH,
        TLS13_WITHOUT_CLIENTAUTH_WITH_RESUMPTION,
        TLS13_WITHOUT_CLIENTAUTH_WITH_ZERO_RTT,
        TLS13_WITH_CLIENTAUTH,
        TLS13_WITH_CLIENTAUTH_WITH_ALERT_END,
        TLS13_WITH_CLIENTAUTH_WITH_RESUMPTION,
    }
}