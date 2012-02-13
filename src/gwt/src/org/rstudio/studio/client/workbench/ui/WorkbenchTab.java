/*
 * WorkbenchTab.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.ui;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.IsWidget;
import org.rstudio.core.client.events.HasEnsureHiddenHandlers;
import org.rstudio.core.client.events.HasEnsureVisibleHandlers;

public interface WorkbenchTab extends IsWidget,
                                      HasEnsureVisibleHandlers,
                                      HasEnsureHiddenHandlers
{
   String getTitle();
   void onBeforeUnselected();
   void onBeforeSelected();
   void onSelected();
   void prefetch(Command continuation);
   boolean isSuppressed();

   boolean closeable();
   void confirmClose(Command onConfirmed);
}
