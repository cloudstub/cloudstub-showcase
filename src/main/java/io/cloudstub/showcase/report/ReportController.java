package io.cloudstub.showcase.report;

import java.io.IOException;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @PostMapping("/reports/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws IOException {
        Report report = service.upload(file.getOriginalFilename(), file.getBytes());
        return Map.of("id", report.getId(), "status", report.getStatus());
    }

    @GetMapping("/reports/{id}/status")
    public Map<String, Object> status(@PathVariable Long id) {
        Report report = service.get(id);
        return Map.of("id", report.getId(), "status", report.getStatus());
    }

    @GetMapping("/reports/{id}/summary")
    public ReportSummary summary(@PathVariable Long id) {
        return service.summarize(id);
    }
}
