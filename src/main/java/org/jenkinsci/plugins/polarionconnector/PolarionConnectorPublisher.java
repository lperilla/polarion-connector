package org.jenkinsci.plugins.polarionconnector;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.polarionconnector.util.Constants;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import net.sf.json.JSONObject;

public class PolarionConnectorPublisher extends Publisher implements SimpleBuildStep {

	@DataBoundSetter
	private String urlPolarion;

	@DataBoundSetter
	private String username;

	@DataBoundSetter
	private String password;

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.BUILD;
	}

	public DescriptorImpl getDescriptor() {
		return (DescriptorImpl) super.getDescriptor();
	}

	@Override
	public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener)
			throws InterruptedException, IOException {
		try {

			listener.getLogger().println("***********************************************");
			listener.getLogger().printf("Build id: %s\n", new Object[] { build.getId() });
			listener.getLogger().printf("Build Number: %s\n", new Object[] { Integer.valueOf(build.getNumber()) });
			listener.getLogger().printf("Build Name: %s\n", new Object[] { build.getDisplayName() });
			listener.getLogger().printf("Build Status URL: %s\n", new Object[] { build.getBuildStatusUrl() });
			listener.getLogger().printf("Build Status Summary: %s\n", new Object[] { build.getBuildStatusSummary() });
			listener.getLogger().printf("Build Result: %s\n", new Object[] { build.getResult() });

			EnvVars envVars = build.getEnvironment(listener);

			Set<Map.Entry<String, String>> buildEnv = envVars.entrySet();
			for (Map.Entry<String, String> entry : buildEnv) {
				listener.getLogger().printf("\t %s: %s\n", new Object[] { entry.getKey(), entry.getValue() });
			}

			String jobName = envVars.get(Constants.POLARION_JOB_NAME);
			String projectId = envVars.get(Constants.POLARION_PROJECT_ID);
			String workitemId = envVars.get(Constants.POLARION_WORKITEM_ID);
			String nextStatusOK = envVars.get(Constants.POLARION_NEXT_STATUS_OK);
			String nextStatusKO = envVars.get(Constants.POLARION_NEXT_STATUS_KO);
			String jenkinsHome = envVars.get(Constants.JENKINS_HOME);
			boolean attachment = Boolean.parseBoolean(envVars.get(Constants.POLARION_ATTACHMENT));
			String triggeredBuildResult = envVars.get(String.format(Constants.TRIGGERED_BUILD_RESULT, jobName));

			int triggeredBuildNumber = envVars.containsKey(String.format(Constants.TRIGGERED_BUILD_NUMBER, jobName))
					? Integer.parseInt(envVars.get(String.format(Constants.TRIGGERED_BUILD_RESULT, jobName))) : -1;

			String nextStatus = nextStatusKO;
			if ((StringUtils.isNotBlank(triggeredBuildResult))
					&& (Result.SUCCESS.toExportedObject().equals(triggeredBuildResult))) {
				nextStatus = nextStatusOK;
			}

			listener.getLogger().println("***********************************************");
			listener.getLogger().println("Parameter Polarion Task");
			listener.getLogger().println("***********************************************");
			listener.getLogger().printf("Jenkins Home: %s\n", new Object[] { jenkinsHome });
			listener.getLogger().printf("Url Polarion: %s\n", new Object[] { getUrlPolarion() });
			listener.getLogger().printf("User Name: %s\n", new Object[] { getUsername() });
			listener.getLogger().printf("Job Name: %s\n", new Object[] { jobName });
			listener.getLogger().printf("ProjectId: %s\n", new Object[] { projectId });
			listener.getLogger().printf("Workitem Id: %s\n", new Object[] { workitemId });
			listener.getLogger().printf("Next status ok: %s\n", new Object[] { nextStatusOK });
			listener.getLogger().printf("Next status ko: %s\n", new Object[] { nextStatusKO });
			listener.getLogger().printf("Attachment: %s\n", new Object[] { Boolean.valueOf(attachment) });
			listener.getLogger().printf("Next status: %s\n", new Object[] { nextStatus });
			listener.getLogger().printf("Result: %s\n", new Object[] { triggeredBuildResult });
			listener.getLogger().printf("Triggered build number: %d\n",
					new Object[] { Integer.valueOf(triggeredBuildNumber) });

			ChangeStatusWI importer = new ChangeStatusWI(listener, getUrlPolarion(), getUsername(), getPassword());
			boolean result = importer.execute(projectId, workitemId, nextStatus);

			if (!result) {
				build.setResult(Result.FAILURE);
			}

		} catch (IOException ex) {
			build.setResult(Result.FAILURE);
			ex.printStackTrace();
		} catch (InterruptedException ex) {
			build.setResult(Result.FAILURE);
			ex.printStackTrace();
		} catch (Exception ex) {
			build.setResult(Result.FAILURE);
			ex.printStackTrace();
		}
	}

	public String getUrlPolarion() {
		return urlPolarion;
	}

	public void setUrlPolarion(String urlPolarion) {
		this.urlPolarion = urlPolarion;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public static String getValue(EnvVars envVars, String key) {
		if (envVars.containsKey(key)) {
			return envVars.get(key);
		}
		return null;
	}

	@Extension
	public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
		private static final String DISPLAY_NAME = "Polarion Connector Plugin";

		public DescriptorImpl() {
			load();
		}

		public FormValidation doCheckUrlPolarion(@QueryParameter String value) throws IOException, ServletException {
			if (StringUtils.isBlank(value)) {
				return FormValidation.error("Please set the of Polarion server");
			}
			return FormValidation.ok();
		}

		public FormValidation doCheckUsername(@QueryParameter String value) throws IOException, ServletException {
			if (StringUtils.isBlank(value)) {
				return FormValidation.error("Please set a username");
			}
			return FormValidation.ok();
		}

		public FormValidation doCheckPassword(@QueryParameter String value) throws IOException, ServletException {
			if (StringUtils.isBlank(value)) {
				return FormValidation.error("Please set a password");
			}
			return FormValidation.ok();
		}

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}

		public String getDisplayName() {
			return DISPLAY_NAME;
		}

		public boolean configure(StaplerRequest req, JSONObject formData) throws Descriptor.FormException {
			save();
			return super.configure(req, formData);
		}
	}

}
