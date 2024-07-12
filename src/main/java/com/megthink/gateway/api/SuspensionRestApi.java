package com.megthink.gateway.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.megthink.gateway.api.response.PortMeAPIResponse;
import com.megthink.gateway.dao.BillingResolutionDao;
import com.megthink.gateway.dao.NumberPlanDao;
import com.megthink.gateway.form.SuspensionForm;
import com.megthink.gateway.model.BillingConstant;
import com.megthink.gateway.model.BillingResolution;
import com.megthink.gateway.model.PortMe;
import com.megthink.gateway.model.User;
import com.megthink.gateway.producer.JmsProducer;
import com.megthink.gateway.repository.PortMeRepository;
import com.megthink.gateway.service.BillingConstantService;
import com.megthink.gateway.service.BillingResolutionService;
import com.megthink.gateway.service.PortMeService;
import com.megthink.gateway.service.UserService;
import com.megthink.gateway.utils.APIConst;
import com.megthink.gateway.utils.NPOUtils;
import com.megthink.gateway.utils.PortMeUtils;
import com.megthink.gateway.utils.ReadConfigFile;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SuspensionRestApi {

	private static final Logger _logger = LoggerFactory.getLogger(SuspensionRestApi.class);

	Timestamp timestamp = new Timestamp(System.currentTimeMillis());

	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private UserService userService;
	@Autowired
	private PortMeService portMeService;
	@Autowired
	private BillingResolutionService billingResolutionService;
	@Autowired
	private BillingConstantService billingConstantService;
	@Autowired
	private PortMeRepository portMeRepository;
	@Autowired
	private NumberPlanDao numberPlanDao;
	@Autowired
	private JmsProducer jmsProducer;
	@Autowired
	private BillingResolutionDao billingResolutionDao;

	@PostMapping("/api/suspension-dno-request")
	public ResponseEntity<?> suspensionBillPayRequest(@RequestParam("suspension") String suspension) {
		String sessionId = Long.toString(System.currentTimeMillis());
		_logger.info("UID = [" + sessionId + "], Getting suspension donor request.");
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		ResponseEntity response = null;
		String reqType = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
		try {
			SuspensionForm suspensionForm = objectMapper.readValue(suspension, SuspensionForm.class);
			if (suspensionForm != null) {
				int isExist = billingResolutionDao.isExist(suspensionForm.getMsisdn(), reqType);
				if (isExist == 0) {
					int noOfDays = numberPlanDao.calculatePortingDate(suspensionForm.getMsisdn());
					if (noOfDays != 0) {
						String days = ReadConfigFile.getProperties().getProperty("SUS_PORTINDAYS");
						int convertDays = Integer.parseInt(days);
						if (noOfDays <= convertDays) {
							/* fetch for area to get MCH type zone 1 or zone 2 */
							// String area = numberPlanDao.getArea(suspensionForm.getMsisdn());
							// String nrhArea = numberPlanDao.getNRH(suspensionForm.getMsisdn());// in array
							String area = null;
							String requestType = ReadConfigFile.getProperties().getProperty("PORTME_IN");
							PortMe portMe = portMeService.getPortMeByMsisdn(requestType, suspensionForm.getMsisdn());
							if (portMe != null) {
								area = portMe.getArea();
								int mch_type = portMe.getMch();
								String nrhArea = numberPlanDao.getNRH(suspensionForm.getMsisdn());// in array
								String[] stringList = nrhArea.split(",");
								String original_area = null;
								// String original_op = null;
								if (stringList.length == 2) {
									original_area = stringList[0];
									area = original_area;
									// original_op = stringList[1];
								}
								mch_type = numberPlanDao.getMCHTypeByArea(original_area);
								if (mch_type != 0) {

									BillingResolution billingResolution = new BillingResolution();
									billingResolution.setBill_no(suspensionForm.getBillingUid());
									billingResolution.setAcc_no(suspensionForm.getAccountId());
									billingResolution.setArea(area);
									billingResolution.setRno(portMe.getRno());
									billingResolution.setDno(portMe.getDno());
									billingResolution.setOriginal_op(portMe.getOriginal_op());
									billingResolution.setLast_area(portMe.getLast_area());
									billingResolution.setMsisdn(suspensionForm.getMsisdn());
									billingResolution.setBill_date(suspensionForm.getBillDate());
									billingResolution.setDue_date(suspensionForm.getDueDate());
									billingResolution.setAmount(suspensionForm.getAmount());
									billingResolution.setComments(suspensionForm.getComment());
									billingResolution.setRequest_type(reqType);
									billingResolution.setStatus(1);
									billingResolution.setUser_id(user.getUserId());
									billingResolution.setCreated_date(timestamp);
									billingResolution.setUpdated_date(timestamp);
									String messageSenderTelco = ReadConfigFile.getProperties()
											.getProperty("MessageSenderTelco-ZOOM");
									String xml = null;
									if (mch_type == 1) {
										// String opId = numberPlanDao.getOperatorId(suspensionForm.getMsisdn());
										String requestId = portMeRepository.getSynReqeustId(user.getOp_id());
										billingResolution.setRequestId(requestId);
										// String rnodno = numberPlanDao.getRnoDno(suspensionForm.getMsisdn());
										// String[] strList = rnodno.split(",");
										String rno = portMe.getRno();// null;
										String dno = portMe.getDno();// null;
										// if (strList.length == 2) {
										// dno = strList[0];
										// rno = strList[1];
										//
										// }
										String referenceId = portMeService.getReferenceId(rno, dno, dno, "SUS");
										if (referenceId != null) {
											billingResolution.setTransactionId(referenceId);
											xml = new NPOUtils().convertJsonIntoInitSuspensionRequest(billingResolution,
													messageSenderTelco, mch_type, area, requestId);
										} else {
											_logger.info("Not able to generate reference id");
										}
									} else {
										// String donorLSAID = numberPlanDao.getDonorLSAID(suspensionForm.getMsisdn());
										// if (donorLSAID == null) {
										// donorLSAID = area;
										// }
										String transactionId = portMeService.getTransactionId(messageSenderTelco, area);
										if (transactionId != null) {
											billingResolution.setTransactionId(transactionId);
											billingResolution.setRequestId(transactionId);
											xml = new NPOUtils().convertJsonIntoInitSuspensionRequest(billingResolution,
													messageSenderTelco, mch_type, area, null);
										}
									}
									if (billingResolution.getTransactionId() != null) {
										billingResolutionService.saveBillingResolution(billingResolution);
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
										errorBean.setResponseMessage(APIConst.loggerMsg12);
										String msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
										response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
									}
								} else {
									PortMeAPIResponse errorBean = new PortMeAPIResponse();
									errorBean.setResponseCode(APIConst.loggerCode1);
									errorBean.setResponseMessage(APIConst.loggerMsg15);
									String msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
									response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
								}
							} else {
								PortMeAPIResponse errorBean = new PortMeAPIResponse();
								errorBean.setResponseCode(APIConst.loggerCode1);
								errorBean.setResponseMessage(APIConst.loggerMsg9);
								String msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
								response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
							}

						} else {
							PortMeAPIResponse errorBean = new PortMeAPIResponse();
							errorBean.setResponseCode(APIConst.loggerCode1);
							errorBean.setResponseMessage(APIConst.loggerMsg11);
							String msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
							response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
						}
					} else {
						PortMeAPIResponse errorBean = new PortMeAPIResponse();
						errorBean.setResponseCode(APIConst.loggerCode1);
						errorBean.setResponseMessage(APIConst.loggerMsg13);
						String msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
						response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
					}
				} else {
					PortMeAPIResponse errorBean = new PortMeAPIResponse();
					errorBean.setResponseCode(APIConst.loggerCode1);
					errorBean.setResponseMessage(APIConst.loggerMsg14);
					String msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
					response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
				}
			}
		} catch (Exception e) {
			return null;
		}
		return response;
	}

	@PostMapping("/api/suspension-dno-cancel")
	public ResponseEntity<?> suspensionBillCancelRequest(@RequestBody Map<String, Object> payload) {
		@SuppressWarnings("unchecked")
		List<String> checkedIds = (List<String>) payload.get("checkedIds");
		int selectReasonId = Integer.parseInt((String) payload.get("selectReason"));
		String sessionId = Long.toString(System.currentTimeMillis());
		_logger.info("UID = [" + sessionId + "], Getting suspension donor cancel request.");
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		String reqType = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
		ResponseEntity response = null;
		try {
			for (String transId : checkedIds) {
				BillingResolution suspensionForm = billingResolutionDao
						.getBillingResolutionByTransactionIdAndRequestType(transId, reqType);
				if (suspensionForm != null) {
					/* fetch for area to get mch type zone 1 or zone 2 */
					String area = numberPlanDao.getArea(suspensionForm.getMsisdn());
					if (area != "NA") {
						String nrhArea = numberPlanDao.getNRH(suspensionForm.getMsisdn());// in array
						String[] stringList = nrhArea.split(",");
						String original_area = null;
						// String original_op = null;
						if (stringList.length == 2) {
							original_area = stringList[0];
							// original_op = stringList[1];

						}
						int mch_type = numberPlanDao.getMCHTypeByArea(original_area);
						if (mch_type != 0) {
							BillingResolution billingResolution = new BillingResolution();
							billingResolution.setMsisdn(suspensionForm.getMsisdn());
							billingResolution.setReason(suspensionForm.getReason());
							billingResolution.setStatus(7);
							billingResolution.setUser_id(user.getUserId());
							billingResolution.setRequest_type(reqType);
							String requestId = null;
							if (mch_type == 1) {
								requestId = portMeRepository.getSynReqeustId(user.getOp_id());
								billingResolution.setRequestId(requestId);
							} else {
								billingResolution.setRequestId(suspensionForm.getTransactionId());
							}
							int success = billingResolutionDao.updateBillingResolution(billingResolution, sessionId);
							if (success != 0) {
								String messageSenderTelco = ReadConfigFile.getProperties()
										.getProperty("MessageSenderTelco-ZOOM");
								String xml = null;
								if (mch_type == 1) {
									billingResolution.setTransactionId(suspensionForm.getTransactionId());
									xml = new NPOUtils().convertJsonIntoInitSuspensionCancel(billingResolution,
											messageSenderTelco, mch_type, requestId, area);
								} else {
									String transactionId = suspensionForm.getTransactionId();
									xml = new NPOUtils().convertJsonIntoInitSuspensionCancel(billingResolution,
											messageSenderTelco, mch_type, transactionId, area);
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
							}
						}
					}
				}
			}
		} catch (Exception e) {
			return null;
		}
		return response;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@PostMapping("/api/suspension-dno-ack")
	public ResponseEntity<?> suspensionBillACKRequest(@RequestBody Map<String, Object> payload) {
		List<String> checkedIds = (List<String>) payload.get("checkedIds");
		int selectReasonId = Integer.parseInt((String) payload.get("selectReason"));
		String sessionId = Long.toString(System.currentTimeMillis());
		_logger.info("UID = [" + sessionId + "], Getting suspension donor bill ack request.");
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		String reqType = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
		ResponseEntity response = null;
		try {
			for (String transId : checkedIds) {
				BillingResolution suspensionForm = billingResolutionDao
						.getBillingResolutionByTransactionIdAndRequestType(transId, reqType);
				if (suspensionForm != null) {
					/* fetch for area to get mch type zone 1 or zone 2 */
					BillingConstant billingReasonDetails = billingConstantService.findById(selectReasonId);
					String area = numberPlanDao.getArea(suspensionForm.getMsisdn());
					if (area != "NA") {
						String nrhArea = numberPlanDao.getNRH(suspensionForm.getMsisdn());// in array
						String[] stringList = nrhArea.split(",");
						String original_area = null;
						// String original_op = null;
						if (stringList.length == 2) {
							original_area = stringList[0];
							// original_op = stringList[1];

						}
						int mch_type = numberPlanDao.getMCHTypeByArea(original_area);
						if (mch_type != 0) {
							BillingResolution billingResolution = new BillingResolution();
							billingResolution.setBill_no(suspensionForm.getBill_no());
							billingResolution.setAcc_no(suspensionForm.getAcc_no());
							billingResolution.setMsisdn(suspensionForm.getMsisdn());
							billingResolution.setStatus(3);
							billingResolution.setUser_id(user.getUserId());
							billingResolution.setRequest_type(reqType);
							String requestId = null;
							if (mch_type == 1) {
								billingResolution.setReason(billingReasonDetails.getZ1Code());
								requestId = portMeRepository.getSynReqeustId(user.getOp_id());
								billingResolution.setRequestId(requestId);
							} else {
								billingResolution.setReason(billingReasonDetails.getZ2Code());
								billingResolution.setRequestId(suspensionForm.getTransactionId());
							}
							int success = billingResolutionDao.updateSusDnoACK(billingResolution, sessionId);
							if (success != 0) {
								String messageSenderTelco = ReadConfigFile.getProperties()
										.getProperty("MessageSenderTelco-ZOOM");
								String xml = null;
								if (mch_type == 1) {
									billingResolution.setTransactionId(suspensionForm.getTransactionId());
									xml = new NPOUtils().convertJsonSusDONORACK(billingResolution, messageSenderTelco,
											mch_type, requestId, area, billingReasonDetails.getZ1Code());
								} else {
									String transactionId = suspensionForm.getTransactionId();
									xml = new NPOUtils().convertJsonSusDONORACK(billingResolution, messageSenderTelco,
											mch_type, transactionId, area, billingReasonDetails.getZ2Code());
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
							}
						}
					}
				}
			}
		} catch (Exception e) {
			return null;
		}
		return response;
	}

	@PostMapping("/api/suspension-dno-reack")
	public ResponseEntity<?> suspensionBillReACKRequest(@RequestBody Map<String, Object> payload) {
		List<String> checkedIds = (List<String>) payload.get("checkedIds");
		int selectReasonId = Integer.parseInt((String) payload.get("selectReason"));
		String sessionId = Long.toString(System.currentTimeMillis());
		_logger.info("UID = [" + sessionId + "], Getting suspension donor bill ack request.");
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		String reqType = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
		ResponseEntity response = null;
		try {
			for (String transId : checkedIds) {
				BillingResolution suspensionForm = billingResolutionDao
						.getBillingResolutionByTransactionIdAndRequestType(transId, reqType);
				if (suspensionForm != null) {
					/* fetch for area to get mch type zone 1 or zone 2 */
					BillingConstant billingReasonDetails = billingConstantService.findById(selectReasonId);
					String area = numberPlanDao.getArea(suspensionForm.getMsisdn());
					if (area != "NA") {
						String nrhArea = numberPlanDao.getNRH(suspensionForm.getMsisdn());// in array
						String[] stringList = nrhArea.split(",");
						String original_area = null;
						// String original_op = null;
						if (stringList.length == 2) {
							original_area = stringList[0];
							// original_op = stringList[1];

						}
						int mch_type = numberPlanDao.getMCHTypeByArea(original_area);
						if (mch_type != 0) {
							BillingResolution billingResolution = new BillingResolution();
							billingResolution.setBill_no(suspensionForm.getBill_no());
							billingResolution.setAcc_no(suspensionForm.getAcc_no());
							billingResolution.setMsisdn(suspensionForm.getMsisdn());
							billingResolution.setStatus(5);
							billingResolution.setUser_id(user.getUserId());
							billingResolution.setRequest_type(reqType);
							String requestId = null;
							if (mch_type == 1) {
								billingResolution.setReason(billingReasonDetails.getZ1Code());
								requestId = portMeRepository.getSynReqeustId(user.getOp_id());
								billingResolution.setRequestId(requestId);
							} else {
								billingResolution.setReason(billingReasonDetails.getZ2Code());
								billingResolution.setRequestId(suspensionForm.getTransactionId());
							}
							int success = billingResolutionDao.updateSusDnoReACK(billingResolution, sessionId);
							if (success != 0) {
								String messageSenderTelco = ReadConfigFile.getProperties()
										.getProperty("MessageSenderTelco-ZOOM");
								String xml = null;
								if (mch_type == 1) {
									billingResolution.setTransactionId(suspensionForm.getTransactionId());
									xml = new NPOUtils().convertJsonSusDNOReACK(billingResolution, messageSenderTelco,
											mch_type, requestId, area, billingReasonDetails.getZ1Code());
								} else {
									String transactionId = suspensionForm.getTransactionId();
									xml = new NPOUtils().convertJsonSusDNOReACK(billingResolution, messageSenderTelco,
											mch_type, transactionId, area, billingReasonDetails.getZ2Code());
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
							}
						}
					}
				}
			}
		} catch (Exception e) {
			return null;
		}
		return response;
	}

	@PostMapping("/api/suspension-rno-confirmation")
	public ResponseEntity<?> suspensionRecipientBillACKRequest(@RequestBody Map<String, Object> payload) {
		@SuppressWarnings("unchecked")
		List<String> checkedIds = (List<String>) payload.get("checkedIds");
		int selectReasonId = Integer.parseInt((String) payload.get("selectReason"));
		String sessionId = Long.toString(System.currentTimeMillis());
		_logger.info("UID = [" + sessionId + "], Getting suspension recipient bill ack request.");
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		String reqType = ReadConfigFile.getProperties().getProperty("SUS_RECIPIENT_IN");
		ResponseEntity response = null;
		try {
			for (String transId : checkedIds) {
				BillingResolution suspensionForm = billingResolutionDao
						.getBillingResolutionByTransactionIdAndRequestType(transId, reqType);
				if (suspensionForm != null) {
					/* fetch for area to get mch type zone 1 or zone 2 */
					BillingConstant billingReasonDetails = billingConstantService.findById(selectReasonId);
					String area = numberPlanDao.getArea(suspensionForm.getMsisdn());
					if (area != "NA") {
						String nrhArea = numberPlanDao.getNRH(suspensionForm.getMsisdn());// in array
						String[] stringList = nrhArea.split(",");
						String original_area = null;
						// String original_op = null;
						if (stringList.length == 2) {
							original_area = stringList[0];
							// original_op = stringList[1];

						}
						int mch_type = numberPlanDao.getMCHTypeByArea(original_area);
						if (mch_type != 0) {
							BillingResolution billingResolution = new BillingResolution();
							billingResolution.setBill_no(suspensionForm.getBill_no());
							billingResolution.setAcc_no(suspensionForm.getAcc_no());
							billingResolution.setMsisdn(suspensionForm.getMsisdn());
							billingResolution.setStatus(2);
							billingResolution.setUser_id(user.getUserId());
							billingResolution.setRequest_type(reqType);
							String requestId = null;
							if (mch_type == 1) {
								billingResolution.setReason(billingReasonDetails.getZ1Code());
								requestId = portMeRepository.getSynReqeustId(user.getOp_id());
								billingResolution.setRequestId(requestId);
							} else {
								billingResolution.setReason(billingReasonDetails.getZ2Code());
								billingResolution.setRequestId(transId);
							}
							int success = billingResolutionDao.updateSusRnoCon(billingResolution, sessionId);
							if (success != 0) {
								String messageSenderTelco = ReadConfigFile.getProperties()
										.getProperty("MessageSenderTelco-ZOOM");
								String xml = null;
								if (mch_type == 1) {
									billingResolution.setTransactionId(transId);
									xml = new NPOUtils().convertRecipietPaymentConfirmation(billingResolution,
											messageSenderTelco, mch_type, requestId, area,
											billingReasonDetails.getZ2Code());
								} else {
									String donorLSAID = numberPlanDao.getDonorLSAID(suspensionForm.getMsisdn());
									if (donorLSAID == null) {
										donorLSAID = area;
									}
									String transactionId = transId;// portMeService.getTransactionId(messageSenderTelco,
																	// area);
									xml = new NPOUtils().convertRecipietPaymentConfirmation(billingResolution,
											messageSenderTelco, mch_type, transactionId, area,
											billingReasonDetails.getZ2Code());
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
							}
						}
					}
				}
			}
		} catch (Exception e) {
			return null;
		}
		return response;
	}

	@PostMapping("/api/suspension-rno-reconnection")
	public ResponseEntity<?> sendBarringReconnection(@RequestBody Map<String, Object> payload) {
		@SuppressWarnings("unchecked")
		List<String> checkedIds = (List<String>) payload.get("checkedIds");
		int selectReasonId = Integer.parseInt((String) payload.get("selectReason"));
		String sessionId = Long.toString(System.currentTimeMillis());
		_logger.info("UID = [" + sessionId + "], Getting suspension recipient bill ack request.");
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		String reqType = ReadConfigFile.getProperties().getProperty("SUS_RECIPIENT_IN");
		ResponseEntity response = null;
		try {
			for (String transId : checkedIds) {
				BillingResolution suspensionForm = billingResolutionDao
						.getBillingResolutionByTransactionIdAndRequestType(transId, reqType);
				if (suspensionForm != null) {
					String area = numberPlanDao.getArea(suspensionForm.getMsisdn());
					if (area != "NA") {
						String nrhArea = numberPlanDao.getNRH(suspensionForm.getMsisdn());// in array
						String[] stringList = nrhArea.split(",");
						String original_area = null;
						// String original_op = null;
						if (stringList.length == 2) {
							original_area = stringList[0];
							// original_op = stringList[1];

						}
						int mch_type = numberPlanDao.getMCHTypeByArea(original_area);
						if (mch_type != 0) {
							BillingConstant billingReasonDetails = billingConstantService.findById(selectReasonId);
							BillingResolution billingResolution = new BillingResolution();
							billingResolution.setBill_no(suspensionForm.getBill_no());
							billingResolution.setAcc_no(suspensionForm.getAcc_no());
							billingResolution.setMsisdn(suspensionForm.getMsisdn());
							billingResolution.setStatus(4);
							billingResolution.setUser_id(user.getUserId());
							billingResolution.setRequest_type(reqType);
							String requestId = null;
							if (mch_type == 1) {
								billingResolution.setReason(billingReasonDetails.getZ1Code());
								requestId = portMeRepository.getSynReqeustId(user.getOp_id());
								billingResolution.setRequestId(requestId);
							} else {
								billingResolution.setReason(billingReasonDetails.getZ2Code());
								billingResolution.setRequestId(transId);
							}
							int success = billingResolutionDao.updateSusRnoReCon(billingResolution, sessionId);
							if (success != 0) {
								String messageSenderTelco = ReadConfigFile.getProperties()
										.getProperty("MessageSenderTelco-ZOOM");
								String xml = null;
								if (mch_type == 1) {
									billingResolution.setTransactionId(transId);
									xml = new NPOUtils().convertJsonIntoISuspensionReconnection(billingResolution,
											messageSenderTelco, mch_type, requestId, area,
											billingReasonDetails.getZ1Code());
								} else {
									String donorLSAID = numberPlanDao.getDonorLSAID(suspensionForm.getMsisdn());
									if (donorLSAID == null) {
										donorLSAID = area;
									}
									String transactionId = transId;// portMeService.getTransactionId(messageSenderTelco,
																	// area);
									xml = new NPOUtils().convertJsonIntoISuspensionReconnection(billingResolution,
											messageSenderTelco, mch_type, transactionId, area,
											billingReasonDetails.getZ2Code());
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
							}
						}
					}
				}
			}
		} catch (Exception e) {
			return null;
		}
		return response;
	}
}