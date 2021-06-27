package ru.fitkb.nkarin.scoringreact.dao.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonData {

	private String lastName;
	private String name;
	private String secondName;
	private String birthDate;
	private String sex;
	private String documentName;
	private String serialAndNumber;
	private String phone;
	private String address;
	private Integer age;
	private Double plusMoneyMonth;
	private Double minusMoneyMonth;
	private Boolean approvedCredit;
}
