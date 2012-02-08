/*
 * CompilePdfDependencyChecker.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.tex.TexMagicComment;
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
import org.rstudio.studio.client.workbench.views.source.model.TexServerOperations;

import com.google.inject.Inject;

public class CompilePdfDependencyChecker
{
   public interface Display
   {
      void showWarningBar(String message);
      void hideWarningBar();
   }
   
   
   @Inject
   public CompilePdfDependencyChecker(UIPrefs prefs,
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
   
   
   public void check(final Display display, TextFileType fileType, String code)
   {
      // for all tex files we need to parse magic comments and validate
      // any explict latex proram directive
      ArrayList<TexMagicComment> magicComments = null;
      if (fileType.canCompilePDF())
      {
         magicComments = TexMagicComment.parseComments(code);
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
   
   
   private final UIPrefs prefs_;
   private final Session session_;
   private final TexServerOperations server_;
   private final RnwWeaveRegistry rnwWeaveRegistry_;
   private final LatexProgramRegistry latexProgramRegistry_;
}
