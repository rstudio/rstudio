/*
 * MathJaxPopupPanel.java
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
package org.rstudio.studio.client.common.mathjax.display;

import org.rstudio.core.client.widget.MiniPopupPanel;
import org.rstudio.studio.client.common.mathjax.MathJax;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class MathJaxPopupPanel extends MiniPopupPanel
{
   public MathJaxPopupPanel(MathJax mathjax)
   {
      super(true, false, false);
      
      mathjax_ = mathjax;
      
      container_ = new VerticalPanel();
      contentPanel_ = new FlowPanel();
      
      container_.add(contentPanel_);
      
      setWidget(container_);
      
      addStyleName(RES.styles().popupPanel());
   }
   
   public Element getContentElement()
   {
      return contentPanel_.getElement();
   }
   
   // Styles ------------------------------------------
   
   public interface Styles extends CssResource
   {
      String popupPanel();
   }
   
   public interface Resources extends ClientBundle
   {
      @Source("MathJaxPopupPanel.css")
      Styles styles();
   }
   
   private static Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }
   
   @SuppressWarnings("unused") private final MathJax mathjax_;
   private final VerticalPanel container_;
   private final FlowPanel contentPanel_;
   
}
