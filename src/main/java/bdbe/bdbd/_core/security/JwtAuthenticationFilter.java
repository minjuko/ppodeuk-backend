package bdbe.bdbd._core.security;

import bdbe.bdbd._core.exception.ApiException;
import bdbe.bdbd._core.exception.BadRequestError;
import bdbe.bdbd._core.exception.InternalServerError;
import bdbe.bdbd._core.exception.UnAuthorizedError;
import bdbe.bdbd._core.utils.ApiUtils;
import bdbe.bdbd.model.Code.MemberRole;
import bdbe.bdbd.model.member.Member;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Slf4j
public class JwtAuthenticationFilter extends BasicAuthenticationFilter {

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String jwt = request.getHeader(JWTProvider.HEADER);

        try {
            if (jwt != null && !isNonProtectedUrl(request)) {
                DecodedJWT decodedJWT = JWTProvider.verify(jwt);
                Long id = decodedJWT.getClaim("id").asLong();
                String role = decodedJWT.getClaim("role").asString();

                MemberRole roleEnum = MemberRole.valueOf(role);
                Member member = Member.builder().id(id).role(roleEnum).build();

                CustomUserDetails myUserDetails = new CustomUserDetails(member);
                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(
                                myUserDetails,
                                null,
                                myUserDetails.getAuthorities()
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (SignatureVerificationException sve) {
            handleException(response, new BadRequestError(BadRequestError.ErrorCode.WRONG_REQUEST_TRANSMISSION, Collections.singletonMap("defaultMessage", "Invalid token signature")));
            return;
        } catch (TokenExpiredException tee) {
            handleException(response, new UnAuthorizedError(UnAuthorizedError.ErrorCode.AUTHENTICATION_FAILED, Collections.singletonMap("defaultMessage", "JWT has expired")));
            return;
        } catch (Exception e) {
            handleException(response, new InternalServerError(InternalServerError.ErrorCode.INTERNAL_SERVER_ERROR, Collections.singletonMap("defaultMessage", "An unexpected error occurred")));
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isNonProtectedUrl(HttpServletRequest request) {
        AntPathRequestMatcher openMatcher = new AntPathRequestMatcher("/api/open/**");
        AntPathRequestMatcher demoImageMatcher = new AntPathRequestMatcher("/demo-images/**");
        return openMatcher.matches(request) || demoImageMatcher.matches(request);
    }

    private void handleException(HttpServletResponse response, ApiException exception) throws IOException {
        ApiUtils.ApiResult<?> apiResult = exception.body();
        response.setStatus(exception.getStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(apiResult.toString());
    }
}
