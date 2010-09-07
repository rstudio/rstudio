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
package com.google.gwt.user.client.ui;

import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SimpleKeyProvider;

import java.util.Arrays;

/**
 * Eponymous unit test.
 */
public class ValueListBoxTest extends GWTTestCase {

  static class Foo {
    final String value;

    Foo(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return "Foo [value=" + value + "]";
    }
  }

  static class FooRenderer extends AbstractRenderer<Foo> {
    public String render(Foo object) {
      if (object == null) {
        return "";
      }
      return "Foo: " + object.value;
    }
  }

  static class KeyProvider implements ProvidesKey<Foo> {
    public String getKey(Foo item) {
      return item.value;
    }
  }

  private static final FooRenderer renderer = new FooRenderer();

  ValueListBox<Foo> subject;

  @Override
  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void xx_testExtraValueSet() {  // TODO(rjrjr) Strange failures on firefox
    Foo[] values = new Foo[] {new Foo("able"), new Foo("baker")};
    Foo baz = new Foo("baz");
    
    subject.setAcceptableValues(Arrays.asList(values));
    assertEquals(2, getSelect().getLength());

    subject.setValue(baz);
    assertEquals(baz, subject.getValue());
    assertEquals(3, getSelect().getLength());
  }

  public void testNakedSet() {
    assertNull(subject.getValue());
    
    SelectElement elm = getSelect();
    assertEquals(0, elm.getLength());

    Foo barFoo = new Foo("bar");

    setAndCheck(barFoo);
    
    assertEquals(1, elm.getLength());
    assertEquals(renderer.render(barFoo), elm.getValue());
  }

  public void xx_testNormalSet() { // TODO(rjrjr) Strange failures on firefox
    Foo[] values = new Foo[] {new Foo("able"), new Foo("baker")};
    subject.setAcceptableValues(Arrays.asList(values));

    assertEquals(2, getSelect().getLength());

    setAndCheck(values[0]);
    setAndCheck(values[1]);
    setAndCheck(values[0]);

    assertEquals(2, getSelect().getLength());
  }

  public void xx_testNormalSetWithKeyProvider() { // TODO(rjrjr) Strange failures on firefox
    gwtTearDown(); // Tear down the usual test subject
    
    KeyProvider keyProvider = new KeyProvider();
    subject = new ValueListBox<Foo>(renderer, keyProvider);
    RootPanel.get().add(subject);
    
    Foo[] byReference = new Foo[] {new Foo("able"), new Foo("baker")};
    subject.setAcceptableValues(Arrays.asList(byReference));
    Foo[] byValue = new Foo[] {new Foo("able"), new Foo("baker")};

    assertEquals(2, getSelect().getLength());

    setAndCheck(byReference[0], keyProvider);
    setAndCheck(byReference[1], keyProvider);
    setAndCheck(byReference[0], keyProvider);

    setAndCheck(byValue[0], keyProvider);
    setAndCheck(byValue[1], keyProvider);
    setAndCheck(byValue[0], keyProvider);

    assertEquals(2, getSelect().getLength());
  }

  @Override
  protected void gwtSetUp() {
    subject = new ValueListBox<Foo>(renderer);
    RootPanel.get().add(subject);
  }

  @Override
  protected void gwtTearDown() {
    RootPanel.get().remove(subject);
  }
  
  private SelectElement getSelect() {
    return subject.getWidget().getElement().cast();
  }
  private void setAndCheck(Foo value) {
    setAndCheck(value, new SimpleKeyProvider<Foo>());
  }

  private void setAndCheck(Foo value, ProvidesKey<Foo> keyProvider) {
    subject.setValue(value);
    assertEquals(keyProvider.getKey(value), keyProvider.getKey(subject.getValue()));
    assertEquals(renderer.render(value), getSelect().getValue());
  }
}
