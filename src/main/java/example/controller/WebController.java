package example.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {
    private static final Logger LOG = LoggerFactory.getLogger(WebController.class);

    @GetMapping("/video")
    public String videoPlayer(Model model) {
        return "index.html";
    }
}
