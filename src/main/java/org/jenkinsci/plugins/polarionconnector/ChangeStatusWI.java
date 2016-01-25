package org.jenkinsci.plugins.polarionconnector;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.Calendar;

import javax.xml.rpc.ServiceException;

import org.apache.commons.io.FileUtils;
import org.jenkinsci.plugins.polarionconnector.util.Constants;

import com.polarion.alm.ws.client.WebServiceFactory;
import com.polarion.alm.ws.client.projects.ProjectWebService;
import com.polarion.alm.ws.client.session.SessionWebService;
import com.polarion.alm.ws.client.tracker.TrackerWebService;
import com.polarion.alm.ws.client.types.projects.Project;
import com.polarion.alm.ws.client.types.tracker.CustomField;
import com.polarion.alm.ws.client.types.tracker.EnumOptionId;
import com.polarion.alm.ws.client.types.tracker.WorkItem;
import com.polarion.alm.ws.client.types.tracker.WorkflowAction;

import hudson.model.TaskListener;

public class ChangeStatusWI {

	private TaskListener listener;

	private WebServiceFactory factory;

	private SessionWebService sessionService;

	private TrackerWebService trackerService;

	private ProjectWebService projectService;

	private Project project;

	private String username;

	private String password;

	private String url;

	public ChangeStatusWI(TaskListener listener, String url, String username, String password) {
		setListener(listener);
		setUrl(url);
		setUsername(username);
		setPassword(password);
	}

	public void setUpPolarionAddress() throws MalformedURLException {
		setUrl(this.getUrl() + Constants.POLARION_WEB_SERVICES);
		setFactory(new WebServiceFactory(this.getUrl()));
	}

	private void setUpPolarionWebServices() throws ServiceException {
		setSessionService(getFactory().getSessionService());
		setTrackerService(getFactory().getTrackerService());
		setProjectService(getFactory().getProjectService());
	}

	public boolean execute(String projectId, String workItemiId, String statusId) throws Exception {
		try {
			setUpPolarionAddress();
			setUpPolarionWebServices();
			getSessionService().logIn(getUsername(), getPassword());
		} catch (ServiceException se) {
			printError("Unreachable web services at Polarion server.");
			throw se;
		} catch (RemoteException re) {
			printWarning("Log in unsuccessful");
			throw re;
		}

		try {

			this.setProject(getProjectService().getProject(projectId));
			if (getProject().isUnresolvable()) {
				printError("Project not found: " + projectId);
				return false;
			}

			String queryWI = String.format("project.id:%s AND id:%s", new Object[] { projectId, workItemiId });
			getListener().getLogger().println("Executing Polarion Plugin");
			getListener().getLogger().printf("Searching workitem: %s\n", new Object[] { workItemiId });
			getListener().getLogger().printf("Performing query: %s\n", new Object[] { queryWI });

			WorkItem[] workItems = getTrackerService().queryWorkItems(queryWI, "id", Constants.POLARION_WORKITEM_FIELDS);

			if ((workItems != null) && (workItems.length > 0)) {
				getListener().getLogger().printf("Workitem \"%s\" found!\n", new Object[] { workItemiId });

				WorkItem workItem = workItems[0];
				EnumOptionId status = workItem.getStatus();
				getListener().getLogger().printf("Changing status of Workitem to %s\n", new Object[] { statusId });
				status.setId(statusId);
				workItem.setStatus(status);

				CustomField buildDateCF = getTrackerService().getCustomField(workItem.getUri(), "buildDate");
				buildDateCF.setValue(Calendar.getInstance());

				getTrackerService().setCustomField(buildDateCF);
				getSessionService().beginTransaction();

				WorkflowAction[] actions = getTrackerService().getAvailableActions(workItem.getUri());
				for (WorkflowAction workflowAction : actions) {
					EnumOptionId targetStatus = workflowAction.getTargetStatus();
					if (targetStatus.getId().equals(statusId)) {
						getTrackerService().performWorkflowAction(workItem.getUri(), workflowAction.getActionId());
					}
				}

				getTrackerService().updateWorkItem(workItem);
				getSessionService().endTransaction(false);

				getListener().getLogger().println("Workitem updated!");
				return true;
			}
			printError(String.format("Workitem \"%s\" not found!\n", new Object[] { workItemiId }));
		}
		catch (RemoteException re) {
			printError("Error occured during the execution of a remote method call.");
			throw re;
		} finally {
			getSessionService().endSession();
		}
		return false;
	}

	public boolean createAttachment(WorkItem workItem, File file, String buildId) throws IOException {
		if ((file != null) && (file.exists()) && (file.isFile())) {
			byte[] bytes = FileUtils.readFileToByteArray(file);
			String tittle = String.format("%s [%s]", new Object[] { file.getName(), buildId });
			getTrackerService().createAttachment(workItem.getUri(), file.getName(), tittle, bytes);
			return true;
		}
		printWarning(String.format("El fichero %s no existe!", new Object[] { file.getName() }));
		return false;
	}

	private void printError(String msg) {
		getListener().getLogger().println("ERROR: " + msg);
	}

	private void printWarning(String msg) {
		getListener().getLogger().println("WARNING: " + msg);
	}

	public SessionWebService getSessionService() {
		return this.sessionService;
	}

	public void setSessionService(SessionWebService sessionService) {
		this.sessionService = sessionService;
	}

	public TrackerWebService getTrackerService() {
		return this.trackerService;
	}

	public void setTrackerService(TrackerWebService trackerService) {
		this.trackerService = trackerService;
	}

	public ProjectWebService getProjectService() {
		return this.projectService;
	}

	public void setProjectService(ProjectWebService projectService) {
		this.projectService = projectService;
	}

	public WebServiceFactory getFactory() {
		return this.factory;
	}

	public void setFactory(WebServiceFactory factory) {
		this.factory = factory;
	}

	public Project getProject() {
		return this.project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public TaskListener getListener() {
		return this.listener;
	}

	public void setListener(TaskListener listener) {
		this.listener = listener;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
}
