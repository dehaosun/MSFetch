package cc.tool.ms;


import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.tools.ms.entities.StockInfo;
import cc.tools.ms.utilities.PropertiesHandler;

public class StockHandler implements Runnable {
	
	private Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());
	
	@Override
	public void run() {
		startDownload();
	}
	
	
	private ConcurrentLinkedQueue <StockInfo> stockQueue  = null;
	private int threadNumberMax = 10; 
	private int threadSingleHandler = 2; 
	
	private boolean includingOthers = false;
	private boolean forceStop = false;
	private boolean finished = false;
	private boolean started = false;
	private boolean testMode = false;
	
	ArrayList<Thread> downThreads = new ArrayList<Thread>();
	ArrayList<StockDownloder> downloaders = new ArrayList <StockDownloder>();
	

	public StockHandler(boolean includingBSISCF) {
		
		try {threadNumberMax = Integer.valueOf(PropertiesHandler.getPropsHandler().getMainProps().getProperty("stockdownload.thread.max"));}catch(Exception e){}finally {}
		try {threadSingleHandler = Integer.valueOf(PropertiesHandler.getPropsHandler().getMainProps().getProperty("stockdownload.single.handle.min.count"));}catch(Exception e){}finally {}

		stockQueue = MainHandler.stockQueue ;

		includingOthers = includingBSISCF;
	}

	
	public void startDownload() {
		started = true;
		
		int finalThreadNum = 1;
		int queueCnt = stockQueue.size();
		
		if(threadSingleHandler<=0)threadSingleHandler=1;
		int threadCnt = queueCnt/threadSingleHandler;
		
		//calculate final thread number
		if(threadCnt >= threadNumberMax) {
			finalThreadNum = threadNumberMax;
		}else {
			finalThreadNum = (threadCnt > 0)? threadCnt : 1;
		}
		log.info("Start Download with {} thread(s)",finalThreadNum);
		
		downThreads = new ArrayList<Thread>();
		downloaders = new ArrayList <StockDownloder>();
		
		for(int i = 0 ; i<finalThreadNum; i++ ) {
			StockDownloder downloader = new StockDownloder(includingOthers);
			Thread downThread = new Thread(downloader);
			downloaders.add(downloader);
			downThreads.add(downThread);
			
			if(testMode) {
				downloader.run();
			}else {		
				downThread.start();
			}
		}
		
		
		//Monitor
		boolean keepMonitor = true;
		boolean readyForStop = false;
		
		while( keepMonitor) {
			
			//check forceStop
			if(forceStop && !readyForStop) {
				//Force
				
				notifyStopDownloaders();
				waitAndGo(3000);
				readyForStop = true;
				
			}
			
			//normally check Thread status
			boolean allThreadDone = checkThreadAllDone();
			if(allThreadDone) {
				readyForStop = true;
			}
			
					
			//check wait or post action
			if(!readyForStop) {
				waitAndGo(200);	
			}else {
				
				tryStopThreads();			
				keepMonitor = false;
			}
			
		}

		finished = true;
		log.info("Stop Download with {} thread(s)",finalThreadNum);
	}



	private boolean checkThreadAllDone() {
		boolean rtn = false;
		int threadDoneCnt = getThreadDoneCount();
		if(threadDoneCnt >= downThreads.size()) {
			rtn =true;
		}
		return rtn;
	}


	public int getThreadDoneCount() {
		int cnt = 0;
		for(int i=0; i<downThreads.size();  i++) {
			Thread t = downThreads.get(i);
			StockDownloder d = downloaders.get(i);
			if(!t.isAlive() || d.finished )cnt++;
		}
		return cnt;
	}

	public int getThreadAliveCount() {
		int cnt = 0;
		for(int i=0; i<downThreads.size();  i++) {
			Thread t = downThreads.get(i);
			StockDownloder d = downloaders.get(i);
			if(t.isAlive() || !d.finished)cnt++;
		}
		return cnt;
	}
	
	public int getThreadCount() {
		return downThreads.size();
	}
	

	public int tryStopThreads() {
		
		int cntDone = 0;
		int cntIntpt = 0;
		
		for(int i=0; i<downThreads.size();  i++) {
			Thread t = downThreads.get(i);
			if(t.isAlive()) {
				try{
					t.interrupt();
					log.info("Interrupt thread #{}.",i);
				}finally {
					cntIntpt++;
				}
			}else {
				cntDone++;
			}
		}
		log.info("Done stopping threads, Stoped/Done/All = {}/{}/{}.", cntIntpt,cntDone,downThreads.size() );
		return cntIntpt;
	}


	private void notifyStopDownloaders() {
		
		for(int i = 0 ; i< downloaders.size() ; i++) {
			StockDownloder downloader = downloaders.get(i);
			downloader.setForceStop();
		}
		
	}



	private void waitAndGo(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			log.error("Get Sleep Error while montoring download threads:{}",e);
		}
	}
	
	
	public boolean isStart() {
		return this.started;
	}
	
	public boolean isfinished() {
		return this.finished;
	}


	public void setForcelyStop(boolean b) {
		this.forceStop = b;	
		if(b==true) {
			notifyStopDownloaders();
			waitAndGo(1000);
		}
	}
	
	public void setTestMode(boolean b) {
		this.testMode = b;
	}
}
