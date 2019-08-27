package com.matching.config.filter;

import com.matching.domain.JwtToken;
import com.matching.config.auth.SecurityConstants;
import com.matching.repository.JwtTokenRepository;
import io.jsonwebtoken.*;
import lombok.var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;


public class JwtAuthorizationFilter extends BasicAuthenticationFilter {

    private JwtTokenRepository jwtTokenRepository;

    private ApplicationContext ctx;

    public void setCtx(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    private static final Logger log = LoggerFactory.getLogger(JwtAuthorizationFilter.class);

    public JwtAuthorizationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        UsernamePasswordAuthenticationToken authentication = getAuthentication(request);
        if (authentication == null) {
            filterChain.doFilter(request, response);
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
        this.jwtTokenRepository = ctx.getBean(JwtTokenRepository.class);
        String token = request.getHeader(SecurityConstants.TOKEN_HEADER);

        String findToken = token;
        findToken = token != null ? findToken.replaceAll("Bearer", "").trim() : token;
        JwtToken jwtToken = jwtTokenRepository.findByToken(findToken);

        if(jwtToken == null) {
            return null;
        } else if(!jwtToken.getStatus() || jwtToken.isExpired()) {
            jwtTokenRepository.delete(jwtToken);
            return null;
        }

        if (StringUtils.isNotEmpty(token) && token.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            try {
                var signingKey = SecurityConstants.JWT_SECRET.getBytes();

                var parsedToken = Jwts.parser()
                        .setSigningKey(signingKey)
                        .parseClaimsJws(token.replace("Bearer ", ""));

                var username = parsedToken
                        .getBody()
                        .getSubject();

                var authorities = ((List<?>) parsedToken.getBody()
                        .get("rol")).stream()
                        .map(authority -> new SimpleGrantedAuthority((String) authority))
                        .collect(Collectors.toList());

                if (StringUtils.isNotEmpty(username)) {
                    return new UsernamePasswordAuthenticationToken(username, null, authorities);
                }
            } catch (ExpiredJwtException exception) {
                log.warn("Request to parse expired JwtToken : {} failed : {}", token, exception.getMessage());
            } catch (UnsupportedJwtException exception) {
                log.warn("Request to parse unsupported JwtToken : {} failed : {}", token, exception.getMessage());
            } catch (MalformedJwtException exception) {
                log.warn("Request to parse invalid JwtToken : {} failed : {}", token, exception.getMessage());
            } catch (SignatureException exception) {
                log.warn("Request to parse JwtToken with invalid signature : {} failed : {}", token, exception.getMessage());
            } catch (IllegalArgumentException exception) {
                log.warn("Request to parse empty or null JwtToken : {} failed : {}", token, exception.getMessage());
            }
        }

        return null;
    }
}
