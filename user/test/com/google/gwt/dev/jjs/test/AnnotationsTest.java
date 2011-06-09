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

import com.google.gwt.junit.client.GWTTestCase;

import java.lang.annotation.Annotation;

/**
 * Tests annotations support.
 */
public class AnnotationsTest extends GWTTestCase {

  private static class Foo implements IFoo {
    public Class<? extends Annotation> annotationType() {
      return IFoo.class;
    }

    public NestedEnum value() {
      return IFoo.NestedEnum.FOO2;
    }

    public Class<? extends NestedEnum> valueClass() {
      return IFoo.NestedEnum.class;
    }
  }

  private @interface IFoo {
    enum NestedEnum {
      FOO1, FOO2
    }

    // http://bugs.sun.com/view_bug.do?bug_id=6512707
    NestedEnum value() default com.google.gwt.dev.jjs.test.AnnotationsTest.IFoo.NestedEnum.FOO1;

    // http://bugs.sun.com/view_bug.do?bug_id=6512707
    Class<? extends NestedEnum> valueClass() default com.google.gwt.dev.jjs.test.AnnotationsTest.IFoo.NestedEnum.class;
  }

  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testAnnotationImplementor() {
    Foo f = new Foo();
    assertEquals(Foo.class, f.getClass());
    assertEquals(IFoo.NestedEnum.FOO2, f.value());
    assertEquals(IFoo.NestedEnum.class, f.valueClass());
  }

}
