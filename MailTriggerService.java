package com.merittrac.apollo.rps.services;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.google.gson.Gson;
import com.merittrac.apollo.common.CryptUtil;
import com.merittrac.apollo.common.EmailClient;
import com.merittrac.apollo.common.HttpClientFactory;
import com.merittrac.apollo.data.service.RpsPackFailedStatusService;
import com.merittrac.apollo.rps.dataobject.EmailTriggerEntity;


public class MailTriggerService {

	@Autowired
	CryptUtil cryptUtil;

	@Autowired
	Gson gson;

	@Autowired
	RpsPackFailedStatusService rpsPackFailedStatusService;

	@Value("#{'${email_to_dev_team}'.split(',')}")
	private List<String> listOfMailIdsToDevTeam;

	@Value("#{'${email_to_qa_team}'.split(',')}")
	private List<String> listOfMailIdsToQaTeam;

	@Value("#{'${email_to_leadership_team}'.split(',')}")
	private List<String> listOfMailIdsToLeadershipTeam;

	@Value("${email_trigger_from}")
	private String emailTriggerFrom;

	@Value("${email_trigger_from_name}")
	private String emailTriggerFromName;

	@Value("${email_trigger_subject}")
	private String emailTriggerSubject;

	@Value("${email_trigger_send_grid_key}")
	private String emailTriggerSendGridKey;

	@Value("${email_trigger_uri}")
	private String emailTriggerUri;

	@Value("${email_trigger_method}")
	private String emailTriggerMethod;

	@Value("${email_proxy_address}")
	private String proxyAddress;

	@Value("${email_proxy_port}")
	private int proxyPort;

	@Value("${email_username}")
	private String username;

	@Value("${email_password}")
	private String password;

	@Value("${email_proxy_domain}")
	private String proxyDomain;

	protected static final Logger LOGGER = LoggerFactory.getLogger(MailTriggerService.class);

	public boolean populateMailContentAndTriggerMail(String message) {
		LOGGER.debug(" populateMailContentAndTriggerMail : message " + message);
		List<String> listOfMailIdsToSendMail = getEmailRecepientList();

		if (listOfMailIdsToSendMail == null || listOfMailIdsToSendMail.isEmpty()) {
			LOGGER.debug(" populateMailContentAndTriggerMail : no mail sent, TO list is empty");
			return false;
		}
		try {
			EmailTriggerEntity emailTriggerEntity = new EmailTriggerEntity(emailTriggerFrom, emailTriggerFromName,
					emailTriggerSubject, message, emailTriggerSendGridKey);
			for (String recepient : listOfMailIdsToSendMail) {
				emailTriggerEntity.setTo(recepient);
				String jsonInput = gson.toJson(emailTriggerEntity);
				// Invoke the REST api to trigger emAIL.
				String response = HttpClientFactory.getInstance().requestPostWithJson(emailTriggerUri,
						emailTriggerMethod, jsonInput);
				LOGGER.debug(" populateMailContentAndTriggerMail : mail sent :" + jsonInput + "response :" + response);
				// logger response

			}
		} catch (Exception e) {
			// logger
			LOGGER.error("Error in email trigger " + e.getMessage());
			// e.printStackTrace();
			return false;
		}
		return true;
	}


	private List<String> getEmailRecepientList() {
		List<String> listOfMailIdsToSendMail = new ArrayList<String>();
		if (listOfMailIdsToDevTeam != null && !listOfMailIdsToDevTeam.isEmpty())
			listOfMailIdsToSendMail.addAll(listOfMailIdsToDevTeam);
		if (listOfMailIdsToQaTeam != null && !listOfMailIdsToQaTeam.isEmpty())
			listOfMailIdsToSendMail.addAll(listOfMailIdsToQaTeam);
		if (listOfMailIdsToLeadershipTeam != null && !listOfMailIdsToLeadershipTeam.isEmpty())
			listOfMailIdsToSendMail.addAll(listOfMailIdsToLeadershipTeam);
		return listOfMailIdsToSendMail;
	}

	public boolean populateMailContentAndTriggerMailWithProxy(String message) {
		LOGGER.debug(" populateMailContentAndTriggerMail : message " + message);
		List<String> listOfMailIdsToSendMail = getEmailRecepientList();

		if (listOfMailIdsToSendMail == null || listOfMailIdsToSendMail.isEmpty()) {
			LOGGER.debug(" populateMailContentAndTriggerMail : no mail sent, TO list is empty");
			return false;
		}
		try {
			EmailTriggerEntity emailTriggerEntity = new EmailTriggerEntity(emailTriggerFrom, emailTriggerFromName,
					emailTriggerSubject, message, emailTriggerSendGridKey);
			for (String recepient : listOfMailIdsToSendMail) {
				emailTriggerEntity.setTo(recepient);
				String jsonInput = gson.toJson(emailTriggerEntity);
				// Invoke the REST api to trigger emAIL.
				String response = EmailClient.emailClientTrigger(proxyAddress, proxyPort, emailTriggerUri+emailTriggerMethod, username, password, jsonInput);
				LOGGER.debug(" populateMailContentAndTriggerMail : mail sent :" + jsonInput + "response :" + response);
				// logger response
			}

		}
		catch (Exception e) {
			// logger
			LOGGER.error("Error in email trigger " + e.getMessage());
			// e.printStackTrace();
			return false;
		}
		return true;
	}

}
