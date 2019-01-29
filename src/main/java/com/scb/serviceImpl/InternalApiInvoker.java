package com.scb.serviceImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.scb.config.CreditTransferServiceConfig;
import com.scb.model.AuditLog;
import com.scb.model.BalanceEnquiry;
import com.scb.model.CreditTransferResponse;
import com.scb.model.CreditTransferValidateResponse;
import com.scb.model.MsErrorLog;
import com.scb.model.PersistanceData;
import com.scb.model.ProcessFlowSequence;
import com.scb.model.RequestData;
import com.scb.model.ResponseMessage;
import com.scb.utils.ServiceUtil;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class InternalApiInvoker {

	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private CreditTransferServiceConfig customerConfig;
	@Autowired
	private ServiceUtil commonMethods;
	
	public ResponseEntity<CreditTransferResponse> persistenceServiceApiCall(PersistanceData persistanceData) {
		//log.info("OutwardBalanceEnquiry in persistence service: " + persistanceData);
		log.info("RequestData in persistenceServiceApiCall - Transaction Type: " + persistanceData.getTransactionType() 
		+ " Transaction Sub Type :" + persistanceData.getTransactionSubType() + " Payload Format :" + persistanceData.getPayloadFormat());
		ResponseEntity<CreditTransferResponse> persistenceServiceApi = null;
		ResponseEntity<ResponseMessage> reponseMessage = null;
		try {
			HttpEntity<PersistanceData> entity = new HttpEntity<PersistanceData>(persistanceData);
			log.info("calling restTemplate...");
			reponseMessage = restTemplate.exchange(customerConfig.getPersistenceServiceURL(),
					HttpMethod.POST, entity, ResponseMessage.class);
			log.info("reponseMessage : " + reponseMessage);
			CreditTransferResponse br = new CreditTransferResponse().builder().responseCode(reponseMessage.getBody().getResponseCode()).responseMessage(reponseMessage.getBody().getResponseMessage()).build();
			persistenceServiceApi = new ResponseEntity<CreditTransferResponse>(br, HttpStatus.CREATED);
			log.info("Response Entity [persistenceServiceApi] : " + persistenceServiceApi);
		} catch (HttpClientErrorException | HttpServerErrorException httpClientOrServerEx) {
			log.info("HttpClientErrorException | HttpServerErrorException occured.... : " + httpClientOrServerEx.getMessage());
			httpClientOrServerEx.printStackTrace();
			MsErrorLog msErrorLog = commonMethods.getErrorLogDetails(httpClientOrServerEx);
			msErrorLog.setErrorCode(httpClientOrServerEx.getStatusCode().toString());
			msErrorLog.setUuid(persistanceData.getTransactionID());
			msErrorLog.setTimeStamp(persistanceData.getCreatedOn());
			if (HttpStatus.INTERNAL_SERVER_ERROR.equals(httpClientOrServerEx.getStatusCode())) {
				msErrorLogApiCall(msErrorLog);
				return new ResponseEntity<CreditTransferResponse>( commonMethods.getErrorResponse("Problem While calling persister api"), HttpStatus.BAD_GATEWAY);
				//return responseOfCustomerApi;
				// retry logic goes here
			} else {
				return new ResponseEntity<CreditTransferResponse>( commonMethods.getErrorResponse("Problem While calling persister api"), HttpStatus.BAD_GATEWAY);
				
				// do something
			}
		} catch (Exception e) {
			MsErrorLog msErrorLog = commonMethods.getErrorLogDetails(e);
			msErrorLog.setUuid(persistanceData.getTransactionID());
			msErrorLog.setTimeStamp(persistanceData.getCreatedOn());
			msErrorLogApiCall(msErrorLog);
			return new ResponseEntity<CreditTransferResponse>( commonMethods.getErrorResponse("Problem While calling persister api"), HttpStatus.BAD_GATEWAY);
			
		}
		return persistenceServiceApi;
	}

	public ResponseEntity<CreditTransferResponse> serviceApiCall(RequestData requestData, String serviceURL) {
		log.info("RequestData in serviceApiCall - Transaction Type: " + requestData.getTransactionType() 
			+ " Transaction Sub Type :" + requestData.getTransactionSubType() + " Payload Format :" + requestData.getPayloadFormat());
		log.info("ServiceURL in serviceApiCall: " + serviceURL);
		ResponseEntity<CreditTransferResponse> serviceApiResponse = null;
		ResponseEntity<ResponseMessage> reponseMessage = null;
		try {
			
			 List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();        
	         //Add the Jackson Message converter
			 MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
			 // Note: here we are making this converter to process any kind of response, 
			 // not only application/*json, which is the default behaviour
			 List<MediaType> mediaList = new ArrayList<MediaType>();
			 mediaList.add(MediaType.APPLICATION_JSON);
			 mediaList.add(MediaType.APPLICATION_JSON_UTF8);
			 mediaList.add(MediaType.APPLICATION_XML);
			 mediaList.add(MediaType.APPLICATION_XHTML_XML);
			 //converter.setSupportedMediaTypes(Arrays.asList({MediaType.ALL}));
			 converter.setSupportedMediaTypes(mediaList);
			 //converter.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL));
			 messageConverters.add(converter);  
			 restTemplate.setMessageConverters(messageConverters);  
	   
	   
			HttpEntity<RequestData> entity = new HttpEntity<RequestData>(requestData);
			log.info("calling restTemplate...");
			reponseMessage = restTemplate.exchange(serviceURL, HttpMethod.POST, entity, ResponseMessage.class);
			log.info("reponseMessage : " + reponseMessage);
			CreditTransferResponse br = new CreditTransferResponse().builder().responseCode(reponseMessage.getBody().getResponseCode()).responseMessage(reponseMessage.getBody().getResponseMessage()).responseData(reponseMessage.getBody().getResponseData()).build();
			serviceApiResponse = new ResponseEntity<CreditTransferResponse>(br, HttpStatus.OK);
			log.info("Response Entity [serviceApiResponse] : " + serviceApiResponse);
		} catch (HttpClientErrorException | HttpServerErrorException httpClientOrServerEx) {
			log.info("HttpClientErrorException | HttpServerErrorException occured.... : " + httpClientOrServerEx.getMessage());
			httpClientOrServerEx.printStackTrace();
			MsErrorLog msErrorLog = commonMethods.getErrorLogDetails(httpClientOrServerEx);
			msErrorLog.setErrorCode(httpClientOrServerEx.getStatusCode().toString());
			msErrorLog.setUuid(requestData.getTransactionID());
			msErrorLog.setTimeStamp(requestData.getCreatedOn());
			if (HttpStatus.INTERNAL_SERVER_ERROR.equals(httpClientOrServerEx.getStatusCode())) {
				msErrorLogApiCall(msErrorLog);
				return new ResponseEntity<CreditTransferResponse>( commonMethods.getErrorResponse("Problem While calling serviceApiCall api"), HttpStatus.BAD_GATEWAY);
			} else {
				return new ResponseEntity<CreditTransferResponse>( commonMethods.getErrorResponse("Problem While calling serviceApiCall api"), HttpStatus.BAD_GATEWAY);
			}
		} catch (Exception e) {
			log.info("Exception occured.... : " + e.getMessage());
			e.printStackTrace();
			MsErrorLog msErrorLog = commonMethods.getErrorLogDetails(e);
			msErrorLog.setUuid(requestData.getTransactionID());
			msErrorLog.setTimeStamp(requestData.getCreatedOn());
			msErrorLogApiCall(msErrorLog);
			return new ResponseEntity<CreditTransferResponse>( commonMethods.getErrorResponse("Problem While calling serviceApiCall api"), HttpStatus.BAD_GATEWAY);
		}
		return serviceApiResponse;
	}

	public ResponseEntity<ResponseMessage> configServiceApiCall(RequestData requestData, String serviceURL) {
		log.info("RequestData in serviceApiCall: " + requestData);
		log.info("RequestData in serviceURL: " + serviceURL);
		ResponseEntity<List<ProcessFlowSequence>> serviceApiResponse = null;
		ResponseEntity<ResponseMessage> reponseMessage = null;
		//ResponseMessage reponseMessage = null;
		List processesList = null;
		
		try {
			HttpEntity<RequestData> entity = new HttpEntity<RequestData>(requestData);
			log.info("calling restTemplate...");
			reponseMessage = restTemplate.exchange(serviceURL, HttpMethod.POST, entity, ResponseMessage.class);
			log.info("reponseMessage : " + reponseMessage);
			//BalanceEnquiryResponse br = new BalanceEnquiryResponse().builder().responseCode(reponseMessage.getBody().getResponseCode()).responseMessage(reponseMessage.getBody().getResponseMessage()).build();
			//serviceApiResponse = new ResponseEntity<List<ProcessFlowSequence>>((List<ProcessFlowSequence>) reponseMessage.getBody().getList(), HttpStatus.OK);
			log.info("Response Entity [serviceApiResponse] : " + serviceApiResponse);
		} catch (HttpClientErrorException | HttpServerErrorException httpClientOrServerEx) {
			log.info("HttpClientErrorException | HttpServerErrorException occured.... : " + httpClientOrServerEx.getMessage());
			httpClientOrServerEx.printStackTrace();
			MsErrorLog msErrorLog = commonMethods.getErrorLogDetails(httpClientOrServerEx);
			msErrorLog.setErrorCode(httpClientOrServerEx.getStatusCode().toString());
			msErrorLog.setUuid(requestData.getTransactionID());
			msErrorLog.setTimeStamp(requestData.getCreatedOn());
			if (HttpStatus.INTERNAL_SERVER_ERROR.equals(httpClientOrServerEx.getStatusCode())) {
				msErrorLogApiCall(msErrorLog);
				return new ResponseEntity<ResponseMessage>( commonMethods.getResponseMessage("Problem While calling serviceApiCall api"), HttpStatus.BAD_GATEWAY);
			} else {
				return new ResponseEntity<ResponseMessage>( commonMethods.getResponseMessage("Problem While calling serviceApiCall api"), HttpStatus.BAD_GATEWAY);
			}
		} catch (Exception e) {
			log.info("Exception occured.... : " + e.getMessage());
			e.printStackTrace();
			MsErrorLog msErrorLog = commonMethods.getErrorLogDetails(e);
			msErrorLog.setUuid(requestData.getTransactionID());
			msErrorLog.setTimeStamp(requestData.getCreatedOn());
			msErrorLogApiCall(msErrorLog);
			return new ResponseEntity<ResponseMessage>( commonMethods.getResponseMessage("Problem While calling serviceApiCall api"), HttpStatus.BAD_GATEWAY);
		}
		return reponseMessage;
	}

	public ResponseEntity<AuditLog> auditLogApiCall(AuditLog auditLog) {
		ResponseEntity<AuditLog> responseAuditLog = null;
		try {
			log.debug("AuditLogService call...");
			HttpEntity<AuditLog> entity = new HttpEntity<AuditLog>(auditLog);
			responseAuditLog = restTemplate.exchange(customerConfig.getAuditLogServiceURL(), HttpMethod.POST, entity,
					AuditLog.class);
		} catch (HttpClientErrorException | HttpServerErrorException httpClientOrServerEx) {
			MsErrorLog msErrorLog = commonMethods.getErrorLogDetails(httpClientOrServerEx);
			msErrorLog.setErrorCode(httpClientOrServerEx.getStatusCode().toString());
			//msErrorLog.setUuid(auditLog.getUuid());
			msErrorLog.setTimeStamp(auditLog.getTimestamp().toString());
			if (HttpStatus.INTERNAL_SERVER_ERROR.equals(httpClientOrServerEx.getStatusCode())) {
				msErrorLogApiCall(msErrorLog);
				// retry logic goes here
			} else {
				// do something
			}
		} catch (Exception e) {
			MsErrorLog msErrorLog = commonMethods.getErrorLogDetails(e);
			//msErrorLog.setUuid(auditLog.getUuid());
			msErrorLog.setTimeStamp(auditLog.getTimestamp().toString());
			msErrorLogApiCall(msErrorLog);
		}
		return responseAuditLog;
	}

	public void msErrorLogApiCall(MsErrorLog msErrorLog) {
		try {
			restTemplate.postForObject(customerConfig.getErrorLogURL(), msErrorLog, MsErrorLog.class);
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	public ResponseEntity<CreditTransferResponse> msCustomerPersistApiCall(BalanceEnquiry customerRequestData) {
		ResponseEntity<CreditTransferResponse> responseOfCustomerApi = null;
		try {
			HttpEntity<BalanceEnquiry> entity = new HttpEntity<BalanceEnquiry>(customerRequestData);
			responseOfCustomerApi = restTemplate.exchange(customerConfig.getCustomerRequestPersistURL(),
					HttpMethod.POST, entity, CreditTransferResponse.class);
		} catch (HttpClientErrorException | HttpServerErrorException httpClientOrServerEx) {
			MsErrorLog msErrorLog = commonMethods.getErrorLogDetails(httpClientOrServerEx);
			msErrorLog.setErrorCode(httpClientOrServerEx.getStatusCode().toString());
			msErrorLog.setUuid(customerRequestData.getTransactionId());
			msErrorLog.setTimeStamp(customerRequestData.getTimeStamp());
			if (HttpStatus.INTERNAL_SERVER_ERROR.equals(httpClientOrServerEx.getStatusCode())) {
				msErrorLogApiCall(msErrorLog);
				return new ResponseEntity<CreditTransferResponse>( commonMethods.getErrorResponse("Problem While calling persister api"), HttpStatus.BAD_GATEWAY);
				//return responseOfCustomerApi;
				// retry logic goes here
			} else {
				return new ResponseEntity<CreditTransferResponse>( commonMethods.getErrorResponse("Problem While calling persister api"), HttpStatus.BAD_GATEWAY);
				
				// do something
			}
		} catch (Exception e) {
			MsErrorLog msErrorLog = commonMethods.getErrorLogDetails(e);
			msErrorLog.setUuid(customerRequestData.getTransactionId());
			msErrorLog.setTimeStamp(customerRequestData.getTimeStamp());
			msErrorLogApiCall(msErrorLog);
			return new ResponseEntity<CreditTransferResponse>( commonMethods.getErrorResponse("Problem While calling persister api"), HttpStatus.BAD_GATEWAY);
			
		}
		return responseOfCustomerApi;
	}

	public ResponseEntity<CreditTransferResponse> msDownStreamCall(BalanceEnquiry customerRequestData) {
		ResponseEntity<BalanceEnquiry> responseOfCustomerApi = null;
		//ResponseEntity<CustomerRequestData> responseCustomerRequestData = null;
		try{
			HttpEntity<BalanceEnquiry> entity = new HttpEntity<BalanceEnquiry>(customerRequestData);
			responseOfCustomerApi = restTemplate.exchange(customerConfig.getDownStreamURL(), HttpMethod.POST, entity,
					BalanceEnquiry.class);
		}catch (HttpClientErrorException | HttpServerErrorException httpClientOrServerEx) {
			MsErrorLog msErrorLog = commonMethods.getErrorLogDetails(httpClientOrServerEx);
			msErrorLog.setErrorCode(httpClientOrServerEx.getStatusCode().toString());
			msErrorLog.setUuid(customerRequestData.getTransactionId());
			msErrorLog.setTimeStamp(customerRequestData.getTimeStamp());
			if (HttpStatus.INTERNAL_SERVER_ERROR.equals(httpClientOrServerEx.getStatusCode())) {
				msErrorLogApiCall(msErrorLog);
				return new ResponseEntity<CreditTransferResponse>( commonMethods.getErrorResponse("Problem While calling downstream api"), HttpStatus.BAD_GATEWAY);
				
				// retry logic goes here
			} else {
				return new ResponseEntity<CreditTransferResponse>( commonMethods.getErrorResponse("Problem While calling downstream api"), HttpStatus.BAD_GATEWAY);
				
				// do something
			}
		} catch (Exception e) {
			MsErrorLog msErrorLog = commonMethods.getErrorLogDetails(e);
			msErrorLog.setUuid(customerRequestData.getTransactionId());
			msErrorLog.setTimeStamp(customerRequestData.getTimeStamp());
			msErrorLogApiCall(msErrorLog);
			return new ResponseEntity<CreditTransferResponse>( commonMethods.getErrorResponse("Problem While calling downstream api"), HttpStatus.BAD_GATEWAY);
			
		}
		//return responseOfCustomerApi;
		log.debug("Downstream response: "+responseOfCustomerApi.getBody().toString());
		return new  ResponseEntity<CreditTransferResponse>(commonMethods.getSuccessResponse(responseOfCustomerApi.getBody()), HttpStatus.OK);
	}
	
	public ResponseEntity<CreditTransferResponse> msValidatorCall(BalanceEnquiry customerRequestData) {
		ResponseEntity<CreditTransferValidateResponse> responseOfCustomerApi = null;
		//ResponseEntity<CustomerRequestData> responseCustomerRequestData = null;
		try{
			HttpEntity<BalanceEnquiry> entity = new HttpEntity<BalanceEnquiry>(customerRequestData);
			responseOfCustomerApi = restTemplate.exchange(customerConfig.getCustomerValidatorURL(), HttpMethod.POST, entity,
					CreditTransferValidateResponse.class);
		}catch (HttpClientErrorException | HttpServerErrorException httpClientOrServerEx) {
			MsErrorLog msErrorLog = commonMethods.getErrorLogDetails(httpClientOrServerEx);
			msErrorLog.setErrorCode(httpClientOrServerEx.getStatusCode().toString());
			msErrorLog.setUuid(customerRequestData.getTransactionId());
			msErrorLog.setTimeStamp(customerRequestData.getTimeStamp());
			if (HttpStatus.INTERNAL_SERVER_ERROR.equals(httpClientOrServerEx.getStatusCode())) {
				msErrorLogApiCall(msErrorLog);
				return new ResponseEntity<CreditTransferResponse>( commonMethods.getErrorResponse("Problem While calling validator api"), HttpStatus.BAD_GATEWAY);
				
				// retry logic goes here
			} else {
				return new ResponseEntity<CreditTransferResponse>( commonMethods.getErrorResponse("Problem While calling validator api"), HttpStatus.BAD_GATEWAY);
				
				// do something
			}
		} catch (Exception e) {
			MsErrorLog msErrorLog = commonMethods.getErrorLogDetails(e);
			msErrorLog.setUuid(customerRequestData.getTransactionId());
			msErrorLog.setTimeStamp(customerRequestData.getTimeStamp());
			msErrorLogApiCall(msErrorLog);
			return new ResponseEntity<CreditTransferResponse>( commonMethods.getErrorResponse(500, e.getMessage()), HttpStatus.BAD_GATEWAY);
			
		}
		//return responseOfCustomerApi;
		log.debug("Downstream response: "+responseOfCustomerApi.getBody().toString());
		if(responseOfCustomerApi.getBody().getResponseCode() == 200){
			return new  ResponseEntity<CreditTransferResponse>(commonMethods.getSuccessResponse(responseOfCustomerApi.getBody().getCustomerRequestData(), responseOfCustomerApi.getBody().getDownstream_protocol()), HttpStatus.OK);
		}else{
			return new  ResponseEntity<CreditTransferResponse>(commonMethods.getErrorResponse(responseOfCustomerApi.getBody().getResponseCode(), responseOfCustomerApi.getBody().getResponseMessage()), HttpStatus.OK);
		}
	//	return new  ResponseEntity<CustomerResponse>(commonMethods.getSuccessResponse(responseOfCustomerApi.getBody().getCustomerRequestData()), HttpStatus.OK);
	}

}
