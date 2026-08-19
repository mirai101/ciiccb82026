package com.moocash.api.security;

import com.moocash.api.model.Customer;
import com.moocash.api.repository.CustomerRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomerRepository customerRepository;

    public JwtAuthFilter(JwtUtil jwtUtil, CustomerRepository customerRepository) {
        this.jwtUtil = jwtUtil;
        this.customerRepository = customerRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (jwtUtil.validateToken(token)) {
            String customerId = jwtUtil.extractCustomerId(token);
            long tokenVersion = jwtUtil.extractTokenVersion(token);

            var customerOpt = customerRepository.findById(customerId);
            if (customerOpt.isPresent() && customerOpt.get().getTokenVersion() == tokenVersion) {
                // Previously this always granted Collections.emptyList() (no
                // authorities), so hasRole("ADMIN") could never be enforced at the
                // security filter level - every admin check lived only in service
                // code as a manual "if" statement, with nothing stopping a new
                // endpoint from forgetting that check. Granting the role here lets
                // SecurityConfig enforce admin-only routes as defense-in-depth on
                // top of (not instead of) the existing service-layer checks.
                String role = customerOpt.get().getRole();
                List<SimpleGrantedAuthority> authorities = role != null
                        ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        : List.of();

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    customerId, null, authorities
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
