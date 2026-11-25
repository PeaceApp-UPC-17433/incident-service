package pe.upc.peaceapp.incident.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import pe.upc.peaceapp.incident.security.GatewayIdentityFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, GatewayIdentityFilter identityFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error", "/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/incidents/stats").hasAnyRole("ADMINISTRADOR", "MODERADOR")
                        .requestMatchers(org.springframework.http.HttpMethod.PUT, "/api/v1/incidents/*/status").hasAnyRole("ADMINISTRADOR", "MODERADOR")
                        .anyRequest().authenticated())
                .addFilterBefore(identityFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
