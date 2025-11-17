package com.store.store.controller;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.store.store.dto.auth.LoggingResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Tests d'Intégration - LoggingController")
class LoggingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        // répare un logger technique pour capter les logs du contrôleur
        Logger logger = (Logger) LoggerFactory.getLogger(LoggingController.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.TRACE); // Assurez-vous que tous les niveaux sont activés
    }

    @Test
    @DisplayName("Intégration - GET /api/v1/logging devrait générer tous les niveaux de logs")
    @WithMockUser
    void integrationTest_ShouldGenerateAllLogLevels() throws Exception {
        // Given - Vider les logs précédents
        listAppender.list.clear();

        // When
        mockMvc.perform(get("/api/v1/logging")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.message").value("Logging tested successfully"))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        // Then - Vérifier les logs générés
        List<ILoggingEvent> logsList = listAppender.list;

        // Extraire les messages de log
        List<String> logMessages = logsList.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());

        // Vérifier que les logs caractéristiques sont présents
        assertThat(logMessages)
                .as("Devrait contenir les logs de différents niveaux")
                .anyMatch(msg -> msg.contains("TRACE:") && msg.contains("detailed trace log"))
                .anyMatch(msg -> msg.contains("DEBUG:") && msg.contains("debug message"))
                .anyMatch(msg -> msg.contains("INFO:") && msg.contains("informational message"))
                .anyMatch(msg -> msg.contains("WARN:") && msg.contains("warning"))
                .anyMatch(msg -> msg.contains("ERROR:") && msg.contains("error occurred"));

        // Vérifier les niveaux de log
        List<Level> logLevels = logsList.stream()
                .map(ILoggingEvent::getLevel)
                .collect(Collectors.toList());

        assertThat(logLevels)
                .as("Devrait contenir tous les niveaux de log")
                .contains(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR);
    }

    @Test
    @DisplayName("Intégration - GET /api/v1/logging devrait retourner un JSON valide")
    @WithMockUser
    void integrationTest_ShouldReturnValidJson() throws Exception {
        // When & Then
        String response = mockMvc.perform(get("/api/v1/logging")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.status").isString())
                .andExpect(jsonPath("$.message").value("Logging tested successfully"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Vérifier la désérialisation
        LoggingResponseDto responseDto = objectMapper.readValue(response, LoggingResponseDto.class);
        assertThat(responseDto.message()).isEqualTo("Logging tested successfully");
        assertThat(responseDto.status()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("Intégration - Les logs devraient être générés même avec plusieurs appels")
    @WithMockUser
    void integrationTest_ShouldGenerateLogsOnMultipleCalls() throws Exception {
        // Given
        listAppender.list.clear();

        // When - Premier appel
        mockMvc.perform(get("/api/v1/logging")
                        .with(csrf()))
                .andExpect(status().isOk());

        int firstCallLogCount = listAppender.list.size();

        // When - Deuxième appel
        mockMvc.perform(get("/api/v1/logging").with(csrf())).andExpect(status().isOk());

        // Then - Vérifier que des logs ont été générés à chaque appel
        assertThat(listAppender.list.size())
                .as("Devrait avoir généré des logs supplémentaires au deuxième appel")
                .isGreaterThan(firstCallLogCount);

        // Vérifier que tous les niveaux sont présents au moins une fois dans l'ensemble des logs
        List<Level> allLogLevels = listAppender.list.stream()
                .map(ILoggingEvent::getLevel)
                .collect(Collectors.toList());

        assertThat(allLogLevels)
                .as("Devrait contenir tous les niveaux de log sur l'ensemble des appels")
                .contains(Level.TRACE, Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR);
    }
}