/*
 * TextEditingTargetCompilePdfHelper.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.inject.Inject;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.tex.TexMagicComment;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.latex.LatexProgramRegistry;
import org.rstudio.studio.client.common.rnw.RnwWeave;
import org.rstudio.studio.client.common.rnw.RnwWeaveDirective;
import org.rstudio.studio.client.common.rnw.RnwWeaveRegistry;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.model.TexCapabilities;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorPosition;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.source.model.RnwChunkOptions;
import org.rstudio.studio.client.workbench.views.source.model.TexServerOperations;

import java.util.ArrayList;
import java.util.HashMap;

public class TextEditingTargetCompilePdfHelper
      implements RnwChunkOptions.AsyncProvider
{ 
   public TextEditingTargetCompilePdfHelper(DocDisplay docDisplay)
   {
      docDisplay_ = docDisplay;
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   public void initialize(UIPrefs prefs,
                          Session session,
                          TexServerOperations server,
                          RnwWeaveRegistry rnwWeaveRegistry,
                          LatexProgramRegistry latexProgramRegistry)
   {
      prefs_ = prefs;
      session_ = session;
      server_ = server;
      rnwWeaveRegistry_ = rnwWeaveRegistry;
      latexProgramRegistry_ = latexProgramRegistry;
   }
   
   // get the chunk options which apply to the current document. when 
   // the chunk options are ready the callback command is execute (note
   // that if there are no chunk options available then execute will 
   // never be called). this method caches the results from the server
   // so that chunk options are only retreived once per session -- we 
   // do this not only to save the round-trip but also because knitr takes
   // over 500ms to load and it may need to be loaded to serve the
   // request for chunk options
   public void getChunkOptions(
                   final ServerRequestCallback<RnwChunkOptions> requestCallback)
   {
      // determine the current rnw weave type
      final RnwWeave rnwWeave = getActiveRnwWeave();
      if (rnwWeave == null)
         return;
      
      // look it up in the cache
      if (chunkOptionsCache_.containsKey(rnwWeave.getName()))
      {
         requestCallback.onResponseReceived(
                                    chunkOptionsCache_.get(rnwWeave.getName()));
      }
      else
      {
         server_.getChunkOptions(
            rnwWeave.getName(), 
            new ServerRequestCallback<RnwChunkOptions>() {
               @Override
               public void onResponseReceived(RnwChunkOptions options)
               {
                  chunkOptionsCache_.put(rnwWeave.getName(), options);
                  requestCallback.onResponseReceived(options);
               }

               @Override
               public void onError(ServerError error)
               {
                  requestCallback.onError(error);
               }
            });
      }
   }
   
   public void ensureRnwConcordance()
   {
      RnwWeave rnwWeave = getActiveRnwWeave();
      if ( (rnwWeave != null) && rnwWeave.getInjectConcordance())
      {
         if (!hasConcordanceDirective(docDisplay_.getCode()))
         {    
            InputEditorSelection doc = docDisplay_.search(
                                          "\\\\begin{document}",
                                          false,   // backwards
                                          true,    // wrap
                                          false,   // case sensitive
                                          false,   // whole word
                                          true,    // from selection
                                          false,   // selection only
                                          true);   // regexp mode
            if (doc != null)
            {  
               InputEditorPosition pos = doc.getEnd().moveToNextLine();
               docDisplay_.insertCode(pos, "\\SweaveOpts{concordance=TRUE}\n");
            }
         }
      }
   }
   
   
   public void checkCompilers(final WarningBarDisplay display, 
                              TextFileType fileType)
   {
      // for all tex files we need to parse magic comments and validate
      // any explict latex proram directive
      ArrayList<TexMagicComment> magicComments = null;
      if (fileType.canCompilePDF())
      {
         magicComments = TexMagicComment.parseComments(docDisplay_.getCode());
         String latexProgramDirective = 
                           detectLatexProgramDirective(magicComments);
           
         if (latexProgramDirective != null)
         {
            if (latexProgramRegistry_.findTypeIgnoreCase(latexProgramDirective)
                  == null)
            {
               // show warning and bail 
               display.showWarningBar(
                  "Unknown LaTeX program type '" + latexProgramDirective + 
                  "' specified (valid types are " + 
                  latexProgramRegistry_.getPrintableTypeNames() +  ")");
               
               return;
            }
         }
      }
   
      // for Rnw we first determine the RnwWeave type
      RnwWeave rnwWeave = null;
      RnwWeaveDirective rnwWeaveDirective = null;
      if (fileType.isRnw())
      {
         rnwWeaveDirective = detectRnwWeaveDirective(magicComments);
         if (rnwWeaveDirective != null) 
         {
            rnwWeave = rnwWeaveDirective.getRnwWeave();
            if (rnwWeave == null)
            {
               // show warning and bail 
               display.showWarningBar(
                  "Unknown Rnw weave method '" + rnwWeaveDirective.getName() + 
                  "' specified (valid types are " + 
                  rnwWeaveRegistry_.getPrintableTypeNames() +  ")");
               
               return;
            }    
         }
         else
         {
            rnwWeave = rnwWeaveRegistry_.findTypeIgnoreCase(
                                    prefs_.defaultSweaveEngine().getValue());
         }     
      }
            
       
      final SessionInfo sessionInfo = session_.getSessionInfo();
      TexCapabilities texCap = sessionInfo.getTexCapabilities();

      final boolean checkForTeX = fileType.canCompilePDF() && 
                                  !texCap.isTexInstalled();
      
      final boolean checkForRnwWeave = (rnwWeave != null) && 
                                       !texCap.isRnwWeaveAvailable(rnwWeave);
                                 
      if (checkForTeX || checkForRnwWeave)
      {
         // alias variables to finals
         final boolean hasRnwWeaveDirective = rnwWeaveDirective != null;
         final RnwWeave fRnwWeave = rnwWeave;
         
         server_.getTexCapabilities(new ServerRequestCallback<TexCapabilities>()
         {
            @Override
            public void onResponseReceived(TexCapabilities response)
            {
               if (checkForTeX && !response.isTexInstalled())
               {
                  String warning;
                  if (Desktop.isDesktop())
                     warning = "No TeX installation detected. Please install " +
                               "TeX before compiling.";
                  else
                     warning = "This server does not have TeX installed. You " +
                               "may not be able to compile.";
                  display.showWarningBar(warning);
               }
               else if (checkForRnwWeave && 
                        !response.isRnwWeaveAvailable(fRnwWeave))
               {
                  String forContext = "";
                  if (hasRnwWeaveDirective)
                     forContext = "this file";
                  else if (sessionInfo.getActiveProjectFile() != null)
                     forContext = "Rnw files for this project"; 
                  else
                     forContext = "Rnw files";
                  
                  display.showWarningBar(
                     fRnwWeave.getName() + " is configured to weave " + 
                     forContext + " " + "however the " + 
                     fRnwWeave.getPackageName() + " package is not installed.");
               }
               else
               {
                  display.hideWarningBar();
               }
            }

            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
            }
         });
      }
      else
      {
         display.hideWarningBar();
      }
   }
   
   private boolean hasConcordanceDirective(String code)
   {
      Iterable<String> lines = StringUtil.getLineIterator(code);
      
      for (String line : lines)
      {
         line = line.trim();
         if (line.length() == 0)
         {
            continue;
         }
         else if (line.startsWith("\\SweaveOpts"))
         {
            Match match = concordancePattern_.match(line, 0);
            if (match != null)
               return true;
         }
      }
    
      return false;  
   }
    
   // get the currently active rnw weave method -- note this can return
   // null in the case that there is an embedded directive which is invalid
   private RnwWeave getActiveRnwWeave()
   {
      RnwWeaveDirective rnwWeaveDirective = detectRnwWeaveDirective(
                         TexMagicComment.parseComments(docDisplay_.getCode()));
      if (rnwWeaveDirective != null)
         return rnwWeaveDirective.getRnwWeave();
      else
         return rnwWeaveRegistry_.findTypeIgnoreCase(
                                    prefs_.defaultSweaveEngine().getValue());
   }
   
   private RnwWeaveDirective detectRnwWeaveDirective(
                                    ArrayList<TexMagicComment> magicComments)
   {
      for (TexMagicComment comment : magicComments)
      {
         RnwWeaveDirective rnwWeaveDirective = 
                           RnwWeaveDirective.fromTexMagicComment(comment);
         if (rnwWeaveDirective != null)
            return rnwWeaveDirective;
      }
      
      return null;
   }
   
   private String detectLatexProgramDirective(
                     ArrayList<TexMagicComment> magicComments)
   {
      for (TexMagicComment comment : magicComments)
      {
         if (comment.getScope().equalsIgnoreCase("tex") &&
             (comment.getVariable().equalsIgnoreCase("program") ||
              comment.getVariable().equalsIgnoreCase("ts-program")))
         { 
            return comment.getValue();
         }
      }
      
      return null;
   }
   
   private final DocDisplay docDisplay_;
   
   private UIPrefs prefs_;
   private Session session_;
   private TexServerOperations server_;
   private RnwWeaveRegistry rnwWeaveRegistry_;
   private LatexProgramRegistry latexProgramRegistry_;
   
   private static final Pattern concordancePattern_ = Pattern.create(
                     "\\\\[\\s]*SweaveOpts[\\s]*{.*concordance[\\s]*=.*}");
   
   private static HashMap<String, RnwChunkOptions> chunkOptionsCache_ = 
                                    new HashMap<String, RnwChunkOptions>();
}
