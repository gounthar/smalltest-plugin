package com.smalltest.smalltest;

import com.smalltest.utils.HttpUtils;
import com.smalltest.utils.ResponseEntity;
import hudson.model.AbstractBuild;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.ClassResult;
import hudson.tasks.junit.SuiteResult;
import hudson.tasks.junit.TestResult;
import jenkins.model.Jenkins;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JiraLogService {

  public void submitTestLogs(
          String jiraUrl, String username, String password, AbstractBuild build, List<TestResult> testResults)
          throws Exception {
    Jenkins instance = Jenkins.getInstance();
    String rootUrl = instance == null ? "" : instance.getRootUrl();
    String buildUrl = rootUrl + build.getUrl();
    Set<String> processedNames = new HashSet<>();
    for (TestResult testResult : testResults) {
      Collection<SuiteResult> suiteResults = testResult.getSuites();
      for (SuiteResult suiteResult : suiteResults) {
        List<CaseResult> cases = suiteResult.getCases();
        for (CaseResult caseResult : cases) {
          ClassResult classResult = caseResult.getParent();
          String classFullName = classResult.getFullName();
          if (!processedNames.contains(classFullName)) {
            processedNames.add(classFullName);
            String className = classResult.getClassName();
            String issueKey = getIssueKey(className);
            if (issueKey != null) {
              String status = "SKIPPED";
              if (classResult.getFailCount() > 0) {
                status = "FAILED";
              } else if (classResult.getPassCount() > 0) {
                status = "PASSED";
              }
              submitToJira(jiraUrl, username, password, buildUrl, classFullName, issueKey, status);
            }
          }
          String caseFullName = caseResult.getFullName();
          if (!processedNames.contains(caseFullName)) {
            processedNames.add(caseFullName);
            String testName = caseResult.getName();
            String issueKey = getIssueKey(testName);
            if (issueKey != null) {
              String status = "SKIPPED";
              if (caseResult.isFailed()) {
                status = "FAILED";
              } else if (caseResult.isPassed()) {
                status = "PASSED";
              }
              submitToJira(jiraUrl, username, password, buildUrl, caseFullName, issueKey, status);
            }
          }
        }
      }
    }
  }

  private void submitToJira(
          String jiraUrl, String username, String password,
          String buildUrl, String test, String issueKey, String status)
          throws Exception {
    JiraTestLog testLog = new JiraTestLog();
    String comment = "* Build: " + buildUrl + "\n* Test: " + test;
    testLog.setComment(comment);
    testLog.setStatus(status);
    testLog.setIssueKey(issueKey);
    submitToJira(jiraUrl, username, password, testLog);
  }

  private String getIssueKey(String className) {
    String regex = "[a-zA-Z][a-zA-Z]+[_|-]\\d+";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(className);

    if (matcher.find()) {
      String issueKey = matcher.group();
      return issueKey.replace("_", "-");
    }
    return null;
  }

  private ResponseEntity submitToJira(
          String jiraUrl, String username, String password, JiraTestLog testLog)
          throws Exception {
    String authenticate = username + ":" + password;
    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Basic " + Base64.encodeBase64String(authenticate.getBytes("UTF-8")));
    HttpEntity entity = HttpUtils.toEncodedFormEntity(testLog);

    String url = jiraUrl + "/rest/smalltest/1/test-logs";
    return HttpUtils.post(url, headers, entity);
  }
}
