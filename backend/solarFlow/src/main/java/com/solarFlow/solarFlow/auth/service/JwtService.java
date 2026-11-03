package com.solarFlow.solarFlow.auth.service;


import com.solarFlow.solarFlow.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key ;
    private final long expiryMin;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiry-minutes}") long expiryMinutes) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expiryMin = expiryMinutes;
    }


    public String generate(User user){
        Instant now = Instant.now() ;
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(expiryMin))))
                .signWith(key)
                .compact();
    }

    public Claims parse ( String token ){
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public  long expirationInSecond(){
        return  expiryMin *60;
    }


}
