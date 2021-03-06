package com.walmart.ocr.util;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.http.auth.AUTH;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.storage.StorageScopes;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionScopes;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.AnnotateImageResponse;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.ImageSource;
import com.google.common.collect.ImmutableList;

public class GVision {
	private static final String APPLICATION_NAME = "ust-smart-ocr";
	private static final int MAX_RESULTS = 6;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private Vision vision;

	public GVision() {
		vision = authenticateGoogleAPI();
	}

	/**
	 * Connects to the Vision API using Application Default Credentials.
	 */
	private Vision authenticateGoogleAPI() {
		try {
			InputStream resourceAsStream = AUTH.class.getClassLoader().getResourceAsStream("USTSmartOCR-bc067713a664.json");

			GoogleCredential credential = GoogleCredential.fromStream(resourceAsStream);
			if (credential.createScopedRequired()) {
			      Collection<String> scopes = StorageScopes.all();
			      credential = credential.createScoped(scopes);
			    }
			//GoogleCredential credential = GoogleCredential.getApplicationDefault().createScoped(VisionScopes.all());
			JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
			return new Vision.Builder(GoogleNetHttpTransport.newTrustedTransport(), jsonFactory, credential)
					.setApplicationName(APPLICATION_NAME)
					.build();
		} catch (IOException e) {
			logger.error("Unable to access Google Vision API", e);
		} catch (GeneralSecurityException e) {
			logger.error("Unable to authenticate with Google Vision API", e);
		}
		return vision;
	}

	/**
	 * Gets up to {@code maxResults} text for an image stored at
	 * {@code uri}.
	 */
	public AnnotateImageResponse doOCR(File file) throws Exception {

		if (vision == null)
			authenticateGoogleAPI();
		Base64 base64 = new Base64();
		byte[] encoded =base64.encode(FileUtils.readFileToByteArray(file));
		
		FileInputStream fileInputStream=null;
        
       // File file = new File("C:\\testing.txt");
        
        byte[] bFile = new byte[(int) file.length()];
        
        
	    fileInputStream = new FileInputStream(file);
	    fileInputStream.read(bFile);
	    fileInputStream.close();
	    
		AnnotateImageRequest request = new AnnotateImageRequest()
			.setImage(new Image().encodeContent(bFile))
			.setFeatures(ImmutableList.of(new Feature().setType("TEXT_DETECTION").setMaxResults(MAX_RESULTS),
					new Feature().setType("LOGO_DETECTION").setMaxResults(MAX_RESULTS),
					new Feature().setType("LABEL_DETECTION").setMaxResults(MAX_RESULTS)));
		Vision.Images.Annotate annotate;
		try {
			
			annotate = vision.images()
					.annotate(new BatchAnnotateImagesRequest().setRequests(ImmutableList.of(request)));
			BatchAnnotateImagesResponse batchResponse = annotate.execute();
			assert batchResponse.getResponses().size() == 1;
			AnnotateImageResponse response = batchResponse.getResponses().get(0);
			if (response.getError() != null) {
				logger.error("Failed to process document ["+file.getName()+"]");
				logger.error(response.getError().getMessage());
				throw new Exception(response.getError().getMessage());
			} else {
				
				ObjectMapper objectMapper = new ObjectMapper();
				objectMapper.writeValue(
					    new FileOutputStream("output.json"), response);
				return response;				
			}
		} catch (IOException e) {
			logger.error("Failed to process document ["+file.getName()+"]",e);
			throw e;
		}
	}
	public static void main (String args[]){
		GVision gvision = new GVision();
		try {
			File myFile  = new File("Toys Lego 3.jpeg");
			gvision.doOCR(myFile);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}