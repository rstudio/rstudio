/*
 * TutorialServer.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.tutorial;

import org.rstudio.studio.client.server.Void;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.server.ServerRequestCallback;

public interface TutorialServerOperations
{
   void tutorialStarted(String tutorialName,
                        String tutorialPackage,
                        String tutorialUrl,
                        ServerRequestCallback<Void> requestCallback);
   
   void tutorialStop(String tutorialUrl,
                     ServerRequestCallback<Void> requestCallback);
   
   void tutorialMetadata(String tutorialUrl,
                         ServerRequestCallback<JsObject> requestCallback);
   
   void isPackageInstalled(String packageName,
                           String version,
                           ServerRequestCallback<Boolean> requestCallback);
   
}
