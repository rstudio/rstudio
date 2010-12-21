//Modified by Google
package org.jboss.testharness.spi;

//import java.io.IOException;
//import java.io.InputStream;

import org.jboss.testharness.api.DeploymentException;

/**
 * Incontainer mode deployment related operations
 *
 * The TCK porting package must provide an implementation of this interface
 * which is suitable for the target implementation and application server
 * 
 * <p>
 * Modified by Google.
 * <ul>
 * <li>Removed refrences to java.io.</li>
 * <ul> 
 *
 * @author Pete Muir
 *
 */
public interface Containers
{

   public static final String PROPERTY_NAME = Containers.class.getName();

   /**
    * The war/ear to deploy to the container, it should be read using a
    * JarInputStream. Any deployment exceptions can be found through
    * {@link #getDeploymentException()}.
    *
    * For a successful deployment, a symmetric {@link #undeploy(String)} will be
    * called.
    *
    * @see #undeploy(String)
    *
    * @param archive the archive
    * @param name the name the TCK uses to refer to this archive, unique within
    *           this tck run
    * @return true if the deployment suceeded, otherwise false
    * @throws IOException if any communication problems with the server occur
    *            during deployment. These will cause the test suite to fail.
    */
   public boolean deploy(Object archive, String name);

   public DeploymentException getDeploymentException();

   /**
    * Undeploy the war/ear from the container.
    *
    * @see #deploy(InputStream, String)
    *
    * @param name the name the TCK uses to refer to this archive, unique within
    *           this tck run
    * @throws IOException if any communication problems with the server occur
    *            during deployment. These will cause the test suite to fail.
    */
   public void undeploy(String name);

   /**
    * Called before the TCK starts executing the testsuite, but after the suite
    * has been configured.
    *
    * A TCK suite lifecycle callback, useful for setting up the container. This
    * method may be a no-op if no setup is required.
    *
    * @throws IOException if any communication problems with the server occur
    *            during setup. These will cause the test suite to fail.
    */
   public void setup();

   /**
    * Called after the TCK finishes executing the testsuite.
    *
    * A TCK suite lifecycle callback, useful for cleaning up and shutting down
    * the container. This method may be a no-op if no setup is required.
    *
    * @throws IOException if any communication problems with the server occur
    *            during cleanup. These will cause the test suite to fail.
    */
   public void cleanup() ;

}