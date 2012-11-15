/**
 * JPaaS
 * Copyright 2012 Bull S.A.S.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * $Id:$
 */ 
package org.ow2.jonas.jpaas.environment.manager.bean;

import org.ow2.bonita.facade.ManagementAPI;
import org.ow2.bonita.facade.QueryDefinitionAPI;
import org.ow2.bonita.facade.QueryRuntimeAPI;
import org.ow2.bonita.facade.RuntimeAPI;
import org.ow2.bonita.facade.def.element.BusinessArchive;
import org.ow2.bonita.facade.def.majorElement.ProcessDefinition;
import org.ow2.bonita.facade.exception.ProcessNotFoundException;
import org.ow2.bonita.facade.exception.VariableNotFoundException;
import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.ProcessInstanceUUID;
import org.ow2.bonita.light.LightProcessInstance;
import org.ow2.bonita.util.AccessorUtil;
import org.ow2.bonita.util.BonitaConstants;
import org.ow2.bonita.util.BusinessArchiveFactory;
import org.ow2.bonita.util.SimpleCallbackHandler;
import org.ow2.easybeans.osgi.annotation.OSGiResource;
import org.ow2.jonas.jpaas.environment.manager.api.EnvironmentManager;
import org.ow2.jonas.jpaas.environment.manager.api.EnvironmentManagerBeanException;
import org.ow2.jonas.jpaas.manager.api.ApplicationVersionInstance;
import org.ow2.jonas.jpaas.manager.api.Connector;
import org.ow2.jonas.jpaas.manager.api.Datasource;
import org.ow2.jonas.jpaas.manager.api.Environment;
import org.ow2.jonas.jpaas.manager.api.ExternalDatabase;
import org.ow2.jonas.jpaas.manager.api.JkRouter;
import org.ow2.jonas.jpaas.manager.api.JonasContainer;
import org.ow2.jonas.jpaas.manager.api.Node;
import org.ow2.jonas.jpaas.manager.api.Relationship;
import org.ow2.jonas.jpaas.manager.api.Topology;
import org.ow2.jonas.jpaas.sr.facade.api.ISrEnvironmentFacade;
import org.ow2.jonas.jpaas.sr.facade.vo.ConnectorTemplateVO;
import org.ow2.jonas.jpaas.sr.facade.vo.ContainerNodeTemplateVO;
import org.ow2.jonas.jpaas.sr.facade.vo.DatabaseNodeTemplateVO;
import org.ow2.jonas.jpaas.sr.facade.vo.EnvironmentVO;
import org.ow2.jonas.jpaas.sr.facade.vo.NodeTemplateVO;
import org.ow2.jonas.jpaas.sr.facade.vo.RelationshipTemplateVO;
import org.ow2.jonas.jpaas.sr.facade.vo.RouterNodeTemplateVO;
import org.ow2.util.log.Log;
import org.ow2.util.log.LogFactory;

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

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
    private QueryRuntimeAPI queryRuntimeAPI;        // for process not finished
    private QueryRuntimeAPI queryRuntimeAPIHistory; // for process finished and transfert into history
    private ProcessDefinitionUUID uuidProcessCreateEnvironment = null;
    private ProcessDefinitionUUID uuidProcessDeleteEnvironment = null;
    private ProcessInstanceUUID uuidInstance = null;
    private LoginContext loginContext = null;

    @OSGiResource
    private ISrEnvironmentFacade envSR;

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
            deployBarProcess();

            if (uuidProcessCreateEnvironment != null) {
                ExecutorService es = Executors.newFixedThreadPool(3);
                final Future<Environment> future = es.submit(new Callable<Environment>() {
                    public Environment call() throws Exception {
                        try {
                            login();
                            uuidInstance = runtimeAPI.instantiateProcess(uuidProcessCreateEnvironment, param);
                            Environment env = new Environment();

                            env.setEnvId(uuidInstance.getValue());

                            // wait until processInstance is finished
                            Set<LightProcessInstance> lightProcessInstances = queryRuntimeAPIHistory.getLightProcessInstances();
                            waitProcessInstanceUUIDIsFinished(uuidInstance);

                            // read Variable process instance to detect errors
                            String variableErrorRouteur = (String) queryRuntimeAPIHistory.getProcessInstanceVariable(uuidInstance, "errorCode");
                            String environmentID = (String) queryRuntimeAPIHistory.getProcessInstanceVariable(uuidInstance, "environmentID");
                            env.setEnvId(environmentID);

                            if (!variableErrorRouteur.equals(""))
                                env.setState(Environment.ENVIRONMENT_FAILED);
                            else
                                env.setState(Environment.ENVIRONMENT_STOPPED);
                            return env;
                        } catch (ProcessNotFoundException e) {
                            e.printStackTrace();
                            throw (new EnvironmentManagerBeanException("Error during intanciation of the process CreateEnvironment, process not found"));
                        } catch (org.ow2.bonita.facade.exception.VariableNotFoundException e) {
                            e.printStackTrace();
                            throw (new EnvironmentManagerBeanException("Error during intanciation of the process CreateEnvironment, variable not found"));
                        } finally {
                            logout();
                        }
                    }
                });
                return future;
            } else {
                throw (new EnvironmentManagerBeanException("process CreateEnvironment can't be deploy on server..."));
            }
        } finally {
            logger.info("JPAAS-ENVIRONMENT-MANAGER / createEnvironment finished");
        }
    }

    public Future deleteEnvironment(String envid) throws EnvironmentManagerBeanException {
        logger.info("JPAAS-ENVIRONMENT-MANAGER / deleteEnvironment called");
        final Map param = new HashMap();
        param.put("environmentID", envid);
        try {
            logger.info("JPAAS-ENVIRONMENT-MANAGER / deleteEnvironment called : " + envid);
            login();
            deployBarProcess();

            if (uuidProcessDeleteEnvironment != null) {
                ExecutorService es = Executors.newFixedThreadPool(3);
                final AtomicReference<Future> future = new AtomicReference<Future>(es.submit(new Callable<Void>() {
                    public Void call() throws Exception {
                        try {
                            login();
                            uuidInstance = runtimeAPI.instantiateProcess(uuidProcessDeleteEnvironment, param);

                            // wait until processInstance is finished
                            Set<LightProcessInstance> lightProcessInstances = queryRuntimeAPIHistory.getLightProcessInstances();
                            waitProcessInstanceUUIDIsFinished(uuidInstance);

                            return null;
                        } catch (ProcessNotFoundException e) {
                            e.printStackTrace();
                            throw (new EnvironmentManagerBeanException("Error during intanciation of the process deleteEnvironment, process not found"));
                        } catch (VariableNotFoundException e) {
                            e.printStackTrace();
                            throw (new EnvironmentManagerBeanException("Error during intanciation of the process deleteEnvironment, variable not found"));
                        } finally {
                            logout();
                        }
                    }
                }));
                return future.get();
            } else {
                throw (new EnvironmentManagerBeanException("process DeleteEnvironment can't be deploy on server..."));
            }
        } finally {
            logger.info("JPAAS-ENVIRONMENT-MANAGER / deleteEnvironment finished");
        }
    }

    public List<Environment> findEnvironments() {
        //TODO

        logger.info("JPAAS-ENVIRONMENT-MANAGER / findEnvironments called JEJE");

        // For this prototype user is defined statically
        List<EnvironmentVO> listEnvVO = envSR.findEnvironments("1");
        if (listEnvVO != null)
            return environmentVOListToEnvironmentList(listEnvVO);
        else
            return new ArrayList<Environment>();
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

    // deploy process if necessary
    private void deployBarProcess() {

        String processCreateEnvironment = "CreateEnvironment--1.0.bar";  // default value overrides by specific deployment descritor value
        String subProcessRouterCreateEnvironment = "InstanciateRouter--1.0.bar";
        String subProcessContainerCreateEnvironment = "InstanciateContainer--1.0.bar";
        String subProcessDBCreateEnvironment = "InstanciateDatabase--1.0.bar";
        String subProcessConnectorsCreateEnvironment = "InstanciateConnectors--1.0.bar";
        String subProcessConnectorCreateEnvironment = "InstanciateConnector--1.0.bar";
        String processDeleteEnvironment = "DeleteEnvironment--1.0.bar";

        deployProcess(subProcessRouterCreateEnvironment);
        deployProcess(subProcessContainerCreateEnvironment);
        deployProcess(subProcessDBCreateEnvironment);
        deployProcess(subProcessConnectorsCreateEnvironment);
        deployProcess(subProcessConnectorCreateEnvironment);
        uuidProcessCreateEnvironment = deployProcess(processCreateEnvironment);

        uuidProcessDeleteEnvironment = deployProcess(processDeleteEnvironment);
    }

    private ProcessDefinitionUUID deployProcess(String processName) {
        final File tempFileBarProcess;
        ProcessDefinitionUUID result = null;
        try {
            URL processBar = EnvironmentManagerBean.class.getClassLoader().getResource(processName);
            tempFileBarProcess = createTempFileBar(processBar);
            BusinessArchive businessArchive = BusinessArchiveFactory.getBusinessArchive(tempFileBarProcess);
            result = deployBarFile(businessArchive);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (EnvironmentManagerBeanException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
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
        queryRuntimeAPIHistory = AccessorUtil.getQueryRuntimeAPI(AccessorUtil.QUERYLIST_HISTORY_KEY);
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

    private void waitProcessInstanceUUIDIsFinished(ProcessInstanceUUID uuidInstance) {
        Set<LightProcessInstance> lightProcessInstances = queryRuntimeAPIHistory.getLightProcessInstances();
        Iterator iter = lightProcessInstances.iterator();
        LightProcessInstance processInstanceCurrent = null;
        boolean processExist = false;
        while (!processExist) {
            if (!iter.hasNext()) {
                lightProcessInstances = queryRuntimeAPIHistory.getLightProcessInstances();
                iter = lightProcessInstances.iterator();
            }
            if (iter.hasNext()) {
                processInstanceCurrent = (LightProcessInstance) iter.next();

                if (processInstanceCurrent.getProcessInstanceUUID().equals(uuidInstance))
                    processExist = true;
            }
        }
    }

    private List<Environment> environmentVOListToEnvironmentList(List<EnvironmentVO> environmentVOList) {
        List<Environment> resultList = new ArrayList<Environment>();
        for (EnvironmentVO tmpEnv : environmentVOList) {
            Environment env = new Environment();
            env.setEnvId(tmpEnv.getId());
            env.setEnvName(tmpEnv.getName());
            env.setEnvDesc(tmpEnv.getDescription());

            List<NodeTemplateVO> nodesTemplateVO = tmpEnv.getTopologyTemplate().getNodeTemplateList();
            Topology topo = new Topology();

            //Nodes
            List<Node> nodeList = new ArrayList<Node>();
            for (NodeTemplateVO tmpNode : nodesTemplateVO) {
                if (tmpNode instanceof RouterNodeTemplateVO) {
                    JkRouter node = new JkRouter();
                    node.setId(tmpNode.getId());
                    node.setMaxSize(tmpNode.getMaxSize());
                    node.setMinSize(tmpNode.getMinSize());
                    node.setName(tmpNode.getName());
                    node.setCurrentSize(tmpNode.getCurrentSize());
                    nodeList.add(node);
                } else if (tmpNode instanceof ContainerNodeTemplateVO) {
                    JonasContainer node = new JonasContainer();
                    node.setId(tmpNode.getId());
                    node.setMaxSize(tmpNode.getMaxSize());
                    node.setMinSize(tmpNode.getMinSize());
                    node.setName(tmpNode.getName());
                    node.setCurrentSize(tmpNode.getCurrentSize());
                    nodeList.add(node);
                } else if (tmpNode instanceof DatabaseNodeTemplateVO) {
                    ExternalDatabase node = new ExternalDatabase();
                    node.setId(tmpNode.getId());
                    node.setMaxSize(tmpNode.getMaxSize());
                    node.setMinSize(tmpNode.getMinSize());
                    node.setName(tmpNode.getName());
                    node.setCurrentSize(tmpNode.getCurrentSize());
                    nodeList.add(node);
                }
                topo.setNodeList(nodeList);
            }

            //RelationShips
            List<RelationshipTemplateVO> relationShipVO = tmpEnv.getTopologyTemplate().getRelationshipTemplateList();
            List<Relationship> listRelationShip = new ArrayList<Relationship>();
            for (RelationshipTemplateVO tmpRel : relationShipVO) {
                if (tmpRel instanceof ConnectorTemplateVO) {
                    Connector conn = new Connector();
                    conn.setRouterId(tmpRel.getId());
                    conn.setContainerId(tmpRel.getTemplateId());
                    listRelationShip.add(conn);
                } else {
                    Datasource datasource = new Datasource();
                    datasource.setDatabaseId(tmpRel.getId());
                    datasource.setContainerId(tmpRel.getTemplateId());
                    listRelationShip.add(datasource);
                }

            }
            topo.setRelationShipList(listRelationShip);
            env.setTopology(topo);
            resultList.add(env);

            // TODO ADD Application link into Environment
        }
        return resultList;
    }
}
