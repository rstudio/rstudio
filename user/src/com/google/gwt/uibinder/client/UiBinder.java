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
package com.google.gwt.uibinder.client;

/**
 * Interface implemented by classes that generate DOM or Widget structures from
 * ui.xml template files, and which inject portions of the generated UI into the
 * fields of an owner.
 * <p>
 * The generated UiBinder implementation will be based on an xml file resource
 * in the same package as the owner class, with the same name and a "ui.xml"
 * suffix. For example, a UI owned by class {@code bar.baz.Foo} will be sought
 * in {@code /bar/baz/Foo.ui.xml}. (To use a different template file, put the
 * {@link UiTemplate} annotation on your UiBinder interface declaration to point
 * the code generator at it.)
 *
 * @param <U> The type of the root object of the generated UI, typically a
 *          subclass of {@link com.google.gwt.dom.client.Element Element} or
 *          {@link com.google.gwt.user.client.ui.UIObject UiObject}
 * @param <O> The type of the object that will own the generated UI
 */
public interface UiBinder<U, O> {
  /**
   * Creates and returns the root object of the UI, and fills any fields of owner
   * tagged with {@link UiField}.
   *
   * @param owner the object whose {@literal @}UiField needs will be filled
   */
  U createAndBindUi(O owner);
}
