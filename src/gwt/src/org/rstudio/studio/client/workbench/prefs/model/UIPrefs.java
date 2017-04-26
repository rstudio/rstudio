/*
 * UIPrefs.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.prefs.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.notebook.CompileNotebookPrefs;
import org.rstudio.studio.client.notebookv2.CompileNotebookv2Prefs;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.exportplot.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.events.UiPrefsChangedEvent;
import org.rstudio.studio.client.workbench.prefs.events.UiPrefsChangedHandler;
import org.rstudio.studio.client.workbench.views.plots.model.SavePlotAsPdfOptions;

@Singleton
public class UIPrefs extends UIPrefsAccessor implements UiPrefsChangedHandler
{
   @Inject
   public UIPrefs(Session session, 
                  EventBus eventBus,
                  PrefsServerOperations server,
                  SatelliteManager satelliteManager)
   {
      super(session.getSessionInfo(),
            session.getSessionInfo().getUiPrefs(),
            session.getSessionInfo().getProjectUIPrefs());
      
      session_ = session;
      server_ = server;
      satelliteManager_ = satelliteManager;
      
      eventBus.addHandler(UiPrefsChangedEvent.TYPE, this);
   }
   
   public void writeUIPrefs()
   {
      server_.setUiPrefs(
         session_.getSessionInfo().getUiPrefs(),
         new ServerRequestCallback<Void>() 
         {
            @Override
            public void onResponseReceived(Void v)
            {
               UiPrefsChangedEvent event = new UiPrefsChangedEvent(
                     UiPrefsChangedEvent.Data.create(
                              UiPrefsChangedEvent.GLOBAL_TYPE,
                              session_.getSessionInfo().getUiPrefs()));

               if (Satellite.isCurrentWindowSatellite())
               {
                  RStudioGinjector.INSTANCE.getEventBus()
                     .fireEventToMainWindow(event);
               }
               else
               {
                  // let satellites know prefs have changed
                  satelliteManager_.dispatchCrossWindowEvent(event);
               }
            }
            @Override
            public void onError(ServerError error)
            {
               Debug.logError(error);
            }
         });
   }
   
   @Override
   public void onUiPrefsChanged(UiPrefsChangedEvent e)
   {        
      if (e.getType().equals(UiPrefsChangedEvent.GLOBAL_TYPE))
      {
         // get prefs accessor
         UIPrefsAccessor newUiPrefs = new UIPrefsAccessor(
                                                   session_.getSessionInfo(),
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
   
         // blinking cursor
         blinkingCursor().setGlobalValue(
               newUiPrefs.blinkingCursor().getGlobalValue());
         
         // show margin
         showMargin().setGlobalValue(
                                 newUiPrefs.showMargin().getGlobalValue());
         
         // print margin column
         printMarginColumn().setGlobalValue(
                              newUiPrefs.printMarginColumn().getGlobalValue());
      
         // show invisibles
         showInvisibles().setGlobalValue(
                              newUiPrefs.showInvisibles().getGlobalValue());
         
         // show indent guides
         showIndentGuides().setGlobalValue(
                              newUiPrefs.showIndentGuides().getGlobalValue());
         
         // document outline width
         preferredDocumentOutlineWidth().setGlobalValue(
                              newUiPrefs.preferredDocumentOutlineWidth().getGlobalValue());
         
         // show document outline by default for Rmd
         showDocumentOutlineRmd().setGlobalValue(
                              newUiPrefs.showDocumentOutlineRmd().getGlobalValue());
         
         // use vim mode
         useVimMode().setGlobalValue(
                              newUiPrefs.useVimMode().getGlobalValue());
         
         // emacs keybindings
         enableEmacsKeybindings().setGlobalValue(
                              newUiPrefs.enableEmacsKeybindings().getGlobalValue());
         
         continueCommentsOnNewline().setGlobalValue(
                              newUiPrefs.continueCommentsOnNewline().getGlobalValue());
         
         // insert matching
         insertMatching().setGlobalValue(
                                 newUiPrefs.insertMatching().getGlobalValue());
              
         codeComplete().setGlobalValue(
                                 newUiPrefs.codeComplete().getGlobalValue());
         
         codeCompleteOther().setGlobalValue(
               newUiPrefs.codeCompleteOther().getGlobalValue());
         
         alwaysCompleteInConsole().setGlobalValue(
                                 newUiPrefs.alwaysCompleteInConsole().getGlobalValue());
         
         alwaysCompleteDelayMs().setGlobalValue(
                                 newUiPrefs.alwaysCompleteDelayMs().getGlobalValue());
         
         alwaysCompleteCharacters().setGlobalValue(
                                 newUiPrefs.alwaysCompleteCharacters().getGlobalValue());
         
         insertParensAfterFunctionCompletion().setGlobalValue(
                                 newUiPrefs.insertParensAfterFunctionCompletion().getGlobalValue());
         
         allowTabMultilineCompletion().setGlobalValue(
                                 newUiPrefs.allowTabMultilineCompletion().getGlobalValue());
         
         showFunctionTooltipOnIdle().setGlobalValue(
                                 newUiPrefs.showFunctionTooltipOnIdle().getGlobalValue());
         
         surroundSelection().setGlobalValue(
                                 newUiPrefs.surroundSelection().getGlobalValue());
         
         enableSnippets().setGlobalValue(
                                 newUiPrefs.enableSnippets().getGlobalValue());
         
         insertSpacesAroundEquals().setGlobalValue(
                                 newUiPrefs.insertSpacesAroundEquals().getGlobalValue());
         
         showSignatureTooltips().setGlobalValue(
                                 newUiPrefs.showSignatureTooltips().getGlobalValue());
         
         terminalLocalEcho().setGlobalValue(
                                 newUiPrefs.terminalLocalEcho().getGlobalValue());
         
         terminalUseWebsockets().setGlobalValue(
                                 newUiPrefs.terminalUseWebsockets().getGlobalValue());
         
         /* Diagnostics */
         
         // R Diagnostics
         
         showDiagnosticsR().setGlobalValue(
               newUiPrefs.showDiagnosticsR().getGlobalValue());
         
         diagnosticsInRFunctionCalls().setGlobalValue(
               newUiPrefs.diagnosticsInRFunctionCalls().getGlobalValue());
         
         checkArgumentsToRFunctionCalls().setGlobalValue(
               newUiPrefs.checkArgumentsToRFunctionCalls().getGlobalValue());
         
         warnIfNoSuchVariableInScope().setGlobalValue(
               newUiPrefs.warnIfNoSuchVariableInScope().getGlobalValue());
         
         warnIfVariableDefinedButNotUsed().setGlobalValue(
               newUiPrefs.warnIfVariableDefinedButNotUsed().getGlobalValue());
         
         enableStyleDiagnostics().setGlobalValue(
               newUiPrefs.enableStyleDiagnostics().getGlobalValue());
         
         // Other diagnostics
         
         showDiagnosticsCpp().setGlobalValue(
               newUiPrefs.showDiagnosticsCpp().getGlobalValue());
         
         showDiagnosticsOther().setGlobalValue(
               newUiPrefs.showDiagnosticsOther().getGlobalValue());
         
         // Background Linting
         
         diagnosticsOnSave().setGlobalValue(
               newUiPrefs.diagnosticsOnSave().getGlobalValue());
         
         enableBackgroundDiagnostics().setGlobalValue(
               newUiPrefs.enableBackgroundDiagnostics().getGlobalValue());
         
         backgroundDiagnosticsDelayMs().setGlobalValue(
               newUiPrefs.backgroundDiagnosticsDelayMs().getGlobalValue());
         
         /* End Diagnostics UI Prefs */
         
         autoAppendNewline().setGlobalValue(
                                 newUiPrefs.autoAppendNewline().getGlobalValue());
         
         stripTrailingWhitespace().setGlobalValue(
                       newUiPrefs.stripTrailingWhitespace().getGlobalValue());
      
         // soft wrap R files
         softWrapRFiles().setGlobalValue(
                                 newUiPrefs.softWrapRFiles().getGlobalValue());
         
         // focus console after exec
         focusConsoleAfterExec().setGlobalValue(
                         newUiPrefs.focusConsoleAfterExec().getGlobalValue());
         
         // fold style
         foldStyle().setGlobalValue(
               newUiPrefs.foldStyle().getGlobalValue());
         
         // save before sourcing
         saveBeforeSourcing().setGlobalValue(
                         newUiPrefs.saveBeforeSourcing().getGlobalValue());
         
         // syntax color console
         syntaxColorConsole().setGlobalValue(
                             newUiPrefs.syntaxColorConsole().getGlobalValue());
         
         // enable scroll past end of document
         scrollPastEndOfDocument().setGlobalValue(
                             newUiPrefs.scrollPastEndOfDocument().getGlobalValue());
         
         // highlight R function calls
         highlightRFunctionCalls().setGlobalValue(
                             newUiPrefs.highlightRFunctionCalls().getGlobalValue());
         
         // truncate long lines in console history
         truncateLongLinesInConsoleHistory().setGlobalValue(
                             newUiPrefs.truncateLongLinesInConsoleHistory().getGlobalValue());
         
         // console handling of ANSI escape codes
         consoleAnsiMode().setGlobalValue(
               newUiPrefs.consoleAnsiMode().getGlobalValue());
         
         // chunk toolbar
         showInlineToolbarForRCodeChunks().setGlobalValue(
               newUiPrefs.showInlineToolbarForRCodeChunks().getGlobalValue());
         
         // save all before build
         saveAllBeforeBuild().setGlobalValue(
                             newUiPrefs.saveAllBeforeBuild().getGlobalValue());
      
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
         
         // clear hidden values in workspace
         clearHidden().setGlobalValue(
                                 newUiPrefs.clearHidden().getGlobalValue());
         
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
         
         // export viewer options
         if (!ExportPlotOptions.areEqual(
               newUiPrefs.exportViewerOptions().getGlobalValue(),
               exportViewerOptions().getGlobalValue()))
         {
            exportViewerOptions().setGlobalValue(
                          newUiPrefs.exportViewerOptions().getGlobalValue());
         }
         
         
         // compile notebook options
         if (!CompileNotebookPrefs.areEqual(
               newUiPrefs.compileNotebookOptions().getGlobalValue(),
               compileNotebookOptions().getGlobalValue()))
         {
            compileNotebookOptions().setGlobalValue(
                        newUiPrefs.compileNotebookOptions().getGlobalValue());
         }
         if (!CompileNotebookv2Prefs.areEqual(
               newUiPrefs.compileNotebookv2Options().getGlobalValue(),
               compileNotebookv2Options().getGlobalValue()))
         {
            compileNotebookv2Options().setGlobalValue(
                        newUiPrefs.compileNotebookv2Options().getGlobalValue());
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
         
         // use roxygen
         useRoxygen().setGlobalValue(
                           newUiPrefs.useRoxygen().getGlobalValue());
        
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
         
         // navigate to build error
         navigateToBuildError().setGlobalValue(
                    newUiPrefs.navigateToBuildError().getGlobalValue());
         
         // enable packages pane
         packagesPaneEnabled().setGlobalValue(
                    newUiPrefs.packagesPaneEnabled().getGlobalValue());
         
         // use rcpp template
         useRcppTemplate().setGlobalValue(
                    newUiPrefs.useRcppTemplate().getGlobalValue());
         
         // restore source documents
         restoreSourceDocuments().setGlobalValue(
                    newUiPrefs.restoreSourceDocuments().getGlobalValue());
         
         // break in user code only on unhandled errors
         handleErrorsInUserCodeOnly().setGlobalValue(
                    newUiPrefs.handleErrorsInUserCodeOnly().getGlobalValue());
                    
         // auto expand error tracebacks
         autoExpandErrorTracebacks().setGlobalValue(
                    newUiPrefs.autoExpandErrorTracebacks().getGlobalValue());
         
         // preferred R Markdown template
         rmdPreferredTemplatePath().setGlobalValue(
               newUiPrefs.rmdPreferredTemplatePath().getGlobalValue());

         // whether to show publish UI 
         showPublishUi().setGlobalValue(
               newUiPrefs.showPublishUi().getGlobalValue());
         
         // how to view R Markdown documents
         rmdViewerType().setGlobalValue(
               newUiPrefs.rmdViewerType().getGlobalValue());
         
         // show improved data import dialog
         useDataImport().setGlobalValue(
               newUiPrefs.useDataImport().getGlobalValue());
      }
      else if (e.getType().equals(UiPrefsChangedEvent.PROJECT_TYPE))
      {
         // get prefs accessor
         UIPrefsAccessor newUiPrefs = new UIPrefsAccessor(
                                                   session_.getSessionInfo(),
                                                   JsObject.createJsObject(),
                                                   e.getUIPrefs());
         
         // use spaces for tab
         useSpacesForTab().setProjectValue(
                          newUiPrefs.useSpacesForTab().getValue());
           
         // num spaces for tab
         numSpacesForTab().setProjectValue(
               newUiPrefs.numSpacesForTab().getValue());
         
         // auto-append newline
         autoAppendNewline().setProjectValue(
               newUiPrefs.autoAppendNewline().getValue());
         
         // strip trailing whitespace
         stripTrailingWhitespace().setProjectValue(
               newUiPrefs.stripTrailingWhitespace().getValue());
   
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
         
         // use roxygen
         useRoxygen().setProjectValue(newUiPrefs.useRoxygen().getValue());
      }
      else
      {
         Debug.log("Unexpected uiPrefs type: " + e.getType());
      }
   }
   
   private final Session session_;
   private final PrefsServerOperations server_;
   private final SatelliteManager satelliteManager_;
}
