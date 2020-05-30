/*
 * CreateKeyOptions.java
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
package org.rstudio.studio.client.common.vcs;

import com.google.gwt.core.client.JavaScriptObject;

public class CreateKeyOptions extends JavaScriptObject
{
   protected CreateKeyOptions()
   {
   }
   
   public native static final CreateKeyOptions create(String path,
                                                      String type,
                                                      String passphrase,
                                                      boolean overwrite) /*-{
      var options = new Object();
      options.path = path;
      options.type = type;
      options.passphrase = passphrase;  
      options.overwrite = overwrite;                            
      return options;
   }-*/;
   
   public native final String getPath() /*-{
      return this.path;
   }-*/;

   public native final String getType() /*-{
      return this.type;
   }-*/;
   
   public native final String getPassphrase() /*-{
      return this.passphrase;
   }-*/;   
   
   public native final boolean getOverwrite() /*-{
      return this.overwrite;
   }-*/;   
}
