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
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.IsRenderable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PotentialElement;
import com.google.gwt.user.client.ui.RenderablePanel;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Integration test for {@link com.google.gwt.user.client.ui.IsRenderable
 * IsRenderable}.
 */
public class IsRenderableIntegrationTest extends GWTTestCase {
  static class Deep {
    interface Binder extends UiBinder<IsRenderable, Deep> {
    }

    static final Binder binder = GWT.create(Binder.class);

    @UiField
    RenderablePanel top;
    @UiField
    DivElement outerDiv;
    @UiField
    DivElement innerDiv;
    @UiField
    SimpleRenderable outerRenderable;
    @UiField
    Label outerLabel;
    @UiField
    Label innerLabel;
    @UiField
    SimpleRenderable innerRenderable;
    @UiField
    Image outerI18nImage;
    @UiField
    Image innerI18nImage;
    @UiField
    RenderableComposite outerComposite;
    @UiField
    RenderableComposite innerComposite;

    Object receivedValue;

    Deep() {
      binder.createAndBindUi(this);
    }

    @UiHandler("outerRenderable")
    void onValueChange(ValueChangeEvent<Object> event) {
      receivedValue = event.getValue();
    }
  }

  static class Shallow {
    interface Binder extends UiBinder<IsRenderable, Shallow> {
    }

    static final Binder binder = GWT.create(Binder.class);

    @UiField
    SimpleRenderable widget;

    Shallow() {
      binder.createAndBindUi(this);
    }
  }

  @Override
  public String getModuleName() {
    return "com.google.gwt.uibinder.test.LazyWidgetBuilderSuite";
  }

  public void testDeep() {
    Deep deep = new Deep();
    assertTrue(PotentialElement.isPotential(deep.top.getElement()));
    assertTrue(PotentialElement.isPotential(deep.outerRenderable.getElement()));
    assertTrue(PotentialElement.isPotential(deep.innerRenderable.getElement()));

    /*
     * This is pretty surprising. I don't think it's a good idea to be filling
     * in the owner's fields at some arbitrary later date. I doubt it's a real
     * speed up, since the things will still have to be instantiated before the
     * user sees them, and it seems guaranteed to be a source of confusion.
     * 
     * Hmm. What keeps the object that fills in the the fields at attach time
     * from being gc'd? And are we sure it's not leaking?
     * 
     * Although I suppose we have no choice with dom children. Interesting
     * problem.
     */
    assertNull(deep.outerI18nImage);
    assertNull(deep.innerI18nImage);
    assertNull(deep.outerLabel);
    assertNull(deep.innerLabel);
    assertNull(deep.innerLabel);
    assertNull(deep.outerDiv);
    assertNull(deep.innerDiv);

    // Oh dear, we're not even consistent about it. That means the above is a
    // bug, period.
    assertNotNull(deep.outerComposite);
    assertNotNull(deep.innerComposite);
    assertTrue(PotentialElement.isPotential(deep.outerComposite.getWidget().getElement()));
    assertTrue(PotentialElement.isPotential(deep.innerComposite.getWidget().getElement()));

    try {
      RootPanel.get().add(deep.top);

      assertEquals("Outer div", deep.outerDiv.getInnerText());
      assertEquals("Inner div", deep.innerDiv.getInnerText());

      assertEquals("[string built]Outer renderable",
          deep.outerRenderable.getElement().getInnerText());
      assertEquals("[string built]Inner renderable",
          deep.innerRenderable.getElement().getInnerText());

      assertNotNull(deep.outerI18nImage);
      assertNotNull(deep.innerI18nImage);

      assertEquals("Outer label", deep.outerLabel.getText());
      assertEquals("Inner label", deep.innerLabel.getText());

      assertEquals("[string built]Renderable",
          deep.outerComposite.getWidget().getElement().getInnerText());
      assertEquals("[string built]Renderable",
          deep.innerComposite.getWidget().getElement().getInnerText());

      // Test event handling. It is cool as hell that this works!
      deep.outerRenderable.setValue("foo");
      assertEquals("foo", deep.receivedValue);

      /*
       * This is really a test of SimpleRenderable itself, but what the hell.
       * That class might be useful some day.
       */
      deep.outerRenderable.setText("fnord");
      assertEquals("[updated]fnord", deep.outerRenderable.getElement().getInnerText());

    } finally {
      deep.top.removeFromParent();
    }
  }

  public void testLegacyComposite() {
    LegacyComposite legacyComposite = new LegacyComposite();
    assertEquals("span", legacyComposite.span.getInnerText());
  }

  public void testNestedRenderableComposite() {
    RenderableComposite.Meta meta = new RenderableComposite.Meta();
    assertTrue(PotentialElement.isPotential(meta.getElement()));

    try {
      RootPanel.get().add(meta);
      assertEquals("[dom built]Renderable",
          meta.getWidget().getElement().getInnerText());
    } finally {
      meta.removeFromParent();
    }
  }
  
  public void testRenderableComposite() {
    RenderableComposite renderableComposite = new RenderableComposite();
    assertTrue(PotentialElement.isPotential(renderableComposite.getElement()));

    // TODO(rdcastro) This results in an NPE 
    // assertEquals("something useful", renderableComposite.toString());

    try {
      RootPanel.get().add(renderableComposite);
      assertEquals("[dom built]Renderable",
          renderableComposite.getWidget().getElement().getInnerText());
    } finally {
      renderableComposite.removeFromParent();
    }
  }


  public void testShallow() {
    Shallow shallow = new Shallow();
    assertTrue(PotentialElement.isPotential(shallow.widget.getElement()));
    try {
      RootPanel.get().add(shallow.widget);
      assertEquals("[dom built]" + shallow.widget.getText(),
          shallow.widget.getElement().getInnerText());
    } finally {
      shallow.widget.removeFromParent();
    }
  }
}
