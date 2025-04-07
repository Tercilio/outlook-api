package com.outlook.integration.dtos;

import java.time.LocalDateTime;
import java.util.List;
import  com.outlook.integration.dtos.Attachment;

public class EmailDTO {

	private String id; // ID do e-mail
	private String conversationId; // ID da thread de conversa
	private String subject;
	private String textBody;
	private String htmlBody;
	private List<Attachment> attachments; // Lista de nomes dos anexos
	private String from;
	private List<String> to;
	private List<String> cc;
	private List<String> bcc;
	private LocalDateTime date;
	private Boolean hasAttachments;

	public EmailDTO() {
	}

		public EmailDTO(String id, String conversationId, String subject, String textBody, String htmlBody,
			List<Attachment> attachments, String from, List<String> to, List<String> cc, List<String> bcc,
			LocalDateTime date) {
		super();
		this.id = id;
		this.conversationId = conversationId;
		this.subject = subject;
		this.textBody = textBody;
		this.htmlBody = htmlBody;
		this.attachments = attachments;
		this.from = from;
		this.to = to;
		this.cc = cc;
		this.bcc = bcc;
		this.date = date;
	}
		
	public Boolean getHasAttachments() {
		return hasAttachments;
	}

	public void setHasAttachments(Boolean hasAttachments) {
		this.hasAttachments = hasAttachments;
	}
		
	public String getId() {
	    return id;
	}
	public void setId(String id) {
	    this.id = id;
	}

	public String getConversationId() {
	    return conversationId;
	}
	public void setConversationId(String conversationId) {
	    this.conversationId = conversationId;
	}

	public String getSubject() {
	    return subject;
	}
	public void setSubject(String subject) {
	    this.subject = subject;
	}

	public String getTextBody() {
	    return textBody;
	}
	public void setTextBody(String textBody) {
	    this.textBody = textBody;
	}

	public String getHtmlBody() {
	    return htmlBody;
	}
	public void setHtmlBody(String htmlBody) {
	    this.htmlBody = htmlBody;
	}


	public List<Attachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}

	public String getFrom() {
	    return from;
	}
	public void setFrom(String from) {
	    this.from = from;
	}

	public List<String> getTo() {
	    return to;
	}
	public void setTo(List<String> to) {
	    this.to = to;
	}

	public List<String> getCc() {
	    return cc;
	}
	public void setCc(List<String> cc) {
	    this.cc = cc;
	}

	public List<String> getBcc() {
	    return bcc;
	}
	public void setBcc(List<String> bcc) {
	    this.bcc = bcc;
	}

	public LocalDateTime getDate() {
	    return date;
	}
	public void setDate(LocalDateTime date) {
	    this.date = date;
	}
}