package uk.ac.bham.codeclassroom.controller;

import java.io.IOException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @GetMapping("/node")
    public String nodeVersion() throws IOException, InterruptedException {
        Process process = new ProcessBuilder("node", "-v").start();

        process.waitFor();

        return new String(process.getInputStream().readAllBytes());
    }
}