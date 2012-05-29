package org.ow2.jonas.jpaas.environment;


import java.util.List;

public interface ManagerLocal {
  public String createEnvironment(String environmentTemplateDescriptor) throws ManagerBeanException;
  public void deleteEnvironment(String envid);
  public List findEnvironments();
  public String startEnvironment(String envId);
  public String stopEnvironment(String envId);
  public void deployApplication(String envId, String appId,String versionId, String instanceId);
  public void undeployApplication(String envId, String appId,String versionId, String instanceId);
  public void getEnvironment(String envId);
  public void getDeployedApplicationVersionInstance(String envId);
}
