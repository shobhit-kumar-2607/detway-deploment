package com.megthink.gateway.dao.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;
import com.megthink.gateway.model.SubscriberInfoQueryDetail;

public class SubscriberInfoQueryDetailMapper implements RowMapper<SubscriberInfoQueryDetail> {

	public SubscriberInfoQueryDetail mapRow(ResultSet rs, int rowNum) throws SQLException {
		SubscriberInfoQueryDetail result = new SubscriberInfoQueryDetail();
		result.setMsisdn(rs.getString("msisdn"));
		result.setRequestId(rs.getString("request_id"));
		result.setReferenceId(rs.getString("reference_id"));
		result.setDnolsaId(rs.getString("dno_lsa_id"));
		result.setDonor(rs.getString("donor"));
		result.setTimeoutDate(rs.getString("timeout_date"));
		result.setCreated_date(rs.getTimestamp("created_date"));
		return result;
	}
}
