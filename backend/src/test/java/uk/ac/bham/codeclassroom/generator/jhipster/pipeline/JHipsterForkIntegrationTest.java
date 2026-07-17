package uk.ac.bham.codeclassroom.generator.jhipster.pipeline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JHipsterForkIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void testGlobalJHipsterResolutionWhenPropertyIsAbsent() {
        JHipsterCLIInvoker invoker = new JHipsterCLIInvoker();
        invoker.setJHipsterForkPath(null);

        List<String> command = invoker.buildJHipsterCommand(List.of("jdl", "app.jdl"));
        
        assertEquals(3, command.size());
        assertEquals("jhipster", command.get(0));
        assertEquals("jdl", command.get(1));
        assertEquals("app.jdl", command.get(2));
    }

    @Test
    void testGlobalJHipsterResolutionWhenPropertyIsEmpty() {
        JHipsterCLIInvoker invoker = new JHipsterCLIInvoker();
        invoker.setJHipsterForkPath("   ");

        List<String> command = invoker.buildJHipsterCommand(List.of("jdl", "app.jdl"));
        
        assertEquals(3, command.size());
        assertEquals("jhipster", command.get(0));
    }

    @Test
    void testLocalForkResolutionWithJitExecutable() throws IOException {
        // Create fake JIT script in a temp directory
        Path fakeFork = tempDir.resolve("fake-fork");
        Path fakeBin = fakeFork.resolve("bin");
        Files.createDirectories(fakeBin);
        Path jitScript = fakeBin.resolve("jhipster.cjs");
        Files.writeString(jitScript, "// Fake JIT JHipster script");

        JHipsterCLIInvoker invoker = new JHipsterCLIInvoker();
        invoker.setJHipsterForkPath(fakeFork.toAbsolutePath().toString());

        List<String> command = invoker.buildJHipsterCommand(List.of("jdl", "app.jdl"));

        assertEquals(4, command.size());
        assertEquals("node", command.get(0));
        assertEquals(jitScript.toAbsolutePath().toString(), command.get(1));
        assertEquals("jdl", command.get(2));
        assertEquals("app.jdl", command.get(3));
    }

    @Test
    void testLocalForkResolutionWithSubdirectoryAndCompiledExecutable() throws IOException {
        // Create fake structure with "generator-jhipster" subdirectory
        Path fakeFork = tempDir.resolve("fake-fork");
        Path subDir = fakeFork.resolve("generator-jhipster");
        Path fakeDistCli = subDir.resolve("dist/cli");
        Files.createDirectories(fakeDistCli);
        Path compiledScript = fakeDistCli.resolve("jhipster.cjs");
        Files.writeString(compiledScript, "// Fake compiled script");

        JHipsterCLIInvoker invoker = new JHipsterCLIInvoker();
        // Point to the parent directory, it should automatically find the subdirectory generator-jhipster
        invoker.setJHipsterForkPath(fakeFork.toAbsolutePath().toString());

        List<String> command = invoker.buildJHipsterCommand(List.of("jdl", "app.jdl"));

        assertEquals(4, command.size());
        assertEquals("node", command.get(0));
        assertEquals(compiledScript.toAbsolutePath().toString(), command.get(1));
        assertEquals("jdl", command.get(2));
        assertEquals("app.jdl", command.get(3));
    }

    @Test
    void testVersionCheckExecutesWithGlobalJHipster() {
        JHipsterCLIInvoker invoker = new JHipsterCLIInvoker();
        invoker.setJHipsterForkPath(null);

        ProcessResult result = invoker.executeVersionCheck();
        assertNotNull(result);
        assertEquals(0, result.exitCode());
        assertFalse(result.logs().isBlank());
    }

    @Test
    void testVersionCheckExecutesWithLocalForkIfPresent() {
        Path realFork = Path.of("/Users/theverma/Developer/experiments/jh");
        if (Files.exists(realFork)) {
            JHipsterCLIInvoker invoker = new JHipsterCLIInvoker();
            invoker.setJHipsterForkPath(realFork.toAbsolutePath().toString());

            ProcessResult result = invoker.executeVersionCheck();
            assertNotNull(result);
            assertEquals(0, result.exitCode(), "Real local fork version check failed: " + result.logs());
            assertFalse(result.logs().isBlank());
        }
    }

    @Test
    void testRealGenerationOfProfessorAndGraduateProject() throws Exception {
        Path realFork = Path.of("/Users/theverma/Developer/experiments/jh");
        if (Files.exists(realFork)) {
            System.out.println("====== STARTING REAL GENERATION TEST ======");
            // Instantiate real GenerationService with local fork path
            uk.ac.bham.codeclassroom.generator.api.GenerationService service = new uk.ac.bham.codeclassroom.generator.api.GenerationService();
            
            // Use reflection to set the private jhipsterForkPath property
            java.lang.reflect.Field field = service.getClass().getDeclaredField("jhipsterForkPath");
            field.setAccessible(true);
            field.set(service, realFork.toAbsolutePath().toString());
            
            // Run init so it propagates the path down the pipeline
            service.init();

            String cdl = "entity Professor { name String email String } entity GraduateProject { title String description String }";

            Path zipResult = service.generateStandaloneProject(cdl);
            assertNotNull(zipResult);
            assertTrue(Files.exists(zipResult));
            System.out.println("Generated ZIP exists at: " + zipResult.toAbsolutePath());
            
            // Cleanup the zip
            Files.deleteIfExists(zipResult);
            System.out.println("====== COMPLETED REAL GENERATION TEST ======");
        }
    }
}
