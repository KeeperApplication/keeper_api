package xyz.piod.keeper.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.piod.keeper.dto.LinkPreviewRequest;
import xyz.piod.keeper.service.MessageService;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PutMapping("/{messageId}/link-preview")
    public ResponseEntity<Void> updateLinkPreview(
            @PathVariable Long messageId,
            @RequestBody LinkPreviewRequest previewRequest) {
        messageService.updateMessageWithLinkPreview(messageId, previewRequest);
        return ResponseEntity.ok().build();
    }
}