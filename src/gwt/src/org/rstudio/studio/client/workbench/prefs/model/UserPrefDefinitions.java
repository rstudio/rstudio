/*
 * UserPrefDefinitions.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.prefs.model;

import org.rstudio.core.client.js.JsObject;

import com.google.gwt.core.client.JsArrayString;

public class UserPrefDefinitions extends JsObject
{
   protected UserPrefDefinitions()
   {
   }

   /**
    * Creates a new, empty set of preference definitions.
    * 
    * @return An empty set of preference definitions.
    */
   public final static UserPrefDefinitions createEmpty()
   {
      return JsObject.createJsObject().cast();
   }

   /**
    * Gets a definition of a single preference.
    * 
    * @param pref The ID of the preference.
    * @return The definition of the preference, or null if the preference is not
    *   defined.
    */
   public final UserPrefDefinition getDefinition(String pref)
   {
      if (hasKey(pref))
      {
         return getObject(pref).cast();
      }
      else
         return null;
   }
   
   /**
    * Get a list of all the preference names.
    * 
    * @return All the preference names.
    */
   public final JsArrayString getPrefNames()
   {
      return keys();
   }
}
