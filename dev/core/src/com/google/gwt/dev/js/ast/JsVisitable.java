/*
 * Copyright 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.js.ast;

/**
 * Abstracts the idea that a class can be traversed.
 */
public interface JsVisitable {

  /**
   * Causes this object to have the visitor visit itself and its children.
   *
   * @param visitor the visitor that should traverse this node
   * @param ctx the context of an existing traversal
   */
  void traverse(JsVisitor visitor, JsContext ctx);
}
