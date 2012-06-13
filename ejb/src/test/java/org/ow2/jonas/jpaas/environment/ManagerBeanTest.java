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
import org.ow2.jonas.jpaas.manager.api.Environment;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.login.LoginContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Hashtable;


public class ManagerBeanTest {

  /**
   * Default InitialContextFactory to use.
   */
  private static final String DEFAULT_INITIAL_CONTEXT_FACTORY = "org.objectweb.carol.jndi.spi.MultiOrbInitialContextFactory";
  private static final String  DEFAULT_PROVIDER_URL = "rmi://localhost:7099";
  private static final String DEFAULT_EJB_NAME_REMOTE_ENVIRONMENT_MANAGER= "EnvironmentManagerBean";
  private LoginContext loginContext = null;

  @Test
  public void testCreateEnvironmentsAtTheSameTime() throws Exception {
    Context initialContext = null;

    try {
      initialContext = getInitialContext();
    } catch (NamingException e) {
       Assert.fail("Cannot get InitialContext: " + e);
    }
    EnvironmentManagerRemote statefulBean = null;
    try {
      statefulBean = (EnvironmentManagerRemote) initialContext.lookup(DEFAULT_EJB_NAME_REMOTE_ENVIRONMENT_MANAGER);
    } catch (NamingException e) {
      Assert.fail("Cannot get Bean: " + e);
    }

    Thread thread1 = new BasicThread1(statefulBean);
    thread1.start();

    Thread thread2 = new BasicThread1(statefulBean);
    thread2.start();

    Thread thread3 = new BasicThread1(statefulBean);
    thread3.start();

    Thread thread4 = new BasicThread1(statefulBean);
    thread4.start();
  }

  class BasicThread1 extends Thread {

    EnvironmentManagerRemote envBean;
    BasicThread1(EnvironmentManagerRemote bean) {
      this.envBean = bean;
    }

    public void run() {
      try {
        Environment idProcess = envBean.createEnvironment("");//.get();
        Assert.assertNotNull(idProcess);
//      } catch (InterruptedException e) {
//        Assert.fail(e.getMessage());
//      } catch (ExecutionException e) {
//        Assert.fail(e.getMessage());
      } catch (EnvironmentManagerBeanException e) {
        Assert.fail(e.getMessage());
      }
    }
}

  @Test
  public void testCreateTwoEnvironments() throws Exception {
    Context initialContext = null;

    try {
      initialContext = getInitialContext();
    } catch (NamingException e) {
       Assert.fail("Cannot get InitialContext: " + e);
    }
    EnvironmentManagerRemote envBean = null;
    try {
      envBean = (EnvironmentManagerRemote) initialContext.lookup(DEFAULT_EJB_NAME_REMOTE_ENVIRONMENT_MANAGER);
    } catch (NamingException e) {
      Assert.fail("Cannot get Bean: " + e);
    }
    Environment idProcess1 = envBean.createEnvironment("");//.get();
    Assert.assertNotNull(idProcess1.getEnvId());

    Environment idProcess2 = envBean.createEnvironment("");//.get();
    Assert.assertNotNull(idProcess2.getEnvId());

    Assert.assertFalse(idProcess1.getEnvId().equals(idProcess2.getEnvId()));
  }

  @Test
  public void testCreateEnvironmentWithTemplate() throws Exception {

    String PATH_EXAMPLE_1 = "xmlExamples/environment-template-v6.xml";
    URL urlEnvironmentTemplate = this.getClass().getClassLoader().getResource(PATH_EXAMPLE_1);

    Context initialContext = null;

    try {
      initialContext = getInitialContext();
    } catch (NamingException e) {
       Assert.fail("Cannot get InitialContext: " + e);
    }
    EnvironmentManagerRemote envBean = null;
    try {
      envBean = (EnvironmentManagerRemote) initialContext.lookup(DEFAULT_EJB_NAME_REMOTE_ENVIRONMENT_MANAGER);
    } catch (NamingException e) {
      Assert.fail("Cannot get Bean: " + e);
    }

    if (urlEnvironmentTemplate != null) {
      Environment idProcess1 = envBean.createEnvironment(convertUrlToString(urlEnvironmentTemplate));//.get();
      Assert.assertNotNull(idProcess1.getEnvId());
    } else {
      Assert.fail("template environment can't find in ressource");
    }
  }

  @Test
   public void testCreateEnvironmentWithTemplateWithNoRouter() throws Exception {

     String PATH_EXAMPLE_1 = "xmlExamples/environment-template-v6-withnorouter.xml";
     URL urlEnvironmentTemplate = this.getClass().getClassLoader().getResource(PATH_EXAMPLE_1);

     Context initialContext = null;

     try {
       initialContext = getInitialContext();
     } catch (NamingException e) {
        Assert.fail("Cannot get InitialContext: " + e);
     }
     EnvironmentManagerRemote envBean = null;
     try {
       envBean = (EnvironmentManagerRemote) initialContext.lookup(DEFAULT_EJB_NAME_REMOTE_ENVIRONMENT_MANAGER);
     } catch (NamingException e) {
       Assert.fail("Cannot get Bean: " + e);
     }

     if (urlEnvironmentTemplate != null) {
       Environment idProcess1 = envBean.createEnvironment(convertUrlToString(urlEnvironmentTemplate));//.get();
       Assert.assertNotNull(idProcess1.getEnvId());
     } else {
       Assert.fail("template environment can't find in ressource");
     }
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

  private String convertUrlToString(URL file) throws IOException {

     InputStream processBarStream = file.openStream();
     InputStreamReader is = new InputStreamReader(processBarStream);
     BufferedReader br = new BufferedReader(is);
     String read = br.readLine();
     StringBuffer sb = new StringBuffer(read);
      while(read != null) {
          read = br.readLine();
          if (read != null)
             sb.append(read);
      }

      return sb.toString();
  }

}
