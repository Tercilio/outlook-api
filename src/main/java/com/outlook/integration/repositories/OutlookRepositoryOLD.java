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
public class OutlookRepositoryOLD {

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

        return mapMessagesToDTO(messagePage,userId);
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





    
    public List<EmailDTO> searchEmailsByUserIdAndQuery(String userId, String text) {
        GraphServiceClient<?> graphClient = createGraphClientWithAppToken();

        List<QueryOption> options = List.of(new QueryOption("$search", "\"" + text + "\""));

        MessageCollectionPage messages = graphClient.users(userId).messages().buildRequest(options)
                .select("id,subject,from,bodyPreview,receivedDateTime,hasAttachments").get();

        return mapMessagesToDTO(messages, userId);
    }


    public List<EmailDTO> searchEmailsWithRefreshToken(String refreshToken, String userId, String text) {
        String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);

        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

        GraphServiceClient<?> graphClient = GraphServiceClient.builder().authenticationProvider(authProvider)
                .buildClient();

        List<QueryOption> options = List.of(new QueryOption("$search", "\"" + text + "\""));

        MessageCollectionPage messages = graphClient.users(userId).messages().buildRequest(options)
                .select("id,subject,from,bodyPreview,receivedDateTime,hasAttachments").top(10).get();

        return mapMessagesToDTO(messages,refreshToken);
    }

    public List<String> searchEmailIdsByQuery(String refreshToken, String text) {
        String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);

        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

        GraphServiceClient<?> graphClient = GraphServiceClient.builder().authenticationProvider(authProvider)
                .buildClient();

        List<QueryOption> options = List.of(new QueryOption("$search", "\"" + text + "\""));

        MessageCollectionPage messages = graphClient.me().messages()
                .buildRequest(options)
                .select("id,conversationId,isDraft") 
                .get();

        List<String> ids = new ArrayList<>();
        if (messages != null && messages.getCurrentPage() != null) {
            for (Message msg : messages.getCurrentPage()) {
            	if(msg.isDraft) {
            		continue;
            	}
                if (msg.conversationId == null || msg.conversationId.equals(msg.id)) {
                    ids.add(msg.id);
                } else {
                	ids.add(msg.conversationId);
                	}
                
            }
        }
        
        Set<String> set = new HashSet<>(ids);
        ids.clear();
        ids.addAll(set);
        
        return ids;
    }


    
    public User getUserInfo(String refreshToken){
    	
    	String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);
    	IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);
    	
    	GraphServiceClient<?> graphClient = GraphServiceClient.builder().authenticationProvider(authProvider)
                .buildClient();
    	
		return graphClient.me().buildRequest().get();
    	
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

        //Message message = graphClient.me().messages(messageId).buildRequest().select("conversationId").get();

        //String conversationId = message.conversationId;

        List<QueryOption> options = List.of(new QueryOption("$filter", "conversationId eq '" + messageId + "'"));

        MessageCollectionPage threadMessages = graphClient
                .me()
                .messages()
                .buildRequest(options)
                .select("id,subject,from,body,bodyPreview,receivedDateTime,conversationId,toRecipients,ccRecipients,bccRecipients,hasAttachments,parentFolderId")
                .get(); 

        return mapMessagesToDTO(threadMessages,refreshToken);
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
    
    
  
    private List<EmailDTO> mapMessagesToDTO(List<Message> messageList) {
        List<EmailDTO> result = new ArrayList<>();
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
                String cleanText = Jsoup.parse(html).text();
                dto.setTextBody(cleanText);
                dto.setHtmlBody(html);
            }
            
            if (msg.receivedDateTime != null) {
                dto.setDate(msg.receivedDateTime.toLocalDateTime());
            }
            
            dto.setHasAttachments(msg.hasAttachments);
            
            if (msg.additionalDataManager().containsKey("folderName")) {
                dto.setFolderName(msg.additionalDataManager().get("folderName").getAsString());
            }

            result.add(dto);
        }
        return result;
    }


    private String getFolderName(String folderId, String refreshToken) {
    	
    	String accessToken = tokenUtil.generateAccessTokenFromRefreshToken(refreshToken);

        IAuthenticationProvider authProvider = requestUrl -> CompletableFuture.completedFuture(accessToken);

        GraphServiceClient<?> graphClient = GraphServiceClient.builder().authenticationProvider(authProvider)
                .buildClient();

        List<QueryOption> options = List.of(new QueryOption("$search", "id eq " + folderId));

        MailFolderCollectionPage mailFolders = graphClient.me().mailFolders()
                .buildRequest(options)
                .select("id,displayName") 
                .get();
        
        ArrayList<MailFolder> folders = new ArrayList<>(mailFolders.getCurrentPage());
        
		return folders.get(0).displayName;

    }

    private List<EmailDTO> mapMessagesToDTO(MessageCollectionPage messages,String refreshToken) {
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

                if (msg.body != null && msg.body.content != null) {
                    String html = msg.body.content;
                    String cleanText = Jsoup.parse(html).text();
                    dto.setTextBody(cleanText);
                    dto.setHtmlBody(html);
                }

                if (msg.receivedDateTime != null) {
                    dto.setDate(msg.receivedDateTime.toLocalDateTime());
                }

                dto.setHasAttachments(msg.hasAttachments);

                // TO
                if (msg.toRecipients != null) {
                    List<String> toList = msg.toRecipients.stream()
                            .map(recipient -> recipient.emailAddress != null ? recipient.emailAddress.address : null)
                            .filter(address -> address != null)
                            .toList();
                    dto.setTo(toList);
                }

                // CC
                if (msg.ccRecipients != null) {
                    List<String> ccList = msg.ccRecipients.stream()
                            .map(recipient -> recipient.emailAddress != null ? recipient.emailAddress.address : null)
                            .filter(address -> address != null)
                            .toList();
                    dto.setCc(ccList);
                }

                // BCC
                if (msg.bccRecipients != null) {
                    List<String> bccList = msg.bccRecipients.stream()
                            .map(recipient -> recipient.emailAddress != null ? recipient.emailAddress.address : null)
                            .filter(address -> address != null)
                            .toList();
                    dto.setBcc(bccList);
                }
                
//                if (msg.parentFolderId != null) {
//                	dto.setFolderName(getFolderName(msg.parentFolderId, refreshToken));
//                    dto.setFolderName(msg.parentFolderId);
//                }

                result.add(dto);
            }
        }
        return result;
    }

}
