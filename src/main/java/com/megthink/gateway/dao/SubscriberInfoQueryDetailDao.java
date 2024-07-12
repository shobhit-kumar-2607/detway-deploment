package com.megthink.gateway.dao;

import java.sql.Timestamp;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import com.megthink.gateway.dao.common.CommonDao;
import com.megthink.gateway.dao.mapper.SubscriberInfoQueryDetailMapper;
import com.megthink.gateway.model.SubscriberInfoQueryDetail;
import com.megthink.gateway.utils.ReadConfigFile;

@Repository
public class SubscriberInfoQueryDetailDao extends CommonDao {

	private static final Logger _logger = LoggerFactory.getLogger(SubscriberInfoQueryDetailDao.class);

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public List<SubscriberInfoQueryDetail> getSubscriberInfoQueryDetail() {
		List<SubscriberInfoQueryDetail> list = null;
		try {
			String sql = "SELECT * FROM msisdn_validation "
					+ "WHERE CAST(timeout_date AS timestamp) <= NOW()";
			MapSqlParameterSource namedParameters = new MapSqlParameterSource();
			list = namedParameterJdbcTemplate.query(sql, namedParameters, new SubscriberInfoQueryDetailMapper());
			_logger.debug(
					"SubscriberInfoQueryDetailDao.getSubscriberInfoQueryDetail() SQL = {} ; namedParameters = status : {} ; Returned list = {}",
					sql, 2, list);
		} catch (Exception e) {
			_logger.error(
					"Exception occurs when getting chart data SubscriberInfoQueryDetailDao.getSubscriberInfoQueryDetail()->"
							+ e.getMessage());
		}
		return list;
	}

	public int updateSubscriberInfoQueryDetail(SubscriberInfoQueryDetail item, String sessionId) {
		String sql = null;
		int success = 0;
		try {
			sql = "update msisdn_validation set status=2,corporate=:corporate, contractual_obligation=:contractual_obligation,activate_aging=:activate_aging,ownership_change=:ownership_change, "
					+ " outstanding_bill=:outstanding_bill,undersub_judice=:undersub_judice,porting_prohibited=:porting_prohibited,sim_swap=:sim_swap,updated_date=now() WHERE msisdn=:msisdn and reference_id=:referenceId";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("corporate", item.getCorporate())
					.addValue("contractual_obligation", item.getContractualObligation())
					.addValue("activate_aging", item.getActivateAging())
					.addValue("ownership_change", item.getOwnershipChange())
					.addValue("outstanding_bill", item.getOutstandingBill())
					.addValue("undersub_judice", item.getUnderSubJudice())
					.addValue("porting_prohibited", item.getPortingProhibited()).addValue("sim_swap", item.getSimSwap())
					.addValue("msisdn", item.getMsisdn()).addValue("referenceId", item.getReferenceId());
			executeUpdate(sql, namedParameters);
			success = 1;
		} catch (Exception e) {
			success = 0;
			_logger.error("[sessionId=" + sessionId
					+ "]: Exception occurs during update SubscriberInfoQueryDetailDao.updateSubscriberInfoQueryDetail(), sql : ["
					+ sql + "]  with timestamp:[" + new Timestamp(System.currentTimeMillis()) + "]-" + e.getMessage());
		}
		return success;
	}

	public void updateAckSubscriberInfoQueryDetail(String requestId, String sessionId, int resultCode) {
		String sql = null;
		try {
			sql = "update msisdn_validation set status=3, result_code=:resultCode ,updated_date=now() WHERE request_id=:requestId";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("requestId", requestId);
			executeUpdate(sql, namedParameters);
		} catch (Exception e) {
			_logger.error("[sessionId=" + sessionId
					+ "]: Exception occurs during update SubscriberInfoQueryDetailDao.updateAckSubscriberInfoQueryDetail(), sql : ["
					+ sql + "]  with timestamp:[" + new Timestamp(System.currentTimeMillis()) + "]-" + e.getMessage());
		}
	}
}