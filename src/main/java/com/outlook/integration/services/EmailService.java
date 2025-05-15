package com.outlook.integration.services;

import com.outlook.integration.dtos.Attachment;
import com.outlook.integration.dtos.EmailDTO;
import com.outlook.integration.repositories.OutlookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {

    @Autowired
    private OutlookRepository outlookRepository;

    public List<String> listEmailSubjects(String userId) {
        return outlookRepository.listEmailSubjects(userId);
    }

    public List<EmailDTO> searchEmailsByQuery(String userId, String text) {
        return outlookRepository.searchEmailsByUserIdAndQuery(userId, text);
    }

    public List<EmailDTO> listLatestEmails(String userId, int limit) {
        return outlookRepository.listLatestEmails(userId, limit);
    }

    public List<EmailDTO> searchEmailsWithRefreshToken(String refreshToken, String userId, String text) {
        return outlookRepository.searchEmailsWithRefreshToken(refreshToken, userId, text);
    }

    public List<EmailDTO> listLatestEmailsWithoutUserId(String refreshToken, int limit) {
        return outlookRepository.listLatestEmailsWithoutUserId(refreshToken, limit);
    }

    public EmailDTO getEmailById(String refreshToken, String messageId) {
        return outlookRepository.getEmailById(refreshToken, messageId);
    }

    public List<String> searchEmailIdsByQuery(String refreshToken, String text) {
        return outlookRepository.searchEmailIdsByQuery(refreshToken, text);
    }

    public List<EmailDTO> getThreadByEmailId(String refreshToken, String messageId) {
        return outlookRepository.getThreadByEmailId(refreshToken, messageId);
    }

    public List<Attachment> getAttachmentsByEmailId(String refreshToken, String messageId) {
        return outlookRepository.getAttachmentsByEmailId(refreshToken, messageId);
    }

    public Attachment downloadAttachment(String refreshToken, String messageId, String attachmentId) {
        return outlookRepository.downloadAttachmentContent(refreshToken, messageId, attachmentId);
    }
    
  

}
