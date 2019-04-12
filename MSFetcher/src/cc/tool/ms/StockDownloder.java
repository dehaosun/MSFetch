package cc.tool.ms;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.tools.ms.entities.MsKeyRatio;
import cc.tools.ms.entities.StockInfo;
import cc.tools.ms.utilities.PropertiesHandler;


public class StockDownloder implements Runnable{

	
	private Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());
	
	private static final int BUFFER_SIZE = 4096;
		 
//	private static final String _MS_KR = "http://financials.morningstar.com/ajax/exportKR2CSV.html?t=%TICKER%";
//	private static final String _MS_BS = "http://financials.morningstar.com/ajax/ReportProcess4CSV.html?&t=%TICKER%&region=%REGION%&culture=en-US&version=SAL&cur=&reportType=bs&period=12&dataType=A&order=asc&columnYear=10&curYearPart=1st5year&rounding=3&view=raw&r=634108&denominatorView=raw&number=3";
//	private static final String _MS_IS = "http://financials.morningstar.com/ajax/ReportProcess4CSV.html?&t=%TICKER%&region=%REGION%&culture=en-US&version=SAL&cur=&reportType=is&period=12&dataType=A&order=asc&columnYear=10&curYearPart=1st5year&rounding=3&view=raw&r=634108&denominatorView=raw&number=3";
//	private static final String _MS_CF = "http://financials.morningstar.com/ajax/ReportProcess4CSV.html?&t=%TICKER%&region=%REGION%&culture=en-US&version=SAL&cur=&reportType=cf&period=12&dataType=A&order=asc&columnYear=10&curYearPart=1st5year&rounding=3&view=raw&r=634108&denominatorView=raw&number=3";
//	 
 
	private boolean includeAllInfo = false;
	private ConcurrentLinkedQueue<StockInfo> stockQueue;
	private ConcurrentLinkedQueue<StockInfo> doneQueue;
	private ConcurrentLinkedQueue<StockInfo> errorQueue;
	
	boolean started = false;
	boolean finished = false;
	boolean forcelyStop = false;

	private boolean reportLog = false;

	private int tryMax;

	private int tryWaitMs;
	
	private String stockName;
	 
	@Override
	public void run() {
		stratDownload();	
	}
	
	public StockDownloder() {
		initilze(false);
	}
	
	
	public StockDownloder(boolean includeBSISCF) {
		initilze(includeBSISCF);
	}
	
	private void initilze(boolean includeBSISCF) {
		stockQueue = MainHandler.stockQueue;
		doneQueue = MainHandler.doneQueue;
		errorQueue = MainHandler.errorQueue;
		includeAllInfo = includeBSISCF;
	}
	
	
	
	public void stratDownload() {
		started = true;	

		String url;
		String folderPath;
		String fileName;
		
		int successCnt = 0;
		int successPass = includeAllInfo?4:1;
		try { tryMax = Integer.valueOf(PropertiesHandler.getPropsHandler().getMainProps().getProperty("stockdownload.try.max")); }catch(Exception e){}finally {tryMax = (tryMax >= 30)? 30:tryMax;}
		try { tryWaitMs = Integer.valueOf(PropertiesHandler.getPropsHandler().getMainProps().getProperty("stockdownload.try.wait.ms")); }catch(Exception e){}finally { tryWaitMs = (tryWaitMs<=200)? 200:tryWaitMs;}
		
		//		boolean readyForStop = false;
//		boolean downloadSuccess = false;
		while(stockQueue.size() > 0 && !forcelyStop) {
			StockInfo stInfo = stockQueue.poll();
			if(stInfo == null)break;
			stockName = stInfo.stockName;
			successCnt = 0;
			//key ratio
			url = stInfo.getKRUrl();
			folderPath = EnvironHandler.getExistedEnvironHandler().getKRPath();
			fileName = stInfo.getKRFileName();
			if(tryDownloadFileAndCalculateInsight(tryMax,url,folderPath,fileName,true)) {
				successCnt++;
			}else {
				log.debug("Stock {}	KR csv download not success.",stockName);
			}

			if(includeAllInfo && !forcelyStop) {
				folderPath = EnvironHandler.getExistedEnvironHandler().getBSISCFPath();
				//BS
				url = stInfo.getBSUrl();
				fileName = stInfo.getBSFileName();
				if(tryDownloadFile(tryMax,url,folderPath,fileName)) {
					successCnt++;
				}else {
					log.debug("Stock {}	BS csv download not success.",stockName);
				}
				//IS
				url = stInfo.getISUrl();
				fileName = stInfo.getISFileName();
				if(tryDownloadFile(tryMax,url,folderPath,fileName)) {
					successCnt++;
				}else {
					log.debug("Stock {}	IS csv download not success.",stockName);
				}
				//CF
				url = stInfo.getCFUrl();
				fileName = stInfo.getCFFileName();
				if(tryDownloadFile(tryMax,url,folderPath,fileName)) {
					successCnt++;
				}else {
					log.debug("Stock {}	CF csv download not success.",stockName);
				}
				
			}
			
			if(successCnt >= successPass) {
				doneQueue.add(stInfo);
				if(reportLog) {
					log.info("Stock {}		{}/{} csv(s) download, success.",stockName,successCnt,successPass);
				}else {
					log.debug("Stock {}		{}/{} csv(s) download, success.",stockName,successCnt,successPass);
				}
					
			}else {
				errorQueue.add(stInfo);
				log.error("Stock {}		only {}/{} csv(s) download, failed.",stockName,successCnt,successPass);
			}
			
			
		}
	
		finished = true;
	}
	
	
	private boolean tryDownloadFile(int tryCnt, String fileUrl, String folderPath, String inPutFileName) {
	
		
		return tryDownloadFileAndCalculateInsight(fileUrl,folderPath,inPutFileName,false);
	}
	
	

	private boolean tryDownloadFileAndCalculateInsight(int tryCnt, String fileUrl, String folderPath, String inPutFileName, boolean krMode) {
		int cnt = 0;
		boolean success = false;
	
		do {
			success = tryDownloadFileAndCalculateInsight(fileUrl,folderPath,inPutFileName,krMode);
			cnt++;
			try {
				Thread.sleep(tryWaitMs);
			} catch (InterruptedException e) {

			} 
		}while(!success && (cnt < tryCnt));
		
		
		return success;
	}
	 
	 
    private  boolean tryDownloadFileAndCalculateInsight(String fileUrl, String folderPath, String inPutFileName,boolean krMode){
	    	if(fileUrl == null) {
	    		log.error("Null Input httpUrl for {}",inPutFileName);
	    		return false;
	    	}
	    	if(folderPath == null) {
	    		log.error("Null Input folderPath for {}",inPutFileName);
	    		return false;
	    	}
	    	
	    	boolean isDownload = false;

	    	HttpURLConnection httpConn;
    	
        
        httpConn = null;
        try {
        		URL url = new URL(fileUrl);
        		httpConn = (HttpURLConnection) url.openConnection();
        		int responseCode = httpConn.getResponseCode();
        		String fileName = inPutFileName;
        		
            // always check HTTP response code first
            if (responseCode != HttpURLConnection.HTTP_OK) {
                log.warn(fileUrl.substring(fileUrl.lastIndexOf("/") + 1,fileUrl.length()) + " No file to download. Server replied HTTP code: " + responseCode);
                isDownload = false;
            }else if(httpConn.getContentLength() == 0) {
            		log.debug("{} get file failed http ContentLength = 0");
            		isDownload = false;
            }else {
                               
                String saveFilePath = folderPath + File.separator + fileName;
                
                //delete file if download OK
                File f = new File(saveFilePath);
                if(f.exists() && !f.isDirectory()) { 
                    f.delete();
                }
     
                // opens input stream from the HTTP connection
                InputStream inputStream = httpConn.getInputStream();
             
                
                 
                // opens an output stream to save into file
                FileOutputStream outputStream = new FileOutputStream(saveFilePath);
     
                int bytesRead = -1;
                byte[] buffer = new byte[BUFFER_SIZE];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
     
                outputStream.close();
                
                if(!krMode) {
                	 	inputStream.close();   
                     log.debug(fileName + " File downloaded");        
                }else {
                		MsKeyRatio kr = new MsKeyRatio(saveFilePath,stockName);
                		boolean insight = kr.recordToSummaryQueue();
                		inputStream.close();
                		if(!insight) {
                			log.error("{}, download OK, but recorder to data failed!",stockName);
                		}else {
                			 log.debug(fileName + " File downloaded and recorder to data queue.");  
                		}
                }
                
                isDownload = true;              
            }
        		
        } catch (Exception e) {
        		log.error("Downlpad {} with error:",inPutFileName,e);
		}finally{
        		if(httpConn != null ) httpConn.disconnect();
        }
         
        return isDownload;
        
    }


	

	public void setForceStop() {
		forcelyStop = true;		
	}

	public void setReportLog(boolean b) {
		reportLog  = b;
	}

	

	
	
}
