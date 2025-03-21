package com.outlook.integration.repositories;

import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.models.FileAttachment;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.AttachmentCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.MessageCollectionPage;
import com.outlook.integration.dtos.Attachment;
import com.outlook.integration.dtos.EmailDTO;
import com.outlook.integration.utils.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Repository
public class OutlookRepository {

    @Value("${outlook.client.id}")
    private String clientId;

    @Value("${outlook.client.secret}")
    private String clientSecret;

    @Value("${outlook.tenant.id}")
    private String tenantId;

    @Autowired
    private TokenUtil tokenUtil;

    public List<String> listEmailSubjects(String userId) {
        GraphServiceClient<?> graphClient = createGraphClientWithAppToken();

        MessageCollectionPage messagePage = graphClient.users(userId).messages().buildRequest().select("id,subject")
                .top(10).get();

        List<String> subjects = new ArrayList<>();
        if (messagePage != null && messagePage.getCurrentPage() != null) {
            messagePage.getCurrentPage().forEach(msg -> subjects.add(msg.subject));
        }
        return subjects;
    }

    public List<EmailDTO> listLatestEmails(String userId, int limit) {
        GraphServiceClient<?> graphClient = createGraphClientWithAppToken();

        MessageCollectionPage messagePage = graphClient.users(userId).messages().buildRequest()
                .select("id,subject,from,bodyPreview,receivedDateTime").orderBy("receivedDateTime desc").top(limit)
                .get();

        return mapMessagesToDTO(messagePage);
    }

    public List<EmailDTO> listLatestEmailsWithoutUserId(String refreshToken, int limit) {
        String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);

        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

        GraphServiceClient<?> graphClient = GraphServiceClient.builder().authenticationProvider(authProvider)
                .buildClient();

        MessageCollectionPage messagePage = graphClient.me().messages().buildRequest()
                .select("id,subject,from,bodyPreview,receivedDateTime").orderBy("receivedDateTime desc").top(limit)
                .get();

        return mapMessagesToDTO(messagePage);
    }
    
    public List<EmailDTO> searchEmailsByUserIdAndQuery(String userId, String text) {
        GraphServiceClient<?> graphClient = createGraphClientWithAppToken();

        List<QueryOption> options = List.of(new QueryOption("$search", "\"" + text + "\""));

        MessageCollectionPage messages = graphClient.users(userId).messages().buildRequest(options)
                .select("id,subject,from,bodyPreview,receivedDateTime").get();

        return mapMessagesToDTO(messages);
    }


    public List<EmailDTO> searchEmailsWithRefreshToken(String refreshToken, String userId, String text) {
        String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);

        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

        GraphServiceClient<?> graphClient = GraphServiceClient.builder().authenticationProvider(authProvider)
                .buildClient();

        List<QueryOption> options = List.of(new QueryOption("$search", "\"" + text + "\""));

        MessageCollectionPage messages = graphClient.users(userId).messages().buildRequest(options)
                .select("id,subject,from,bodyPreview,receivedDateTime").top(10).get();

        return mapMessagesToDTO(messages);
    }

    public List<String> searchEmailIdsByQuery(String refreshToken, String text) {
        String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);

        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

        GraphServiceClient<?> graphClient = GraphServiceClient.builder().authenticationProvider(authProvider)
                .buildClient();

        List<QueryOption> options = List.of(new QueryOption("$search", "\"" + text + "\""));

        MessageCollectionPage messages = graphClient.me().messages().buildRequest(options).select("id").top(10).get();

        List<String> ids = new ArrayList<>();
        if (messages != null && messages.getCurrentPage() != null) {
            for (Message msg : messages.getCurrentPage()) {
                ids.add(msg.id);
            }
        }
        return ids;
    }

    public EmailDTO getEmailById(String refreshToken, String messageId) {
        String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);

        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

        GraphServiceClient<?> graphClient = GraphServiceClient.builder().authenticationProvider(authProvider)
                .buildClient();

        Message message = graphClient.me().messages(messageId).buildRequest().select(
                "id,subject,from,toRecipients,ccRecipients,bccRecipients,bodyPreview,receivedDateTime,conversationId")
                .get();

        EmailDTO dto = new EmailDTO();
        dto.setId(message.id);
        dto.setConversationId(message.conversationId);
        dto.setSubject(message.subject);
        dto.setTextBody(message.bodyPreview);
        if (message.from != null && message.from.emailAddress != null) {
            dto.setFrom(message.from.emailAddress.address);
        }
        if (message.receivedDateTime != null) {
            dto.setDate(message.receivedDateTime.toLocalDateTime());
        }
        return dto;
    }

    public List<EmailDTO> getThreadByEmailId(String refreshToken, String messageId) {
        String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);

        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

        GraphServiceClient<?> graphClient = GraphServiceClient.builder().authenticationProvider(authProvider)
                .buildClient();

        Message message = graphClient.me().messages(messageId).buildRequest().select("conversationId").get();

        String conversationId = message.conversationId;

        List<QueryOption> options = List.of(new QueryOption("$filter", "conversationId eq '" + conversationId + "'"));

        MessageCollectionPage threadMessages = graphClient.me().messages().buildRequest(options).select(
                "id,subject,from,bodyPreview,receivedDateTime,conversationId,toRecipients,ccRecipients,bccRecipients")
                .get();

        return mapMessagesToDTO(threadMessages);
    }

    public List<Attachment> getAttachmentsByEmailId(String refreshToken, String messageId) {
        String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);

        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

        GraphServiceClient<?> graphClient = GraphServiceClient.builder().authenticationProvider(authProvider)
                .buildClient();

        AttachmentCollectionPage attachments = graphClient.me().messages(messageId).attachments().buildRequest().get();

        List<Attachment> attachmentsList = new ArrayList<>();
        attachments.getCurrentPage().forEach(att -> {
            Attachment attachment = new Attachment(att.id, att.name, att.contentType, null);
            attachmentsList.add(attachment);
        });

        return attachmentsList;
    }

    public Attachment downloadAttachmentContent(String refreshToken, String messageId, String attachmentId) {
        String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);

        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

        GraphServiceClient<?> graphClient = GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();

        com.microsoft.graph.models.Attachment attachment = graphClient
                .me()
                .messages(messageId)
                .attachments(attachmentId)
                .buildRequest()
                .get();

        if (attachment instanceof FileAttachment fileAttachment) {
            return new Attachment(fileAttachment.id, fileAttachment.name, fileAttachment.contentType, fileAttachment.contentBytes);
        }

        throw new RuntimeException("Attachment não é um FileAttachment válido ou não possui conteúdo.");
    }

    public String getAppAccessToken() {
        TokenRequestContext context = new TokenRequestContext().addScopes("https://graph.microsoft.com/.default");
        return new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build()
                .getToken(context)
                .block()
                .getToken();
    }

    private GraphServiceClient<?> createGraphClientWithAppToken() {
        String token = getAppAccessToken();
        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(token);
        return GraphServiceClient.builder().authenticationProvider(authProvider).buildClient();
    }

    private List<EmailDTO> mapMessagesToDTO(MessageCollectionPage messages) {
        List<EmailDTO> result = new ArrayList<>();
        if (messages != null && messages.getCurrentPage() != null) {
            for (Message msg : messages.getCurrentPage()) {
                EmailDTO dto = new EmailDTO();
                dto.setId(msg.id);
                dto.setConversationId(msg.conversationId);
                dto.setSubject(msg.subject);
                if (msg.from != null && msg.from.emailAddress != null) {
                    dto.setFrom(msg.from.emailAddress.address);
                }
                dto.setTextBody(msg.bodyPreview);
                if (msg.receivedDateTime != null) {
                    dto.setDate(msg.receivedDateTime.toLocalDateTime());
                }
                result.add(dto);
            }
        }
        return result;
    }
}
