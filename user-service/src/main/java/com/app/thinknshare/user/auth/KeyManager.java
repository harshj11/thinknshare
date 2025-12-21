package com.app.thinknshare.user.auth;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages current signing key and JWKS publication.
 * Dev: generates RSA keypair at startup.
 * Prod: replace generateDevKey() with keystore loading.
 */
@Component
public class KeyManager {

    private final List<RSAKey> publishedKeys = new CopyOnWriteArrayList<>();
    private RSAKey currentKey;

    @PostConstruct
    public void init() {
        // In prod: loadKeystoreKey();
        this.currentKey = generateDevKey();
        this.publishedKeys.add(currentKey);
    }

    public RSAKey getCurrentSigningKey() {
        return currentKey;
    }

    public List<JWK> getPublicJwks() {
        List<JWK> pubs = new ArrayList<>();
        for (RSAKey k : publishedKeys) {
            pubs.add(k.toPublicJWK());
        }
        return pubs;
    }

    /**
     * Dev-only: generate a 2048-bit RSA keypair and assign a kid.
     */
    private RSAKey generateDevKey() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KeyPair kp = gen.generateKeyPair();
            RSAPublicKey pub = (RSAPublicKey) kp.getPublic();
            RSAPrivateKey priv = (RSAPrivateKey) kp.getPrivate();

            String kid = "key-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm"));

            return new RSAKey.Builder(pub)
                    .privateKey(priv)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(com.nimbusds.jose.JWSAlgorithm.RS256)
                    .keyID(kid)
                    .build();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate dev RSA key", e);
        }
    }

    // Example prod method (skeleton), if using PKCS12 keystore:
    /*
    private void loadKeystoreKey() {
        try (InputStream in = Files.newInputStream(Paths.get(keystorePath))) {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(in, keystorePassword.toCharArray());
            Key key = ks.getKey(keyAlias, keystorePassword.toCharArray());
            if (!(key instanceof RSAPrivateKey)) throw new IllegalStateException("Not RSA private key");
            RSAPrivateKey priv = (RSAPrivateKey) key;
            X509Certificate cert = (X509Certificate) ks.getCertificate(keyAlias);
            RSAPublicKey pub = (RSAPublicKey) cert.getPublicKey();

            this.currentKey = new RSAKey.Builder(pub)
                .privateKey(priv)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID("key-prod-" + cert.getSerialNumber())
                .build();
            this.publishedKeys.add(currentKey);
        } catch (Exception e) {
            throw new IllegalStateException("Keystore load failed", e);
        }
    }
    */
}
