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
package com.google.web.bindery.requestfactory.apt;

import com.google.gwt.dev.util.Name.BinaryName;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

/**
 * The entry point for annotation validation.
 */
@SupportedAnnotationTypes("*")
@SupportedOptions({"rootOverride", "suppressErrors", "suppressWarnings", "verbose"})
public class RfValidator extends AbstractProcessor {

  private boolean clientOnly;
  private boolean mustResolveAllMappings;
  private List<String> rootOverride;
  private boolean forceErrors;
  private State state;

  @Override
  public SourceVersion getSupportedSourceVersion() {
      return SourceVersion.latestSupported();
  }
  
  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    String option = processingEnv.getOptions().get("rootOverride");
    if (option != null) {
      setRootOverride(Arrays.asList(option.split(",")));
    }
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    // Are we finished, if so, clean up
    if (roundEnv.processingOver()) {
      state = null;
      return false;
    }

    // Newly initialized or being reused?
    if (state == null) {
      state = forceErrors ? new State.ForTesting(processingEnv) : new State(processingEnv);
      if (state.isPoisoned()) {
        // Could not initialize State object, bail out
        return false;
      }
      // Produce a "lite" map just for JRE-only clients
      state.setClientOnly(clientOnly);
      // Disallow @ProxyForName or @ServiceName that can't be resolved
      state.setMustResolveAllMappings(mustResolveAllMappings);
    }

    try {
      // Bootstrap the State's work queue
      new Finder().scan(getTypesToProcess(state, roundEnv), state);
      // Execute the work items
      state.executeJobs();
    } catch (HaltException ignored) {
      // Already logged. Let any unhandled RuntimeExceptions fall out.
    }
    return false;
  }

  public void setClientOnly(boolean clientOnly) {
    this.clientOnly = clientOnly;
  }

  void setForceErrors(boolean forceErrors) {
    this.forceErrors = forceErrors;
  }

  /**
   * Make it an error to not resolve all ProxyForName and ServiceName mappings.
   */
  void setMustResolveAllMappings(boolean requireAll) {
    this.mustResolveAllMappings = requireAll;
  }

  /**
   * Instead of scanning the round's root elements, scan these type names
   * instead. This is used by the ValidationTool to scan pre-compiled
   * classfiles.
   */
  void setRootOverride(List<String> binaryTypeNames) {
    this.rootOverride = binaryTypeNames;
  }

  private Set<TypeElement> getTypesToProcess(State state, RoundEnvironment roundEnv) {
    if (rootOverride == null) {
      return ElementFilter.typesIn(roundEnv.getRootElements());
    }
    Set<TypeElement> toScan = new HashSet<TypeElement>();
    for (String binaryTypeName : rootOverride) {
      TypeElement found =
          state.elements.getTypeElement(BinaryName.toSourceName(binaryTypeName.trim()));
      if (found == null) {
        state.poison(null, Messages.noSuchType(binaryTypeName));
      } else {
        toScan.add(found);
      }
    }
    rootOverride = null;
    return toScan;
  }
}
