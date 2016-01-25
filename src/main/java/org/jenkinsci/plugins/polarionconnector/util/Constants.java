package org.jenkinsci.plugins.polarionconnector.util;

public class Constants {

	public static final String SUCCESS = "SUCCESS";
	
	public static final String BUILD_NUMBER = "BUILD_NUMBER";
	
	public static final String TRIGGERED_BUILD_NUMBER = "TRIGGERED_BUILD_NUMBER_%s";
	
	public static final String TRIGGERED_BUILD_RESULT = "TRIGGERED_BUILD_RESULT_%s";
	
	public static final String JENKINS_HOME = "JENKINS_HOME";
	
	public static final String POLARION_JOB_NAME = "jobName";
	
	public static final String POLARION_PROJECT_ID = "projectId";
	
	public static final String POLARION_WORKITEM_ID = "workItemId";
	
	public static final String POLARION_NEXT_STATUS_OK = "nextStatusOK";
	
	public static final String POLARION_NEXT_STATUS_KO = "nextStatusKO";
	
	public static final String POLARION_ATTACHMENT = "attachment";
	
	public static final String POLARION_QUERY_WORKITEM = "project.id:%s AND id:%s";
	
	public static final String[] POLARION_WORKITEM_FIELDS = { "status", "previousStatus", "title", "resolution",
			"resolvedOn", "project", "type", "customFields", "attachments" };

	public static final String POLARION_WEB_SERVICES = "/polarion/ws/services/";
}
