/*
 * VirtualConsolePreferences.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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
package org.rstudio.studio.client.common.console;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.VirtualConsole.Preferences;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

public class VirtualConsolePreferences implements Preferences
{
   @Inject
   public VirtualConsolePreferences(Provider<UIPrefs> pUIPrefs)
   {
      pUIPrefs_ = pUIPrefs; 
   }
   
   @Override
   public int truncateLongLinesInConsoleHistory()
   {
      return pUIPrefs_.get().truncateLongLinesInConsoleHistory().getGlobalValue();
   }
   
   @Override
   public int consoleAnsiMode()
   {
      return pUIPrefs_.get().consoleAnsiMode().getValue();
   }
   
   private final Provider<UIPrefs> pUIPrefs_; 
}
