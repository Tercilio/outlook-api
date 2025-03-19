package com.outlook.integration.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.http.IHttpRequest;
import com.microsoft.graph.models.Request;
import com.microsoft.graph.requests.AttachmentCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.outlook.integration.dtos.EmailDTO;
import com.outlook.integration.dtos.Attachment;
import com.outlook.integration.repositories.OutlookRepository;

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

    public List<EmailDTO> searchEmailsWithToken(String accessToken, String userId, String text) {
        return outlookRepository.searchEmailsWithToken(accessToken, userId, text);
    }

    public List<EmailDTO> listLatestEmailsWithoutUserId(String accessToken, int limit) {
        return outlookRepository.listLatestEmailsWithoutUserId(accessToken, limit);
    }
    
    // Método para ler o email a partir de um ID
    public EmailDTO getEmailById(String accessToken, String messageId) {
        return outlookRepository.getEmailById(accessToken, messageId);
    }


    // Método para retornar apenas os IDs dos e-mails com base em uma busca
    public List<String> searchEmailIdsByQuery(String accessToken, String text) {
        return outlookRepository.searchEmailIdsByQuery(accessToken, text);
    }

    // Método para retornar a thread completa de um e-mail a partir do ID
    public List<EmailDTO> getThreadByEmailId(String accessToken, String messageId) {
        return outlookRepository.getThreadByEmailId(accessToken, messageId);
    }
    
    // Método para retornar a thread
    public List<Attachment> getAttachmentsByEmailId(String accessToken, String messageId) {
        return outlookRepository.getAttachmentsByEmailId(accessToken, messageId);
    }
    
    public Attachment downloadAttachment(String accessToken, String messageId, String attachmentId) {
        return outlookRepository.downloadAttachmentContent(accessToken, messageId, attachmentId);
    }
}
