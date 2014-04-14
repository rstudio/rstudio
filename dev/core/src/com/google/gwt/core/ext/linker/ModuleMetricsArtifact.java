/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.core.ext.linker;

import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.linker.SoycReportLinker;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Captures some metrics from the module load and initial type oracle compile
 * step.
 */
@Transferable
public class ModuleMetricsArtifact extends Artifact<ModuleMetricsArtifact> {
  /*
   * In an ideal world, there should be only one of these module instances, but
   * since this information is recomputed for each precompile task, there will
   * be multiple entries which are all about the same.
   */
  private static AtomicInteger nextInstance = new AtomicInteger(0);

  private final int instanceId;
  private long elapsedMilliseconds;
  private String[] sourceFiles;
  private String[] initialTypes;

  public ModuleMetricsArtifact() {
    this(SoycReportLinker.class, nextInstance.getAndIncrement());
  }

  protected ModuleMetricsArtifact(Class<? extends Linker> linker, int instanceId) {
    super(linker);
    this.instanceId = instanceId;
  }

  /**
   * @return wall clock time elapsed since start of module load to end of the
   *         initial type oracle build.
   */
  public long getElapsedMilliseconds() {
    return elapsedMilliseconds;
  }

  /**
   * @return the number of types resulting from the type oracle build which
   *         compiles all of the source files initially presented to the
   *         compiler.
   */
  public String[] getInitialTypes() {
    return initialTypes;
  }

  /**
   * @return the source files initially presented to the compiler
   */
  public String[] getSourceFiles() {
    return sourceFiles;
  }

  @Override
  public int hashCode() {
    return getName().hashCode();
  }

  /**
   * @param elapsedMilliseconds wall clock time elapsed since start of module
   *        load to end of the initial type oracle build.
   */
  public ModuleMetricsArtifact setElapsedMilliseconds(long elapsedMilliseconds) {
    this.elapsedMilliseconds = elapsedMilliseconds;
    return this;
  }

  /**
   * @param initialTypes the number of types resulting from the initial type
   *        oracle build which compiles all of the source files initially
   *        presented to the compiler.
   */
  public ModuleMetricsArtifact setInitialTypes(Collection<String> initialTypes) {
    this.initialTypes = initialTypes.toArray(new String[initialTypes.size()]);
    return this;
  }

  /**
   * @param sourceFiles the list of source files presented to the compiler on
   *        the module source path.
   */
  public ModuleMetricsArtifact setSourceFiles(String[] sourceFiles) {
    this.sourceFiles = sourceFiles;
    return this;
  }

  @Override
  protected int compareToComparableArtifact(ModuleMetricsArtifact o) {
    return getName().compareTo(o.getName());
  }

  @Override
  protected final Class<ModuleMetricsArtifact> getComparableArtifactType() {
    return ModuleMetricsArtifact.class;
  }

  private String getName() {
    return "ModuleMetricsArtifact-" + instanceId;
  }
}
