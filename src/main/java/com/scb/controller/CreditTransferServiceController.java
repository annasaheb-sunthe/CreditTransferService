package com.scb.controller;


import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.scb.model.AuditLog;
import com.scb.model.CreditTransfer;
import com.scb.model.CreditTransferRequest;
import com.scb.model.CreditTransferResponse;
import com.scb.service.CreditTransferService;
import com.scb.serviceImpl.InternalApiInvoker;
import com.scb.utils.CommonConstants;
import com.scb.utils.ContextPathConstants;
import com.scb.utils.RequestParser;
import com.scb.utils.ServiceUtil;
import com.scb.utils.XmlParser;

import lombok.extern.log4j.Log4j2;


@RestController @Log4j2
@RequestMapping(ContextPathConstants.CUSTOMER_URL)
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CreditTransferServiceController {
	@Autowired
	private CreditTransferService creditTransferService;
	
	@Autowired
	private ServiceUtil commonMethods;
	
	@Autowired
	private InternalApiInvoker internalApiInvoker;
	
	@RequestMapping(value = ContextPathConstants.CUSTOMER_REQUEST_HANDLE_URL, method = RequestMethod.POST, produces = { "application/json", "application/xml" })
	public CreditTransferResponse customerRequestHandle(@RequestHeader Map<String, String> requestMap, @RequestBody String requestData) {
		
		log.info("RequestHeader received "+ requestMap);
		
		CreditTransfer creditTransfer = null;
		if (CommonConstants.APPLICATION_XML.equalsIgnoreCase(requestMap.get("content-type"))) {
			RequestParser parser = new XmlParser();
			creditTransfer = (CreditTransfer) parser.parse(requestData);
		}
		
		AuditLog auditLog = commonMethods.getAuditLog(creditTransfer, "RECEIVED", "Request processing initiated");
		ResponseEntity<AuditLog> responseAuditLog = internalApiInvoker.auditLogApiCall(auditLog);
		
		CreditTransferResponse customerResponse = new CreditTransferResponse();
		
		customerResponse = creditTransferService.requestHandleService(requestMap, requestData);
		log.info("Response: " + customerResponse.toString());
		
		if (customerResponse.getResponseCode() != 200) {
			auditLog = commonMethods.getAuditLog(creditTransfer, "FAILED", "Request processing failed");
		} else {
			auditLog = commonMethods.getAuditLog(creditTransfer, "COMPLTED", "Request processed successfully");
		}
		
		responseAuditLog = internalApiInvoker.auditLogApiCall(auditLog);
		return customerResponse;
	}

	@RequestMapping(value = ContextPathConstants.CUSTOMER_REQUEST_HANDLE_URL_REQUEST)
	public CreditTransferRequest customerRequestHandleExampleRequest() {

		return CreditTransferRequest.builder().customerAccType("Saving").customerId(22).customerName("Test Customer")
				.customerRegion("India").correlationId(200).build();
	}
}
