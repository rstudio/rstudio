/*
 * Copyright 2011 Google Inc.
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

package com.google.gwt.user.client.rpc;

import java.io.Serializable;
import java.util.List;

/**
 * Test class for the
 * {@link com.google.gwt.user.client.rpc.TypeCheckedObjectsTest} unit tests.
 * 
 * Intended to test the type checking of containers that use static nested classes.
 * This class is in its own file so that it can have a static nested class.
 */
public class TypeCheckedInnerClass extends TypeCheckedBaseClass {
  /**
   * An InnerClass for use in the List.
   */
  public static class InnerClass implements Serializable, IsSerializable {
    public int value;
  }
  
  public List<InnerClass> values;
  
  public TypeCheckedInnerClass() {
  }

}
