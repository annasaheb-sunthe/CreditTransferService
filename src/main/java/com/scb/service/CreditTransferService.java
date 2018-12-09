package com.scb.service;

import java.util.Map;

import com.scb.model.CreditTransferRequest;
import com.scb.model.CreditTransferResponse;
import com.scb.model.CreditTransfer;

public interface CreditTransferService {
	
	public CreditTransferResponse customerRequestHandleService(CreditTransferRequest customerRequest);
	
	public CreditTransferResponse requestHandleService(Map<String, String> requestHeader, String request);

	public boolean saveTrancation(CreditTransfer balanceEnquiry);
}
