/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.dev.javac.testing;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.javac.CompilationState;
import com.google.gwt.dev.javac.CompilationStateBuilder;
import com.google.gwt.dev.javac.StandardGeneratorContext;
import com.google.gwt.dev.javac.testing.impl.JavaResourceBase;
import com.google.gwt.dev.javac.testing.impl.MockResource;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.collect.HashSet;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Set;

/**
 * Builder creating a new generator context for testing purposes, based on the
 * sources provided.
 */
public class GeneratorContextBuilder {

  /**
   * Creates a new {@link GeneratorContextBuilder} that is pre-populated with a
   * number of basic types provided by
   * {@link JavaResourceBase#getStandardResources()}.
   *
   * @return pre-populated context builder
   */
  public static GeneratorContextBuilder newCoreBasedBuilder() {
    Set<Resource> resources = new HashSet<Resource>();
    resources.addAll(Arrays.asList(JavaResourceBase.getStandardResources()));
    return new GeneratorContextBuilder(resources);
  }

  /**
   * Creates a new empty {@link GeneratorContextBuilder}. Note that this
   * builder does <b>not</b> contain any base java types.
   *
   * @return empty context builder
   */
  public static GeneratorContextBuilder newEmptyBuilder() {
    return new GeneratorContextBuilder(new HashSet<Resource>());
  }

  private final Set<Resource> resources;
  private TreeLogger treeLogger;

  private GeneratorContextBuilder(Set<Resource> resources) {
    this.resources = resources;
  }

  /**
   * Adds the provided source to this builder, it will be included in any
   * subsequently built generator context.
   *
   * @param source source to be added
   */
  public GeneratorContextBuilder add(Source source) {
    resources.add(new StubResource(source));
    return this;
  }

  /**
   * Returns a newly created {@link GeneratorContext} based on the sources
   * added to this builder.
   */
  public GeneratorContext buildGeneratorContext() {
    // TODO: Add ability to add property values later and add them to this
    // context.
    return new StandardGeneratorContext(buildCompilationState(), null, null, null, false);
  }

  /**
   * Sets a custom logger on for this generator context.
   * <p>
   * If no custom logger is set, errors will be printed to {@code System.err}.
   */
  public void setTreeLogger(TreeLogger treeLogger) {
    this.treeLogger = treeLogger;
  }

  private CompilationState buildCompilationState() {
    TreeLogger logger = treeLogger != null ? treeLogger : createLogger();
    return new CompilationStateBuilder().doBuildFrom(logger, resources, null, false);
  }

  private TreeLogger createLogger() {
    PrintWriterTreeLogger logger = new PrintWriterTreeLogger(new PrintWriter(
        System.err, true));
    logger.setMaxDetail(TreeLogger.ERROR);
    return logger;
  }

  private static class StubResource extends MockResource {

    private final Source source;

    public StubResource(Source source) {
      super(source.getPath());
      this.source = source;
    }

    @Override
    public CharSequence getContent() {
      return source.getSource();
    }
  }
}
