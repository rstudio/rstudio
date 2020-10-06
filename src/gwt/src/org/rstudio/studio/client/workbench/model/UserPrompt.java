/*
 * UserPrompt.java
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

package org.rstudio.studio.client.workbench.model;

import com.google.gwt.core.client.JavaScriptObject;

public class UserPrompt extends JavaScriptObject
{
   // NOTE: these mirror the same constants in MessageDialog
   public final static int INFO = 1;
   public final static int WARNING = 2;
   public final static int ERROR = 3;
   public final static int QUESTION = 4;
   
   public final static int RESPONSE_YES = 0;
   public final static int RESPONSE_NO = 1;
   public final static int RESPONSE_CANCEL = 2;
   
   protected UserPrompt()
   {
   }
   
   public final native int getType() /*-{
      return this.type;
   }-*/;
   
   public final native String getCaption() /*-{
      return this.caption;
   }-*/;
   
   public final native String getMessage() /*-{
      return this.message;
   }-*/;
   
   public final native String getYesLabel() /*-{
      return this.yesLabel;
   }-*/;

   public final native String getNoLabel() /*-{
      return this.noLabel;
   }-*/;
   
   public final native boolean getYesIsDefault() /*-{
      return this.yesIsDefault;
   }-*/;

   public final native boolean getIncludeCancel() /*-{
      return this.includeCancel;
   }-*/;
}
