/*
 * AceInfoBar.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.dom.DomUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

public class AceInfoBar extends Composite
{
   public AceInfoBar(AceEditorWidget parent)
   {
      container_ = new FlowPanel();
      container_.addStyleName(RES.styles().container());

      label_ = new Label();
      label_.addStyleName(RES.styles().label());

      container_.add(label_);

      initWidget(container_);
      attachTo(parent);
   }
   
   public void setText(String text)
   {
      label_.setText(text);
   }
   
   private boolean attachTo(AceEditorWidget widget)
   {
      Element aceScroller = DomUtils.getFirstElementWithClassName(
            widget.getElement(),
            "ace_scroller");
      
      if (aceScroller == null)
         return false;
      
      aceScroller.appendChild(getElement());
      return true;
   }
   
   public void show()
   {
      setVisible(true);
   }
   
   public void hide()
   {
      setVisible(false);
   }
   
   private final FlowPanel container_;
   private final Label label_;
   
   // Styling boilerplate ----
   public interface Styles extends CssResource
   {
      String container();
      String label();
   }

   public interface Resources extends ClientBundle
   {
      @Source("AceInfoBar.css")
      Styles styles();
   }

   private static Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }

}
