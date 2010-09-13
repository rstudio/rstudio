/*
 * Copyright 2009 Google Inc.
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

/**
 * Base tests for classes that extends {@link SimplePanel}.
 * 
 * @param <T> the panel type
 */
public abstract class SimplePanelTestBase<T extends SimplePanel> extends
    PanelTestBase<T> {

  public void testAddArmor() {
    T panel = createPanel();
    Label l = new Label("L");
    Label l2 = new Label("L2");

    panel.add(l);
    try {
      panel.add(l2);
      fail("expected IllegalStateException");
    } catch (IllegalStateException e) {
      /* pass */
    }
  }

  public void testHasOneWidget() {
    HasOneWidget panel = createPanel();
    Label l = new Label("L");
    Label l2 = new Label("L2");
    assertNull(panel.getWidget());

    panel.setWidget(l);
    assertEquals(l, panel.getWidget());

    panel.setWidget(l2);
    assertEquals(l2, panel.getWidget());

    panel.setWidget(null);
    assertNull(panel.getWidget());
  }

  public void testHasOneWidgetAsWidget() {
    HasOneWidget panel = createPanel();
    IsWidgetImpl liw = new IsWidgetImpl(new Label("L"));
    IsWidgetImpl liw2 = new IsWidgetImpl(new Label("L2"));
    
    assertNull(panel.getWidget());

    panel.setWidget(liw);
    assertSame(liw.w, panel.getWidget());

    panel.setWidget(liw2);
    assertSame(liw2.w, panel.getWidget());

    panel.setWidget(null);
    assertNull(panel.getWidget());
  }
  
  public void testRemoveAndClear() {
    T panel = createPanel();
    Label l = new Label("L");
    panel.setWidget(l);
    panel.remove(l);
    assertNull(panel.getWidget());

    panel.setWidget(l);
    assertEquals(l, panel.getWidget());

    panel.clear();
    assertNull(panel.getWidget());
  }

  @Override
  protected boolean supportsMultipleWidgets() {
    return false;
  }
}
