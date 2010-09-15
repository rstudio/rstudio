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
package com.google.gwt.editor.client;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Describes an editor whose behavior is not altered by the value being
 * displayed.
 * <p>
 * Editors, which may be classes or interfaces, may expose their sub-editors in
 * one or more of the following ways:
 * <ul>
 * <li>An instance field with at least package visibility whose name exactly is
 * the property that will be edited or <code><em>propertyName</em>Editor</code>.
 * </li>
 * <li>A no-arg method with at least package visibility whose name exactly is
 * the property that will be edited or <code><em>propertyName</em>Editor</code>.
 * </li>
 * <li>The {@link Path} annotation may be used on the field or accessor method
 * to specify a dotted path or to bypass the implicit naming convention.</li>
 * <li>Sub-Editors may be null. In this case, the Editor framework will ignore
 * these sub-editors.</li>
 * </ul>
 * Any exposed field or method whose type is Editor may also use the
 * {@link IsEditor} interface to provide an Editor instance. This allows view
 * objects to be written that can be attached to an Editor hierarchy without the
 * view directly implementing an Editor interface.
 * 
 * @param <T> the type of object the editor displays.
 */
public interface Editor<T> {
  /**
   * Tells the Editor framework to ignore an Editor accessor.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target(value = {ElementType.FIELD, ElementType.METHOD})
  public @interface Ignore {
  }

  /**
   * Maps a composite Editor's component Editors into the data-model.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target(value = {ElementType.FIELD, ElementType.METHOD})
  public @interface Path {
    String value();
  }
}