package com.megthink.gateway.model;

public class NumReturnRequestWithRte {
	private NumberRange numberRange;
	private String lastRecipientLSAID;
	private String lastRecipient;
	private String route;
	private String comments;

	public NumberRange getNumberRange() {
		return numberRange;
	}

	public void setNumberRange(NumberRange numberRange) {
		this.numberRange = numberRange;
	}

	public String getLastRecipientLSAID() {
		return lastRecipientLSAID;
	}

	public void setLastRecipientLSAID(String lastRecipientLSAID) {
		this.lastRecipientLSAID = lastRecipientLSAID;
	}

	public String getLastRecipient() {
		return lastRecipient;
	}

	public void setLastRecipient(String lastRecipient) {
		this.lastRecipient = lastRecipient;
	}

	public String getRoute() {
		return route;
	}

	public void setRoute(String route) {
		this.route = route;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

}
