/*
 * LineEndingsSelectWidget.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.prefs.views;

import java.util.ArrayList;

import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.workbench.prefs.model.EditingPrefs;

public class LineEndingsSelectWidget extends SelectWidget
{
   public LineEndingsSelectWidget()
   {
      this(false);
   }
   
   public LineEndingsSelectWidget(boolean includeDefault)
   {
      super("Line ending conversion:",
            getLineEndingsCaptions(includeDefault),
            getLineEndingsValues(includeDefault),
            false, 
            true, 
            false);
   }

   private static String[] getLineEndingsCaptions(boolean includeDefault)
   {
      ArrayList<String> captions = new ArrayList<String>();
      if (includeDefault)
         captions.add("(Use Default)");
      captions.add("None");
      captions.add("Platform Native");
      captions.add("Posix (LF)");
      captions.add("Windows (CR/LF)");
      
      return captions.toArray(new String[0]);
   }
   
   private static String[] getLineEndingsValues(boolean includeDefault)
   {
      ArrayList<String> values = new ArrayList<String>();
      if (includeDefault)
         values.add("-1");
      values.add(Integer.toString(EditingPrefs.LINEENDINGS_PASSTHROUGH));
      values.add(Integer.toString(EditingPrefs.LINEENDINGS_NATIVE));
      values.add(Integer.toString(EditingPrefs.LINEENDINGS_POSIX));
      values.add(Integer.toString(EditingPrefs.LINEENDINGS_WINDOWS));
      
      return values.toArray(new String[0]);
   }
   
}
