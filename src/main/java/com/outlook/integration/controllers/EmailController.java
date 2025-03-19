// EmailController.java
package com.outlook.integration.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import com.outlook.integration.dtos.Attachment;
import com.outlook.integration.dtos.EmailDTO;
import com.outlook.integration.services.EmailService;

import io.swagger.v3.oas.annotations.tags.Tag;
@RestController
@RequestMapping("/emails")
@Tag(name = "Emails", description = "Operações de leitura de e-mails Outlook") // Explicação que aparece no 
public class EmailController {

    @Autowired
    private EmailService emailService;

    @GetMapping("/subjects")
    public ResponseEntity<List<String>> listSubjects(@RequestParam String userId) {
        List<String> result = emailService.listEmailSubjects(userId);
        return ResponseEntity.ok(result);
    }


    @GetMapping("/search-query")
    public ResponseEntity<List<EmailDTO>> searchEmailsByQuery(@RequestParam String userId, @RequestParam String text) {
        List<EmailDTO> result = emailService.searchEmailsByQuery(userId, text);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/latest")
    public ResponseEntity<List<EmailDTO>> getLatestEmails(@RequestParam String userId, @RequestParam(defaultValue = "5") int limit) {
        List<EmailDTO> emails = emailService.listLatestEmails(userId, limit);
        return ResponseEntity.ok(emails);
    }

    // Para testar com token MANUAL
    @GetMapping("/search-token")
    public ResponseEntity<List<EmailDTO>> searchEmailsWithToken(
            @RequestParam String accessToken,
            @RequestParam String userId,
            @RequestParam(required = false, defaultValue = "") String text
    ) {
        List<EmailDTO> result = emailService.searchEmailsWithToken(accessToken, userId, text);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/latest-me")
    public ResponseEntity<List<EmailDTO>> getLatestEmailsMe(
            @RequestParam String accessToken,
            @RequestParam(defaultValue = "5") int limit) {
        List<EmailDTO> emails = emailService.listLatestEmailsWithoutUserId(accessToken, limit);
        return ResponseEntity.ok(emails);
    }
    
    // Ler o email passando o ID
    @GetMapping("/read")
    public ResponseEntity<EmailDTO> getEmailById(
            @RequestParam String accessToken,
            @RequestParam String messageId
    ) {
        EmailDTO email = emailService.getEmailById(accessToken, messageId);
        return ResponseEntity.ok(email);
    }


    //endpoint para retornar apenas os IDs dos e-mails por texto
    @GetMapping("/search-ids")
    public ResponseEntity<List<String>> searchEmailIdsByQuery(
            @RequestParam String accessToken,
            @RequestParam String text) {
        List<String> ids = emailService.searchEmailIdsByQuery(accessToken, text);
        return ResponseEntity.ok(ids);
    }

    //endpoint para retornar toda a thread/conversa de um e-mail pelo ID
    @GetMapping("/thread")
    public ResponseEntity<List<EmailDTO>> getThreadByEmailId(
            @RequestParam String accessToken,
            @RequestParam String messageId) {
        List<EmailDTO> thread = emailService.getThreadByEmailId(accessToken, messageId);
        return ResponseEntity.ok(thread);
    }
    
    // attachments
    @GetMapping("/attachments")
    public ResponseEntity<List<Attachment>> getAttachmentsByEmailId(
            @RequestParam String accessToken,
            @RequestParam String messageId) {
        List<Attachment> thread = emailService.getAttachmentsByEmailId(accessToken, messageId);
        return ResponseEntity.ok(thread);
    }
    
    @GetMapping("/attachments/download")
    public ResponseEntity<byte[]> downloadAttachmentFile(
            @RequestParam String accessToken,
            @RequestParam String messageId,
            @RequestParam String attachmentId) {

        Attachment attachment = emailService.downloadAttachment(accessToken, messageId, attachmentId);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + attachment.getName())
                .header("Content-Type", attachment.getContentType())
                .body(attachment.getContentBytes());
    }

}