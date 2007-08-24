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
package com.google.gwt.sample.mail.client;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A composite that contains the shortcut stack panel on the left side. The
 * mailbox tree and shortcut lists don't actually do anything, but serve to show
 * how you can construct an interface using
 * {@link com.google.gwt.user.client.ui.StackPanel},
 * {@link com.google.gwt.user.client.ui.Tree}, and other custom widgets.
 */
public class Shortcuts extends Composite {

  /**
   * An image bundle specifying the images for this Widget and aggragating
   * images needed in child widgets.
   */
  public interface Images extends Contacts.Images, Mailboxes.Images {
    AbstractImagePrototype contactsgroup();

    AbstractImagePrototype leftCorner();

    AbstractImagePrototype mailgroup();

    AbstractImagePrototype rightCorner();

    AbstractImagePrototype tasksgroup();
  }
  
  private int nextHeaderIndex = 0;
  
  private StackPanel stackPanel = new StackPanel() {
    @Override
    public void onBrowserEvent(Event event) {
      int oldIndex = getSelectedIndex();
      super.onBrowserEvent(event);
      int newIndex = getSelectedIndex();
      if (oldIndex != newIndex) {
        updateSelectedStyles(oldIndex, newIndex);
      }
    }
  };

  /**
   * Constructs a new shortcuts widget using the specified images.
   * 
   * @param images a bundle that provides the images for this widget
   */
  public Shortcuts(Images images) {
    // Create the groups within the stack panel.
    add(images, new Mailboxes(images), images.mailgroup(), "Mail");
    add(images, new Tasks(), images.tasksgroup(), "Tasks");
    add(images, new Contacts(images), images.contactsgroup(), "Contacts");
    
    initWidget(stackPanel);
  }
  
  @Override
  protected void onLoad() {
    // Show the mailboxes group by default.
    stackPanel.showStack(0);
    updateSelectedStyles(-1, 0);
  }

  private void add(Images images, Widget widget, AbstractImagePrototype imageProto,
      String caption) {
    widget.addStyleName("mail-StackContent");
    stackPanel.add(widget, createHeaderHTML(images, imageProto, caption), true);
  }

  private String computeHeaderId(int index) {
    return "header-" + this.hashCode() + "-" + index;
  }
  
  /**
   * Creates an HTML fragment that places an image & caption together, for use
   * in a group header.
   * 
   * @param imageProto an image prototype for an image
   * @param caption the group caption
   * @return the header HTML fragment
   */
  private String createHeaderHTML(Images images,
      AbstractImagePrototype imageProto, String caption) {
    
    boolean isTop = (nextHeaderIndex == 0);
    String cssId = computeHeaderId(nextHeaderIndex);
    nextHeaderIndex++;
    
    String captionHTML = "<table class='caption' cellpadding='0' cellspacing='0'>"
        + "<tr><td class='lcaption'>" + imageProto.getHTML()
        + "</td><td class='rcaption'><b style='white-space:nowrap'>" + caption 
        + "</b></td></tr></table>";
    
    return "<table id='" + cssId + "' align='left' cellpadding='0' cellspacing='0'"
        + (isTop ? " class='is-top'" : "" ) + "><tbody>"
        + "<tr><td class='box-00'>" + images.leftCorner().getHTML() + "</td>"
        + "<td class='box-10'>&nbsp;</td>"
        + "<td class='box-20'>" + images.rightCorner().getHTML() + "</td>"        
        + "</tr><tr>"
        + "<td class='box-01'>&nbsp;</td>"
        + "<td class='box-11'>" + captionHTML + "</td>"
        + "<td class='box-21'>&nbsp;</td>"        
        + "</tr></tbody></table>";
  }
  
  /**
   * Example of using the DOM class to do CSS class name tricks that have
   * become common to AJAX apps. In this case we add CSS class name for the
   * stack panel item that is below the selected item.
   */
  private void updateSelectedStyles(int oldIndex, int newIndex) {
    oldIndex++;
    if (oldIndex > 0 && oldIndex < stackPanel.getWidgetCount()) {
      Element elem = DOM.getElementById(computeHeaderId(oldIndex));
      DOM.setElementProperty(elem, "className", "");
    }
    
    newIndex++;
    if (newIndex > 0 && newIndex < stackPanel.getWidgetCount()) {
      Element elem = DOM.getElementById(computeHeaderId(newIndex));
      DOM.setElementProperty(elem, "className", "is-beneath-selected");
    }
  }
}
