/*
 * EnvironmentObjectsObserver.java
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
package org.rstudio.studio.client.workbench.views.environment.view;

import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.workbench.views.environment.model.RObject;

public interface EnvironmentObjectsObserver
{
   void viewObject(String action, String objectName);
   void setObjectExpanded(String objectName);
   void setObjectCollapsed(String objectName);
   void setPersistedScrollPosition(int scrollPosition);
   void changeContextDepth(int newDepth);
   void setViewDirty();
   boolean getShowInternalFunctions();
   void setShowInternalFunctions(boolean show);
   void fillObjectContents(RObject object, Operation onCompleted);
}
