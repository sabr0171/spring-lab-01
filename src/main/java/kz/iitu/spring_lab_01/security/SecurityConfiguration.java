package kz.iitu.spring_lab_01.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    // Variant 6: the Content-Security-Policy sent with every response. The browser runs only scripts and
    // stylesheets loaded from this origin: inline <script> blocks, onclick="..." handlers, style="..."
    // attributes and anything from another host are refused, so an injected script cannot run.
    static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self'",
            "img-src 'self' data:",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    // Lab 12: the JSON API gets a chain of its own, consulted before the page chain (in lab 10 /api/** fell
    // through to denyAll()). Programs, not browsers, call it: credentials travel with every request (HTTP Basic,
    // the users of app_user), no session is created, so there is no session cookie for a forged cross-site
    // request to carry — CSRF protection is switched off for this chain only. Lab 11's bearer tokens would
    // replace httpBasic(); the URL rules would stay. The CSP of variant 6 is sent here too.
    @Bean
    @Order(1)
    SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/books/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/fines/mine").hasAnyRole("USER", "ADMIN")   // lab 12, variant 6
                .requestMatchers(HttpMethod.POST, "/api/books/*/fines").hasRole("ADMIN")          // the librarian fines
                .requestMatchers("/api/books/**").hasRole("ADMIN")                                // create, replace, delete
                .anyRequest().denyAll())                                                          // /api/hello, /api/info ...
            .httpBasic(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY)));
        return http.build();
    }

    // Rules are evaluated in order and the first match wins: specific patterns first, denyAll() last,
    // so that an endpoint nobody wrote a rule for is closed rather than open.
    @Bean
    @Order(2)
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // The container's own ERROR dispatch (rendering a 400/403/404 page) is not a new request:
                // without this line every error page would itself be refused by denyAll().
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/", "/login", "/css/**", "/js/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/books/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/books/**").hasRole("ADMIN")   // create, edit, delete
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().denyAll())
            .formLogin(form -> form
                // Always land on the catalogue: otherwise a browser's background request made while the
                // login page was open (DevTools, for example) could be "remembered" and replayed after login.
                .defaultSuccessUrl("/books", true)
                .permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/?logout"))
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY)));
        // CSRF protection is on by default and is deliberately not touched.
        return http.build();
    }

    // Answers «is this the right password?». BCrypt: a per-user random salt and a work factor (10).
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
