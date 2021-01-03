package example.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
public class MediaController {
    private static final Logger LOG = LoggerFactory.getLogger(MediaController.class);

    @GetMapping(value = "/media/{mediaName}")
    public ResponseEntity<StreamingResponseBody> getMediaStream(@PathVariable("mediaName") String mediaName) {
        LOG.info("Playing: " + mediaName);
        return null;
    }
}
