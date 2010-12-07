/*
 * FocusHelper.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Focusable;

public class FocusHelper
{
   public static void setFocusDeferred(final CanFocus canFocus)
   {
      DeferredCommand.addCommand(new Command() {
         public void execute()
         {
            canFocus.focus();
         }   
      });
   }
   
   public static void setFocusDeferred(final Focusable focusable)
   {
      DeferredCommand.addCommand(new Command() {
         public void execute()
         {
            focusable.setFocus(true);
         }   
      });
   } 
}
