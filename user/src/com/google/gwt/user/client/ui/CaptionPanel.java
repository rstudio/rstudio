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
package com.google.gwt.user.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;

/**
 * A panel that wraps its contents in a border with a caption that appears in the
 * upper left corner of the border. This is an implementation of the fieldset
 * HTML element.
 */
public class CaptionPanel extends SimplePanel {
  /**
   * Implementation class without browser-specific hacks.
   */
  public static class CaptionPanelImpl {
    public void setCaption(Element fieldset, Element legend, String caption) {
      if (caption != null && !"".equals(caption)) {
        DOM.setInnerHTML(legend, caption);
        DOM.insertChild(fieldset, legend, 0);
      } else if (DOM.getParent(legend) != null) {
        DOM.removeChild(fieldset, legend);
      }
    }
  }

  /**
   * Implementation class that handles Mozilla rendering issues.
   */
  public static class CaptionPanelImplMozilla extends CaptionPanelImpl {
    @Override
    public void setCaption(final Element fieldset, Element legend, String caption) {
      DOM.setStyleAttribute(fieldset, "display", "none");
      super.setCaption(fieldset, legend, caption);
      DOM.setStyleAttribute(fieldset, "display", "");
    }
  }
  
  /**
   * Implementation class that handles Safari rendering issues.
   */
  public static class CaptionPanelImplSafari extends CaptionPanelImpl {
    @Override
    public void setCaption(final Element fieldset, Element legend, String caption) {
      DOM.setStyleAttribute(fieldset, "visibility", "hidden");
      super.setCaption(fieldset, legend, caption);
      DeferredCommand.addCommand(new Command() {
        public void execute() {
          DOM.setStyleAttribute(fieldset, "visibility", "");
        }
      });
    }
  }

  /**
   * The implementation instance.
   */
  private static CaptionPanelImpl impl = (CaptionPanelImpl) GWT.create(CaptionPanelImpl.class);

  /**
   * The legend element used as the caption.
   */
  private Element legend;

  /**
   * The caption at the top of the border.
   */
  private String caption;

  /**
   * Constructs a CaptionPanel having the specified caption.
   * 
   * @param caption the caption to display
   */
  public CaptionPanel(String caption) {
    super(DOM.createElement("fieldset"));
    legend = DOM.createElement("legend");
    DOM.appendChild(getElement(), legend);
    setCaption(caption);
  }

  /**
   * Constructor.
   * 
   * @param caption the caption to display
   * @param w the widget to add to the panel
   */
  public CaptionPanel(String caption, Widget w) {
    this(caption);
    setWidget(w);
  }

  /**
   * @return the caption on top of the border
   */
  public String getCaption() {
    return this.caption;
  }

  /**
   * Set the caption in the border. Pass in null or an empty string to remove the
   * caption completely, leaving just a box.
   * 
   * @param caption the new caption
   */
  public void setCaption(String caption) {
    this.caption = caption;
    impl.setCaption(getElement(), legend, caption);
  }
}
