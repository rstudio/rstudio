/*
 * PanmirrorUI.java
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


import org.rstudio.studio.client.panmirror.dialogs.PanmirrorDialogs;

import elemental2.core.JsObject;
import jsinterop.annotations.JsType;

@JsType
public class PanmirrorUI
{    
   public PanmirrorUI(PanmirrorUIContext context, 
                      PanmirrorUIDisplay display,
                      PanmirrorUIExecute execute)
   {
      this.context = context;
      this.display = display;
      this.execute = execute;
      this.math = new PanmirrorUIMath();
      this.prefs = new PanmirrorUIPrefs();
      this.dialogs = new PanmirrorDialogs(this.context);
   }
   
   public PanmirrorDialogs dialogs;
   public PanmirrorUIDisplay display;
   public PanmirrorUIExecute execute;
   public PanmirrorUIMath math;
   public PanmirrorUIPrefs prefs;
   public PanmirrorUIContext context;
   public JsObject images;
}
