package com.outlook.integration.repositories;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.models.FileAttachment;
import com.microsoft.graph.models.MailFolder;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.User;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.AttachmentCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.MailFolderCollectionPage;
import com.microsoft.graph.requests.MessageCollectionPage;
import com.outlook.integration.dtos.Attachment;
import com.outlook.integration.dtos.EmailDTO;
import com.outlook.integration.utils.TokenUtil;

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

        MessageCollectionPage messagePage = graphClient.users(userId).messages().buildRequest()
                .select("id,subject")
                .top(10)
                .get();

        List<String> subjects = new ArrayList<>();
        if (messagePage != null && messagePage.getCurrentPage() != null) {
            messagePage.getCurrentPage().forEach(msg -> subjects.add(msg.subject));
        }
        return subjects;
    }


    public List<EmailDTO> listLatestEmails(String refreshToken, int limit) {
        String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);
        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

        GraphServiceClient<?> graphClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();

        var foldersPage = graphClient.me().mailFolders()
                .buildRequest()
                .select("id,displayName")
                .get();

        List<Message> allMessages = new ArrayList<>();

        for (var folder : foldersPage.getCurrentPage()) {
            if ("Drafts".equalsIgnoreCase(folder.displayName)) {
                continue;
            }

            try {
                var messagesPage = graphClient.me()
                        .mailFolders(folder.id)
                        .messages()
                        .buildRequest()
                        .select("id,subject,from,body,receivedDateTime,hasAttachments,toRecipients,ccRecipients,bccRecipients,isDraft")
                        .orderBy("receivedDateTime desc")
                        .top(limit)
                        .get();

                List<Message> validMessages = messagesPage.getCurrentPage().stream()
                        .filter(msg -> msg.isDraft == null || !msg.isDraft)
                        .toList();

                for (Message msg : validMessages) {
                    msg.additionalDataManager().put("folderName", new com.google.gson.JsonPrimitive(folder.displayName));
                }

                allMessages.addAll(validMessages);

            } catch (Exception e) {
                System.err.println("Erro ao ler mensagens da pasta: " + folder.displayName);
            }
        }

        return mapMessagesToDTO(allMessages.stream().limit(limit).toList());
    }

    public List<EmailDTO> listLatestEmailsWithoutUserId(String refreshToken, int limit) {
        String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);
        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

        GraphServiceClient<?> graphClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();

        var foldersPage = graphClient.me().mailFolders()
                .buildRequest()
                .select("id,displayName")
                .get();

        List<Message> allMessages = new ArrayList<>();

        for (var folder : foldersPage.getCurrentPage()) {
            if ("Drafts".equalsIgnoreCase(folder.displayName)) {
                continue;
            }

            try {
                var messagesPage = graphClient.me()
                        .mailFolders(folder.id)
                        .messages()
                        .buildRequest()
                        .select("id,subject,from,body,receivedDateTime,hasAttachments,toRecipients,ccRecipients,bccRecipients,isDraft")
                        .orderBy("receivedDateTime desc")
                        .top(limit)
                        .get();

                List<Message> validMessages = messagesPage.getCurrentPage().stream()
                        .filter(msg -> msg.isDraft == null || !msg.isDraft)
                        .toList();

                for (Message msg : validMessages) {
                    msg.additionalDataManager().put("folderName", new com.google.gson.JsonPrimitive(folder.displayName));
                }

                allMessages.addAll(validMessages);

            } catch (Exception e) {
                System.err.println("Erro ao ler mensagens da pasta: " + folder.displayName);
            }
        }

        return mapMessagesToDTO(allMessages.stream().limit(limit).toList());
    }


    
    public List<EmailDTO> searchEmailsByUserIdAndQuery(String refreshToken, String text) {
        String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);

        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

        GraphServiceClient<?> graphClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();

        List<QueryOption> options = List.of(new QueryOption("$search", "\"" + text + "\""));

        MessageCollectionPage messagesPage = graphClient.me().messages()
                .buildRequest(options)
                .select("id,subject,from,body,receivedDateTime,hasAttachments,toRecipients,ccRecipients,bccRecipients,parentFolderId,isDraft")
                .top(10)
                .get();

        List<Message> filteredMessages = messagesPage.getCurrentPage().stream()
                .filter(msg -> msg.isDraft == null || !msg.isDraft)
                .toList();
     
        for (Message msg : filteredMessages) {
            if (msg.parentFolderId != null) {
                try {
                    String folderName = getFolderName(msg.parentFolderId, refreshToken);
                    msg.additionalDataManager().put("folderName", new com.google.gson.JsonPrimitive(folderName));
                } catch (Exception e) {
                    msg.additionalDataManager().put("folderName", new com.google.gson.JsonPrimitive("Desconhecida"));
                }
            }
        }

        return mapMessagesToDTO(filteredMessages);
    }





    public List<EmailDTO> searchEmailsWithRefreshToken(String refreshToken, String text) {
        String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);

        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

        GraphServiceClient<?> graphClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();

        List<QueryOption> options = List.of(new QueryOption("$search", "\"" + text + "\""));

        MessageCollectionPage messagesPage = graphClient.me().messages().buildRequest(options)
                .select("id,subject,from,body,receivedDateTime,hasAttachments,toRecipients,ccRecipients,bccRecipients,parentFolderId,isDraft")
                .top(10)
                .get();

        List<Message> validMessages = messagesPage.getCurrentPage().stream()
                .filter(msg -> msg.isDraft == null || !msg.isDraft)
                .toList();

        for (Message msg : validMessages) {
            if (msg.parentFolderId != null) {
                try {
                    String folderName = getFolderName(msg.parentFolderId, refreshToken);
                    msg.additionalDataManager().put("folderName", new com.google.gson.JsonPrimitive(folderName));
                } catch (Exception e) {
                    msg.additionalDataManager().put("folderName", new com.google.gson.JsonPrimitive("Desconhecida"));
                }
            }
        }

        return mapMessagesToDTO(validMessages);
    }




    public List<String> searchEmailIdsByQuery(String refreshToken, String text) {
        String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);

        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

        GraphServiceClient<?> graphClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();

        List<QueryOption> options = List.of(new QueryOption("$search", "\"" + text + "\""));

        MessageCollectionPage messages = graphClient.me().messages()
                .buildRequest(options)
                .select("id,conversationId,isDraft")
                .top(50) // opcional: define um limite razoável
                .get();

        Set<String> uniqueIds = new HashSet<>();
        if (messages != null && messages.getCurrentPage() != null) {
            for (Message msg : messages.getCurrentPage()) {
                if (msg.isDraft != null && msg.isDraft) {
                    continue;
                }

                if (msg.conversationId == null || msg.conversationId.equals(msg.id)) {
                    uniqueIds.add(msg.id);
                } else {
                    uniqueIds.add(msg.conversationId);
                }
            }
        }

        return new ArrayList<>(uniqueIds);
    }


    
    public User getUserInfo(String refreshToken) {
        String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);

        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

        GraphServiceClient<?> graphClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();

        return graphClient.me()
                .buildRequest()
                .select("id,displayName,mail,userPrincipalName") // opcional: já seleciona o necessário
                .get();
    }


    public EmailDTO getEmailById(String refreshToken, String messageId) {
        String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);

        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

        GraphServiceClient<?> graphClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();

        Message message = graphClient.me().messages(messageId).buildRequest()
                .select("id,subject,from,toRecipients,ccRecipients,bccRecipients,bodyPreview,receivedDateTime,conversationId,parentFolderId")
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

        if (message.parentFolderId != null) {
            try {
                String folderName = getFolderName(message.parentFolderId, refreshToken);
                dto.setFolderName(folderName);
            } catch (Exception e) {
                dto.setFolderName("Unknown");
            }
        }

        return dto;
    }

    public List<EmailDTO> getThreadByEmailId(String refreshToken, String messageId) {
        String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);

        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

        GraphServiceClient<?> graphClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();

        Message baseMessage = graphClient.me().messages(messageId).buildRequest()
                .select("conversationId")
                .get();

        String conversationId = baseMessage.conversationId;

        List<QueryOption> options = List.of(
                new QueryOption("$filter", "conversationId eq '" + conversationId + "'")
        );

        MessageCollectionPage threadMessagesPage = graphClient.me().messages()
                .buildRequest(options)
                .select("id,subject,from,body,bodyPreview,receivedDateTime,conversationId,toRecipients,ccRecipients,bccRecipients,hasAttachments,parentFolderId,isDraft")
                .get();

        List<Message> filteredMessages = threadMessagesPage.getCurrentPage().stream()
                .filter(msg -> msg.isDraft == null || !msg.isDraft)
                .toList();

        for (Message msg : filteredMessages) {
            if (msg.parentFolderId != null) {
                try {
                    String folderName = getFolderName(msg.parentFolderId, refreshToken);
                    msg.additionalDataManager().put("folderName", new com.google.gson.JsonPrimitive(folderName));
                } catch (Exception e) {
                    msg.additionalDataManager().put("folderName", new com.google.gson.JsonPrimitive("Desconhecida"));
                }
            }
        }

        return mapMessagesToDTO(filteredMessages);
    }



    public List<Attachment> getAttachmentsByEmailId(String refreshToken, String messageId) {
        String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);

        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

        GraphServiceClient<?> graphClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();

        AttachmentCollectionPage attachmentsPage = graphClient.me()
                .messages(messageId)
                .attachments()
                .buildRequest()
                .get();

        List<Attachment> attachmentsList = new ArrayList<>();

        if (attachmentsPage != null && attachmentsPage.getCurrentPage() != null) {
            for (com.microsoft.graph.models.Attachment att : attachmentsPage.getCurrentPage()) {
                attachmentsList.add(new Attachment(att.id, att.name, att.contentType, null));
            }
        }

        return attachmentsList;
    }


    public Attachment downloadAttachmentContent(String refreshToken, String messageId, String attachmentId) {
        String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);

        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

        GraphServiceClient<?> graphClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .buildClient();

        com.microsoft.graph.models.Attachment attachment = graphClient.me()
                .messages(messageId)
                .attachments(attachmentId)
                .buildRequest()
                .get();

        if (attachment instanceof FileAttachment fileAttachment) {
            return new Attachment(
                    fileAttachment.id,
                    fileAttachment.name,
                    fileAttachment.contentType,
                    fileAttachment.contentBytes
            );
        }

        throw new IllegalArgumentException("O attachment não é um FileAttachment válido ou não possui conteúdo.");
    }


    public String getAppAccessToken() {
        try {
            TokenRequestContext context = new TokenRequestContext()
                    .addScopes("https://graph.microsoft.com/.default");

            return new ClientSecretCredentialBuilder()
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .tenantId(tenantId)
                    .build()
                    .getToken(context)
                    .block()
                    .getToken();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao obter access token da aplicação: " + e.getMessage(), e);
        }
    }


    private GraphServiceClient<?> createGraphClientWithAppToken() {
        try {
            String token = getAppAccessToken();

            IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(token);

            return GraphServiceClient.builder()
                    .authenticationProvider(authProvider)
                    .buildClient();

        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar GraphServiceClient com token da aplicação: " + e.getMessage(), e);
        }
    }

    
  
    private List<EmailDTO> mapMessagesToDTO(List<Message> messageList) {
        List<EmailDTO> result = new ArrayList<>();

        if (messageList == null || messageList.isEmpty()) {
            return result;
        }

        for (Message msg : messageList) {
            EmailDTO dto = new EmailDTO();
            dto.setId(msg.id);
            dto.setConversationId(msg.conversationId);
            dto.setSubject(msg.subject);

            if (msg.from != null && msg.from.emailAddress != null) {
                dto.setFrom(msg.from.emailAddress.address);
            }

            if (msg.body != null && msg.body.content != null) {
                String html = msg.body.content;
                dto.setHtmlBody(html);
                dto.setTextBody(Jsoup.parse(html).text());
            }

            if (msg.receivedDateTime != null) {
                dto.setDate(msg.receivedDateTime.toLocalDateTime());
            }

            dto.setHasAttachments(msg.hasAttachments);

            if (msg.additionalDataManager() != null && msg.additionalDataManager().containsKey("folderName")) {
                dto.setFolderName(msg.additionalDataManager().get("folderName").getAsString());
            }

            result.add(dto);
        }

        return result;
    }


    private String getFolderName(String folderId, String refreshToken) {
        try {
            String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);

            IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

            GraphServiceClient<?> graphClient = GraphServiceClient.builder()
                    .authenticationProvider(authProvider)
                    .buildClient();

            MailFolder folder = graphClient.me()
                    .mailFolders(folderId)
                    .buildRequest()
                    .select("displayName")
                    .get();

            return folder.displayName != null ? folder.displayName : "Desconhecida";

        } catch (Exception e) {
            return "Desconhecida";
        }
    }


    private List<EmailDTO> mapMessagesToDTO(MessageCollectionPage messages, String refreshToken) {
        List<EmailDTO> result = new ArrayList<>();

        if (messages == null || messages.getCurrentPage() == null) {
            return result;
        }

        for (Message msg : messages.getCurrentPage()) {
            EmailDTO dto = new EmailDTO();
            dto.setId(msg.id);
            dto.setConversationId(msg.conversationId);
            dto.setSubject(msg.subject);

            if (msg.from != null && msg.from.emailAddress != null) {
                dto.setFrom(msg.from.emailAddress.address);
            }

            if (msg.body != null && msg.body.content != null) {
                String html = msg.body.content;
                dto.setHtmlBody(html);
                dto.setTextBody(Jsoup.parse(html).text());
            }

            if (msg.receivedDateTime != null) {
                dto.setDate(msg.receivedDateTime.toLocalDateTime());
            }

            dto.setHasAttachments(msg.hasAttachments != null && msg.hasAttachments);

            if (msg.toRecipients != null) {
                dto.setTo(msg.toRecipients.stream()
                        .map(r -> r.emailAddress != null ? r.emailAddress.address : null)
                        .filter(addr -> addr != null)
                        .toList());
            }

            if (msg.ccRecipients != null) {
                dto.setCc(msg.ccRecipients.stream()
                        .map(r -> r.emailAddress != null ? r.emailAddress.address : null)
                        .filter(addr -> addr != null)
                        .toList());
            }

            if (msg.bccRecipients != null) {
                dto.setBcc(msg.bccRecipients.stream()
                        .map(r -> r.emailAddress != null ? r.emailAddress.address : null)
                        .filter(addr -> addr != null)
                        .toList());
            }

            if (msg.parentFolderId != null) {
                try {
                    String folderName = getFolderName(msg.parentFolderId, refreshToken);
                    dto.setFolderName(folderName);
                } catch (Exception e) {
                    dto.setFolderName("Desconhecida");
                }
            }

            result.add(dto);
        }

        return result;
    }

}
