/*
 * UserPrefsSubset.java
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
package org.rstudio.studio.client.workbench.prefs.model;

import com.google.inject.Provider;

/**
 * Used to make it easier to mock subsets of the userPrefs in unit tests without.
 */
public abstract class UserPrefsSubset
{
   public UserPrefsSubset(Provider<UserPrefs> pUserPrefs)
   {
      pUserPrefs_ = pUserPrefs;
   }

   protected UserPrefs getUserPrefs()
   {
      return pUserPrefs_.get();
   }

   private final Provider<UserPrefs> pUserPrefs_;
}
