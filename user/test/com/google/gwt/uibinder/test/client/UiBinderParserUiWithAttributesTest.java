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
package com.google.gwt.uibinder.test.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiConstructor;
import com.google.gwt.uibinder.client.UiField;

/**
 * Tests &lt;ui:attribute>.
 */
public class UiBinderParserUiWithAttributesTest extends GWTTestCase {
  
  static class TestBeanA {
  }
  
  static class TestBeanB {
    TestBeanA beanA;

    public void setBeanA(TestBeanA beanA) {
      this.beanA = beanA;
    }
  }
  
  static class TestBeanC {
    TestBeanA beanA;
    TestBeanB beanB;
    
    @UiConstructor
    public TestBeanC(TestBeanA beanA) {
      this.beanA = beanA;
    }
    
    public void setBeanB(TestBeanB beanB) {
      this.beanB = beanB;
    }
  }
  
  
  static class Ui {
    interface Binder extends UiBinder<Element, Ui> {
    }
    
    static final Binder binder = GWT.create(Binder.class);
    
    @UiField(provided = true)
    TestBeanA test1 = new TestBeanA();
    
    @UiField
    TestBeanA test2;
    
    @UiField
    TestBeanB test3;
    
    @UiField
    TestBeanC test4;
    
    @UiField
    TestBeanC test5;
    
    Ui() {
      binder.createAndBindUi(this);
    }
  }
  
  @Override
  public String getModuleName() {
    return "com.google.gwt.uibinder.test.LazyWidgetBuilderSuite";
  }
  
  public void testUiWith() {
    Ui ui = new Ui();
    
    assertNotNull(ui.test1);
    assertNotNull(ui.test2);
    
    assertNotNull(ui.test3);
    assertNotNull(ui.test3.beanA);
    assertSame(ui.test1, ui.test3.beanA);
    
    
    assertNotNull(ui.test4);
    assertSame(ui.test1, ui.test4.beanA);
    assertNull(ui.test4.beanB);
    
    assertNotNull(ui.test5);
    assertSame(ui.test1, ui.test5.beanA);
    assertSame(ui.test3, ui.test5.beanB);
  }
}
