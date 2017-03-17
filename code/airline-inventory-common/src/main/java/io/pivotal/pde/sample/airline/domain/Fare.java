package io.pivotal.pde.sample.airline.domain;

import java.math.BigDecimal;

public class Fare {
	
	private int id;
	private String origin;
	private String destination;
	private String fareClass;
	private String effectiveStartDate;
	private String effectiveEndDate;
	private String startingTravelDate;
	private String endingTravelDate;
	private BigDecimal fare;

	private String timeZone;

	public Integer getKey() {
		return Integer.valueOf(id);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
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

	/*
	 * note - effective dates apply to the booking date - not the travel date
	 */
	public String getEffectiveStartDate() {
		return effectiveStartDate;
	}

	public void setEffectiveStartDate(String effectiveStartDate) {
		this.effectiveStartDate = effectiveStartDate;
	}

	public String getEffectiveEndDate() {
		return effectiveEndDate;
	}

	public void setEffectiveEndDate(String effectiveEndDate) {
		this.effectiveEndDate = effectiveEndDate;
	}

	public BigDecimal getFare() {
		return fare;
	}

	public void setFare(BigDecimal fare) {
		this.fare = fare;
	}

	public String getStartingTravelDate() {
		return startingTravelDate;
	}

	public void setStartingTravelDate(String startingTravelDate) {
		this.startingTravelDate = startingTravelDate;
	}

	public String getEndingTravelDate() {
		return endingTravelDate;
	}

	public void setEndingTravelDate(String endingTravelDate) {
		this.endingTravelDate = endingTravelDate;
	}

	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	@Override
	public String toString() {
		return "Fare [origin=" + origin + ", destination=" + destination
				+ ", fareClass=" + fareClass + ", startingTravelDate="
				+ startingTravelDate + ", endingTravelDate="
				+ endingTravelDate + ", timeZone=" + timeZone
				+ ", fare=" + fare + "]";
	}


}
