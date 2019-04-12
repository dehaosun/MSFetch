package cc.tools.ms.utilities;

import java.io.File;
import java.net.URISyntaxException;

public class testRun {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	
		
		File f;
		try {
			f = new File(testRun.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			f.getParent();
			System.out.println(f.getPath());
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			
			e.printStackTrace();
		}
		
		
	}

}
