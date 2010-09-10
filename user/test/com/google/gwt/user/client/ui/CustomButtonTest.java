/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.CustomButton.Face;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Test for <code>PushButton</code> as most of this widget's functionality is UI
 * based, the primary test will be in the new UI testing framework once it is
 * released.
 */
public class CustomButtonTest extends GWTTestCase {

  public String getModuleName() {
    return "com.google.gwt.user.User";
  }

  public void testCleanupOnRemoval() {
    PushButton pb = new PushButton();
    ToggleButton tb = new ToggleButton();

    RootPanel.get().add(pb);
    RootPanel.get().add(tb);

    // Synthesize mouse-over events to get both buttons into the 'hover' state.
    pb.getElement().dispatchEvent(
        Document.get().createMouseOverEvent(1, 0, 0, 0, 0, false, false, false,
            false, Event.BUTTON_LEFT, null));
    tb.getElement().dispatchEvent(
        Document.get().createMouseOverEvent(1, 0, 0, 0, 0, false, false, false,
            false, Event.BUTTON_LEFT, null));
    assertTrue(pb.isHovering());
    assertTrue(tb.isHovering());

    // Remove the buttons. The hover state should be cleared.
    pb.removeFromParent();
    tb.removeFromParent();
    assertFalse(pb.isHovering());
    assertFalse(tb.isHovering());
  }

  public void testCSS() {
    ToggleButton b = new ToggleButton("up", "down");
    b.setStyleName("random");
    b.setDown(true);
    assertEquals(b.getStylePrimaryName(), "random");

    Map faces = new HashMap();
    faces.put("downDisabled", b.getDownDisabledFace());
    faces.put("upDisabled", b.getUpDisabledFace());
    faces.put("down", b.getDownFace());
    faces.put("up", b.getUpFace());
    faces.put("upHovering", b.getUpHoveringFace());
    faces.put("downHovering", b.getDownHoveringFace());
    Iterator entries = faces.entrySet().iterator();
    // Set all faces as text.
    while (entries.hasNext()) {
      Map.Entry entry = (Entry) entries.next();
      Face f = (Face) entry.getValue();
      b.setCurrentFace(f);
      assertEquals("random", b.getStylePrimaryName());
      assertTrue(b.getStyleName().indexOf("random-" + f.getName()) != -1);
    }

    entries = faces.entrySet().iterator();
    b.addStyleName("fobar");
    // Set all faces as text.
    while (entries.hasNext()) {
      Map.Entry entry = (Entry) entries.next();
      Face f = (Face) entry.getValue();
      b.setCurrentFace(f);
      String computedStyleName = DOM.getElementProperty(b.getElement(),
          "className");
      assertTrue(computedStyleName.indexOf("random") == 0);
      assertTrue(computedStyleName.indexOf("random-" + f.getName()) >= 0);
      assertTrue(computedStyleName.indexOf("fobar") >= 0);
    }
  }

  public void testSettingFaces() {
    PushButton b = new PushButton();
    Map faces = new HashMap();
    faces.put("downDisabled", b.getDownDisabledFace());
    faces.put("upDisabled", b.getUpDisabledFace());
    faces.put("down", b.getDownFace());
    faces.put("up", b.getUpFace());
    faces.put("upHovering", b.getUpHoveringFace());
    faces.put("downHovering", b.getDownHoveringFace());
    Iterator entries = faces.entrySet().iterator();

    // Set all faces as text.
    while (entries.hasNext()) {
      Map.Entry entry = (Entry) entries.next();
      Face f = (Face) entry.getValue();
      String faceName = (String) entry.getKey();
      f.setText(faceName);
    }
    entries = faces.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry entry = (Entry) entries.next();
      Face f = (Face) entry.getValue();
      String faceName = (String) entry.getKey();
      assertEquals(f.getText(), faceName);
    }
    // Set all faces as HTML
    entries = faces.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry entry = (Entry) entries.next();
      Face f = (Face) entry.getValue();
      String faceName = (String) entry.getKey();
      f.setHTML("<b>" + faceName + "</b>");
    }

    entries = faces.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry entry = (Entry) entries.next();
      Face f = (Face) entry.getValue();
      String faceName = (String) entry.getKey();
      assertEquals(f.getText(), faceName);
      assertEquals(f.getHTML().toLowerCase(), "<b>" + faceName.toLowerCase()
          + "</b>");
    }
  }

  public void testSyntheticClick() {
    PushButton b = new PushButton();
    final ArrayList<String> events = new ArrayList<String>();

    b.addClickHandler(new ClickHandler() {
      public void onClick(ClickEvent event) {
        events.add(event.getNativeEvent().getType());
      }
    });

    RootPanel.get().add(b);

    // Synthesize over/down/up events, which should kick off CustomButton's
    // internal machinery to synthesize a click.
    b.getElement().dispatchEvent(
        Document.get().createMouseOverEvent(1, 0, 0, 0, 0, false, false, false,
            false, Event.BUTTON_LEFT, null));
    b.getElement().dispatchEvent(
        Document.get().createMouseDownEvent(1, 0, 0, 0, 0, false, false, false,
            false, Event.BUTTON_LEFT));
    b.getElement().dispatchEvent(
        Document.get().createMouseUpEvent(1, 0, 0, 0, 0, false, false, false,
            false, Event.BUTTON_LEFT));
    assertEquals("Expecting one click event", 1, events.size());
    assertEquals("Expecting one click event", "click", events.get(0));
  }
  
  public void testTransitions() {
    ToggleButton b = new ToggleButton("transitions");

    b.setDown(true);
    assertTrue(b.isDown());
    assertFalse(b.isHovering());
    assertTrue(b.isEnabled());

    b.setHovering(true);
    assertTrue(b.isDown());
    assertTrue(b.isHovering());
    assertTrue(b.isEnabled());

    b.setEnabled(false);
    assertTrue(b.isDown());
    assertFalse(b.isHovering());
    assertFalse(b.isEnabled());

    b.setDown(false);
    assertFalse(b.isHovering());
    assertFalse(b.isEnabled());
    assertFalse(b.isDown());

    b.setEnabled(false);
    assertFalse(b.isHovering());
    assertFalse(b.isEnabled());
    assertFalse(b.isDown());

    b.setEnabled(false);
    b.setHovering(true);
    assertTrue(b.isHovering());
    assertFalse(b.isDown());
    assertFalse(b.isEnabled());
  }
}
