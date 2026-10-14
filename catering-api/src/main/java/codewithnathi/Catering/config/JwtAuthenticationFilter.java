package codewithnathi.Catering.config;

import codewithnathi.Catering.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Stateless JWT authentication filter that runs once per request.
 * <p>
 * Intercepts every HTTP request, extracts the Bearer token from the
 * {@code Authorization} header, validates it, and — if valid — populates
 * the {@link SecurityContextHolder} with the authenticated user's details.
 * </p>
 * <p>
 * Placed before {@link org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter}
 * in the security filter chain so that subsequent filters see an authenticated context.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    /**
     * Core filter logic: extract → validate → authenticate.
     *
     * @param request     the incoming HTTP request
     * @param response    the outgoing HTTP response
     * @param filterChain the remaining filter chain
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Pass through immediately if there is no Bearer token — unauthenticated
        // requests to public endpoints (e.g. /api/auth/**) are handled by SecurityConfig
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Strip "Bearer " prefix to get the raw JWT
        final String token = authHeader.substring(7);

        try {
            final String email = jwtUtil.extractEmail(token);

            // Only set authentication if not already set — avoids overwriting an
            // existing valid authentication on the same thread
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userService.loadUserByUsername(email);

                if (jwtUtil.isTokenValid(token, userDetails)) {
                    // Construct an authenticated token and attach request-level details
                    // (remote address, session id) for audit/logging purposes
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,                          // credentials are null post-authentication
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Any token parsing or user-lookup failure means the request is unauthenticated.
            // We don't return 401 here — SecurityConfig's authorization rules will do that
            // for protected endpoints after the filter chain completes.
        }

        filterChain.doFilter(request, response);
    }
}
