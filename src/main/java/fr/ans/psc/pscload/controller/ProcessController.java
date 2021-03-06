package fr.ans.psc.pscload.controller;

import fr.ans.psc.pscload.component.Process;
import fr.ans.psc.pscload.component.utils.FilesUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The type Load controller.
 */
@RestController
class ProcessController {

    /**
     * The logger.
     */
    private static final Logger log = LoggerFactory.getLogger(ProcessController.class);

    @Autowired
    private final Process process;

    @Value("${files.directory}")
    private String filesDirectory;

    @Value("${test.download.url}")
    private String testDownloadUrl;

    @Value("${extract.download.url}")
    private String extractDownloadUrl;

    /**
     * Instantiates a new Load controller.
     *
     * @param process the process
     */
    ProcessController(Process process) {
        this.process = process;
    }

    /**
     * Index string.
     *
     * @return the string
     */
    @GetMapping(value = "/check", produces = MediaType.APPLICATION_JSON_VALUE)
    public String index() {
        return "health check OK";
    }

    /**
     * Clean all string.
     *
     * @return the string
     * @throws IOException the io exception
     */
    @PostMapping(value = "/clean-all", produces = MediaType.APPLICATION_JSON_VALUE)
    public String cleanAll() throws IOException {
        FileUtils.cleanDirectory(new File(filesDirectory));
        log.info("all files in {} were deleted!", filesDirectory);
        return "all files in storage were deleted";
    }

    /**
     * Clean string.
     *
     * @return the string
     * @throws IOException the io exception
     */
    @PostMapping(value = "/clean", produces = MediaType.APPLICATION_JSON_VALUE)
    public String clean() throws IOException {
        String[] fileList = Stream.of(Objects.requireNonNull(new File(filesDirectory).listFiles()))
                .map(File::getAbsolutePath).distinct().toArray(String[]::new);
        for (String file : fileList) {
            if (!file.endsWith(".ser")) {
                boolean isDeleted = Files.deleteIfExists(Path.of(file));
                log.info("file: {} is deleted: {}", file, isDeleted);
            }
        }
        log.info("cleanup complete!");
        return "cleanup complete";
    }

    /**
     * List files string.
     *
     * @return the string
     */
    @GetMapping(value = "/files", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String listFiles() {
        return Stream.of(Objects.requireNonNull(new File(filesDirectory).listFiles()))
                .filter(file -> !file.isDirectory())
                .map(File::getName)
                .collect(Collectors.toSet()).toString();
    }

    /**
     * Delete file string.
     *
     * @param payload the payload
     * @return the string
     * @throws IOException the io exception
     */
    @PostMapping(value = "/deleteFile",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String deleteFile(@RequestBody Map<String, Object> payload) throws IOException {
        String fileName = (String) payload.get("fileName");
        FileUtils.forceDelete(new File(filesDirectory, fileName));
        log.info("deleted {}", fileName);
        return "deleted " + fileName;
    }

    /**
     * Download test string.
     *
     * @param fileName the file name
     * @return the string
     * @throws IOException              the io exception
     * @throws GeneralSecurityException the general security exception
     */
    @PostMapping(value = "/process/download/test", produces = MediaType.APPLICATION_JSON_VALUE)
    public String downloadTest(@RequestParam String fileName) throws IOException, GeneralSecurityException {
        String downloadUrl = testDownloadUrl + fileName + ".zip";
        log.info("downloading from {}", downloadUrl);
        process.downloadAndUnzip(downloadUrl);
        log.info("download complete");
        return "download complete!";
    }

    /**
     * Download string.
     *
     * @return the string
     * @throws IOException              the io exception
     * @throws GeneralSecurityException the general security exception
     */
    @PostMapping(value = "/process/download/prod", produces = MediaType.APPLICATION_JSON_VALUE)
    public String download() throws IOException, GeneralSecurityException {
        log.info("downloading from {}", extractDownloadUrl);
        process.downloadAndUnzip(extractDownloadUrl);
        log.info("download complete");
        return "download complete!";
    }

    /**
     * Load string.
     *
     * @return the string
     * @throws IOException the io exception
     */
    @PostMapping(value = "/process/load/new", produces = MediaType.APPLICATION_JSON_VALUE)
    public String loadNew() throws IOException {
        process.loadLatestFile();
        log.info("new Ps and Structure maps loaded");
        return "new maps loading complete";
    }

    /**
     * Load current string.
     *
     * @return the string
     * @throws IOException the io exception
     */
    @PostMapping(value = "/process/load/current", produces = MediaType.APPLICATION_JSON_VALUE)
    public String loadCurrent() throws IOException {
        process.deserializeFileToMaps();
        log.info("current Ps and Structure maps loaded");
        return "current maps loading complete!";
    }

    /**
     * Serialize string.
     *
     * @return the string
     * @throws IOException the io exception
     */
    @PostMapping(value = "/process/serialize", produces = MediaType.APPLICATION_JSON_VALUE)
    public String serialize() throws IOException {
        process.serializeMapsToFile();
        log.info("new Ps and Structure maps serialized");
        return "new Ps and Structure maps serialization complete!";
    }

    /**
     * Diff string.
     *
     * @return the string
     */
    @PostMapping(value = "/process/diff", produces = MediaType.APPLICATION_JSON_VALUE)
    public String diff() {
        log.info("computing map differential");
        process.computeDiff();
        log.info("computing map differential complete");
        return "computing map differential complete!";
    }

    /**
     * Upload string.
     *
     * @return the string
     */
    @PostMapping(value = "/process/upload/diff", produces = MediaType.APPLICATION_JSON_VALUE)
    public String uploadDiff() throws IOException {
        log.info("uploading changes");
        process.uploadChanges();
        FilesUtils.cleanup(filesDirectory);
        log.info("uploading changes finished");
        return "uploading changes complete!";
    }

    @PostMapping(value = "/process/run", produces = MediaType.APPLICATION_JSON_VALUE)
    public String runFullProcess() throws IOException {
        log.info("running full process");
        process.loadLatestFile();
        process.deserializeFileToMaps();
        process.computeDiff();
        process.serializeMapsToFile();
        process.uploadChanges();
        log.info("full upload finished");
        return "full upload complete!";
    }

}
