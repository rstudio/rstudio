/*
 * MiniPopupPanel.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.core.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.DecoratedPopupPanel;

public class MiniPopupPanel extends DecoratedPopupPanel
{
   public MiniPopupPanel()
   {
      super();
      commonInit();
   }

   public MiniPopupPanel(boolean autoHide)
   {
      super(autoHide);
      commonInit();
   }

   public MiniPopupPanel(boolean autoHide, boolean modal)
   {
      super(autoHide, modal);
      commonInit();
   }
   
   private void commonInit()
   {
      addStyleName(RES.styles().popupPanel());
   }
   
   public interface Styles extends CssResource
   {
      String popupPanel();
   }
   
   public interface Resources extends ClientBundle
   {
      @Source("MiniPopupPanel.css")
      Styles styles();
   }
   
   private static Resources RES = GWT.create(Resources.class);
   static {
      RES.styles().ensureInjected();
   }

}
