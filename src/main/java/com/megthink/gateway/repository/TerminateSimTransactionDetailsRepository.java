package com.megthink.gateway.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.megthink.gateway.model.TerminateSimTransactionDetails;

@Repository("terminateSimTransactionDetailsRepository")
public interface TerminateSimTransactionDetailsRepository extends JpaRepository<TerminateSimTransactionDetails, Integer> {

}
