package com.outlook.integration.dtos;

public class Attachment {
		
		private String id;
		private String name;
		private String contentType;
		private byte[] contentBytes;
		
		
		public Attachment() {
			super();
		}
		public Attachment(String id, String name, String contentType, byte[] contentBytes) {
			super();
			this.id = id;
			this.name = name;
			this.contentType = contentType;
			this.contentBytes = contentBytes;
		}
		
		public String getId() { 
			return id; 
		}
		
	    public void setId(String id) { 
	    	this.id = id; 
	    }

	    public String getName() { 
	    	return name; 
	    }
	    
	    public void setName(String name) { 
	    	this.name = name; 
	    }

	    public String getContentType() { 
	    	return contentType; 
	    }
	    
	    public void setContentType(String contentType) { 
	    	this.contentType = contentType; 
	    }

	    public byte[] getContentBytes() { 
	    	return contentBytes; 
	    }
	    
	    public void setContentBytes(byte[] contentBytes) { 
	    	this.contentBytes = contentBytes; 
	    }
		
	
}
