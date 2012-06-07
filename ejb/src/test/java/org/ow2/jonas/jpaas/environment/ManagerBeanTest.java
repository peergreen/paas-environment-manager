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

import junit.framework.Assert;
import org.junit.Test;
import org.ow2.jonas.jpaas.api.Environment;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.login.LoginContext;
import java.util.Hashtable;


public class ManagerBeanTest {

  /**
   * Default InitialContextFactory to use.
   */
  private static final String DEFAULT_INITIAL_CONTEXT_FACTORY = "org.objectweb.carol.jndi.spi.MultiOrbInitialContextFactory";
  private static final String  DEFAULT_PROVIDER_URL = "rmi://localhost:7099";
  private static final String DEFAULT_EJB_NAME_REMOTE_MANAGER= "org.ow2.jonas.jpaas.environment.ManagerBean_org.ow2.jonas.jpaas.environment.ManagerRemote@Remote";
  private LoginContext loginContext = null;

  @Test
  public void testCreateOneEnvironment() throws Exception {
    Context initialContext = null;

    try {
      initialContext = getInitialContext();
    } catch (NamingException e) {
       Assert.fail("Cannot get InitialContext: " + e);
    }
    ManagerRemote statefulBean = null;
    try {
      statefulBean = (ManagerRemote) initialContext.lookup(DEFAULT_EJB_NAME_REMOTE_MANAGER);
    } catch (NamingException e) {
      Assert.fail("Cannot get statefulBean: " + e);
    }
    Environment idProcess = statefulBean.createEnvironment("test").get();
    Assert.assertNotNull(idProcess);
  }

  @Test
  public void testCreateTwoEnvironments() throws Exception {
    Context initialContext = null;

    try {
      initialContext = getInitialContext();
    } catch (NamingException e) {
       Assert.fail("Cannot get InitialContext: " + e);
    }
    ManagerRemote statefulBean = null;
    try {
      statefulBean = (ManagerRemote) initialContext.lookup(DEFAULT_EJB_NAME_REMOTE_MANAGER);
    } catch (NamingException e) {
      Assert.fail("Cannot get statefulBean: " + e);
    }
    Environment idProcess1 = statefulBean.createEnvironment("test").get();
    Assert.assertNotNull(idProcess1.getEnvId());

    Environment idProcess2 = statefulBean.createEnvironment("test").get();
    Assert.assertNotNull(idProcess2.getEnvId());

    Assert.assertFalse(idProcess1.getEnvId().equals(idProcess2.getEnvId()));
  }

  /**
   * @return Returns the InitialContext.
   * @throws NamingException If the Context cannot be created.
   */
  private static Context getInitialContext() throws NamingException {

    Hashtable<String, Object> env = new Hashtable<String, Object>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, DEFAULT_INITIAL_CONTEXT_FACTORY);
    env.put(Context.PROVIDER_URL, DEFAULT_PROVIDER_URL);

    return new InitialContext(env);
  }

}
