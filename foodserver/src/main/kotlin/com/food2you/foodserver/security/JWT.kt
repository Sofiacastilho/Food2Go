package com.food2you.foodserver.security

import com.food2you.foodserver.restaurant.Restaurant
import com.food2you.foodserver.security.tokens.CostumerToken
import com.food2you.foodserver.security.tokens.RestaurantToken
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.jackson.io.JacksonDeserializer
import io.jsonwebtoken.jackson.io.JacksonSerializer
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletRequest
import org.aspectj.weaver.NameMangler.PREFIX
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Component
data class JWT (

    private val prefix: String = "Bearer",
    private val issuer: String = "Food2GoServer",
    private val secret: String = "Senhateste",
    private val restaurant: String = "restaurant",
    private val days: Int = 2

) {
    private fun toDate(date: LocalDate): Date {
        return Date.from(date.atStartOfDay(ZoneOffset.UTC).toInstant())
    }

    fun createToken(restaurant: Restaurant): String {
        val now = LocalDate.now()
        val restaurantToken = RestaurantToken(
            restaurant.id!!,
            restaurant.email,
            restaurant.name,
            restaurant.roles
        )


        fun createHMACKeyFromString(keyString: String): SecretKey {
            val messageDigest = MessageDigest.getInstance("SHA-256")
            val keyBytes = messageDigest.digest(keyString.toByteArray(StandardCharsets.UTF_8))
            return SecretKeySpec(keyBytes, "HmacSHA256")
        }

        return Jwts.builder()
            .signWith(createHMACKeyFromString(secret))
            .serializeToJsonWith(JacksonSerializer())
            .setIssuedAt(toDate(now))
            .setExpiration(toDate(now.plusDays(days.toLong())))
            .setIssuer(issuer)
            .setSubject(restaurant.id.toString())
            .addClaims(mutableMapOf("restaurantId" to restaurant.id, "restaurantToken" to restaurantToken))
            .compact()
    }

    fun extract(req: HttpServletRequest): Authentication? {
        val header = req.getHeader(HttpHeaders.AUTHORIZATION)
        if (header == null || !header.startsWith(PREFIX)) return null

        val token = header.replace(PREFIX, "").trim()

        val claims = Jwts.parserBuilder()
            .setSigningKey(secret.toByteArray())
            .deserializeJsonWith(JacksonDeserializer(mapOf(restaurant to RestaurantToken::class.java)))
            .build()
            .parseClaimsJws(token)
            .body

        if (issuer != claims.issuer) return null

        val restaurant = claims[restaurant, RestaurantToken::class.java] ?: return null
        val authorities = restaurant.roles.map { SimpleGrantedAuthority("ROLE_$it") }
        return UsernamePasswordAuthenticationToken(restaurant, restaurant.id, authorities)
    }
}



