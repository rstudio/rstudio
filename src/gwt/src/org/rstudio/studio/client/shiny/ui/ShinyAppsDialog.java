/*
 * ShinyAppsDialog.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.shiny.ui;

import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.shiny.model.ShinyAppsServerOperations;

import com.google.gwt.user.client.ui.Widget;

public abstract class ShinyAppsDialog<W extends Widget> 
                extends ModalDialogBase
{
   public ShinyAppsDialog(ShinyAppsServerOperations server, 
                          final GlobalDisplay display, 
                          W contents)
   {
      server_ = server;
      display_ = display;
      contents_ = contents;
   }
   
   @Override
   protected Widget createMainWidget()
   {
      return contents_;
   }
   
   protected W contents_;
   
   protected ShinyAppsServerOperations server_;
   protected GlobalDisplay display_;
}
