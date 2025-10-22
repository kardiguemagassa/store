package com.store.store.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Locale;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebMvcTest
@ExtendWith(MockitoExtension.class)
@Import(GlobalExceptionHandler.class)
public class GlobalExceptionHandlerTestConfig {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected MessageSource messageSource;

    protected static final Locale FRENCH_LOCALE = Locale.FRENCH;
    protected static final Locale ENGLISH_LOCALE = Locale.ENGLISH;

    @BeforeEach
    void setUpBaseMocks() {
        // Configuration de base pour MessageSource
        when(messageSource.getMessage(anyString(), any(), anyString(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(messageSource.getMessage(any(org.springframework.context.MessageSourceResolvable.class), any(Locale.class)))
                .thenAnswer(invocation -> {
                    org.springframework.context.MessageSourceResolvable resolvable =
                            invocation.getArgument(0);
                    return resolvable.getDefaultMessage();
                });
    }
}