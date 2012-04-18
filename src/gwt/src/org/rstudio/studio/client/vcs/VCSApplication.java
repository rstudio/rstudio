/*
 * VCSApplication.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.vcs;

import org.rstudio.studio.client.application.ApplicationUncaughtExceptionHandler;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteApplication;
import org.rstudio.studio.client.common.vcs.AskPassManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceThemes;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class VCSApplication extends SatelliteApplication
{
   @Inject
   public VCSApplication(VCSApplicationView view,
                         Satellite satellite,
                         Provider<AceThemes> pAceThemes,
                         ApplicationUncaughtExceptionHandler uncaughtExHandler,
                         AskPassManager askPassManager) // force gin to create
   {
      super("review_changes", view, satellite, pAceThemes, uncaughtExHandler);
   }
}
