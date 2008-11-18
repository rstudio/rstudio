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
package com.google.gwt.museum.client.defaultmuseum;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.museum.client.common.AbstractIssue;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * <h1>Attaching and detaching a RichTextArea too fast crashes GWT</h1>
 * 
 * <p>
 * The RichTextArea uses a Timer or iframe.onload event (depending on the
 * browser) to trigger the event system initialization. Basically, that
 * introduces a small delay between the time the RichTextArea is attached to the
 * page and the time that the event handlers are added.
 * </p>
 * <p>
 * However, if you programmatically (or just very quickly) remove the
 * RichTextArea from the page, you will immediately trigger the teardown methods
 * that remove the event listeners. In some browsers, removing the event
 * listeners before they are added throws a JavaScript exception do to an
 * undefined variable, which messes up the rest of the onDetach mechanism, which
 * gets your attach state out of sync, which means your app stops working
 * properly.
 * </p>
 * <p>
 * I am able to reliably reproduce these errors by attaching and removing a
 * RichTextArea very quickly, even in user time. The errors affect all browsers,
 * but Firefox is by far the most affected because it catches the iframe.onload
 * event, whereas the others just use a 1ms timer. Also, Safari and IE seem to
 * handle the javascript exceptions a little cleaner.
 * </p>
 */
public class Issue1897 extends AbstractIssue {
  /**
   * A set of options used to set the caption and content in the caption panel.
   */
  private class ControlPanel extends Composite {
    private final HorizontalPanel hPanel = new HorizontalPanel();

    public ControlPanel() {
      initWidget(hPanel);
      hPanel.setSpacing(10);

      // Add option to attach RichTextArea
      Button attachButton = new Button("Attach RichText", new ClickHandler() {
        public void onClick(ClickEvent event) {
          if (rta.isAttached()) {
            Window.alert("RichTextArea is already attached.");
          }
          wrapper.add(rta);
        }
      });
      hPanel.add(attachButton);

      // Add option to detach RichTextArea
      Button detachButton = new Button("Detach RichText", new ClickHandler() {
        public void onClick(ClickEvent event) {
          if (!rta.isAttached()) {
            Window.alert("RichTextArea is already detached.");
            return;
          }
          wrapper.remove(rta);
        }
      });
      hPanel.add(detachButton);

      // Add option to attach and detach RichTextArea
      Button quickDetachButton = new Button("Attach/Detach RichText",
          new ClickHandler() {
            public void onClick(ClickEvent event) {
              if (rta.isAttached()) {
                Window.alert("RichTextArea is already attached.");
                return;
              }
              wrapper.add(rta);
              wrapper.remove(rta);
            }
          });
      hPanel.add(quickDetachButton);
    }
  }

  private RichTextArea rta;

  private VerticalPanel wrapper;

  @Override
  public Widget createIssue() {
    rta = new RichTextArea();
    rta.setPixelSize(200, 100);

    // Combine the control panel and RichTextArea in a wrapper
    wrapper = new VerticalPanel();
    wrapper.setSpacing(10);
    wrapper.add(new ControlPanel());
    wrapper.add(rta);

    // Remove the RichTextArea after the wrapper is attached to the DOM
    DeferredCommand.addCommand(new Command() {
      public void execute() {
        wrapper.remove(rta);
      }
    });

    return wrapper;
  }

  @Override
  public String getInstructions() {
    return "You should <b>not</b> see a RichTextArea, and no errors should "
        + "occur.  If you click the Attach/Detach button, the RichTextArea will"
        + " be programmatically attached and detached from the DOM structure, "
        + "and you should not see any errors.";
  }

  @Override
  public String getSummary() {
    return "Attaching and detaching a RichTextArea too fast crashes GWT";
  }

  @Override
  public boolean hasCSS() {
    return true;
  }
}
