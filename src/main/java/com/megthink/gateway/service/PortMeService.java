package com.megthink.gateway.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.megthink.gateway.model.PortMe;
import com.megthink.gateway.repository.PortMeRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

@Service("portMeService")
public class PortMeService {

	private static final Logger _logger = LoggerFactory.getLogger(PortMeService.class);

	private PortMeRepository portMeRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	public PortMeService(PortMeRepository portMeRepository) {
		this.portMeRepository = portMeRepository;
	}

	public PortMe savePortMe(PortMe portMe) {
		return portMeRepository.save(portMe);
	}

	public PortMe findByReferenceId(String query) {
		return portMeRepository.findByReferenceId(query);
	}

	public String getSynReqeustId(String op_id) {
		return portMeRepository.getSynReqeustId(op_id);
	}

	@Transactional
	public String getTransactionId(String param1, String param2) {
		try {
			int row = 0;
			String transactionId = null;
			String query = "SELECT * FROM generate_id2('" + param1 + "','" + param2 + "')";
			@SuppressWarnings("unchecked")
			List<Object[]> columns = entityManager.createNativeQuery(query).getResultList();
			for (Object obj : columns.get(0)) {
				// we set row == 0 so object have 2 data
				if (row == 0) {
					row++;
					transactionId = obj.toString();
				}
			}
			return transactionId;
		} catch (Exception e) {
			return null;
		}
	}

	@Transactional
	public String getReferenceId(String rno, String dno, String nrh, String portme) {
		try {
			String transactionId = null;
			String query = "SELECT * FROM generate_id1('" + rno + "','" + dno + "','" + nrh + "','" + portme + "')";
			@SuppressWarnings("unchecked")
			List<Object[]> columns = entityManager.createNativeQuery(query).getResultList();
			for (Object obj : columns) {
				transactionId = obj.toString();
			}
			return transactionId;
		} catch (Exception e) {
			return null;
		}
	}

	@Transactional
	public void updatePortMeStatus(int currstatus, String referenceId, String requestType) {
		try {
			entityManager.createNativeQuery(
					"update port_tx set status=?,updated_date_time = now() WHERE reference_id=? and request_type=?")
					.setParameter(1, currstatus).setParameter(2, referenceId).setParameter(3, requestType)
					.executeUpdate();
		} catch (Exception e) {
			_logger.error("PortMtDao.updatePortMtStatusByMsisdn() - " + e.getMessage());
		}
	}

	@Transactional
	public void updatePortMeStatusByRequestId(int currstatus, int response_code, String requestId, String requestType) {
		try {
			entityManager.createNativeQuery(
					"update port_tx set status=?, response_code=?, updated_date_time = now() WHERE request_id=? and request_type=?")
					.setParameter(1, currstatus).setParameter(2, response_code).setParameter(3, requestId)
					.setParameter(4, requestType).executeUpdate();
		} catch (Exception e) {
			_logger.error("PortMtDao.updatePortMtStatusByMsisdn() - " + e.getMessage());
		}
	}

	@Transactional
	public void updatePortMeStatus(int currstatus, String referenceId, int userId, int mch, String requestId) {
		try {
			entityManager.createNativeQuery(
					"UPDATE port_tx SET status = ?, user_id = ?, mch=?,request_id=?, updated_date_time = NOW() WHERE reference_id = ?")
					.setParameter(1, currstatus).setParameter(2, userId).setParameter(3, mch).setParameter(4, requestId)
					.setParameter(5, referenceId).executeUpdate();
		} catch (Exception e) {
			_logger.error("PortMeDao.updatePortMeStatus() - " + e.getMessage());
		}
	}

	@Transactional
	public void updatePortMeStats(int currstatus, String requestId, int userId, String referenceId, String reqType) {
		try {
			entityManager.createNativeQuery(
					"UPDATE port_tx SET status = ?, user_id = ?,request_id = ?, updated_date_time = NOW() WHERE reference_id=? and request_type=?")
					.setParameter(1, currstatus).setParameter(2, userId).setParameter(3, requestId)
					.setParameter(4, referenceId).setParameter(5, reqType).executeUpdate();
		} catch (Exception e) {
			_logger.error("PortMeDao.updatePortMeStatus() - " + e.getMessage());
		}
	}

	@Transactional
	public void updatePortMeDetails(int currstatus, String requestId, int userId, String referenceId, String reqType,
			String billingId, String instanceId) {
		try {
			entityManager.createNativeQuery(
					"UPDATE port_tx SET status = ?, user_id = ?,request_id = ?,billinguid1 = ?,instanceid=?, updated_date_time = NOW() WHERE reference_id=? and request_type=?")
					.setParameter(1, currstatus).setParameter(2, userId).setParameter(3, requestId)
					.setParameter(4, billingId).setParameter(5, instanceId).setParameter(6, referenceId)
					.setParameter(7, reqType).executeUpdate();
		} catch (Exception e) {
			_logger.error("PortMeDao.updatePortMeStatus() - " + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	@Transactional
	public List<PortMe> getListPortMtByDateRange(String reqType, int status, String dateRange, String reference_id,
			int userId) {
		List<PortMe> list = null;
		try {

			if (!reference_id.equals("") && dateRange == null) {
				String sql = "SELECT port_tx.* FROM port_tx JOIN constants ON constkey = ? AND constcode = port_tx.status WHERE request_type = ? "
						+ "AND port_tx.status = ? AND reference_id = ? ";
				Query query = entityManager.createNativeQuery(sql, PortMe.class).setParameter(1, reqType)
						.setParameter(2, reqType).setParameter(3, status).setParameter(4, reference_id);
				list = (List<PortMe>) query.getResultList();
			} else if (!reference_id.equals("") && dateRange != null) {
				String sql = "SELECT port_tx.* FROM port_tx JOIN constants ON constkey = ? AND constcode = port_tx.status WHERE request_type = ? "
						+ "AND port_tx.status = ? AND reference_id = ? AND Date(port_tx.created_date_time) BETWEEN "
						+ dateRange;
				Query query = entityManager.createNativeQuery(sql, PortMe.class).setParameter(1, reqType)
						.setParameter(2, reqType).setParameter(3, status).setParameter(4, reference_id);
				list = (List<PortMe>) query.getResultList();
			} else if (reference_id.equals("") && dateRange != null) {
				String sql = "SELECT port_tx.* FROM port_tx JOIN constants ON constkey = ? AND constcode = port_tx.status WHERE request_type = ? "
						+ "AND port_tx.status = ? AND Date(port_tx.created_date_time) BETWEEN " + dateRange;
				Query query = entityManager.createNativeQuery(sql, PortMe.class).setParameter(1, reqType)
						.setParameter(2, reqType).setParameter(3, status);
				list = (List<PortMe>) query.getResultList();
			} else {
				String sql = "SELECT port_tx.* FROM port_tx JOIN constants ON constkey = ? AND constcode = port_tx.status WHERE request_type = ? "
						+ "AND port_tx.status = ?";
				Query query = entityManager.createNativeQuery(sql, PortMe.class).setParameter(1, reqType)
						.setParameter(2, reqType).setParameter(3, status);
				list = (List<PortMe>) query.getResultList();
			}
		} catch (Exception e) {
			list = new ArrayList<PortMe>();
			_logger.error(
					"Exception occurs while getting PortMeService.getListPortMtByDateRange() - " + e.getMessage());
		}
		return list;
	}

	@Transactional
	public PortMe getListPortMeByReferenceId(String reqType, String reference_id) {
		PortMe list = null;
		try {
			String sql = "SELECT port_tx.* FROM port_tx WHERE request_type = ? AND reference_id = ? ";
			Query query = entityManager.createNativeQuery(sql, PortMe.class).setParameter(1, reqType).setParameter(2,
					reference_id);
			list = (PortMe) query.getSingleResult();
		} catch (Exception e) {
			list = new PortMe();
			_logger.error(
					"Exception occurs while getting PortMeService.getListPortMtByDateRange() - " + e.getMessage());
		}
		return list;
	}

	@Transactional
	public PortMe getPortMeByMsisdn(String reqType, String msisnd) {
		PortMe list = null;
		try {
			String sql = "SELECT port_tx.* FROM port_tx join port_mt on port_mt.port_id = port_tx.port_id "
					+ " WHERE port_mt.request_type = ? AND port_mt.msisdn = ?";
			Query query = entityManager.createNativeQuery(sql, PortMe.class).setParameter(1, reqType).setParameter(2,
					msisnd);
			list = (PortMe) query.getSingleResult();
		} catch (Exception e) {
			list = new PortMe();
			_logger.error("Exception occurs while getting PortMeService.getPortMeByMsisdn() - " + e.getMessage());
		}
		return list;
	}

	@Transactional
	public void updatePortMtResultCodeByMsisdn(int currstatus, String msisdn, String req_type, int resultCode) {
		try {
			entityManager.createNativeQuery(
					"UPDATE port_mt SET status = ?, result_code = ?, updated_date_time = NOW() WHERE msisdn = ? AND request_type = ?")
					.setParameter(1, currstatus).setParameter(2, resultCode).setParameter(3, msisdn)
					.setParameter(4, req_type).executeUpdate();
		} catch (Exception e) {
			_logger.error("PortMeService.updatePortMtResultCodeByMsisdn() - " + e.getMessage());
		}
	}

	@Transactional
	public void updateNPOARsp(int currstatus, String msisdn, String req_type, int resultCode,
			String orderedTransferTime) {
		try {
			if (orderedTransferTime != null) {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
				LocalDateTime localDateTime = LocalDateTime.parse(orderedTransferTime, formatter);
				Timestamp assigned_disc_time = Timestamp.valueOf(localDateTime);
				if (resultCode == 0) {
					entityManager.createNativeQuery(
							"UPDATE port_mt SET status = ?, assigned_disc_time=?, updated_date_time = NOW() WHERE msisdn = ? AND request_type = ?")
							.setParameter(1, currstatus).setParameter(2, assigned_disc_time).setParameter(3, msisdn)
							.setParameter(4, req_type).executeUpdate();
				} else {
					entityManager.createNativeQuery(
							"UPDATE port_mt SET status = ?, result_code = ?, assigned_disc_time=?, updated_date_time = NOW() WHERE msisdn = ? AND request_type = ?")
							.setParameter(1, currstatus).setParameter(2, resultCode).setParameter(3, assigned_disc_time)
							.setParameter(4, msisdn).setParameter(5, req_type).executeUpdate();
				}
			} else {
				entityManager.createNativeQuery(
						"UPDATE port_mt SET status = ?, result_code = ?, updated_date_time = NOW() WHERE msisdn = ? AND request_type = ?")
						.setParameter(1, currstatus).setParameter(2, resultCode).setParameter(3, msisdn)
						.setParameter(4, req_type).executeUpdate();
			}
		} catch (Exception e) {
			_logger.error("PortMeService.updateNPOARsp() - " + e.getMessage());
		}
	}

	@Transactional
	public void updateNPOARequest(int currstatus, String msisdn, String req_type, int resultCode,
			String orderedTransferTime) {
		try {
			if (orderedTransferTime == null) {
				entityManager.createNativeQuery(
						"UPDATE port_mt SET status = ?, result_code = ?, updated_date_time = NOW() WHERE msisdn = ? AND request_type = ?")
						.setParameter(1, currstatus).setParameter(2, resultCode).setParameter(3, msisdn)
						.setParameter(4, req_type).executeUpdate();
			} else {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
				LocalDateTime localDateTime = LocalDateTime.parse(orderedTransferTime, formatter);
				Timestamp timestamp = Timestamp.valueOf(localDateTime);
				entityManager.createNativeQuery(
						"UPDATE port_mt SET status = ?, result_code = ?,assigned_disc_time=?, updated_date_time = NOW() WHERE msisdn = ? AND request_type = ?")
						.setParameter(1, currstatus).setParameter(2, resultCode).setParameter(3, timestamp)
						.setParameter(4, msisdn).setParameter(5, req_type).executeUpdate();
			}
		} catch (Exception e) {
			_logger.error("PortMeService.updatePortMtResultCodeByMsisdn() - " + e.getMessage());
		}
	}

	@Transactional
	public void updatePortAprovalReqeust(int currstatus, String msisdn, String req_type, int resultCode) {
		try {
			entityManager.createNativeQuery(
					"UPDATE port_mt SET status = ?, result_code = ?, updated_date_time = NOW(), aproval_time=now() WHERE msisdn = ? AND request_type = ?")
					.setParameter(1, currstatus).setParameter(2, resultCode).setParameter(3, msisdn)
					.setParameter(4, req_type).executeUpdate();
		} catch (Exception e) {
			_logger.error("PortMeService.updatePortMtResultCodeByMsisdn() - " + e.getMessage());
		}
	}

	// @Transactional
	// public List<String> getListOfMSISDN(String referenceId, String requesType) {
	// String sql = "SELECT port_mt.msisdn FROM port_mt JOIN port_tx ON
	// port_mt.port_id = port_tx.port_id WHERE port_tx.reference_id = ? and
	// port_tx.request_type = ?";
	// Query query = entityManager.createNativeQuery(sql,
	// String.class).setParameter(1, referenceId).setParameter(2,
	// requesType);
	// @SuppressWarnings("unchecked")
	// List<String> list = (List<String>) query.getResultList();
	// return list;
	// }

	@Transactional
	public void cancelOrderByMsisdn(int currstatus, String msisdn, String requestType, String curReqType) {
		try {

			String sql = "UPDATE port_mt SET status = ?, request_type = ?, updated_date_time = NOW() WHERE msisdn = ? AND request_type = ?";
			entityManager.createNativeQuery(sql).setParameter(1, currstatus).setParameter(2, curReqType)
					.setParameter(3, msisdn).setParameter(4, requestType).executeUpdate();
		} catch (Exception e) {
			_logger.error("PortMeService.cancelOrderByMsisdn() - " + e.getMessage());
		}
	}

	// @SuppressWarnings({ "unchecked" })
	// @Transactional
	// public List<PortMe> getListPortMeDetailsByDateRange(String reqType, String
	// dateRange, String referenceId,
	// int userId, String msisdn) {
	// List<PortMe> list = null;
	// try {
	//
	// if ((!referenceId.equals("")) && (dateRange == null)) {
	// String sql = "select port_tx.*,constants.description as statusDesc from
	// port_tx "
	// + "join constants on constkey=? and constcode=port_tx.status where
	// request_type=? and port_tx.user_id=? and reference_id=? order by
	// created_date_time desc";
	// Query query = entityManager.createNativeQuery(sql,
	// PortMe.class).setParameter(1, reqType)
	// .setParameter(2, reqType).setParameter(3, userId).setParameter(4,
	// referenceId.trim());
	// list = (List<PortMe>) query.getResultList();
	// } else if (dateRange != null && referenceId.equals("")) {
	// String sql = "select port_tx.*,constants.description as statusDesc from
	// port_tx "
	// + " join constants on constkey=? and constcode=port_tx.status "
	// + " where request_type=? and port_tx.user_id=? and Date(created_date_time)
	// between " + dateRange
	// + " order by created_date_time desc";
	// Query query = entityManager.createNativeQuery(sql,
	// PortMe.class).setParameter(1, reqType)
	// .setParameter(2, reqType).setParameter(3, userId);
	// list = (List<PortMe>) query.getResultList();
	// } else if ((!referenceId.equals("")) && (dateRange != null)) {
	// String sql = "select port_tx.*,constants.description as statusDesc from
	// port_tx "
	// + " join constants on constkey=? and constcode=port_tx.status "
	// + " where request_type=? and port_tx.user_id=? and reference_id=? and
	// Date(created_date_time) between "
	// + dateRange + " order by created_date_time desc";
	// Query query = entityManager.createNativeQuery(sql,
	// PortMe.class).setParameter(1, reqType)
	// .setParameter(2, reqType).setParameter(3, userId).setParameter(4,
	// referenceId.trim());
	// list = (List<PortMe>) query.getResultList();
	// }
	// } catch (Exception e) {
	// list = new ArrayList<PortMe>();
	// _logger.error(
	// "Exception occurs while getting PortMeDao.getListPortMeDetailsByDateRange()-"
	// + e.getMessage());
	// }
	// return list;
	// }

	@SuppressWarnings("unchecked")
	@Transactional
	public List<PortMe> getReversalDetails(int id, String msisdn) {
		List<PortMe> details = null;
		try {
			if (msisdn != null) {
				String sql = "select * from port_history where msisdn=? and id =?";
				Query query = entityManager.createNativeQuery(sql, PortMe.class).setParameter(1, msisdn).setParameter(2,
						id);
				details = (List<PortMe>) query.getResultList();
			}
		} catch (Exception e) {
			details = new ArrayList<PortMe>();
			_logger.error("Exception occurs while getting PortMeService.getReversalDetails()-" + e.getMessage());
		}
		return details;
	}

	@Transactional
	public int isExistReqeust(String msisdn, String reqType) {
		int count = 0;
		try {
			if (msisdn != null) {
				String sql = "select id from port_mt where msisdn=? and request_type=?";
				Query query = entityManager.createNativeQuery(sql, PortMe.class).setParameter(1, msisdn).setParameter(2,
						reqType);
				count = query.getFirstResult();
			}
		} catch (Exception e) {
			count = 0;
			_logger.error("PortMtDao.isExist() - " + e.getMessage());
		}
		return count;
	}

	@Transactional
	public int getPortIdByReferenceId(String referenceId, String reqType) {
		int port_id = 0;
		try {
			String sql = "select port_id from port_tx where reference_id=? and request_type=?";
			Query query = entityManager.createNativeQuery(sql);
			query.setParameter(1, referenceId);
			query.setParameter(2, reqType);

			// Use getSingleResult() to fetch the single result
			Object result = query.getSingleResult();
			if (result != null) {
				port_id = ((Number) result).intValue();
			}
		} catch (Exception e) {
			port_id = 0;
			_logger.error("PortMtDao.isExist() - " + e.getMessage(), e);
		}
		return port_id;
	}
}