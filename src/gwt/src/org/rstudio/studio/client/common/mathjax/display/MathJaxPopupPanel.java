/*
 * MathJaxPopupPanel.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;

public class MathJaxPopupPanel extends MiniPopupPanel
{
   public MathJaxPopupPanel()
   {
      super(true, false, false);
      addStyleName(RES.styles().popupPanel());
   }
   
   public void setText(String text)
   {
      getElement().setInnerText(text);
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
   
}
