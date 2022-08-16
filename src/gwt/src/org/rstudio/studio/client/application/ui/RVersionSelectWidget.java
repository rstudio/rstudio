/*
 * RVersionSelectWidget.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.application.ui;

import java.util.ArrayList;

import com.google.gwt.core.client.GWT;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.HelpButton;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.application.StudioClientApplicationConstants;
import org.rstudio.studio.client.application.model.RVersionSpec;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

public class RVersionSelectWidget extends SelectWidget
{
   public RVersionSelectWidget(ElementIds.SelectWidgetId uniqueId, JsArray<RVersionSpec> rVersions)
   {
      this(constants_.defaultVersionRCaption(), uniqueId, rVersions, true, true, false);
   }
   
   public RVersionSelectWidget(String caption,
                               ElementIds.SelectWidgetId uniqueId,
                               JsArray<RVersionSpec> rVersions,
                               boolean includeSystemDefault,
                               boolean includeHelpButton,
                               boolean fillContainer)
   {
      this(caption, uniqueId, rVersions, includeSystemDefault, includeHelpButton, fillContainer, false);
   }

   public RVersionSelectWidget(String caption,
                               ElementIds.SelectWidgetId uniqueId,
                               JsArray<RVersionSpec> rVersions,
                               boolean includeSystemDefault,
                               boolean includeHelpButton,
                               boolean fillContainer,
                               boolean includeUserSpecified)
   {
      super(caption,
            uniqueId,
            rVersionChoices(rVersions, includeSystemDefault, includeUserSpecified),
            rVersionValues(rVersions, includeSystemDefault, includeUserSpecified),
            false, 
            true, 
            false,
            fillContainer);
      rVersions_ = rVersions;
      includeSystemDefault_ = includeSystemDefault;
      includeUserSpecified_ = includeUserSpecified;
      if (includeHelpButton)
         HelpButton.addHelpButton(this, "multiple_r_versions", constants_.helpOnRVersionsTitle());
   }

   public void setIncludeUserSpecified(boolean includeUserSpecified) {
      includeUserSpecified_ = includeUserSpecified;
      updateChoices();
   }

   public void setIncludeSystemDefault(boolean includeSystemDefault) {
      includeSystemDefault_= includeSystemDefault;
      updateChoices();
   }

   public void setRVersions(JsArray<RVersionSpec> rVersions) {
      rVersions_ = rVersions;
      updateChoices();
   }

   private void updateChoices() {
      setChoices(
         rVersionChoices(rVersions_, includeSystemDefault_, includeUserSpecified_),
         rVersionValues(rVersions_, includeSystemDefault_, includeUserSpecified_)
      );
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
   
   public boolean isSystemDefault() {
      return getValue().isEmpty();
   }

   public boolean isUserSpecified() {
      return getValue() == USER_SPECIFIED;
   }
   
   private static String[] rVersionChoices(JsArray<RVersionSpec> rVersions,
                                           boolean includeSystemDefault,
                                           boolean includeUserSpecified)
   {
      // do we need to disambiguate identical version numbers
      boolean disambiguate = RVersionSpec.hasDuplicates(rVersions);

      // build list of choices
      ArrayList<String> choices = new ArrayList<>();

      // include "default" label if requested
      if (includeSystemDefault)
         choices.add(USE_DEFAULT_VERSION);

      for (int i=0; i<rVersions.length(); i++)
      {
         RVersionSpec version = rVersions.get(i);

         // Modules contain the system default as the version number, so ignore these
         boolean module = !StringUtil.isNullOrEmpty(version.getModule());
         StringBuilder choice = new StringBuilder();

         if (module)
            choice.append(constants_.moduleText() + version.getModule());
         else
            choice.append(constants_.rVersionText() + version.getVersion());
         if (disambiguate)
            choice.append(" (" + version.getRHome() + ")");
         if (!version.getLabel().isEmpty())
           choice.append(" (" + version.getLabel() + ")");
         choices.add(choice.toString());
      }

      if (includeUserSpecified)
         choices.add(USER_SPECIFIED);

      return choices.toArray(new String[0]);
   }

   private static String[] rVersionValues(JsArray<RVersionSpec> rVersions,
                                          boolean includeSystemDefault,
                                          boolean includeUserSpecified)
   {
      ArrayList<String> values = new ArrayList<>();

      if (includeSystemDefault)
         values.add(rVersionSpecToString(RVersionSpec.createEmpty()));

      for (int i=0; i<rVersions.length(); i++)
         values.add(rVersionSpecToString(rVersions.get(i)));

      // this should not pass conversion to rVersionSpec,
      // and should safely retrun an empty rVersionSpec
      if (includeUserSpecified)
         values.add(USER_SPECIFIED);

      return values.toArray(new String[0]);
   }
   
   private static RVersionSpec rVersionSpecFromString(String str)
   {
      if (str != null)
      {
         JsArrayString values = StringUtil.split(str, SEP);
         if (values.length() >= 3)
         {
            String version = values.get(0);
            String rHomeDir = values.get(1);
            String label = values.get(2);
            String module = values.get(3);
            if (version.length() > 0 && rHomeDir.length() > 0)
               return RVersionSpec.create(version, rHomeDir, label, module);
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
         return version.getVersion() + SEP + version.getRHome() + SEP + version.getLabel() + SEP + version.getModule();
   }
   private static final StudioClientApplicationConstants constants_ = GWT.create(StudioClientApplicationConstants.class);
   private final static String USE_DEFAULT_VERSION = constants_.useSystemDefaultText();
   private final static String USER_SPECIFIED = constants_.userSpecifiedText();
   private final static String SEP = "::::";

   private JsArray<RVersionSpec> rVersions_;
   private boolean includeUserSpecified_ = false;
   private boolean includeSystemDefault_ = false;
}
