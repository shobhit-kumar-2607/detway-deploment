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
import com.megthink.gateway.dao.mapper.BillingResolutionDetailsMapper;
import com.megthink.gateway.dao.mapper.BillingResolutionMapper;
import com.megthink.gateway.model.BillingResolution;
import com.megthink.gateway.model.BillingResolutionDetails;
import com.megthink.gateway.utils.ReadConfigFile;

@Repository
public class BillingResolutionDao extends CommonDao {

	private static final Logger _logger = LoggerFactory.getLogger(BillingResolutionDao.class);
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public int updateBillingResolution(BillingResolution item, String sessionId) {
		String sql = null;
		int success = 0;
		try {
			sql = "update tbl_billing_resolution set reason=:reason,status=:status,user_id=:user_id,request_id=:reqId,updated_date=now(),canceled_date=now() WHERE msisdn=:msisdn and request_type=:request_type";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("reason", item.getReason())
					.addValue("status", item.getStatus()).addValue("user_id", item.getUser_id())
					.addValue("reqId", item.getRequestId()).addValue("msisdn", item.getMsisdn())
					.addValue("request_type", item.getRequest_type());
			executeUpdate(sql, namedParameters);
			success = 1;
		} catch (Exception e) {
			success = 0;
			_logger.error("[sessionId=" + sessionId
					+ "]: Exception occurs during update BillingResolutionDao.updateBillingResolution(), sql : [" + sql
					+ "]  with timestamp:[" + new Timestamp(System.currentTimeMillis()) + "]-" + e.getMessage());
		}
		return success;
	}

	public int updateSusDnoACK(BillingResolution item, String sessionId) {
		String sql = null;
		int success = 0;
		try {
			sql = "update tbl_billing_resolution set bill_no=:bill_no,acc_no=:acc_no, status=:status,user_id=:user_id,request_id=:reqId,reason=:reason,updated_date=now(),ack_date=now() WHERE msisdn=:msisdn and request_type=:request_type";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("bill_no", item.getBill_no())
					.addValue("acc_no", item.getAcc_no()).addValue("status", item.getStatus())
					.addValue("user_id", item.getUser_id()).addValue("reqId", item.getRequestId())
					.addValue("reason", item.getReason()).addValue("msisdn", item.getMsisdn())
					.addValue("request_type", item.getRequest_type());
			executeUpdate(sql, namedParameters);
			success = 1;
		} catch (Exception e) {
			success = 0;
			_logger.error("[sessionId=" + sessionId
					+ "]: Exception occurs during update BillingResolutionDao.updateBillingResolutionACK(), sql : ["
					+ sql + "]  with timestamp:[" + new Timestamp(System.currentTimeMillis()) + "]-" + e.getMessage());
		}
		return success;
	}

	public int updateSusDnoReACK(BillingResolution item, String sessionId) {
		String sql = null;
		int success = 0;
		try {
			sql = "update tbl_billing_resolution set bill_no=:bill_no,acc_no=:acc_no, status=:status,user_id=:user_id,request_id=:reqId,reason=:reason,updated_date=now(),re_ack_date=now() WHERE msisdn=:msisdn and request_type=:request_type";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("bill_no", item.getBill_no())
					.addValue("acc_no", item.getAcc_no()).addValue("status", item.getStatus())
					.addValue("user_id", item.getUser_id()).addValue("reqId", item.getRequestId())
					.addValue("reason", item.getReason()).addValue("msisdn", item.getMsisdn())
					.addValue("request_type", item.getRequest_type());
			executeUpdate(sql, namedParameters);
			success = 1;
		} catch (Exception e) {
			success = 0;
			_logger.error("[sessionId=" + sessionId
					+ "]: Exception occurs during update BillingResolutionDao.updateBillingResolutionACK(), sql : ["
					+ sql + "]  with timestamp:[" + new Timestamp(System.currentTimeMillis()) + "]-" + e.getMessage());
		}
		return success;
	}

	public int updateSusRnoCon(BillingResolution item, String sessionId) {
		String sql = null;
		int success = 0;
		try {
			sql = "update tbl_billing_resolution set bill_no=:bill_no,acc_no=:acc_no, status=:status,user_id=:user_id,request_id=:reqId,reason=:reason,updated_date=now(),answer_date=now() WHERE msisdn=:msisdn and request_type=:request_type";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("bill_no", item.getBill_no())
					.addValue("acc_no", item.getAcc_no()).addValue("status", item.getStatus())
					.addValue("user_id", item.getUser_id()).addValue("reqId", item.getRequestId())
					.addValue("reason", item.getReason()).addValue("msisdn", item.getMsisdn())
					.addValue("request_type", item.getRequest_type());
			executeUpdate(sql, namedParameters);
			success = 1;
		} catch (Exception e) {
			success = 0;
			_logger.error("[sessionId=" + sessionId
					+ "]: Exception occurs during update BillingResolutionDao.updateBillingResolutionACK(), sql : ["
					+ sql + "]  with timestamp:[" + new Timestamp(System.currentTimeMillis()) + "]-" + e.getMessage());
		}
		return success;
	}

	public int updateSusRnoReCon(BillingResolution item, String sessionId) {
		String sql = null;
		int success = 0;
		try {
			sql = "update tbl_billing_resolution set bill_no=:bill_no,acc_no=:acc_no, status=:status,user_id=:user_id,request_id=:reqId,reason=:reason,updated_date=now(),re_answer_date=now() WHERE msisdn=:msisdn and request_type=:request_type";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("bill_no", item.getBill_no())
					.addValue("acc_no", item.getAcc_no()).addValue("status", item.getStatus())
					.addValue("user_id", item.getUser_id()).addValue("reqId", item.getRequestId())
					.addValue("reason", item.getReason()).addValue("msisdn", item.getMsisdn())
					.addValue("request_type", item.getRequest_type());
			executeUpdate(sql, namedParameters);
			success = 1;
		} catch (Exception e) {
			success = 0;
			_logger.error("[sessionId=" + sessionId
					+ "]: Exception occurs during update BillingResolutionDao.updateBillingResolutionACK(), sql : ["
					+ sql + "]  with timestamp:[" + new Timestamp(System.currentTimeMillis()) + "]-" + e.getMessage());
		}
		return success;
	}

	public String getRequestId(String msisdn) {
		String sql = null;
		try {
			sql = "select transaction_id from public.tbl_billing_resolution where msisdn= :msisdn";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("msisdn", msisdn);
			String reqId = executeSingleStringColumnQuery(sql, namedParameters);
			return reqId;
		} catch (Exception e) {
		}
		return null;
	}

	public String getMsisdnByReferenceId(String referenceId, String reqType) {
		String sql = null;
		try {
			sql = "select msisdn from public.tbl_billing_resolution where transaction_id= :referenceId and request_type=:reqType";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("referenceId", referenceId)
					.addValue("reqType", reqType);
			String msisdn = executeSingleStringColumnQuery(sql, namedParameters);
			return msisdn;
		} catch (Exception e) {
		}
		return null;
	}

	public int getStatusReferenceId(String referenceId, String reqType) {
		String sql = null;
		try {
			sql = "select status from public.tbl_billing_resolution where transaction_id= :referenceId and request_type=:reqType";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("referenceId", referenceId)
					.addValue("reqType", reqType);
			int status = executeSingleIntColumnQuery(sql, namedParameters);
			return status;
		} catch (Exception e) {
		}
		return 0;
	}

	public int isExist(String msisdn, String reqType) {
		try {
			String sql = "select count(*) from tbl_billing_resolution where msisdn= :msisdn and request_type=:reqType";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("msisdn", msisdn)
					.addValue("reqType", reqType);
			int count = executeSingleIntColumnQuery(sql, namedParameters);
			return count;
		} catch (Exception e) {
		}
		return 0;
	}

	public int updateNPOSPR(BillingResolution item, String sessionId) {
		String sql = null;
		int success = 0;
		try {
			sql = "update tbl_billing_resolution set reason=:reason,status=:status,updated_date=now(),canceled_date=now() WHERE transaction_id=:transaction_id and request_type=:request_type";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("reason", item.getReason())
					.addValue("status", item.getStatus()).addValue("transaction_id", item.getTransactionId())
					.addValue("request_type", item.getRequest_type());
			executeUpdate(sql, namedParameters);
			success = 1;
		} catch (Exception e) {
			success = 0;
			_logger.error("[sessionId=" + sessionId
					+ "]: Exception occurs during update BillingResolutionDao.updateNPOSPR(), sql : [" + sql
					+ "]  with timestamp:[" + new Timestamp(System.currentTimeMillis()) + "]-" + e.getMessage());
		}
		return success;
	}

	public int updateNPOSTER(BillingResolution item, String sessionId) {
		String sql = null;
		int success = 0;
		try {
			sql = "update tbl_billing_resolution set reason=:reason,status=:status,updated_date=now() WHERE transaction_id=:transaction_id and request_type=:request_type";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("reason", item.getReason())
					.addValue("status", item.getStatus()).addValue("transaction_id", item.getTransactionId())
					.addValue("request_type", item.getRequest_type());
			executeUpdate(sql, namedParameters);
			success = 1;
		} catch (Exception e) {
			success = 0;
			_logger.error("[sessionId=" + sessionId
					+ "]: Exception occurs during update BillingResolutionDao.updateNPOSTER(), sql : [" + sql
					+ "]  with timestamp:[" + new Timestamp(System.currentTimeMillis()) + "]-" + e.getMessage());
		}
		return success;
	}

	public int updateNPOSAACK(BillingResolution item, String sessionId) {
		String sql = null;
		int success = 0;
		try {
			if (item.getStatus() == 5) {
				sql = "update tbl_billing_resolution set reason=:reason,status=:status,updated_date=now(),re_ack_date=now() WHERE transaction_id=:transaction_id and request_type=:request_type";
			} else {
				sql = "update tbl_billing_resolution set reason=:reason,status=:status,updated_date=now(),ack_date=now() WHERE transaction_id=:transaction_id and request_type=:request_type";
			}
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("reason", item.getReason())
					.addValue("status", item.getStatus()).addValue("transaction_id", item.getTransactionId())
					.addValue("request_type", item.getRequest_type());
			executeUpdate(sql, namedParameters);
			success = 1;
		} catch (Exception e) {
			success = 0;
			_logger.error("[sessionId=" + sessionId
					+ "]: Exception occurs during update BillingResolutionDao.updateNPOSAACK(), sql : [" + sql
					+ "]  with timestamp:[" + new Timestamp(System.currentTimeMillis()) + "]-" + e.getMessage());
		}
		return success;
	}

	public int updateACK(String requestId, String requestType, int responseCode) {
		String sql = null;
		int success = 0;
		try {
			sql = "update tbl_billing_resolution set updated_date=now(),response_code=:responseCode WHERE request_id=:requestId and request_type=:requestType";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("responseCode", responseCode)
					.addValue("requestId", requestId).addValue("requestType", requestType);
			executeUpdate(sql, namedParameters);
			success = 1;
		} catch (Exception e) {
			success = 0;
			_logger.error("Exception occurs during update BillingResolutionDao.updateNPOSA(), sql : [" + sql
					+ "]  with timestamp:[" + new Timestamp(System.currentTimeMillis()) + "]-" + e.getMessage());
		}
		return success;
	}

	public int updateNPOSA(BillingResolution item, String sessionId, int status) {
		String sql = null;
		int success = 0;
		try {
			if (item.getStatus() == 4) {
				sql = "update tbl_billing_resolution set reason=:reason,status=:status,updated_date=now(), re_answer_date=now() WHERE transaction_id=:transaction_id and request_type=:request_type";
			} else {
				sql = "update tbl_billing_resolution set reason=:reason,status=:status,updated_date=now(), answer_date=now() WHERE transaction_id=:transaction_id and request_type=:request_type";
			}
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("reason", item.getReason())
					.addValue("status", item.getStatus()).addValue("transaction_id", item.getTransactionId())
					.addValue("request_type", item.getRequest_type());
			executeUpdate(sql, namedParameters);
			success = 1;
		} catch (Exception e) {
			success = 0;
			_logger.error("[sessionId=" + sessionId
					+ "]: Exception occurs during update BillingResolutionDao.updateNPOSA(), sql : [" + sql
					+ "]  with timestamp:[" + new Timestamp(System.currentTimeMillis()) + "]-" + e.getMessage());
		}
		return success;
	}

	public int updateNPOSRsp(BillingResolution item, String sessionId) {
		String sql = null;
		int success = 0;
		try {

			sql = "update tbl_billing_resolution set reason=:reason,status=:status,updated_date=now() WHERE transaction_id=:transaction_id and request_type=:request_type";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("reason", item.getReason())
					.addValue("status", item.getStatus()).addValue("transaction_id", item.getTransactionId())
					.addValue("request_type", item.getRequest_type());
			executeUpdate(sql, namedParameters);
			success = 1;
		} catch (Exception e) {
			success = 0;
			_logger.error("[sessionId=" + sessionId
					+ "]: Exception occurs during update BillingResolutionDao.updateNPOSRsp(), sql : [" + sql
					+ "]  with timestamp:[" + new Timestamp(System.currentTimeMillis()) + "]-" + e.getMessage());
		}
		return success;
	}

	public List<BillingResolution> getBillingResolutionDnoCancel(int statuss, String reqType, String transId,
			String msisdn) {
		List<BillingResolution> list = null;
		try {
			String days = ReadConfigFile.getProperties().getProperty("SUS_CANCEL_DAYS");
			// List<String> statusList = Arrays.asList("2"); // 1-DNO NPOS, 3-DNO ACK, 4-DNO
			// NPOSA

			String sql = "SELECT transaction_id, bill_no, acc_no, msisdn, bill_date, due_date, amount "
					+ "FROM tbl_billing_resolution WHERE status NOT IN (6,7) "
					+ "AND request_type = :reqType AND created_date > NOW() - INTERVAL '" + days + " DAY'";

			MapSqlParameterSource namedParameters = new MapSqlParameterSource();
			namedParameters.addValue("statuss", statuss);
			namedParameters.addValue("reqType", reqType);
			if (!transId.isEmpty()) {
				sql += "AND transaction_id = :transactionId ";
				namedParameters.addValue("transactionId", transId);
			}

			if (!msisdn.isEmpty()) {
				sql += "AND msisdn = :msisdn ";
				namedParameters.addValue("msisdn", msisdn);
			}

			list = namedParameterJdbcTemplate.query(sql, namedParameters, new BillingResolutionMapper());
			_logger.debug(
					"BillingResolutionDao.getBillingResolutionDnoCancel() SQL = {}; namedParameters = {}; Returned list = {}",
					sql, namedParameters, list);

		} catch (Exception e) {
			_logger.error(
					"BillingResolutionDao.getBillingResolutionDnoCancel()-Exception occurs when getting billing resolution data: {}",
					e.getMessage());
		}
		return list;
	}

	public List<BillingResolution> getBillingResolutionDnoAck(String status, String reqType) {
		List<BillingResolution> list = null;
		try {
			String sql = "select transaction_id,bill_no,acc_no,msisdn,bill_date,due_date,amount from tbl_billing_resolution where status IN ( "
					+ status + " ) and request_type=:reqType";
			MapSqlParameterSource namedParameters = new MapSqlParameterSource();
			// namedParameters.addValue("status", status);
			namedParameters.addValue("reqType", reqType);
			list = namedParameterJdbcTemplate.query(sql, namedParameters, new BillingResolutionMapper());
			_logger.debug(
					"BillingResolutionDao.getBillingResolutionDnoAck() SQL = {} ; namedParameters = status : {} ; Returned list = {}",
					sql, 2, list);
		} catch (Exception e) {
			_logger.error("Exception occurs when getting chart data BillingResolutionDao.getBillingResolutionDnoAck()->"
					+ e.getMessage());
		}
		return list;
	}

	public List<BillingResolution> getBillingResolutionDnoAck(String status, String reqType, String transId,
			String msisdn) {
		List<BillingResolution> list = null;
		try {
			String sql = "SELECT transaction_id, bill_no, acc_no, msisdn, bill_date, due_date, amount "
					+ "FROM tbl_billing_resolution WHERE status IN ( " + status + " ) AND request_type = :reqType ";
			MapSqlParameterSource namedParameters = new MapSqlParameterSource();
			// namedParameters.addValue("status", status);
			namedParameters.addValue("reqType", reqType);
			if (!transId.isEmpty()) {
				sql += "AND transaction_id = :transId ";
				namedParameters.addValue("transId", transId);
			}
			if (!msisdn.isEmpty()) {
				sql += "AND msisdn = :msisdn ";
				namedParameters.addValue("msisdn", msisdn);
			}
			list = namedParameterJdbcTemplate.query(sql, namedParameters, new BillingResolutionMapper());
			_logger.debug(
					"BillingResolutionDao.getBillingResolutionDnoAck() SQL = {}; namedParameters = {}; Returned list = {}",
					sql, namedParameters, list);
		} catch (Exception e) {
			_logger.error(
					"BillingResolutionDao.getBillingResolutionDnoAck()-Exception occurs when getting billing resolution data: {}",
					e.getMessage());
		}
		return list;
	}

	public List<BillingResolution> getBillingResolutionRnoPayment(int status, String reqType, String transId,
			String msisdn) {
		List<BillingResolution> list = null;
		try {
			String days = ReadConfigFile.getProperties().getProperty("SUS_RECEIPT_DAYS");
			String sql = "SELECT transaction_id, bill_no, acc_no, msisdn, bill_date, due_date, amount "
					+ " FROM tbl_billing_resolution WHERE status = :status AND request_type = :reqType "
					+ " AND ((ack_date IS NULL AND created_date >= current_date - INTERVAL '" + days
					+ " DAY') OR (ack_date::date = current_date - INTERVAL '" + days + " DAY')) ";
			MapSqlParameterSource namedParameters = new MapSqlParameterSource();
			namedParameters.addValue("status", status);
			namedParameters.addValue("reqType", reqType);
			if (!transId.isEmpty()) {
				sql += "AND transaction_id = :transId ";
				namedParameters.addValue("transId", transId);
			}
			if (!msisdn.isEmpty()) {
				sql += "AND msisdn = :msisdn ";
				namedParameters.addValue("msisdn", msisdn);
			}
			list = namedParameterJdbcTemplate.query(sql, namedParameters, new BillingResolutionMapper());
			_logger.debug(
					"BillingResolutionDao.getBillingResolutionRnoPayment() SQL = {}; namedParameters = {}; Returned list = {}",
					sql, namedParameters, list);
		} catch (Exception e) {
			_logger.error(
					"BillingResolutionDao.getBillingResolutionRnoPayment() - Exception occurs when getting billing resolution data: {}",
					e.getMessage());
		}
		return list;
	}

	public List<BillingResolution> getBillingResolutionReconnect(String reqType, String transactionId, String msisdn) {
		List<BillingResolution> list = null;
		try {
			String days = ReadConfigFile.getProperties().getProperty("SUS_RECEIPT_DAYS");
			String sql = "SELECT transaction_id, bill_no, acc_no, msisdn, bill_date, due_date, amount "
					+ " FROM tbl_billing_resolution "
					+ " WHERE status IN (3,5) AND request_type = :reqType AND created_date >= NOW() - INTERVAL '" + days
					+ " DAY'";
			MapSqlParameterSource namedParameters = new MapSqlParameterSource();
			// namedParameters.addValue("status", status);
			namedParameters.addValue("reqType", reqType);
			if (!transactionId.isEmpty()) {
				sql += "AND transaction_id = :transactionId ";
				namedParameters.addValue("transactionId", transactionId);
			}
			if (!msisdn.isEmpty()) {
				sql += "AND msisdn = :msisdn ";
				namedParameters.addValue("msisdn", msisdn);
			}
			list = namedParameterJdbcTemplate.query(sql, namedParameters, new BillingResolutionMapper());
			_logger.debug(
					"BillingResolutionDao.getBillingResolutionReconnect() SQL = {}; namedParameters = {}; Returned list = {}",
					sql, namedParameters, list);

		} catch (Exception e) {
			_logger.error(
					"BillingResolutionDao.getBillingResolutionReconnect() - Exception occurs when getting billing resolution data: {}",
					e.getMessage());
		}
		return list;
	}

	public BillingResolution getBillingResolutionByTransactionIdAndRequestType(String transactionId, String reqType) {
		List<BillingResolution> list = null;
		try {
			String sql = "select transaction_id,bill_no,acc_no,msisdn,bill_date,due_date,amount from tbl_billing_resolution where transaction_id=:transactionId and request_type=:reqType";
			MapSqlParameterSource namedParameters = new MapSqlParameterSource();
			namedParameters.addValue("transactionId", transactionId);
			namedParameters.addValue("reqType", reqType);
			list = namedParameterJdbcTemplate.query(sql, namedParameters, new BillingResolutionMapper());
			if (list.size() > 0) {
				return list.get(0);
			}
		} catch (Exception e) {
			_logger.error(
					"BillingResolutionDao.getBillingResolutionByTransactionIdAndRequestType() - Exception occurs when getting chart data BillingResolutionDao.getBillingResolutionByTransactionIdAndRequestType()->"
							+ e.getMessage());
		}
		return null;
	}

	public List<BillingResolutionDetails> getBillingResolutionStatus(String reqType, String transactionId,
			String msisdn) {
		List<BillingResolutionDetails> list = null;
		String sql = null;
		try {
			MapSqlParameterSource namedParameters = new MapSqlParameterSource();
			sql = "SELECT transaction_id, bill_no, acc_no, msisdn, bill_date, due_date, amount ,constants.description as status,to_char(created_date, 'YYYY-MM-DD HH24:MI:SS') AS created_date "
					+ " FROM tbl_billing_resolution "
					+ " JOIN constants on tbl_billing_resolution.status=constants.constcode AND constants.constkey = :reqType";
			namedParameters.addValue("reqType", reqType);

			if (!transactionId.isEmpty()) {
				sql = sql + " AND transaction_id = :transactionId ";
				namedParameters.addValue("transactionId", transactionId);
			}

			if (!msisdn.isEmpty()) {
				sql = sql + " AND msisdn = :msisdn";
				namedParameters.addValue("msisdn", msisdn);
			}
			sql = sql + " WHERE request_type = :reqType order by created_date desc";
			list = namedParameterJdbcTemplate.query(sql, namedParameters, new BillingResolutionDetailsMapper());
			_logger.debug(
					"BillingResolutionDao.getBillingResolutionStatus() SQL = {} ; namedParameters = {} ; Returned list = {}",
					sql, namedParameters.getValues(), list);
		} catch (Exception e) {
			_logger.error(
					"BillingResolutionDao.getBillingResolutionStatus() - Exception occurs when getting chart data BillingResolutionDao.getBillingResolutionStatus()->"
							+ e.getMessage());
		}
		return list;
	}

}