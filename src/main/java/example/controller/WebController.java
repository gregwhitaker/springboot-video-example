package example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller responsible for serving the video player.
 */
@Controller
public class WebController {

    /**
     * Serves the video player webpage.
     *
     * @param model spring mvc model
     * @return video player webpage
     */
    @GetMapping("/video")
    public String videoPlayer(Model model) {
        return "index.html";
    }
}
