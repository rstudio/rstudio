/*
 * VisualModeWriterOptions.java
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


package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.panmirror.PanmirrorWriterOptions;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorPandocFormatConfig;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUITools;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUIToolsFormat;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

import com.google.inject.Inject;

public class VisualModeMarkdownWriter
{
   
   public class Options
   {
      public Options(PanmirrorWriterOptions options, boolean wrapColumnChanged)
      {
         this.options = options;
         this.wrapColumnChanged = wrapColumnChanged;
      }
      
      public final PanmirrorWriterOptions options;
      public final boolean wrapColumnChanged;
      
   }
   
   public VisualModeMarkdownWriter()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(UserPrefs prefs)
   {
      prefs_ = prefs;
   }

   
   public Options optionsFromCode(String code)
   {
      PanmirrorUIToolsFormat format = new PanmirrorUITools().format;
      PanmirrorPandocFormatConfig formatConfig = format.parseFormatConfig(code, true);
      return optionsFromConfig(formatConfig); 
   }
   
   public Options optionsFromConfig(PanmirrorPandocFormatConfig formatConfig)
   {
      // options defaults from preferences
      PanmirrorWriterOptions options = new PanmirrorWriterOptions();
      
      // always write atx headers (e.g. ##)
      options.atxHeaders = true;
      
      // use user pref for wrapColumn
      if (prefs_.visualMarkdownEditingWrapAuto().getValue())
         options.wrapColumn = prefs_.visualMarkdownEditingWrapColumn().getValue();
      else
         options.wrapColumn = 0;
      
      // use user pref for references location
      options.references = prefs_.visualMarkdownEditingReferencesLocation().getValue();
      
      // layer in format config
      if (formatConfig.wrapColumn > 0)
         options.wrapColumn = formatConfig.wrapColumn;
      if (formatConfig.references != null)
         options.references = formatConfig.references;
      
      // check if this represents a line wrapping change
      boolean wrapColumnChanged = lastUsedWriterOptions_ != null &&
                                  lastUsedWriterOptions_.wrapColumn != options.wrapColumn;
      
      // set last used
      lastUsedWriterOptions_ = options;
      
      // return context
      return new Options(options, wrapColumnChanged);
   }

   
   private PanmirrorWriterOptions lastUsedWriterOptions_ = null;
   private UserPrefs prefs_;
   
}
