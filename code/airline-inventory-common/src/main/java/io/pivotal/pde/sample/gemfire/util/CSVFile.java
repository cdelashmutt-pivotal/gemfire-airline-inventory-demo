package io.pivotal.pde.sample.gemfire.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Usage:
 * 
 * CSVFile csvFile = new File(aFile);
 * 
 * try {
 * 	while(csvFile.next()){
 *    f1 = csvFile.getField(0);
 *    ...
 *  }
 * } finally {
 *   csvFile.close()
 * }
 * 
 * @author wmay
 *
 */

public class CSVFile {
	
	private static Logger log =LoggerFactory.getLogger(CSVFile.class);
	
	private BufferedReader reader;
	private String nextLine = null;
	private String []fields;
	
	public CSVFile(File file){
		if (!file.exists()) error("file does not exist: " + file.getPath());
		if (!file.canRead()) error("file cannot be read: " + file.getPath());
		
		try {
			reader =  new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			nextLine();
		} catch(FileNotFoundException nfx){
			error("file not found: " + file.getPath());
		} 
		
	}
	
	public CSVFile(InputStream stream){
		reader =  new BufferedReader(new InputStreamReader(stream));
		nextLine();
	}
	
	public boolean next(){
		if (nextLine == null) return false;
		
		fields = nextLine.split(",");
		
		nextLine();
		return true;
	}
	
	public String getField(int i){
		return fields[i];
	}
	
	public void close(){
		try {
			reader.close();
		} catch (IOException e) {
			error("error closing CSVFile", e);
		}
	}
	
	private void nextLine() {
		try {
			nextLine = reader.readLine();
			while ( (nextLine != null) && (nextLine.trim().length() == 0) ) nextLine = reader.readLine();
		} catch(IOException iox){
			error("error reading next line in CSVFile", iox);
		}
	}
	
	private static void error(String msg){
		log.error(msg);
		throw new RuntimeException(msg);
	}
	
	private static void error(String msg, IOException iox){
		log.error(msg,iox);
		throw new RuntimeException(msg,iox);
	}
	
	
}
