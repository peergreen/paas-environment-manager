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

import org.ow2.jonas.jpaas.manager.api.ApplicationVersionInstance;
import org.ow2.jonas.jpaas.manager.api.Environment;

import java.util.List;
import java.util.concurrent.Future;

public interface EnvironmentManagerLocal {
  public Environment createEnvironment(String environmentTemplateDescriptor) throws EnvironmentManagerBeanException;
  public void deleteEnvironment(String envid);
  public List<Environment> findEnvironments();
  public Future<Environment> startEnvironment(String envId);
  public Future<Environment> stopEnvironment(String envId);
  public Future<ApplicationVersionInstance> deployApplication(String envId, String appId,String versionId, String instanceId);
  public Future<ApplicationVersionInstance> undeployApplication(String envId, String appId,String versionId, String instanceId);
  public Environment getEnvironment(String envId);
  public List<ApplicationVersionInstance> getDeployedApplicationVersionInstance(String envId);
}
