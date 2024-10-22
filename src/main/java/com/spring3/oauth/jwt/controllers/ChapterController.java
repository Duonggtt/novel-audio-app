package com.spring3.oauth.jwt.controllers;

import com.spring3.oauth.jwt.models.request.UpsertChapterRequest;
import com.spring3.oauth.jwt.services.impl.ChapterServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3388")
@RequestMapping("/api/chapters")
public class ChapterController {

    private final ChapterServiceImpl chapterService;

    @GetMapping("/{slug}")
    public ResponseEntity<?> getAllChaptersInNovel(@PathVariable String slug) {
        return ResponseEntity.ok(chapterService.getAllChaptersInNovel(slug));
    }

    @GetMapping("/{slug}/chap-{chapNo}")
    public ResponseEntity<?> getChapterDetailInNovel(@PathVariable String slug, @PathVariable int chapNo) {
        return ResponseEntity.ok(chapterService.getChapterByChapNoInNovel(slug, chapNo));
    }

    @PostMapping("/create")
    public ResponseEntity<?> saveChapter(@Valid @RequestBody UpsertChapterRequest request) {
        return ResponseEntity.ok(chapterService.saveChapter(request));
    }

    @PutMapping("/{slug}/chap-{chapNo}/update")
    public ResponseEntity<?> updateChapter(@PathVariable String slug, @PathVariable int chapNo, @Valid @RequestBody UpsertChapterRequest request) {
        return ResponseEntity.ok(chapterService.updateChapter(chapNo, slug, request));
    }

}
