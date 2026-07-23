package uk.ac.bham.codeclassroom.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class SpaController {

    @RequestMapping(value = { "/{path:[^\\.]*}", "/**/{path:[^\\.]*}" })
    public String forwardSpaRoutes(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Exclude backend APIs and the H2 console from being forwarded to the frontend
        if (path.startsWith("/api") || path.startsWith("/h2-console")) {
            return "forward:/error";
        }
        return "forward:/index.html";
    }
}
