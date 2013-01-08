/*
 * ShellInput.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.common.shell;

import com.google.gwt.core.client.JavaScriptObject;

public class ShellInput extends JavaScriptObject
{
   protected ShellInput()
   {
   }
   
   public native static ShellInput create(String text, boolean echoInput) /*-{
      return {
         interrupt: false,
         text: text,
         echo_input: echoInput
      };
   }-*/;
   
   public native static ShellInput createInterrupt() /*-{
      return {
         interrupt: true,
         text: "",
         echo_input: true
      };
   }-*/;
   
   
   
   public native final boolean getInterrupt() /*-{
      return this.interrupt;
   }-*/;

   public native final String getText() /*-{
      return this.text;
   }-*/;
   
   public native final boolean getEchoInput() /*-{
      return this.echo_input;
   }-*/;
   
}
