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
import com.google.gwt.resources.rg.BundleResourceGenerator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The use of this interface is similar to that of ImageBundle. Declare
 * no-argument functions that return subclasses of {@link ResourcePrototype},
 * which are annotated with {@link ClientBundle.Source} annotations specifying
 * the classpath location of the resource to include in the output. At runtime,
 * the functions will return an object that can be used to access the data in
 * the original resource.
 */
@ResourceGeneratorType(BundleResourceGenerator.class)
public interface ClientBundle {
  /**
   * Specifies the classpath location of the resource or resources associated
   * with the {@link ResourcePrototype}.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  public @interface Source {
    String[] value();
  }
}
