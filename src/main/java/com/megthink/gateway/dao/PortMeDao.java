package com.megthink.gateway.dao;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import com.megthink.gateway.dao.common.CommonDao;
import com.megthink.gateway.dao.mapper.PortMeMapper;
import com.megthink.gateway.model.PortMe;

@Repository
public class PortMeDao extends CommonDao {

	private static final Logger _logger = LoggerFactory.getLogger(PortMeDao.class);

	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	public List<PortMe> getListPortMeDetails(String reqType, int userId) {
		List<PortMe> list = null;
		try {
			String sql = "select port_id,reference_id,area,dno,service,constants.description as status,data_type as dataType,Date(created_date_time) as created_date_time from port_tx "
					+ "join constants on constkey=:constkey and constcode=port_tx.status where request_type=:request_type order by created_date_time desc";
			MapSqlParameterSource namedParameters = new MapSqlParameterSource();
			namedParameters.addValue("constkey", reqType);
			namedParameters.addValue("request_type", reqType);
			// namedParameters.addValue("user_id", userId);
			list = namedParameterJdbcTemplate.query(sql, namedParameters, new PortMeMapper());
			_logger.debug(" PortMeDao.getListPortMeDetails() SQL = {} ; Returned list = {}", sql, list);
		} catch (Exception e) {
			list = new ArrayList<PortMe>();
			_logger.error("Exception occurs while getting PortmeDao.getListPortMeDetails-" + e.getMessage());
		}
		return list;
	}

	public List<PortMe> getListPortMeDetails(String reqType, int userId, String reqId, String dateRange,
			String msisdn) {
		List<PortMe> list = null;
		try {
			if (reqId != "" && dateRange == null && msisdn == "") {
				String sql = "select port_id,reference_id,area,dno,service,constants.description as status,data_type as dataType,Date(created_date_time) as created_date_time from port_tx "
						+ "join constants on constkey=:constkey and constcode=port_tx.status where request_type=:request_type and port_tx.reference_id=:reqId order by created_date_time desc";
				MapSqlParameterSource namedParameters = new MapSqlParameterSource();
				namedParameters.addValue("constkey", reqType);
				namedParameters.addValue("request_type", reqType);
				// namedParameters.addValue("user_id", userId);
				namedParameters.addValue("reqId", reqId);
				list = namedParameterJdbcTemplate.query(sql, namedParameters, new PortMeMapper());
			} else if (reqId == "" && dateRange != null && msisdn == "") {
				String sql = "select port_id,reference_id,area,dno,service,constants.description as status,data_type as dataType,Date(created_date_time) as created_date_time from port_tx "
						+ "join constants on constkey=:constkey and constcode=port_tx.status where request_type=:request_type AND Date(port_tx.created_date_time) BETWEEN "
						+ dateRange + " order by created_date_time desc";
				MapSqlParameterSource namedParameters = new MapSqlParameterSource();
				namedParameters.addValue("constkey", reqType);
				namedParameters.addValue("request_type", reqType);
				// namedParameters.addValue("user_id", userId);
				list = namedParameterJdbcTemplate.query(sql, namedParameters, new PortMeMapper());
			} else if (reqId != "" && dateRange != null && msisdn == "") {
				String sql = "select port_id,reference_id,area,dno,service,constants.description as status,data_type as dataType,Date(created_date_time) as created_date_time from port_tx "
						+ "join constants on constkey=:constkey and constcode=port_tx.status where request_type=:request_type and port_tx.reference_id=:reqId AND Date(port_tx.created_date_time) BETWEEN "
						+ dateRange + " order by created_date_time desc";
				MapSqlParameterSource namedParameters = new MapSqlParameterSource();
				namedParameters.addValue("constkey", reqType);
				namedParameters.addValue("request_type", reqType);
				// namedParameters.addValue("user_id", userId);
				namedParameters.addValue("reqId", reqId);
				list = namedParameterJdbcTemplate.query(sql, namedParameters, new PortMeMapper());
			}
		} catch (Exception e) {
			list = new ArrayList<PortMe>();
			_logger.error("Exception occurs while getting PortmeDao.getListPortMeDetails-" + e.getMessage());
		}
		return list;
	}

	public List<PortMe> getListPortMeDetail(String reqType, int userId, String reqId, String dateRange, String msisdn) {
		List<PortMe> list = null;
		try {
			String sql = "select port_tx.port_id, port_tx.reference_id, port_tx.area, port_tx.dno, port_tx.service, constants.description as status, port_tx.data_type as dataType, Date(port_tx.created_date_time) as created_date_time from port_tx "
					+ "join constants on constkey=:constkey and constcode=port_tx.status "
					+ "left join port_mt on port_tx.port_id = port_mt.port_id and port_mt.request_type=:request_type "
					+ "where port_tx.request_type=:request_type ";

			MapSqlParameterSource namedParameters = new MapSqlParameterSource();
			namedParameters.addValue("constkey", reqType);
			namedParameters.addValue("request_type", reqType);

			// Add conditions based on the provided parameters
			if (reqId != null && !reqId.trim().isEmpty()) {
				sql = sql + "and port_tx.reference_id=:reqId ";
				namedParameters.addValue("reqId", reqId);
			}
			if (dateRange != null && !dateRange.isEmpty()) {
				sql = sql + "and Date(port_tx.created_date_time) BETWEEN " + dateRange;
			}
			if (msisdn != null && !msisdn.trim().isEmpty()) {
				sql = sql + " and port_mt.msisdn=:msisdn ";
				namedParameters.addValue("msisdn", msisdn);
			}

			sql = sql
					+ " group by port_tx.port_id, port_tx.reference_id, constants.description order by port_tx.created_date_time desc";

			list = namedParameterJdbcTemplate.query(sql, namedParameters, new PortMeMapper());
		} catch (Exception e) {
			list = new ArrayList<>();
			_logger.error("Exception occurs while getting PortMeDao.getListPortMeDetails - " + e.getMessage());
		}
		return list;
	}

	public List<PortMe> getListPortMT(String reqType, int status, String opId) {
		List<PortMe> list = null;
		try {
			String sql = "select port_id,reference_id,area,dno,service,constants.description as status,data_type as dataType,Date(created_date_time) as created_date_time from port_tx "
					+ "join constants on constkey=:constkey and constcode=port_tx.status where request_type=:request_type and port_tx.status =:status order by created_date_time desc";
			MapSqlParameterSource namedParameters = new MapSqlParameterSource();
			namedParameters.addValue("constkey", reqType);
			namedParameters.addValue("request_type", reqType);
			namedParameters.addValue("status", status);
			// namedParameters.addValue("user_id", userId);
			list = namedParameterJdbcTemplate.query(sql, namedParameters, new PortMeMapper());
			_logger.info("getting port activation results...", sql);
			_logger.debug(" PortMeDao.getListPortMeDetails() SQL = {} ; Returned list = {}", sql, list);
		} catch (Exception e) {
			list = new ArrayList<PortMe>();
		}
		return list;
	}

	/* shobhit */
	public List<String> getListOfAreaByOpId(String opId) {
		try {
			String sql = "select area from msisdn_range where op_id=:opId";
			MapSqlParameterSource namedParameters = new MapSqlParameterSource();
			namedParameters.addValue("opId", opId);
			List<String> resultList = (List<String>) getNamedParameterJdbcOperations().queryForList(sql,
					namedParameters, String.class);
			_logger.debug(" UserDao.getUserHierarchy() SQL = {} ; Returned List = {}", sql, resultList);
			return resultList;
		} catch (Exception e) {
			return null;
		}
	}

	public String getRnbyOpIdandarea(String opId, String area) {
		try {
			String sql = "select routing_info from msisdn_range where op_id=:opId and area=:area";
			MapSqlParameterSource namedParameters = new MapSqlParameterSource();
			namedParameters.addValue("opId", opId);
			namedParameters.addValue("area", area);
			String resultList = getNamedParameterJdbcOperations().queryForObject(sql, namedParameters, String.class);
			_logger.debug(" UserDao.getUserHierarchy() SQL = {} ; Returned List = {}", sql, resultList);
			return resultList;
		} catch (Exception e) {
			return null;
		}
	}

	public String getdonorbymsisdn(String msisdn) {
		try {
			String sql = "select op_id from msisdn_range where :msisdn between start_range and end_range";
			MapSqlParameterSource namedParameters = new MapSqlParameterSource();
			namedParameters.addValue("msisdn", msisdn);
			String resultList = getNamedParameterJdbcOperations().queryForObject(sql, namedParameters, String.class);
			_logger.debug("UserDao.getUserHierary() SQL = {}; Returned List = {}", sql, resultList);
			return resultList;
		} catch (Exception e) {
			return null;
		}
	}
	/* end shobhit */

	public int isExistNPO(String referenceId, String requestType) {
		try {
			String sql = "select count(*) from port_tx where reference_id=:referenceId and request_type=:requestType";
			SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("referenceId", referenceId)
					.addValue("requestType", requestType);
			int count = executeSingleIntColumnQuery(sql, namedParameters);
			return count;
		} catch (Exception e) {
			return 0;
		}
	}

	public List<String> getListOfMSISDN(int id) {
		try {
			String sql = "select msisdn from port_mt where port_id=:id";
			MapSqlParameterSource namedParameters = new MapSqlParameterSource();
			namedParameters.addValue("id", id);
			List<String> resultList = (List<String>) getNamedParameterJdbcOperations().queryForList(sql,
					namedParameters, String.class);
			_logger.debug("PortMeDao.port_mt() SQL = {} ; Returned List = {}", sql, resultList);
			return resultList;
		} catch (Exception e) {
			return null;
		}
	}
}