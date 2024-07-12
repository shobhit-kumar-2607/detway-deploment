package com.megthink.gateway.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.megthink.gateway.api.response.PortMeAPIResponse;
import com.megthink.gateway.dao.NumberPlanDao;
import com.megthink.gateway.dao.PortMeDao;
import com.megthink.gateway.model.PortMe;
import com.megthink.gateway.model.PortMeTransactionDetails;
import com.megthink.gateway.model.SubscriberArrType;
import com.megthink.gateway.model.User;
import com.megthink.gateway.producer.JmsProducer;
import com.megthink.gateway.repository.PortMeRepository;
import com.megthink.gateway.service.CustomerDataService;
import com.megthink.gateway.service.FileStorageService;
import com.megthink.gateway.service.PersonCustomerService;
import com.megthink.gateway.service.PortMeService;
import com.megthink.gateway.service.PortMeTransactionDetailsService;
import com.megthink.gateway.service.SubscriberArrTypeService;
import com.megthink.gateway.service.UserService;
import com.megthink.gateway.utils.APIConst;
import com.megthink.gateway.utils.NPOUtils;
import com.megthink.gateway.utils.PortMeUtils;
import com.megthink.gateway.utils.ReadConfigFile;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class PortMeRestApi {

	private static final Logger _logger = LoggerFactory.getLogger(PortMeRestApi.class);

	Timestamp timestamp = new Timestamp(System.currentTimeMillis());

	@Autowired
	private UserService userService;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private PortMeService portMeService;
	@Autowired
	private CustomerDataService customerDataService;
	@Autowired
	private PersonCustomerService personCustomerSerive;
	@Autowired
	private PortMeTransactionDetailsService portMeTransactionService;
	@Autowired
	private PortMeRepository portMeRepository;
	@Autowired
	private FileStorageService fileStorageService;
	@Autowired
	private SubscriberArrTypeService subscriberArrTypeService;
	@Autowired
	private JmsProducer jmsProducer;
	@Autowired
	private PortMeDao portMeDao;
	@Autowired
	private NumberPlanDao numberPlanDao;

	@PostMapping("/api/initportrequest")
	public ResponseEntity<?> schedulePortMe(@RequestParam("portme") String portme,
			@RequestParam(name = "bulkUpload", required = false) MultipartFile bulkUpload,
			@RequestParam(name = "docFile", required = false) MultipartFile docFile) {
		String sessionId = Long.toString(System.currentTimeMillis());
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		PortMe portMeDetails = null;
		_logger.info("[sessionId=" + sessionId
				+ "]: PortMeRestApi.schedulePortMe() - portin request process start with timestamp:["
				+ new Timestamp(System.currentTimeMillis()) + "]");
		ResponseEntity response = null;
		int current_status = 0;
		String requestId = null;
		PortMeAPIResponse errorBean = new PortMeAPIResponse();
		PortMeTransactionDetails portMeTransaction = new PortMeTransactionDetails();
		String msg = null;
		String reqType = ReadConfigFile.getProperties().getProperty("PORTME_IN");
		String source = ReadConfigFile.getProperties().getProperty("PORTME_SOURCE");
		Boolean isFileProcess = true;
		int mch_type = 0;
		String referenceId = null;
		try {
			String binaryFile = null;
			portMeDetails = objectMapper.readValue(portme, PortMe.class);
			if (bulkUpload != null) {
				try {
					String hlr = portMeDetails.getSubscriberArrType().get(0).getHlr();
					String alternateNumber = portMeDetails.getSubscriberArrType().get(0).getDummyMSISDN();
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeRestApi.schedulePortMe() - trying to convert msisdn file into list with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					List<SubscriberArrType> listOfPlan = subscriberArrTypeService.processBulkFile(bulkUpload, hlr,
							alternateNumber);
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeRestApi.schedulePortMe() -  converted msisdn file into list with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					portMeDetails.setSubscriberArrType(listOfPlan);
				} catch (Exception e) {
					isFileProcess = false;
				}
			}
			if (isFileProcess) {
				if (portMeDetails == null) {
					errorBean.setResponseCode(APIConst.successCode1);
					errorBean.setResponseMessage(APIConst.successMsg1);
					msg = PortMeUtils.generateJsonResponse(errorBean, "ERROR");
					response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.BAD_REQUEST);
				} else {
					_logger.info("[sessionId=" + sessionId
							+ "]: PortMeRestApi.schedulePortMe() -  start portin request with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					String msisdns = null;
					if (portMeDetails.getSubscriberArrType().size() > 0) {
						msisdns = portMeDetails.getSubscriberArrType().get(0).getMsisdn();
					} else {
						msisdns = portMeDetails.getSubscriberSequence().getSubscriberNumber();
					}
					String lastArea = numberPlanDao.getDonorLSAID(msisdns);
					if (lastArea == null) {
						lastArea = numberPlanDao.getArea(msisdns);
					}

					String nrhArea = numberPlanDao.getNRH(msisdns);// in array
					String original_area = null;
					String original_op = null;
					if (nrhArea != null) {
						String[] strList = nrhArea.split(",");
						if (strList.length == 2) {
							original_area = strList[0];
							original_op = strList[1];

						}
						portMeDetails.setDno(original_op);
						if (nrhArea != null) {
							mch_type = numberPlanDao.getMCHTypeByArea(original_area);
							if (mch_type == 0) {
								errorBean.setResponseCode(APIConst.loggerCode1);
								errorBean.setResponseMessage(APIConst.loggerMsg9);
								msg = PortMeUtils.generateJsonResponse(errorBean, "ERROR");
								_logger.debug("[sessionId=" + sessionId
										+ "]: PortMeRestApi.schedulePortMe() - Number plan doesn't mapping lsa into lsa_zone_mapping with timestamp:["
										+ new Timestamp(System.currentTimeMillis()) + "]");
								response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
								return response;
							}
						}
					} else {
						errorBean.setResponseCode(APIConst.loggerCode1);
						errorBean.setResponseMessage(APIConst.loggerMsg9);
						msg = PortMeUtils.generateJsonResponse(errorBean, "ERROR");
						_logger.debug("[sessionId=" + sessionId
								+ "]: PortMeRestApi.schedulePortMe() - Number plan doesn't exist with timestamp:["
								+ new Timestamp(System.currentTimeMillis()) + "]");
						response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
						return response;
					}
					if (mch_type == 1) {
						requestId = portMeRepository.getSynReqeustId(user.getOp_id());
						referenceId = portMeService.getReferenceId(portMeDetails.getRno(), portMeDetails.getDno(),
								portMeDetails.getDno(), "PORT");
					} else {
						String area = numberPlanDao.getArea(msisdns);
						String messageSenderTelco = ReadConfigFile.getProperties()
								.getProperty("MessageSenderTelco-ZOOM");
						referenceId = portMeService.getTransactionId(messageSenderTelco, area);
						requestId = referenceId;
						portMeDetails.setLast_area(lastArea);
					}

					if (docFile != null) {
						_logger.debug("[sessionId=" + sessionId
								+ "]: PortMeRestApi.schedulePortMe() - trying to upload document file with timestamp:["
								+ new Timestamp(System.currentTimeMillis()) + "]");
						fileStorageService.storeFile(docFile, referenceId + "IN.pdf");
						binaryFile = Base64.getEncoder().encodeToString(docFile.getBytes());
						_logger.debug("[sessionId=" + sessionId
								+ "]: PortMeRestApi.schedulePortMe() - uploaded upload document file with timestamp:["
								+ new Timestamp(System.currentTimeMillis()) + "]");
					}

					current_status = 1;
					portMeDetails.setUserId(user.getUserId());
					portMeDetails.setRequestId(requestId);
					portMeDetails.setReferenceId(referenceId);
					portMeDetails.setSource(source);
					portMeDetails.setRequest_type(reqType);
					portMeTransaction.setReferenceId(referenceId);
					portMeTransaction.setStatus(current_status);
					portMeTransaction.setRequestType(reqType);
					if (portMeDetails.getSubscriberArrType().size() > 0) {
						_logger.debug("[sessionId=" + sessionId
								+ "]: PortMeRestApi.schedulePortMe() -  trying to store into transaction table with timestamp:["
								+ new Timestamp(System.currentTimeMillis()) + "]");
						portMeTransactionService.savePortMeTransactionDetails(portMeTransaction,
								portMeDetails.getSubscriberArrType());
						_logger.debug("[sessionId=" + sessionId
								+ "]: PortMeRestApi.schedulePortMe() -  PortMe Corporate Transactiondetails insterted into db with reqeustId with timestamp:["
								+ new Timestamp(System.currentTimeMillis()) + "]");
					} else {
						portMeTransaction.setMsisdn(portMeDetails.getSubscriberSequence().getSubscriberNumber());
						portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
						_logger.debug("[sessionId=" + sessionId
								+ "]: PortMeRestApi.schedulePortMe() -  PortMe personal Transactiondetails insterted into db with reqeustId with timestamp:["
								+ new Timestamp(System.currentTimeMillis()) + "]");
					}
					portMeDetails.setStatus(current_status);
					portMeDetails.setCustomerRequestTime(timestamp.toString());
					portMeDetails.setOrderDate(timestamp.toString());
					portMeDetails.setOriginalCarrier(portMeDetails.getDno());
					portMeDetails.setLast_area(lastArea);
					portMeDetails.setMch(mch_type);
					portMeDetails.setTimeStamp(timestamp.toString());
					portMeDetails.setOriginal_area(original_area);
					portMeDetails.setOriginal_op(original_op);
					PortMe portMe = portMeService.savePortMe(portMeDetails);
					if (portMe.getPortId() != 0) {
						_logger.debug("[sessionId=" + sessionId
								+ "]: PortMeRestApi.schedulePortMe() -  successfully saved port me details with timestamp:["
								+ new Timestamp(System.currentTimeMillis()) + "]");
						if (portMeDetails.getSubscriberArrType().size() > 0) {
							/* insert MSISDN for corporate number */
							subscriberArrTypeService.saveMt(portMe.getPortId(), portMeDetails.getSubscriberArrType(),
									reqType, current_status);
						} else {
							/* insert MSISDN for personal number */
							subscriberArrTypeService.savePortMT(portMe.getPortId(),
									portMeDetails.getSubscriberSequence().getSubscriberNumber(), reqType,
									current_status, portMeDetails.getHlr(), portMeDetails.getDummyMSISDN());
						}
						if (portMeDetails.getCustomerData() != null) {
							portMeDetails.getCustomerData().setPortId(portMe.getPortId());
							customerDataService.saveCustomerData(portMeDetails.getCustomerData());
						} else {
							portMeDetails.getPersonCustomer().setPortId(portMe.getPortId());
							personCustomerSerive.savePersonCustomer(portMeDetails.getPersonCustomer());
						}
						/* going to validate MSISDN for corporate number */
						if (portMeDetails.getSubscriberArrType().size() > 0) {
							for (SubscriberArrType subscriber : portMeDetails.getSubscriberArrType()) {
								_logger.debug("[sessionId=" + sessionId
										+ "]: PortMeRestApi.schedulePortMe() -  Going to validate corporate msisdn ["
										+ subscriber.getMsisdn() + "] with timestamp:["
										+ new Timestamp(System.currentTimeMillis()) + "]");
								String msisdnValidate = portMeRepository.validateMSISDN(subscriber.getMsisdn());
								String[] validateMsisdnRsp = msisdnValidate.split("[,]", 0);
								_logger.debug("[sessionId=" + sessionId
										+ "]: PortMeRestApi.schedulePortMe() -  successfully get DNO info : "
										+ msisdnValidate + " for msisdn[" + subscriber.getMsisdn()
										+ "]  with timestamp:[" + new Timestamp(System.currentTimeMillis()) + "]");
								/*
								 * 1-> check for msisdn, donor(and rno, lrn let's do later, operatorid should be
								 * donor) from tbl_master_np if not exist then msisdn_range
								 */
								/*
								 * 2-> select rn from msisdn_range for rno=op_id and area = area(opcode=airtel,
								 * area = dl) jis me port hoga uska area
								 */
								if (validateMsisdnRsp[1].equals(portMeDetails.getRno())
										&& validateMsisdnRsp[4].equals(portMeDetails.getArea())) {
									subscriberArrTypeService.updatePortMtStatsByMsisdn(current_status,
											subscriber.getMsisdn(), reqType, 400);
									portMeTransaction = new PortMeTransactionDetails();
									portMeTransaction.setReferenceId(referenceId);
									portMeTransaction.setStatus(19);
									portMeTransaction.setRequestType(reqType);
									portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
									errorBean.setResponseCode(APIConst.loggerCode1);
									errorBean.setResponseMessage(APIConst.loggerMsg19);
									msg = PortMeUtils.generateJsonResponse(errorBean, "ERROR");
									_logger.debug("[sessionId=" + sessionId
											+ "]: PortMeRestApi.schedulePortMe() - Number plan" + subscriber.getMsisdn()
											+ " porting from same operator to same lsa not allowed with timestamp:["
											+ new Timestamp(System.currentTimeMillis()) + "]");
									response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
								} else {
									/* checking MSISDN porting calculation days with porting date */
									Boolean portin_days = false;
									if (validateMsisdnRsp[3].equals("null")) {
										portin_days = true;
									} else {
										int no_of_day = portMeRepository.getNoOfDaysByMSISDN(subscriber.getMsisdn());
										String days = ReadConfigFile.getProperties().getProperty("PORTINDAYS");
										if (Integer.parseInt(days) >= no_of_day) {
											portin_days = true;
										} else {
											portin_days = false;
										}
									}
									if (portin_days == true) {
										String validateRNO = portMeRepository.validateRNO(portMeDetails.getRno(),
												portMeDetails.getArea());
										String[] validateRNORsp = validateRNO.split("[,]", 0);
										if (validateRNORsp[1].equals(portMeDetails.getRn())) {
											current_status = 2;
											portMeTransaction = new PortMeTransactionDetails();
											portMeTransaction.setReferenceId(referenceId);
											portMeTransaction.setStatus(current_status);
											portMeTransaction.setRequestType(reqType);
											portMeTransaction.setMsisdn(subscriber.getMsisdn());
											portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
											subscriberArrTypeService.updatePortMtStatsByMsisdn(current_status,
													subscriber.getMsisdn(), reqType, 0);
											_logger.debug("[sessionId=" + sessionId
													+ "]: PortMeRestApi.schedulePortMe() - Number plan"
													+ subscriber.getMsisdn() + " exist in our table with timestamp:["
													+ new Timestamp(System.currentTimeMillis()) + "]");
										} else {
											portMeTransaction = new PortMeTransactionDetails();
											portMeTransaction.setReferenceId(referenceId);
											portMeTransaction.setStatus(19);
											portMeTransaction.setRequestType(reqType);
											portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
											subscriberArrTypeService.updatePortMtStatsByMsisdn(current_status,
													subscriber.getMsisdn(), reqType, 400);
											errorBean.setResponseCode(APIConst.loggerCode1);
											errorBean.setResponseMessage(APIConst.loggerMsg7);
											msg = PortMeUtils.generateJsonResponse(errorBean, "ERROR");
											_logger.debug("[sessionId=" + sessionId
													+ "]: PortMeRestApi.schedulePortMe() - Number plan"
													+ subscriber.getMsisdn()
													+ " route info is not correct with timestamp:["
													+ new Timestamp(System.currentTimeMillis()) + "]");
											response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
										}
									} else {
										subscriberArrTypeService.updatePortMtStatusByMsisdn(current_status,
												subscriber.getMsisdn(), reqType, 400);
										portMeTransaction = new PortMeTransactionDetails();
										portMeTransaction.setReferenceId(referenceId);
										portMeTransaction.setStatus(19);
										portMeTransaction.setRequestType(reqType);
										portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
										subscriberArrTypeService.updatePortMtStatsByMsisdn(current_status,
												subscriber.getMsisdn(), reqType, 400);
										errorBean.setResponseCode(APIConst.loggerCode1);
										errorBean.setResponseMessage(APIConst.loggerMsg8);
										msg = PortMeUtils.generateJsonResponse(errorBean, "ERROR");
										_logger.debug("[sessionId=" + sessionId
												+ "]: PortMeRestApi.schedulePortMe() - Number plan"
												+ subscriber.getMsisdn() + " does not allow to port with timestamp:["
												+ new Timestamp(System.currentTimeMillis()) + "]");
										response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
									}
								}
							}
						} else {
							/* going to validate MSISDN for personal number */
							String msisdn = portMeDetails.getSubscriberSequence().getSubscriberNumber();
							_logger.debug("[sessionId=" + sessionId
									+ "]: PortMeRestApi.schedulePortMe() - tring to get DNO, RNO for personal plan "
									+ msisdn + " with timestamp:[" + new Timestamp(System.currentTimeMillis()) + "]");
							String msisdnValidate = portMeRepository.validateMSISDN(msisdn);
							_logger.debug(
									"[sessionId=" + sessionId + "]: PortMeRestApi.schedulePortMe() - got DNO, RNO ["
											+ msisdnValidate + "] for personal plan " + msisdn + " with timestamp:["
											+ new Timestamp(System.currentTimeMillis()) + "]");
							String[] validateMsisdnRsp = msisdnValidate.split("[,]", 0);
							if (validateMsisdnRsp[1].equals(portMeDetails.getRno())
									&& validateMsisdnRsp[4].equals(portMeDetails.getArea())) {
								portMeTransaction = new PortMeTransactionDetails();
								portMeTransaction.setReferenceId(referenceId);
								portMeTransaction.setStatus(19);
								portMeTransaction.setRequestType(reqType);
								portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
								subscriberArrTypeService.updatePortMtStatsByMsisdn(current_status, msisdn, reqType,
										400);
								errorBean.setResponseCode(APIConst.loggerCode1);
								errorBean.setResponseMessage(APIConst.loggerMsg19);
								msg = PortMeUtils.generateJsonResponse(errorBean, "ERROR");
								_logger.info("[sessionId=" + sessionId
										+ "]: PortMeRestApi.schedulePortMe() - Plan number [" + msisdn
										+ "] porting from same operator to same lsa not allowed with timestamp:["
										+ new Timestamp(System.currentTimeMillis()) + "]");
								response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
							} else {
								Boolean portin_days = false;
								if (validateMsisdnRsp[3] != null) {
									portin_days = true;
								} else {
									int no_of_day = portMeRepository.getNoOfDaysByMSISDN(msisdn);
									String days = ReadConfigFile.getProperties().getProperty("PORTINDAYS");
									if (Integer.parseInt(days) >= no_of_day) {
										portin_days = true;
									} else {
										portin_days = false;
									}
								}
								if (portin_days == true) {
									_logger.debug("[sessionId=" + sessionId
											+ "]: PortMeRestApi.schedulePortMe() - trying to get RN info for personal plan "
											+ msisdn + " with timestamp:[" + new Timestamp(System.currentTimeMillis())
											+ "]");
									String validateRNO = portMeRepository.validateRNO(portMeDetails.getRno(),
											portMeDetails.getArea());
									_logger.debug("[sessionId=" + sessionId
											+ "]: PortMeRestApi.schedulePortMe() - got RN info" + validateRNO
											+ " for personal plan " + msisdn + " with timestamp:["
											+ new Timestamp(System.currentTimeMillis()) + "]");
									String[] validateRNORsp = validateRNO.split("[,]", 0);
									if (validateRNORsp[1].equals(portMeDetails.getRn())) {
										current_status = 2;
										portMeTransaction = new PortMeTransactionDetails();
										portMeTransaction.setReferenceId(referenceId);
										portMeTransaction.setStatus(current_status);
										portMeTransaction.setRequestType(reqType);
										portMeTransaction.setMsisdn(msisdn);
										portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
										subscriberArrTypeService.updatePortMtStatsByMsisdn(current_status, msisdn,
												reqType, 0);
									} else {
										portMeTransaction = new PortMeTransactionDetails();
										portMeTransaction.setReferenceId(referenceId);
										portMeTransaction.setStatus(19);
										portMeTransaction.setRequestType(reqType);
										portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
										subscriberArrTypeService.updatePortMtStatsByMsisdn(current_status, msisdn,
												reqType, 400);
										errorBean.setResponseCode(APIConst.loggerCode1);
										errorBean.setResponseMessage(APIConst.loggerMsg7);
										msg = PortMeUtils.generateJsonResponse(errorBean, "ERROR");
										_logger.debug("[sessionId=" + sessionId
												+ "]: PortMeRestApi.schedulePortMe() - Plan number [" + msisdn
												+ "] route info is not correct with timestamp:["
												+ new Timestamp(System.currentTimeMillis()) + "]");
										response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
									}
								} else {
									portMeTransaction = new PortMeTransactionDetails();
									portMeTransaction.setReferenceId(referenceId);
									portMeTransaction.setStatus(19);
									portMeTransaction.setRequestType(reqType);
									portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
									subscriberArrTypeService.updatePortMtStatsByMsisdn(current_status, msisdn, reqType,
											400);
									errorBean.setResponseCode(APIConst.loggerCode1);
									errorBean.setResponseMessage(APIConst.loggerMsg8);
									msg = PortMeUtils.generateJsonResponse(errorBean, "ERROR");

									_logger.info("[sessionId=" + sessionId
											+ "]: PortMeRestApi.schedulePortMe() - Plan number [" + msisdn
											+ "] does not allow to port with timestamp:["
											+ new Timestamp(System.currentTimeMillis()) + "]");
									response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
								}

							}
						}
						if (current_status == 2) {
							List<SubscriberArrType> subArr = subscriberArrTypeService
									.findSubArrByPortIdAndResultCode(portMe.getPortId(), 0);
							if (subArr.size() > 0) {
								_logger.info("[sessionId=" + sessionId
										+ "]: PortMeRestApi.schedulePortMe() - going to convert portme request into xml with timestamp:["
										+ new Timestamp(System.currentTimeMillis()) + "]");

								String xml = null;
								if (mch_type == 1) {
									xml = new NPOUtils().convertJsonIntoInitPortRequest(portMeDetails, binaryFile,
											referenceId, requestId);
								} else {
									xml = new NPOUtils().convertJsonIntoInitPortRequestZone2(portMeDetails, binaryFile,
											referenceId);
								}
								_logger.info("[sessionId=" + sessionId
										+ "]: PortMeRestApi.schedulePortMe() - converted portme request into xml :["
										+ xml + "]");
								int returnvalue = jmsProducer.sendMessageToInQueue(xml, sessionId, mch_type);
								if (returnvalue == 1) {
									_logger.info("[sessionId=" + sessionId
											+ "]: PortMeRestApi.schedulePortMe() - sent portin request in jms queue");
									current_status = 3;
									portMeTransaction = new PortMeTransactionDetails();
									portMeTransaction.setReferenceId(referenceId);
									portMeTransaction.setStatus(current_status);
									portMeTransaction.setRequestType(reqType);
									if (subArr.size() > 0) {
										portMeTransactionService.savePortMeTransactionDetails(portMeTransaction,
												subArr);
										for (SubscriberArrType list : subArr) {
											subscriberArrTypeService.updatePortMtStatsByMsisdn(current_status,
													list.getMsisdn(), reqType, 0);
										}
									}
									_logger.info("[sessionId=" + sessionId
											+ "]: PortMeRestApi.schedulePortMe() - Successfully Received portin request");
									errorBean.setId(portMe.getPortId());
									errorBean.setResponseCode(APIConst.successCode);
									errorBean.setResponseMessage(APIConst.successMsg);
									msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
									response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
								} else {
									_logger.info("[sessionId=" + sessionId
											+ "]: PortMeRestApi.schedulePortMe() - Successfully saved data but not able to send queue");
									errorBean.setId(portMe.getPortId());
									errorBean.setResponseCode(APIConst.successCode2);
									errorBean.setResponseMessage(APIConst.successMsg2);
									msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
									response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
								}
							} else {
								_logger.info("[sessionId=" + sessionId
										+ "]: PortMeRestApi.schedulePortMe() - Number plan data is null with requestId : "
										+ requestId);
								errorBean.setId(portMe.getPortId());
								errorBean.setResponseCode(APIConst.successCode3);
								errorBean.setResponseMessage(APIConst.successMsg3);
								msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
								response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
							}
						}
					} else {
						portMeTransaction = new PortMeTransactionDetails();
						portMeTransaction.setReferenceId(referenceId);
						portMeTransaction.setStatus(19);
						portMeTransaction.setRequestType(reqType);
						portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
						_logger.info("[sessionId=" + sessionId
								+ "]: PortMeRestApi.schedulePortMe() - PortMe details unsaved with with requestId : "
								+ requestId);
						errorBean.setResponseCode(APIConst.successCode1);
						errorBean.setResponseMessage(APIConst.successMsg1);
						msg = PortMeUtils.generateJsonResponse(errorBean, "ERROR");
						response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
					}
				}
			} else {
				errorBean.setResponseCode(APIConst.successCode102);
				errorBean.setResponseMessage(APIConst.successMsg102);
				msg = PortMeUtils.generateJsonResponse(errorBean, "ERROR");
				response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.BAD_REQUEST);
			}

		} catch (Exception e) {
			portMeTransaction = new PortMeTransactionDetails();
			portMeTransaction.setReferenceId(referenceId);
			portMeTransaction.setStatus(19);
			portMeTransaction.setRequestType(reqType);
			portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
			errorBean.setResponseCode(APIConst.successCode1);
			errorBean.setResponseMessage(APIConst.successMsg1);
			msg = PortMeUtils.generateJsonResponse(errorBean, "ERROR");
			_logger.error("[sessionId=" + sessionId
					+ "]: PortMeRestApi.schedulePortMe() - Something is wrong with the system with requestId : "
					+ requestId + " " + e.getMessage());
			// e.printStackTrace();
			response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
		}
		if (current_status != 0) {
			// going to update current status in port_tx table
			portMeService.updatePortMeStatus(current_status, referenceId, user.getUserId(), mch_type, requestId);
		}
		return response;
	}

	@PostMapping("/api/connectionanswered")
	public ResponseEntity<?> sendConnectionAnswered(@RequestBody Map<String, List<String>> payload) {
		List<String> checkedIds = payload.get("checkedIds");
		// Process the checked IDs as needed
		System.out.println("Received checked IDs: " + checkedIds);

		String sessionId = Long.toString(System.currentTimeMillis());
		_logger.info("received portme connection answer request");
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		ResponseEntity response = null;
		PortMeAPIResponse errorBean = new PortMeAPIResponse();
		String msg = null;
		int current_status = 0;
		PortMeTransactionDetails portMeTransaction = new PortMeTransactionDetails();
		String reqType = ReadConfigFile.getProperties().getProperty("PORTME_IN");
		String referenceId = null;
		String requestId = null;
		List<PortMe> listOfZ1 = new ArrayList<PortMe>();
		List<PortMe> listOfZ2 = new ArrayList<PortMe>();
		try {
			// portMeDetails = objectMapper.readValue(portmeanswer, PortMe.class);
			for (String refId : checkedIds) {
				PortMe portMe = portMeService.getListPortMeByReferenceId(reqType, refId);
				if (portMe.getMch() == 1) {
					listOfZ1.add(portMe);
				} else {
					listOfZ2.add(portMe);
				}
			}
			if (listOfZ1.size() > 0) {
				requestId = portMeRepository.getSynReqeustId(user.getOp_id());
				current_status = 15;
				List<String> listOfMsisdn = new ArrayList<String>();
				for (PortMe item : listOfZ1) {
					List<String> list = portMeDao.getListOfMSISDN(item.getPortId());
					listOfMsisdn.addAll(list);
					subscriberArrTypeService.updatePortMtByPortId(current_status, item.getPortId());
					portMeService.updatePortMeStats(current_status, requestId, user.getUserId(), item.getReferenceId(),
							reqType);
				}
				_logger.info(
						"trying convert portme connection answer request into xml with requestId : " + referenceId);
				int mch_type = 1;
				String xml = null;
				// portMeDetails.setRequestId(referenceId);
				xml = new NPOUtils().convertJsonIntoZ1SCAxml(listOfMsisdn, requestId);
				_logger.info("convert portme connection answer request into xml with requestId:" + requestId + xml);
				int returnvalue = jmsProducer.sendMessageToInQueue(xml, sessionId, mch_type);
				if (returnvalue == 1) {
					_logger.info("sent portme connection answer request in jms queue with requestId-" + requestId);
					_logger.info("Successfully Received portmeconnectionanswer request with requestId:" + requestId);
					errorBean.setResponseCode(APIConst.successCode);
					errorBean.setResponseMessage(APIConst.successMsg);
					msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
					response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
				} else {
					_logger.info("Portme connection answer request saved successfully but not able to send queue");
					errorBean.setResponseCode(APIConst.successCode);
					errorBean.setResponseMessage(APIConst.successMsg2);
					msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
					response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
				}
			}
			if (listOfZ2.size() > 0) {
				current_status = 15;
				for (PortMe item : listOfZ2) {
					List<String> listOfMsisdn = portMeDao.getListOfMSISDN(item.getPortId());
					subscriberArrTypeService.updatePortMtByPortId(current_status, item.getPortId());
					portMeService.updatePortMeStats(current_status, item.getReferenceId(), user.getUserId(),
							item.getReferenceId(), reqType);
					_logger.info("trying convert portme connection answer request into xml with requestId : "
							+ item.getReferenceId());
					String area = numberPlanDao.getArea(listOfMsisdn.get(0));
					int mch_type = 2;
					String xml = null;
					String donorLSAID = numberPlanDao.getDonorLSAID(listOfMsisdn.get(0));
					if (donorLSAID == null) {
						donorLSAID = area;
					}
					String transactionId = item.getReferenceId();
					item.setLast_area(donorLSAID);
					String routeInfo = numberPlanDao.getRouteInfo(transactionId, reqType);
					item.setRn(routeInfo);
					xml = new NPOUtils().convertJsonIntoZ2SCAxml(item, listOfMsisdn, transactionId);
					_logger.info(
							"convert portme connection answer request into xml with requestId:" + transactionId + xml);
					int returnvalue = jmsProducer.sendMessageToInQueue(xml, sessionId, mch_type);
					if (returnvalue == 1) {
						_logger.info(
								"sent portme connection answer request in jms queue with requestId-" + transactionId);
						_logger.info(
								"Successfully Received portmeconnectionanswer request with requestId:" + transactionId);
						errorBean.setResponseCode(APIConst.successCode);
						errorBean.setResponseMessage(APIConst.successMsg);
						msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
						response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
					} else {
						_logger.info("Portme connection answer request saved successfully but not able to send queue");
						errorBean.setResponseCode(APIConst.successCode);
						errorBean.setResponseMessage(APIConst.successMsg2);
						msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
						response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
					}
				}
			}
		} catch (Exception e) {
			_logger.error("error Portme connection answer request with requestId :" + requestId + e.getMessage());
			portMeTransaction = new PortMeTransactionDetails();
			portMeTransaction.setReferenceId(referenceId);
			portMeTransaction.setStatus(19);
			portMeTransaction.setRequestType(reqType);
			portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
			errorBean.setResponseCode(APIConst.successCode1);
			errorBean.setResponseMessage(APIConst.successMsg1);
			msg = PortMeUtils.generateJsonResponse(errorBean, "ERROR");
			// e.printStackTrace();
			response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.BAD_REQUEST);
		}
		return response;
	}

	@PostMapping("/api/portapprovalresponse")
	public ResponseEntity<?> sendPortApprovalResponse(@RequestBody Map<String, Object> payload) {
		List<String> checkedIds = (List<String>) payload.get("checkedIds");
		String selectedValue = (String) payload.get("selectedValue");
		String selectReason = (String) payload.get("selectReason");
		String billingUID = (String) payload.get("billingUID");
		String instanceId = (String) payload.get("instanceId");

		String sessionId = Long.toString(System.currentTimeMillis());
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		ResponseEntity response = null;
		PortMeAPIResponse errorBean = new PortMeAPIResponse();
		PortMeTransactionDetails portMeTransaction = new PortMeTransactionDetails();
		String requestId = null;
		String referenceId = null;
		String msg = null;
		int current_status = 0;
		String reqType = ReadConfigFile.getProperties().getProperty("PORTME_OUT");
		String source = ReadConfigFile.getProperties().getProperty("PORTME_SOURCE");
		PortMe portMeDetails = null;
		int mch_type = 0;
		String area = null;
		int resultCode = 0;
		try {
			for (String refId : checkedIds) {
				portMeDetails = new PortMe();
				portMeDetails.setBillingUID1(billingUID);
				portMeDetails.setInstanceID(instanceId);

				referenceId = refId;
				portMeDetails.setSource(source);
				portMeDetails.setApproval(selectedValue);
				if (selectedValue.equals("Accept")) {
					resultCode = 0;
				} else {
					resultCode = Integer.parseInt(selectReason);
					portMeDetails.setComment(selectReason);
				}
				_logger.info("Recieved port approval request");
				portMeTransaction.setReferenceId(referenceId);
				current_status = 6;
				portMeTransaction.setStatus(current_status);
				portMeTransaction.setRequestType(reqType);
				int portId = portMeService.getPortIdByReferenceId(referenceId, reqType);
				List<SubscriberArrType> subscriberList = subscriberArrTypeService.findSubArrByPortId(portId);
				portMeDetails.getSubscriberArrType().clear();
				portMeDetails.getSubscriberArrType().addAll(subscriberList);
				if (portMeDetails.getSubscriberArrType().size() > 0) {
					area = numberPlanDao.getArea(portMeDetails.getSubscriberArrType().get(0).getMsisdn());
					String nrhArea = numberPlanDao.getNRH(portMeDetails.getSubscriberArrType().get(0).getMsisdn());// array
					String[] strList = nrhArea.split(",");
					String original_area = null;
					// String original_op = null;
					if (strList.length == 2) {
						original_area = strList[0];
						// original_op = strList[1];

					}
					if (original_area != null) {
						mch_type = numberPlanDao.getMCHTypeByArea(original_area);
					}
					portMeDetails.setArea(area);
					for (SubscriberArrType subAuth : portMeDetails.getSubscriberArrType()) {
						portMeService.updatePortAprovalReqeust(current_status, subAuth.getMsisdn(), reqType,
								resultCode);
					}
					portMeTransactionService.savePortMeTransactionDetails(portMeTransaction,
							portMeDetails.getSubscriberArrType());
				}
				// going to convert into xml format
				_logger.info("trying to convert port approval request into xml");
				String xml = null;
				if (mch_type == 1) {
					requestId = portMeRepository.getSynReqeustId(user.getOp_id());
					portMeDetails.setReferenceId(referenceId);
					xml = new NPOUtils().convertJsonIntoPortApproval(portMeDetails, mch_type, requestId, resultCode);
				} else {
					String donorLSAID = numberPlanDao
							.getDonorLSAID(portMeDetails.getSubscriberArrType().get(0).getMsisdn());
					if (donorLSAID == null) {
						donorLSAID = area;
					}
					// String messageSenderTelco =
					// ReadConfigFile.getProperties().getProperty("MessageSenderTelco-ZOOM");
					// String transactionId = portMeService.getTransactionId(messageSenderTelco,
					// area);
					String transactionId = referenceId;
					requestId = referenceId;
					portMeDetails.setLast_area(donorLSAID);
					String routeInfo = numberPlanDao.getRouteInfo(referenceId, reqType);
					portMeDetails.setRn(routeInfo);
					xml = new NPOUtils().convertJsonIntoPortApproval(portMeDetails, mch_type, transactionId,
							resultCode);
				}
				_logger.info("convert port approval request into xml with requestId-" + requestId + xml);
				int returnvalue = jmsProducer.sendMessageToInQueue(xml, sessionId, mch_type);
				if (returnvalue == 1) {
					_logger.info("Successfully sent portme approval request in jms queue with requestId-" + requestId);
					errorBean.setResponseCode(APIConst.successCode);
					errorBean.setResponseMessage(APIConst.successMsg);
					msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
					response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
				} else {
					_logger.info("Successfully saved port approval data but not able to send jms queue with requestId-"
							+ requestId);
					errorBean.setId(1);
					errorBean.setResponseCode(APIConst.successCode2);
					errorBean.setResponseMessage(APIConst.successMsg2);
					msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
					response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
				}

				if (current_status != 0) {
					portMeService.updatePortMeDetails(current_status, requestId, user.getUserId(), referenceId, reqType,
							portMeDetails.getBillingUID1(), portMeDetails.getInstanceID());
				}
			}
		} catch (Exception e) {
			_logger.error("Something is wrong port approval request with requestId-" + requestId + e.getMessage());
			portMeTransaction = new PortMeTransactionDetails();
			portMeTransaction.setReferenceId(referenceId);
			portMeTransaction.setStatus(19);
			portMeTransaction.setRequestType(reqType);
			portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
			errorBean.setResponseCode(APIConst.successCode1);
			errorBean.setResponseMessage(APIConst.successMsg1);
			msg = PortMeUtils.generateJsonResponse(errorBean, "ERROR");
			response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.BAD_REQUEST);
		}
		return response;
	}

	@PostMapping("/api/portdisconnectionanswer")
	public ResponseEntity<?> sendPortDisconnectionAnswer(@RequestBody Map<String, List<String>> payload) {
		List<String> checkedIds = payload.get("checkedIds");
		String sessionId = Long.toString(System.currentTimeMillis());
		_logger.info("Recieved port disconnection answer request");
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		PortMeAPIResponse errorBean = new PortMeAPIResponse();
		PortMeTransactionDetails portMeTransaction = new PortMeTransactionDetails();
		ResponseEntity response = null;
		String requestId = null;
		String referenceId = null;
		String msg = null;
		int current_status = 15;
		String reqType = ReadConfigFile.getProperties().getProperty("PORTME_OUT");
		List<PortMe> listOfZ1 = new ArrayList<PortMe>();
		List<PortMe> listOfZ2 = new ArrayList<PortMe>();
		int mch_type = 0;
		try {
			for (String refId : checkedIds) {
				PortMe portMe = portMeService.getListPortMeByReferenceId(reqType, refId);
				if (portMe.getMch() == 1) {
					listOfZ1.add(portMe);
				} else {
					listOfZ2.add(portMe);
				}
			}
			if (listOfZ1.size() > 0) {
				List<String> listOfMsisdn = new ArrayList<String>();
				requestId = portMeRepository.getSynReqeustId(user.getOp_id());
				for (PortMe item : listOfZ1) {
					List<String> list = portMeDao.getListOfMSISDN(item.getPortId());
					listOfMsisdn.addAll(list);
					subscriberArrTypeService.updateSDAStatusByPortId(current_status, item.getPortId());
					portMeService.updatePortMeStats(current_status, requestId, user.getUserId(), item.getReferenceId(),
							reqType);
				}

				// going to convert into xml format
				_logger.info("trying to convert port disconnection answer into xml with requestId-" + referenceId);

				mch_type = 1;
				String xml = new NPOUtils().convertJsonIntoXmlSDA(listOfMsisdn, mch_type, requestId, null);
				_logger.info("converted port disconnection answer with requestId-" + requestId + xml);
				int returnvalue = jmsProducer.sendMessageToInQueue(xml, sessionId, mch_type);
				if (returnvalue == 1) {
					_logger.info("sent portme disconnection request in jms queue with requestId-" + requestId);
					_logger.info("processed port disconnection answer with requestId-" + requestId);
					errorBean.setResponseCode(APIConst.successCode);
					errorBean.setResponseMessage(APIConst.successMsg);
					msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
					response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
				} else {
					_logger.info(
							"Successfully saved port disconnection answer data but not able to send jmsqueue with requestId-"
									+ requestId);
					errorBean.setResponseCode(APIConst.successCode2);
					errorBean.setResponseMessage(APIConst.successMsg2);
					msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
					response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
				}
			}
			if (listOfZ2.size() > 0) {
				for (PortMe item : listOfZ2) {
					requestId = item.getReferenceId();
					List<String> listOfMsisdn = portMeDao.getListOfMSISDN(item.getPortId());
					subscriberArrTypeService.updateSDAStatusByPortId(current_status, item.getPortId());
					portMeService.updatePortMeStats(current_status, requestId, user.getUserId(), item.getReferenceId(),
							reqType);
					_logger.info("trying convert portme connection answer request into xml with requestId : "
							+ item.getReferenceId());
					String area = numberPlanDao.getArea(listOfMsisdn.get(0));
					mch_type = 2;
					String donorLSAID = numberPlanDao.getDonorLSAID(listOfMsisdn.get(0));
					if (donorLSAID == null) {
						donorLSAID = area;
					}
					String xml = new NPOUtils().convertJsonIntoXmlSDA(listOfMsisdn, mch_type, requestId, donorLSAID);
					_logger.info("converted port disconnection answer with requestId-" + requestId + xml);
					int returnvalue = jmsProducer.sendMessageToInQueue(xml, sessionId, mch_type);
					if (returnvalue == 1) {
						_logger.info("sent portme disconnection request in jms queue with requestId-" + requestId);
						_logger.info("processed port disconnection answer with requestId-" + requestId);
						errorBean.setResponseCode(APIConst.successCode);
						errorBean.setResponseMessage(APIConst.successMsg);
						msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
						response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
					} else {
						_logger.info(
								"Successfully saved port disconnection answer data but not able to send jmsqueue with requestId-"
										+ requestId);
						errorBean.setResponseCode(APIConst.successCode2);
						errorBean.setResponseMessage(APIConst.successMsg2);
						msg = PortMeUtils.generateJsonResponse(errorBean, "SUCCESS");
						response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.OK);
					}
				}
			}
		} catch (

		Exception e) {
			_logger.error("get error port disconnection answer with requestId-" + requestId + e.getMessage());
			portMeTransaction = new PortMeTransactionDetails();
			portMeTransaction.setReferenceId(referenceId);
			portMeTransaction.setStatus(19);
			portMeTransaction.setRequestType(reqType);
			portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
			errorBean.setResponseCode(APIConst.successCode1);
			errorBean.setResponseMessage(APIConst.successMsg1);
			msg = PortMeUtils.generateJsonResponse(errorBean, "ERROR");
			// e.printStackTrace();
			response = new ResponseEntity(msg, new HttpHeaders(), HttpStatus.BAD_REQUEST);
		}
		if (current_status != 0) {
			// inserting current status in port_tx table
			portMeService.updatePortMeStats(current_status, requestId, user.getUserId(), referenceId, reqType);
		}
		return response;
	}

	/* start code shobhit */
	@GetMapping(value = "getrn")
	public String getrn(@RequestParam(name = "op_id") String opId, @RequestParam(name = "area") String area) {
		String rn = portMeDao.getRnbyOpIdandarea(opId, area);
		return rn;
	}

	@GetMapping(value = "getmsisdn")
	public String getmsisdn(@RequestParam(name = "msisdn") String msisdn) {
		String dno = portMeDao.getdonorbymsisdn(msisdn);
		return dno;
	}

	/* end code shobhit */
}
