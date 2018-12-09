package com.scb.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Random;

import org.springframework.stereotype.Component;

import com.scb.model.AuditLog;
import com.scb.model.BalanceEnquiry;
import com.scb.model.CreditTransfer;
import com.scb.model.CreditTransferRequest;
import com.scb.model.CreditTransferResponse;
import com.scb.model.MsAuditLog;
import com.scb.model.MsErrorLog;
import com.scb.model.ResponseMessage;

@Component
public class ServiceUtil {

	public String getCurrentDateTime() {
		LocalDateTime localDateTime = LocalDateTime.now();
		return localDateTime.toString();
	}

	public BalanceEnquiry getBalanceEnquiryFromRequest(CreditTransferRequest balanceEnquiryRequest) {
		BalanceEnquiry balanceEnquiry = BalanceEnquiry.builder()
				.customerAccType(balanceEnquiryRequest.getCustomerAccType()).customerId(balanceEnquiryRequest.getCustomerId())
				.customerName(balanceEnquiryRequest.getCustomerName()).customerRegion(balanceEnquiryRequest.getCustomerRegion())
				.timeStamp(getCurrentDateTime()).correlationId(balanceEnquiryRequest.getCorrelationId())
				.transactionId(getTransactionId()).transactionType("BalanceEnquiry").build();
		return balanceEnquiry;
	}

	public CreditTransferResponse getSuccessResponse(BalanceEnquiry customerRequestData) {
		return CreditTransferResponse.builder().customerRequestData(customerRequestData).responseCode(200)
				.responseMessage("Success").build();
	}
	
	public CreditTransferResponse getSuccessResponse(BalanceEnquiry customerRequestData, String responseMessage) {
		return CreditTransferResponse.builder().customerRequestData(customerRequestData).responseCode(200)
				.responseMessage(responseMessage).build();
	}

	public CreditTransferResponse getErrorResponse() {
		return CreditTransferResponse.builder().responseCode(400).responseMessage("Bad request").build();

	}

	public CreditTransferResponse getErrorResponse(long errorCode, String errorMessage) {
		return CreditTransferResponse.builder().responseCode(errorCode).responseMessage(errorMessage).build();

	}

	public boolean isValidateCustomerRequest(CreditTransferRequest customerRequest) {
		if (null == customerRequest) {
			return false;
		} else if (null == customerRequest.getCustomerName()) {
			return false;
		} else if (customerRequest.getCustomerId() == 0) {
			return false;
		} else if ("USA".equals(customerRequest.getCustomerRegion())) {
			return false;
		} else if (customerRequest.getCorrelationId() == 0) {
			return false;
		}
		return true;
	}

	public CreditTransferResponse getErrorResponse(String errorMessage) {
		return CreditTransferResponse.builder().responseCode(400).responseMessage(errorMessage).build();
	}

	public ResponseMessage getResponseMessage(String errorMessage) {
		return ResponseMessage.builder().responseCode(400).responseMessage(errorMessage).build();
	}

	public long getTransactionId() {
		Random random = new Random(System.nanoTime() % 100000);
		long uniqueTansactionId = random.nextInt(1000000000);
		return uniqueTansactionId;
	}

	public MsAuditLog getAuditLogDetails(BalanceEnquiry customerRequestData) {
		MsAuditLog auditLog = MsAuditLog.builder().msComponent("Router").payload(toByteArray(customerRequestData)).timeStamp(customerRequestData.getTimeStamp()).uuid(customerRequestData.getTransactionId()).logMessage("Calling from router").build();
		return auditLog;
	}


	// convert object to toByteArray 
	public static byte[] toByteArray(Object obj) {
		byte[] bytes = null;
		ByteArrayOutputStream bos = null;
		ObjectOutputStream oos = null;
		try {
			bos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(bos);
			oos.writeObject(obj);
			oos.flush();
			bytes = bos.toByteArray();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (oos != null) {
				try {
					oos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return bytes;
	}

	public static Object toObject(byte[] bytes) throws IOException, ClassNotFoundException {
		Object obj = null;
		ByteArrayInputStream bis = null;
		ObjectInputStream ois = null;
		try {
			bis = new ByteArrayInputStream(bytes);
			ois = new ObjectInputStream(bis);
			obj = ois.readObject();
		} finally {
			if (bis != null) {
				bis.close();
			}
			if (ois != null) {
				ois.close();
			}
		}
		return obj;
	}

	public MsErrorLog getErrorLogDetails(Exception e) {
		MsErrorLog errorLog = MsErrorLog.builder().errorMessage(e.getMessage()).msComponent("Router").stackTrace(toByteArray(e)).build();
		
		return errorLog;
	}
	
	public AuditLog getAuditLog(CreditTransfer creditTransfer, String status, String message) {
		return AuditLog.builder().transactionType(creditTransfer.getTransactionType())
				.transactionSubType(creditTransfer.getTransactionSubType())
				.transactionId(creditTransfer.getTransactionID())
				.serviceName("CreditTransferService")
				.messageType(creditTransfer.getPayloadFormat())
				.status(status)
				.message(message)
				.timestamp(new Date()).build();
	}
}
