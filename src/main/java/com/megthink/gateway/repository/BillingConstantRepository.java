package com.megthink.gateway.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.megthink.gateway.model.BillingConstant;

@Repository("billingConstantRepository")
public interface BillingConstantRepository extends JpaRepository<BillingConstant, Integer> {

	// List<BillingConstant> findByRequest_type(String request_type);

	@Query("SELECT b FROM BillingConstant b WHERE b.requestType = :requestType")
	List<BillingConstant> findByRequestType(@Param("requestType") String requestType);

	BillingConstant findById(int id);

}
