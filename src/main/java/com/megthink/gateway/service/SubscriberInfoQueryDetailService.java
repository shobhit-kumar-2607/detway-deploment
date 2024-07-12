package com.megthink.gateway.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.megthink.gateway.model.SubscriberInfoQueryDetail;
import com.megthink.gateway.repository.SubscriberInfoQueryDetailRepository;

@Service
public class SubscriberInfoQueryDetailService {

	private final SubscriberInfoQueryDetailRepository infoRepository;

	@Autowired
	public SubscriberInfoQueryDetailService(SubscriberInfoQueryDetailRepository infoRepository) {
		this.infoRepository = infoRepository;
	}

	public SubscriberInfoQueryDetail save(SubscriberInfoQueryDetail item) {
		return infoRepository.save(item);
	}
	
	public SubscriberInfoQueryDetail findByReferenceId(String referenceId) {
		return infoRepository.findByReferenceId(referenceId);
	}
}