package io.pivotal.pde.sample.airline.gemfire.security;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.security.Principal;
import java.util.HashMap;
import java.util.Properties;

import org.apache.geode.LogWriter;
import org.apache.geode.distributed.DistributedMember;
import org.apache.geode.security.AuthenticationFailedException;
import org.apache.geode.security.Authenticator;

/**
 * reads security configuration from a file with the following format
 * 
 * username password role
 * 
 * where role is  look, book or system
 * 
 * specify the name of the file using the security.authfile property
 * 
 * caches the information in the file for 1 minute
 * 
 * @author wmay
 *
 */
public class SimpleFileAuthenticator implements Authenticator {
	
	private static String USERNAME_CREDENTIAL = "username";
	private static String PASSWORD_CREDENTIAL = "password";
	private static String AUTHFILE_PROPERTY = "security-authfile";
	
	private static long MAX_USERINFO_AGE_SECONDS = 20 * 1000;  // cache user info for 20s
	
	private LogWriter secLogger, logger;
	
	File authFile;
	HashMap<String,SimpleFilePrincipal> userInfo;
	private long readTime = 0l;

	public static Authenticator create(){ return new SimpleFileAuthenticator();}
	
	@Override
	public void close() {
	}

	@Override
	public Principal authenticate(Properties props, DistributedMember distrMember)
			throws AuthenticationFailedException {
		String passedUsername = props.getProperty(USERNAME_CREDENTIAL);
		String passedPassword = props.getProperty(PASSWORD_CREDENTIAL);
		
		if (passedUsername == null) autherror("missing required credential: " + USERNAME_CREDENTIAL);
		if (passedPassword == null) autherror("missing required credential: " + PASSWORD_CREDENTIAL);
		
		SimpleFilePrincipal p  = this.getUserInfo(passedUsername);
		
		if (p == null) {
			secLogger.error("authentication failed - unknown user passed in credentials: " + passedUsername);
			throw new AuthenticationFailedException("invalid user name or password");
		}
		
		if (!p.authenticate(passedPassword)) {
			secLogger.error("authentication failed - password did not match - user: " + passedUsername);
			throw new AuthenticationFailedException("invalid user name or password");
		}
		
		secLogger.info("authenticated " + passedUsername);
		return p;
	}

	@Override
	public void init(Properties props, LogWriter sysLogger, LogWriter secLogger)
			throws AuthenticationFailedException {
		this.logger = sysLogger;
		this.secLogger = secLogger;
		
		String filename = props.getProperty(AUTHFILE_PROPERTY);
		if (filename == null) error("required property missing: " + AUTHFILE_PROPERTY);
		
		authFile = new File(filename);
		if (!authFile.exists()) error("Authorization file does not exist: " + authFile.getAbsolutePath());
		if (!authFile.isFile()) error("File is not a regular file: " + authFile.getAbsolutePath());
		if (!authFile.canRead()) error("Cannot read authorization file: " + authFile.getAbsolutePath());
		
		userInfo = new HashMap<String,SimpleFilePrincipal>();
	}

	private synchronized SimpleFilePrincipal getUserInfo(String uname){
		long age = System.currentTimeMillis() - readTime;
		if (age < (1000 * MAX_USERINFO_AGE_SECONDS)) return userInfo.get(uname);
		
		userInfo.clear();
		
		try {
			String line = null;
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(authFile)));
			try {
				line = reader.readLine();
				String []words = null;
				SimpleFilePrincipal p = null;
				while(line != null){
					words = line.split("\\s+");
					if (words.length != 3) secLogger.error("error parsing security file: " + authFile.getAbsolutePath()); // THROWS EXCEPTION
					
					p = new SimpleFilePrincipal(words[0], words[1], words[2]);
					userInfo.put(p.getName(), p);
					
					line = reader.readLine();
				}
			} finally {
				reader.close();
			}
		} catch(Exception x){
			logger.error("exception accessing auth file: " + authFile.getAbsolutePath(), x);
			error(x.getMessage());
		}
			
		return userInfo.get(uname);
	}
	
	private  void error(String msg){
		secLogger.error(msg);
		throw new RuntimeException(msg);
	}

	private  void autherror(String msg){
		secLogger.error(msg);
		throw new AuthenticationFailedException(msg);
	}
	
	
	
}
