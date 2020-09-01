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

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.panmirror.PanmirrorWriterOptions;
import org.rstudio.studio.client.panmirror.PanmirrorWriterReferencesOptions;
import org.rstudio.studio.client.panmirror.format.PanmirrorExtendedDocType;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorPandocFormatConfig;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUITools;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUIToolsAttr;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUIToolsFormat;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.inject.Inject;


public class VisualModeMarkdownWriter
{
   
   public class Options
   {
      public Options(PanmirrorWriterOptions options, boolean wrapChanged)
      {
         this.options = options;
         this.wrapChanged = wrapChanged;
      }
      
      public final PanmirrorWriterOptions options;
      public final boolean wrapChanged;
      
   }
   
   public VisualModeMarkdownWriter(DocUpdateSentinel docUpdateSentinel, VisualModePanmirrorFormat format)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      docUpdateSentinel_ = docUpdateSentinel;
      format_ = format;
   }
   
   @Inject
   void initialize(WorkbenchContext workbenchContext, UserPrefs prefs)
   {
      workbenchContext_ = workbenchContext;
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
      
      // determine prefs based on whether this file is a project file
      String wrapPref;
      Integer wrapAtColumnPref;
      String referencesLocationPref;
      if (VisualModeUtil.isDocInProject(workbenchContext_, docUpdateSentinel_))
      {
         wrapPref = prefs_.visualMarkdownEditingWrap().getValue();
         wrapAtColumnPref = prefs_.visualMarkdownEditingWrapAtColumn().getValue();
         referencesLocationPref = prefs_.visualMarkdownEditingReferencesLocation().getValue();
      }
      else
      {
         wrapPref = prefs_.visualMarkdownEditingWrap().getGlobalValue();
         wrapAtColumnPref = prefs_.visualMarkdownEditingWrapAtColumn().getGlobalValue();
         referencesLocationPref = prefs_.visualMarkdownEditingReferencesLocation().getGlobalValue();
      }     
          
      if (wrapPref.equals(UserPrefsAccessor.VISUAL_MARKDOWN_EDITING_WRAP_COLUMN))
         options.wrap = wrapAtColumnPref.toString();
      else
         options.wrap = wrapPref;
         
      // use user pref for references location
      PanmirrorWriterReferencesOptions references = new PanmirrorWriterReferencesOptions();
      references.location = referencesLocationPref;
      options.references = references;
            
      // layer in format config
      if (formatConfig.wrap != null)
      {
         if (formatConfig.wrap.equals(PanmirrorWriterOptions.kWrapNone) || 
             formatConfig.wrap.equals(PanmirrorWriterOptions.kWrapSentence))
         {
            options.wrap = formatConfig.wrap;
         }
         else
         {
            int column = StringUtil.parseInt(formatConfig.wrap, 0);
            if (column > 0)
               options.wrap = Integer.toString(column);
            else
               options.wrap = PanmirrorWriterOptions.kWrapNone;
         }
      }
      
      if (formatConfig.references_location != null)
         options.references.location = formatConfig.references_location;
      if (formatConfig.references_prefix != null)
         options.references.prefix = formatConfig.references_prefix;
      
      // if the config doesn't have a references_prefix then provide one for
      // bookdown documents(otherwise there will be duplicate footnotes)
      if (options.references.prefix == null && 
          (format_.isBookdownProjectDocument() || 
           PanmirrorPandocFormatConfig.isDoctype(formatConfig, PanmirrorExtendedDocType.bookdown)
          )
         )
      {
         String docPath = docUpdateSentinel_.getPath();
         if (docPath != null)
         {
            String filename = FileSystemItem.createFile(docPath).getStem();
            PanmirrorUIToolsAttr attr = new PanmirrorUITools().attr;
            options.references.prefix = attr.pandocAutoIdentifier(filename) + "-";
         }
      }
      
      // check if this represents a line wrapping change
      boolean wrapChanged = lastUsedWriterOptions_ != null &&
                            !lastUsedWriterOptions_.wrap.equals(options.wrap);
      
      // set last used
      lastUsedWriterOptions_ = options;
      
      // return context
      return new Options(options, wrapChanged);
   }

   
   private PanmirrorWriterOptions lastUsedWriterOptions_ = null;
   private WorkbenchContext workbenchContext_;
   private UserPrefs prefs_;
   private final VisualModePanmirrorFormat format_;
   private final DocUpdateSentinel docUpdateSentinel_;
   
}
