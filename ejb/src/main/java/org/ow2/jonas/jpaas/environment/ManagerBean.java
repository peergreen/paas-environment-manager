/**
 * JPaaS Util
 * Copyright (C) 2012 Bull S.A.S.
 * Contact: jasmine@ow2.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * --------------------------------------------------------------------------
 * $Id$
 * --------------------------------------------------------------------------
 */
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
import org.ow2.jonas.jpaas.api.ApplicationVersionInstance;
import org.ow2.jonas.jpaas.api.Environment;

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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
  private String processNameAndVersionCreateEnvironment = "CreateEnvironment--1.0.bar";  // default value overrides by specific deployment descritor value

  public ManagerBean() throws ManagerBeanException {
     initEnv();
  }

  public Future<Environment> createEnvironment(String environmentTemplateDescriptor) throws ManagerBeanException {
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


         ExecutorService es = Executors.newFixedThreadPool(3);
         final Future<Environment> future = es.submit(new Callable() {
                    public Object call() throws Exception {
                        uuidInstance = runtimeAPI.instantiateProcess(uuidProcessCreateEnvironnement);
                        Environment env = new Environment();
                        env.setEnvId(uuidInstance.getValue());
                        return env;
                    }
         });

         return future;
      }
      else {
         throw (new ManagerBeanException("process CreateEnvironment can't be deploy on server..."));
      }
   // } catch (ProcessNotFoundException e) {
   //   e.printStackTrace();
   //   return null;
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

  public Future<Environment> startEnvironment(String envId) {
    //TODO
    System.out.println("JPAAS-ENVIRONMENT-MANAGER / startEnvironment called");
    return null;
  }

  public Future<Environment> stopEnvironment(String envId) {
    //TODO
    System.out.println("JPAAS-ENVIRONMENT-MANAGER / stopEnvironment called");
    return null;
  }

  public Future<ApplicationVersionInstance> deployApplication(String envId, String appId,String versionId, String instanceId) {
    //TODO
    System.out.println("JPAAS-ENVIRONMENT-MANAGER / deployApplication called");
    return null;
  }

  public Future<ApplicationVersionInstance> undeployApplication(String envId, String appId,String versionId, String instanceId) {
    //TODO
    System.out.println("JPAAS-ENVIRONMENT-MANAGER / undeployApplication called");
    return null;
  }

  public Environment getEnvironment(String envId) {
    //TODO
    System.out.println("JPAAS-ENVIRONMENT-MANAGER / getEnvironment called");
    return null;
  }

  public List<ApplicationVersionInstance> getDeployedApplicationVersionInstance(String envId) {
    //TODO
    System.out.println("JPAAS-ENVIRONMENT-MANAGER / getDeployedApplicationVersionInstance called");
    return null;
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
      // 0.5Mo
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
