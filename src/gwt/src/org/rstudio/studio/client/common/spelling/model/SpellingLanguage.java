/*
 * SpellingLanguage.java
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
package org.rstudio.studio.client.common.spelling.model;

import com.google.gwt.core.client.JavaScriptObject;

public class SpellingLanguage extends JavaScriptObject
{
   protected SpellingLanguage()
   {
   }
   
   public native final String getId() /*-{
      return this.id;
   }-*/;

   public native final String getName() /*-{
      return this.name;
   }-*/;
}
