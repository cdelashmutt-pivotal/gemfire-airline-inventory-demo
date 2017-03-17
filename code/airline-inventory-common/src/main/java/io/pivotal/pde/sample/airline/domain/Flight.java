package io.pivotal.pde.sample.airline.domain;

import io.pivotal.pde.sample.airline.keys.FlightKey;


/**
 * 
 * @author wmay
 * 
 */
public class Flight   {
	
	private int flightNumber;
	private String departureDate;
	private String origin;
	private String destination;
	private  String arrivalTime;
	private String departureTime;
	private int capacity;
	private int seatsSold;
	private String equipmentCode;
	private String timeZone; // used internally for displaying dates
	
	public FlightKey getKey(){
		return new FlightKey(this.flightNumber, this.departureDate);
	}
	
	public int getFlightNumber() {
		return flightNumber;
	}
	public void setFlightNumber(int flightNumber) {
		this.flightNumber = flightNumber;
	}
	
	// departureDate is expected to be in yyyyymmdd format
	public String getDepartureDate() {
		return departureDate;
	}
	public void setDepartureDate(String date) {
		this.departureDate = date;
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
	public String getArrivalTime() {
		return arrivalTime;
	}
	public void setArrivalTime(String arrivalTime) {
		this.arrivalTime = arrivalTime;
	}
	public String getDepartureTime() {
		return departureTime;
	}
	public void setDepartureTime(String departureTime) {
		this.departureTime = departureTime;
	}
	public int getCapacity() {
		return capacity;
	}
	public void setCapacity(int capacity) {
		this.capacity = capacity;
	}
	public int getSeatsSold() {
		return seatsSold;
	}
	public void setSeatsSold(int seatsSold) {
		this.seatsSold = seatsSold;
	}
	
	public String getEquipmentCode() {
		return equipmentCode;
	}

	public void setEquipmentCode(String equipmentCode) {
		this.equipmentCode = equipmentCode;
	}
	
	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}


	@Override
	public String toString() {
		return "Flight [flightNumber=" + flightNumber + ", departureDate=" +departureDate 
				+ ", origin=" + origin + ", destination=" + destination +  ", departureTime=" + departureTime 
				+ ", arrivalTime=" + arrivalTime + ", capacity=" + capacity + ", seatsSold="
				+ seatsSold + ", equipmentCode=" + equipmentCode
				+ ", timeZone=" + timeZone + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((arrivalTime == null) ? 0 : arrivalTime.hashCode());
		result = prime * result + capacity;
		result = prime * result
				+ ((departureDate == null) ? 0 : departureDate.hashCode());
		result = prime * result
				+ ((departureTime == null) ? 0 : departureTime.hashCode());
		result = prime * result
				+ ((destination == null) ? 0 : destination.hashCode());
		result = prime * result
				+ ((equipmentCode == null) ? 0 : equipmentCode.hashCode());
		result = prime * result + flightNumber;
		result = prime * result + ((origin == null) ? 0 : origin.hashCode());
		result = prime * result + seatsSold;
		result = prime * result
				+ ((timeZone == null) ? 0 : timeZone.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Flight other = (Flight) obj;
		if (arrivalTime == null) {
			if (other.arrivalTime != null)
				return false;
		} else if (!arrivalTime.equals(other.arrivalTime))
			return false;
		if (capacity != other.capacity)
			return false;
		if (departureDate == null) {
			if (other.departureDate != null)
				return false;
		} else if (!departureDate.equals(other.departureDate))
			return false;
		if (departureTime == null) {
			if (other.departureTime != null)
				return false;
		} else if (!departureTime.equals(other.departureTime))
			return false;
		if (destination == null) {
			if (other.destination != null)
				return false;
		} else if (!destination.equals(other.destination))
			return false;
		if (equipmentCode == null) {
			if (other.equipmentCode != null)
				return false;
		} else if (!equipmentCode.equals(other.equipmentCode))
			return false;
		if (flightNumber != other.flightNumber)
			return false;
		if (origin == null) {
			if (other.origin != null)
				return false;
		} else if (!origin.equals(other.origin))
			return false;
		if (seatsSold != other.seatsSold)
			return false;
		if (timeZone == null) {
			if (other.timeZone != null)
				return false;
		} else if (!timeZone.equals(other.timeZone))
			return false;
		return true;
	}

	

}
