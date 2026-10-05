package org.example.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.config.AuthFilterProperties;
import org.example.config.JwtAuthenticationFilter;
import org.example.domain.auth.adapter.port.TokenPort;
import org.example.domain.auth.model.entity.TokenEntity;
import org.example.domain.auth.model.entity.UserEntity;
import org.example.infrastructure.auth.JwtUtil;
import org.example.types.context.UserContext;
import org.example.types.exception.AppException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JwtAuthenticationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TokenPort tokenPort;
    private JwtAuthenticationFilter filter;

    @Before
    public void setUp() {
        tokenPort = mock(TokenPort.class);
        AuthFilterProperties properties = new AuthFilterProperties();
        properties.setExcludePaths(Collections.singletonList("/auth/login"));
        filter = new JwtAuthenticationFilter(tokenPort, properties, objectMapper);
    }

    @After
    public void tearDown() {
        UserContext.clear();
    }

    @Test
    public void jwtGenerateAndVerifyKeepsUserIdentity() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(jwtUtil, "expireMinutes", 120L);

        UserEntity user = new UserEntity();
        user.setId("user-1");
        user.setUsername("alice");

        TokenEntity token = jwtUtil.generate(user);
        UserEntity verified = jwtUtil.verify(token.getToken());

        assertEquals("user-1", verified.getId());
        assertEquals("alice", verified.getUsername());
    }

    @Test
    public void jwtRejectsTamperedToken() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "0123456789abcdef0123456789abcdef");
        ReflectionTestUtils.setField(jwtUtil, "expireMinutes", 120L);

        UserEntity user = new UserEntity();
        user.setId("user-1");
        user.setUsername("alice");
        String token = jwtUtil.generate(user).getToken();

        try {
            jwtUtil.verify(token + "tampered");
            fail("Expected AppException");
        } catch (AppException e) {
            assertTrue(e.getInfo().contains("无效"));
        }
    }

    @Test
    public void filterRejectsMissingToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/agent/list");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.doFilter(request, response, (req, res) -> invoked.set(true));

        assertFalse(invoked.get());
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":\"401\""));
    }

    @Test
    public void filterSetsCurrentUserAndClearsAfterRequest() throws Exception {
        UserEntity user = new UserEntity();
        user.setId("user-1");
        when(tokenPort.verify("valid-token")).thenReturn(user);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/agent/list");
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> currentUser = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> currentUser.set(UserContext.getCurrentUserId()));

        assertEquals("user-1", currentUser.get());
        assertNull(UserContext.getCurrentUserId());
    }

    @Test
    public void filterSkipsWhitelistPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        verify(tokenPort, org.mockito.Mockito.never()).verify(org.mockito.ArgumentMatchers.anyString());
        assertNull(UserContext.getCurrentUserId());
    }
}
