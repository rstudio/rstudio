//Modified by Google.
package org.jboss.testharness;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import java.util.logging.Logger;
import org.jboss.testharness.api.Configuration;
import org.jboss.testharness.api.DeploymentException;
//import org.jboss.testharness.api.TestResult;
//import org.jboss.testharness.api.TestResult.Status;
//import org.jboss.testharness.impl.ConfigurationImpl;
//import org.jboss.testharness.impl.packaging.ArtifactGenerator;
import org.jboss.testharness.impl.packaging.TCKArtifact;
import org.testng.IHookCallBack;
import org.testng.IHookable;
//import org.testng.ITestContext;
import org.testng.ITestResult;
//import org.testng.annotations.AfterClass;
//import org.testng.annotations.AfterSuite;
//import org.testng.annotations.BeforeClass;
//import org.testng.annotations.BeforeSuite;

/**
 * Abstract Test
 * <p>
 * Modified by Google to only include the minimum needed to run the JSR-303 tck.
 */
public abstract class AbstractTest implements IHookable
{

   private static Logger log = Logger.getLogger(AbstractTest.class.getName());

   private static boolean inContainer = false;

   public static boolean isInContainer()
   {
      return inContainer;
   }

   public static void setInContainer(boolean inContainer)
   {
      AbstractTest.inContainer = inContainer;
   }

   private TCKArtifact artifact;
   private DeploymentException deploymentException;
   private boolean skipTest = false;

   private boolean isSuiteDeployingTestsToContainer()
   {
      return !isInContainer() && (!getCurrentConfiguration().isStandalone() || getCurrentConfiguration().isRunIntegrationTests());
   }

   private void generateArtifact()
   {

   }

   protected TCKArtifact postCreate(TCKArtifact artifact)
   {
      return artifact;
   }

   private boolean isDeployToContainerNeeded()
   {
      /*
       * If this isn't running inside the container AND there is an artifact to
       * deploy AND EITHER we are in standalone mode and it isn't a unit test OR
       * we aren't in standalone mode THEN we need to deploy
       */
      return !isInContainer() && artifact != null && ((getCurrentConfiguration().isStandalone() && !artifact.isUnit() && getCurrentConfiguration().isRunIntegrationTests()) || !getCurrentConfiguration().isStandalone());
   }

   private void deployArtifact()
   {

   }

   protected DeploymentException handleDeploymentFailure(DeploymentException deploymentException)
   {
      return deploymentException;
   }


   private void undeployArtifact() throws Exception
   {
      if (isDeployToContainerNeeded())
      {
         getCurrentConfiguration().getContainers().undeploy(artifact.getDefaultName());
      }
      if (getCurrentConfiguration().isStandalone() && artifact != null && artifact.isUnit())
      {
         getCurrentConfiguration().getStandaloneContainers().undeploy();
      }
   }

   private void checkAssertionsEnabled()
   {
      boolean assertionsEnabled = false;
      try
      {
         assert false;
      }
      catch (AssertionError error)
      {
         assertionsEnabled = true;
      }
      if (!assertionsEnabled)
      {
         throw new IllegalStateException("Assertions must be enabled!");
      }
   }

   //@BeforeSuite(alwaysRun = true, groups = "scaffold")
//   public void beforeSuite(ITestContext context) throws Exception
//   {
//      if (isSuiteDeployingTestsToContainer())
//      {
//         getCurrentConfiguration().getContainers().setup();
//      }
//      if (getCurrentConfiguration().isStandalone())
//      {
//         getCurrentConfiguration().getStandaloneContainers().setup();
//      }
//      checkAssertionsEnabled();
//   }

   //@AfterSuite(alwaysRun = true, groups = "scaffold")
   public void afterSuite() throws Exception
   {
      if (isSuiteDeployingTestsToContainer())
      {
         getCurrentConfiguration().getContainers().cleanup();
      }
      if (getCurrentConfiguration().isStandalone())
      {
         getCurrentConfiguration().getStandaloneContainers().cleanup();
      }
   }

   //@BeforeClass(alwaysRun = true, groups = "scaffold")
   public void beforeClass() throws Throwable
   {
      generateArtifact();
      deployArtifact();

   }

   //@AfterClass(alwaysRun = true, groups = "scaffold")
   public void afterClass() throws Exception
   {
      undeployArtifact();
      this.artifact = null;
      this.deploymentException = null;
      skipTest = false;
   }

   public void beforeMethod()
   {

   }

   public void afterMethod()
   {

   }

   public final void run(IHookCallBack callback, ITestResult testResult)
   {
   }

   protected Configuration getCurrentConfiguration()
   {
      return null; //ConfigurationImpl.get();
   }

   protected String getContextPath()
   {
      return "http://" + getCurrentConfiguration().getHost() + "/" + this.getClass().getName() + "/";
   }

   protected boolean isThrowablePresent(Class<? extends Throwable> throwableType, Throwable throwable)
   {
      if (throwable == null)
      {
         return false;
      }
//      else if (throwableType.isAssignableFrom(throwable.getClass()))
//      {
//         return true;
//      }
//      else
//      {
//         return isThrowablePresent(throwableType, throwable.getCause());
//      }
      return true;
   }
}