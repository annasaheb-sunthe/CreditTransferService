package com.scb.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.scb.model.CreditTransfer;

//@RepositoryRestResource
public interface CreditTransferRepository extends JpaRepository<CreditTransfer, CreditTransfer> {
	@Query(value="SELECT * FROM CreditTransfer BET WHERE BET.transactionID = ?1, BET.transactionType?2, BET.transactionSubType?3",nativeQuery=true)
	List<CreditTransfer> findByTransactionIdTypeAndSubType(long transactionID, String transactionType, String transactionSubType);
	
	@Query(value="SELECT * FROM CreditTransfer WHERE transactionID = ?1", nativeQuery=true)
	List<CreditTransfer> findByTransactionId(long transactionID);
}
