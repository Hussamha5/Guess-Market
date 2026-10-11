package server;

import engine.XmlParser;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * POST /api/upload (multipart, part "file") - adds the events of an XML file to the market.
 * The uploader becomes the Market Maker of all of them.
 * The threshold is bigger than the maximum file size, so the content stays in memory and is never written
 * to the disk of the server.
 */
@WebServlet("/api/upload")
@MultipartConfig(fileSizeThreshold = UploadServlet.LIMIT_BYTES + 1,
        maxFileSize = UploadServlet.LIMIT_BYTES, maxRequestSize = UploadServlet.LIMIT_BYTES * 2L)
public class UploadServlet extends ApiServlet {

    static final int LIMIT_BYTES = XmlParser.MAX_FILE_BYTES;
    private static final String FILE_PART = "file";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        guarded(response, () -> {
            Part file = readFilePart(request);
            List<String> names;
            try (InputStream content = file.getInputStream()) {
                names = service().upload(currentUser(request), file.getSubmittedFileName(), content);
            }
            sendOk(response, "The file is valid and was accepted. " + names.size() + " event(s) were added: "
                    + String.join(", ", names) + ". You are the Market Maker of all of them.");
        });
    }

    private Part readFilePart(HttpServletRequest request) throws Exception {
        Part file;
        try {
            file = request.getPart(FILE_PART);
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException("The file is too big. The maximum size is "
                    + LIMIT_BYTES / 1024 + " KB.");
        }
        if (file == null) {
            throw new IllegalArgumentException("No file was sent (the file must be sent in a form field named '"
                    + FILE_PART + "').");
        }
        return file;
    }
}