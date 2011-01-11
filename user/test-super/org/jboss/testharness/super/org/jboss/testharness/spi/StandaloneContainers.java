//Modified by Google
package org.jboss.testharness.spi;

import java.util.Collection;

import org.jboss.testharness.api.DeploymentException;

/**
 * Standalone mode deployment related operations. If the TCK is placed in
 * standalone mode, unit tests will be deployed via this interface.
 *
 * You must implement this as part of the porting package if you intend to run
 * the TCK in standalone mode
 *
 * @author Pete Muir
 *
 */
public interface StandaloneContainers
{

   public static final String PROPERTY_NAME = StandaloneContainers.class.getName();

   /**
    * <p>Bootstrap the container by registering Beans and Observers, raising
    * @Initialized event, validating the deployment, and raising the
    * @Deployed event.</p>
    *
    * <p>Any classes passed in should be fully deployed. This includes:</p>
    *
    * <ul>
    * <li>Simple beans</li>
    * <li>Session beans</li>
    * <li>Producer methods and producer fields</li>
    * <li>Observer methods</li>
    * <li>support for Event and Instance injection points</li>
    * </ul>
    *
    * The container should be in an fully initialized state when the
    * method returns
    *
    * @param classes the classes to deploy
    */
   public void deploy(Collection<Class<?>> classes) throws DeploymentException;

   /**
    * <p>Bootstrap the container for a test by registering Beans and Observers,
    * raising @Initialized event, validating the deployment, and raising the
    * @Deployed event.</p>
    *
    * <p>Any classes passed in should be fully deployed. This includes:</p>
    *
    * <ul>
    * <li>Simple beans</li>
    * <li>Session beans</li>
    * <li>Producer methods and producer fields</li>
    * <li>Observer methods</li>
    * <li>support for Event and Instance injection points</li>
    * </ul>
    *
    * The container should be in an fully initialized state when the
    * method returns
    *
    * @param classes the classes to deploy
    * @param beansXmls the beans.xml files to deploy
    *
    * @return true if the deployment suceeded, otherwise false
    */
   public boolean deploy(Collection<Class<?>> classes, Collection<Object> beansXmls);

   /**
    * Any deployment exception, or null if the deployment suceeded
    *
    * @return
    */
   public DeploymentException getDeploymentException();

   /**
    * Cleanup the container after this test
    *
    */
   public void undeploy();

   /**
    * Called before the TCK starts executing the testsuite, but after the suite
    * has been configured.
    *
    * A TCK suite lifecycle callback, useful for setting up the container. This
    * method may be a no-op if no setup is required.
    *
    */
   public void setup();

   /**
    * Called after the TCK finishes executing the testsuite.
    *
    * A TCK suite lifecycle callback, useful for cleaning up and shutting down
    * the container. This method may be a no-op if no setup is required.
    *
    */
   public void cleanup();

}
