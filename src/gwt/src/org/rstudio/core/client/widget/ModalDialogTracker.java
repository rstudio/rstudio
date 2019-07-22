/*
 * ModalDialogTracker.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.PopupPanel;
import org.rstudio.core.client.command.impl.DesktopMenuCallback;
import org.rstudio.studio.client.application.Desktop;

import java.util.ArrayList;

public class ModalDialogTracker
{
   public static void onShow(PopupPanel panel)
   {
      dialogStack_.add(panel);
      if (Desktop.hasDesktopFrame())
         DesktopMenuCallback.setMainMenuEnabled(false);
      updateInert();
   }

   public static boolean isTopMost(PopupPanel panel)
   {
      return !dialogStack_.isEmpty() &&
             dialogStack_.get(dialogStack_.size()-1) == panel;
   }

   public static void onHide(PopupPanel panel)
   {
      dialogStack_.removeIf(panel::equals);
      if (Desktop.hasDesktopFrame() && numModalsShowing() == 0)
         DesktopMenuCallback.setMainMenuEnabled(true);
      updateInert();
   }
   
   public static int numModalsShowing()
   {
      return dialogStack_.size();
   }

   private static void updateInert()
   {
      Element ideWrapper = DOM.getElementById("rstudio_container");
      Element ideFrame = DOM.getElementById("rstudio");
      if (ideWrapper == null || ideFrame == null)
         return;
      
      if (numModalsShowing() == 0)
      {
         ideWrapper.removeAttribute("inert");
         ideFrame.removeAttribute("inert");
      }
      else if (numModalsShowing() == 1)
      {
         ideWrapper.setAttribute("inert", "");
         ideFrame.setAttribute("inert", "");
      }
   }

   private static final ArrayList<PopupPanel> dialogStack_ = new ArrayList<>();
}
