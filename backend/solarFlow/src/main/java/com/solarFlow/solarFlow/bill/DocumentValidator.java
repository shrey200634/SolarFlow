package com.solarFlow.solarFlow.bill;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;

@Component
public class DocumentValidator {

    public static final long MAX_BYTES = 8L * 1024 * 1024;
    private static final int MIN_WIDTH = 600;
    private static final int MIN_HEIGHT = 400;

    public String validate(byte[] data) {
        if (data == null || data.length == 0) {
            throw bad("File is empty");
        }
        if (data.length > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File must be 8 MB or smaller");
        }

        String type = detectType(data);
        if (type == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only JPEG, PNG or PDF files are allowed");
        }

        if (!type.equals("application/pdf")) {
            checkImageResolution(data);
        }
        return type;
    }

    private String detectType(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 0x50 && d[2] == 0x4E && d[3] == 0x47
                && d[4] == 0x0D && d[5] == 0x0A && d[6] == 0x1A && d[7] == 0x0A) {
            return "image/png";
        }
        if (d.length >= 5 && d[0] == 0x25 && d[1] == 0x50 && d[2] == 0x44 && d[3] == 0x46 && d[4] == 0x2D) {
            return "application/pdf";   // "%PDF-"
        }
        return null;
    }

    private void checkImageResolution(byte[] data) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw bad("Image is corrupt or unreadable");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                int w = reader.getWidth(0);
                int h = reader.getHeight(0);
                if (w < MIN_WIDTH || h < MIN_HEIGHT) {
                    throw bad("Image is too small. Please upload a clearer photo (at least "
                            + MIN_WIDTH + "x" + MIN_HEIGHT + " pixels)");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw bad("Image is corrupt or unreadable");
        }
    }

    private static ResponseStatusException bad(String msg) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }
}