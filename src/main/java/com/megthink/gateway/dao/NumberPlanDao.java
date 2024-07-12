package com.megthink.gateway.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import com.megthink.gateway.dao.common.CommonDao;
import com.megthink.gateway.dao.mapper.NumberPlanMapper;
import com.megthink.gateway.model.NumberPlan;

@Repository
public class NumberPlanDao extends CommonDao {

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	private static final Logger _logger = LoggerFactory.getLogger(NumberPlanDao.class);

	public List<NumberPlan> getOperatorInformation() {
		List<NumberPlan> list = new ArrayList<NumberPlan>();
		try {
			String sql = "SELECT slno, op_id, op_name, null as area, null as start_range, null as end_range, null as technology, null as type, null as routing_info FROM operator_information";
			list = namedParameterJdbcTemplate.query(sql, new NumberPlanMapper());
			_logger.debug(" PlanNumberDao.getOperatorInformation() SQL = {} ; Returned list = {}", sql, list);
		} catch (Exception e) {
			_logger.error("Exception occurs while getting PlanNumberDao.getOperatorInformation()-" + e.getMessage());
		}
		return list;
	}

	public String getArea(String msisdn) {
		String area = null;
		String sql = null;
		try {
			sql = "select area from msisdn_range where :msisdn BETWEEN msisdn_range.start_range AND msisdn_range.end_range limit 1";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("msisdn", msisdn);
			area = executeSingleStringColumnQuery(sql, namedParameters);
		} catch (Exception e) {
			area = null;
		}
		if (area == null) {
			area = "NA";
		}
		return area;
	}

	public String getDonorLSAID(String msisdn) {
		String area = null;
		String sql = null;
		try {
			sql = "select area from public.tbl_master_np where msisdn= :msisdn limit 1";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("msisdn", msisdn);
			area = executeSingleStringColumnQuery(sql, namedParameters);
		} catch (Exception e) {
			area = null;
		}
		return area;
	}

	public String getSusTransactionIdByMsisdn(String msisdn) {
		String transactionId = null;
		String sql = null;
		try {
			sql = "select transaction_id from tbl_billing_resolution where msisdn= :msisdn limit 1";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("msisdn", msisdn);
			transactionId = executeSingleStringColumnQuery(sql, namedParameters);
		} catch (Exception e) {
			transactionId = null;
		}
		return transactionId;
	}

	public String getNRH(String msisdn) {
		String sql = null;
		try {
			sql = "select area, op_id from public.msisdn_range where :msisdn between start_range and end_range";
			// sql = "select area from public.msisdn_range where :msisdn between start_range
			// and end_range";
			// SqlParameterSource namedParameters = new
			// MapSqlParameterSource().addValue("msisdn", msisdn);
			// area = executeSingleStringColumnQuery(sql, namedParameters);
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("msisdn", msisdn);
			Map<String, Object> result = namedParameterJdbcTemplate.queryForMap(sql, namedParameters);
			String area = (String) result.get("area");
			String opId = (String) result.get("op_id");
			return area.trim() + "," + opId.trim();
		} catch (Exception e) {
		}
		return null;
	}

	public String getOperatorId(String msisdn) {
		String sql = null;
		try {
			sql = "select present_carrier from public.tbl_master_np where msisdn= :msisdn limit 1";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("msisdn", msisdn);
			String opId = executeSingleStringColumnQuery(sql, namedParameters);
			return opId;
		} catch (Exception e) {
		}
		return null;
	}

	public String getRnoDno(String msisdn) {
		String sql = null;
		try {
			sql = "select orginal_carrier, present_carrier from public.tbl_master_np where msisdn= :msisdn limit 1";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("msisdn", msisdn);
			Map<String, Object> result = namedParameterJdbcTemplate.queryForMap(sql, namedParameters);
			String op = (String) result.get("orginal_carrier");
			String opId = (String) result.get("present_carrier");
			return op.trim() + "," + opId.trim();
		} catch (Exception e) {
		}
		return null;
	}

	public String getRouteInfo(String id, String requestType) {
		String routeInfo = null;
		String sql = null;
		try {
			sql = "select rn from port_tx where reference_id= :id and request_type=:reqType";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("id", id).addValue("reqType",
					requestType);
			routeInfo = executeSingleStringColumnQuery(sql, namedParameters);
		} catch (Exception e) {
			routeInfo = null;
		}
		return routeInfo;
	}

	public String getRouteInfoByMsisdn(String msisdn) {
		String routeInfo = null;
		String sql = null;
		try {
			sql = "select routing_info from msisdn_range where msisdn= :msisdn between start_range and end_range";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("msisdn", msisdn);
			routeInfo = executeSingleStringColumnQuery(sql, namedParameters);
		} catch (Exception e) {
			routeInfo = null;
		}
		return routeInfo;
	}

	public int getMCHTypeByArea(String area) {
		String sql = "SELECT mch FROM public.lsa_zone_mapping WHERE lsa LIKE CONCAT('%', :area, '%') LIMIT 1";
		MapSqlParameterSource namedParameters = new MapSqlParameterSource().addValue("area", area);
		try {
			return namedParameterJdbcTemplate.queryForObject(sql, namedParameters, Integer.class);
		} catch (EmptyResultDataAccessException e) {
			_logger.error("No result found for area: " + area + " - " + sql);
			return 0;
		} catch (Exception e) {
			_logger.error("Exception occurs during NumberPlanDao.getMCHTypeByArea() - " + e.getMessage() + " - " + sql);
			return 0;
		}
	}

	public int calculatePortingDate(String msisdn) {
		try {
			String sql = "SELECT CEIL(EXTRACT(EPOCH FROM (current_timestamp - created_date_time::timestamp)) / 86400) AS days_difference "
					+ "from port_mt where msisdn=:msisdn order by created_date_time desc limit 1";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("msisdn", msisdn);
			int days = executeSingleIntColumnQuery(sql, namedParameters);
			return days;
		} catch (Exception e) {

		}
		return 0;
	}
}
