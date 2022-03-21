/*
 * LineEndingsSelectWidget.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.projects.StudioClientProjectConstants;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import java.util.ArrayList;

public class UseNativePipeOperatorSelectWidget extends SelectWidget
{
   public UseNativePipeOperatorSelectWidget()
   {
      super(constants_.useNativePipeOperatorLabel(),
            getNativePipeOperatorCaptions(),
            getNativePipeOperatorValues(),
            false, 
            true, 
            false);
   }

   private static String[] getNativePipeOperatorCaptions()
   {
      ArrayList<String> captions = new ArrayList<>();
      captions.add(constants_.useNativePipeOperatorNeverText());
      captions.add(constants_.useNativePipeOperatorR41Text());
      captions.add(constants_.useNativePipeOperatorAlwaysText());
      
      return captions.toArray(new String[0]);
   }
   
   private static String[] getNativePipeOperatorValues()
   {
      ArrayList<String> values = new ArrayList<>();
      values.add(UserPrefs.USE_NATIVE_PIPE_OPERATOR_NEVER);
      values.add(UserPrefs.USE_NATIVE_PIPE_OPERATOR_ONLY_R41);
      values.add(UserPrefs.USE_NATIVE_PIPE_OPERATOR_ALWAYS);
      
      return values.toArray(new String[0]);
   }
   private static final StudioClientProjectConstants constants_ = GWT.create(StudioClientProjectConstants.class);
}
