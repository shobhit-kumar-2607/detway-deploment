package com.megthink.gateway.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.megthink.gateway.api.response.PortMeAPIResponse;
import com.megthink.gateway.dao.NumberPlanDao;
import com.megthink.gateway.dao.TerminateSimMTDao;
import com.megthink.gateway.form.TerminateForm;
import com.megthink.gateway.model.MSISDNUIDType;
import com.megthink.gateway.model.MasterNP;
import com.megthink.gateway.model.TerminateSim;
import com.megthink.gateway.model.TerminateSimMT;
import com.megthink.gateway.model.TerminateSimTransactionDetails;
import com.megthink.gateway.model.User;
import com.megthink.gateway.producer.JmsProducer;
import com.megthink.gateway.repository.PortMeRepository;
import com.megthink.gateway.service.MasterNPService;
import com.megthink.gateway.service.PortMeService;
import com.megthink.gateway.service.TerminateSimMTService;
import com.megthink.gateway.service.TerminateSimService;
import com.megthink.gateway.service.TerminateSimTransactionDetailsService;
import com.megthink.gateway.service.UserService;
import com.megthink.gateway.utils.NPOUtils;
import com.megthink.gateway.utils.PortMeUtils;
import com.megthink.gateway.utils.ReadConfigFile;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TerminateSIMRequestRestApi {

	private static final Logger _logger = LoggerFactory.getLogger(TerminateSIMRequestRestApi.class);

	Timestamp timestamp = new Timestamp(System.currentTimeMillis());

	@Autowired
	private UserService userService;
	@Autowired
	private TerminateSimService terminateSimService;
	@Autowired
	private TerminateSimTransactionDetailsService terminateTransactionService;
	@Autowired
	private TerminateSimMTService terminateSimMTService;
	@Autowired
	private TerminateSimMTDao terminateSimMTDao;
	@Autowired
	private PortMeRepository portMeRepository;
	@Autowired
	private MasterNPService masterNPService;
	@Autowired
	private PortMeService portMeService;
	@Autowired
	private JmsProducer jmsProducer;
	@Autowired
	private NumberPlanDao numberPlanDao;
	@Autowired
	private ObjectMapper objectMapper;

	@PostMapping("/api/terminatesim")
	public ResponseEntity<?> scheduleTermiateRequest(@RequestParam("terminateForm") String terminateForm) {
		_logger.info("getting initportrequest data");
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		ResponseEntity response = null;
		int current_status = 0;
		String requestId = null;
		PortMeAPIResponse errorBean = new PortMeAPIResponse();
		TerminateSimTransactionDetails terminationTransaction = new TerminateSimTransactionDetails();
		TerminateSimMT terminateSIMMt = new TerminateSimMT();
		String msg = null;
		String reqType = ReadConfigFile.getProperties().getProperty("TERMINATE_IN");
		int resultCode = 0;
		int mch_type = 0;
		String area = null;
		String referenceId = null;
		try {
			TerminateForm terminateItem = objectMapper.readValue(terminateForm, TerminateForm.class);
			TerminateSim terminationDetails = new TerminateSim();
			MasterNP masterNP = masterNPService.findByMsisdn(terminateItem.getMsisdn());
			MSISDNUIDType msisdnuId = new MSISDNUIDType();
			msisdnuId.setMsisdn(terminateItem.getMsisdn());
			List<MSISDNUIDType> list = new ArrayList<MSISDNUIDType>();
			list.add(msisdnuId);
			terminationDetails.setMsisdnUID(list);
			if (terminationDetails.getMsisdnUID().size() > 0) {
				String msisdn = terminationDetails.getMsisdnUID().get(0).getMsisdn();
				current_status = 1;
				String nrhArea = numberPlanDao.getNRH(msisdn);// in array
				String[] strList = nrhArea.split(",");
				String original_area = null;
				String original_op = null;
				if (strList.length == 2) {
					original_area = strList[0];
					original_op = strList[1];
					area = original_area;

				}
				if (original_area != null) {
					mch_type = numberPlanDao.getMCHTypeByArea(original_area);
				}
				if (mch_type == 1) {
					requestId = portMeRepository.getSynReqeustId(user.getOp_id());
					if (terminateItem.getIsSuspended()) {
						referenceId = numberPlanDao.getSusTransactionIdByMsisdn(msisdn);
					} else {
						referenceId = portMeService.getReferenceId(user.getOp_id(), original_op, original_op, "TER");
					}
				} else {
					if (terminateItem.getIsSuspended()) {
						requestId = numberPlanDao.getSusTransactionIdByMsisdn(msisdn);
						referenceId = requestId;
					} else {
						String donorLSAID = numberPlanDao.getDonorLSAID(msisdn);
						if (donorLSAID == null) {
							donorLSAID = original_area;
						}
						String messageSenderTelco = ReadConfigFile.getProperties()
								.getProperty("MessageSenderTelco-ZOOM");
						requestId = portMeService.getTransactionId(messageSenderTelco, area);
						referenceId = requestId;
					}
				}
				terminationDetails.setRequestId(requestId);
				_logger.info("received Terminate MSISDN request with requestId : " + requestId);
				terminationDetails.setArea(masterNP.getArea());
				terminationDetails.setOriginalCarrier(masterNP.getOrginal_carrier());
				terminationDetails.setRn(masterNP.getRn());
				terminationDetails.setService(masterNP.getService());
				terminationDetails.setUserId(user.getUserId());
				terminationDetails.setStatus(current_status);
				terminationDetails.setReference_id(referenceId);
				terminationDetails.setRno(user.getOp_id());
				terminationDetails.setTimeStamp(timestamp.toString());
				terminationDetails.setSource(user.getOp_id());
				terminationDetails.setDno(original_op);
				terminationDetails.setMch(mch_type);
				terminationDetails.setRequestType(reqType);
				TerminateSim terminateSim = terminateSimService.saveTerminateSim(terminationDetails);
				for (MSISDNUIDType msisdnuid : terminationDetails.getMsisdnUID()) {
					terminationTransaction = new TerminateSimTransactionDetails();
					terminationTransaction.setRequestId(msisdnuid.getRequestId());
					terminationTransaction.setStatus(current_status);
					terminationTransaction.setRequestType(reqType);
					terminationTransaction.setMsisdn(msisdnuid.getMsisdn());
					terminateTransactionService.saveTerminateSimTransactionDetails(terminationTransaction);
					terminateSIMMt = new TerminateSimMT();
					terminateSIMMt.setTerminateId(terminateSim.getTerminateId());
					terminateSIMMt.setRequest_type(reqType);
					terminateSIMMt.setStatus(current_status);
					terminateSIMMt.setSubscriberNumber(msisdnuid.getMsisdn());
					terminateSIMMt.setResultCode(resultCode);
					terminateSimMTService.saveTerminateSimMT(terminateSIMMt);
				}
				// going to convert into xml format
				List<TerminateSimMT> listMSISDN = terminateSimMTDao
						.getListOfTerminateSimMT(terminateSim.getTerminateId(), resultCode);
				if (listMSISDN.size() > 0) {
					current_status = 2;
					for (TerminateSimMT msisdnuid : listMSISDN) {
						terminationTransaction = new TerminateSimTransactionDetails();
						terminationTransaction.setRequestId(requestId);
						terminationTransaction.setStatus(current_status);
						terminationTransaction.setRequestType(reqType);
						terminationTransaction.setMsisdn(msisdnuid.getSubscriberNumber());
						terminateTransactionService.saveTerminateSimTransactionDetails(terminationTransaction);
						terminateSimMTService.updateTerminateSIMMT(current_status, msisdnuid.getSubscriberNumber(),
								reqType, resultCode);
					}

					_logger.info("trying to convert Termination MSISDN request into xml with requestId : " + requestId);
					String xml = new NPOUtils().convertJsonIntoTerminationSoap(terminationDetails, listMSISDN, mch_type,
							area, masterNP, terminateItem.getIsSuspended());
					_logger.info("convert portme termination request into xml with requestId:" + requestId + xml);
					String sessionId = Long.toString(System.currentTimeMillis());

					int returnvalue = jmsProducer.sendMessageToInQueue(xml, sessionId, mch_type);
					if (returnvalue != 0) {
						_logger.info("sent Termination MSISDN request in jms queue with requestId-" + requestId);
						current_status = 3;
						for (TerminateSimMT msisdnuid : listMSISDN) {
							terminationTransaction = new TerminateSimTransactionDetails();
							terminationTransaction.setRequestId(requestId);
							terminationTransaction.setStatus(current_status);
							terminationTransaction.setRequestType(reqType);
							terminationTransaction.setMsisdn(msisdnuid.getSubscriberNumber());
							terminateTransactionService.saveTerminateSimTransactionDetails(terminationTransaction);
							terminateSimMTService.updateTerminateSIMMT(current_status, msisdnuid.getSubscriberNumber(),
									reqType, resultCode);
						}
						_logger.info("Successfully Received Terminate MSISDN request with requestId:" + requestId);
						errorBean.setResponseCode(200);
						errorBean.setResponseMessage("Successfully Received");
						msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
						response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
					} else {
						_logger.info("Terminate MSISDN request saved successfully but not able to send queue");
						errorBean.setResponseCode(200);
						errorBean.setResponseMessage("Successfully saved data but not able to send queue");
						msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
						response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
					}
				} else {
					_logger.info("Terminate MSISDN request saved successfully but all MISDN number is not port before");
					errorBean.setResponseCode(200);
					errorBean.setResponseMessage("Successfully saved data but not able to send queue");
					msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
					response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
				}
				if (current_status != 0) {
					terminateSimService.updateTerminateSIM(terminateSim.getTerminateId(), current_status);
				}
			} else {
				_logger.info("Terminate MSISDN request is size zero with requestId :" + requestId);
				errorBean.setResponseCode(400);
				errorBean.setResponseMessage("Terminate msisdn request size is zero");
				msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
				response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
			}

		} catch (Exception e) {
			errorBean.setResponseCode(110);
			errorBean.setResponseMessage("Something is wrong with the system");
			msg = PortMeUtils.generateJsonResponse(errorBean, "ERROR");
			_logger.error("Something is wrong with the system - " + e.getMessage());
			response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.BAD_REQUEST);
		}
		if (current_status != 0) {
			terminateSimService.updateTerminateSIM(requestId, current_status, user.getUserId(), mch_type);
		}
		return response;
	}

	@PostMapping("/api/nrhconfirmation")
	public ResponseEntity<?> scheduleNRHConfirmation(@RequestParam("nrh_confirmation") String nrh_confirmation) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		ResponseEntity response = null;
		int current_status = 0;
		String requestId = null;
		PortMeAPIResponse errorBean = new PortMeAPIResponse();
		TerminateSimTransactionDetails terminationTransaction = new TerminateSimTransactionDetails();
		String msg = null;
		String reqType = ReadConfigFile.getProperties().getProperty("TERMINATE_OUT");
		int resultCode = 0;
		int mch_type = 0;
		try {
			JSONObject obj = new JSONObject(nrh_confirmation);
			String msisdn = obj.get("msisdn").toString();
			requestId = obj.get("requestId").toString();
			_logger.info(
					"received NRH_CONFIRMATION answer request with requestId : " + requestId + " msisdn :" + msisdn);
			TerminateSim terminationDetails = terminateSimService.getTerminateSimByReferenceIdAndRequestType(requestId,
					reqType);
			if (terminationDetails != null) {
				mch_type = terminationDetails.getMch();
				MSISDNUIDType msisdn_uid = new MSISDNUIDType();
				msisdn_uid.setMsisdn(msisdn);
				List<MSISDNUIDType> msisdnUID = new ArrayList<MSISDNUIDType>();
				msisdnUID.add(msisdn_uid);
				terminationDetails.setMsisdnUID(msisdnUID);
				if (terminationDetails.getMsisdnUID().size() > 0) {
					current_status = 6;
					for (MSISDNUIDType msisdnuid : terminationDetails.getMsisdnUID()) {
						terminationTransaction = new TerminateSimTransactionDetails();
						if (mch_type == 1) {
							requestId = portMeRepository.getSynReqeustId(user.getOp_id());
							terminationDetails.setRequestId(requestId);
							terminationTransaction.setRequestId(requestId);
						} else {
							terminationDetails.setRequestId(terminationDetails.getReference_id());
							terminationTransaction.setRequestId(terminationDetails.getReference_id());
						}
						terminationTransaction.setStatus(current_status);
						terminationTransaction.setRequestType(reqType);
						terminationTransaction.setMsisdn(msisdnuid.getMsisdn());
						terminateTransactionService.saveTerminateSimTransactionDetails(terminationTransaction);
						terminateSimMTService.updateTerminateSIMMT(current_status, msisdnuid.getMsisdn(), reqType,
								resultCode);

					}

					_logger.info("trying convert NRH_CONFIRMATION request into xml with requestId : " + requestId);
					String xml = new NPOUtils().convertJsonIntoNPOTAConfirmation(terminationDetails, mch_type);
					_logger.info("convert NRH_CONFIRMATION request into xml with requestId:" + requestId + xml);
					String sessionId = Long.toString(System.currentTimeMillis());
					int returnvalue = jmsProducer.sendMessageToInQueue(xml, sessionId, mch_type);
					if (returnvalue != 0) {
						terminateSimService.updateTerminateSIMbyReferenceId(terminationDetails.getRequestId(),
								current_status, terminationDetails.getReference_id(), reqType);
						_logger.info("sent NRH_CONFIRMATION request in jms queue with requestId-" + requestId);
						_logger.info("Successfully Received NRH_CONFIRMATION request with requestId:" + requestId);
						errorBean.setResponseCode(200);
						errorBean.setResponseMessage("Successfully Received");
						msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
						response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
					} else {
						_logger.info("NRH_CONFIRMATION request saved successfully but not able to send queue");
						errorBean.setResponseCode(200);
						errorBean.setResponseMessage("Successfully saved data but not able to send queue");
						msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
						response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
					}
				} else {
					_logger.info("NRH_CONFIRMATION request size is zero with requestId :" + requestId);
					errorBean.setResponseCode(400);
					errorBean.setResponseMessage("NRH_CONFIRMATION request size is zero");
					msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
					response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
				}
			} else {
				errorBean.setResponseCode(101);
				errorBean.setResponseMessage("Message Recieved null value");
				msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
				_logger.info("NRH_CONFIRMATION request recieved null value");
				response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.BAD_REQUEST);
			}

		} catch (Exception e) {
			errorBean.setResponseCode(110);
			errorBean.setResponseMessage("Something is wrong with the system");
			msg = PortMeUtils.generateJsonResponse(errorBean, "ERROR");
			_logger.error("Something is wrong with the system - " + e.getMessage());
			// e.printStackTrace();
			response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.BAD_REQUEST);
		}
		return response;
	}
}