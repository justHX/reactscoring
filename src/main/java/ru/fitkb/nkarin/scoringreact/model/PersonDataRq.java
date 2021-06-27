package ru.fitkb.nkarin.scoringreact.model;

import lombok.Data;

@Data
public class PersonDataRq {

	private String lastName;
	private String name;
	private String secondName;
	private String birthDate;
	private String birthLocation;
	private String sex;
	private String documentName;
	private String serialAndNumber;
	private String phone;
	private String address;
	private Integer age;
	private String nationality;
	private String materialStatus;
	private Boolean isChildren;
	private Double plusMoneyMonth;
	private Double minusMoneyMonth;
	private Boolean realty;
	private Boolean isCar;
	private String inn;
}
