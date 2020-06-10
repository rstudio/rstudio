/*
 * CollabEditor.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.studio.client.workbench.views.source.events.CollabEditStartParams;
import org.rstudio.studio.client.workbench.views.source.model.DirtyState;

import com.google.gwt.user.client.Command;

public class CollabEditor
{
   void beginCollabSession(AceEditor editor, CollabEditStartParams params, 
         DirtyState dirtyState, Command onCompleted)
   {
      onCompleted.execute();
   }
   
   boolean hasActiveCollabSession(AceEditor editor)
   {
      return false;
   }
   
   boolean hasFollowingCollabSession(AceEditor editor)
   {
      return false;
   }
   
   void endCollabSession(AceEditor editor)
   {
      
   }
}
