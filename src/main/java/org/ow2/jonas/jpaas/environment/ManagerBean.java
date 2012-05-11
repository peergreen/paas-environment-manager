package org.ow2.jonas.jpaas.environment;

import org.ow2.bonita.facade.ManagementAPI;
import org.ow2.bonita.facade.QueryDefinitionAPI;
import org.ow2.bonita.facade.RuntimeAPI;
import org.ow2.bonita.facade.def.element.BusinessArchive;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.exception.ProcessNotFoundException;
import org.ow2.bonita.facade.exception.VariableNotFoundException;
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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Stateful
@Local(ManagerLocal.class)
@Remote(ManagerRemote.class)
public class ManagerBean {

  private static final String DEFAULT_INITIAL_CONTEXT_FACTORY = "org.objectweb.carol.jndi.spi.MultiOrbInitialContextFactory";

  private QueryDefinitionAPI queryDefinitionAPI;
  private RuntimeAPI runtimeAPI;
  private ManagementAPI managementAPI;
  private ProcessDefinitionUUID uuid = null;
  private ProcessInstanceUUID uuidInstance = null;
  private LoginContext loginContext = null;

  @Resource(name = "processNameCreateEnvironmentAndVersion")
  private String processNameCreateEnvironmentAndVersion = "CreateEnvironement--1.0.bar";

  public ManagerBean() {
    deployProcessCreateEnvironment();
  }

  private boolean deployProcessCreateEnvironment(){
    initEnv();
    login();
    try {
       URL processBar = ManagerBean.class.getClassLoader().getResource(processNameCreateEnvironmentAndVersion);
       BusinessArchive businessArchive = BusinessArchiveFactory.getBusinessArchive(new File(processBar.toURI()));
       ProcessDefinitionUUID uuid = deployBarFile(businessArchive);
       return true;
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      return false;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    } finally {
      logout();
    }
  }

  public String createEnvironment(String environmentTemplateDescriptor) {
    try {
      login();
      Map param = new HashMap();
      param.put("Env", environmentTemplateDescriptor);
      uuidInstance = runtimeAPI.instantiateProcess(uuid, param);
      return uuidInstance.getValue();
    } catch (ProcessNotFoundException e) {
      e.printStackTrace();
      return null;
    } catch (VariableNotFoundException e) {
      e.printStackTrace();
      return null;
    }  finally {
      logout();
    }
  }

  private ProcessDefinitionUUID deployBarFile(BusinessArchive businessArchive) throws Exception{
    //Check if process is already deployed
    QueryDefinitionAPI queryDefinitionAPI = AccessorUtil.getQueryDefinitionAPI();
    ManagementAPI managementAPI = AccessorUtil.getManagementAPI();
    try {
       ProcessDefinition p=queryDefinitionAPI.getProcess(businessArchive.getProcessDefinition().getUUID());
       return p.getUUID();
     } catch (ProcessNotFoundException e) {
       final ProcessDefinition process = managementAPI.deploy(businessArchive);
       return process.getUUID();
     }
  }

  private void initEnv() {
     System.setProperty(BonitaConstants.API_TYPE_PROPERTY, "EJB3");
     // System.setProperty(BonitaConstants.JEE_SERVER_PROPERTY, "http://localhost:9000/bonita");
     System.setProperty(Context.INITIAL_CONTEXT_FACTORY, DEFAULT_INITIAL_CONTEXT_FACTORY);
     System.setProperty("java.naming.provider.url", "rmi://localhost:7099");
     URL jaasFile = ManagerBean.class.getClassLoader().getResource("jaas-bonita.cfg");
     try {
       System.setProperty(BonitaConstants.JAAS_PROPERTY, jaasFile.toURI().getPath());
     } catch (URISyntaxException e) {
      e.printStackTrace();
     }
     QueryDefinitionAPI queryDefinitionAPI = AccessorUtil.getQueryDefinitionAPI();
     RuntimeAPI runtimeAPI = AccessorUtil.getRuntimeAPI();
     ManagementAPI managementAPI = AccessorUtil.getManagementAPI();
  }

  private boolean login() {
    try {
      if (loginContext == null) {
         LoginContext loginContext = new LoginContext("BonitaStore", new SimpleCallbackHandler("admin", "bpm"));
      }
      loginContext.login();
      return true;
    } catch (LoginException e) {
      e.printStackTrace();
      return false;
    }
  }

  private boolean logout() {
    try {
      if (loginContext != null) {
         loginContext.logout();
         return true;
      } else
        return false;
    } catch (LoginException e) {
      e.printStackTrace();
      return false;
    }
  }
}
