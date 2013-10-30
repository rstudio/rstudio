/*
 * Copyright 2014 Google Inc.
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
package com.google.gwt.user.server.rpc;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A dummy LinkedHashSet implementation for RPC tests.
 *
 * Originally, the RPC tests used GWT's java.util.LinkedHashSet for testing boundary
 * cases in generic handling. 
 *
 * However, LinkedHashSet now has its own custom field serializer, and no longer
 * exercises these boundary cases (which led to test failures).
 * 
 * So this class provides the existing RPC tests (which are fairly complicated) the
 * old/no custom field serializer LinkedHashSet, so they can continue testing their
 * boundary conditions.
 */
public class TestLinkedHashSet<E> extends HashSet<E> implements Set<E>, Cloneable {

  public TestLinkedHashSet() {
    super();
  }

  public TestLinkedHashSet(Collection<? extends E> c) {
    super(c);
  }

  public TestLinkedHashSet(int ignored) {
    super(ignored);
  }

  public TestLinkedHashSet(int ignored, float alsoIgnored) {
    super(ignored, alsoIgnored);
  }

  @Override
  public Object clone() {
    return new TestLinkedHashSet<E>(this);
  }

}
