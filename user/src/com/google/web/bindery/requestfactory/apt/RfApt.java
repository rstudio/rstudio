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

import static com.google.web.bindery.requestfactory.vm.impl.TypeTokenResolver.TOKEN_MANIFEST;

import com.google.web.bindery.requestfactory.shared.BaseProxy;
import com.google.web.bindery.requestfactory.vm.impl.OperationKey;
import com.google.web.bindery.requestfactory.vm.impl.TypeTokenResolver;

import java.io.IOException;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.ElementScanner6;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

/**
 * An annotation processor that creates an obfuscated type manifest that is used
 * by {@link com.google.web.bindery.requestfactory.vm.RequestFactorySource} and
 * related implementation classes.
 */
@SupportedAnnotationTypes("com.google.web.bindery.requestfactory.*")
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedOptions("verbose")
public class RfApt extends AbstractProcessor {

  /**
   * Looks for all types assignable to {@link BaseProxy} and adds them to the
   * output state.
   */
  private class Finder extends ElementScanner6<Void, Void> {
    // Only valid for a single round
    TypeMirror baseProxyType = elements.getTypeElement(BaseProxy.class.getCanonicalName()).asType();

    @Override
    public Void visitType(TypeElement elt, Void arg1) {
      String binaryName = elements.getBinaryName(elt).toString();
      if (types.isSubtype(elt.asType(), baseProxyType)) {
        String hash = OperationKey.hash(binaryName);
        builder.addTypeToken(hash, binaryName);
        log(elt, "Processed proxy %s %s", binaryName, hash);
      }
      return super.visitType(elt, arg1);
    }
  }

  private TypeTokenResolver.Builder builder;
  private Elements elements;
  private Filer filer;
  private boolean verbose;
  private Types types;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    log("RfApt init");
    elements = processingEnv.getElementUtils();
    filer = processingEnv.getFiler();
    types = processingEnv.getTypeUtils();
    verbose = Boolean.parseBoolean(processingEnv.getOptions().get("verbose"));
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    log("RequestFactory processing a round");

    if (builder == null) {
      builder = new TypeTokenResolver.Builder();
      // Try not to obliterate existing data
      try {
        FileObject resource = filer.getResource(StandardLocation.CLASS_OUTPUT, "", TOKEN_MANIFEST);
        builder.load(resource.openInputStream());
        log("Reusing old data");
      } catch (IOException e) {
        // Likely because the file does not exist
        log("Not reusing existing manifest file: " + e.getMessage());
      }
    }

    // Extract data
    new Finder().scan(ElementFilter.typesIn(roundEnv.getRootElements()), null);
    
    // On the last round, write out accumulated data
    if (roundEnv.processingOver()) {
      TypeTokenResolver d = builder.build();
      builder = null;
      try {
        FileObject res = filer.createResource(StandardLocation.CLASS_OUTPUT, "", TOKEN_MANIFEST);
        d.store(res.openOutputStream());
      } catch (IOException e) {
        error("Could not write output: " + e.getMessage());
      }
      log("Finished!");
    }
    return false;
  }

  private void error(String message, Object... args) {
    processingEnv.getMessager().printMessage(Kind.ERROR, "ERROR: " + String.format(message, args));
  }

  private void log(Element elt, String message, Object... args) {
    if (verbose) {
      processingEnv.getMessager().printMessage(Kind.NOTE, String.format(message, args), elt);
    }
  }

  private void log(String message, Object... args) {
    if (verbose) {
      processingEnv.getMessager().printMessage(Kind.NOTE, String.format(message, args));
    }
  }
}
