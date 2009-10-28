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
package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.core.client.Duration;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.museum.client.common.ControlInputPanel;
import com.google.gwt.museum.client.common.SimpleLogger;
import com.google.gwt.museum.client.common.ControlInputPanel.IntegerInput;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.List;

/*
 * Originally, on IE, about three seconds to distroy
 */
/**
 * Tests bad behavior for clear.
 * 
 * <pre>
 * ff -- 1000 flow panels, aprox 500  millis
 * ie -- 1000 flow panels, aprox 3000 millis
 * 
 * in new version
 * ie -- 1000 flow panels, aprox 30-80 millis
 * ff -- 1000 flow panels, aprox 13-50 millis
 * </pre>
 * <p>
 * <img class='gallery' src='FlowPanel.png'/>
 * </p>
 */
public class SpeedForClear extends AbstractIssue {
  private Panel target;
  private List<Widget> children = new ArrayList<Widget>();
  private SimpleLogger log = new SimpleLogger();

  @Override
  public Widget createIssue() {
    VerticalPanel v = new VerticalPanel();
    ControlInputPanel p = new ControlInputPanel();
    v.add(p);
    v.add(log);
    final IntegerInput size = new IntegerInput("flowpanel", 10, p);
    Button create = new Button("create widget", new ClickHandler() {

      public void onClick(ClickEvent event) {
        createLargeFlowPanel(size.getValue());
      }
    });

    Button distroy = new Button("time the removal", new ClickHandler() {
      public void onClick(ClickEvent event) {
        Duration d = new Duration();
        target.clear();

        log.report("Took " + d.elapsedMillis() + " milliseconds to clear "
            + size.getValue() + " widgets from a flow panel");
        for (Widget child : children) {
          if (child.getElement().getPropertyString("__listener") != null) {
            throw new IllegalStateException(
                "each child should no longer have a listener");
          }
        }
      }
    });
    v.add(create);
    v.add(distroy);
    return v;
  }

  @Override
  public String getInstructions() {
    return "check the speed of clear methods";
  }

  @Override
  public String getSummary() {
    return "clear() speed check";
  }

  @Override
  public boolean hasCSS() {
    return false;
  }

  private void createLargeFlowPanel(int size) {

    if (target != null) {
      target.removeFromParent();
    }
    target = new FlowPanel();

    for (int i = 0; i < size; i++) {
      Widget w = new Label("widget-" + i);
      target.add(w);
      children.add(w);
    }

    RootPanel.get().add(target);
    for (Widget child : target) {
      if (child.getElement().getPropertyString("__listener") == null) {
        throw new IllegalStateException("each child should now have a listener");
      }
    }
  }

}
