package io.pivotal.pde.sample.airline.domain;

public class Airport {
	private String code;
	private String timeZone;
	
	public String getKey(){ return this.code;}
	
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getTimeZone() {
		return timeZone;
	}
	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}
	
	@Override
	public String toString() {
		return "Airport [code=" + code + ", timeZone=" + timeZone + "]";
	}
	
	
}
