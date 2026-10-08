package pe.upc.peaceapp.incident.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Verifica tokens emitidos por identity-service. Es un sustituto temporal de la
 * propagacion de claims por el API Gateway (CA-04), valido mientras TS-04 no exista:
 * ambos servicios comparten el mismo secreto HMAC por configuracion.
 */
@Service
public class JwtClaimsVerifier {

    private final SecretKey signingKey;

    public JwtClaimsVerifier(@Value("${incident.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
