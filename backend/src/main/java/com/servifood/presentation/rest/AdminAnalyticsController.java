package com.servifood.presentation.rest;

import static com.servifood.presentation.rest.dto.AnalyticsDtos.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import com.servifood.application.AnalyticsService;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminAnalyticsController {
    private final AnalyticsService service;
    public AdminAnalyticsController(AnalyticsService service) { this.service = service; }
    @GetMapping("/analytics") AnalyticsOverview overview(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) { return service.overview(from, to); }
    @GetMapping("/reports/{type}") ReportData report(@PathVariable ReportType type, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) { return service.report(type, from, to); }
    @GetMapping(value = "/reports/{type}/csv", produces = "text/csv") ResponseEntity<byte[]> csv(@PathVariable ExportType type, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=servifood-" + type.name().toLowerCase() + ".csv").contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(service.csv(type, from, to));
    }
}
