package io.pivotal.pde.sample.airline.domain;

import java.math.BigDecimal;

/**
 * @author wmay
 *
 */
public class RevenueControl {
	
	private int flightNumber;
	
	private BigDecimal zeroSeatPrice;
	private BigDecimal slope;
	
	public Integer getKey(){
		return Integer.valueOf(flightNumber);
	}
	
	public int getFlightNumber() {
		return flightNumber;
	}
	public void setFlightNumber(int flightNumber) {
		this.flightNumber = flightNumber;
	}
	public BigDecimal getZeroSeatPrice() {
		return zeroSeatPrice;
	}
	public void setZeroSeatPrice(BigDecimal zeroSeatPrice) {
		this.zeroSeatPrice = zeroSeatPrice;
	}
	public BigDecimal getSlope() {
		return slope;
	}
	public void setSlope(BigDecimal slope) {
		this.slope = slope;
	}
	
	@Override
	public String toString() {
		return "RevenueControl [flightNumber=" + flightNumber
				+ ", zeroSeatPrice=" + zeroSeatPrice + ", slope=" + slope + "]";
	}
	
	
	
}
