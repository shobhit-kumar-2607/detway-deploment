package com.megthink.gateway.dao.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;
import com.megthink.gateway.model.BillingResolution;

public class BillingResolutionMapper implements RowMapper<BillingResolution> {

	public BillingResolution mapRow(ResultSet rs, int rowNum) throws SQLException {
		BillingResolution item = new BillingResolution();
		item.setTransactionId(rs.getString("transaction_id"));
		item.setBill_no(rs.getString("bill_no"));
		item.setAcc_no(rs.getString("acc_no"));
		item.setMsisdn(rs.getString("msisdn"));
		item.setBill_date(rs.getString("bill_date"));
		item.setDue_date(rs.getString("due_date"));
		item.setAmount(rs.getString("amount"));
		return item;
	}
}
