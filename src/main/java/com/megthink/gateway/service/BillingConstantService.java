package com.megthink.gateway.service;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.megthink.gateway.model.BillingConstant;
import com.megthink.gateway.repository.BillingConstantRepository;

@Service("billingConstant")
public class BillingConstantService {

	private BillingConstantRepository repository;

	Timestamp timestamp = new Timestamp(System.currentTimeMillis());

	@Autowired
	public BillingConstantService(BillingConstantRepository repository) {
		this.repository = repository;
	}

	public BillingConstant findById(int id) {
		return repository.findById(id);
	}

	public List<BillingConstant> findByRequestType(String requestType) {
		return repository.findByRequestType(requestType);
	}

}