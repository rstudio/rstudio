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

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

/**
 * The entry point for annotation validation.
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedOptions({"suppressErrors", "suppressWarnings", "verbose"})
public class RfValidator extends AbstractProcessor {

  private boolean forceErrors;
  private State state;

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (state == null) {
      state = forceErrors ? new State.ForTesting(processingEnv) : new State(processingEnv);
    }

    try {
      // Bootstrap the State's work queue
      new Finder().scan(ElementFilter.typesIn(roundEnv.getRootElements()), state);
      // Execute the work items
      state.executeJobs();
      if (roundEnv.processingOver()) {
        // Verify mappings
        new DomainChecker().scan(state.getClientToDomainMap().keySet(), state);
        state = null;
      }
    } catch (HaltException ignored) {
      // Already logged. Let any unhandled RuntimeExceptions fall out.
    }
    return false;
  }

  void setForceErrors(boolean forceErrors) {
    this.forceErrors = forceErrors;
  }
}
