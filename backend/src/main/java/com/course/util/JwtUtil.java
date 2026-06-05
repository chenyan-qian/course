package com.course.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.util.Date;

public class JwtUtil {
    private static final String KEY = "my-course-management-system-secret-key-2026-secure";

    public static String generateToken(Long userId, String username) {
        return Jwts.builder()
                .setSubject(username)
                .claim("id", userId)
                .setExpiration(new Date(System.currentTimeMillis() + 7L * 24 * 3600 * 1000))
                .signWith(Keys.hmacShaKeyFor(KEY.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(KEY.getBytes())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
