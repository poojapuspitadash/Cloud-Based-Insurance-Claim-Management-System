package com.claimsys.config;

import com.claimsys.model.AppUser;
import com.claimsys.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public UserDetailsService userDetailsService(UserRepository users) {
        return email -> {
            AppUser u = users.findByEmail(email.toLowerCase())
                    .orElseThrow(() -> new UsernameNotFoundException("No such user"));
            return new User(u.getEmail(), u.getPassword(), List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole())));
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Stateless REST API authenticated per request with HTTP Basic -> CSRF tokens not needed.
            // (For a production browser app you would switch to JWT/OAuth2 + HTTPS.)
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                .requestMatchers("/", "/index.html", "/app.js", "/style.css", "/favicon.ico").permitAll()
                .requestMatchers("/actuator/health", "/error").permitAll()
                .requestMatchers("/api/employee/**").hasRole("EMPLOYEE")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll())
            // Return a plain 401 WITHOUT the "WWW-Authenticate: Basic" header, otherwise browsers pop up their own login box.
            .httpBasic(b -> b.authenticationEntryPoint((req, res, ex) -> {
                res.setStatus(401);
                res.setContentType("application/json");
                res.getWriter().write("{\"error\":\"Unauthorized\"}");
            }));
        return http.build();
    }
}
