/*
 * Prefs.java
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
package org.rstudio.studio.client.workbench.prefs.model;

public class ProjectPrefs
{
   public static final int LINEENDINGS_DEFAULT = -1;
   public static final int LINEENDINGS_WINDOWS = 0;
   public static final int LINEENDINGS_POSIX = 1;
   public static final int LINEENDINGS_NATIVE = 2;
   public static final int LINEENDINGS_PASSTHROUGH = 3;
   
   public static final String prefFromLineEndings(int prefValue)
   {
      switch (prefValue)
      {
      case LINEENDINGS_DEFAULT:
         return UserPrefs.LINE_ENDING_CONVERSION_DEFAULT;
      case LINEENDINGS_WINDOWS:
         return UserPrefs.LINE_ENDING_CONVERSION_WINDOWS;
      case LINEENDINGS_POSIX:
         return UserPrefs.LINE_ENDING_CONVERSION_POSIX;
      case LINEENDINGS_NATIVE:
         return UserPrefs.LINE_ENDING_CONVERSION_NATIVE;
      case LINEENDINGS_PASSTHROUGH:
         return UserPrefs.LINE_ENDING_CONVERSION_PASSTHROUGH;
      default:
         return UserPrefs.LINE_ENDING_CONVERSION_DEFAULT;
      }
   }
   
   public static final int lineEndingsFromPref(String pref)
   {
      switch(pref)
      {
      case UserPrefs.LINE_ENDING_CONVERSION_DEFAULT:
         return LINEENDINGS_DEFAULT;
      case UserPrefs.LINE_ENDING_CONVERSION_WINDOWS:
         return LINEENDINGS_WINDOWS;
      case UserPrefs.LINE_ENDING_CONVERSION_POSIX:
         return LINEENDINGS_POSIX;
      case UserPrefs.LINE_ENDING_CONVERSION_NATIVE:
         return LINEENDINGS_NATIVE;
      case UserPrefs.LINE_ENDING_CONVERSION_PASSTHROUGH:
         return LINEENDINGS_PASSTHROUGH;
      }
      return LINEENDINGS_DEFAULT;
   }
}
