package com.megthink.gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.megthink.gateway.model.SubscriberInfoQueryDetail;

@Repository("subscriberInfoQueryDetailRepository")
public interface SubscriberInfoQueryDetailRepository extends JpaRepository<SubscriberInfoQueryDetail, Integer> {

	SubscriberInfoQueryDetail findByReferenceId(String referenceId);

}
