package com.megthink.gateway.model;

import java.sql.Timestamp;

import jakarta.persistence.*;

@Entity
@Table(name = "msisdn_validation")
public class SubscriberInfoQueryDetail {

	@Id
	@Column(name = "msisdn")
	private String msisdn;

	@Column(name = "request_id")
	private String requestId;

	@Column(name = "reference_id")
	private String referenceId;

	@Column(name = "dno_lsa_id")
	private String dnolsaId;

	@Column(name = "donor")
	private String donor;

	@Column(name = "timeout_date")
	private String timeoutDate;

	@Column(name = "remark")
	private String remark;

	@Column(name = "corporate")
	private String corporate;

	@Column(name = "contractual_obligation")
	private String contractualObligation;

	@Column(name = "activate_aging")
	private String activateAging;

	@Column(name = "ownership_change")
	private String ownershipChange;

	@Column(name = "outstanding_bill")
	private String outstandingBill;

	@Column(name = "undersub_judice")
	private String underSubJudice;

	@Column(name = "porting_prohibited")
	private String portingProhibited;

	@Column(name = "sim_swap")
	private String simSwap;

	@Column(name = "status")
	private int status;

	@Column(name = "result_code")
	private int result_code;

	@Column(name = "created_date")
	private Timestamp created_date;

	@Column(name = "updated_date")
	private Timestamp updated_date;

	public String getMsisdn() {
		return msisdn;
	}

	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public String getReferenceId() {
		return referenceId;
	}

	public void setReferenceId(String referenceId) {
		this.referenceId = referenceId;
	}

	public String getDnolsaId() {
		return dnolsaId;
	}

	public void setDnolsaId(String dnolsaId) {
		this.dnolsaId = dnolsaId;
	}

	public String getDonor() {
		return donor;
	}

	public void setDonor(String donor) {
		this.donor = donor;
	}

	public String getTimeoutDate() {
		return timeoutDate;
	}

	public void setTimeoutDate(String timeoutDate) {
		this.timeoutDate = timeoutDate;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getCorporate() {
		return corporate;
	}

	public void setCorporate(String corporate) {
		this.corporate = corporate;
	}

	public String getContractualObligation() {
		return contractualObligation;
	}

	public void setContractualObligation(String contractualObligation) {
		this.contractualObligation = contractualObligation;
	}

	public String getActivateAging() {
		return activateAging;
	}

	public void setActivateAging(String activateAging) {
		this.activateAging = activateAging;
	}

	public String getOwnershipChange() {
		return ownershipChange;
	}

	public void setOwnershipChange(String ownershipChange) {
		this.ownershipChange = ownershipChange;
	}

	public String getOutstandingBill() {
		return outstandingBill;
	}

	public void setOutstandingBill(String outstandingBill) {
		this.outstandingBill = outstandingBill;
	}

	public String getUnderSubJudice() {
		return underSubJudice;
	}

	public void setUnderSubJudice(String underSubJudice) {
		this.underSubJudice = underSubJudice;
	}

	public String getPortingProhibited() {
		return portingProhibited;
	}

	public void setPortingProhibited(String portingProhibited) {
		this.portingProhibited = portingProhibited;
	}

	public String getSimSwap() {
		return simSwap;
	}

	public void setSimSwap(String simSwap) {
		this.simSwap = simSwap;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getResult_code() {
		return result_code;
	}

	public void setResult_code(int result_code) {
		this.result_code = result_code;
	}

	public Timestamp getCreated_date() {
		return created_date;
	}

	public void setCreated_date(Timestamp created_date) {
		this.created_date = created_date;
	}

	public Timestamp getUpdated_date() {
		return updated_date;
	}

	public void setUpdated_date(Timestamp updated_date) {
		this.updated_date = updated_date;
	}

}
