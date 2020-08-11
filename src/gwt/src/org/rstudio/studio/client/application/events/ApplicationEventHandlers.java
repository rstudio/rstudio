/*
 * ApplicationEventHandlers.java
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
package org.rstudio.studio.client.application.events;

import org.rstudio.studio.client.workbench.events.SessionInitEvent;

public interface ApplicationEventHandlers extends LogoutRequestedEvent.Handler,
                                                  UnauthorizedEvent.Handler,
                                                  ReloadEvent.Handler,
                                                  ReloadWithLastChanceSaveEvent.Handler,
                                                  QuitEvent.Handler,
                                                  SwitchToRVersionEvent.Handler,
                                                  SuicideEvent.Handler,
                                                  SessionAbendWarningEvent.Handler,
                                                  SessionSerializationEvent.Handler,
                                                  SessionRelaunchEvent.Handler,
                                                  ServerUnavailableEvent.Handler,
                                                  ClientDisconnectedEvent.Handler,
                                                  InvalidClientVersionEvent.Handler,
                                                  ServerOfflineEvent.Handler,
                                                  InvalidSessionEvent.Handler,
                                                  SessionInitEvent.Handler,
                                                  RestartStatusEvent.Handler,
                                                  FileUploadEvent.Handler,
                                                  AriaLiveStatusEvent.Handler
{
}
