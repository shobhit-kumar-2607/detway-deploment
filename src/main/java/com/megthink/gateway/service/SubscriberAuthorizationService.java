package com.megthink.gateway.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.megthink.gateway.model.SubscriberAuthorization;
import com.megthink.gateway.repository.SubscriberAuthorizationRepository;
import com.megthink.gateway.utils.ReadConfigFile;

@Service
public class SubscriberAuthorizationService {

	private static final Logger _logger = LoggerFactory.getLogger(SubscriberAuthorizationService.class);

	Timestamp timestamp = new Timestamp(System.currentTimeMillis());

	private final SubscriberAuthorizationRepository subscriberAuthorizationRepository;

	@Autowired
	public SubscriberAuthorizationService(SubscriberAuthorizationRepository subscriberAuthorizationRepository) {
		this.subscriberAuthorizationRepository = subscriberAuthorizationRepository;
	}

	public int saveMt(int portId, List<SubscriberAuthorization> list, String requestType, int status,
			String activationDateTime, String disconnectionDateTime) {
		List<SubscriberAuthorization> authList = new ArrayList<SubscriberAuthorization>();
		int resetCnt = 0;
		int totalRecord = 0;
		try {
			String removeCountryCode = ReadConfigFile.getProperties().getProperty("zone1-countrycode");
			int removeCountryCodeLimit = Integer.parseInt(removeCountryCode);
			for (int i = 0; i < list.size(); i++) {
				try {
					SubscriberAuthorization subAuth = new SubscriberAuthorization();
					subAuth.setSubscriberNumber(list.get(i).getSubscriberNumber().substring(removeCountryCodeLimit));
					subAuth.setOwnerId(list.get(i).getOwnerId());
					subAuth.setTypeOfId(list.get(i).getTypeOfId());
					subAuth.setPortId(portId);
					subAuth.setRequest_type(requestType);
					subAuth.setStatus(status);
					if (activationDateTime != null) {
						subAuth.setActivationDateTime(activationDateTime);
					}
					if (disconnectionDateTime != null) {
						subAuth.setDisconnectionDateTime(disconnectionDateTime);
					}
					subAuth.setCreatedDateTime(timestamp);
					subAuth.setUpdatedDateTime(timestamp);
					subAuth.setResultCode(0);
					resetCnt++;
					totalRecord++;
					authList.add(subAuth);
					if (resetCnt == 500) {
						subscriberAuthorizationRepository.saveAll(authList);
						subscriberAuthorizationRepository.flush();
						authList.clear();
						resetCnt = 0;
					}
				} catch (Exception e) {
					_logger.error("SubscriberAuthorizationService.saveMt()-Exception occurs while save portmt details "
							+ e.getMessage());
				}
			}

			if (resetCnt < 500) {
				subscriberAuthorizationRepository.saveAll(authList);
				authList.clear();
			}
		} catch (Exception e) {
			_logger.error("SubscriberAuthorizationService.saveMt()-Exception occurs while save portmt details "
					+ e.getMessage());
		}
		return totalRecord;
	}
}