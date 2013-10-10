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
package org.ow2.jonas.jpaas.environment.manager.api;

import org.ow2.jonas.jpaas.manager.api.ApplicationVersionInstance;
import org.ow2.jonas.jpaas.manager.api.Environment;

import java.util.List;
import java.util.concurrent.Future;

public interface EnvironmentManager {
  public Future<Environment> createEnvironment(String environmentTemplateDescriptor) throws EnvironmentManagerBeanException;
  public Future deleteEnvironment(String envid) throws EnvironmentManagerBeanException;
  public List<Environment> findEnvironments();
  public Future<Environment> startEnvironment(String envId);
  public Future<Environment> stopEnvironment(String envId);
  public Future<ApplicationVersionInstance> deployApplication(String envId, String appId,String versionId, String instanceId);
  public Future<ApplicationVersionInstance> undeployApplication(String envId, String appId,String versionId, String instanceId);
  public Environment getEnvironment(String envId);
  public List<ApplicationVersionInstance> getDeployedApplicationVersionInstance(String envId);
}
