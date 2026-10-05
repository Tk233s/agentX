package org.example.auth;

import org.example.domain.auth.adapter.port.TokenPort;
import org.example.domain.auth.adapter.repository.UserRepository;
import org.example.domain.auth.model.entity.LoginEntity;
import org.example.domain.auth.model.entity.TokenEntity;
import org.example.domain.auth.model.entity.UserEntity;
import org.example.domain.auth.service.Impl.AuthDomainServiceImpl;
import org.example.domain.auth.util.PasswordHasher;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenPort tokenPort;

    @InjectMocks
    private AuthDomainServiceImpl authDomainService;

    private UserEntity user;

    @Before
    public void setUp() {
        user = new UserEntity();
        user.setId("user-1");
        user.setUsername("alice");
        user.setPasswordHash(PasswordHasher.hash("correct-password"));
    }

    @Test
    public void loginReturnsTokenForValidPassword() {
        TokenEntity token = new TokenEntity("jwt-token", LocalDateTime.now().plusHours(2));
        when(userRepository.findByUsername("alice")).thenReturn(user);
        when(tokenPort.generate(user)).thenReturn(token);

        LoginEntity result = authDomainService.login("alice", "correct-password");

        assertNotNull(result);
        assertEquals("user-1", result.getUserId());
        assertEquals("alice", result.getUsername());
        assertEquals("jwt-token", result.getToken());
        assertEquals(token.getExpiresAt(), result.getExpiresAt());
    }

    @Test
    public void loginRejectsWrongPassword() {
        when(userRepository.findByUsername("alice")).thenReturn(user);

        try {
            authDomainService.login("alice", "wrong-password");
            fail("Expected AppException");
        } catch (AppException e) {
            assertEquals(ResponseCode.ILLEGAL_PARAMETER.getCode(), e.getCode());
        }
    }

    @Test
    public void loginRejectsUnknownUser() {
        when(userRepository.findByUsername("alice")).thenReturn(null);

        try {
            authDomainService.login("alice", "correct-password");
            fail("Expected AppException");
        } catch (AppException e) {
            assertEquals(ResponseCode.ILLEGAL_PARAMETER.getCode(), e.getCode());
        }
    }

    @Test
    public void passwordHasherUsesDifferentSaltForSamePassword() {
        String first = PasswordHasher.hash("same-password");
        String second = PasswordHasher.hash("same-password");

        assertTrue(PasswordHasher.matches("same-password", first));
        assertTrue(PasswordHasher.matches("same-password", second));
        assertTrue(!first.equals(second));
    }
}
