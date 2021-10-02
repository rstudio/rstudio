/*
 * PresentationEditorSync.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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

package org.rstudio.studio.client.common.presentation2;

import org.rstudio.studio.client.common.presentation2.model.PresentationEditorLocation;

public class PresentationEditorSync
{
   
   // compute the preview slide index from the current editor state and
   // (explicit or implicit) pandoc slide level
   public static int slideIndexForLocation(PresentationEditorLocation location, int slideLevel)
   {
      // TODO: implement 
      
      return 0;
   }

   
   // re-compute the editor state with the cursor location implied by 
   // the passed slide index
   public static PresentationEditorLocation locationForSlideIndex(
      int slideIndex, PresentationEditorLocation location, int slideLevel)
   {
      
      // TODO: implement
      
      return location;
   }
   
}
