package com.megthink.gateway.api;

import com.megthink.gateway.api.response.PortMeAPIResponse;
import com.megthink.gateway.dao.NumberPlanDao;
import com.megthink.gateway.dao.SubscriberInfoQueryDetailDao;
import com.megthink.gateway.model.SubscriberInfoQueryDetail;
import com.megthink.gateway.model.User;
import com.megthink.gateway.producer.JmsProducer;
import com.megthink.gateway.repository.PortMeRepository;
import com.megthink.gateway.service.SubscriberInfoQueryDetailService;
import com.megthink.gateway.service.UserService;
import com.megthink.gateway.utils.APIConst;
import com.megthink.gateway.utils.NPOUtils;
import com.megthink.gateway.utils.PortMeUtils;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SubscriberValidationRestApi {

	private static final Logger _logger = LoggerFactory.getLogger(SubscriberValidationRestApi.class);

	Timestamp timestamp = new Timestamp(System.currentTimeMillis());

	@Autowired
	private UserService userService;
	@Autowired
	private SubscriberInfoQueryDetailDao subscriberInfoQueryDetailDao;
	@Autowired
	private PortMeRepository portMeRepository;
	@Autowired
	private NumberPlanDao numberPlanDao;
	@Autowired
	private JmsProducer jmsProducer;
	@Autowired
	private SubscriberInfoQueryDetailService subscriberInfoQueryDetailService;

	@PostMapping("/api/createnvpa")
	public ResponseEntity<?> createSubscriberNVPARequest(@RequestBody Map<String, Object> payload) {
		String sessionId = Long.toString(System.currentTimeMillis());
		_logger.info("[sessionId=" + sessionId
				+ "]: SubscriberValidationRestApi.createSubscriberNVPARequest() - NVPA Request process start with timestamp:["
				+ new Timestamp(System.currentTimeMillis()) + "]");
		List<String> checkedIds = (List<String>) payload.get("checkedIds");
		String selectCorporate = (String) payload.get("selectCorporate");
		String selectContractualObligation = (String) payload.get("selectContractualObligation");
		String selectActivateAging = (String) payload.get("selectActivateAging");
		String selectOwnership = (String) payload.get("selectOwnership");
		String selectOutstandingBill = (String) payload.get("selectOutstandingBill");
		String selectUnderSubJudice = (String) payload.get("selectUnderSubJudice");
		String selectPortingProhibited = (String) payload.get("selectPortingProhibited");
		String selectSimswap = (String) payload.get("selectSimswap");

		_logger.info("UID = [" + sessionId + "], Getting suspension donor bill ack request.");
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		ResponseEntity response = null;
		try {
			for (String referenceId : checkedIds) {
				SubscriberInfoQueryDetail subscriberInfoQueryDetail = subscriberInfoQueryDetailService
						.findByReferenceId(referenceId);
				if (subscriberInfoQueryDetail != null) {
					subscriberInfoQueryDetail.setCorporate(selectCorporate);
					subscriberInfoQueryDetail.setContractualObligation(selectContractualObligation);
					subscriberInfoQueryDetail.setActivateAging(selectActivateAging);
					subscriberInfoQueryDetail.setOwnershipChange(selectOwnership);
					subscriberInfoQueryDetail.setOutstandingBill(selectOutstandingBill);
					subscriberInfoQueryDetail.setUnderSubJudice(selectUnderSubJudice);
					subscriberInfoQueryDetail.setPortingProhibited(selectPortingProhibited);
					subscriberInfoQueryDetail.setSimSwap(selectSimswap);
					int mch_type = numberPlanDao.getMCHTypeByArea(subscriberInfoQueryDetail.getDnolsaId());
					if (mch_type != 0) {
						String requestId = null;
						if (mch_type == 1) {
							requestId = portMeRepository.getSynReqeustId(user.getOp_id());
							subscriberInfoQueryDetail.setRequestId(requestId);
						} else {
							subscriberInfoQueryDetail.setRequestId(referenceId);
						}
						int success = subscriberInfoQueryDetailDao
								.updateSubscriberInfoQueryDetail(subscriberInfoQueryDetail, sessionId);
						if (success != 0) {
							String xml = null;
							if (mch_type == 1) {
								xml = new NPOUtils().generateNVPA(subscriberInfoQueryDetail, 1);
							} else {
								xml = new NPOUtils().generateNVPA(subscriberInfoQueryDetail, 2);
							}
							int returnvalue = jmsProducer.sendMessageToInQueue(xml, sessionId, mch_type);
							if (returnvalue == 1) {
								PortMeAPIResponse errorBean = new PortMeAPIResponse();
								errorBean.setResponseCode(APIConst.successCode);
								errorBean.setResponseMessage(APIConst.successMsg);
								String msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
								response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
							} else {
								PortMeAPIResponse errorBean = new PortMeAPIResponse();
								errorBean.setResponseCode(APIConst.successCode2);
								errorBean.setResponseMessage(APIConst.successMsg2);
								String msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
								response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
							}
						} else {
							PortMeAPIResponse errorBean = new PortMeAPIResponse();
							errorBean.setResponseCode(APIConst.successCode2);
							errorBean.setResponseMessage("Failed to process request due to some error");
							String msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
							response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
						}
					}
				} else {
					_logger.debug("[sessionId=" + sessionId
							+ "]: SubscriberValidationRestApi.createSubscriberNVPARequest() - [ReferenceId - "
							+ referenceId + "] : Request doesn't exist in db  with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
				}
			}
		} catch (Exception e) {
			_logger.debug("[sessionId=" + sessionId
					+ "]: SubscriberValidationRestApi.createSubscriberNVPARequest() - Exception occeurs while process NVPA request, Error:["
					+ e.getMessage() + "]");
		}
		return response;
	}
}