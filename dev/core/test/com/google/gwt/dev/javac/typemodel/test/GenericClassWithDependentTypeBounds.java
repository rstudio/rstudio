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
package com.google.gwt.dev.javac.typemodel.test;

import java.io.Serializable;

/**
 * Tests a generic class that has type parameters that depend on each other.
 * Also tests a generic method with type parameters that depend on each other.
 * 
 * @param <C>
 * @param <M>
 */
public class GenericClassWithDependentTypeBounds<C extends GenericClassWithTypeBound<M>, M extends Serializable> {

  public <Q extends GenericClassWithTypeBound<P>, P extends Serializable> void genericMethod(
      Q param) {
  }
}
