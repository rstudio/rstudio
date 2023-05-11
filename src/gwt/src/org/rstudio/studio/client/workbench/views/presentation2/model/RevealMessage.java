/*
 * PresentationMessage.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.workbench.views.presentation2.model;

import com.google.gwt.core.client.JavaScriptObject;


public class RevealMessage extends JavaScriptObject
{
   protected RevealMessage() {}

   public static native RevealMessage create(String message, JavaScriptObject data) /*-{
      return {message: message, data: data};
   }-*/;

   public native final String getMessage() /*-{
      return this.message;
   }-*/;

   public native final JavaScriptObject getData() /*-{
      return this.data;
   }-*/;
}
