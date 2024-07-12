package com.megthink.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.megthink.gateway.model.TerminateSim;
import com.megthink.gateway.repository.TerminateSimRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

@Service("terminateSimService")
public class TerminateSimService {
	@PersistenceContext
	private EntityManager entityManager;

	private static final Logger _logger = LoggerFactory.getLogger(TerminateSimService.class);
	private TerminateSimRepository terminateSimRepository;

	@Autowired
	public TerminateSimService(TerminateSimRepository terminateSimRepository) {
		this.terminateSimRepository = terminateSimRepository;
	}

	public TerminateSim saveTerminateSim(TerminateSim terminateSim) {
		return terminateSimRepository.save(terminateSim);
	}

	public TerminateSim findByRequestId(String requestId) {
		return terminateSimRepository.findByRequestId(requestId);

	}

	@Transactional
	public void updateTerminateSIMbyReferenceId(String reqId, int currstatus, String refId, String reqType) {
		try {
			String sql = "update sim_terminate_tx set status=?, request_id=?, updated_date_time = now() WHERE reference_id=? and request_type=?";
			entityManager.createNativeQuery(sql).setParameter(1, currstatus).setParameter(2, reqId)
					.setParameter(3, refId).setParameter(4, reqType).executeUpdate();
		} catch (Exception e) {
			_logger.error("TerminateSimService.updateTerminateSIMbyRequestId() - " + e.getMessage());
		}
	}

	@Transactional
	public void updateTerminateSIM(String requestId, int currstatus, int userId, int mch) {
		if (requestId != null) {
			try {
				String sql = "update sim_terminate_tx set status=?,user_id=?,mch=?,updated_date_time = now() WHERE request_id=?";
				entityManager.createNativeQuery(sql).setParameter(1, currstatus).setParameter(2, userId)
						.setParameter(3, mch).setParameter(4, requestId).executeUpdate();
			} catch (Exception e) {
				_logger.error("TerminateSimService.updateTerminateSIM() - " + e.getMessage());
			}
		}
	}

	@Transactional
	public void updateTerminateSIM(int terminateId, int currstatus) {
		try {
			String sql = "update sim_terminate_tx set status=?,updated_date_time = now() WHERE terminate_id=?";
			entityManager.createNativeQuery(sql).setParameter(1, currstatus).setParameter(2, terminateId)
					.executeUpdate();
		} catch (Exception e) {
			_logger.error("TerminateSimService.updateTerminateSIM() - " + e.getMessage());
		}
	}

	@Transactional
	public TerminateSim getTerminateSimByReferenceIdAndRequestType(String referenceId, String requestType) {
		TerminateSim item = null;
		try {
			String sql = "select * from sim_terminate_tx where reference_id=? and request_type=?";
			Query query = entityManager.createNativeQuery(sql, TerminateSim.class).setParameter(1, referenceId)
					.setParameter(2, requestType);
			item = (TerminateSim) query.getSingleResult();
		} catch (Exception e) {
			_logger.error(
					"Exception occurs while geting TerminateSimService.getTerminateSimByReferenceIdAndRequestType()-"
							+ e.getMessage());
		}
		return item;
	}
}