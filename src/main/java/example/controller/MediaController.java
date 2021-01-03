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
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.FileNotFoundException;
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

    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_CONTENT_LENGTH = "Content-Length";
    private static final String HEADER_ACCEPT_RANGES = "Accept-Ranges";
    private static final String HEADER_CONTENT_RANGE = "Content-Range";
    private static final String CONTENT_TYPE_MP4 = "video/mp4";

    /**
     * Serves video stream.
     *
     * @param mediaName name of media to play
     * @param rangeHeader content range header
     * @return video stream
     */
    @GetMapping(value = "/media/{mediaName}")
    public ResponseEntity<StreamingResponseBody> getMediaStream(@PathVariable("mediaName") String mediaName,
                                                                @RequestHeader(value = "Range", required = false) String rangeHeader) {
        try {
            StreamingResponseBody responseStream;
            final HttpHeaders responseHeaders = new HttpHeaders();

            final Path filePath = getMediaPath(mediaName);
            final long fileSize = Files.size(filePath);

            byte[] buffer = new byte[1024];
            if (rangeHeader == null) {
                responseHeaders.add(HEADER_CONTENT_TYPE, CONTENT_TYPE_MP4);
                responseHeaders.add(HEADER_CONTENT_LENGTH, Long.toString(fileSize));
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
                    // If range is not found then just request the whole file
                    rangeEnd = fileSize - 1;
                }

                // Check to make sure that content range is not outside the length of the file
                if (fileSize < rangeEnd) {
                    rangeEnd = fileSize - 1;
                }

                LOG.info("Received request for byte range: {} - {}", rangeStart, rangeEnd);

                String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
                responseHeaders.add(HEADER_CONTENT_TYPE, CONTENT_TYPE_MP4);
                responseHeaders.add(HEADER_CONTENT_LENGTH, contentLength);
                responseHeaders.add(HEADER_ACCEPT_RANGES, "bytes");
                responseHeaders.add(HEADER_CONTENT_RANGE, "bytes" + " " + rangeStart + "-" + rangeEnd + "/" + fileSize);

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

    /**
     * Get the path to the example video in the resources directory.
     *
     * @param mediaName name of media file to retrieve
     * @return path to example video in resources directory
     * @throws FileNotFoundException
     */
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
