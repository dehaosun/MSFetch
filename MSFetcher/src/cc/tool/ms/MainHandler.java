package cc.tool.ms;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.tools.ms.entities.StockInfo;
import cc.tools.ms.utilities.PropertiesHandler;

public class MainHandler {

	private static Logger logger = LoggerFactory.getLogger(MainHandler.class.getSimpleName());
	private Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());
	
	public static ConcurrentLinkedQueue <StockInfo> stockQueue = null;
	
	public static ConcurrentLinkedQueue <StockInfo> doneQueue = null;
	public static ConcurrentLinkedQueue <StockInfo> errorQueue = null;
	public static ConcurrentLinkedQueue<LinkedHashMap<String, List<String>>> dataQueue = null;
	
	private static String _EMPTY_STR = "";
	private static String _HEADDER_TICKER_STR = "TickerName";
	private static String _HEADDER_MSCODE_STR = "MSExchangeCode";
	
	private Properties mainProps = PropertiesHandler.getPropsHandler().getMainProps();
	private ConcurrentHashMap <String,String> jsonMap = PropertiesHandler.getPropsHandler().getJSONMap();
	private EnvironHandler envHandler = null;
	
	private String countryTag = "US";
	private String folderPath = null;
	private String srcTag = "US_ALL";

	private int stockQueueTotal = 0;
	private int totalTimeOut = 1800;
	private long startMilSecs;
	private long currentMilSecs;
	private Thread stHandlerT;
	StockHandler stHandler;

	private long currentDiffMilSec;
	private boolean singleThreadMode = true;
	private boolean includeBSISCF = false;
	private boolean multiThreadTestMode = false;
	private boolean uploadToGoogle = false;
	float doneRate;
	float remainSecs;
	private int currentRemainQueueCnt;
	private int currentDoneQueueCnt;
	private int montorSleepDur = 1000;

	
	public static void main(String[] args) {
		
		String countryTagIn;
		String srcTagIn;
		boolean uploadToGoogle = true;
		boolean singleThreadMode = false;

		
		
//		args = new String[4];
//		args[0] = "US";
//		args[1] = "US_ALL";
//		args[2] = "true";
//		args[3] = "false";
		
		
		

		//must input at least 2 args
		if(args == null || args.length<2) {
			logger.error("Input must have at least 2 args.");
			return;
		}else {
			countryTagIn = args[0].trim();
			srcTagIn = args[1].trim();
		}
		
		if(args.length >=3) {
			try{uploadToGoogle = Boolean.valueOf(args[2].trim());}finally{}
		}
		if(args.length >=4) {
			try{singleThreadMode = Boolean.valueOf(args[3].trim());}finally{}
		}
		
		//validation
		int validationfailed = 0;
		if(countryTagIn == null || countryTagIn.isEmpty()) {
			logger.error("Input args[0]=> countryTagIn is empty");
			validationfailed++;
		}
		if(srcTagIn == null || countryTagIn.isEmpty()) {
			logger.error("Input args[1]=> srcTagIn is empty");
			validationfailed++;
		}
		
		if(validationfailed>0) {
			return;
		}else {
			logger.info("##### Input rgs: countryTagIn/srcTagIn/uploadToGoogle/singleThreadMode = {}/{}/{}/{} #####",countryTagIn,srcTagIn,String.valueOf(uploadToGoogle),String.valueOf(singleThreadMode));
			MainHandler mh = new MainHandler(countryTagIn,srcTagIn,uploadToGoogle,singleThreadMode);
			mh.startMainProcess();
		}

	}
	
	
	
	public MainHandler(String countryTagIn, String srcTagIn, boolean uploadToGoogleIn, boolean singleThreadModeIn) {
		if(srcTagIn!=null)countryTag = countryTagIn;
		if(srcTagIn!=null)srcTag = srcTagIn;
		uploadToGoogle = uploadToGoogleIn;
		singleThreadMode = singleThreadModeIn;
		includeBSISCF = false;
		montorSleepDur = 1000;
	}
	
	public void startMainProcess() {
		
		log.info("##### Starting Stock(s) Info Download Process #####");
		
		
		startMilSecs = System.currentTimeMillis();
		
		//pre check
		if(tryInitializEnviron() == false) {
			log.error("Initialize Failed.");
			return;			
		}else if(tryBuildStockQueue() == false) {
			log.error("Build Stock List Failed.");
			return;
		}
		
		//initial variable
		try {montorSleepDur = Integer.valueOf(PropertiesHandler.getPropsHandler().getMainProps().getProperty("main.handler.monitor.sleep.duration"));}finally {montorSleepDur=(montorSleepDur<=1000)?1000:montorSleepDur;}
		try {totalTimeOut = Integer.valueOf(PropertiesHandler.getPropsHandler().getMainProps().getProperty("main.handler.timeout.sec"));}finally {}
		try {multiThreadTestMode = Boolean.valueOf(PropertiesHandler.getPropsHandler().getMainProps().getProperty("main.handler.multiThread.testmode"));}finally {}	
		try {includeBSISCF = Boolean.valueOf(PropertiesHandler.getPropsHandler().getMainProps().getProperty("main.handler.get.isbscf.csv"));}finally {}	
		
		if(singleThreadMode) {
			downloadBySingleThread();
		}else {
			downloadByMultiThread();
		}
			
		
		// save done/remain Queue for reference
		trySaveStockQueue();
		
		log.info("##### End Stock(s) Info Download Process , {}/{} Stock(s) => {}% Done Rate #####",currentDoneQueueCnt,stockQueueTotal, String.format("%.1f", doneRate));
	}

	private void downloadBySingleThread() {
		StockDownloder stDown = new StockDownloder(includeBSISCF);
		stDown.setReportLog(true);
		stDown.run();
		currentDoneQueueCnt = doneQueue.size();
		doneRate = (float) (100.0 * (float)currentDoneQueueCnt / (float)stockQueueTotal);
	}



	private void downloadByMultiThread() {
		
		//start
		stHandler = new StockHandler(includeBSISCF);
		stHandlerT = new Thread(stHandler);
		
		
		if(multiThreadTestMode) {
			stHandler.run();
		}else {
			stHandlerT.start();
		}
		
	
		while( !stHandler.isStart() && !multiThreadTestMode) {
			waitAndGo(100);
		}
		//monitor and log
		boolean keepMonitor = true;
		boolean isforcelyStop = false;
		int forcelyWaitCnt = 0;
		long cntLog =0;
		while( keepMonitor) {
			currentMilSecs = System.currentTimeMillis();
			currentDiffMilSec = currentMilSecs - startMilSecs;
			//log Info
			cntLog = logCurrentInfo(cntLog);
				
			//check if timeout or forcely Stop			
			if(!isforcelyStop) {
				if( currentDiffMilSec > totalTimeOut*1000) {
					log.info("Main Handler reach timeout : {} seconds.",totalTimeOut);
					isforcelyStop = true;
				}else {
					isforcelyStop = getForcelyStopStatus();
				}
				
				if(isforcelyStop) {
					log.info("Main Handler detect Forcely Stop.");
					stHandler.setForcelyStop(true);
					waitAndGo(1000);
				}
			}
			
			if(stHandler.isfinished() || !stHandlerT.isAlive()) {
				log.info("Main handler monitor finished normally.");
				keepMonitor = false;
			}else if(isforcelyStop && forcelyWaitCnt >=2){
				log.info("Main handler monitor stop by forcely stopped.");
				//safty kill thread
				tryStopThreadHandler();
				keepMonitor = false;
			}else if(isforcelyStop) {
				forcelyWaitCnt++;
			}
			
			if(keepMonitor)waitAndGo(montorSleepDur);
			
		}
		
	}



	private void trySaveStockQueue() {
		
		//remain queue only
		if(stockQueue.size() >0) {
			log.info("Saving not downloaded stock(s) list to csv...");
			String path = envHandler.getRemainFilePath();
			saveCSVFromQueue(stockQueue,path);
		}
		
//		if(doneQueue.size() >0) {
//			log.info("Saving sucessful downloaded stock(s) list to csv...");
//			String path = envHandler.getDoneFilePath();
//			saveCSVFromQueue(doneQueue,path);
//		}
		
		if(errorQueue.size() >0) {
			log.info("Saving failed downloaded stock(s) list to csv...");
			String path = envHandler.getErrorFilePath();
			saveCSVFromQueue(errorQueue,path);
		}
		
		if(dataQueue.size() >0) {
			log.info("Saving stock(s) summary data to csv...");
			String path = envHandler.getSummaryFilePath();
			List<List<Object>> dataAll = saveDataSummaryFromQueue(dataQueue,path);
			if(uploadToGoogle && dataAll!= null && dataAll.size() > 0) {
				log.info("Upload summary data to GoogleSheet...");
				GSHandler gs = new GSHandler(dataAll);	
				gs.upload();
			}
		}
		
		
	}

	
	
	



    private List<List<Object>> saveDataSummaryFromQueue(ConcurrentLinkedQueue<LinkedHashMap<String, List<String>>> queue,
			String filePath) {

		//Prepare Header with first data
    		List<List<Object>> dataAll = new ArrayList<List<Object>>();

    		ArrayList<Object> header =  new ArrayList<Object>();
    		
    		int cnt = 0;	
    		List<String> tmpdata;
    		List<Object> data;
    		LinkedHashMap<String, List<String>> dataSet = null;
    		
		while(queue.size()>0){
			dataSet = queue.poll();
			data = new ArrayList<Object>();
			for(String title : dataSet.keySet()) {
				tmpdata = dataSet.get(title);
				
				//heealder
				if(cnt == 0) {
					//if length = 1, no append surrfix
					//if length = 2, _Last, _TTM
					//if length =11, _1 ~_10, _TTM
					
					if(tmpdata.size() == 1) {
						header.add(title);
					}else if(tmpdata.size() == 2) {
						header.add(title+"_Last");
						header.add(title+"_TTM");
					}else if(tmpdata.size() == 11) {
						for(int k = 1; k<=10; k++) {
							header.add(title+"_"+k);
						}
						header.add(title+"_TTM");
					}else {
						logger.error("Error On dataSummary to tmpdata size not 1,2,11..!,title:{}, size:{}",title, tmpdata.size());
						continue;
					}
				}
				
				//data
				for(String value:tmpdata) {
					data.add(value);
				}
			}
			
			//add to final data
			if(cnt == 0) {
				dataAll.add(header);
			}
			dataAll.add(data);
			cnt++;
		}
    	
		
		//to csv
		CSVFormat fmt = CSVFormat.EXCEL
				.withFirstRecordAsHeader();
				;
		
		BufferedWriter writer;
		try {
			writer = Files.newBufferedWriter(Paths.get(filePath));
			CSVPrinter csvPrinter = new CSVPrinter(writer, fmt);
			for(List<Object>ls : dataAll) {
				csvPrinter.printRecord(ls);			
			}
            csvPrinter.flush();            
            csvPrinter.close();
		} catch (IOException e) {
			log.error("Svae Summary Queue to CSV with Error:{}",e);
		}
		
		return dataAll;
		
		
	}



	private void saveCSVFromQueue(ConcurrentLinkedQueue <StockInfo> queue, String filePath) {
    
    	CSVFormat fmt = CSVFormat.EXCEL
    				.withHeader(_HEADDER_TICKER_STR,_HEADDER_MSCODE_STR)
    				;

	    	BufferedWriter writer;
		try {
			writer = Files.newBufferedWriter(Paths.get(filePath));
			CSVPrinter csvPrinter = new CSVPrinter(writer, fmt);
			
			while(queue.size()>0){
				StockInfo stInfo = queue.poll();
				if(stInfo == null)continue;
				String tickerName = stInfo.stockName;
				String msExCode = stInfo.msExCode;
				csvPrinter.printRecord(	tickerName,	msExCode);
			}
            csvPrinter.flush();            
            csvPrinter.close();
		} catch (IOException e) {
			log.error("Svae Queue to CSV with Error:{}",e);
		}
    	
    	

    }

	
	


	private void tryStopThreadHandler() {
		stHandler.tryStopThreads();
	
		if(stHandlerT.isAlive()) {
			try {
				stHandlerT.interrupt();
			}finally {
				
			}
		}
	}



	private long logCurrentInfo(long cnt) {
		//check if finished
		int totalTCnt= stHandler.getThreadCount();
		int doneTCnt = stHandler.getThreadDoneCount();
		int aliveTCnt = stHandler.getThreadAliveCount();

		currentRemainQueueCnt = stockQueue.size();
		currentDoneQueueCnt = doneQueue.size();
		doneRate = (float) (100.0 * (float)currentDoneQueueCnt / (float)stockQueueTotal);
		
		if(currentDoneQueueCnt == 0) {
			remainSecs = totalTimeOut;
		}else {
			remainSecs = (float) (((float)(stockQueueTotal-currentDoneQueueCnt)/(float)currentDoneQueueCnt) * ( (float)currentDiffMilSec)/1000.0) ;
		}
		
		log.info("Download # : Total / Done / Remain = {} / {} / {}",stockQueueTotal,currentDoneQueueCnt,currentRemainQueueCnt);
		if(cnt%2 == 0)log.info("Process Status: DoneRatio / RemianSecs = {}% / {}",String.format("%.1f", doneRate),String.format("%.1f", remainSecs));
		if(cnt%10 == 0)log.info("Thread(s) Total / Done / Alive = {} / {} / {}",totalTCnt,doneTCnt,aliveTCnt);
				
		cnt++;
		return cnt;
	}



	private boolean getForcelyStopStatus() {
		
		boolean currentForcely = false;
		try{
			currentForcely = Boolean.valueOf(PropertiesHandler.getPropsHandler().getCurrentMainProp().getProperty("main.handler.forcely.stop"));
		}finally {
			
		}

		return currentForcely;
	}



	private void waitAndGo(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			log.error("Get Sleep Error while montoring : {}",e);
		}
	}


	private boolean tryInitializEnviron() {
		envHandler = EnvironHandler.getEnvironHandler(countryTag, srcTag, folderPath);
		boolean checkStatus = envHandler.checkEnvrion();
		
		return checkStatus;
	}



	private boolean tryBuildStockQueue() {
		
		boolean rtn = false;
		
		//read filefrom excel or csv, then putinto queue
		String srcFile = envHandler.getSourceFilePath();
		Reader in;
		
		
		
		
		try {
			in = new FileReader(srcFile);
			CSVFormat fmt = CSVFormat.EXCEL
					.withTrim()
					;
			Iterable<CSVRecord> records = fmt.parse(in);
			
			stockQueue = new ConcurrentLinkedQueue <StockInfo>();
			doneQueue = new ConcurrentLinkedQueue <StockInfo>();
			errorQueue = new ConcurrentLinkedQueue <StockInfo>();
			dataQueue = new ConcurrentLinkedQueue <LinkedHashMap<String,List<String>>>();
			HashMap<String,Integer> hIdx = new HashMap<String,Integer>();
			
			int cnt = 0;
			boolean headerChecked = false;
			String tickerName = null;
			String msExCode = null;
//			String sector = null;
//			String industry = null;
//			String lastSale = null;
			for (CSVRecord record : records) {
				
				if(headerChecked == false) {
					//handle column index
					String tmpStr;
					for(int k = 0; k < record.size();k++) {
						tmpStr = record.get(k);
						if(tmpStr != null && tmpStr.trim().equalsIgnoreCase(_HEADDER_TICKER_STR)) {
							hIdx.put(_HEADDER_TICKER_STR,new Integer(k));
						}else if(tmpStr != null && tmpStr.trim().equalsIgnoreCase(_HEADDER_MSCODE_STR)) {
							hIdx.put(_HEADDER_MSCODE_STR,new Integer(k));
						}
					}
					
					//finally check must had TickerName & MSCodeHeader
					if(!hIdx.containsKey(_HEADDER_TICKER_STR)) {
						log.error("Src file does not have header :{}!",_HEADDER_TICKER_STR);
						return false;
					}
					
					headerChecked = true;
					continue;
				}
				
				

				tickerName = null;
				msExCode = null;
				try {
					
				    tickerName = tryGetCSVRecordValue(record,_HEADDER_TICKER_STR,hIdx,_EMPTY_STR);
				    msExCode = tryGetCSVRecordValue(record,_HEADDER_MSCODE_STR,hIdx,_EMPTY_STR);
				    
				   
				}catch(Exception e) {
					log.error("Src file read error on record:{}",cnt,e);
				}
				
			   
				if(tickerName.length() == 0) {
					log.error("Src file read empty TickerName on record:{}",cnt);
				}else {
					StockInfo stInfo = new StockInfo(tickerName,msExCode);
				    stockQueue.add(stInfo);
				}
				

				cnt++;
			}
		} catch (Exception e) {
			log.error("Src file error:{}",e);
		} finally {
			if(stockQueue != null && (stockQueue.size() > 0) ) {
				stockQueueTotal = stockQueue.size();
				log.info("Get {} stock(s) in total from source File:{}",stockQueueTotal,srcFile);
				rtn=true;
			}else {
				log.error("No stock(s) queue from source.");
			}
		}

		return rtn;
	}



	private String tryGetCSVRecordValue(CSVRecord record, String headerKey, HashMap<String, Integer> headIndex,
			String errorStr) {
		
		if(!headIndex.containsKey(headerKey)) {
			return errorStr;
		}else {
			int i = headIndex.get(headerKey);
			if(i<record.size()) {
				return getValidString(record.get(i));
			}else {
				return errorStr;
			}
			
		}
		
	}



	private String getValidString(String s) {
		return (s == null)?_EMPTY_STR:s.trim();
	}
	
	
}
