/*
 * ChatTab.java
 *
 * Copyright (C) 2025 by Posit Software, PBC
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.chat;

import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;

import com.google.inject.Inject;

public class ChatTab extends DelayLoadWorkbenchTab<ChatPresenter>
{
   public abstract static class Shim extends DelayLoadTabShim<ChatPresenter, ChatTab>
   {
   }

   @Inject
   public ChatTab(Shim shim)
   {
      super(constants_.chatTitle(), shim);
      shim_ = shim;
   }

   @SuppressWarnings("unused") private final Shim shim_;
   private static final ChatConstants constants_ = com.google.gwt.core.client.GWT.create(ChatConstants.class);
}
