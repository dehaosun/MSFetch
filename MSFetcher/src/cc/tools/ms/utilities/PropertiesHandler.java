package cc.tools.ms.utilities;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.json.java.JSON;
import com.ibm.json.java.JSONObject;

public class PropertiesHandler {
	
	private static Logger log = LoggerFactory.getLogger(PropertiesHandler.class.getName());			
	private Properties sysProp=new Properties();
	private Properties mainProp=new Properties();
	private ConcurrentHashMap<String,String> jsonMap;
	private static PropertiesHandler _InstanceHandler;
	String sysPath;
	String jsonPath;
	
	public synchronized static PropertiesHandler getPropsHandler(){
		if(_InstanceHandler==null)_InstanceHandler= new PropertiesHandler();
		return _InstanceHandler;
		
	}
	
	
	public PropertiesHandler(){
		setProps();
	}
	
	
	public Properties getCurrentMainProp(){
		
		Properties currenrProps=new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream(sysPath);
			currenrProps.load(input);
		} catch (FileNotFoundException e) {
			log.error("{}",e);
		} catch (IOException e) {
			log.error("{}",e);
		}
	
		return currenrProps;
	}
	
	private void setProps(){
		
		
		InputStream input = null;

		
		
		try {
			sysProp.load(ClassLoader.getSystemResourceAsStream("setting.properties"));

			sysPath = sysProp.getProperty("setting.main.props.path");
			jsonPath=sysProp.getProperty("setting.main.json.path");
			input = new FileInputStream(sysPath);
			mainProp.load(input);
			
			input = new FileInputStream(jsonPath);
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
		} catch (IOException e) {
			log.error(e.getMessage());
		}
		
		JSONObject jsonProps = null;
		try {
			jsonProps = (JSONObject)JSON.parse(input);
		} catch (NullPointerException e) {
			log.error("Get JSON File Nullpoint error:",e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Get JSON File IOException error:",e.getMessage());
			e.printStackTrace();
		}
			
		//handel sqlMap
		if(jsonProps==null || jsonProps.size()==0){
			log.error("Read Json faild..");
		}else{
			jsonMap=new ConcurrentHashMap<String,String>();
			for(Object key : jsonProps.keySet()){
				Object value=jsonProps.get(key);
				if(value!=null){
					jsonMap.put((String)key, (String)value);
				}else{
					log.error("Load Json failed for key:"+(String)key);
				}
			}
		}
	}
	
	public Properties getMainProps(){
		if(mainProp==null || mainProp.isEmpty())setProps();
		return mainProp;
		
	}
	
	public ConcurrentHashMap<String, String> getJSONMap(){
		if(jsonMap==null || jsonMap.isEmpty())setProps();
		return jsonMap;
		
	}
	
	
	
}
