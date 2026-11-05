package com.company.commonstarter.error;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;

class JsonAuthenticationEntryPointTest {

    private enum TestErrorCode implements ErrorCode {
        UNAUTHORIZED(HttpStatus.UNAUTHORIZED);

        private final HttpStatus status;

        TestErrorCode(final HttpStatus status) {
            this.status = status;
        }

        @Override
        public HttpStatus getStatus() {
            return status;
        }
    }

    @Test
    void writesApiErrorJsonBodyWithConfiguredStatus() throws Exception {
        final JsonAuthenticationEntryPoint entryPoint =
                new JsonAuthenticationEntryPoint(TestErrorCode.UNAUTHORIZED, new ObjectMapper());
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("bad creds"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json");
        assertThat(response.getContentAsString()).contains("\"errorCode\":\"UNAUTHORIZED\"", "bad creds");
    }
}