package io.pivotal.pde.sample.airline.domain;

import java.math.BigDecimal;

import io.pivotal.pde.sample.airline.keys.FlightAvailabilityKey;

public class FlightAvailability {
	
	private int flightNumber;
	private String  flightDate;
	private String origin;
	private String destination;
	private String fareClass;
	private BigDecimal fare;
	private int availableSeats;
	
	
	public FlightAvailabilityKey getKey(){
		return new FlightAvailabilityKey(this.flightNumber, this.flightDate, this.fareClass);
	}
	
	public int getFlightNumber() {
		return flightNumber;
	}
	public void setFlightNumber(int flightNumber) {
		this.flightNumber = flightNumber;
	}
	public String getDate() {
		return flightDate;
	}
	public void setDate(String date) {
		this.flightDate = date;
	}
	public String getOrigin() {
		return origin;
	}
	public void setOrigin(String origin) {
		this.origin = origin;
	}
	public String getDestination() {
		return destination;
	}
	public void setDestination(String destination) {
		this.destination = destination;
	}
	public String getFareClass() {
		return fareClass;
	}
	public void setFareClass(String fareClass) {
		this.fareClass = fareClass;
	}
	public BigDecimal getFare() {
		return fare;
	}
	public void setFare(BigDecimal fare) {
		this.fare = fare;
	}
	public int getAvailableSeats() {
		return availableSeats;
	}
	public void setAvailableSeats(int availableSeats) {
		this.availableSeats = availableSeats;
	}
	
	//TODO format the dates in here 
	@Override
	public String toString() {
		return "FlightAvailability [flightNumber=" + flightNumber + ", flightDate="
				+ flightDate + ", origin=" + origin + ", destination=" + destination
				+ ", fareClass=" + fareClass + ", fare=" + fare
				+ ", availableSeats=" + availableSeats + "]";
	}
	
	
	
}
