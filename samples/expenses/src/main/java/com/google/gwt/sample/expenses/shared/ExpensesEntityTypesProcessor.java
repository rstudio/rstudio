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
package com.google.gwt.sample.expenses.shared;

import com.google.web.bindery.requestfactory.shared.EntityProxy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A helper class for dealing with proxy types. Subclass it and override the
 * various handle methods for type specific handling of proxy objects or
 * classes, then call {@link #process(Class)} or {@link #process(Object)}.
 * Optionally use {#setResult} to set the return value of the {@link #process}
 * call.
 * <P>
 * Use {@link #getAll} for a set of all proxy types.
 * 
 * @param <T> the type to filter to
 */
public abstract class ExpensesEntityTypesProcessor<T> {

  /**
   * Return a set of all proxy types available to this application.
   */
  public static Set<Class<? extends EntityProxy>> getAll() {
    Set<Class<? extends EntityProxy>> rtn = new HashSet<Class<? extends EntityProxy>>();

    rtn.add(EmployeeProxy.class);
    rtn.add(ReportProxy.class);

    return Collections.unmodifiableSet(rtn);
  }

  private static void process(ExpensesEntityTypesProcessor<?> processor,
      Class<?> clazz) {
    if (EmployeeProxy.class.equals(clazz)) {
      processor.handleEmployee((EmployeeProxy) null);
      return;
    }
    if (ReportProxy.class.equals(clazz)) {
      processor.handleReport((ReportProxy) null);
      return;
    }
    processor.handleNonProxy(null);
  }

  private static void process(ExpensesEntityTypesProcessor<?> processor,
      Object proxy) {
    if (proxy instanceof EmployeeProxy) {
      processor.handleEmployee((EmployeeProxy) proxy);
      return;
    }
    if (proxy instanceof ReportProxy) {
      processor.handleReport((ReportProxy) proxy);
      return;
    }
    processor.handleNonProxy(proxy);
  }

  private final T defaultValue;

  private T result;

  /**
   * Create an instance with a null default value.
   */
  public ExpensesEntityTypesProcessor() {
    defaultValue = null;
  }

  /**
   * Create an instance with the given default value.
   * 
   * @param defaultValue the value that will be returned by {@link #process} if
   *          {@link #setResult} is not called.
   */
  public ExpensesEntityTypesProcessor(T defaultValue) {
    this.defaultValue = defaultValue;
  }

  public abstract void handleEmployee(EmployeeProxy proxy);

  /**
   * Called if {@link #process} is called with a non-proxy object. This default
   * implementation does nothing.
   */
  public void handleNonProxy(@SuppressWarnings("unused") Object object) {
  }

  public abstract void handleReport(ReportProxy proxy);

  /**
   * Call the handle method of the appropriate type, with a null argument. Note
   * that this will not work as expected on the class objects returned by the
   * {@link #getClass()} method of a proxy object, due to limitations of GWT's
   * metadata. It will only work with against class objects in the set returned
   * by {@link #getAll()}, or returned by
   * {@link com.google.web.bindery.requestfactory.shared.RequestFactory#getClass(EntityProxy)}
   * or
   * {@link com.google.web.bindery.requestfactory.shared.RequestFactory#getClass(String)}
   * .
   * 
   * @param clazz the proxy type to resolve
   * @return the value provided via {@link #setResult}, or the default value
   */
  public T process(Class<?> clazz) {
    setResult(defaultValue);
    ExpensesEntityTypesProcessor.process(this, clazz);
    return result;
  }

  /**
   * Process a proxy object.
   * 
   * @param proxy the proxy to process
   * @return the value provided via {@link #setResult}, or the default value
   */
  public T process(Object proxy) {
    setResult(defaultValue);
    ExpensesEntityTypesProcessor.process(this, proxy);
    return result;
  }

  /**
   * Set the value to return from a call to {@link #process(Class)} or
   * {@link #process(Object)}.
   * 
   * @param result the value to return
   */
  protected void setResult(T result) {
    this.result = result;
  }
}
