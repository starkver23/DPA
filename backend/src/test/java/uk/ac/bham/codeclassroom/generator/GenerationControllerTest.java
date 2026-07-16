package uk.ac.bham.codeclassroom.generator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.bham.codeclassroom.generator.api.GenerationController;
import uk.ac.bham.codeclassroom.generator.api.GenerationService;
import uk.ac.bham.codeclassroom.generator.exception.ParserException;
import uk.ac.bham.codeclassroom.generator.inheritance.InheritanceResolutionException;
import uk.ac.bham.codeclassroom.generator.project.ProjectBuilderException;
import uk.ac.bham.codeclassroom.generator.semantic.SemanticException;
import uk.ac.bham.codeclassroom.generator.token.Token;
import uk.ac.bham.codeclassroom.generator.token.TokenType;
import uk.ac.bham.codeclassroom.generator.zip.ZipGenerationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = GenerationController.class)
class GenerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GenerationService generationService;

    private Path tempFile;

    @BeforeEach
    void setUp() throws IOException {
        // Create a dummy temporary ZIP file to return from the service mock
        tempFile = Files.createTempFile("codeclassroom-gen-test-", ".zip");
        Files.writeString(tempFile, "dummy zip contents");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testValidCDLReturnsHttp200AndZipFile() throws Exception {
        Mockito.when(generationService.generateStandaloneProject(Mockito.anyString()))
                .thenReturn(tempFile);

        String validRequestJson = """
                {
                    "cdl": "entity Student { name String }"
                }
                """;

        mockMvc.perform(post("/api/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/zip"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"generated-project.zip\""))
                .andExpect(content().string("dummy zip contents"));
    }

    @Test
    void testEmptyRequestReturnsHttp400() throws Exception {
        String emptyRequestJson = """
                {
                    "cdl": ""
                }
                """;

        mockMvc.perform(post("/api/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyRequestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testNullRequestReturnsHttp400() throws Exception {
        String nullRequestJson = "{}";

        mockMvc.perform(post("/api/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nullRequestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void testParserErrorReturnsHttp400() throws Exception {
        Token dummyToken = new Token(TokenType.IDENTIFIER, "Student", 1, 1);
        Mockito.when(generationService.generateStandaloneProject(Mockito.anyString()))
                .thenThrow(new ParserException("Parser syntax error", dummyToken));

        String requestJson = """
                {
                    "cdl": "entity Student"
                }
                """;

        mockMvc.perform(post("/api/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Syntax Error [line 1, column 1] at 'Student': Parser syntax error"));
    }

    @Test
    void testSemanticErrorReturnsHttp400() throws Exception {
        Mockito.when(generationService.generateStandaloneProject(Mockito.anyString()))
                .thenThrow(new SemanticException("Duplicate entity found"));

        String requestJson = """
                {
                    "cdl": "entity Student {} entity Student {}"
                }
                """;

        mockMvc.perform(post("/api/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Duplicate entity found"));
    }

    @Test
    void testInheritanceResolutionErrorReturnsHttp400() throws Exception {
        Mockito.when(generationService.generateStandaloneProject(Mockito.anyString()))
                .thenThrow(new InheritanceResolutionException("Circular inheritance"));

        String requestJson = """
                {
                    "cdl": "entity Student extends Student {}"
                }
                """;

        mockMvc.perform(post("/api/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Circular inheritance"));
    }

    @Test
    void testProjectBuilderErrorReturnsHttp500() throws Exception {
        Mockito.when(generationService.generateStandaloneProject(Mockito.anyString()))
                .thenThrow(new ProjectBuilderException("Directory cannot be created"));

        String requestJson = """
                {
                    "cdl": "entity Student {}"
                }
                """;

        mockMvc.perform(post("/api/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("Directory cannot be created"));
    }

    @Test
    void testZipGenerationErrorReturnsHttp500() throws Exception {
        Mockito.when(generationService.generateStandaloneProject(Mockito.anyString()))
                .thenThrow(new ZipGenerationException("ZIP packaging failed"));

        String requestJson = """
                {
                    "cdl": "entity Student {}"
                }
                """;

        mockMvc.perform(post("/api/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("ZIP packaging failed"));
    }
}
