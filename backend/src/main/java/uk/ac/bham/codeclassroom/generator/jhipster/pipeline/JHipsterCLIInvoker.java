package uk.ac.bham.codeclassroom.generator.jhipster.pipeline;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the execution of JHipster and system process commands.
 */
public class JHipsterCLIInvoker {

    private static final Logger log = LoggerFactory.getLogger(JHipsterCLIInvoker.class);

    private String jhipsterForkPath;

    public void setJHipsterForkPath(String jhipsterForkPath) {
        this.jhipsterForkPath = jhipsterForkPath;
    }

    public String getJHipsterForkPath() {
        return this.jhipsterForkPath;
    }

    /**
     * Resolves the generator base directory from the configured path.
     */
    private Path getGeneratorBaseDir() {
        if (jhipsterForkPath == null || jhipsterForkPath.isBlank()) {
            return null;
        }
        Path path = Path.of(jhipsterForkPath.trim());
        if (!Files.exists(path)) {
            return null;
        }
        Path subDir = path.resolve("generator-jhipster");
        if (Files.isDirectory(subDir)) {
            return subDir;
        }
        return path;
    }

    /**
     * Builds the complete command list to run JHipster with the given arguments.
     */
    public List<String> buildJHipsterCommand(List<String> subCommandAndArgs) {
        Path baseDir = getGeneratorBaseDir();
        if (baseDir != null) {
            // Check for JIT executable (bin/jhipster.cjs) as recommended by the fork
            Path jitExec = baseDir.resolve("bin/jhipster.cjs");
            if (Files.exists(jitExec)) {
                log.info("[JHipster] Using Local Fork (JIT Node script) from path: {}", jitExec.toAbsolutePath());
                System.out.println("[JHipster] Using Local Fork (JIT Node script) from path: " + jitExec.toAbsolutePath());
                List<String> cmd = new java.util.ArrayList<>();
                cmd.add("node");
                cmd.add(jitExec.toAbsolutePath().toString());
                cmd.addAll(subCommandAndArgs);
                return cmd;
            }

            // Check for compiled executable (dist/cli/jhipster.cjs)
            Path distExec = baseDir.resolve("dist/cli/jhipster.cjs");
            if (Files.exists(distExec)) {
                log.info("[JHipster] Using Local Fork (Compiled Node script) from path: {}", distExec.toAbsolutePath());
                System.out.println("[JHipster] Using Local Fork (Compiled Node script) from path: " + distExec.toAbsolutePath());
                List<String> cmd = new java.util.ArrayList<>();
                cmd.add("node");
                cmd.add(distExec.toAbsolutePath().toString());
                cmd.addAll(subCommandAndArgs);
                return cmd;
            }

            // Check for cli executable (cli/jhipster.cjs)
            Path cliExec = baseDir.resolve("cli/jhipster.cjs");
            if (Files.exists(cliExec)) {
                log.info("[JHipster] Using Local Fork (CLI Node script) from path: {}", cliExec.toAbsolutePath());
                System.out.println("[JHipster] Using Local Fork (CLI Node script) from path: " + cliExec.toAbsolutePath());
                List<String> cmd = new java.util.ArrayList<>();
                cmd.add("node");
                cmd.add(cliExec.toAbsolutePath().toString());
                cmd.addAll(subCommandAndArgs);
                return cmd;
            }

            log.info("[JHipster] Local fork path configured but no standard script found. Falling back to global JHipster.");
            System.out.println("[JHipster] Local fork path configured but no standard script found. Falling back to global JHipster.");
        }

        log.info("[JHipster] Using Global JHipster CLI");
        System.out.println("[JHipster] Using Global JHipster CLI");
        List<String> cmd = new java.util.ArrayList<>();
        cmd.add("jhipster");
        cmd.addAll(subCommandAndArgs);
        return cmd;
    }

    /**
     * Executes the version check command for JHipster (either global or local fork).
     *
     * @return the execution result
     */
    public ProcessResult executeVersionCheck() {
        Path baseDir = getGeneratorBaseDir();
        if (baseDir != null) {
            List<String> command = buildJHipsterCommand(List.of("--version"));
            return executeCommand(command, null);
        } else {
            return executeRawCommand("jhipster --version", null);
        }
    }

    /**
     * Executes a JHipster JDL import command in the specified workspace directory.
     *
     * @param jdlFilePath the path to the standard JDL file
     * @param workspace   the directory to execute the JHipster command in
     * @return the execution result
     */
    public ProcessResult invokeJDLImport(Path jdlFilePath, Path workspace) {
        if (jdlFilePath == null || workspace == null) {
            throw new JHipsterGenerationException("JDL file path and workspace cannot be null");
        }

        System.out.println("Debugging [CLIInvoker]: Verifying JDL file exists at: " + jdlFilePath.toAbsolutePath());
        if (!java.nio.file.Files.exists(jdlFilePath)) {
            System.out.println("Debugging [CLIInvoker]: ERROR - JDL file DOES NOT EXIST.");
        } else {
            System.out.println("Debugging [CLIInvoker]: JDL file verified and exists.");
        }

        // JHipster command: [executable] jdl <file> --no-insight --skip-git --skip-install
        List<String> subCommandAndArgs = List.of(
            "jdl",
            jdlFilePath.toAbsolutePath().toString(),
            "--no-insight",
            "--skip-git",
            "--skip-install"
        );

        List<String> command = buildJHipsterCommand(subCommandAndArgs);

        ProcessResult result = executeCommand(command, workspace);

        System.out.println("Debugging [CLIInvoker]: Starting 30-second polling for file generation race conditions...");
        java.util.Set<String> seenFiles = new java.util.HashSet<>();
        long endTime = System.currentTimeMillis() + 30000;
        int pollCount = 0;
        
        while (System.currentTimeMillis() < endTime) {
            System.out.println("Debugging [CLIInvoker]: Polling #" + (++pollCount) + " at " + java.time.LocalTime.now());
            boolean hasNewFiles = false;
            try (java.util.stream.Stream<Path> paths = java.nio.file.Files.walk(workspace)) {
                java.util.List<Path> allPaths = paths.collect(java.util.stream.Collectors.toList());
                for (Path p : allPaths) {
                    String relPath = workspace.relativize(p).toString();
                    if (!relPath.isEmpty() && !seenFiles.contains(relPath)) {
                        seenFiles.add(relPath);
                        System.out.println("  [New File/Dir]: " + relPath);
                        hasNewFiles = true;
                    }
                }
            } catch (Exception e) {
                System.out.println("Debugging [CLIInvoker]: Error polling workspace: " + e.getMessage());
            }
            if (!hasNewFiles) {
                System.out.println("  No new files detected in this poll.");
            }
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        System.out.println("Debugging [CLIInvoker]: Polling finished.");

        return result;
    }

    /**
     * Executes a raw system command. Primarily used for environment validation.
     *
     * @param command   the system command (e.g. "node -v")
     * @param workspace the directory to execute the command in (can be null)
     * @return the process execution result
     */
    public ProcessResult executeRawCommand(String command, Path workspace) {
        if (command == null || command.isBlank()) {
            throw new JHipsterGenerationException("Command cannot be null or empty");
        }

        List<String> commandParts;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            commandParts = List.of("cmd.exe", "/c", command);
        } else {
            commandParts = List.of("sh", "-c", command);
        }

        return executeCommand(commandParts, workspace);
    }

    private ProcessResult executeCommand(List<String> commandParts, Path workspace) {
        System.out.println("Debugging [CLIInvoker]: Executing command: " + String.join(" ", commandParts));
        System.out.println("Debugging [CLIInvoker]: Working directory: " + (workspace != null ? workspace.toAbsolutePath() : "null"));
        try {
            ProcessBuilder builder = new ProcessBuilder(commandParts);
            if (workspace != null) {
                builder.directory(workspace.toFile());
            }
            builder.redirectErrorStream(true);

            Process process = builder.start();
            StringBuilder logs = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logs.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            System.out.println("Debugging [CLIInvoker]: Process exited with code: " + exitCode);
            System.out.println("Debugging [CLIInvoker]: Process Output (stdout + stderr):");
            System.out.println(logs.toString());
            System.out.println("Debugging [CLIInvoker]: End of Process Output");
            return new ProcessResult(exitCode, logs.toString());
        } catch (Exception e) {
            System.out.println("Debugging [CLIInvoker]: Exception during execution: " + e.getMessage());
            e.printStackTrace();
            throw new JHipsterGenerationException("System process execution failed: " + commandParts, e);
        }
    }
}
