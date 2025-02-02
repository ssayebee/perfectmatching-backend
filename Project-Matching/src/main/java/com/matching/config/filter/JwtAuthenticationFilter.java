package com.matching.config.filter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matching.config.auth.SecurityConstants;
import com.matching.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.var;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, ApplicationContext ctx) {
        this.authenticationManager = authenticationManager;
        this.userRepository = ctx.getBean(UserRepository.class);

        setFilterProcessesUrl(SecurityConstants.AUTH_LOGIN_URL);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) {
        Map<String, String> map = new HashMap<>();

        try {
            map = getParamsFromPost(request);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String username = map.get("username");
        String password = map.get("password");

        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username, password));
    }

    private Map<String, String> getParamsFromPost(HttpServletRequest request) throws IOException {
        BufferedReader reader = request.getReader();
        StringBuilder sb = new StringBuilder();
        ObjectMapper mapper = new ObjectMapper();
        String line = reader.readLine();

        while (line != null) {
            sb.append(line + "\n");
            line = reader.readLine();
        }

        reader.close();

        return new HashMap<>(mapper.readValue(sb.toString(), new TypeReference<Map<String, String>>(){})) ;
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain filterChain, Authentication authentication) {
        User user = ((User) authentication.getPrincipal());

        List<String> roles = user.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        byte[] signingKey = SecurityConstants.JWT_SECRET.getBytes();

        String token = Jwts.builder()
                .signWith(SignatureAlgorithm.HS512, signingKey)
                .setHeaderParam("typ", SecurityConstants.TOKEN_TYPE)
                .setIssuer(SecurityConstants.TOKEN_ISSUER)
                .setAudience(SecurityConstants.TOKEN_AUDIENCE)
                .setSubject("Perfect-Matching JWT Token")
                .claim("idx", userRepository.findByEmail(user.getUsername()).getIdx())
                .claim("email", user.getUsername())
                .claim("nickname", userRepository.findByEmail(user.getUsername()).getNick())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .claim("role", roles)
                .compact();

        response.addHeader(SecurityConstants.TOKEN_HEADER, SecurityConstants.TOKEN_PREFIX + token);
    }

}
