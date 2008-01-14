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
package com.google.gwt.dev.jjs.test;

/**
 * This is used by {@link JsniConstructorTest}.
 */
class StaticObject {
  /**
   * We'll always create this like <code>staticObj.new InstanceObject()</code>.
   */
  public class InstanceObject {
    private final int i;

    private InstanceObject(int i) {
      this.i = i;
    }

    public int foo() {
      return i + StaticObject.this.foo();
    }
    
    public class NestedInstanceObject {
      private final int i;
      
      private NestedInstanceObject(int i) {
        this.i = i;
      }
      
      public int foo() {
        return i + InstanceObject.this.foo();
      }
    }
  }

  public static class NoArgObject {
    private final int i = 4;

    public int foo() {
      return i;
    }
  }

  public static class NoInitObject {
    public int foo() {
      return 5;
    }
  }

  public static class StaticInnerObject {
    private final int i;

    private StaticInnerObject(int i) {
      this.i = i;
    }

    public int foo() {
      return i;
    }
  }

  private final int i;

  /**
   * This constructor always throws an exception.
   */
  private StaticObject(boolean throwRuntime) throws StaticObjectException {
    if (throwRuntime) {
      throw new RuntimeException();
    } else {
      throw new StaticObjectException();
    }
  }

  private StaticObject(int i) {
    this.i = i;
  }

  public int foo() {
    return i;
  }

  public static class StaticObjectException extends Exception {
  }
}
