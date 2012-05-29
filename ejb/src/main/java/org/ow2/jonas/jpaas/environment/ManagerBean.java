package org.ow2.jonas.jpaas.environment;

import org.ow2.bonita.facade.ManagementAPI;
import org.ow2.bonita.facade.QueryDefinitionAPI;
import org.ow2.bonita.facade.RuntimeAPI;
import org.ow2.bonita.facade.def.element.BusinessArchive;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.exception.ProcessNotFoundException;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.util.AccessorUtil;
import org.ow2.bonita.util.BonitaConstants;
import org.ow2.bonita.util.BusinessArchiveFactory;
import org.ow2.bonita.util.SimpleCallbackHandler;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateful;
import javax.naming.Context;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateful
@Local(ManagerLocal.class)
@Remote(ManagerRemote.class)
public class ManagerBean {

  private QueryDefinitionAPI queryDefinitionAPI;
  private RuntimeAPI runtimeAPI;
  private ManagementAPI managementAPI;
  private ProcessDefinitionUUID uuidProcessCreateEnvironnement = null;
  private ProcessInstanceUUID uuidInstance = null;
  private LoginContext loginContext = null;

  @Resource(name = "processNameAndVersionCreateEnvironment")
  private String processNameAndVersionCreateEnvironment = "CreateEnvironement--1.0.bar";  // default value overrides by specific deployement descritor value

  public ManagerBean() throws ManagerBeanException {
     initEnv();
  }

  public String createEnvironment(String environmentTemplateDescriptor) throws ManagerBeanException {
    try {
      login();
      // deploy process once only
      synchronized (this) {
        if (uuidProcessCreateEnvironnement == null)
          deployProcessCreateEnvironment();
      }
      Map param = new HashMap();
      //  TODO : ajouter paramètres param.put("Env", environmentTemplateDescriptor);
      if (uuidProcessCreateEnvironnement != null) {
         uuidInstance = runtimeAPI.instantiateProcess(uuidProcessCreateEnvironnement);
         return uuidInstance.getValue();
      }
      else {
         throw (new ManagerBeanException("process CreateEnvironment can't be deploy on server..."));
      }
    } catch (ProcessNotFoundException e) {
      e.printStackTrace();
      return null;
      // } catch (VariableNotFoundException e) {
      //   e.printStackTrace();
      //   return null;
    } catch (ManagerBeanException e) {
      e.printStackTrace();
      throw (new ManagerBeanException("Error during deployment of the process CreateEnvironment"));
    } finally {
      logout();
    }
  }

  public void deleteEnvironment(String envid) {
    //TODO
    System.out.println("JPAAS-ENVIRONMENT-MANAGER / deleteEnvironment called");
  }

  public List findEnvironments() {
    //TODO
    System.out.println("JPAAS-ENVIRONMENT-MANAGER / findEnvironments called");
    return null;
  }

  public String startEnvironment(String envId) {
    //TODO
    System.out.println("JPAAS-ENVIRONMENT-MANAGER / startEnvironment called");
    return null;
  }

  public String stopEnvironment(String envId) {
    //TODO
    System.out.println("JPAAS-ENVIRONMENT-MANAGER / stopEnvironment called");
    return null;
  }

  public void deployApplication(String envId, String appId,String versionId, String instanceId) {
    //TODO
    System.out.println("JPAAS-ENVIRONMENT-MANAGER / deployApplication called");
  }

  public void undeployApplication(String envId, String appId,String versionId, String instanceId) {
    //TODO
    System.out.println("JPAAS-ENVIRONMENT-MANAGER / undeployApplication called");
  }

  public void getEnvironment(String envId) {
    //TODO
    System.out.println("JPAAS-ENVIRONMENT-MANAGER / getEnvironment called");
  }

  public void getDeployedApplicationVersionInstance(String envId) {
    //TODO
    System.out.println("JPAAS-ENVIRONMENT-MANAGER / getDeployedApplicationVersionInstance called");
  }


  private boolean deployProcessCreateEnvironment() {
    final File tempFileBarProcess;
    try {
      URL processBar = ManagerBean.class.getClassLoader().getResource(processNameAndVersionCreateEnvironment);
      tempFileBarProcess = createTempFileBar(processBar);
      BusinessArchive businessArchive = BusinessArchiveFactory.getBusinessArchive(tempFileBarProcess);
      uuidProcessCreateEnvironnement = deployBarFile(businessArchive);
      return true;
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      return false;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    } catch (ManagerBeanException e) {
      e.printStackTrace();
      return false;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  private File createTempFileBar(URL processBar) throws IOException {
    File tempFile = null;
    tempFile = File.createTempFile("MyProcess", ".bar");   // empty file
    tempFile.deleteOnExit();
    java.io.FileOutputStream destinationFile = new java.io.FileOutputStream(tempFile);
    InputStream processBarStream = processBar.openStream();
    try {
      // Lecture par segment de 0.5Mo
      byte buffer[] = new byte[512 * 1024];
      int nbLecture;
      while ((nbLecture = processBarStream.read(buffer)) != -1) {
        destinationFile.write(buffer, 0, nbLecture);
      }
    } finally {
      destinationFile.close();
    }
    return (tempFile);
  }

  private ProcessDefinitionUUID deployBarFile(BusinessArchive businessArchive) throws Exception {
    try {
      ProcessDefinition p = queryDefinitionAPI.getProcess(businessArchive.getProcessDefinition().getUUID());
      return p.getUUID();
    } catch (ProcessNotFoundException e) {
      final ProcessDefinition process = managementAPI.deploy(businessArchive); //deployJar
      return process.getUUID();
    }
  }

  private void initEnv() throws ManagerBeanException {
    String DEFAULT_INITIAL_CONTEXT_FACTORY = "org.objectweb.carol.jndi.spi.MultiOrbInitialContextFactory";

    System.setProperty(BonitaConstants.API_TYPE_PROPERTY, "EJB3");
    System.setProperty(Context.INITIAL_CONTEXT_FACTORY, DEFAULT_INITIAL_CONTEXT_FACTORY);
    System.setProperty("java.naming.provider.url", "rmi://localhost:7099");
    queryDefinitionAPI = AccessorUtil.getQueryDefinitionAPI();
    runtimeAPI = AccessorUtil.getRuntimeAPI();
    managementAPI = AccessorUtil.getManagementAPI();
  }

  private void login() throws ManagerBeanException {
    String login = "admin";
    String password = "bpm";
    try {
      if (loginContext == null) {
        loginContext = new LoginContext("BonitaStore", new SimpleCallbackHandler(login, password));
      }
      if (loginContext == null) {
        throw (new ManagerBeanException("Error during login with login:" + login + "and password:" + password));
      } else {
        loginContext.login();
      }
    } catch (LoginException e) {
      e.printStackTrace();
      throw (new ManagerBeanException("Error during login with login:" + login + "and password:" + password));
    }
  }

  private void logout() throws ManagerBeanException {
    try {
      if (loginContext != null) {
        loginContext.logout();
        loginContext = null;
      } else
        throw (new ManagerBeanException("Error during logout. The loginContext is null"));
    } catch (LoginException e) {
      e.printStackTrace();
      throw (new ManagerBeanException("Error during logout : loginException"));
    }
  }
}
