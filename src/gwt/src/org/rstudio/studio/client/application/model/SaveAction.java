/*
 * SaveAction.java
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
package org.rstudio.studio.client.application.model;

import com.google.gwt.core.client.JavaScriptObject;

public class SaveAction extends JavaScriptObject
{
   public final static int NOSAVE = 0;
   public final static int SAVE = 1;
   public final static int SAVEASK = -1;
   
   protected SaveAction()
   {  
   }
   
   public native static final SaveAction saveAsk() /*-{
      var saveAction = new Object();
      saveAction.action = -1;
      return saveAction
   }-*/;

   public native final int getAction() /*-{
      return this.action;
   }-*/;
}
