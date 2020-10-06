/*
 * PanmirrorUIDisplay.java
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


package org.rstudio.studio.client.panmirror.ui;

import org.rstudio.core.client.XRef;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.panmirror.command.PanmirrorMenuItem;

import com.google.inject.Inject;

import elemental2.promise.Promise;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

@JsType
public class PanmirrorUIDisplay {
   
  
   public PanmirrorUIDisplay() {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(GlobalDisplay globalDisplay)
   {
      this.globalDisplay_ = globalDisplay;
   }
   
   public void openURL(String url) 
   {
      globalDisplay_.openWindow(url);
   }
   
   public NavigateToXRef navigateToXRef;
   
   @JsFunction
   public interface NavigateToXRef
   {
      void navigate(String file, XRef xref);
   }

   public ShowContextMenu showContextMenu;   
   
   @JsFunction
   public interface ShowContextMenu
   {
      Promise<Boolean> show(PanmirrorMenuItem[] items, int clientX, int clientY);
   }
   
   private GlobalDisplay globalDisplay_;
}
