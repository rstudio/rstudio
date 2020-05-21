/*
 * AceSupport.java
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

package org.rstudio.core.client;

// This class is used primarily to export the Ace support classes we define
// for inter-operation with GWT JsInterop. See ScopeManager.java for an example.
public class AceSupport
{
   public static final native void initialize()
   /*-{
      
      $wnd.AceSupport = {
         ScopeManager : $wnd.require("mode/r_scope_tree").ScopeManager,
         ScopeNode    : $wnd.require("mode/r_scope_tree").ScopeNode
      };
      
   }-*/;
}
