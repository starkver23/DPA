package uk.ac.bham.codeclassroom.generator.jhipster.pipeline;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Validates that the local environment meets all pre-requisites for JHipster generation.
 */
public class JHipsterEnvironmentValidator {

    private final JHipsterCLIInvoker cliInvoker;

    /**
     * Default constructor.
     */
    public JHipsterEnvironmentValidator() {
        this.cliInvoker = new JHipsterCLIInvoker();
    }

    /**
     * Constructor allowing injection of a CLI invoker for testing.
     *
     * @param cliInvoker the CLI invoker to use
     */
    public JHipsterEnvironmentValidator(JHipsterCLIInvoker cliInvoker) {
        this.cliInvoker = cliInvoker;
    }

    /**
     * Validates the system environment and output directory.
     *
     * @param outputDirectory the final output directory path to check
     * @throws JHipsterGenerationException if any validation pre-requisite fails
     */
    public void validate(Path outputDirectory) {
        if (outputDirectory == null) {
            throw new JHipsterGenerationException("Output directory cannot be null");
        }

        // 1. Verify output directory is writable or can be created
        try {
            if (!Files.exists(outputDirectory)) {
                Files.createDirectories(outputDirectory);
            }
            if (!Files.isWritable(outputDirectory)) {
                throw new JHipsterGenerationException("Output directory is not writable: " + outputDirectory);
            }
        } catch (JHipsterGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new JHipsterGenerationException("Failed to validate output directory writable status", e);
        }

        // 2. Verify Node.js is installed
        try {
            ProcessResult nodeResult = cliInvoker.executeRawCommand("node -v", null);
            if (nodeResult.exitCode() != 0) {
                throw new JHipsterGenerationException("Node.js is not installed or returned error: " + nodeResult.logs());
            }
        } catch (JHipsterGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new JHipsterGenerationException("Node.js is not installed or CLI verification failed", e);
        }

        // 3. Verify npm is installed
        try {
            ProcessResult npmResult = cliInvoker.executeRawCommand("npm -v", null);
            if (npmResult.exitCode() != 0) {
                throw new JHipsterGenerationException("npm is not installed or returned error: " + npmResult.logs());
            }
        } catch (JHipsterGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new JHipsterGenerationException("npm is not installed or CLI verification failed", e);
        }

        // 4. Verify JHipster CLI is available
        try {
            ProcessResult jhipsterResult = cliInvoker.executeVersionCheck();
            if (jhipsterResult.exitCode() != 0) {
                throw new JHipsterGenerationException("JHipster CLI or local fork is not installed or returned error: " + jhipsterResult.logs());
            }
        } catch (JHipsterGenerationException e) {
            throw e;
        } catch (Exception e) {
            throw new JHipsterGenerationException("JHipster CLI or local fork verification failed", e);
        }
    }
}
