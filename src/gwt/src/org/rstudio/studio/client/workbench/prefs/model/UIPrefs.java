/*
 * UIPrefs.java
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
package org.rstudio.studio.client.workbench.prefs.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.events.UiPrefsChangedEvent;
import org.rstudio.studio.client.workbench.prefs.events.UiPrefsChangedHandler;
import org.rstudio.studio.client.workbench.views.plots.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.SavePlotAsPdfOptions;

@Singleton
public class UIPrefs extends UIPrefsAccessor implements UiPrefsChangedHandler
{
   @Inject
   public UIPrefs(Session session, 
                  EventBus eventBus,
                  PrefsServerOperations server)
   {
      super(session.getSessionInfo().getUiPrefs(),
            session.getSessionInfo().getProjectUIPrefs());
      
      session_ = session;
      server_ = server;
      
      eventBus.addHandler(UiPrefsChangedEvent.TYPE, this);
   }
   
   public void writeUIPrefs()
   {
      server_.setUiPrefs(
         session_.getSessionInfo().getUiPrefs(),
         new VoidServerRequestCallback());
   }
   
   @Override
   public void onUiPrefsChanged(UiPrefsChangedEvent e)
   {        
      if (e.getType().equals(UiPrefsChangedEvent.GLOBAL_TYPE))
      {
         // get prefs accessor
         UIPrefsAccessor newUiPrefs = new UIPrefsAccessor(
                                                   e.getUIPrefs(), 
                                                   JsObject.createJsObject());
         
         // show line numbers
         showLineNumbers().setGlobalValue(
                                 newUiPrefs.showLineNumbers().getGlobalValue());
         
         // highlight selected word
         highlightSelectedWord().setGlobalValue(
                           newUiPrefs.highlightSelectedWord().getGlobalValue());
         
         // highlight selected line
         highlightSelectedLine().setGlobalValue(
                          newUiPrefs.highlightSelectedLine().getGlobalValue());
       
         // pane config
         if (!newUiPrefs.paneConfig().getGlobalValue().isEqualTo(
                                 paneConfig().getGlobalValue()))
         {
            paneConfig().setGlobalValue(
                              newUiPrefs.paneConfig().getGlobalValue());
         }
         
         // use spaces for tab
         useSpacesForTab().setGlobalValue(
                          newUiPrefs.useSpacesForTab().getGlobalValue());
           
         // num spacers for tab
         numSpacesForTab().setGlobalValue(
               newUiPrefs.numSpacesForTab().getGlobalValue());
   
         // show margin
         showMargin().setGlobalValue(
                                 newUiPrefs.showMargin().getGlobalValue());
         
         // print margin column
         printMarginColumn().setGlobalValue(
                              newUiPrefs.printMarginColumn().getGlobalValue());
      
         // insert matching
         insertMatching().setGlobalValue(
                                 newUiPrefs.insertMatching().getGlobalValue());
      
         // soft wrap R files
         softWrapRFiles().setGlobalValue(
                                 newUiPrefs.softWrapRFiles().getGlobalValue());
         
         // syntax color console
         syntaxColorConsole().setGlobalValue(
                             newUiPrefs.syntaxColorConsole().getGlobalValue());
      
         // font size
         fontSize().setGlobalValue(
                             newUiPrefs.fontSize().getGlobalValue());
      
         // theme
         theme().setGlobalValue(newUiPrefs.theme().getGlobalValue());
      
         // default encoding
         defaultEncoding().setGlobalValue(
                                 newUiPrefs.defaultEncoding().getGlobalValue());
         
         // default project location
         defaultProjectLocation().setGlobalValue(
                        newUiPrefs.defaultProjectLocation().getGlobalValue());
      
         // toolbar visible
         toolbarVisible().setGlobalValue(
                                 newUiPrefs.toolbarVisible().getGlobalValue());
         
         // source with echo
         sourceWithEcho().setGlobalValue(
                                 newUiPrefs.sourceWithEcho().getGlobalValue());
         
         // export plot options
         if (!ExportPlotOptions.areEqual(
               newUiPrefs.exportPlotOptions().getGlobalValue(),
               exportPlotOptions().getGlobalValue()))
         {
            exportPlotOptions().setGlobalValue(
                              newUiPrefs.exportPlotOptions().getGlobalValue());
         }
         
         // save plot as pdf options
         if (!SavePlotAsPdfOptions.areEqual(
               newUiPrefs.savePlotAsPdfOptions().getGlobalValue(),
               savePlotAsPdfOptions().getGlobalValue()))
         {
            savePlotAsPdfOptions().setGlobalValue(
                         newUiPrefs.savePlotAsPdfOptions().getGlobalValue());
         }
         
         // default sweave engine
         defaultSweaveEngine().setGlobalValue(
                           newUiPrefs.defaultSweaveEngine().getGlobalValue());
         
         // default latex program
         defaultLatexProgram().setGlobalValue(
                           newUiPrefs.defaultLatexProgram().getGlobalValue());
         
         // root document
         rootDocument().setGlobalValue(
                           newUiPrefs.rootDocument().getGlobalValue());
        
         // pdf preview
         pdfPreview().setGlobalValue(
                           newUiPrefs.pdfPreview().getGlobalValue());
         
         // always enable rnw concordance
         alwaysEnableRnwConcordance().setGlobalValue(
                    newUiPrefs.alwaysEnableRnwConcordance().getGlobalValue());
         
         // insert numbered latex sections
         insertNumberedLatexSections().setGlobalValue(
                    newUiPrefs.insertNumberedLatexSections().getGlobalValue());
         
         // spelling dictionary language
         spellingDictionaryLanguage().setGlobalValue(
                    newUiPrefs.spellingDictionaryLanguage().getGlobalValue());
         
         // spelling custom dictionaries
         if (!JsUtil.areEqual(
                     spellingCustomDictionaries().getGlobalValue(),
                     newUiPrefs.spellingCustomDictionaries().getGlobalValue()))
         {
            spellingCustomDictionaries().setGlobalValue(
                     newUiPrefs.spellingCustomDictionaries().getGlobalValue());
         }
            
         // ignore words in uppercase
         ignoreWordsInUppercase().setGlobalValue(
                    newUiPrefs.ignoreWordsInUppercase().getGlobalValue());
         
         // ignore words with numbers
         ignoreWordsWithNumbers().setGlobalValue(
                    newUiPrefs.ignoreWordsWithNumbers().getGlobalValue());
      }
      else if (e.getType().equals(UiPrefsChangedEvent.PROJECT_TYPE))
      {
         // get prefs accessor
         UIPrefsAccessor newUiPrefs = new UIPrefsAccessor(
                                                   JsObject.createJsObject(),
                                                   e.getUIPrefs());
         
         // use spaces for tab
         useSpacesForTab().setProjectValue(
                          newUiPrefs.useSpacesForTab().getValue());
           
         // num spaces for tab
         numSpacesForTab().setProjectValue(
               newUiPrefs.numSpacesForTab().getValue());
   
         // default encoding
         defaultEncoding().setProjectValue(
                                 newUiPrefs.defaultEncoding().getValue());
         
         // default sweave engine
         defaultSweaveEngine().setProjectValue(
                                 newUiPrefs.defaultSweaveEngine().getValue());
         
         // default latex program
         defaultLatexProgram().setProjectValue(
                            newUiPrefs.defaultLatexProgram().getValue());
         
         // root document
         rootDocument().setProjectValue(newUiPrefs.rootDocument().getValue());
      }
      else
      {
         Debug.log("Unexpected uiPrefs type: " + e.getType());
      }
   }
   
   private final Session session_;
   private final PrefsServerOperations server_;
}
