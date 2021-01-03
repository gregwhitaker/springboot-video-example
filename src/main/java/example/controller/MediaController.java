package example.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controller responsible for serving the video stream.
 */
@Controller
public class MediaController {
    private static final Logger LOG = LoggerFactory.getLogger(MediaController.class);

    @GetMapping(value = "/media/{mediaName}")
    public ResponseEntity<StreamingResponseBody> getMediaStream(@PathVariable("mediaName") String mediaName,
                                                                @RequestHeader(value = "Range", required = false) String rangeHeader) {
        try {
            StreamingResponseBody responseStream;

            Path filePath = getMediaPath(mediaName);
            Long fileSize = Files.size(filePath);

            byte[] buffer = new byte[1024];
            final HttpHeaders responseHeaders = new HttpHeaders();

            if (rangeHeader == null) {
                responseHeaders.add("Content-Type", "video/mp4");
                responseHeaders.add("Content-Length", fileSize.toString());
                responseStream = os -> {
                    try (RandomAccessFile file = new RandomAccessFile(filePath.toFile(), "r")) {
                        long pos = 0;
                        file.seek(pos);

                        while (pos < fileSize) {
                            file.read(buffer);
                            os.write(buffer);
                            pos += buffer.length;
                        }

                        os.flush();
                    } catch (Exception ignored) {
                        // Noop
                    }
                };

                return new ResponseEntity<>(responseStream, responseHeaders, HttpStatus.OK);
            } else {
                // Handle partial content requests
                String[] ranges = rangeHeader.split("-");
                Long rangeStart = Long.parseLong(ranges[0].substring(6));
                Long rangeEnd;

                if (ranges.length > 1) {
                    rangeEnd = Long.parseLong(ranges[1]);
                } else {
                    rangeEnd = fileSize - 1;
                }

                if (fileSize < rangeEnd) {
                    rangeEnd = fileSize - 1;
                }

                LOG.info("Received request for byte range: {} - {}", rangeStart, rangeEnd);

                String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
                responseHeaders.add("Content-Type", "video/mp4");
                responseHeaders.add("Content-Length", contentLength);
                responseHeaders.add("Accept-Ranges", "bytes");
                responseHeaders.add("Content-Range", "bytes" + " " + rangeStart + "-" + rangeEnd + "/" + fileSize);

                final Long _rangeEnd = rangeEnd;
                responseStream = os -> {
                    try (RandomAccessFile file = new RandomAccessFile(filePath.toFile(), "r")) {
                        long pos = rangeStart;
                        file.seek(pos);

                        while (pos < _rangeEnd) {
                            file.read(buffer);
                            os.write(buffer);
                            pos += buffer.length;
                        }

                        os.flush();
                    } catch (Exception ignored) {
                        // Noop
                    }
                };

                return new ResponseEntity<>(responseStream, responseHeaders, HttpStatus.PARTIAL_CONTENT);
            }
        } catch (FileNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Path getMediaPath(String mediaName) throws FileNotFoundException {
        final URL mediaResource = MediaController.class.getClassLoader().getResource(mediaName);

        if (mediaResource != null) {
            try {
                return Paths.get(mediaResource.toURI());
            } catch (URISyntaxException e) {
                throw new FileNotFoundException();
            }
        }

        throw new FileNotFoundException();
    }
}
