package org.jboss.testharness.api;

import java.util.List;

import org.jboss.testharness.spi.Containers;
import org.jboss.testharness.spi.StandaloneContainers;

/**
 * The configuration of the TCK.
 *
 * The TCK may be configured using system properties or placed in a properties
 * file called META-INF/web-beans-tck.properties.
 *
 * Porting package property names are the FQCN of the SPI class. Other property
 * names (one for each non-porting package SPI configuration option) are
 * specified here. The defaults are also listed here.
 *
 * The TCK may also be configured programatically through this interface
 * 
 * <p>
 * Modified by Google.
 * <ul>
 * <li>Removed references to TestLauncher</li>
 * <li>Removed revernces to the output dir.</li>
 * <ul>
 *
 * @author Pete Muir
 *
 */
public interface Configuration
{

   public static final String OUTPUT_DIRECTORY_PROPERTY_NAME = "org.jboss.testharness.outputDirectory";
   public static final String STANDALONE_PROPERTY_NAME = "org.jboss.testharness.standalone";
   public static final String RUN_INTEGRATION_TESTS_PROPERTY_NAME = "org.jboss.testharness.runIntegrationTests";
   public static final String CONNECT_TIMEOUT_PROPERTY_NAME = "org.jboss.testharness.connectTimeout";
   public static final String LIBRARY_DIRECTORY_PROPERTY_NAME = "org.jboss.testharness.libraryDirectory";
   public static final String HOST_PROPERTY_NAME = "org.jboss.testharness.host";
   public static final String TEST_PACKAGE_PROPERTY_NAME = "org.jboss.testharness.testPackage";

   public static final boolean DEFAULT_STANDALONE = true;
   public static final boolean DEFAULT_RUN_INTEGRATION_TESTS = false;
   public static final int DEFAULT_CONNECT_DELAY = 5000;
   public static final boolean DEFAULT_WRITE_DEPLOYED_ARCHIVES_TO_DISK = false;
   public static final String DEFAULT_LIBRARY_DIRECTORY = null;
   public static final String DEFAULT_HOST = "localhost:8080";

   /**
    * The output directory to put TCK specific output. The TestNG output
    * directory is configured via TestNG.
    *
    * By default set to ${tmp}/jsr-299-tck
    */
   public String getOutputDirectory();

   /**
    * Whether the TCK is in standalone mode or not.
    *
    * By default true
    */
   public boolean isStandalone();

   /**
    * When the TCK is running in container tests it will attempt to connect to
    * the server every 200ms until the timeout is reached.
    *
    * By default 5000ms
    */
   public int getConnectTimeout();

   /**
    * The TCK allows additional libraries to be put in the deployed test
    * artifacts (for example the porting package for the implementation). Any
    * jars in this directory will be added to the deployed artifact.
    *
    * By default no directory is used.
    */
   public String getLibraryDirectory();

   /**
    * The TCK test launcher
    *
    * @see TestLauncher
    *
    */
   public Object getInContainerTestLauncher();

   /**
    * The implementation of {@link Containers} in use.
    */
   public Containers getContainers();

   /**
    * Whether to run integration tests, by default false.
    */
   public boolean isRunIntegrationTests();

   public void setOutputDirectory(String outputDirectory);

   public void setStandalone(boolean standalone);

   public void setConnectTimeout(int connectTimeout);

   public void setLibraryDirectory(String libraryDir);

   public void setInContainerTestLauncher(Object testLauncher);

   public void setContainers(Containers containers);

   /**
    * The implementation of {@link StandaloneContainers} in use.
    */
   public StandaloneContainers getStandaloneContainers();

   public void setStandaloneContainers(StandaloneContainers standaloneContainers);

   public void setRunIntegrationTests(boolean runIntegrationTests);

   /**
    * The TCK will use this as the remote host to connect to run in container
    * tests. By default localhost:8080
    *
    */
   public String getHost();

   public void setHost(String host);

   public List<String> getExtraPackages();

   public List<ResourceDescriptor> getExtraResources();

   public List<String> getExtraDeploymentProperties();

   public String getTestPackage();

   public void setTestPackage(String packageName);

}