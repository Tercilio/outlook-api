package com.outlook.integration.controllers;

import com.outlook.integration.dtos.Attachment;
import com.outlook.integration.dtos.EmailDTO;
import com.outlook.integration.services.EmailService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/emails")
@Tag(name = "Emails", description = "Operações de leitura de e-mails Outlook")
public class EmailController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/subjects")
    public ResponseEntity<List<String>> listSubjects(@RequestParam String userId) {
        List<String> result = emailService.listEmailSubjects(userId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/latest")
    public ResponseEntity<List<EmailDTO>> getLatestEmails(
            @RequestParam String refreshToken,
            @RequestParam(defaultValue = "5") int limit) {
        List<EmailDTO> emails = emailService.listLatestEmails(refreshToken, limit);
        return ResponseEntity.ok(emails);
    }

    @GetMapping("/search")
    public ResponseEntity<List<EmailDTO>> searchEmails(
            @RequestParam String refreshToken,
            @RequestParam(defaultValue = "") String text) {
        List<EmailDTO> result = emailService.searchEmailsWithRefreshToken(refreshToken, text);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search-query")
    public ResponseEntity<List<EmailDTO>> searchEmailsByQuery(
            @RequestParam String refreshToken,
            @RequestParam String text) {
        List<EmailDTO> result = emailService.searchEmailsByQuery(refreshToken, text);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/search-ids")
    public ResponseEntity<List<String>> searchEmailIdsByQuery(
            @RequestParam String refreshToken,
            @RequestParam String text) {
        List<String> ids = emailService.searchEmailIdsByQuery(refreshToken, text);
        return ResponseEntity.ok(ids);
    }

    @GetMapping("/read")
    public ResponseEntity<EmailDTO> getEmailById(
            @RequestParam String refreshToken,
            @RequestParam String messageId) {
        EmailDTO email = emailService.getEmailById(refreshToken, messageId);
        return ResponseEntity.ok(email);
    }

    @GetMapping("/thread")
    public ResponseEntity<List<EmailDTO>> getThreadByEmailId(
            @RequestParam String refreshToken,
            @RequestParam String messageId) {
        List<EmailDTO> thread = emailService.getThreadByEmailId(refreshToken, messageId);
        return ResponseEntity.ok(thread);
    }

    @GetMapping("/attachments")
    public ResponseEntity<List<Attachment>> getAttachmentsByEmailId(
            @RequestParam String refreshToken,
            @RequestParam String messageId) {
        List<Attachment> attachments = emailService.getAttachmentsByEmailId(refreshToken, messageId);
        return ResponseEntity.ok(attachments);
    }

    @GetMapping("/attachments/download")
    public ResponseEntity<byte[]> downloadAttachmentFile(
            @RequestParam String refreshToken,
            @RequestParam String messageId,
            @RequestParam String attachmentId) {
        Attachment attachment = emailService.downloadAttachment(refreshToken, messageId, attachmentId);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + attachment.getName())
                .header("Content-Type", attachment.getContentType())
                .body(attachment.getContentBytes());
    }
}
