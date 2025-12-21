
package com.app.thinknshare.user.auth;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
public class JwksController {

    private final KeyManager keyManager;

    public JwksController(KeyManager keyManager) {
        this.keyManager = keyManager;
    }

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Object> jwks() {
        List<JWK> publicKeys = keyManager.getPublicJwks();
        JWKSet set = new JWKSet(publicKeys);

        // Cache headers: allow clients (gateway/services) to cache for N seconds
        CacheControl cc = CacheControl.maxAge(900, TimeUnit.SECONDS).cachePublic();

        return ResponseEntity.ok()
                .cacheControl(cc)
                .body(set.toJSONObject()); // {"keys":[...]}
    }
}
