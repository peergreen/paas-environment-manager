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
package org.ow2.jonas.jpaas.environment.manager.bean;

import org.ow2.bonita.facade.ManagementAPI;
import org.ow2.bonita.facade.QueryDefinitionAPI;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.RuntimeAPI;
import org.ow2.bonita.facade.def.element.BusinessArchive;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.exception.InstanceNotFoundException;
import org.ow2.bonita.facade.exception.ProcessNotFoundException;
import org.ow2.bonita.facade.runtime.InstanceState;
import org.ow2.bonita.facade.runtime.ProcessInstance;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.util.AccessorUtil;
import org.ow2.bonita.util.BonitaConstants;
import org.ow2.bonita.util.BusinessArchiveFactory;
import org.ow2.bonita.util.SimpleCallbackHandler;
import org.ow2.jonas.jpaas.environment.manager.api.EnvironmentManager;
import org.ow2.jonas.jpaas.environment.manager.api.EnvironmentManagerBeanException;
import org.ow2.jonas.jpaas.manager.api.ApplicationVersionInstance;
import org.ow2.jonas.jpaas.manager.api.Environment;
import org.ow2.util.log.Log;
import org.ow2.util.log.LogFactory;

import javax.annotation.Resource;
import javax.ejb.Local;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.naming.Context;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Stateless(mappedName = "EnvironmentManagerBean")
@Local(EnvironmentManager.class)
@Remote(EnvironmentManager.class)
public class EnvironmentManagerBean implements EnvironmentManager {

  /**
  * The logger
  */
  private Log logger = LogFactory.getLog(EnvironmentManagerBean.class);

  private QueryDefinitionAPI queryDefinitionAPI;
  private RuntimeAPI runtimeAPI;
  private ManagementAPI managementAPI;
  private QueryRuntimeAPI queryRuntimeAPI;
  private ProcessDefinitionUUID uuidProcessCreateEnvironnement = null;
  private ProcessInstanceUUID uuidInstance = null;
  private LoginContext loginContext = null;

  @Resource(name = "processNameAndVersionCreateEnvironment")
  private String processCreateEnvironment = "CreateEnvironment--1.0.bar";  // default value overrides by specific deployment descritor value
  private String subProcessRouterCreateEnvironment = "InstanciateRouter--1.0.bar";
  private String subProcessContainerCreateEnvironment = "InstanciateContainer--1.0.bar";
  private String subProcessDBCreateEnvironment = "InstanciateDatabase--1.0.bar";

  public EnvironmentManagerBean() throws EnvironmentManagerBeanException {
    login();
    initEnv();
    logout();
  }

  public Future<Environment> createEnvironment(String environmentTemplateDescriptor) throws EnvironmentManagerBeanException {
    final Map param = new HashMap();
    param.put("environmentTemplateDescriptor", environmentTemplateDescriptor);
    try {
      logger.info("JPAAS-ENVIRONMENT-MANAGER / createEnvironment called : " + environmentTemplateDescriptor);
      login();
      // deploy process if necessary
      deployProcess(subProcessRouterCreateEnvironment);
      deployProcess(subProcessContainerCreateEnvironment);
      deployProcess(subProcessDBCreateEnvironment);
      deployProcess(processCreateEnvironment);

      if (uuidProcessCreateEnvironnement != null) {
        ExecutorService es = Executors.newFixedThreadPool(3);
        final Future<Environment> future = es.submit(new Callable<Environment>() {
          public Environment call() throws Exception {
            try {
              login();
              uuidInstance = runtimeAPI.instantiateProcess(uuidProcessCreateEnvironnement, param);
              Environment env = new Environment();

              env.setEnvId(uuidInstance.getValue());

              // wait until processInstance is finished
              ProcessInstance processInstance = queryRuntimeAPI.getProcessInstance(uuidInstance);
              InstanceState state = processInstance.getInstanceState();
              while (state != InstanceState.STARTED) {
                state = processInstance.getInstanceState();
              }
              if (state == InstanceState.ABORTED)
                throw (new EnvironmentManagerBeanException("Error during execution of processus (state = ABORTED)"));
              else if (state == InstanceState.CANCELLED)
                throw (new EnvironmentManagerBeanException("Error during execution of processus (state = CANCELLED)"));
              // The state of processus is FINISHED
              return env;
            } catch (ProcessNotFoundException e) {
              e.printStackTrace();
              throw (new EnvironmentManagerBeanException("Error during intanciation of the process CreateEnvironment, process not found"));
            } catch (org.ow2.bonita.facade.exception.VariableNotFoundException e) {
              e.printStackTrace();
              throw (new EnvironmentManagerBeanException("Error during intanciation of the process CreateEnvironment, variable not found"));
            } catch (InstanceNotFoundException e) {
              e.printStackTrace();
              throw (new EnvironmentManagerBeanException("Error during intanciation of the process CreateEnvironment, instance not found"));
            }  finally {
              logout();
            }
          }
        });
        return future;
      } else {
        throw (new EnvironmentManagerBeanException("process CreateEnvironment can't be deploy on server..."));
      }
    } finally {
      logout();
      logger.info("JPAAS-ENVIRONMENT-MANAGER / createEnvironment finished");
    }
  }

  public void deleteEnvironment(String envid) {
    //TODO
    logger.info("JPAAS-ENVIRONMENT-MANAGER / deleteEnvironment called");
  }

  public List<Environment> findEnvironments() {
    //TODO

    logger.info("JPAAS-ENVIRONMENT-MANAGER / findEnvironments called");

    ArrayList<Environment> listEnv = new ArrayList<Environment>();

    //1rst environment
    Environment env1 = new Environment();
    env1.setEnvId("156");
    env1.setEnvName("My first environment");
//	  env1.setState(Environment.ENVIRONMENT_RUNNING);

    ApplicationVersionInstance instance1 = new ApplicationVersionInstance();
    instance1.setInstanceId("196");
    instance1.setInstanceName("1rst instance of the 1rst environment");
//	  instance1.setState(ApplicationVersionInstance.INSTANCE_STARTED);

    ApplicationVersionInstance instance2 = new ApplicationVersionInstance();
    instance2.setInstanceId("638");
    instance2.setInstanceName("2nd instance of the 1rst environment");
//	  instance2.setState(ApplicationVersionInstance.INSTANCE_RUNNING);

    env1.getListApplicationVersionInstance().add(instance1);
    env1.getListApplicationVersionInstance().add(instance2);

    //2nd environment
    Environment env2 = new Environment();
    env2.setEnvId("654");
    env2.setEnvName("My second environment");
//	  env2.setState(Environment.ENVIRONMENT_STOPPED);

    ApplicationVersionInstance instance3 = new ApplicationVersionInstance();
    instance3.setInstanceId("789");
    instance3.setInstanceName("1rst instance of the 2nd environment");
//	  instance3.setState(ApplicationVersionInstance.INSTANCE_STOPPED);

    ApplicationVersionInstance instance4 = new ApplicationVersionInstance();
    instance4.setInstanceId("256");
    instance4.setInstanceName("2nd instance of the 2nd environment");
///	  instance4.setState(ApplicationVersionInstance.INSTANCE_STOPPED);

    env2.getListApplicationVersionInstance().add(instance3);
    env2.getListApplicationVersionInstance().add(instance4);

    //add all environments in the list
    listEnv.add(env1);
    listEnv.add(env2);

    return listEnv;

  }

  public Future<Environment> startEnvironment(String envId) {
    //TODO
    logger.info("JPAAS-ENVIRONMENT-MANAGER / startEnvironment called");
    return null;
  }

  public Future<Environment> stopEnvironment(String envId) {
    //TODO
    logger.info("JPAAS-ENVIRONMENT-MANAGER / stopEnvironment called");
    return null;
  }

  public Future<ApplicationVersionInstance> deployApplication(String envId, String appId, String versionId, String instanceId) {
    //TODO
    logger.info("JPAAS-ENVIRONMENT-MANAGER / deployApplication called");
    return null;
  }

  public Future<ApplicationVersionInstance> undeployApplication(String envId, String appId, String versionId, String instanceId) {
    //TODO
    logger.info("JPAAS-ENVIRONMENT-MANAGER / undeployApplication called");
    return null;
  }

  public Environment getEnvironment(String envId) {
    //TODO
    logger.info("JPAAS-ENVIRONMENT-MANAGER / getEnvironment called");
    return null;
  }

  public List<ApplicationVersionInstance> getDeployedApplicationVersionInstance(String envId) {
    //TODO
    logger.info("JPAAS-ENVIRONMENT-MANAGER / getDeployedApplicationVersionInstance called");
    return null;
  }

  private boolean deployProcess(String processName) {
    final File tempFileBarProcess;
    try {
      URL processBar = EnvironmentManagerBean.class.getClassLoader().getResource(processName);
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
    } catch (EnvironmentManagerBeanException e) {
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
      logger.info("Deploy the process " + businessArchive.getProcessDefinition().getName());
      final ProcessDefinition process = managementAPI.deploy(businessArchive); //deployJar
      return process.getUUID();
    }
  }

  private void initEnv() throws EnvironmentManagerBeanException {
    String DEFAULT_INITIAL_CONTEXT_FACTORY = "org.objectweb.carol.jndi.spi.MultiOrbInitialContextFactory";

    System.setProperty(BonitaConstants.API_TYPE_PROPERTY, "EJB3");
    System.setProperty(Context.INITIAL_CONTEXT_FACTORY, DEFAULT_INITIAL_CONTEXT_FACTORY);
    queryDefinitionAPI = AccessorUtil.getQueryDefinitionAPI();
    runtimeAPI = AccessorUtil.getRuntimeAPI();
    managementAPI = AccessorUtil.getManagementAPI();
    queryRuntimeAPI = AccessorUtil.getQueryRuntimeAPI();
  }

  private void login() throws EnvironmentManagerBeanException {
    String login = "admin";
    String password = "bpm";
    try {
      if (loginContext == null) {
        loginContext = new LoginContext("BonitaStore", new SimpleCallbackHandler(login, password));
      }
      if (loginContext == null) {
        throw (new EnvironmentManagerBeanException("Error during login with login:" + login + "and password:" + password));
      } else {
        loginContext.login();
      }
    } catch (LoginException e) {
      e.printStackTrace();
      throw (new EnvironmentManagerBeanException("Error during login with login:" + login + "and password:" + password));
    }
  }

  private void logout() throws EnvironmentManagerBeanException {
    try {
      if (loginContext != null) {
        loginContext.logout();
        loginContext = null;
      } else
        throw (new EnvironmentManagerBeanException("Error during logout. The loginContext is null"));
    } catch (LoginException e) {
      e.printStackTrace();
      throw (new EnvironmentManagerBeanException("Error during logout : loginException"));
    }
  }
}
