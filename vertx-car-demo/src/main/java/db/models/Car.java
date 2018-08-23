package db.models;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import io.vertx.core.json.JsonObject;

public class Car {
	
	private static final AtomicInteger COUNTER = new AtomicInteger();

	private final int id;
	private String registrationNumber;
	private String ownerName;
	private Date timeEntered;
	private double tax;
	
	public Car(String registrationNumber, String ownerName) {
		this.id = COUNTER.getAndIncrement();
		this.registrationNumber = registrationNumber;
		this.ownerName = ownerName;
		this.timeEntered = Calendar.getInstance().getTime();
		this.tax = 2.0;
	}
	
	public Car() {
		this.id = COUNTER.getAndIncrement();
	}

	public String getRegistrationNumber() {
		return registrationNumber;
	}

	public void setRegistrationNumber(String registrationNumber) {
		this.registrationNumber = registrationNumber;
	}

	public String getOwnerName() {
		return ownerName;
	}

	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	public Date getTimeEntered() {
		return timeEntered;
	}

	public void setTimeEntered(Date timeEntered) {
		this.timeEntered = timeEntered;
	}

	public double getTax() {
		Date now = new Date();
		double hours = Math.abs(now.getHours() - this.timeEntered.getHours());
		return tax * (hours == 0 ? 1 : hours);
	}

	public void setTax(double tax) {
		this.tax = tax;
	}

	public int getId() {
		return id;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ID: " + this.getId()).append(System.lineSeparator());
		sb.append("Reg. #: " + this.getRegistrationNumber()).append(System.lineSeparator());
		sb.append("Owner's name: " + this.getOwnerName()).append(System.lineSeparator());		
		sb.append("Date Entered: " + this.getTimeEntered()).append(System.lineSeparator());
		sb.append("Tax: " + this.getTax()).append(System.lineSeparator());
		return sb.toString();
	}
	
	public static void main(String[] args) {
		Car car = new Car("CB 1448 AK", "Ivan");
		System.out.println(car.toString());
		
		JsonObject json = JsonObject.mapFrom(car);
		System.out.println(json.encode());
		
		String jsonStr = "{\"id\":0,\"registrationNumber\":\"CB 1448 AK\",\"ownerName\":\"Ivan\",\"timeEntered\":1534844299236,\"tax\":2.0}";
		Car car2 = new JsonObject(json.encode()).mapTo(Car.class);
		System.out.println(car2.toString());
		/*Date date = Calendar.getInstance().getTime();
		date.setHours(16);
		car.setTimeEntered(date);
		System.out.println(car.toString());*/
		
	}
}
