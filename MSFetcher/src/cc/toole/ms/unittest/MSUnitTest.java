package cc.toole.ms.unittest;

import cc.tool.ms.MainHandler;

public class MSUnitTest {

	public static void main(String[] args) {
		
		args = new String[4];
		args[0] = "US";
		args[1] = "US_ALL";
		args[2] = "true";
		args[3] = "false";
		
		MainHandler.main(args);

	}

}
