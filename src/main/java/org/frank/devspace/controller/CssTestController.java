package org.frank.devspace.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller for the CSS test page.
 * This page demonstrates that Tailwind CSS is working properly in the application.
 */
@Controller
public class CssTestController {

    /**
     * Display the CSS test page.
     * 
     * @return The name of the template to render
     */
    @GetMapping("/css-test")
    public String showCssTestPage() {
        return "css-test";
    }
}