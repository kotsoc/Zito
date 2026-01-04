import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.UUID;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws java.io.IOException, jakarta.servlet.ServletException {

        String traceId = UUID.randomUUID().toString();
        MDC.put(TRACE_ID, traceId);               // add to log context
        response.addHeader("X-Trace-Id", traceId); // optional: return in response headers

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID); // cleanup to prevent memory leaks
        }
    }
}
