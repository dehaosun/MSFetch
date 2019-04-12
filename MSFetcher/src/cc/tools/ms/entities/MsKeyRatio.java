package cc.tools.ms.entities;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.tool.ms.MainHandler;



public class MsKeyRatio {
	
	final static String _TAG_STNAME = "Stock_Name";
	final static String _TAG_CMPNAME = "Company_Name";
	final static String _TAG_CURRENCY = "Currency";
	final static String _TAG_EPS_LAST = "EPS_Last";
	final static String _TAG_EPS_TTM = "EPS_TTM";
	final static String _TAG_EPS_G_3Y = "EPS_G_3Y";
	final static String _TAG_EPS_G_5Y = "EPS_G_5Y";
	final static String _TAG_EPS_G_10Y = "EPS_G_10Y";
	final static String _TAG_DIV_LAST = "Div_Last";
	final static String _TAG_DIV_TTM = "Div_TTM";
	final static String _TAG_BV_LAST = "BK_Last";
	final static String _TAG_BV_TTM = "BK_TTM";
	final static String _STR_EMPTY = "";
	final static String _STR_DASH = "-";
	
	
	private Logger log = LoggerFactory.getLogger(this.getClass().getSimpleName());
	private String stName;
	private String filePath;


	
	public MsKeyRatio(String saveFilePath, String st) {
		stName = st;
		filePath = saveFilePath;
	}
	
	public boolean recordToSummaryQueue() {

		
		
		//get CSV recorder
		Iterable<CSVRecord> csv = null ;
		try {
			
			Reader reader =  new FileReader(filePath);
			
			CSVFormat fmt = CSVFormat.DEFAULT
					.withIgnoreEmptyLines(false)
					.withTrim()
					;
			csv = fmt.parse(reader);
		} catch (IOException e) {
			log.error("{} : Convert Csv File failed:{}",stName,e);
		}
		
		if(csv == null) {
			log.error("{} : Null Csv File!",stName);
			return false;
		}
		
		
		
		//data
		LinkedHashMap<String, List<String>> dataSet = new LinkedHashMap<String, List<String>>();
		
		List<CSVRecord> csvArray = new ArrayList<CSVRecord>();
		
		for (CSVRecord record : csv) {
			//if(record == null)continue;
			csvArray.add(record);
		} 
		if( csvArray.size() <= 10) {
			log.error("{} : invalid csv file, row <= 10!",stName);
			return false;
		}
		
		String tmpStr;
		
		
		// First Stock Name
		dataSet.put(_TAG_STNAME, Arrays.asList(stName));
		
		//CompanyName
		tmpStr = getValidCSVValue(csvArray,0,0,_STR_DASH);
		tmpStr = (tmpStr.length()>10)?tmpStr.substring(47):tmpStr;
		dataSet.put(_TAG_CMPNAME, Arrays.asList(tmpStr));
		
		//Currency
		tmpStr = getValidCSVValue(csvArray,3,0,_STR_DASH);
		tmpStr = (tmpStr.length()>10)?tmpStr.substring(8, 11):tmpStr;
		dataSet.put(_TAG_CURRENCY, Arrays.asList(tmpStr));
		
		//EPS_Last, EPS_TTM, row = 8
		tmpStr = getValidCSVValue(csvArray,8,10,_STR_DASH);
		dataSet.put(_TAG_EPS_LAST, Arrays.asList(tmpStr));
		
		tmpStr = getValidCSVValue(csvArray,8,11,_STR_DASH);
		dataSet.put(_TAG_EPS_TTM, Arrays.asList(tmpStr));
		
		//EPS_G_3Y, EPS_G_5Y_EPS_G_10Y , row =60,61,62, 
		tmpStr = getValidCSVValue(csvArray,60,10,_STR_DASH);
		dataSet.put(_TAG_EPS_G_3Y, Arrays.asList(tmpStr));
		tmpStr = getValidCSVValue(csvArray,61,10,_STR_DASH);
		dataSet.put(_TAG_EPS_G_5Y, Arrays.asList(tmpStr));
		tmpStr = getValidCSVValue(csvArray,62,10,_STR_DASH);
		dataSet.put(_TAG_EPS_G_10Y, Arrays.asList(tmpStr));
		
		
		//Div_Last, Div_TTM, ROW = 9
		tmpStr = getValidCSVValue(csvArray,9,10,_STR_DASH);
		dataSet.put(_TAG_DIV_LAST, Arrays.asList(tmpStr));
		
		tmpStr = getValidCSVValue(csvArray,9,11,_STR_DASH);
		dataSet.put(_TAG_DIV_TTM, Arrays.asList(tmpStr));
		
		
		//BVPS_Last, BVPS_Last; ROW =12
		tmpStr = getValidCSVValue(csvArray,12,10,_STR_DASH);
		dataSet.put(_TAG_BV_LAST, Arrays.asList(tmpStr));
		
		tmpStr = getValidCSVValue(csvArray,12,11,_STR_DASH);
		dataSet.put(_TAG_BV_TTM, Arrays.asList(tmpStr));
		
		List<String> tmpDataSet;
		String tmpTitle;
		int tmpRow;
		int tmpDataCnt;
		String tmpData;
		//loop all other
		for(krMetaData krd : krMetaData.values()) {
			//build title
			tmpTitle = krd.getTag();
			//build data
			tmpRow = krd.getRowN();
			tmpDataCnt = krd.getDataCnt();
			tmpDataSet = new ArrayList<String>();
			if(tmpDataCnt == 2) {
				tmpData = getValidCSVValue(csvArray,tmpRow,10,_STR_DASH);
				tmpDataSet.add(tmpData);
				tmpData = getValidCSVValue(csvArray,tmpRow,11,_STR_DASH);
				tmpDataSet.add(tmpData);
			}else {
				for(int i = 0; i< tmpDataCnt; i++){
					tmpData = getValidCSVValue(csvArray,tmpRow,i+1,_STR_DASH);
					tmpDataSet.add(tmpData);
				}
				
			}			
			dataSet.put(tmpTitle,tmpDataSet);
			
		}
		
		
		MainHandler.dataQueue.add(dataSet);
		
		return true;
	}
	
	private String getValidCSVValue(ArrayList<CSVRecord> csvArray, int row, int csvCol) {
		return getValidCSVValue(csvArray,row,csvCol,_STR_EMPTY);
	}
	
	
	private String getValidCSVValue(List<CSVRecord> csvArray, int row, int csvCol,String errorValue) {
		String rtn = _STR_EMPTY;
		CSVRecord record = csvArray.get(row);
		if( record == null)return errorValue;
		try{
			//System.out.println(record.size());
			if(record.size()>csvCol)rtn = record.get(csvCol);
			if(rtn == null)return _STR_EMPTY;
			rtn = rtn.trim();
		}catch( Exception e) {
			log.error("Get CSV Value with error:{}",e);
		}
		return rtn;
	}
	
	
	enum krMetaData{
		// weight, tpcd
		REV("REV.", 3, 11),
		OPI("OP.I.", 5, 11),
		NI("N.I.", 7, 11),
		CFC("FCF", 15, 11),
		SHR("Shares", 11, 11),
		PS_EPS("PS_EPS", 8, 11),
		PS_FCFPS("PS_FCFPS", 16, 11),
		PS_DIVPS("PS_DIVPS", 9, 11),
		PS_BVPS("PS_BVPS", 12, 11),
		PERCENT_PAYOUT("%PayOut.R.", 10, 11),
		PERCENT_GM("%G.M.", 4, 11),
		PERCENT_OPM("%OP.M.", 6, 11),
		MOS_SGA("MOS%_SGA", 24, 11),
		MOS_RD("MOS%_RD", 25, 11),
		MOS_OTH("MOS%_OTH", 26, 11),
		MOS_NI_OTH("MOS%_N.I._OTH", 28, 11),
		MOS_EBTM("MOS%_EBT.M", 29, 11),
		PERCENT_TAX("%Tax", 32, 11),
		PERCENT_NM("%N.M.", 33, 11),
		PERCENT_ROA("%ROA", 35, 11),
		R_FLVG_AVG("R._FLvg(Avg)", 36, 11),
		PERCENT_ROE("%ROE", 37, 11),
		PERCENT_ROIC("%ROIC", 38, 11),
		R_IC("R._IC", 39, 11),
		R_CR("R._C.R.", 96, 11),
		R_DE("R._D/E", 99, 11),
		PERCENT_CASH_EQU("%CASH_EQU", 74, 11),
		PERCENT_T_CUR_LIAB("%T_CUR_LIAB", 88, 11),
		G_REV("G%_REV", 44, 2),
		G_REV_3Y("G%_REV_3Y", 45, 2),
		G_REV_5Y("G%_REV_5Y", 46, 2),
		G_REV_10Y("G%_REV_10Y", 47, 2),
		G_OP("G%_OP", 49, 2),
		G_OP_3Y("G%_OP_3Y", 50, 2),
		G_OP_5Y("G%_OP_5Y", 51, 2),
		G_OP_10Y("G%_OP_10Y", 52, 2),
		G_NI("G%_NI", 54, 2),
		G_NI_3Y("G%_NI_3Y", 55, 2),
		G_NI_5Y("G%_NI_5Y", 56, 2),
		G_NI_10Y("G%_NI_10Y", 57, 2),
		G_EPS("G%_EPS", 59, 2),
		G_EPS_3Y("G%_EPS_3Y", 60, 2),
		G_EPS_5Y("G%_EPS_5Y", 61, 2),
		G_EPS_10Y("G%_EPS_10Y", 62, 2)
		;
		
		
		private String tagS;
		private int rowN;
		private int dataCnt;
		//
		krMetaData(final String tagS, final int rowN, final int dataCnt){
			this.tagS=tagS;
			this.rowN=rowN;
			this.dataCnt=dataCnt;
		}
		
		public String getTag(){
			return tagS;
		}
		
		public int getRowN(){
			return rowN;
		}
	    
		public int getDataCnt(){
			return dataCnt;
		}
		
	}
}
