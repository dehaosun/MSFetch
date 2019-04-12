package cc.tools.ms.entities;

import cc.tool.ms.EnvironHandler;

public class StockInfo{
	
	public String region;
	public String stockName;
	public String msExCode;
	
	private static final String _MS_KR = "http://financials.morningstar.com/ajax/exportKR2CSV.html?t=%TICKER%";
	private static final String _MS_BS = "http://financials.morningstar.com/ajax/ReportProcess4CSV.html?&t=%TICKER%&region=%REGION%&culture=en-US&version=SAL&cur=&reportType=bs&period=12&dataType=A&order=asc&columnYear=10&curYearPart=1st5year&rounding=3&view=raw&r=634108&denominatorView=raw&number=3";
	private static final String _MS_IS = "http://financials.morningstar.com/ajax/ReportProcess4CSV.html?&t=%TICKER%&region=%REGION%&culture=en-US&version=SAL&cur=&reportType=is&period=12&dataType=A&order=asc&columnYear=10&curYearPart=1st5year&rounding=3&view=raw&r=634108&denominatorView=raw&number=3";
	private static final String _MS_CF = "http://financials.morningstar.com/ajax/ReportProcess4CSV.html?&t=%TICKER%&region=%REGION%&culture=en-US&version=SAL&cur=&reportType=cf&period=12&dataType=A&order=asc&columnYear=10&curYearPart=1st5year&rounding=3&view=raw&r=634108&denominatorView=raw&number=3";
	private static final String _EMPTY_STR = "";
	private static final String _FILENAME_KR_STR = "_KR.csv";
	private static final String _FILENAME_BS_STR  = "_BS.csv";
	private static final String _FILENAME_IS_STR  = "_IS.csv";
	private static final String _FILENAME_CF_STR  = "_CF.csv";
	
	public StockInfo(String stockNameIn, String msExCodeIn){
		region = EnvironHandler.getExistedEnvironHandler().getCountryTag();
		stockName = (stockNameIn != null)?stockNameIn.trim():_EMPTY_STR;
		msExCode = (msExCodeIn != null)?msExCodeIn.trim():_EMPTY_STR;
	}
	
	
	
	private String getURLForMS(String msURL) {
		String url = null;
		
		String finalStockName = null; 
		
	if(msExCode != null && msExCode.length()>0) {
		finalStockName = msExCode + ":" + stockName;
	}else {
		finalStockName = stockName;
	}
		
	url = msURL.replace("%TICKER%", finalStockName).replace("%REGION%", region);
		
		return url;
	}
	
	
	public String getCFUrl() {
		return getURLForMS(_MS_CF);
	}


	public String getBSUrl() {
		return getURLForMS(_MS_BS);
	}


	public String getISUrl(){
		return getURLForMS(_MS_IS);
	}


	public String getKRUrl() {
		return getURLForMS(_MS_KR);
	}

	public String getKRFileName() {
		return stockName + _FILENAME_KR_STR;
	}
	
	public String getBSFileName() {
		return stockName + _FILENAME_BS_STR;
	}
	public String getISFileName() {
		return stockName + _FILENAME_IS_STR;
	}
	public String getCFFileName() {
		return stockName + _FILENAME_CF_STR;
	}
}