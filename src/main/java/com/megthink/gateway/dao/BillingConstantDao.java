package com.megthink.gateway.dao;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.megthink.gateway.dao.common.CommonDao;
import com.megthink.gateway.dao.mapper.BillingResolutionMapper;
import com.megthink.gateway.model.BillingResolution;

@Repository
public class BillingConstantDao extends CommonDao {

	private static final Logger _logger = LoggerFactory.getLogger(BillingConstantDao.class);
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public List<BillingResolution> getBillingResolutionByStatusAndRequestType(int status, String reqType) {
		List<BillingResolution> list = null;
		try {
			String sql = "select transaction_id,bill_no,acc_no,msisdn,bill_date,due_date,amount from tbl_billing_resolution where status=:status and request_type=:reqType";
			MapSqlParameterSource namedParameters = new MapSqlParameterSource();
			namedParameters.addValue("status", status);
			namedParameters.addValue("reqType", reqType);
			list = namedParameterJdbcTemplate.query(sql, namedParameters, new BillingResolutionMapper());
			_logger.debug(
					"BillingResolutionDao.getBillingResolutionByStatusAndRequestType() SQL = {} ; namedParameters = status : {} ; Returned list = {}",
					sql, 2, list);
		} catch (Exception e) {
			_logger.error(
					"Exception occurs when getting chart data BillingResolutionDao.getBillingResolutionByStatusAndRequestType()->"
							+ e.getMessage());
		}
		return list;
	}
}