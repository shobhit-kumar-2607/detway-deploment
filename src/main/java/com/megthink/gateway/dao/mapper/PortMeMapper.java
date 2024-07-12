package com.megthink.gateway.dao.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.springframework.jdbc.core.RowMapper;
import com.megthink.gateway.model.PortMe;

public class PortMeMapper implements RowMapper<PortMe> {

	public PortMe mapRow(ResultSet rs, int rowNum) throws SQLException {
		PortMe portMe = new PortMe();
		portMe.setPortId(rs.getInt("port_id"));
		portMe.setReferenceId(rs.getString("reference_id"));
		portMe.setArea(rs.getString("area"));
		portMe.setDno(rs.getString("dno"));
		portMe.setService(rs.getString("service"));
		portMe.setStatusDesc(rs.getString("status"));
		portMe.setDataType(rs.getInt("dataType"));
		portMe.setCreatedDate(rs.getDate("created_date_time"));
		return portMe;
	}
}