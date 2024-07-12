package com.megthink.gateway.consumer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import com.megthink.gateway.dao.BillingResolutionDao;
import com.megthink.gateway.dao.NumberPlanDao;
import com.megthink.gateway.dao.PortMeDao;
import com.megthink.gateway.dao.SubscriberInfoQueryDetailDao;
import com.megthink.gateway.dao.TerminateSimDao;
import com.megthink.gateway.dao.TerminateSimMTDao;
import com.megthink.gateway.model.BillingResolution;
import com.megthink.gateway.model.InitAck;
import com.megthink.gateway.model.MSISDNUIDType;
import com.megthink.gateway.model.MasterNP;
import com.megthink.gateway.model.NPO;
import com.megthink.gateway.model.NPOA;
import com.megthink.gateway.model.NPOS;
import com.megthink.gateway.model.NPOSA;
import com.megthink.gateway.model.NPOSAACK;
import com.megthink.gateway.model.NPOSPR;
import com.megthink.gateway.model.NPOSTER;
import com.megthink.gateway.model.NPOT;
import com.megthink.gateway.model.NPOTA;
import com.megthink.gateway.model.NVP;
import com.megthink.gateway.model.OrderCancellation;
import com.megthink.gateway.model.OrderReversal;
import com.megthink.gateway.model.PortMe;
import com.megthink.gateway.model.PortMeTransactionDetails;
import com.megthink.gateway.model.RSP;
import com.megthink.gateway.model.RecoveryDB;
import com.megthink.gateway.model.RecoveryDBResponse;
import com.megthink.gateway.model.SC;
import com.megthink.gateway.model.SCInfo;
import com.megthink.gateway.model.SD;
import com.megthink.gateway.model.SDInfo;
import com.megthink.gateway.model.SubscriberInfoQueryDetail;
import com.megthink.gateway.model.SubscriberResult;
import com.megthink.gateway.model.SubscriberSequence;
import com.megthink.gateway.model.TerminateSim;
import com.megthink.gateway.model.TerminateSimMT;
import com.megthink.gateway.model.TerminateSimTransactionDetails;
import com.megthink.gateway.producer.JmsProducer;
import com.megthink.gateway.service.AuthorService;
import com.megthink.gateway.service.BillingResolutionService;
import com.megthink.gateway.service.BroadcastHistoryService;
import com.megthink.gateway.service.CorporateCustomerService;
import com.megthink.gateway.service.MasterNPService;
import com.megthink.gateway.service.PersonCustomerService;
import com.megthink.gateway.service.PortMeService;
import com.megthink.gateway.service.PortMeTransactionDetailsService;
import com.megthink.gateway.service.RecoveryDBService;
import com.megthink.gateway.service.SubscriberArrTypeService;
import com.megthink.gateway.service.SubscriberAuthorizationService;
import com.megthink.gateway.service.SubscriberInfoQueryDetailService;
import com.megthink.gateway.service.TerminateSimMTService;
import com.megthink.gateway.service.TerminateSimService;
import com.megthink.gateway.service.TerminateSimTransactionDetailsService;
import com.megthink.gateway.utils.NPOUtils;
import com.megthink.gateway.utils.ReadConfigFile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PortMeZ1Consumer {

	private static final Logger _logger = LoggerFactory.getLogger(PortMeZ1Consumer.class);

	Timestamp timestamp = new Timestamp(System.currentTimeMillis());

	@Autowired
	private PortMeService portMeService;
	@Autowired
	private SubscriberAuthorizationService subscriberAuthService;
	@Autowired
	private CorporateCustomerService corporateCustomerService;
	@Autowired
	private PersonCustomerService personCustomerSerive;
	@Autowired
	private AuthorService authorService;
	@Autowired
	private PortMeTransactionDetailsService portMeTransactionService;
	@Autowired
	private TerminateSimService terminateSimService;
	@Autowired
	private TerminateSimTransactionDetailsService terminateTransactionService;
	@Autowired
	private TerminateSimMTService terminateSimMTService;
	@Autowired
	private MasterNPService masterNPService;
	@Autowired
	private BroadcastHistoryService broadcastHistoryService;
	@Autowired
	private RecoveryDBService recoveryDBService;
	@Autowired
	private SubscriberArrTypeService subscriberArrTypeService;
	@Autowired
	private BillingResolutionService billingResolutionService;
	@Autowired
	private BillingResolutionDao billingResolutionDao;
	@Autowired
	private NumberPlanDao numberPlanDao;
	@Autowired
	private TerminateSimMTDao terminateSimMTDao;
	@Autowired
	private TerminateSimDao terminateTxDao;
	@Autowired
	private PortMeDao portMeDao;
	@Autowired
	private SubscriberInfoQueryDetailService nvpService;
	@Autowired
	private SubscriberInfoQueryDetailDao subscriberInfoQueryDetailDao;
	@Autowired
	private JmsProducer jmsProducer;

	@SuppressWarnings("deprecation")
	@JmsListener(destination = "Z1INQueue")
	// @JmsListener(destination = "mchNetworkQueue")
	public void receiveMessage(Message message) {
		String sessionId = Long.toString(System.currentTimeMillis());
		_logger.info("[sessionId=" + sessionId
				+ "]: PortMeZ1Consumer.receiveMessage()- Recieved Soap Message - process start with timestamp:["
				+ new Timestamp(System.currentTimeMillis()) + "]");
		int current_status = 0;
		String referenceId = null;
		String messageType = "";
		String removeCountryCode = ReadConfigFile.getProperties().getProperty("zone1-countrycode");
		int removeCountryCodeLimit = Integer.parseInt(removeCountryCode);
		try {
			PortMeTransactionDetails portMeTransaction = new PortMeTransactionDetails();
			TextMessage msg = (TextMessage) message;
			_logger.info("Received Message: " + msg.getText());

			ObjectMapper mapper = new ObjectMapper();
			JsonNode arrayNode = mapper.readTree(msg.getText().toString());
			if (arrayNode.isArray() && arrayNode.size() > 0) {
				JsonNode firstObject = arrayNode.get(0);
				if (firstObject.has("messageType")) {
					messageType = firstObject.get("messageType").asText();
				} else if (firstObject.has("MessageType")) {
					messageType = firstObject.get("MessageType").asText();
				}
			} else {
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				InitAck initAck = mapper.readValue(msg.getText(), InitAck.class);
				messageType = initAck.getMessageType();
			}

			String xml = null;
			/* start code for NPOA Soap request with JMS out queue */
			if (messageType.equals(ReadConfigFile.getProperties().getProperty("INIT_ACK_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-INIT_ACK_MESSAGE Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				InitAck initAck = mapper.readValue(msg.getText(), InitAck.class);
				String requestId = initAck.getRequestId();
				String requestType = ReadConfigFile.getProperties().getProperty("PORTME_IN");
				current_status = 6;
				subscriberArrTypeService.updatePortMeACK(current_status, requestId, requestType);
				/*
				 * _logger.debug("[sessionId=" + sessionId +
				 * "]: PortMeZ1Consumer.receiveMessage()- trying to convert INIT_ACK into xml with timestamp:["
				 * + new Timestamp(System.currentTimeMillis()) + "]"); xml = new
				 * NPOUtils().convertInitAckIntoXML(initAck); _logger.debug("[sessionId=" +
				 * sessionId +
				 * "]: PortMeZ1Consumer.receiveMessage()- successfully to convert INIT_ACK into xml with timestamp:["
				 * + new Timestamp(System.currentTimeMillis()) + "], xml - [" + xml + "]");
				 * _logger.debug("[sessionId=" + sessionId +
				 * "]: PortMeZ1Consumer.receiveMessage()- successfully to convert INIT_ACK into xml with timestamp:["
				 * + new Timestamp(System.currentTimeMillis()) + "], xml - [" + xml + "]"); int
				 * interval = jmsProducer.sentIntoInternalInQ(xml, sessionId); if (interval ==
				 * 1) { _logger.debug("[sessionId=" + sessionId +
				 * "]: PortMeZ1Consumer.receiveMessage()- Successfully Send message to internalInQueue with timestamp:["
				 * + new Timestamp(System.currentTimeMillis()) + "]"); }
				 */
				if (current_status != 0) {
					portMeService.updatePortMeStatusByRequestId(current_status, initAck.getResultCode(), requestId,
							requestType);
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-INIT_ACK_MESSAGE Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			} else if (messageType.equals(ReadConfigFile.getProperties().getProperty("NPOA_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOA Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				NPOA[] listOfNPOA = mapper.readValue(msg.getText().toString(), NPOA[].class);
				for (NPOA npoa : listOfNPOA) {
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveMessage()-NPOA Soap Message - start processing of NPOA message from jms with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					referenceId = npoa.getReferenceId();
					String requestType = ReadConfigFile.getProperties().getProperty("PORTME_IN");
					current_status = 9;
					portMeTransaction.setReferenceId(npoa.getRequestId());
					portMeTransaction.setStatus(current_status);
					portMeTransaction.setRequestType(requestType);
					if (npoa.getSubscriberResult().size() > 0) {
						portMeTransactionService.savePortMeTransactionDetail(portMeTransaction,
								npoa.getSubscriberResult());
						for (SubscriberResult subRestult : npoa.getSubscriberResult()) {
							String orderedTransferTime = null;
							if (subRestult.getResultCode() == 0) {
								orderedTransferTime = npoa.getOrderedTransferTime();
							}
							_logger.debug("[sessionId=" + sessionId
									+ "]: PortMeZ1Consumer.receiveMessage()-NPOA Soap Message - trying to update table plan number ["
									+ subRestult.getSubscriberNumber() + "] with timestamp:["
									+ new Timestamp(System.currentTimeMillis()) + "]");
							portMeService.updateNPOARequest(current_status,
									subRestult.getSubscriberNumber().substring(removeCountryCodeLimit), requestType,
									subRestult.getResultCode(), orderedTransferTime);
							_logger.debug("[sessionId=" + sessionId
									+ "]: PortMeZ1Consumer.receiveMessage()-NPOA Soap Message - successfully updated table plan number ["
									+ subRestult.getSubscriberNumber() + "] with timestamp:["
									+ new Timestamp(System.currentTimeMillis()) + "]");
						}
					} else {

					}
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveMessage()-NPOA Soap Message - trying to convert NPOA into XML with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					xml = "NPOA SOAP";// new NPOUtils().convertNPOASoapIntoXML(npoa,"");// porting confirmation
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveMessage()-NPOA Soap Message - Successfully converted NPOA into XML with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					int interval = jmsProducer.sentIntoInternalInQ(xml, sessionId);
					if (interval == 1) {
						_logger.debug("[sessionId=" + sessionId
								+ "]: PortMeZ1Consumer.receiveMessage()-NPOA Soap Message - Successfully sent XML into jms queue with timestamp:["
								+ new Timestamp(System.currentTimeMillis()) + "]");
					}
					if (current_status != 0) {
						portMeService.updatePortMeStatus(current_status, referenceId, requestType);
					}
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOA Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}
			/* end code for NPOA Request */
			/* start code for connection ACK (SCA) */
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("CON_ACK_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-CONNECTION_ANS_ACK Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				InitAck initAck = mapper.readValue(msg.getText(), InitAck.class);
				String requestId = initAck.getRequestId();
				String requestType = ReadConfigFile.getProperties().getProperty("PORTME_IN");
				current_status = 18;
				subscriberArrTypeService.updateScaAndSdaAck(current_status, requestId, initAck.getResultCode());
				// going to convert into xml format
				/*
				 * xml = new NPOUtils().convertInitAckIntoXML(initAck); sessionId =
				 * Long.toString(System.currentTimeMillis()); int interval =
				 * jmsProducer.sentIntoInternalInQ(xml, sessionId); if (interval == 1) {
				 * _logger.debug("[sessionId=" + sessionId +
				 * "]: PortMeZ1Consumer.receiveMessage()-Successfully sent CONNECTION_ANS_ACK xml into jms queue with timestamp:["
				 * + new Timestamp(System.currentTimeMillis()) + "]"); }
				 */
				if (current_status != 0) {
					portMeService.updatePortMeStatusByRequestId(current_status, initAck.getResultCode(), requestId,
							requestType);
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-CONNECTION_ANS_ACK Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}
			/* start code for NPOResponse Soap request with JMS out queue */
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("NPORsp_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPORsp Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				RSP[] list = mapper.readValue(msg.getText(), RSP[].class);
				for (RSP npoarsp : list) {
					referenceId = npoarsp.getReferenceId();
					String requestType = ReadConfigFile.getProperties().getProperty("PORTME_IN");
					current_status = 6;
					portMeTransaction.setReferenceId(referenceId);
					portMeTransaction.setStatus(current_status);
					portMeTransaction.setRequestType(requestType);
					if (npoarsp.getSubscriberResult().size() > 0) {
						portMeTransactionService.savePortMeTransactionDetail(portMeTransaction,
								npoarsp.getSubscriberResult());
						for (SubscriberResult subResult : npoarsp.getSubscriberResult()) {
							_logger.debug("[sessionId=" + sessionId
									+ "]: PortMeZ1Consumer.receiveMessage()-NPORsp Soap Message - trying to update plan number["
									+ subResult.getSubscriberNumber() + "] into db timestamp:["
									+ new Timestamp(System.currentTimeMillis()) + "]");
							portMeService.updatePortMtResultCodeByMsisdn(current_status,
									subResult.getSubscriberNumber().substring(removeCountryCodeLimit), requestType,
									subResult.getResultCode());
							_logger.debug("[sessionId=" + sessionId
									+ "]: PortMeZ1Consumer.receiveMessage()-NPORsp Soap Message - Successfully update plan number into db with timestamp:["
									+ new Timestamp(System.currentTimeMillis()) + "]");
						}
					} else {
						// update portme
					}
					// going to convert into xml format
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveMessage()-NPORsp Soap Message - trying to convert NPORsp into xml with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					xml = "NPORSP";// new NPOUtils().convertJsonIntoNPOARsp(npoarsp);
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveMessage()-NPORsp Soap Message - successfully converted NPORsp into xml with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					int interval = 1;// jmsProducer.sentIntoInternalInQ(xml, sessionId);
					if (interval == 1) {
						_logger.debug("[sessionId=" + sessionId
								+ "]: PortMeZ1Consumer.receiveMessage()-NPORsp Soap Message - successfully sent xml into jms queue with timestamp:["
								+ new Timestamp(System.currentTimeMillis()) + "]");
					}
					if (current_status != 0) {
						portMeService.updatePortMeStatus(current_status, referenceId, requestType);
					}
					_logger.info("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveMessage()-NPORsp Soap Message - process end with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
				}
			}

			/* start code for NPO SOAP request with jms out queue */
			if (messageType.equals(ReadConfigFile.getProperties().getProperty("NPO_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPO Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				NPO[] listOfNPO = mapper.readValue(msg.getText().toString(), NPO[].class);
				for (NPO npo : listOfNPO) {
					referenceId = npo.getReferenceId();
					String msisdns = null;
					if (npo.getlOAImage() != null) {
						byte[] decodedBytes = Base64.getDecoder().decode(npo.getlOAImage());
						// Write the bytes to a file
						_logger.debug("[sessionId=" + sessionId
								+ "]: PortMeZ1Consumer.receiveMessage()-NPO Soap Message - trying to store document into our system with timestamp:["
								+ new Timestamp(System.currentTimeMillis()) + "]");
						String filePath = System.getProperty("user.home") + "/"
								+ ReadConfigFile.getProperties().getProperty("upload.document") + "/" + referenceId
								+ "OUT.pdf";

						try (FileOutputStream fos = new FileOutputStream(filePath)) {
							fos.write(decodedBytes);
							_logger.debug("[sessionId=" + sessionId
									+ "]: PortMeZ1Consumer.receiveMessage()-NPO Soap Message - successfully stored document into our system with timestamp:["
									+ new Timestamp(System.currentTimeMillis()) + "]");
						} catch (IOException e) {
							_logger.error("[sessionId=" + sessionId
									+ "]: PortMeZ1Consumer.receiveMessage()-NPO Soap Message - Exception occurs while storing document file into our system with timestamp:["
									+ new Timestamp(System.currentTimeMillis()) + "]");
						}
					}
					String requestType = ReadConfigFile.getProperties().getProperty("PORTME_OUT");
					int isExist = portMeDao.isExistNPO(referenceId, requestType);
					if (isExist == 0) {
						_logger.debug("[sessionId=" + sessionId
								+ "]: PortMeZ1Consumer.receiveMessage()-NPO Soap Message - start processing to store NPO data inot db with timestamp:["
								+ new Timestamp(System.currentTimeMillis()) + "]");
						npo.setRequestId(referenceId);
						current_status = 3;
						portMeTransaction.setReferenceId(npo.getReferenceId());
						portMeTransaction.setStatus(current_status);
						portMeTransaction.setRequestType(requestType);
						PortMe portMe = new PortMe();
						if (npo.getSubscriberAuthSequence() != null) {
							if (npo.getSubscriberAuthSequence().getSubscriberAuthorization().size() > 0) {
								portMeTransactionService.savePortMeTransactionDetailStat(portMeTransaction,
										npo.getSubscriberAuthSequence().getSubscriberAuthorization());
								msisdns = npo.getSubscriberAuthSequence().getSubscriberAuthorization().get(0)
										.getSubscriberNumber();
								portMe.setCompanyCode(npo.getSubscriberAuthSequence().getSubscriberAuthorization()
										.get(0).getOwnerId());
								portMe.setDataType(2);
							}
						} else {
							for (SubscriberSequence item : npo.getSubscriberSequence()) {
								portMeTransaction
										.setMsisdn(item.getSubscriberNumber().substring(removeCountryCodeLimit));
								portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
								msisdns = item.getSubscriberNumber();
								portMe.setDataType(1);
							}
						}
						String lastArea = numberPlanDao.getDonorLSAID(msisdns.substring(removeCountryCodeLimit));
						if (lastArea == null) {
							lastArea = numberPlanDao.getArea(msisdns.substring(removeCountryCodeLimit));
						}
						String nrhArea = numberPlanDao.getNRH(msisdns.substring(removeCountryCodeLimit));// in array
						String[] strList = nrhArea.split(",");
						String original_area = null;
						String original_op = null;
						if (strList.length == 2) {
							original_area = strList[0];
							original_op = strList[1];

						}
						portMe.setOriginal_area(original_area);
						portMe.setOriginal_op(original_op);
						portMe.setLast_area(lastArea);
						portMe.setSource(npo.getMessageSenderTelco());
						portMe.setReferenceId(npo.getReferenceId());
						portMe.setTimeStamp(npo.getTimestamp());
						portMe.setDno(npo.getDonorTelco());
						portMe.setArea(npo.getLsa());
						portMe.setRn(npo.getRouteNumber());
						portMe.setService(npo.getServiceType());
						portMe.setStatus(current_status);
						portMe.setMch(1);
						portMe.setOrderDate(timestamp.toString());
						portMe.setOriginalCarrier(npo.getDonorTelco());
						portMe.setRno(npo.getRecipientTelco());
						portMe.setCustomerRequestTime(timestamp.toString());
						portMe.setCreatedDate(timestamp);
						portMe.setUpdatedDate(timestamp);
						portMe.setRequest_type(requestType);
						if (npo.getPersonCustomer() != null) {
							portMe.setCompanyCode(npo.getPersonCustomer().getOwnerId());
						}
						PortMe port = portMeService.savePortMe(portMe);
						String hlr = null;
						if (port.getPortId() != 0) {
							if (npo.getCorporateCustomer() != null) {
								npo.getCorporateCustomer().setPortId(port.getPortId());
								corporateCustomerService.saveCorporateCustomer(npo.getCorporateCustomer());
							} else {
								npo.getPersonCustomer().setPortId(port.getPortId());
								personCustomerSerive.savePersonCustomer(npo.getPersonCustomer());
								hlr = npo.getPersonCustomer().getOwnerId();
							}

							if (npo.getSubscriberAuthSequence() != null) {
								if (npo.getSubscriberAuthSequence().getSubscriberAuthorization().size() > 0) {
									subscriberAuthService.saveMt(portMe.getPortId(),
											npo.getSubscriberAuthSequence().getSubscriberAuthorization(), requestType,
											current_status, npo.getOrderedApprovalTime(), npo.getOrderedTransferTime());
								}
							} else {
								for (SubscriberSequence item : npo.getSubscriberSequence()) {
									String msisdn = item.getSubscriberNumber().substring(removeCountryCodeLimit);
									portMeTransaction.setMsisdn(msisdn);
									subscriberArrTypeService.createPortMTNPO(port.getPortId(), msisdn, requestType,
											current_status, npo.getOrderedApprovalTime(), npo.getOrderedTransferTime(),
											hlr);
								}
							}

							npo.getAuthor().setPortId(port.getPortId());
							authorService.saveAuthor(npo.getAuthor());
							_logger.debug("[sessionId=" + sessionId
									+ "]: PortMeZ1Consumer.receiveMessage()-NPO Soap Message - Successfully stored NPO data inot db with timestamp:["
									+ new Timestamp(System.currentTimeMillis()) + "]");
							// going to convert into xml format
							_logger.debug("[sessionId=" + sessionId
									+ "]: PortMeZ1Consumer.receiveMessage()-NPO Soap Message - Trying to convert NPO into xml with timestamp:["
									+ new Timestamp(System.currentTimeMillis()) + "]");
							/* This xml is used for client if he want to get XML */
							xml = "NPO SOAP";// new NPOUtils().generatePortingOutXML(npo, "");// should be porting out,
												// this
												// should be
							_logger.debug("[sessionId=" + sessionId
									+ "]: PortMeZ1Consumer.receiveMessage()-NPO Soap Message - Successfully converted NPO into xml with timestamp:["
									+ new Timestamp(System.currentTimeMillis()) + "]");
							// configure flag yes or not for generate
							int interval = jmsProducer.sentIntoInternalInQ(xml, sessionId);
							if (interval == 1) {
								// going to update current status in port_tx table
								_logger.debug("[sessionId=" + sessionId
										+ "]: PortMeZ1Consumer.receiveMessage()-NPO Soap Message - successfully sent xml into jms queue with timestamp:["
										+ new Timestamp(System.currentTimeMillis()) + "]");
								portMeService.updatePortMeStatus(current_status, referenceId, requestType);
							}
						}
					} else {
						_logger.info("[sessionId=" + sessionId
								+ "]: PortMeZ1Consumer.receiveMessage()-NPO Soap Message - already exist with timestamp:["
								+ new Timestamp(System.currentTimeMillis()) + "]");
					}
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPO Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}
			/* end NPO SOAP Request */
			/* start code for NPOAResponse Soap request with JMS out queue */
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("NPOARsp_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOARsp Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				RSP[] list = mapper.readValue(msg.getText(), RSP[].class);
				for (RSP npoarsp : list) {
					referenceId = npoarsp.getReferenceId();
					String requestType = ReadConfigFile.getProperties().getProperty("PORTME_OUT");
					current_status = 9;
					portMeTransaction.setReferenceId(npoarsp.getReferenceId());
					portMeTransaction.setStatus(current_status);
					portMeTransaction.setRequestType(requestType);
					if (npoarsp.getSubscriberResult().size() > 0) {
						portMeTransactionService.savePortMeTransactionDetail(portMeTransaction,
								npoarsp.getSubscriberResult());
						for (SubscriberResult item : npoarsp.getSubscriberResult()) {
							_logger.debug("[sessionId=" + sessionId
									+ "]: PortMeZ1Consumer.receiveMessage()-NPOARsp Soap Message - trying to update plan number ["
									+ item.getSubscriberNumber() + "] into db with timestamp:["
									+ new Timestamp(System.currentTimeMillis()) + "]");
							String msisdn = item.getSubscriberNumber().substring(removeCountryCodeLimit);
							portMeService.updateNPOARsp(current_status, msisdn, requestType, item.getResultCode(),
									npoarsp.getOrderedTransferTime());
							_logger.debug("[sessionId=" + sessionId
									+ "]: PortMeZ1Consumer.receiveMessage()-NPOARsp Soap Message - successfully updated plan number ["
									+ item.getSubscriberNumber() + "] into db with timestamp:["
									+ new Timestamp(System.currentTimeMillis()) + "]");
						}
					} else {
						// update portmt
					}
					// going to convert into xml format
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveMessage()-NPOARsp Soap Message - trying to convert NPOARsp into xml with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					xml = "NPOARSP";// new NPOUtils().convertJsonIntoNPOARsp(npoarsp);
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveMessage()-NPOARsp Soap Message - successfully converted NPOARsp into xml with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					int interval = 1;// jmsProducer.sentIntoInternalInQ(xml, sessionId);
					if (interval == 1) {
						_logger.debug("[sessionId=" + sessionId
								+ "]: PortMeZ1Consumer.receiveMessage()-NPOARsp Soap Message - successfully sent xml into jms queue with timestamp:["
								+ new Timestamp(System.currentTimeMillis()) + "]");
					}
					if (current_status != 0) {
						portMeService.updatePortMeStatus(current_status, referenceId, requestType);
					}
					_logger.info("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveMessage()-NPOARsp Soap Message - process end with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
				}
			}
			/* end code for NPOAResponse */
			/* start code Disconnect ACK */
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("DISCONNECT_ACK_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-DISCONNECT_ACK_MESSAGE Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				InitAck initAck = mapper.readValue(msg.getText(), InitAck.class);
				String requestId = initAck.getRequestId();
				String requestType = ReadConfigFile.getProperties().getProperty("PORTME_OUT");
				current_status = 18;
				_logger.debug("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-DISCONNECT_ACK_MESSAGE Soap Message - trying to update DISCONNECT_ACK_MESSAGE into db with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				subscriberArrTypeService.updateScaAndSdaAck(current_status, requestId, initAck.getResultCode());
				_logger.debug("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-DISCONNECT_ACK_MESSAGE Soap Message - successfully updated DISCONNECT_ACK_MESSAGE into db with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				// going to convert into xml format
				/*
				 * _logger.info("[sessionId=" + sessionId +
				 * "]: PortMeZ1Consumer.receiveMessage()-DISCONNECT_ACK_MESSAGE Soap Message - trying to convert DISCONNECT_ACK_MESSAGE into xml with timestamp:["
				 * + new Timestamp(System.currentTimeMillis()) + "]"); xml = new
				 * NPOUtils().convertInitAckIntoXML(initAck); int interval =
				 * jmsProducer.sentIntoInternalInQ(xml, sessionId); if (interval == 1) {
				 * _logger.info("Send DISCONNECT_ACK xml into internalIn jms queue : " + xml); }
				 */
				if (current_status != 0) {
					portMeService.updatePortMeStatusByRequestId(current_status, initAck.getResultCode(), requestId,
							requestType);
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-DISCONNECT_ACK_MESSAGE Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}

			/**************************************************************
			 * Terminate *
			 *************************************************************/

			/* start code for NPOT Soap (TERMINATE SIM) request with JMS out queue */
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("NPOT_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOT Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				NPOT[] listOfNPOT = mapper.readValue(msg.getText().toString(), NPOT[].class);
				for (NPOT npot : listOfNPOT) {
					String requestType = ReadConfigFile.getProperties().getProperty("TERMINATE_OUT");
					referenceId = npot.getReferenceId();
					int resultCode = 0;
					current_status = 3;
					MasterNP masterNP = null;
					TerminateSim terminationDetails = new TerminateSim();
					if (npot.getSubscriberSequence().size() > 0) {
						String query = npot.getSubscriberSequence().get(0).getSubscriberNumber()
								.substring(removeCountryCodeLimit);
						masterNP = masterNPService.findByMsisdn(query);
						String nrhArea = numberPlanDao.getNRH(query);// in array
						if (nrhArea != null) {
							String[] strList = nrhArea.split(",");
							// String original_area = null;
							String original_op = null;
							if (strList.length == 2) {
								// original_area = strList[0];
								original_op = strList[1];
							}
							terminationDetails.setDno(original_op);
						}
					}
					terminationDetails.setReference_id(referenceId);
					terminationDetails.setRequestId(npot.getRequestId());
					terminationDetails.setSource(npot.getMessageSenderTelco());
					terminationDetails.setTimeStamp(npot.getTimestamp());
					terminationDetails.setStatus(current_status);
					terminationDetails.setMch(1);
					if (masterNP != null) {
						terminationDetails.setArea(masterNP.getArea());
						terminationDetails.setOriginalCarrier(masterNP.getOrginal_carrier());
						terminationDetails.setRn(masterNP.getRn());
						terminationDetails.setService(masterNP.getService());
					}
					if (npot.getOrderedApprovalTime() != null) {
						terminationDetails.setApproval(npot.getOrderedApprovalTime());
					}
					if (npot.getOrderedTransferTime() != null) {
						terminationDetails.setTerminationTime(npot.getOrderedTransferTime());
					}
					terminationDetails.setRequestType(requestType);
					TerminateSim terminateSim = terminateSimService.saveTerminateSim(terminationDetails);
					for (SubscriberSequence item : npot.getSubscriberSequence()) {
						String msisdn = item.getSubscriberNumber().substring(removeCountryCodeLimit);
						TerminateSimTransactionDetails terminationTransaction = new TerminateSimTransactionDetails();
						terminationTransaction.setRequestId(referenceId);
						terminationTransaction.setStatus(current_status);
						terminationTransaction.setRequestType(requestType);
						terminationTransaction.setMsisdn(msisdn);
						terminateTransactionService.saveTerminateSimTransactionDetails(terminationTransaction);
						TerminateSimMT terminateSIMMt = new TerminateSimMT();
						terminateSIMMt.setTerminateId(terminateSim.getTerminateId());
						terminateSIMMt.setRequest_type(requestType);
						terminateSIMMt.setStatus(current_status);
						terminateSIMMt.setSubscriberNumber(msisdn);
						terminateSIMMt.setResultCode(resultCode);
						terminateSimMTService.saveTerminateSimMT(terminateSIMMt);
					}
					// going to convert into xml format

					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveMessage()-NPOT Soap Message - trying to convert NPOT into xml timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					/*
					 * xml = "abcd";// new NPOUtils().convertNPOTSoapIntoXML(npot);
					 * _logger.info("convert portme termination request into xml with requestId:" +
					 * referenceId + xml); sessionId = Long.toString(System.currentTimeMillis());
					 * int interval = jmsProducer.sentIntoInternalInQ(xml, sessionId); if (interval
					 * == 1) { _logger.info("Sent NPOT xml request in jms queue with requestId-" +
					 * referenceId); } else {
					 * _logger.info("Unsent Termination MSISDN request in jms queue with requestId-"
					 * + referenceId); }
					 */
					if (current_status != 0) {
						terminateSimService.updateTerminateSIM(terminateSim.getTerminateId(), current_status);
					}
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOT Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}
			/* end code for NPOT */

			// getting order NPOTA soap request from external system
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("NPOTA_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOTA Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				NPOTA[] listOfNPOT = mapper.readValue(msg.getText().toString(), NPOTA[].class);
				String requestType = ReadConfigFile.getProperties().getProperty("TERMINATE_IN");
				current_status = 6;
				for (NPOTA npota : listOfNPOT) {
					referenceId = npota.getReferenceId();
					for (SubscriberResult item : npota.getSubscriberResult()) {
						String msisdn = item.getSubscriberNumber().substring(removeCountryCodeLimit);
						terminateSimMTDao.updateTerMtByMsisdn(msisdn, item.getResultCode(), current_status,
								requestType);
					}
				}
				terminateTxDao.updateTerminateSIMbyReferenceId(referenceId, current_status);
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOTA Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}
			// END order NPOTA soap request from external system

			// getting order cancellation ack
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("CONCEL_ACK_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-CANCEL_ACK_MESSAGE Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				InitAck cancelAck = mapper.readValue(msg.getText(), InitAck.class);
				String requestId = cancelAck.getRequestId();
				String requestType = ReadConfigFile.getProperties().getProperty("CANCEL_IN");
				current_status = 6;
				subscriberArrTypeService.updatePortMeACK(current_status, requestId, requestType);
				// going to convert into xml format
				/*
				 * _logger.info("converting order cancellation ack into xml config file"); xml =
				 * new NPOUtils().convertInitAckIntoXML(cancelAck); sessionId =
				 * Long.toString(System.currentTimeMillis()); int interval =
				 * jmsProducer.sentIntoInternalInQ(xml, sessionId); if (interval == 1) {
				 * current_status = 6;
				 * _logger.info("Send order cancellation ack xml into internalIn jms queue : " +
				 * xml); }
				 */
				if (current_status != 0) {
					portMeService.updatePortMeStatusByRequestId(current_status, cancelAck.getResultCode(), requestId,
							requestType);
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-CANCEL_ACK_MESSAGE Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}
			// getting order cancellation soap request from external system
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("CANCEL_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-PORT CANCEL Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				OrderCancellation orderCancel = mapper.readValue(msg.getText(), OrderCancellation.class);
				referenceId = orderCancel.getReferenceId();
				String curReqType = ReadConfigFile.getProperties().getProperty("CANCEL_OUT");
				String preReqType = ReadConfigFile.getProperties().getProperty("CANCEL_IN");
				current_status = 1;
				String msisdn = orderCancel.getSubscriberSequence().getSubscriberNumber();
				portMeTransaction = new PortMeTransactionDetails();
				portMeTransaction.setReferenceId(orderCancel.getRequestId());
				portMeTransaction.setStatus(current_status);
				portMeTransaction.setRequestType(curReqType);
				portMeTransaction.setMsisdn(msisdn);
				portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
				portMeService.cancelOrderByMsisdn(current_status, msisdn, preReqType, curReqType);
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-PORT CANCEL Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}
			// end order cancellation soap request from external system
			/* start acknowledgement code for order reversal */
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("REVERSAL_ACK_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-REVERSAL_ACK_MESSAGE Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				InitAck canAck = mapper.readValue(msg.getText(), InitAck.class);
				_logger.info("got ORDER REVERSAL_ACK data from outqueue : " + canAck);
				String requestId = canAck.getRequestId();
				String requestType = ReadConfigFile.getProperties().getProperty("REVERSAL_IN");
				current_status = 6;
				subscriberArrTypeService.updatePortMeACK(current_status, requestId, requestType);
				/*
				 * _logger.debug("[sessionId=" + sessionId +
				 * "]: PortMeZ1Consumer.receiveMessage()-REVERSAL_ACK_MESSAGE Soap Message - trying to convert SOAP into xml timestamp:["
				 * + new Timestamp(System.currentTimeMillis()) + "]"); xml = new
				 * NPOUtils().convertInitAckIntoXML(canAck); sessionId =
				 * Long.toString(System.currentTimeMillis()); int interval =
				 * jmsProducer.sentIntoInternalInQ(xml, sessionId); if (interval == 1) {
				 * _logger.debug("[sessionId=" + sessionId +
				 * "]: PortMeZ1Consumer.receiveMessage()-REVERSAL_ACK_MESSAGE Soap Message - successfully sent xml into jms queue timestamp:["
				 * + new Timestamp(System.currentTimeMillis()) + "]"); }
				 */
				if (current_status != 0) {
					portMeService.updatePortMeStatus(current_status, referenceId, requestType);
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-REVERSAL_ACK_MESSAGE Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}
			/* start code ORDER Reversal confirmation */
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("REVERSAL_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-ORDER REVERSAL_MESSAGE Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				OrderReversal orderReversal = mapper.readValue(msg.getText(), OrderReversal.class);
				referenceId = orderReversal.getReferenceId();
				String requestType = ReadConfigFile.getProperties().getProperty("REVERSAL_OUT");
				current_status = 1;
				String msisdn = orderReversal.getSubscriberSequence().getSubscriberNumber();
				PortMe portReversal = new PortMe();
				portReversal.setRequest_type(requestType);
				portReversal.setReferenceId(referenceId);
				portReversal.setStatus(current_status);
				PortMe portMe = portMeService.savePortMe(portReversal);
				orderReversal.setReferenceId(referenceId);
				orderReversal.setSource(portReversal.getSource());
				portMeTransaction = new PortMeTransactionDetails();
				portMeTransaction.setReferenceId(orderReversal.getRequestId());
				portMeTransaction.setStatus(current_status);
				portMeTransaction.setRequestType(requestType);
				portMeTransaction.setMsisdn(msisdn);
				portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
				subscriberArrTypeService.insertWithQuery(portMe.getPortId(), msisdn, requestType, current_status);
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-ORDER REVERSAL_MESSAGE Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");

			}

			/* start code for NPOT TERMINATE ACK request with jms out queue */
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("TERMINATE_ACK_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-TERMINATE_ACK Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				InitAck terminateAck = mapper.readValue(msg.getText(), InitAck.class);
				String requestId = terminateAck.getRequestId();
				String requestType = ReadConfigFile.getProperties().getProperty("TERMINATE_IN");
				current_status = 6;
				terminateTxDao.updateResponseCode(requestId, current_status, terminateAck.getResultCode(), requestType);

				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-TERMINATE_ACK Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}

			/**************************************************************
			 * SUSPENSION MODULE *
			 *************************************************************/

			// getting order NPOS(Suspension Recipient) soap request from external system
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("NPOS_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOS (Suspension Recipient Request) Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				NPOS[] listOfNPOS = mapper.readValue(msg.getText().toString(), NPOS[].class);
				String requestType = ReadConfigFile.getProperties().getProperty("SUS_RECIPIENT_IN");
				for (NPOS npos : listOfNPOS) {
					String msisdn = npos.getSubscriberNumber().substring(removeCountryCodeLimit);
					BillingResolution billingResolution = new BillingResolution();
					billingResolution.setTransactionId(npos.getReferenceId());
					billingResolution.setMsisdn(msisdn);
					// billing date convert
					DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
					DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
					LocalDateTime billDateTime = LocalDateTime.parse(npos.getBillDate(), inputFormatter);
					String billDate = billDateTime.format(outputFormatter);
					LocalDateTime dueDateTime = LocalDateTime.parse(npos.getDueDate(), inputFormatter);
					String dueDate = dueDateTime.format(outputFormatter);
					billingResolution.setBill_date(billDate);
					billingResolution.setDue_date(dueDate);
					billingResolution.setAmount(npos.getAmount());
					billingResolution.setComments(npos.getRemark());
					billingResolution.setAcc_no(npos.getRemark());
					billingResolution.setRequest_type(requestType);
					billingResolution.setStatus(1);
					billingResolution.setCreated_date(timestamp);
					billingResolution.setUpdated_date(timestamp);
					String reqType = ReadConfigFile.getProperties().getProperty("PORTME_IN");
					PortMe portMe = portMeService.getPortMeByMsisdn(reqType, msisdn);
					if (portMe != null) {
						billingResolution.setRno(portMe.getRno());
						billingResolution.setDno(portMe.getDno());
						billingResolution.setArea(portMe.getArea());
						billingResolution.setLast_area(portMe.getLast_area());
						billingResolution.setOriginal_op(portMe.getOriginal_op());
					}
					billingResolutionService.saveBillingResolution(billingResolution);
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOS (Suspension Recipient Request) Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}
			// END order NPOS soap request from external system
			/* start code for NPOSA_ACK_MESSAGE_TYPE request with jms out queue */
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("NPOSA_ACK_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOSA_ACK_SOAP Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				InitAck susAck = mapper.readValue(msg.getText(), InitAck.class);
				String requestId = susAck.getRequestId();
				String requestType = ReadConfigFile.getProperties().getProperty("SUS_RECIPIENT_IN");
				current_status = 2;
				billingResolutionDao.updateACK(requestId, requestType, susAck.getResultCode());
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOSA_ACK Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}
			// getting order NPOSAACK(Recipient Suspension Acknowledge) soap request from
			/* start code for NPOSAACKRsp Soap request with JMS out queue */
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("NPOSARsp_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOSARsp Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				RSP[] list = mapper.readValue(msg.getText(), RSP[].class);
				for (RSP nposrsp : list) {
					referenceId = nposrsp.getReferenceId();
					String requestType = ReadConfigFile.getProperties().getProperty("SUS_RECIPIENT_IN");
					_logger.info("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveMessage()-NPOSARsp Soap Message - start processing to update data with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					current_status = 2;
					if (nposrsp.getSubscriberResult().size() > 0) {
						for (SubscriberResult item : nposrsp.getSubscriberResult()) {
							BillingResolution billingResolution = new BillingResolution();
							billingResolution.setTransactionId(referenceId);
							billingResolution.setStatus(current_status);
							billingResolution.setRequest_type(requestType);
							billingResolution.setReason(item.getResultCode().toString());
							billingResolutionDao.updateNPOSA(billingResolution, sessionId, 0);
						}
					} else {
						// update portme
					}
					_logger.info("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveMessage()-NPOSARsp Soap Message - successfully processed to update data with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					if (current_status != 0) {
						portMeService.updatePortMeStatus(current_status, referenceId, requestType);
					}
					_logger.info("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveMessage()-NPOSARsp Soap Message - process end with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
				}
			}
			// external system
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("NPOSAACK_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOSAACK(Recipient Suspension Acknowledge) Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				NPOSAACK[] list = mapper.readValue(msg.getText().toString(), NPOSAACK[].class);
				String requestType = ReadConfigFile.getProperties().getProperty("SUS_RECIPIENT_IN");
				for (NPOSAACK nposaack : list) {
					BillingResolution billingResolution = new BillingResolution();
					billingResolution.setTransactionId(nposaack.getReferenceId());
					int currStatus = billingResolutionDao.getStatusReferenceId(nposaack.getReferenceId(), requestType);
					if (currStatus == 2) {
						billingResolution.setStatus(3);
					} else if (currStatus == 4 || currStatus == 5) {
						billingResolution.setStatus(5);
					}
					billingResolution.setRequest_type(requestType);
					billingResolution.setReason(nposaack.getResultCode());
					billingResolutionDao.updateNPOSAACK(billingResolution, sessionId);
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOSAACK(Recipient Suspension Acknowledge) Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}
			// END order NPOSAACK soap request from external system
			// getting order NPOSPR(Recipient Suspension Cancel) soap request from external
			// system
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("NPOSPR_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOSPR(Recipient Suspension Cancel) Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				NPOSPR[] listOfNPOSPR = mapper.readValue(msg.getText().toString(), NPOSPR[].class);
				String requestType = ReadConfigFile.getProperties().getProperty("SUS_RECIPIENT_IN");
				for (NPOSPR npospr : listOfNPOSPR) {
					BillingResolution billingResolution = new BillingResolution();
					billingResolution.setTransactionId(npospr.getReferenceId());
					billingResolution.setRequest_type(requestType);
					billingResolution.setStatus(7);
					billingResolution.setReason(npospr.getResultCode());
					billingResolution.setUpdated_date(timestamp);
					billingResolution.setCanceled_date(timestamp);
					billingResolutionDao.updateNPOSPR(billingResolution, sessionId);
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOSPR(Recipient Suspension Cancel) Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}
			// END order NPOSPR soap request from external system
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("NPOSTER_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOSTER Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				NPOSTER[] listOfNPOSTER = mapper.readValue(msg.getText().toString(), NPOSTER[].class);
				String requestType = ReadConfigFile.getProperties().getProperty("SUS_RECIPIENT_IN");
				for (NPOSTER nposter : listOfNPOSTER) {
					BillingResolution billingResolution = new BillingResolution();
					billingResolution.setTransactionId(nposter.getReferenceId());
					billingResolution.setRequest_type(requestType);
					billingResolution.setStatus(6);
					billingResolution.setReason(nposter.getResultCode());
					billingResolution.setUpdated_date(timestamp);
					billingResolutionDao.updateNPOSTER(billingResolution, sessionId);
					String msisdn = billingResolutionDao.getMsisdnByReferenceId(nposter.getReferenceId(), requestType);
					createTerminateRequest(nposter.getReferenceId(), msisdn);
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOSTER Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}

			/****************** SUSPENSION DONOR ********************/

			/* start code for NPOSRsp Soap request with JMS out queue */
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("NPOSRsp_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOSRsp Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				RSP[] list = mapper.readValue(msg.getText(), RSP[].class);
				for (RSP nposrsp : list) {
					referenceId = nposrsp.getReferenceId();
					String requestType = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
					current_status = 1;
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveMessage()-NPOSRsp Soap Message - trying to update data into db with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					if (nposrsp.getSubscriberResult().size() > 0) {
						for (SubscriberResult item : nposrsp.getSubscriberResult()) {
							BillingResolution billingResolution = new BillingResolution();
							billingResolution.setTransactionId(referenceId);
							billingResolution.setStatus(current_status);
							billingResolution.setRequest_type(requestType);
							billingResolution.setReason(item.getResultCode().toString());
							billingResolutionDao.updateNPOSRsp(billingResolution, sessionId);
						}
					} else {
						// update portme
					}
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveMessage()-NPOSRsp Soap Message - successfully updated data into db with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					_logger.info("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveMessage()-NPOSRsp Soap Message - process end with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
				}
			}
			/* start code for NPOS SUSPENSION ACK request with jms out queue */
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("NPOS_ACK_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOS_ACK Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				InitAck susAck = mapper.readValue(msg.getText(), InitAck.class);
				String requestId = susAck.getRequestId();
				String requestType = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
				current_status = 1;
				billingResolutionDao.updateACK(requestId, requestType, susAck.getResultCode());
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-SUSPENSION_ACK Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}
			// getting order NPOSA soap request from external system
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("NPOSA_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOSA Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				NPOSA[] list = mapper.readValue(msg.getText().toString(), NPOSA[].class);
				String requestType = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
				for (NPOSA nposa : list) {
					BillingResolution billingResolution = new BillingResolution();
					billingResolution.setTransactionId(nposa.getReferenceId());
					int currStatus = billingResolutionDao.getStatusReferenceId(nposa.getReferenceId(), requestType);
					if (currStatus == 1) {
						billingResolution.setStatus(2);
					} else if (currStatus == 3 || currStatus == 5) {
						billingResolution.setStatus(4);
					}
					billingResolution.setRequest_type(requestType);
					billingResolution.setReason(nposa.getResultCode());
					billingResolutionDao.updateNPOSA(billingResolution, sessionId, 0);
					_logger.info("got NPOSA reqeust from external system");
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOSA Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}
			// END order NPOSA soap request from external system

			/* start code for NPOSPR SUSPENSION Cancel ACK request with jms out queue */
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("NPOSPR_ACK_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOSPR_CANCEL_ACK Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				InitAck susAck = mapper.readValue(msg.getText(), InitAck.class);
				String requestId = susAck.getRequestId();
				String requestType = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
				current_status = 7;
				billingResolutionDao.updateACK(requestId, requestType, susAck.getResultCode());
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-SUSPENSION_CANCEL_ACK Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}

			/* start code for NPOSAACK_DONOR_ACK_ACK request with jms out queue */
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("NPOSAACK_ACK_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOSAACK_DONOR_ACK_ACK Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				InitAck susAck = mapper.readValue(msg.getText(), InitAck.class);
				String requestId = susAck.getRequestId();
				String requestType = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
				current_status = 3;
				billingResolutionDao.updateACK(requestId, requestType, susAck.getResultCode());
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOSAACK_DONOR_ACK_ACK Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}

			/* start code for NPOSAACKRsp Soap request with JMS out queue */
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("NPOSAACKRsp_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPOSAACKRsp Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				RSP[] list = mapper.readValue(msg.getText(), RSP[].class);
				for (RSP nposrsp : list) {
					referenceId = nposrsp.getReferenceId();
					String requestType = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveMessage()-NPOSAACKRsp Soap Message - trying to update data into db with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					current_status = 3;
					if (nposrsp.getSubscriberResult().size() > 0) {
						for (SubscriberResult item : nposrsp.getSubscriberResult()) {
							BillingResolution billingResolution = new BillingResolution();
							billingResolution.setTransactionId(referenceId);
							billingResolution.setStatus(current_status);
							billingResolution.setRequest_type(requestType);
							billingResolution.setReason(item.getResultCode().toString());
							billingResolutionDao.updateNPOSRsp(billingResolution, sessionId);
						}
					} else {
						// update portme
					}
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveMessage()-NPOSAACKRsp Soap Message - successfully updated data into db with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					if (current_status != 0) {
						portMeService.updatePortMeStatus(current_status, referenceId, requestType);
					}
					_logger.info("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveMessage()-NPOSAACKRsp Soap Message - process end with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
				}
			}

			/**************************************************************
			 * END SUSPENSION MODULE *
			 *************************************************************/

			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("NVP_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPV Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				NVP[] list = mapper.readValue(msg.getText().toString(), NVP[].class);
				for (NVP nvp : list) {
					try {
						String msisdn = nvp.getSubscriberNumber().substring(removeCountryCodeLimit);
						SubscriberInfoQueryDetail info = new SubscriberInfoQueryDetail();
						info.setReferenceId(nvp.getReferenceId());
						String nrhArea = numberPlanDao.getNRH(msisdn);
						String[] strList = nrhArea.split(",");
						String original_area = null;
						String original_op = null;
						if (strList.length == 2) {
							original_area = strList[0];
							original_op = strList[1];

						}
						info.setDnolsaId(original_area);
						info.setDonor(original_op);
						info.setMsisdn(msisdn);
						info.setTimeoutDate(nvp.getTimeoutDate());
						info.setRemark(nvp.getRemark());
						String isNVPResposneAuto = ReadConfigFile.getProperties().getProperty("IS_NVP_RESPONSE_AUTO");
						if (isNVPResposneAuto.equalsIgnoreCase("Y")) {
							String messageSenderTelco = ReadConfigFile.getProperties()
									.getProperty("MessageSenderTelco-ZOOM");
							String requestId = portMeService.getSynReqeustId(messageSenderTelco);
							info.setRequestId(requestId);
							String isNVPCorporateFlag = ReadConfigFile.getProperties()
									.getProperty("IS_NVP_CORPORATE_FLAG");
							info.setCorporate(isNVPCorporateFlag);
							info.setContractualObligation("Y");
							info.setActivateAging("Y");
							info.setOwnershipChange("Y");
							info.setOutstandingBill("Y");
							info.setUnderSubJudice("Y");
							info.setPortingProhibited("Y");
							info.setSimSwap("Y");
							info.setStatus(2);
						} else {
							info.setStatus(1);
							info.setRequestId(nvp.getRequestId());
						}
						info.setResult_code(0);
						info.setCreated_date(timestamp);
						info.setUpdated_date(timestamp);
						_logger.debug("[sessionId=" + sessionId
								+ "]: PortMeZ1Consumer.receiveMessage()-Trying to store NVP data into db");
						nvpService.save(info);
						_logger.debug("[sessionId=" + sessionId
								+ "]: PortMeZ1Consumer.receiveMessage()-successfully store NVP data into db");
						if (isNVPResposneAuto.equalsIgnoreCase("Y")) {
							_logger.debug("[sessionId=" + sessionId
									+ "]: PortMeZ1Consumer.receiveMessage()-trying to send NVPA into queue");
							xml = new NPOUtils().generateNVPA(info, 1);
							int interval = jmsProducer.sentIntoInternalInQ(xml, sessionId);
							if (interval == 1) {
								_logger.debug("[sessionId=" + sessionId
										+ "]: PortMeZ1Consumer.receiveMessage()-Successfully sent NVPA Response xml into jms queue with timestamp:["
										+ new Timestamp(System.currentTimeMillis()) + "]");
							}
						}
						// INSERT INTO SUBSCRIBER_VALIDATION
						// NEED TO BE IMPLEMENTED
						/*
						 * ZONE1 || ZONE2 RequestId || transactionId ReferenceId || transactionId
						 * DonorLSAID || DonorLSAID Donor || Donor SubscriberNumber || NumberRange
						 * TimeoutDate || zone2 me current date time se 5mins add karna hai Remark ||
						 * Comments Corporate || CorpPortFlag ContractualObligation ||
						 * ExistingObligations ActivateAging || SubsequentPortRestriction
						 * OwnershipChange || ChangeOfOwnership OutstandingBill || OutstandingDebt
						 * UnderSub-judice || UnderJudgement PortingProhibited || PortingProhibited
						 * SimSwap || SIMSwapOrReplacement
						 */
						_logger.info("got NPOTA reqeust from external system : " + nvp);
					} catch (Exception e) {
						_logger.error("[sessionId=" + sessionId
								+ "]: PortMeZ1Consumer.receiveMessage()-Exception occurs while processing NVP message, error: ["
								+ e.getMessage() + "]");
					}
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NPV Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			} else if (messageType.equals(ReadConfigFile.getProperties().getProperty("NVPA_ACK_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NVPA_ACK_MESSAGE_TYPE Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				InitAck initAck = mapper.readValue(msg.getText(), InitAck.class);
				_logger.debug("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NVPA_ACK_MESSAGE_TYPE Soap Message - trying to update with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				subscriberInfoQueryDetailDao.updateAckSubscriberInfoQueryDetail(initAck.getRequestId(), sessionId,
						initAck.getResultCode());
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NVPA_ACK_MESSAGE_TYPE Soap Message - successfully updated with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-NVPA_ACK_MESSAGE_TYPE Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}

			/**************************************************************
			 * START RECOVERY DB MODULE *
			 *************************************************************/

			// gettinng recovery DB partial response from external system
			else if (messageType
					.equals(ReadConfigFile.getProperties().getProperty("RECOVERY_DB_PARTIAL_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-RECOVERY_DB_PARTIAL Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				RecoveryDBResponse partialResponse = mapper.readValue(msg.getText(), RecoveryDBResponse.class);
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.recievedMessage recieved recovery DB Partial response from mch with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				RecoveryDB recoveryDB = new RecoveryDB();
				recoveryDB.setRequest_id(partialResponse.getRequestId());
				recoveryDB.setResult_code(partialResponse.getResultCode());
				recoveryDB.setFile_name(partialResponse.getDataFileName());
				int success = recoveryDBService.updateRecoveryDB(recoveryDB);
				if (success != 0) {
					_logger.info("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.recievedMessage updated recovery DB Partial response into db with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
				} else {
					_logger.info("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.recievedMessage fail to update recovery DB Partial response into db with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-RECOVERY_DB_PARTIAL Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}
			// gettinng recovery DB full response from external system
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("RECOVERY_DB_FULL_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-RECOVERY_DB_FULL Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				RecoveryDBResponse partialResponse = mapper.readValue(msg.getText(), RecoveryDBResponse.class);
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.recievedMessage recieved recovery DB Full response from mch with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				RecoveryDB recoveryDB = new RecoveryDB();
				recoveryDB.setRequest_id(partialResponse.getRequestId());
				recoveryDB.setResult_code(partialResponse.getResultCode());
				recoveryDB.setFile_name(partialResponse.getDataFileName());
				int success = recoveryDBService.updateRecoveryDB(recoveryDB);
				if (success != 0) {
					_logger.info("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.recievedMessage updated recovery DB Full response into db with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
				} else {
					_logger.info("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.recievedMessage failed to update recovery DB Full response into db with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-RECOVERY_DB_FULL Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}
			/* start code for recovery DB ACKNOWLEDGE */
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("RECOVERY_DB_ACK_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-RECOVERY_DB_ACK_MESSAGE Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				InitAck ack = mapper.readValue(msg.getText(), InitAck.class);
				String requestIds = ack.getRequestId();
				int success = recoveryDBService.updateAckOfRecoveryDB(requestIds, ack.getResultCode());
				if (success == 1) {
					_logger.info("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveMessage()-RECOVERY_DB_ACK_MESSAGE Soap Message - updated acknowledge of recovery db reqeust with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
				} else {
					_logger.info("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveMessage()-RECOVERY_DB_ACK_MESSAGE Soap Message - failed to update acknowledge of recovery db reqeust with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-RECOVERY_DB_ACK_MESSAGE Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}

			/**************************************************************
			 * END RECOVERY DB MODULE *
			 *************************************************************/

			/**************************************************************
			 * START MODULE SC ADN SC NOTICE ANSWER ACKNOWLEDGEMENT *
			 *************************************************************/
			/* start acknowledgement code for SC-NOTICE-ANSWER */
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("SCNOTICEANS_ACK_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-SC-NOTICE-ANSWER_ACK Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				InitAck scNoticeAck = mapper.readValue(msg.getText(), InitAck.class);
				_logger.info("got SC-NOTICE-ANSWER_ACK data from outqueue : " + scNoticeAck);
				String transactionId = scNoticeAck.getBatchId();
				broadcastHistoryService.updateBroadcastHistory(transactionId, scNoticeAck.getResultCode());
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-SC-NOTICE-ANSWER_ACK Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}
			/* start acknowledgement code for SD-NOTICE-ANSWER */
			else if (messageType.equals(ReadConfigFile.getProperties().getProperty("SDNOTICEANS_ACK_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-SD-NOTICE-ANSWER_ACK Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				InitAck sdNoticeAck = mapper.readValue(msg.getText(), InitAck.class);
				_logger.info("got SD-NOTICE-ANSWER_ACK data from outqueue : " + sdNoticeAck);
				String transactionId = sdNoticeAck.getBatchId();
				broadcastHistoryService.updateBroadcastHistory(transactionId, sdNoticeAck.getResultCode());
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveMessage()-SD-NOTICE-ANSWER_ACK Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}

			/**************************************************************
			 * END MODULE SC ADN SC NOTICE ANSWER ACKNOWLEDGEMENT *
			 *************************************************************/
		} catch (JsonProcessingException | JMSException |

				JSONException e) {
			_logger.error("Received Message with error : " + e.getMessage() + "RequestId - " + referenceId, e);
		}
		if (current_status != 0) {
			// going to update current status in port_tx table
		}
	}

	@SuppressWarnings("deprecation")
	@JmsListener(destination = "Z1ActivationQueue") // SC and SD data getting from activation queue
	public void receiveScSdTypeDetails(Message message) {
		String sessionId = Long.toString(System.currentTimeMillis());
		_logger.info("[sessionId=" + sessionId
				+ "]: PortMeZ1Consumer.receiveScSdTypeDetails()- Recieved Soap Message - process start with timestamp:["
				+ new Timestamp(System.currentTimeMillis()) + "]");
		int current_status = 0;
		String referenceId = null;
		String messageType = "";
		try {
			String removeCountryCode = ReadConfigFile.getProperties().getProperty("zone1-countrycode");
			int removeCountryCodeLimit = Integer.parseInt(removeCountryCode);
			PortMeTransactionDetails portMeTransaction = new PortMeTransactionDetails();
			TextMessage msg = (TextMessage) message;
			_logger.info("[sessionId=" + sessionId
					+ "]: PortMeZ1Consumer.receiveScSdTypeDetails()- Recieved Soap Message - process start with timestamp:["
					+ new Timestamp(System.currentTimeMillis()) + "]");
			ObjectMapper mapper = new ObjectMapper();
			JsonNode arrayNode = mapper.readTree(msg.getText().toString());
			if (arrayNode.isArray() && arrayNode.size() > 0) {
				JsonNode firstObject = arrayNode.get(0);
				if (firstObject.has("messageType")) {
					messageType = firstObject.get("messageType").asText();
				}
			}
			String xml = null;
			if (messageType.equals(ReadConfigFile.getProperties().getProperty("SD_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveScSdTypeDetails()- SD Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				SD[] list = mapper.readValue(msg.getText().toString(), SD[].class);
				String requestType = ReadConfigFile.getProperties().getProperty("PORTME_OUT");
				for (SD sdType : list) {
					_logger.info("Received Message SD xml from jms ActivationQueue : " + xml);
					current_status = 12;
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveScSdTypeDetails()- SD Soap Message - start updating data of SDInfo with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					if (sdType.getSDInfo().size() > 0) {
						for (SDInfo sdInfo : sdType.getSDInfo()) {
							try {
								referenceId = sdInfo.getReferenceId();
								portMeTransaction = new PortMeTransactionDetails();
								portMeTransaction.setReferenceId(referenceId);
								portMeTransaction.setStatus(current_status);
								portMeTransaction.setRequestType(requestType);
								portMeTransaction.setMsisdn(sdInfo.getSubscriberNumber());
								portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
								subscriberArrTypeService.updateSDByMsisdn(current_status,
										sdInfo.getSubscriberNumber().substring(removeCountryCodeLimit), requestType);
								portMeService.updatePortMeStatus(current_status, referenceId, requestType);
							} catch (Exception e) {
								_logger.error("[sessionId=" + sessionId
										+ "]: PortMeZ1Consumer.receiveScSdTypeDetails()- Recieved Soap Message - Exception occurs while processind SD soap of zone2 with MSISDN : ["
										+ sdInfo.getSubscriberNumber() + "], ERROR : " + e.getMessage());
							}
						}
					}
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveScSdTypeDetails()- SD Soap Message - successfully updated data of SDInfo with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					// going to convert into xml format
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveScSdTypeDetails()- SD Soap Message - trying to convert into xml with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					xml = new NPOUtils().convertSDTypeIntoXML(sdType);
					int interval = jmsProducer.sentIntoInternalInQ(xml, sessionId);
					if (interval == 1) {
						_logger.debug("[sessionId=" + sessionId
								+ "]: PortMeZ1Consumer.receiveScSdTypeDetails()- SD Soap Message - successfully sent SD Soap Response into queue with timestamp:["
								+ new Timestamp(System.currentTimeMillis()) + "]");
					} else {
						_logger.debug("[sessionId=" + sessionId
								+ "]: PortMeZ1Consumer.receiveScSdTypeDetails()- SD Soap Message - unable to sent SD Soap Response into queue with timestamp:["
								+ new Timestamp(System.currentTimeMillis()) + "]");
					}
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveScSdTypeDetails()- SD Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			} else if (messageType.equals(ReadConfigFile.getProperties().getProperty("SC_MESSAGE_TYPE"))) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveScSdTypeDetails()- SC Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
				SC[] list = mapper.readValue(msg.getText().toString(), SC[].class);
				String requestType = ReadConfigFile.getProperties().getProperty("PORTME_IN");
				for (SC scType : list) {
					current_status = 12;
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveScSdTypeDetails()- SC Soap Message - Start to proccessing to update data into database with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					if (scType.getSCInfo().size() > 0) {
						for (SCInfo scInfo : scType.getSCInfo()) {
							try {
								referenceId = scInfo.getReferenceId();
								portMeTransaction = new PortMeTransactionDetails();
								portMeTransaction.setReferenceId(referenceId);
								portMeTransaction.setStatus(current_status);
								portMeTransaction.setRequestType(requestType);
								portMeTransaction.setMsisdn(scInfo.getSubscriberNumber());
								portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
								subscriberArrTypeService.updateSCByMsisdn(current_status,
										scInfo.getSubscriberNumber().substring(removeCountryCodeLimit), requestType,
										sessionId);
								portMeService.updatePortMeStatus(current_status, referenceId, requestType);
							} catch (Exception e) {
								_logger.error("[sessionId=" + sessionId
										+ "]: PortMeZ1Consumer.receiveScSdTypeDetails()- Recieved Soap Message - Exception occurs while processind SD soap of zone2 with MSISDN : ["
										+ scInfo.getSubscriberNumber() + "], ERROR : " + e.getMessage());
							}
						}
					}
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveScSdTypeDetails()- SC Soap Message - Start to proccessed to update data into database with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					// going to convert into xml format
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveScSdTypeDetails()- SC Soap Message - trying to convert into xml with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					xml = new NPOUtils().convertSCTypeIntoXML(scType);
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ1Consumer.receiveScSdTypeDetails()- SC Soap Message - successfully converted into xml with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					int interval = jmsProducer.sentIntoInternalInQ(xml, sessionId);
					if (interval == 1) {
						_logger.debug("[sessionId=" + sessionId
								+ "]: PortMeZ1Consumer.receiveScSdTypeDetails()- SC Soap Message - Successfully sent SC Soap Response into queue with timestamp:["
								+ new Timestamp(System.currentTimeMillis()) + "]");
					} else {
						_logger.debug("[sessionId=" + sessionId
								+ "]: PortMeZ1Consumer.receiveScSdTypeDetails()- SC Soap Message - unable to sent SC Soap Response into queue with timestamp:["
								+ new Timestamp(System.currentTimeMillis()) + "]");
					}
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ1Consumer.receiveScSdTypeDetails()- SC Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}
		} catch (JMSException e) {
			handleException("PortMeZ1Consumer.receiveScSdTypeDetails()- Received Message with JMS error:", e,
					sessionId);
		} catch (JSONException e) {
			handleException("PortMeZ1Consumer.receiveScSdTypeDetails()- Received Message with JSON error:", e,
					sessionId);
		} catch (JsonMappingException e) {
			handleException("PortMeZ1Consumer.receiveScSdTypeDetails()- Received Message with JSON MAPPING error:", e,
					sessionId);
		} catch (JsonProcessingException e) {
			handleException("PortMeZ1Consumer.receiveScSdTypeDetails()- Received Message with JSON Processing error:",
					e, sessionId);
		}
	}

	private void handleException(String message, Exception e, String sessionId) {
		_logger.error("[sessionId=" + sessionId + "] " + message + " : " + e.getMessage(), e);
	}

	public void createTerminateRequest(String referenceId, String msisdns) {
		int current_status = 0;
		String requestId = null;
		TerminateSimTransactionDetails terminationTransaction = new TerminateSimTransactionDetails();
		TerminateSimMT terminateSIMMt = new TerminateSimMT();
		String reqType = ReadConfigFile.getProperties().getProperty("TERMINATE_IN");
		int resultCode = 0;
		int mch_type = 1;
		String area = null;
		try {
			TerminateSim terminationDetails = new TerminateSim();
			MasterNP masterNP = masterNPService.findByMsisdn(msisdns);
			if (masterNP != null) {
				MSISDNUIDType msisdnuId = new MSISDNUIDType();
				msisdnuId.setMsisdn(msisdns);
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
					// if (original_area != null) {
					// mch_type = numberPlanDao.getMCHTypeByArea(original_area);
					// }
					String messageSenderTelco = ReadConfigFile.getProperties().getProperty("MessageSenderTelco-ZOOM");
					// if (mch_type == 1) {
					requestId = portMeService.getSynReqeustId(messageSenderTelco);
					// } else {
					// requestId = referenceId;
					// }
					terminationDetails.setRequestId(requestId);
					_logger.info("received Terminate MSISDN request with requestId : " + requestId);
					terminationDetails.setArea(masterNP.getArea());
					terminationDetails.setOriginalCarrier(masterNP.getOrginal_carrier());
					terminationDetails.setRn(masterNP.getRn());
					terminationDetails.setService(masterNP.getService());
					terminationDetails.setStatus(current_status);
					terminationDetails.setReference_id(referenceId);
					terminationDetails.setRno(messageSenderTelco);
					terminationDetails.setTimeStamp(timestamp.toString());
					terminationDetails.setSource(messageSenderTelco);
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

						_logger.info(
								"trying to convert Termination MSISDN request into xml with requestId : " + requestId);
						String xml = new NPOUtils().convertJsonIntoTerminationSoap(terminationDetails, listMSISDN,
								mch_type, area, masterNP, true);
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
								terminateSimMTService.updateTerminateSIMMT(current_status,
										msisdnuid.getSubscriberNumber(), reqType, resultCode);
							}
							_logger.info("Successfully Received Terminate MSISDN request with requestId:" + requestId);
						} else {
							_logger.info("Terminate MSISDN request saved successfully but not able to send queue");
						}
					} else {
						_logger.info(
								"Terminate MSISDN request saved successfully but all MISDN number is not port before");
					}
				} else {
					_logger.info("Terminate MSISDN request is size zero with requestId :" + requestId);
				}
			} else {
				_logger.info("Terminate MSISDN request is size zero in master table with requestId :" + requestId);
			}
		} catch (Exception e) {
			_logger.error("Something is wrong with the system - " + e.getMessage());
		}
		if (current_status != 0) {
			terminateSimService.updateTerminateSIM(requestId, current_status, 0, mch_type);
		}
	}
}
