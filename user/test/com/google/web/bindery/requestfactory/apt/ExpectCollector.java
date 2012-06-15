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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementScanner6;
import javax.tools.Diagnostic.Kind;

/**
 * This is a trivial scanner that collects {@code @Expect} declarations on a
 * type. It does not perform any type-chasing or supertype traversal. The named
 * method on the {@link Messages} class is invoked with the provided arguments
 * and the resulting message is emitted to the {@link Messager}.
 */
@SupportedAnnotationTypes({
    "com.google.web.bindery.requestfactory.apt.Expect",
    "com.google.web.bindery.requestfactory.apt.Expected"})
class ExpectCollector extends AbstractProcessor {
  
  @Override
  public SourceVersion getSupportedSourceVersion() {
      return SourceVersion.latestSupported();
  }

  class Scanner extends ElementScanner6<Void, Void> {

    private final Messager messager;

    
    
    public Scanner(Messager messager) {
      this.messager = messager;
    }

    @Override
    public Void scan(Element e, Void p) {
      Expect expect = e.getAnnotation(Expect.class);
      if (expect != null) {
        addExpect(expect, e);
      }
      Expected expected = e.getAnnotation(Expected.class);
      if (expected != null) {
        for (Expect v : expected.value()) {
          addExpect(v, e);
        }
      }
      return super.scan(e, p);
    }

    private void addExpect(Expect expect, Element accumulator) {
      Method toInvoke = null;
      for (Method m : Messages.class.getDeclaredMethods()) {
        if (m.getName().equals(expect.method())) {
          toInvoke = m;
          break;
        }
      }
      if (toInvoke == null) {
        throw new RuntimeException("No method named " + expect.method());
      }
      String[] originalArgs = expect.args();
      Object[] args = new Object[originalArgs.length];
      for (int i = 0, j = args.length; i < j; i++) {
        // Special case for domainMethodWrongModifier
        if (boolean.class.equals(toInvoke.getParameterTypes()[i])) {
          args[i] = Boolean.valueOf(originalArgs[i]);
        } else {
          args[i] = originalArgs[i];
        }
      }
      String message;
      Throwable ex;
      try {
        message = (String) toInvoke.invoke(null, args);
        if (expect.warning()) {
          message += Messages.warnSuffix();
        }
        messager.printMessage(expect.warning() ? Kind.WARNING : Kind.ERROR, message, accumulator);
        return;
      } catch (IllegalArgumentException e) {
        ex = e;
      } catch (IllegalAccessException e) {
        ex = e;
      } catch (InvocationTargetException e) {
        ex = e.getCause();
      }
      throw new RuntimeException("Could not get test message", ex);
    }
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    Scanner scanner = new Scanner(processingEnv.getMessager());
    scanner.scan(roundEnv.getElementsAnnotatedWith(Expect.class), null);
    scanner.scan(roundEnv.getElementsAnnotatedWith(Expected.class), null);
    return false;
  }
}