package org.jboss.testharness.impl.packaging;

import java.util.Set;

import org.jboss.testharness.api.ResourceDescriptor;

/**
 * @author Hardy Ferentschik
 */
public interface TCKArtifact {
  TCKArtifact initialize();

  Object getJarAsStream();

  Object getJar();

  Object getExplodedJar();

  void create();

  void writeArtifactToDisk(String outputDirectory);

  void writeArtifactToDisk(String outputDirectory, String ObjectName);

  String getDefaultName();

  Set<Class<?>> getClasses();

  Set<ResourceDescriptor> getResources();

  void addPackage(Object pkg);

  void addPackage(String packageName, boolean addRecursively);

  Object getClassesRoot(Object archiveRoot);

  String getClassesRoot();

  void setClassesRoot(String classesRoot);

  Class<?> getDeclaringClass();

  String getExtension();

  void setExtension(String extension);

  Set<ResourceDescriptor> getLibraries();

  Object getLibraryRoot(Object archiveRoot);

  void setLibrariesRoot(String libraryRoot);

  boolean isLibrariesSupported();

  void setLibrariesSupported(boolean librariesSupported);

  boolean isUnit();

  void setUnit(boolean unit);

  Class<? extends Throwable> getExpectedDeploymentException();

  void setExpectedDeploymentException(Class<? extends Throwable> expectedDeploymentException);

  boolean isRunLocally();

  void setRunLocally(boolean runLocally);

  String getXmlConfigDestination();

  void setXmlConfigDestination(String xmlConfigDest);

  ResourceDescriptor getXmlConfig();

  void skipIncludeXmlConfig(boolean skip);
}

