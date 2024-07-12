package com.megthink.gateway.model;

import jakarta.persistence.*;

@Entity
@Table(name = "tbl_billing_constant")
public class BillingConstant {

	@Id
	@Column(name = "id")
	private int id;

	@Column(name = "z1_code")
	private String z1Code;

	@Column(name = "z2_code")
	private String z2Code;

	@Column(name = "description")
	private String description;

	@Column(name = "request_type")
	private String requestType;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getZ1Code() {
		return z1Code;
	}

	public void setZ1Code(String z1Code) {
		this.z1Code = z1Code;
	}

	public String getZ2Code() {
		return z2Code;
	}

	public void setZ2Code(String z2Code) {
		this.z2Code = z2Code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getRequestType() {
		return requestType;
	}

	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}

}
