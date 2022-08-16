/*
 * IndentGuideStyle.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

public class IndentGuideStyle
{
   public final static String INDENT_GUIDES_NONE = "none";
   public final static String INDENT_GUIDES_GRAY = "gray";
   public final static String INDENT_GUIDES_RAINBOWLINES = "rainbowlines";
   public final static String INDENT_GUIDES_RAINBOWFILLS = "rainbowfills";
  
   public static String fromPref(String pref)
   {
      switch (pref)
      {
      case UserPrefs.INDENT_GUIDES_GRAY:
         return INDENT_GUIDES_GRAY;
      case UserPrefs.INDENT_GUIDES_RAINBOWLINES:
         return INDENT_GUIDES_RAINBOWLINES;
      case UserPrefs.INDENT_GUIDES_RAINBOWFILLS:
         return INDENT_GUIDES_RAINBOWFILLS;
      }
      return INDENT_GUIDES_NONE;
   }
}
