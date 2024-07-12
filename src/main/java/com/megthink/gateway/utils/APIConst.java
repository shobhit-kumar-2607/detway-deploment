package com.megthink.gateway.utils;

public class APIConst {

	public static final int successCode = 200;
	public static final String successMsg = "Successfully submitted";
	public static final int successCode1 = 101;
	public static final String successMsg1 = "something is wrong with requested data";
	public static final int successCode2 = 200;
	public static final String successMsg2 = "Your reqeust saved but not able to send to further processe. Please connect concern person";
	public static final int successCode3 = 103;
	public static final String successMsg3 = "Number plan data cann't be empty";
	public static final int successCode102 = 102;
	public static final String successMsg102 = "Uploaded bulk of file is not correct";
	public static final String successMsg4 = "Number plan already exist";
	public static final int successCode104 = 104;

	public static final int loggerCode1 = 101;
	public static final String loggerMsg1 = "Successfully Received portin request with requestId - ";
	public static final String loggerMsg2 = "Initiate port me reqeust with requestId - ";
	public static final String loggerMsg3 = "PortMe Corporate Transactiondetails insterted into db with reqeustId - ";
	public static final String loggerMsg4 = "PortMe Personal details insterted into db with reqeustId - ";
	public static final String loggerMsg5 = "going to validate corporate msisdn numbers with requestId - ";
	public static final String loggerMsg6 = "Number plan exist in our table with requestId - ";
	public static final String loggerMsg7 = "Number Plan route info is not correct";
	public static final String loggerMsg8 = "cann't allow to port of number plan";
	public static final String loggerMsg9 = "Number plan doesn't exist";
	public static final String loggerMsg19 = "Number plan from same operator to same lsa not allowed";
	public static final String loggerMsg10 = "Number Plan route info is not correct";
	public static final String loggerMsg11 = "Suspension cann't be created due to exceed timeline";
	public static final String loggerMsg12 = "Not able to generate transactionId";
	public static final String loggerMsg13 = "This number was never ported out";
	public static final String loggerMsg14 = "This number has already been requested";
	public static final String loggerMsg15 = "Request Not processed due to mapping setting, please inform to support teams";
	

}
