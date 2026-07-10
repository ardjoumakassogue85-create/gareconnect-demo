package com.hackathon.gares.controller;

import com.hackathon.gares.dto.ConseilAntiFileRequest;
import com.hackathon.gares.dto.ConseilAntiFileResponse;
import com.hackathon.gares.service.AssistantAntiFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantAntiFileController {

    private final AssistantAntiFileService assistantAntiFileService;

    @PostMapping("/anti-file")
    public ConseilAntiFileResponse conseiller(@RequestBody ConseilAntiFileRequest request) {
        return assistantAntiFileService.conseiller(request);
    }
}
