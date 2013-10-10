package org.ow2.jonas.jpaas.environment.manager.bean;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
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
import org.ow2.jonas.jpaas.util.clouddescriptors.cloudapplication.CloudApplicationDesc;
import org.ow2.jonas.jpaas.util.clouddescriptors.cloudapplication.v1.generated.CloudApplicationType;
import org.ow2.jonas.jpaas.util.clouddescriptors.environmenttemplate.EnvironmentTemplateDesc;
import org.ow2.jonas.jpaas.util.clouddescriptors.environmenttemplate.v1.generated.EnvironmentTemplateType;
import org.ow2.util.log.Log;
import org.ow2.util.log.LogFactory;

import javax.naming.Context;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

@Component(immediate = true)
@Instantiate
@Provides
public class EnvironmentManagerBean implements EnvironmentManager {

    /**
     * The logger
     */
    private Log logger = LogFactory.getLog(EnvironmentManagerBean.class);


    public Future<Environment> createEnvironment(final String environmentTemplateDescriptor) throws EnvironmentManagerBeanException {
        final Map param = new HashMap();

        logger.info("JPAAS-ENVIRONMENT-MANAGER / createEnvironment called : " + environmentTemplateDescriptor);
        ExecutorService es = Executors.newFixedThreadPool(3);
        final Future<Environment> future = es.submit(new Callable<Environment>() {
            public Environment call() throws Exception {
                Environment env = new Environment();

                EnvironmentTemplateDesc environmentTemplateDesc = null;
                EnvironmentTemplateType environmentTemplate = null;

                try {
                    environmentTemplateDesc = new EnvironmentTemplateDesc(environmentTemplateDescriptor);
                    environmentTemplate = (EnvironmentTemplateType) environmentTemplateDesc.getEnvironmentTemplate();
                } catch (Exception e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
                env.setEnvId(UUID.randomUUID().toString());
                env.setEnvName(environmentTemplate.getName());
                return env;
            }
        });
        return future;
    }

    public Future deleteEnvironment(final String envId) throws EnvironmentManagerBeanException {
        logger.info("JPAAS-ENVIRONMENT-MANAGER / deleteEnvironment called");
        ExecutorService es = Executors.newFixedThreadPool(3);
        final Future<Environment> future = es.submit(new Callable<Environment>() {
            public Environment call() throws Exception {
                Environment env = new Environment();
                env.setEnvId(envId);
                env.setEnvName("myenv");

                return env;
            }
        });
        return future;
    }

    public List<Environment> findEnvironments() {
        Environment env = new Environment();
        env.setEnvId(UUID.randomUUID().toString());
        env.setEnvName("myenv");

        List<Environment> envList = new ArrayList<Environment>();
        envList.add(env);

        return envList;

    }

    public Future<Environment> startEnvironment(final String envId) {
        logger.info("JPAAS-ENVIRONMENT-MANAGER / startEnvironment called");
        ExecutorService es = Executors.newFixedThreadPool(3);
        final Future<Environment> future = es.submit(new Callable<Environment>() {
            public Environment call() throws Exception {
                Environment env = new Environment();
                env.setEnvId(envId);
                env.setEnvName("myenv");
                env.setState(Environment.ENVIRONMENT_RUNNING);

                return env;
            }
        });
        return future;

    }

    public Future<Environment> stopEnvironment(final String envId) {
        logger.info("JPAAS-ENVIRONMENT-MANAGER / stopEnvironment called");
        ExecutorService es = Executors.newFixedThreadPool(3);
        final Future<Environment> future = es.submit(new Callable<Environment>() {
            public Environment call() throws Exception {
                Environment env = new Environment();
                env.setEnvId(envId);
                env.setEnvName("myenv");
                env.setState(Environment.ENVIRONMENT_STOPPED);
                return env;
            }
        });
        return future;
    }

    public Future<ApplicationVersionInstance> deployApplication(final String envId, final String appId, final String versionId, final String instanceId) {
        logger.info("JPAAS-ENVIRONMENT-MANAGER / deployApplication called");
        ExecutorService es = Executors.newFixedThreadPool(3);
        final Future<ApplicationVersionInstance> future = es.submit(new Callable<ApplicationVersionInstance>() {
            public ApplicationVersionInstance call() throws Exception {
                ApplicationVersionInstance appVersionInstance = new ApplicationVersionInstance();
                appVersionInstance.setAppId(appId);
                appVersionInstance.setVersionId(versionId);
                appVersionInstance.setInstanceName(instanceId);
                appVersionInstance.setState(ApplicationVersionInstance.INSTANCE_STARTED);
                appVersionInstance.setTargetEnvId(envId);


                return appVersionInstance;
            }
        });
        return future;

    }

    public Future<ApplicationVersionInstance> undeployApplication(final String envId, final String appId, final String versionId, final String instanceId) {
        //TODO
        logger.info("JPAAS-ENVIRONMENT-MANAGER / undeployApplication called");
        ExecutorService es = Executors.newFixedThreadPool(3);
        final Future<ApplicationVersionInstance> future = es.submit(new Callable<ApplicationVersionInstance>() {
            public ApplicationVersionInstance call() throws Exception {
                ApplicationVersionInstance appVersionInstance = new ApplicationVersionInstance();
                appVersionInstance.setAppId(appId);
                appVersionInstance.setVersionId(versionId);
                appVersionInstance.setInstanceName(instanceId);
                appVersionInstance.setState(ApplicationVersionInstance.INSTANCE_STOPPED);
                appVersionInstance.setTargetEnvId(envId);


                return appVersionInstance;
            }
        });
        return future;
    }

    public Environment getEnvironment(final String envId) {
        //TODO
        logger.info("JPAAS-ENVIRONMENT-MANAGER / getEnvironment called");
        Environment env = new Environment();
        env.setEnvId(envId);
        env.setEnvName("myenv");
        return env;
    }

    public List<ApplicationVersionInstance> getDeployedApplicationVersionInstance(String envId) {
        //TODO
        logger.info("JPAAS-ENVIRONMENT-MANAGER / getDeployedApplicationVersionInstance called");
        ApplicationVersionInstance appVersionInstance = new ApplicationVersionInstance();
        appVersionInstance.setAppId(UUID.randomUUID().toString());
        appVersionInstance.setVersionId(UUID.randomUUID().toString());
        appVersionInstance.setInstanceName("myinstance");
        appVersionInstance.setState(ApplicationVersionInstance.INSTANCE_STARTED);
        appVersionInstance.setTargetEnvId(envId);

        List<ApplicationVersionInstance> appVersionInstanceList = new ArrayList<ApplicationVersionInstance>();
        appVersionInstanceList.add(appVersionInstance);

        return appVersionInstanceList;

    }

}
