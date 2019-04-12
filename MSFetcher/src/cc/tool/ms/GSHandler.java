package cc.tool.ms;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ClearValuesRequest;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import cc.tools.ms.utilities.PropertiesHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GSHandler {
    private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static EnvironHandler environHangler = EnvironHandler.getExistedEnvironHandler();
    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     * @throws GeneralSecurityException 
     */

    
    private static Sheets getShService() throws GeneralSecurityException, IOException {
    	
    		
	    	NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
	    	
	 	File initialFile = new File(environHangler.getCredFilePath());
	    InputStream in = new FileInputStream(initialFile);
	    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
	
	    // Build flow and trigger user authorization request.
	    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
	            HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
	            .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(environHangler.getConfPath())))
	            .setAccessType("offline")
	            .build();
	    LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
	    
	    Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    		
	    
	    
	    
	    
	    
		Sheets sheetsService = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, setTimeout(credential,60000))
	              .setApplicationName(APPLICATION_NAME)
	              .build();
        
        
        return sheetsService;
    }
    
   
    private static HttpRequestInitializer setTimeout(final HttpRequestInitializer initializer, final int timeout) {
        return request -> {
            initializer.initialize(request);
            request.setConnectTimeout(3 * 60000);
            request.setReadTimeout(3 * 60000);
        
        };
    }
   
    
    /**
     * Prints the names and majors of students in a sample spreadsheet:
     * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
     * https://docs.google.com/spreadsheets/d/1dZNQNZRfXS-5knRgSFO7-mA-0fFgCfnSwKQ2yIyMOls/edit#gid=0
     */
    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        final String spreadsheetId = "1dZNQNZRfXS-5knRgSFO7-mA-0fFgCfnSwKQ2yIyMOls";

   
    }
    
    
    private String sheetId;
    private String sheetName;
    private List<List<Object>> values;
    private List<List<List<Object>>> listDataSet = new ArrayList<List<List<Object>>>();
	private Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());
    
    public GSHandler(List<List<Object>> ds) {
    		sheetId = PropertiesHandler.getPropsHandler().getMainProps().getProperty("googlesheet.upload.sheetId").trim();
    		sheetName = PropertiesHandler.getPropsHandler().getMainProps().getProperty("googlesheet.upload.sheetName").trim();
    		values = ds;
    		int cnt = 1;
    		List<List<Object>> tmpValue = new ArrayList<List<Object>>();
    		
    		
    		for(int i=0; i<values.size();i++) {
    			//count 1000 to send a batch
    			tmpValue.add(values.get(i));
    			
    			if(cnt%2000 == 0 || i == values.size()-1 ) {

    				listDataSet.add(tmpValue);
    				if(i < values.size()-1)tmpValue = new ArrayList<List<Object>>();
    			}
    			cnt ++;
    		}
    		
    }
    
    //        final String spreadsheetId = "1dZNQNZRfXS-5knRgSFO7-mA-0fFgCfnSwKQ2yIyMOls";
    
    public boolean upload() {
    	
        
        Sheets sheetsService = null;
        try {
        		sheetsService = getShService();
			//clean
			sheetsService.spreadsheets().values().clear(sheetId, sheetName+"!1:10000", new ClearValuesRequest()).execute();
        } catch (Exception e) {
			log.error("Upload to GoogleSheet with Error:{}",e);
		}

		//batch upload
        
        long row = 1;
        for(List<List<Object>> tmpValues:listDataSet) {
	        	try {
	   			 
	   			 //batch Upload	
	   			 ValueRange body = new ValueRange().setValues(tmpValues);	   			 
	   		     UpdateValuesResponse result = sheetsService.spreadsheets().values().update(sheetId, sheetName+"!A"+ row, body).setValueInputOption("RAW").execute();
	   		     log.info("GoogleSheet: {} have been updated.", result.getUpdatedRange());
	   		     row = row + tmpValues.size() ;
	   		} catch (Exception e) {
	   			log.error("Upload to GoogleSheet with Error:{}",e);
	   		}
	        	
        }
		
    	
    		return true;
    }
    
    
   
}