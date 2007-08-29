/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.dev.js.ast;

/**
 * The context in which a JsNode visitation occurs. This represents the set of
 * possible operations a JsVisitor subclass can perform on the currently visited
 * node.
 * 
 * @param <T>
 */
public interface JsContext<T extends JsVisitable<T>> {

  boolean canInsert();

  boolean canRemove();

  void insertAfter(T node);

  void insertBefore(T node);

  void removeMe();

  void replaceMe(T node);
}
