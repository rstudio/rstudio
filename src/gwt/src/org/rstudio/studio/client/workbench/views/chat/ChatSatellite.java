/*
 * ChatSatellite.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.chat;

import org.rstudio.studio.client.application.ApplicationUncaughtExceptionHandler;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteApplication;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceThemes;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class ChatSatellite extends SatelliteApplication
{
   public static final String NAME = "chat";

   @Inject
   public ChatSatellite(ChatSatelliteView view,
                         Satellite satellite,
                         Provider<AceThemes> pAceThemes,
                         Provider<UserPrefs> pUserPrefs,
                         ApplicationUncaughtExceptionHandler exHandler,
                         Commands commands)
   {
      super(NAME, view, satellite, pAceThemes, pUserPrefs, exHandler, commands);
   }
}
