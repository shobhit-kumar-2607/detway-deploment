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
import com.megthink.gateway.dao.TerminateSimMTDao;
import com.megthink.gateway.model.BillingResolution;
import com.megthink.gateway.model.InitiateNumberReturn;
import com.megthink.gateway.model.MSISDNUIDType;
import com.megthink.gateway.model.MasterNP;
import com.megthink.gateway.model.NPCData;
import com.megthink.gateway.model.NonpaymentDisconnReq;
import com.megthink.gateway.model.NonpaymentDisconnResp;
import com.megthink.gateway.model.NpdAckResponse;
import com.megthink.gateway.model.NpdCancelRequest;
import com.megthink.gateway.model.NumReturnRequestWithRte;
import com.megthink.gateway.model.NumberRange;
import com.megthink.gateway.model.NumberRangeFlagged;
import com.megthink.gateway.model.PortCancelNotification;
import com.megthink.gateway.model.PortDeactWithRte;
import com.megthink.gateway.model.PortExecute;
import com.megthink.gateway.model.PortMe;
import com.megthink.gateway.model.PortMeTransactionDetails;
import com.megthink.gateway.model.PortRequest;
import com.megthink.gateway.model.PortRespWithFlag;
import com.megthink.gateway.model.ReconnectionAck;
import com.megthink.gateway.model.ReconnectionNotification;
import com.megthink.gateway.model.SubscriberInfoQuery;
import com.megthink.gateway.model.SubscriberInfoQueryDetail;
import com.megthink.gateway.model.TerminateSim;
import com.megthink.gateway.model.TerminateSimMT;
import com.megthink.gateway.model.TerminateSimTransactionDetails;
import com.megthink.gateway.producer.JmsProducer;
import com.megthink.gateway.repository.PortMeRepository;
import com.megthink.gateway.service.BillingResolutionService;
import com.megthink.gateway.service.MasterNPService;
import com.megthink.gateway.service.PortMeService;
import com.megthink.gateway.service.PortMeTransactionDetailsService;
import com.megthink.gateway.service.SubscriberArrTypeService;
import com.megthink.gateway.service.SubscriberInfoQueryDetailService;
import com.megthink.gateway.service.TerminateSimMTService;
import com.megthink.gateway.service.TerminateSimService;
import com.megthink.gateway.service.TerminateSimTransactionDetailsService;
import com.megthink.gateway.utils.NPOUtils;
import com.megthink.gateway.utils.PortMeUtils;
import com.megthink.gateway.utils.ReadConfigFile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PortMeZ2Consumer {

	private static final Logger _logger = LoggerFactory.getLogger(PortMeZ2Consumer.class);

	Timestamp timestamp = new Timestamp(System.currentTimeMillis());

	@Autowired
	private PortMeService portMeService;
	@Autowired
	private PortMeRepository portMeRepository;
	@Autowired
	private PortMeTransactionDetailsService portMeTransactionService;
	@Autowired
	private TerminateSimService terminateSimService;
	@Autowired
	private TerminateSimTransactionDetailsService terminateTransactionService;
	@Autowired
	private TerminateSimMTService terminateSimMTService;
	@Autowired
	private TerminateSimMTDao terminateSimMTDao;
	@Autowired
	private JmsProducer jmsProducer;
	@Autowired
	private MasterNPService masterNPService;
	@Autowired
	private SubscriberArrTypeService subscriberArrTypeService;
	@Autowired
	private BillingResolutionService billingResolutionService;
	@Autowired
	private BillingResolutionDao billingResolutionDao;
	@Autowired
	private NumberPlanDao numberPlanDao;
	@Autowired
	private PortMeDao portMeDao;
	@Autowired
	private SubscriberInfoQueryDetailService nvpService;

	@SuppressWarnings("deprecation")
	@JmsListener(destination = "Z2INQueue")
	public void receiveMessage(Message message) {
		String sessionId = Long.toString(System.currentTimeMillis());
		_logger.info("[sessionId=" + sessionId
				+ "]: PortMeZ2Consumer.receiveMessage()- Recieved Z2 Soap Message - process start with timestamp:["
				+ new Timestamp(System.currentTimeMillis()) + "]");
		System.out.println("Received Z2 Message from queue : " + message.toString());
		int current_status = 0;
		String requestId = null;
		try {
			PortMeTransactionDetails portMeTransaction = new PortMeTransactionDetails();
			TextMessage msg = (TextMessage) message;
			_logger.debug("[sessionId=" + sessionId
					+ "]: PortMeZ2Consumer.receiveMessage()- Recieved Z2 Soap Message :[" + msg.getText() + "]");
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
			NPCData npcData = objectMapper.readValue(msg.getText(), NPCData.class);
			/* start code for NPO SOAP request with jms out queue */
			if (npcData.getMessageHeader().getMessageID() == 1002) {
				PortRequest portRequest = npcData.getNPCMessage().getPortRequest();
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage( )- Start processing (NPO MessageId : 1001) with timestamp :["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				if (portRequest.getDocumentFileName() != null) {
					byte[] decodedBytes = Base64.getDecoder().decode(portRequest.getDocumentFileName());
					// Write the bytes to a file
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ2Consumer.receiveMessage( )- trying store document file into our system with timestamp :["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					String filePath = System.getProperty("user.home") + "/"
							+ ReadConfigFile.getProperties().getProperty("upload.document") + "/"
							+ npcData.getMessageHeader().getTransactionID() + "OUT.pdf";

					try (FileOutputStream fos = new FileOutputStream(filePath)) {
						fos.write(decodedBytes);
						_logger.debug("[sessionId=" + sessionId
								+ "]: PortMeZ2Consumer.receiveMessage( )- successfully stored document file into our system with timestamp :["
								+ new Timestamp(System.currentTimeMillis()) + "]");
					} catch (IOException e) {
						_logger.error("[sessionId=" + sessionId
								+ "]: PortMeZ2Consumer.receiveMessage( )- Exception occurs while trying to store document into our system with timestamp :["
								+ new Timestamp(System.currentTimeMillis()) + "]");
					}
				}
				String requestType = ReadConfigFile.getProperties().getProperty("PORTME_OUT");
				current_status = 3;
				requestId = portMeRepository.getSynReqeustId(npcData.getNPCMessage().getPortRequest().getRecipient());
				String referenceId = npcData.getMessageHeader().getTransactionID();
				portMeTransaction.setReferenceId(referenceId);
				portMeTransaction.setStatus(current_status);
				portMeTransaction.setRequestType(requestType);
				// _logger.debug("[sessionId=" + sessionId
				// + "]: PortMeZ2Consumer.receiveMessage( )- Trying to get mch_type with
				// timestamp:["
				// + new Timestamp(System.currentTimeMillis()) + "]");
				int mch_type = 2;// numberPlanDao.getMCHTypeByArea(npcData.getNPCMessage().getPortRequest().getDonorLSAID());
				// _logger.debug("[sessionId=" + sessionId
				// + "]: PortMeZ2Consumer.receiveMessage( )- successfully to get mch_type[" +
				// mch_type
				// + "] with timestamp:[" + new Timestamp(System.currentTimeMillis()) + "]");
				PortMe portMe = new PortMe();
				portMe.setRequestId(requestId);
				portMe.setReferenceId(referenceId);
				portMe.setTimeStamp(npcData.getMessageHeader().getMsgCreateTimeStamp());
				portMe.setDno(npcData.getNPCMessage().getPortRequest().getDonor());
				portMe.setArea(npcData.getNPCMessage().getPortRequest().getRecipientLSAID());
				// portMe.setRn();
				portMe.setService(npcData.getNPCMessage().getPortRequest().getAccountPayType());
				portMe.setMch(mch_type);
				portMe.setStatus(current_status);
				portMe.setRequest_type(requestType);
				portMe.setOrderDate(timestamp.toString());
				if (portRequest.getCorpPortFlag().equalsIgnoreCase("Y")) {
					portMe.setDataType(2);
				} else {
					portMe.setDataType(1);
				}
				portMe.setOriginalCarrier(npcData.getNPCMessage().getPortRequest().getDonor());
				portMe.setRno(npcData.getNPCMessage().getPortRequest().getRecipient());
				portMe.setCustomerRequestTime(timestamp.toString());
				portMe.setCompanyCode(npcData.getNPCMessage().getPortRequest().getPortingCode());
				portMe.setLast_area(npcData.getNPCMessage().getPortRequest().getDonorLSAID());
				PortMe portMeDetails = portMeService.findByReferenceId(referenceId);
				if (portMeDetails != null) {
					portMe.setOriginal_area(portMeDetails.getOriginal_area());
					portMe.setOriginal_op(portMeDetails.getOriginal_op());
					portMe.setOrderType(portMeDetails.getOrderType());
					portMe.setPartnerID(portMeDetails.getPartnerID());
					portMe.setRn(portMeDetails.getRn());
				} else {
					if (portRequest.getNumberRange().size() > 0) {
						String nrhArea = numberPlanDao.getNRH(portRequest.getNumberRange().get(0).getNumberFrom());// array
						String[] strList = nrhArea.split(",");
						String original_area = null;
						String original_op = null;
						if (strList.length == 2) {
							original_area = strList[0];
							original_op = strList[1];

						}
						portMe.setOriginal_area(original_area);
						portMe.setOriginal_op(original_op);
						String routing_info = numberPlanDao
								.getRouteInfoByMsisdn(portRequest.getNumberRange().get(0).getNumberFrom());
						portMe.setRn(routing_info);
					}
				}
				portMe.setSource(npcData.getMessageHeader().getSender());

				_logger.debug("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage( )- Trying to store NPO details into db with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				PortMe port = portMeService.savePortMe(portMe);
				if (port.getPortId() != 0) {
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ2Consumer.receiveMessage( )- successfully to store NPO details into db with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ2Consumer.receiveMessage( )- Trying to store NPO list of plan details into mt_db table with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					for (NumberRange numberRange : portRequest.getNumberRange()) {
						subscriberArrTypeService.insertWithQuery(port.getPortId(), numberRange.getNumberFrom(),
								requestType, current_status);
						portMeTransaction.setMsisdn(numberRange.getNumberFrom());
						portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
					}
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ2Consumer.receiveMessage( )- successfully to store NPO list of plan details into mt_db table with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage( )- End processing with (NPO) MessageId :["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}
			/* end NPO SOAP Request */
			/* start code for NPOA Soap request with JMS out queue */
			else if (npcData.getMessageHeader().getMessageID() == 1004) {
				PortRespWithFlag npoa = npcData.getNPCMessage().getPortRespWithFlag();
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage( )- Start processing (NPOA MessageId : 1004) with timestamp :["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				requestId = npcData.getMessageHeader().getTransactionID();
				String requestType = ReadConfigFile.getProperties().getProperty("PORTME_IN");
				current_status = 9;
				portMeTransaction.setReferenceId(requestId);
				portMeTransaction.setStatus(current_status);
				portMeTransaction.setRequestType(requestType);
				for (NumberRangeFlagged ranges : npoa.getNumberRangeFlagged()) {
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ2Consumer.receiveMessage( )- trying to update port_mt with (NPOA) MSISDN :["
							+ ranges.getNumberFrom() + "]");
					// portMeTransactionService.savePortMeTransactionDetail(portMeTransaction,
					// npoa.getSubscriberResult());
					String orderedTransferTime = null;
					if (npoa.getPortTime() != null) {
						orderedTransferTime = null;// npoa.getPortTime();
					}
					portMeService.updateNPOARequest(current_status, ranges.getNumberFrom(), requestType, 0,
							orderedTransferTime);
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ2Consumer.receiveMessage( )- successfully to updated with (NPO) MSISDN :["
							+ ranges.getNumberFrom() + "]");
				}
				portMeService.updatePortMeStatus(current_status, requestId, requestType);
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage( )- End processing with (NPOA) timestamp :["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				// start code for PORTOUT
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage( )- Start processing with (NPOARsp) timestamp :["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				String portME_PUT = ReadConfigFile.getProperties().getProperty("PORTME_OUT");
				_logger.debug("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage( )- Trying to check if already exist with (NPOARsp) timestamp :["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				int isExist = portMeDao.isExistNPO(requestId, portME_PUT);
				if (isExist == 0) {
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ2Consumer.receiveMessage( )- Trying to insert NPO PORTME_OUT (NPOARsp) timestamp :["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					String referenceId = npcData.getMessageHeader().getTransactionID();
					portMeTransaction.setReferenceId(referenceId);
					portMeTransaction.setStatus(current_status);
					portMeTransaction.setRequestType(portME_PUT);
					int mch_type = numberPlanDao.getMCHTypeByArea(npoa.getDonorLSAID());
					PortMe portMe = new PortMe();
					portMe.setRequestId(requestId);
					portMe.setReferenceId(referenceId);
					portMe.setTimeStamp(npcData.getMessageHeader().getMsgCreateTimeStamp());
					portMe.setDno(npoa.getDonor());
					portMe.setArea(npoa.getRecipientLSAID());
					if (npoa.getCorpPortFlag().equals("Y")) {
						portMe.setDataType(2);
					} else {
						portMe.setDataType(1);
					}
					portMe.setMch(mch_type);
					portMe.setStatus(current_status);
					portMe.setRequest_type(portME_PUT);
					portMe.setOrderDate(timestamp.toString());
					portMe.setOriginalCarrier(npoa.getDonor());
					portMe.setRno(npoa.getRecipient());
					portMe.setCustomerRequestTime(timestamp.toString());
					portMe.setLast_area(npcData.getMessageHeader().getlSAID());
					portMe.setSource(npcData.getMessageHeader().getSender());
					PortMe portMeDetails = portMeService.findByReferenceId(referenceId);
					if (portMeDetails != null) {
						portMe.setOriginal_area(portMeDetails.getOriginal_area());
						portMe.setOriginal_op(portMeDetails.getOriginal_op());
						portMe.setOrderType(portMeDetails.getOrderType());
						portMe.setPartnerID(portMeDetails.getPartnerID());
						portMe.setRn(portMeDetails.getRn());
					} else {
						if (npoa.getNumberRangeFlagged().size() > 0) {
							String nrhArea = numberPlanDao.getNRH(npoa.getNumberRangeFlagged().get(0).getNumberFrom());// array
							String[] strList = nrhArea.split(",");
							String original_area = null;
							String original_op = null;
							if (strList.length == 2) {
								original_area = strList[0];
								original_op = strList[1];

							}
							portMe.setOriginal_area(original_area);
							portMe.setOriginal_op(original_op);
							String routing_info = numberPlanDao
									.getRouteInfoByMsisdn(npoa.getNumberRangeFlagged().get(0).getNumberFrom());
							portMe.setRn(routing_info);
						}
					}
					// portMe.setCompanyCode();
					PortMe port = portMeService.savePortMe(portMe);
					if (port.getPortId() != 0) {
						for (NumberRangeFlagged numberRange : npoa.getNumberRangeFlagged()) {
							subscriberArrTypeService.insertWithQuery(port.getPortId(), numberRange.getNumberFrom(),
									portME_PUT, current_status);
							portMeTransaction.setMsisdn(numberRange.getNumberFrom());
							portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
						}
					}
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ2Consumer.receiveMessage( )- Successfully to inserted NPO PORTME_OUT (NPOARsp) timestamp :["
							+ new Timestamp(System.currentTimeMillis()) + "]");
				} else {
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ2Consumer.receiveMessage( )- already exist with (NPOARsp) timestamp :["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ2Consumer.receiveMessage( )- trying to update port_mt with (NPOARsp) timestamp :["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					for (NumberRangeFlagged ranges : npoa.getNumberRangeFlagged()) {
						// portMeTransactionService.savePortMeTransactionDetail(portMeTransaction,
						// npoa.getSubscriberResult());
						portMeService.updateNPOARsp(current_status, ranges.getNumberFrom(), portME_PUT, 0, null);
						// need to add order transfer time
					}
					_logger.debug("[sessionId=" + sessionId
							+ "]: PortMeZ2Consumer.receiveMessage( )- Successfully updated port_mt with (NPOARsp) timestamp :["
							+ new Timestamp(System.currentTimeMillis()) + "]");
					portMeService.updatePortMeStatus(current_status, requestId, portME_PUT);
					_logger.info("[sessionId=" + sessionId
							+ "]: PortMeZ2Consumer.receiveMessage( )- End processing with (NPOARsp) timestamp :["
							+ new Timestamp(System.currentTimeMillis()) + "]");
				}
			}
			/* end code for NPOA Request */

			/* start code for NPOT Soap (TERMINATE SIM) request with JMS out queue */
			else if (npcData.getMessageHeader().getMessageID() == 5002) {
				NumReturnRequestWithRte npot = npcData.getNPCMessage().getNumReturnRequestWithRte();
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage( )- Start processing (Terminatation MessageId : 5002) Request with timestamp :["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				String requestType = ReadConfigFile.getProperties().getProperty("TERMINATE_OUT");
				requestId = PortMeUtils.randomUniqueUUID();
				int resultCode = 0;
				current_status = 3;
				TerminateSim terminationDetails = new TerminateSim();
				MasterNP masterNP = null;
				masterNP = masterNPService.findByMsisdn(npot.getNumberRange().getNumberFrom());
				String nrhArea = numberPlanDao.getNRH(npot.getNumberRange().getNumberFrom());// in array
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
				terminationDetails.setReference_id(npcData.getMessageHeader().getTransactionID());
				terminationDetails.setRequestId(npcData.getMessageHeader().getTransactionID());
				terminationDetails.setSource(npcData.getMessageHeader().getSender());
				terminationDetails.setTimeStamp(timestamp.toString());
				terminationDetails.setStatus(current_status);
				terminationDetails.setMch(2);
				if (masterNP != null) {
					terminationDetails.setArea(masterNP.getArea());
					terminationDetails.setOriginalCarrier(masterNP.getOrginal_carrier());
					terminationDetails.setRn(masterNP.getRn());
					terminationDetails.setService(masterNP.getService());
				}
				terminationDetails.setRequestType(requestType);
				_logger.debug("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage( )- trying to store (Terminatation) details into db Request with timestamp :["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				TerminateSim terminateSim = terminateSimService.saveTerminateSim(terminationDetails);
				// for (NumberRange item : npot.getNumberRange()) {
				TerminateSimTransactionDetails terminationTransaction = new TerminateSimTransactionDetails();
				terminationTransaction.setRequestId(npcData.getMessageHeader().getTransactionID());
				terminationTransaction.setStatus(current_status);
				terminationTransaction.setRequestType(requestType);
				terminationTransaction.setMsisdn(npot.getNumberRange().getNumberFrom());
				terminateTransactionService.saveTerminateSimTransactionDetails(terminationTransaction);
				TerminateSimMT terminateSIMMt = new TerminateSimMT();
				terminateSIMMt.setTerminateId(terminateSim.getTerminateId());
				terminateSIMMt.setRequest_type(requestType);
				terminateSIMMt.setStatus(current_status);
				terminateSIMMt.setSubscriberNumber(npot.getNumberRange().getNumberFrom());
				terminateSIMMt.setResultCode(resultCode);
				terminateSimMTService.saveTerminateSimMT(terminateSIMMt);
				// }
				_logger.debug("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage( )- successfully to store (Terminatation) details into db Request with timestamp :["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage( )- End processing (Terminatation) Request with timestamp :["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}
			/* end code for NPOT */

			// getting order cancellation soap request from external system
			else if (npcData.getMessageHeader().getMessageID() == 3002) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMez2Consumer.receiveMessage()-PORT CANCEL Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				PortCancelNotification orderCancel = npcData.getNPCMessage().getPortCancelNotification();
				_logger.info("got ORDER cancel reqeust from external system : " + orderCancel);
				requestId = PortMeUtils.randomUniqueUUID();
				String curReqType = ReadConfigFile.getProperties().getProperty("CANCEL_OUT");
				String preReqType = ReadConfigFile.getProperties().getProperty("CANCEL_IN");
				current_status = 1;
				for (NumberRange ranges : orderCancel.getNumberRange()) {
					String msisdn = ranges.getNumberFrom();
					portMeTransaction = new PortMeTransactionDetails();
					portMeTransaction.setReferenceId(requestId);
					portMeTransaction.setStatus(current_status);
					portMeTransaction.setRequestType(curReqType);
					portMeTransaction.setMsisdn(msisdn);
					portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
					portMeService.cancelOrderByMsisdn(current_status, msisdn, preReqType, curReqType);
					_logger.info("[sessionId=" + sessionId
							+ "]: PortMeZ2Consumer.receiveMessage()-PORT CANCEL Soap Message - process end with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
				}
			}
			// end order cancellation soap request from external system

			/**************************************************************
			 * START SUSPENSION MODULE *
			 *************************************************************/

			// getting order NPOS(Suspension Recipient Request) soap request from external
			// system
			else if (npcData.getMessageHeader().getMessageID() == 7002) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage()-NPOS (Suspension Recipient Request MessageId : 7002) Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				NonpaymentDisconnReq npos = npcData.getNPCMessage().getNonpaymentDisconnReq();
				String susRecipientIn = ReadConfigFile.getProperties().getProperty("SUS_RECIPIENT_IN");
				for (NumberRange ranges : npos.getNumberRange()) {
					BillingResolution billingResolution = new BillingResolution();
					billingResolution.setTransactionId(npcData.getMessageHeader().getTransactionID());
					billingResolution.setMsisdn(ranges.getNumberFrom());
					String requestType = ReadConfigFile.getProperties().getProperty("PORTME_IN");
					PortMe portMe = portMeService.getPortMeByMsisdn(requestType, ranges.getNumberFrom());
					if (portMe != null) {
						billingResolution.setRno(portMe.getRno());
						billingResolution.setDno(portMe.getDno());
						billingResolution.setArea(portMe.getArea());
						billingResolution.setLast_area(portMe.getLast_area());
						billingResolution.setOriginal_op(portMe.getOriginal_op());
					}
					DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
					DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
					LocalDateTime billDateTime = LocalDateTime.parse(npos.getBillDate(), inputFormatter);
					String billDate = billDateTime.format(outputFormatter);
					LocalDateTime dueDateTime = LocalDateTime.parse(npos.getBillDueDate(), inputFormatter);
					String dueDate = dueDateTime.format(outputFormatter);
					billingResolution.setBill_date(billDate);
					billingResolution.setDue_date(dueDate);
					billingResolution.setAmount(npos.getBillAmount());
					billingResolution.setComments(npos.getComments());
					billingResolution.setRequest_type(susRecipientIn);
					billingResolution.setAcc_no(npos.getComments());
					billingResolution.setStatus(1);
					billingResolution.setCreated_date(timestamp);
					billingResolution.setUpdated_date(timestamp);
					billingResolutionService.saveBillingResolution(billingResolution);
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage()-NPOS (Suspension Recipient Request MessageId : 7002) Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			} else if (npcData.getMessageHeader().getMessageID() == 7006) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage()-NPOSAACK(Recipient Suspension Acknowledge MessageId : 7006) Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				NpdAckResponse nposa = npcData.getNPCMessage().getNpdAckResponse();
				// for (NumberRange ranges : nposa.getNumberRange()) {
				String susRecipientIn = ReadConfigFile.getProperties().getProperty("SUS_RECIPIENT_IN");
				BillingResolution billingResolution = new BillingResolution();
				billingResolution.setTransactionId(npcData.getMessageHeader().getTransactionID());
				billingResolution.setRequest_type(susRecipientIn);
				billingResolution.setStatus(3);
				billingResolution.setReason(nposa.getReasonCode());
				billingResolutionDao.updateNPOSAACK(billingResolution, sessionId);
				// }
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage()-NPOSAACK(Recipient Suspension Acknowledge) Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			} else if (npcData.getMessageHeader().getMessageID() == 7014) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage()-NPOSAACK(MessageId : 7014) Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				ReconnectionAck npos = npcData.getNPCMessage().getReconnectionAck();
				String susRecipientIn = ReadConfigFile.getProperties().getProperty("SUS_RECIPIENT_IN");
				for (NumberRange ranges : npos.getNumberRange()) {
					String transactionId = npcData.getMessageHeader().getTransactionID();
					BillingResolution billingResolution = new BillingResolution();
					billingResolution.setTransactionId(transactionId);
					billingResolution.setRequest_type(susRecipientIn);
					billingResolution.setStatus(5);
					billingResolution.setReason(ranges.getReasonCode());
					billingResolution.setUpdated_date(timestamp);
					billingResolutionDao.updateNPOSAACK(billingResolution, sessionId);
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage()-NPOSAACK(MessageId : 7014) Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			} else if (npcData.getMessageHeader().getMessageID() == 7020) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage()-NPOSTER(MessageId : 7020) Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				InitiateNumberReturn npos = npcData.getNPCMessage().getInitiateNumberReturn();
				String susRecipientIn = ReadConfigFile.getProperties().getProperty("SUS_RECIPIENT_IN");
				for (NumberRange ranges : npos.getNumberRange()) {
					String transactionId = npcData.getMessageHeader().getTransactionID();
					BillingResolution billingResolution = new BillingResolution();
					billingResolution.setTransactionId(transactionId);
					billingResolution.setRequest_type(susRecipientIn);
					billingResolution.setStatus(6);
					billingResolution.setReason(ranges.getReasonCode());
					billingResolution.setUpdated_date(timestamp);
					billingResolutionDao.updateNPOSTER(billingResolution, sessionId);
					String msisdn = billingResolutionDao.getMsisdnByReferenceId(transactionId, susRecipientIn);
					createTerminateRequest(transactionId, msisdn);
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage()-NPOSTER(MessageId : 7020) Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			} else if (npcData.getMessageHeader().getMessageID() == 7008) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage()-NPOSPR(Recipient Suspension Cancel MessageId : 7008) Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				NpdCancelRequest nposcancel = npcData.getNPCMessage().getNpdCancelRequest();
				String susRecipientIn = ReadConfigFile.getProperties().getProperty("SUS_RECIPIENT_IN");
				// for (NumberRange ranges : nposcancel.getNumberRange()) {
				BillingResolution billingResolution = new BillingResolution();
				billingResolution.setTransactionId(npcData.getMessageHeader().getTransactionID());
				billingResolution.setRequest_type(susRecipientIn);
				billingResolution.setStatus(7);
				if (nposcancel.getNumberRange().size() > 0) {
					billingResolution.setReason(nposcancel.getNumberRange().get(0).getReasonCode());
				}
				billingResolution.setUpdated_date(timestamp);
				billingResolution.setCanceled_date(timestamp);
				billingResolutionDao.updateNPOSPR(billingResolution, sessionId);
				// }
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage()-NPOSPR(Recipient Suspension Cancel) Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			} else if (npcData.getMessageHeader().getMessageID() == 7004) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage(MessageId : 7004)-NPOSA Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				NonpaymentDisconnResp nposa = npcData.getNPCMessage().getNonpaymentDisconnResp();
				// for (NumberRange ranges : nposa.getNumberRange()) {
				String susDonorIn = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
				BillingResolution billingResolution = new BillingResolution();
				billingResolution.setTransactionId(npcData.getMessageHeader().getTransactionID());
				billingResolution.setRequest_type(susDonorIn);
				billingResolution.setStatus(2);
				billingResolution.setReason(nposa.getReasonCode());
				billingResolutionDao.updateNPOSA(billingResolution, sessionId, 0);
				// }
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage(MessageId : 7004)-NPOSA Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			} else if (npcData.getMessageHeader().getMessageID() == 7012) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage()-NPOSA(MessageId : 7012) Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				ReconnectionNotification npos = npcData.getNPCMessage().getReconnectionNotification();
				String susRecipientIn = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
				for (NumberRange ranges : npos.getNumberRange()) {
					String transactionId = npcData.getMessageHeader().getTransactionID();
					BillingResolution billingResolution = new BillingResolution();
					billingResolution.setTransactionId(transactionId);
					billingResolution.setRequest_type(susRecipientIn);
					billingResolution.setStatus(4);
					billingResolution.setReason(ranges.getReasonCode());
					billingResolution.setUpdated_date(timestamp);
					billingResolutionDao.updateNPOSA(billingResolution, sessionId, 0);
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage()-NPOSA(MessageId : 7012) Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}

			/**************************************************************
			 * END SUSPENSION MODULE StART NVP MODULE
			 *************************************************************/

			else if (npcData.getMessageHeader().getMessageID() == 1601) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage()-NPV Soap Message - process start with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				SubscriberInfoQuery nvpItem = npcData.getNPCMessage().getSubscriberInfoQuery();
				for (NumberRange item : nvpItem.getNumberRange()) {
					try {
						String msisdn = item.getNumberTo();
						SubscriberInfoQueryDetail info = new SubscriberInfoQueryDetail();
						info.setReferenceId(npcData.getMessageHeader().getTransactionID());
						info.setDnolsaId(npcData.getMessageHeader().getlSAID());
						String nrhArea = numberPlanDao.getNRH(msisdn);
						String[] strList = nrhArea.split(",");
						// String original_area = null;
						String original_op = null;
						if (strList.length == 2) {
							// original_area = strList[0];
							original_op = strList[1];

						}
						// info.setDnolsaId(original_area);
						info.setDonor(original_op);
						info.setMsisdn(msisdn);
						LocalDateTime currentTime = LocalDateTime.now();
						String minutes = ReadConfigFile.getProperties().getProperty("subscriber-val-minutes");
						LocalDateTime newTime = currentTime.plusMinutes(Integer.parseInt(minutes));
						info.setTimeoutDate(newTime.toString());
						info.setRemark(nvpItem.getComments());
						String isNVPResposneAuto = ReadConfigFile.getProperties().getProperty("IS_NVP_RESPONSE_AUTO");
						if (isNVPResposneAuto.equalsIgnoreCase("Y")) {
							info.setRequestId(npcData.getMessageHeader().getTransactionID());
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
							info.setRequestId(npcData.getMessageHeader().getTransactionID());
						}
						info.setResult_code(0);
						info.setCreated_date(timestamp);
						info.setUpdated_date(timestamp);
						_logger.debug("[sessionId=" + sessionId
								+ "]: PortMeZ2Consumer.receiveMessage()-Trying to store NVP data into db");
						nvpService.save(info);
						_logger.debug("[sessionId=" + sessionId
								+ "]: PortMeZ2Consumer.receiveMessage()-successfully store NVP data into db");
						if (isNVPResposneAuto.equalsIgnoreCase("Y")) {
							_logger.debug("[sessionId=" + sessionId
									+ "]: PortMeZ2Consumer.receiveMessage()-trying to send NVPA into queue");
							String xml = new NPOUtils().generateNVPA(info, 2);
							int interval = jmsProducer.sentIntoInternalInQ(xml, sessionId);
							if (interval == 1) {
								_logger.debug("[sessionId=" + sessionId
										+ "]: PortMeZ2Consumer.receiveMessage()-Successfully sent NVPA Response xml into jms queue with timestamp:["
										+ new Timestamp(System.currentTimeMillis()) + "]");
							}
						}
						// INSERT INTO SUBSCRIBER_VALIDATION
						// TODO : NEED TO BE IMPLEMENTED
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
					} catch (Exception e) {
						_logger.error("[sessionId=" + sessionId
								+ "]: PortMeZ2Consumer.receiveMessage()-Exception occurs while processing NVP message, error: ["
								+ e.getMessage() + "]");
					}
				}
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveMessage()-NPV Soap Message - process end with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
			}
		} catch (JsonProcessingException | JMSException | JSONException e) {
			_logger.error("Received Message with error : " + e.getMessage() + "RequestId - " + requestId, e);
		}
		if (current_status != 0) {
			// going to update current status in port_tx table
		}
	}

	@SuppressWarnings("deprecation")
	@JmsListener(destination = "Z2ActivationQueue") // SC and SD data getting from activation queue
	public void receiveSCSDMessage(Message message) {
		String sessionId = Long.toString(System.currentTimeMillis());
		int current_status = 0;
		String requestId = null;
		try {
			PortMeTransactionDetails portMeTransaction = new PortMeTransactionDetails();
			TextMessage msg = (TextMessage) message;
			_logger.info("[sessionId=" + sessionId
					+ "]: PortMeZ2Consumer.receiveSCSDMessage()- Recieved Z2 Soap Message - process start with timestamp:["
					+ new Timestamp(System.currentTimeMillis()) + "]");
			ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
			NPCData npcData = objectMapper.readValue(msg.getText(), NPCData.class);
			if (npcData.getMessageHeader().getMessageID() == 1006) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveSCSDMessage()- Recieved Z2 Soap Message - start processing (SD MessageId : 1006) with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				PortExecute sdType = npcData.getNPCMessage().getPortExecute();
				if (sdType != null) {
					current_status = 12;
					String requestType = ReadConfigFile.getProperties().getProperty("PORTME_OUT");
					for (NumberRange sdInfo : sdType.getNumberRange()) {
						try {
							requestId = npcData.getMessageHeader().getTransactionID();
							portMeTransaction = new PortMeTransactionDetails();
							portMeTransaction.setReferenceId(requestId);
							portMeTransaction.setStatus(current_status);
							portMeTransaction.setRequestType(requestType);
							portMeTransaction.setMsisdn(sdInfo.getNumberFrom());
							portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
							subscriberArrTypeService.updateSDByMsisdn(current_status, sdInfo.getNumberFrom(),
									requestType);
						} catch (Exception e) {
							_logger.error("[sessionId=" + sessionId
									+ "]: PortMeZ2Consumer.receiveSCSDMessage()- Recieved Z2 Soap Message - Exception occurs while processind SD soap of zone2 with MSISDN : ["
									+ sdInfo.getNumberFrom() + "], ERROR : " + e.getMessage());
						}
					}
					if (current_status != 0) {
						portMeService.updatePortMeStatus(current_status, requestId, requestType);
					}
					_logger.info("[sessionId=" + sessionId
							+ "]: PortMeZ2Consumer.receiveSCSDMessage()- Recieved Z2 Soap Message - End processing (SD MessageId : 1006) with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
				}
			} else if (npcData.getMessageHeader().getMessageID() == 1008) {
				_logger.info("[sessionId=" + sessionId
						+ "]: PortMeZ2Consumer.receiveSCSDMessage()- Recieved Z2 Soap Message - start processing (SC MessageId : 1008) with timestamp:["
						+ new Timestamp(System.currentTimeMillis()) + "]");
				PortDeactWithRte scType = npcData.getNPCMessage().getPortDeactWithRte();
				if (scType != null) {
					current_status = 12;
					String requestType = ReadConfigFile.getProperties().getProperty("PORTME_IN");
					for (NumberRange scInfo : scType.getNumberRange()) {
						try {
							requestId = npcData.getMessageHeader().getTransactionID();
							portMeTransaction = new PortMeTransactionDetails();
							portMeTransaction.setReferenceId(requestId);
							portMeTransaction.setStatus(current_status);
							portMeTransaction.setRequestType(requestType);
							portMeTransaction.setMsisdn(scInfo.getNumberFrom());
							portMeTransactionService.savePortMeTransactionDetails(portMeTransaction);
							subscriberArrTypeService.updateSCByMsisdn(current_status, scInfo.getNumberFrom(),
									requestType, sessionId);
						} catch (Exception e) {
							_logger.error("[sessionId=" + sessionId
									+ "]: PortMeZ2Consumer.receiveSCSDMessage()- Recieved Z2 Soap Message - Exception occurs while processind SC soap of zone2 with MSISDN : ["
									+ scInfo.getNumberFrom() + "], ERROR : " + e.getMessage());
						}
					}
					if (current_status != 0) {
						portMeService.updatePortMeStatus(current_status, requestId, requestType);
					}
					_logger.info("[sessionId=" + sessionId
							+ "]: PortMeZ2Consumer.receiveSCSDMessage()- Recieved Z2 Soap Message - End processing (SC MessageId : 1008) with timestamp:["
							+ new Timestamp(System.currentTimeMillis()) + "]");
				}
			}
		} catch (

		JMSException e) {
			handleException("Received Message with JMS error:", e, requestId);
		} catch (JSONException e) {
			handleException("Received Message with JSON error:", e, requestId);
		} catch (JsonMappingException e) {
			handleException("Received Message with JSON error:", e, requestId);
		} catch (JsonProcessingException e) {
			handleException("Received Message with JSON error:", e, requestId);
		}
	}

	private void handleException(String message, Exception e, String requestId) {
		_logger.error(message + " " + e.getMessage() + " RequestId - " + requestId, e);
	}

	public void createTerminateRequest(String referenceId, String msisdns) {
		int current_status = 0;
		String requestId = null;
		TerminateSimTransactionDetails terminationTransaction = new TerminateSimTransactionDetails();
		TerminateSimMT terminateSIMMt = new TerminateSimMT();
		String reqType = ReadConfigFile.getProperties().getProperty("TERMINATE_IN");
		int resultCode = 0;
		int mch_type = 2;
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
					// requestId = portMeRepository.getSynReqeustId(messageSenderTelco);
					// } else {
					requestId = referenceId;
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
