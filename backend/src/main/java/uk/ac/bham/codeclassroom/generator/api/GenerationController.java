package uk.ac.bham.codeclassroom.generator.api;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Controller exposing the REST endpoint to compile CDL to Spring Boot standalone packages.
 */
@RestController
@Validated
public class GenerationController {

    private final GenerationService generationService;

    public GenerationController(GenerationService generationService) {
        this.generationService = generationService;
    }

    /**
     * Endpoint to compile CDL and return a standalone Spring Boot project zipped.
     *
     * @param request JSON with cdl code
     * @return the raw bytes of the zip archive as an attachment download
     */
    @PostMapping(value = "/api/generate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = "application/zip")
    public ResponseEntity<Resource> generate(@Valid @RequestBody GenerationRequest request) {
        Path zipFile = generationService.generateStandaloneProject(request.cdl());

        Resource resource = new DeleteOnCloseFileSystemResource(zipFile);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"generated-project.zip\"")
                .body(resource);
    }

    /**
     * Custom FileSystemResource that ensures the underlying ZIP file and its temp
     * folder structure are safely pruned off the server as soon as the client finishes downloading.
     */
    private static class DeleteOnCloseFileSystemResource extends FileSystemResource {
        public DeleteOnCloseFileSystemResource(Path path) {
            super(path);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new DeleteOnCloseInputStream(super.getInputStream(), getFile().toPath());
        }

        private static class DeleteOnCloseInputStream extends InputStream {
            private final InputStream delegate;
            private final Path zipPath;

            public DeleteOnCloseInputStream(InputStream delegate, Path zipPath) {
                this.delegate = delegate;
                this.zipPath = zipPath;
            }

            @Override
            public int read() throws IOException {
                return delegate.read();
            }

            @Override
            public int read(byte[] b) throws IOException {
                return delegate.read(b);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return delegate.read(b, off, len);
            }

            @Override
            public void close() throws IOException {
                try {
                    delegate.close();
                } finally {
                    Files.deleteIfExists(zipPath);
                    Path parent = zipPath.getParent();
                    if (parent != null && parent.getFileName().toString().startsWith("codeclassroom-")) {
                        deleteRecursively(parent);
                    }
                }
            }

            private void deleteRecursively(Path path) {
                if (Files.exists(path)) {
                    try (Stream<Path> walk = Files.walk(path)) {
                        walk.sorted(Comparator.reverseOrder())
                            .forEach(p -> {
                                try {
                                    Files.deleteIfExists(p);
                                } catch (IOException ignored) {}
                            });
                    } catch (IOException ignored) {}
                }
            }
        }
    }
}
