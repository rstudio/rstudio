/*
 * ModalDialogTracker.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import com.google.gwt.user.client.ui.PopupPanel;

import java.util.ArrayList;

public class ModalDialogTracker
{
   public static void onShow(PopupPanel panel)
   {
      dialogStack_.add(panel);
   }

   public static boolean isTopMost(PopupPanel panel)
   {
      return !dialogStack_.isEmpty() &&
             dialogStack_.get(dialogStack_.size()-1) == panel;
   }

   public static void onHide(PopupPanel panel)
   {
      while (dialogStack_.remove(panel))
      {}
   }

   private static ArrayList<PopupPanel> dialogStack_ =
         new ArrayList<PopupPanel>();
}
