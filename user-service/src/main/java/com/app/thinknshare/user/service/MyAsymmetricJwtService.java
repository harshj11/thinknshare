package com.app.thinknshare.user.service;

import com.app.thinknshare.user.auth.KeyManager;
import com.app.thinknshare.user.dtos.UserPrincipal;
import com.app.thinknshare.user.util.ApplicationConstants;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class MyAsymmetricJwtService implements AccessTokenService {

    private final KeyManager keyManager;

    private String issuer;

    private String audience;

    private long accessTokenTtlMinutes;

    public MyAsymmetricJwtService(
            KeyManager keyManager,
            @Value("${thinknshare.jwt.issuer}") String issuer,
            @Value("${thinknshare.jwt.audience}") String audience,
            @Value("${thinknshare.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes
    ) {
        this.keyManager = keyManager;
        this.issuer = issuer;
        this.audience = audience;
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
    }

    @Override
    public String getAccessToken(UserPrincipal userPrincipal, Map<String, Object> additionalClaims) {
        try {
            RSAKey privateKey = keyManager.getCurrentSigningKey();
            JWSHeader jwsHeader = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(privateKey.getKeyID())
                    .type(JOSEObjectType.JWT)
                    .build();

            JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .audience(audience)
                    .subject(userPrincipal.userId().toString())
                    .issueTime(new Date())
                    .expirationTime(new Date(new Date().getTime() + accessTokenTtlMinutes * 60 * 1000))
                    .claim(ApplicationConstants.AUTHORITIES, userPrincipal.authorities())
                    .claim(ApplicationConstants.TOKEN_VERSION, userPrincipal.tokenVersion())
                    .build();

            SignedJWT signedJWT = new SignedJWT(jwsHeader, claimSet);
            JWSSigner signer = new RSASSASigner(privateKey.toPrivateKey());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }
}
