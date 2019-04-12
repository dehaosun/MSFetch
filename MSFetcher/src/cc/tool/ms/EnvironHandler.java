package cc.tool.ms;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.tools.ms.utilities.PropertiesHandler;

public class EnvironHandler {
	
	private Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());
	private Properties mainProps = PropertiesHandler.getPropsHandler().getMainProps();
	private ConcurrentHashMap <String,String> jsonMap = PropertiesHandler.getPropsHandler().getJSONMap();
	private String countryTag = null;
	private String folderPath = null;
	private String srcTag = null;
	private static EnvironHandler _Instance;
	
	public synchronized static EnvironHandler getEnvironHandler(String countrytTagIn, String srcTagIn, String topPath){
		if(_Instance == null)_Instance = new EnvironHandler(countrytTagIn, srcTagIn, topPath);
		return _Instance;	
	}
	
	public synchronized static EnvironHandler getExistedEnvironHandler() {
		if(_Instance == null) _Instance = new EnvironHandler("US", "US_ALL", null);
		return _Instance;
	}
	
	
	public EnvironHandler(String countrtTagIn, String srcTagIn, String topPath){
		countryTag = countrtTagIn;
		folderPath = topPath;
		srcTag = srcTagIn;
		
		//TODO : remove
//		countryTag = "US";
//		folderPath = "/Users/chuck";
	}
	

	public boolean checkEnvrion() {
		boolean rtn = false;
		
		if(checkProperties() == false) {
			log.error("Properties check failed.");
		}else if(checkPath() == false){
			log.error("Paths check failed.");
		}else if(checkHttpConnection() == false){
			log.error("Heep Connection check failed.");
		}else {
			rtn = true;
		}
		
		return rtn;
	}
	
	private boolean checkProperties() {
		boolean pass = false;
		int errorCnt = 0;
		
		if(mainProps == null) {
			log.error("mainProps not exist.");
			errorCnt++;
		}
		
		if(jsonMap == null) {
			log.error("jsonMap not exist.");
			errorCnt++;
		}
		
		
		if(errorCnt == 0) pass = true;
		return pass;
		
	}
	
	
	private boolean checkPath() {
		boolean pass = false;
		int errorCnt = 0;
		
		//Folder Check
		String countryPath = getCountryPath();
		String krPath = getKRPath();
		String othersPath = getBSISCFPath();
		String srcPath = getSourcePath();
		String srcFilePath = getSourceFilePath();
		
		setFolder(srcPath);
		setFolder(countryPath);
		setFolder(krPath);
		setFolder(othersPath);
		setFolder(srcPath);
		
		
		if(!isPathExist(srcFilePath)) {
			log.error("{} does not exist:{}","srcFilePath",srcFilePath);
			errorCnt++;
		}
		if(!isPathExist(countryPath)) {
			log.error("{} does not exist:{}","countryPath",countryPath);
			errorCnt++;
		}
		if(!isPathExist(krPath)) {
			log.error("{} does not exist:{}","krPath",krPath);
			errorCnt++;
		}
		if(!isPathExist(othersPath)) {
			log.error("{} does not exist:{}","othersPath",othersPath);
			errorCnt++;
		}
		

		if(errorCnt == 0) pass = true;
		return pass;
		
	}
	
	
	public boolean checkHttpConnection() {
		boolean rtn = false;
	    String strUrl = "http://financials.morningstar.com/ajax/exportKR2CSV.html?t=MORN";
	    HttpURLConnection urlConn = null;
	    try {
	        URL url = new URL(strUrl);
	        urlConn = (HttpURLConnection) url.openConnection();
	        urlConn.connect();
	        int responseCode = urlConn.getResponseCode();
	        if (responseCode == HttpURLConnection.HTTP_OK) {
	        		rtn = true;
	        }
	    } catch (IOException e) {
	        log.error("Error creating HTTP connection:{}",e);
	    } finally {
	    		if(urlConn != null ) urlConn.disconnect();
	    }    
	    return rtn;
	}
	
	
	
	
	private void setFolder(String folderName) {
		new File(folderName).mkdirs();
	}
	
	private boolean isPathExist(String path) {
		boolean rtn = false;
		File f = new File(path);
		if(f.exists()) { 
			rtn = true;
		}
		return rtn;
	}
	
	
	public String getTopPath() {
		String currentPath = (folderPath != null)? folderPath:Paths.get(".").toAbsolutePath().normalize().toString();	
		return currentPath;
	}
	
	public String getConfPath() {
		String currentPath = getTopPath() ;	
		String confPath = currentPath + "/ProgramFiles/conf";
		return confPath;
	}
	
	public String getJarPath() {
		String path = null ;
		try {
			path = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
		} catch (URISyntaxException e) {
			log.error("Get Jar Path Error:{}",e);
		}
		return path;
	}
	
	public String getCountryPath() {
		String currentPath = getTopPath() ;	
		String countryPath = currentPath + "/Data/" + countryTag;
		return countryPath;
	}
	
	public String getSourcePath() {
		String currentPath = getTopPath() ;
		String filepath = currentPath + "/Data/00_SourceList";
		return filepath;
	}
	
	public String getSourceFilePath() {
		String srcPath = getSourcePath();
		String filepath = srcPath + "/" + srcTag + ".csv";
		return filepath;
	}
	
	
	
	public String getRemainFilePath() {
		String srcPath = getSourcePath();
		String filepath = srcPath + "/" + srcTag + "_Remain.csv";
		return filepath;
	}
	
	public String getDoneFilePath() {
		String srcPath = getSourcePath();
		String filepath = srcPath + "/" + srcTag + "_Done.csv";
		return filepath;
	}
	
	public String getErrorFilePath() {
		String srcPath = getSourcePath();
		String filepath = srcPath + "/" + srcTag + "_Error.csv";
		return filepath;
	}
	
	public String getKRPath() {	
		String countryPath = getCountryPath();
		String krPath = countryPath +"/KR" ;
		return krPath;
	}
	
	public String getBSISCFPath() {	
		String countryPath = getCountryPath();
		String othersPath = countryPath +"/BSISCF";
		return othersPath;
	}
	
	public String getCountryTag() {
		return this.countryTag;
	}

	public String getSummaryFilePath() {
		String srcPath = getSourcePath();
		String filepath = srcPath + "/" + srcTag + "_SummaryData.csv";
		return filepath;
	}
	
	public String getCredFilePath() {
		String currentPath = getConfPath() ;	
		String filePath = currentPath + "/credentials.json";
		return filePath;
	}
}
