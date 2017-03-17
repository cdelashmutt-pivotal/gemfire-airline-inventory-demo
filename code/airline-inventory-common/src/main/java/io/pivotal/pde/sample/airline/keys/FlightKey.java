package io.pivotal.pde.sample.airline.keys;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.geode.DataSerializable;
import org.apache.geode.DataSerializer;
import org.apache.geode.cache.EntryOperation;
import org.apache.geode.cache.PartitionResolver;

public class FlightKey implements PartitionResolver, DataSerializable {

	private int flightNumber;
	private String flightDate;
	
	public FlightKey(){
		// required for DataSerializable
	}
	
	public FlightKey(int flightNumber, String flightDate) {
		this.flightNumber = flightNumber;
		this.flightDate = flightDate;
	}

	@Override
	public void close() {
	}

	@Override
	public String getName() {
		return "FlightKey";
	}

	@Override
	public Object getRoutingObject(EntryOperation op) {
		return flightNumber;
	}

	public int getFlightNumber() {
		return flightNumber;
	}

	public void setFlightNumber(int flightNumber) {
		this.flightNumber = flightNumber;
	}

	public String getFlightDate() {
		return flightDate;
	}

	public void setFlightDate(String flightDate) {
		this.flightDate = flightDate;
	}
	

	@Override
	public void fromData(DataInput in) throws IOException, ClassNotFoundException {
		flightNumber = DataSerializer.readInteger(in);
		flightDate = DataSerializer.readString(in);
	}

	@Override
	public void toData(DataOutput out) throws IOException {
		DataSerializer.writeInteger(flightNumber, out);
		DataSerializer.writeString(flightDate, out);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((flightDate == null) ? 0 : flightDate.hashCode());
		result = prime * result + flightNumber;
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
		FlightKey other = (FlightKey) obj;
		if (flightDate == null) {
			if (other.flightDate != null)
				return false;
		} else if (!flightDate.equals(other.flightDate))
			return false;
		if (flightNumber != other.flightNumber)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FlightKey [flightNumber=" + flightNumber + ", flightDate=" + flightDate + "]";
	}


}
