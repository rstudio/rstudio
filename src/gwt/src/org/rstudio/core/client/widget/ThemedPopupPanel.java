/*
 * ThemedPopupPanel.java
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
import com.google.gwt.user.client.ui.PopupPanel;

public class ThemedPopupPanel extends PopupPanel
{
   public interface Resources extends ClientBundle
   {
      @Source("ThemedPopupPanel.css")
      Styles styles();
   }

   public interface Styles extends CssResource
   {
      String themedPopupPanel();
   }

   public ThemedPopupPanel()
   {
      super();
      commonInit(RES);
   }

   public ThemedPopupPanel(boolean autoHide)
   {
      super(autoHide);
      commonInit(RES);
   }

   public ThemedPopupPanel(boolean autoHide, boolean modal)
   {
      super(autoHide, modal);
      commonInit(RES);
   }

   public ThemedPopupPanel(boolean autoHide, boolean modal, Resources res)
   {
      super(autoHide, modal);
      commonInit(res);
   }

   private void commonInit(Resources res)
   {
      addStyleName(res.styles().themedPopupPanel());
   }

   private static Resources RES = GWT.create(Resources.class);
   public static void ensureStylesInjected()
   {
      RES.styles().ensureInjected();
   }
}
