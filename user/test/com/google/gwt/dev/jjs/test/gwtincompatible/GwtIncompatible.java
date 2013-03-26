/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.dev.jjs.test.gwtincompatible;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * GwtIncompatible annotation for use in a test. @see com.google.gwt.core.shared.GwtIncompatible.
 *
 * It only matters that the class name is GwtIncompatible.
 */
@Retention(RetentionPolicy.CLASS)
@Target({
    ElementType.TYPE, ElementType.METHOD,
    ElementType.CONSTRUCTOR, ElementType.FIELD })
@Documented
public @interface GwtIncompatible {
  /**
   * An attribute that can be used to explain why the code is incompatible.
   * All attributes on a GwtIncompatible annotation are ignored by the GWT compiler.
   */
  String value();
}
