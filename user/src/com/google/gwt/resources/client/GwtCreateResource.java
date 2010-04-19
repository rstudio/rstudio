/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.resources.client;

import com.google.gwt.resources.ext.ResourceGeneratorType;
import com.google.gwt.resources.rg.GwtCreateResourceGenerator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This resource type allows any class that can be instantiated via a call to
 * {@link com.google.gwt.core.client.GWT#create(Class)} to be used within an
 * ClientBundle. Example uses include the I18N support classes, RPC
 * endpoints, or any type that supports default instantiation. If no
 * {@link ClassType} annotation is present on the resource accessor method, the
 * type parameter <code>T</code> will be used as the class literal passed to
 * <code>GWT.create()</code>.
 * 
 * @param <T> The type that should be returned from the
 *          <code>GWT.create()</code> call
 */
@ResourceGeneratorType(GwtCreateResourceGenerator.class)
public interface GwtCreateResource<T> extends ResourcePrototype {
  /**
   * This annotation can be applied to the resource getter method in order to
   * call <code>GWT.create</code> with a class literal other than that of the
   * return type parameterization. This annotation would be used with RPC
   * endpoints:
   * 
   * <pre>{@literal @ClassType}(Service.class)
   * GwtCreateResource&lt;ServiceAsync&gt; rpc();
   * </pre>
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface ClassType {
    Class<?> value();
  }

  /**
   * Invokes <code>GWT.create()</code>. Multiple invocations of this method will
   * return different instances of the <code>T</code> type.
   */
  T create();
}
