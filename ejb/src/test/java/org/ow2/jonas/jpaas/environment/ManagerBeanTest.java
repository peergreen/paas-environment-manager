package org.ow2.jonas.jpaas.environment;

import junit.framework.Assert;
import org.junit.Test;

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
    String idProcess = statefulBean.createEnvironment("test");
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
    String idProcess1 = statefulBean.createEnvironment("test");
    Assert.assertNotNull(idProcess1);

    String idProcess2 = statefulBean.createEnvironment("test");
    Assert.assertNotNull(idProcess2);

    Assert.assertFalse(idProcess1.equals(idProcess2));
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
