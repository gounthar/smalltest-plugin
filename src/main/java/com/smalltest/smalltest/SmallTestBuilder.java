package com.smalltest.smalltest;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.*;
import hudson.tasks.junit.TestResult;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.AggregatedTestResultAction;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Sample {@link Builder}.
 * <p>
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link SmallTestBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields
 * to remember the configuration.
 * <p>
 * <p>
 * When a build is performed, the {@link #perform} method will be invoked.
 *
 * @author Kohsuke Kawaguchi
 */
public class SmallTestBuilder extends Notifier {

  private final String jiraUrl;

  private final String username;

  private final String password;

  // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
  @DataBoundConstructor
  public SmallTestBuilder(String jiraUrl, String username, String password) {
    this.jiraUrl = jiraUrl;
    this.username = username;
    this.password = password;
  }

  /**
   * We'll use this from the {@code config.jelly}.
   */
  public String getJiraUrl() {
    return jiraUrl;
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  @Override
  public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
    try {
      // This is where you 'build' the project.
      // Since this is a dummy, we just say 'hello world' and call that a build.

      // This also shows how you can consult the global configuration of the builder
      listener.getLogger().println("Parsing the test results");

      TestResultAction resultAction = build.getAction(TestResultAction.class);
      List<TestResult> testResults = new ArrayList<>();
      if (resultAction != null) {
        testResults.add(resultAction.getResult());
      } else {
        AggregatedTestResultAction aggregatedTestResultAction = build.getAction(AggregatedTestResultAction.class);
        if (aggregatedTestResultAction != null) {
          List<AggregatedTestResultAction.ChildReport> childReports = aggregatedTestResultAction.getResult();
          if (childReports != null) {
            for (AggregatedTestResultAction.ChildReport childReport : childReports) {
              if (childReport.result instanceof TestResult) {
                testResults.add((TestResult) childReport.result);
              }
            }
          }
        }
      }
      if (!testResults.isEmpty()) {
        listener.getLogger().println("Submitting test results to " + jiraUrl + " on behalf of " + username);
        new JiraLogService().submitTestLogs(jiraUrl, username, password, build, testResults);
      }
    } catch (Exception e) {
      listener.getLogger().println("Failed to submit test results " + e);
    }
    return true;
  }

  // Overridden for better type safety.
  // If your plugin doesn't really define any property on Descriptor,
  // you don't have to do this.
  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  @Override
  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  /**
   * Descriptor for {@link SmallTestBuilder}. Used as a singleton.
   * The class is marked as public so that it can be accessed from views.
   * <p>
   * <p>
   * See {@code src/main/resources/hudson/plugins/hello_world/SmallTestBuilder/*.jelly}
   * for the actual HTML fragment for the configuration screen.
   */
  @Extension // This indicates to Jenkins that this is an implementation of an extension point.
  public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
    /**
     * To persist global configuration information,
     * simply store it in a field and call save().
     *
     * <p>
     * If you don't want fields to be persisted, use {@code transient}.
     */
//        private String useFrench;

    /**
     * In order to load the persisted global configuration, you have to
     * call load() in the constructor.
     */
    public DescriptorImpl() {
      load();
    }

    /**
     * Performs on-the-fly validation of the form field 'name'.
     *
     * @param value This parameter receives the value that the user has typed.
     * @return Indicates the outcome of the validation. This is sent to the browser.
     * <p>
     * Note that returning {@link FormValidation#error(String)} does not
     * prevent the form from being saved. It just means that a message
     * will be displayed to the user.
     */
    public FormValidation doCheckJiraUrl(@QueryParameter String value)
      throws IOException, ServletException {
      boolean withProtocol = value.startsWith("http://") || value.startsWith("https://");
      if (!withProtocol) {
        return FormValidation.error("JIRA URL must start with http:// or https://.");
      }
      return FormValidation.ok();
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      // Indicates that this builder can be used with all kinds of project types
      return true;
    }

    /**
     * This human readable name is used in the configuration screen.
     */
    public String getDisplayName() {
      return "Submit test results to SmallTest";
    }

//        @Override
//        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
    // To persist global configuration information,
    // set that to properties and call save().
//            useFrench = formData.getBoolean("useFrench");
    // ^Can also use req.bindJSON(this, formData);
    //  (easier when there are many fields; need set* methods for this, like setUseFrench)
//            save();
//            return super.configure(req,formData);
//        }

    /**
     * This method returns true if the global configuration says we should speak French.
     *
     * The method name is bit awkward because global.jelly calls this method to determine
     * the initial state of the checkbox by the naming convention.
     */
//        public boolean getUseFrench() {
//            return useFrench;
//        }
  }
}

