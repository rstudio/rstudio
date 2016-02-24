/*
 * RVersionSelectWidget.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

package org.rstudio.studio.client.application.ui;

import java.util.ArrayList;

import org.rstudio.core.client.widget.HelpButton;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.application.model.RVersionSpec;

import com.google.gwt.core.client.JsArray;

public class RVersionSelectWidget extends SelectWidget
{
   public RVersionSelectWidget(JsArray<RVersionSpec> rVersions)
   {
      this("Default version of R:", rVersions, true, true, false);
   }
   
   public RVersionSelectWidget(String caption,
                               JsArray<RVersionSpec> rVersions,
                               boolean includeSystemDefault,
                               boolean includeHelpButton,
                               boolean fillContainer)
   {
      super(caption,
            rVersionChoices(rVersions, includeSystemDefault),
            rVersionValues(rVersions, includeSystemDefault),
            false, 
            true, 
            false,
            fillContainer);
      if (includeHelpButton)
         HelpButton.addHelpButton(this, "multiple_r_versions");
   }
   
   public void setRVersion(RVersionSpec version)
   {
      if (!setValue(rVersionSpecToString(version)))
         setValue(rVersionSpecToString(RVersionSpec.createEmpty()));
   }
   
   public RVersionSpec getRVersion()
   {
      return rVersionSpecFromString(getValue());
   }
   
   
   private static String[] rVersionChoices(JsArray<RVersionSpec> rVersions,
                                           boolean includeSystemDefault)
   {
      // do we need to disambiguate identical version numbers
      boolean disambiguate = RVersionSpec.hasDuplicates(rVersions);

      // build list of choices
      ArrayList<String> choices = new ArrayList<String>();

      // include "default" label if requested
      if (includeSystemDefault)
         choices.add(USE_DEFAULT_VERSION);

      for (int i=0; i<rVersions.length(); i++)
      {
         RVersionSpec version = rVersions.get(i);
         String choice = "R version " + version.getVersion();
         if (disambiguate)
            choice = choice + " (" + version.getRHome() + ")";
         choices.add(choice);
      }

      return choices.toArray(new String[0]);
   }

   private static String[] rVersionValues(JsArray<RVersionSpec> rVersions,
                                          boolean includeSystemDefault)
   {
      ArrayList<String> values = new ArrayList<String>();

      if (includeSystemDefault)
         values.add(rVersionSpecToString(RVersionSpec.createEmpty()));

      for (int i=0; i<rVersions.length(); i++)
         values.add(rVersionSpecToString(rVersions.get(i)));

      return values.toArray(new String[0]);
   }
   
   private static RVersionSpec rVersionSpecFromString(String str)
   {
      if (str != null)
      {
         int loc = str.indexOf(SEP);
         if (loc != -1)
         {
            String version = str.substring(0, loc);
            String rHomeDir = str.substring(loc + SEP.length());
            if (version.length() > 0 && rHomeDir.length() > 0)
               return RVersionSpec.create(version, rHomeDir);
         }
      }
      
      // couldn't parse it
      return RVersionSpec.createEmpty();
   }
   
   private static String rVersionSpecToString(RVersionSpec version)
   {
      if (version.getVersion().length() == 0)
         return "";
      else
         return version.getVersion() + SEP + version.getRHome();
   }

   private final static String USE_DEFAULT_VERSION = "(Use System Default)";
   private final static String SEP = "::::";
}