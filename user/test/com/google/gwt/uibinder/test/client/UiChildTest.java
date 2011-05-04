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
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiChild;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.Widget;

/**
 * Integration tests of @UiChild.
 */
public class UiChildTest extends GWTTestCase {
  static class ArbitraryType {
  }

  static class MyPanel extends FlowPanel {
    MenuItem menuItem;
    private ArbitraryType arbitrary;

    @UiChild(tagname = "childAsImpl")
    public void addChildAsImpl(MyViewImpl child) {
      this.add(child);
    }

    @UiChild(tagname = "childAsInterface")
    public void addChildAsInterface(MyView child) {
      this.add(child);
    }

    @UiChild
    public void addUiObject(MenuItem item) {
      this.menuItem = item;
    }

    @UiChild
    void addArbitraryType(ArbitraryType thingy) {
      this.arbitrary = thingy;
    }
  }

  interface MyView extends IsWidget {
    String getName();

    void setName(String name);
  }

  static class MyViewImpl extends Composite implements MyView {
    MyViewImpl() {
      initWidget(new Label());
    }

    public String getName() {
      return asLabel().getText();
    }

    public void setName(String name) {
      asLabel().setText(name);
    }

    private Label asLabel() {
      return ((Label) getWidget());
    }
  }

  static class Ui extends Composite {
    interface Binder extends UiBinder<HTMLPanel, Ui> {
    }

    static final Binder BINDER = GWT.create(Binder.class);

    @UiField
    MyViewImpl asImpl;

    @UiField
    MyView asInterface;

    @UiField
    MyPanel panel;
    
    @UiField
    MenuItem menuItem;
    
    @UiField
    ArbitraryType arbitrary;

    Ui() {
      initWidget(BINDER.createAndBindUi(this));
    }

    @Override
    public Widget asWidget() {
      return this;
    }

    @UiFactory
    MyView createForInterface() {
      return new MyViewImpl();
    }
  }

  Ui subject;

  @Override
  public String getModuleName() {
    return "com.google.gwt.uibinder.test.UiBinderSuite";
  }

  public void testIsWidget() {
    assertNotNull(subject.asInterface);
    assertEquals("interface", subject.asInterface.getName());
    assertSame(subject.panel, subject.asInterface.asWidget().getParent());
  }

  public void testSimple() {
    assertNotNull(subject.asImpl);
    assertEquals("impl", subject.asImpl.getName());
    assertSame(subject.panel, subject.asImpl.getParent());
  }
  
  public void testUiObject() {
    assertSame(subject.menuItem, subject.panel.menuItem);
  }
  
  public void testPojo() {
    assertSame(subject.arbitrary, subject.panel.arbitrary);
  }

  @Override
  protected void gwtSetUp() {
    subject = new Ui();
  }
}
