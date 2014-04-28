/*
 * RmdOutput.java
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
package org.rstudio.studio.client.rmarkdown;

import java.util.Map;
import java.util.HashMap;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperation;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.common.viewfile.ViewFilePanel;
import org.rstudio.studio.client.pdfviewer.PDFViewer;
import org.rstudio.studio.client.rmarkdown.events.ConvertToShinyDocEvent;
import org.rstudio.studio.client.rmarkdown.events.RenderRmdEvent;
import org.rstudio.studio.client.rmarkdown.events.RenderRmdSourceEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdRenderCompletedEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdRenderStartedEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdShinyDocStartedEvent;
import org.rstudio.studio.client.rmarkdown.model.RMarkdownServerOperations;
import org.rstudio.studio.client.rmarkdown.model.RmdOutputFormat;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;
import org.rstudio.studio.client.rmarkdown.model.RmdRenderResult;
import org.rstudio.studio.client.rmarkdown.ui.ShinyDocumentWarningDialog;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class RmdOutput implements RmdRenderStartedEvent.Handler,
                                  RmdRenderCompletedEvent.Handler,
                                  RmdShinyDocStartedEvent.Handler,
                                  RenderRmdEvent.Handler,
                                  RenderRmdSourceEvent.Handler,
                                  RestartStatusEvent.Handler
{
   public interface Binder
   extends CommandBinder<Commands, RmdOutput> {}

   @Inject
   public RmdOutput(EventBus eventBus, 
                    Commands commands,
                    GlobalDisplay globalDisplay,
                    FileTypeRegistry fileTypeRegistry,
                    Provider<ViewFilePanel> pViewFilePanel,
                    Binder binder,
                    UIPrefs prefs,
                    PDFViewer pdfViewer,
                    final SatelliteManager satelliteManager,
                    RMarkdownServerOperations server)
   {
      satelliteManager_ = satelliteManager;
      globalDisplay_ = globalDisplay;
      fileTypeRegistry_ = fileTypeRegistry;
      pViewFilePanel_ = pViewFilePanel;
      prefs_ = prefs;
      pdfViewer_ = pdfViewer;
      server_ = server;
      events_ = eventBus;
      
      eventBus.addHandler(RmdRenderStartedEvent.TYPE, this);
      eventBus.addHandler(RmdRenderCompletedEvent.TYPE, this);
      eventBus.addHandler(RmdShinyDocStartedEvent.TYPE, this);
      eventBus.addHandler(RenderRmdEvent.TYPE, this);
      eventBus.addHandler(RenderRmdSourceEvent.TYPE, this);
      eventBus.addHandler(RestartStatusEvent.TYPE, this);

      binder.bind(commands, this);
      
      exportRmdOutputClosedCallback();
   }
   
   @Override
   public void onRmdRenderStarted(RmdRenderStartedEvent event)
   {
      // When a Word document starts rendering, tell the desktop frame 
      // (if it exists) to get ready; this generally involves closing the 
      // document in preparation for a refresh
      if (event.getFormat().getFormatName()
            .equals(RmdOutputFormat.OUTPUT_WORD_DOCUMENT) &&
          Desktop.isDesktop())
      {
         Desktop.getFrame().prepareShowWordDoc();
      }
   }
   
   @Override
   public void onRmdRenderCompleted(RmdRenderCompletedEvent event)
   {
      // if there's a custom operation to be run when render completes, run
      // that instead
      if (onRenderCompleted_ != null)
      {
         onRenderCompleted_.execute();
         onRenderCompleted_ = null;
         return;
      }

      // ignore failures and completed Shiny docs (the latter are handled when
      // the server starts rather than when the render process is finished)
      final RmdRenderResult result = event.getResult();
      if (result.isShinyDocument())
      {
         currentShinyFile_ = null;
         currentShinyUrl_ = null;
         return;
      }
      if (!result.getSucceeded())
         return;
      
      if (result.hasShinyContent() && !result.isShinyDocument())
      {
         // If the result has Shiny content but wasn't rendered as a Shiny
         // document, suggest rendering as a Shiny document instead
         new ShinyDocumentWarningDialog(new OperationWithInput<Integer>()
         {
            @Override
            public void execute(Integer input)
            {
               switch (input)
               {
               case ShinyDocumentWarningDialog.RENDER_SHINY_NO:
                  displayRenderResult(result);
                  break;
               case ShinyDocumentWarningDialog.RENDER_SHINY_ONCE:
                  rerenderAsShiny(result);
                  break;
               case ShinyDocumentWarningDialog.RENDER_SHINY_ALWAYS:
                  events_.fireEvent(new ConvertToShinyDocEvent
                        (result.getTargetFile()));
                  break;
               }
            }
         }).showModal();
      }
      else
      {
         displayRenderResult(event.getResult());
      }
   }
      
   @Override
   public void onRmdShinyDocStarted(RmdShinyDocStartedEvent event)
   {
      currentShinyFile_ = event.getFile();
      currentShinyUrl_ = event.getUrl();
      displayHTMLRenderResult(
            RmdRenderResult.createFromShinyUrl(event.getFile(), 
                                               event.getUrl()));
   }
   
   @Override
   public void onRenderRmd(final RenderRmdEvent event)
   {
      final Operation renderOperation = new Operation() {
         @Override
         public void execute()
         {
            server_.renderRmd(event.getSourceFile(), 
                              event.getSourceLine(),
                              event.getFormat(),
                              event.getEncoding(), 
                              event.asTempfile(),
                              event.asShiny(),
                  new SimpleRequestCallback<Boolean>());
         }
      };

      if (event.getSourceFile().equals(currentShinyFile_))
      {
         // this is a re-render of the current Shiny document; we can reload
         // it in place
         displayHTMLRenderResult(
               RmdRenderResult.createFromShinyUrl(currentShinyFile_,
                                                  currentShinyUrl_));
      }
      else 
      {
         performRenderOperation(renderOperation);
      }
   }
   
   @Override
   public void onRenderRmdSource(final RenderRmdSourceEvent event)
   {
      performRenderOperation(new Operation() {
         @Override
         public void execute()
         {
            server_.renderRmdSource(event.getSource(),
                                    new SimpleRequestCallback<Boolean>()); 
         }

      });
   }

   @Override
   public void onRestartStatus(RestartStatusEvent event)
   {
      // preemptively close the satellite window when R restarts (so we don't
      // wait around if the session doesn't get a chance to tell us about
      // terminated renders)
      if (event.getStatus() == RestartStatusEvent.RESTART_INITIATED) 
      {
         satelliteManager_.closeSatelliteWindow(RmdOutputSatellite.NAME);
         restarting_ = true;
      }
      else
      {
         restarting_ =  false;
      }
   }

   // Private methods ---------------------------------------------------------
   
   // perform the given render after terminating the currently running Shiny
   // application if there is one
   private void performRenderOperation(final Operation renderOperation)
   {
      if (currentShinyFile_ != null)
      {
         // there is a Shiny doc running; we'll need to terminate it before 
         // we can render this document
         satelliteManager_.closeSatelliteWindow(RmdOutputSatellite.NAME);
         server_.terminateRenderRmd(true, new ServerRequestCallback<Void>()
         {
            @Override
            public void onResponseReceived(Void v)
            {
               onRenderCompleted_ = renderOperation;
            }

            @Override
            public void onError(ServerError error)
            {
               globalDisplay_.showErrorMessage("Shiny Terminate Failed", 
                     "The Shiny document " + currentShinyFile_ + " needs to " +
                     "be stopped before the document can be rendered.");
            }
         });
      }
      else
      {
         renderOperation.execute();
      }
   }
   
   private void rerenderAsShiny(RmdRenderResult result)
   {
      events_.fireEvent(new RenderRmdEvent(
            result.getTargetFile(), result.getTargetLine(), 
            null, result.getTargetEncoding(), false, true));
   }
   
   private void displayRenderResult(final RmdRenderResult result)
   {
      String extension = FileSystemItem.getExtensionFromPath(
                                                result.getOutputFile()); 
      if (".pdf".equals(extension))
      {
         String previewer = prefs_.getPdfPreviewValue();
         if (previewer.equals(UIPrefs.PDF_PREVIEW_RSTUDIO))
         {
            pdfViewer_.viewPdfUrl(
                  result.getOutputUrl(), 
                  result.getPreviewSlide() >= 0 ? 
                        result.getPreviewSlide() : null);
         }
         else if (!previewer.equals(UIPrefs.PDF_PREVIEW_NONE))
         {
            if (Desktop.isDesktop())
               Desktop.getFrame().showPDF(result.getOutputFile(),
                                          result.getPreviewSlide());
            else 
               globalDisplay_.showHtmlFile(result.getOutputFile());
         }
      }
      else if (".docx".equals(extension))
      {
         if (Desktop.isDesktop())
            globalDisplay_.showWordDoc(result.getOutputFile());
         
         // it's not possible to show Word docs inline in a useful way from
         // within the browser, so just offer to download the file.
         else
         {
            globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_INFO, 
                  "R Markdown Render Completed", 
                  "R Markdown has finished rendering " + 
                  result.getTargetFile() + " to " + 
                  result.getOutputFile() + ".", 
                  false, 
                  new ProgressOperation()
                  {
                     @Override
                     public void execute(ProgressIndicator indicator)
                     {
                        globalDisplay_.showWordDoc(result.getOutputFile());
                        indicator.onCompleted();
                     }
                  },
                  null, 
                  "Download File", 
                  "OK", 
                  false);
         }
      }
      else if (".html".equals(extension))
      {
         displayHTMLRenderResult(result);
      }
      else if (".md".equalsIgnoreCase(extension) || 
               extension.toLowerCase().startsWith(".markdown") ||
               ".tex".equalsIgnoreCase(extension))
      {
         ViewFilePanel viewFilePanel = pViewFilePanel_.get();
         viewFilePanel.showFile(
            FileSystemItem.createFile(result.getOutputFile()), "UTF-8");
      }
      else
      {
         if (Desktop.isDesktop())
            Desktop.getFrame().showFile(result.getOutputFile());
         else
            globalDisplay_.openWindow(result.getOutputUrl());
      }
   }
   
   private void displayHTMLRenderResult(RmdRenderResult result)
   {
      // find the last known position for this file
      int scrollPosition = 0;
      String anchor = "";
      if (scrollPositions_.containsKey(keyFromResult(result)))
      {
         scrollPosition = scrollPositions_.get(keyFromResult(result));
      }
      if (anchors_.containsKey(keyFromResult(result)))
      {
         anchor = anchors_.get(keyFromResult(result));
      }
      final RmdPreviewParams params = RmdPreviewParams.create(
            result, scrollPosition, anchor);

      WindowEx win = satelliteManager_.getSatelliteWindowObject(
            RmdOutputSatellite.NAME);
      
      // if there's a window up but it's showing a different document type, 
      // close it so that we can create a new one better suited to this doc type
      if (win != null && 
          result_ != null && 
          !result_.getFormatName().equals(result.getFormatName()))
      {
         satelliteManager_.closeSatelliteWindow(RmdOutputSatellite.NAME);
         win = null;
         // let window finish closing before continuing
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            @Override
            public void execute()
            {
               displayRenderResult(null, params);
            }
         });
      }
      else
      {
         displayRenderResult(win, params);
      }
   }
   
   private void displayRenderResult(WindowEx win, RmdPreviewParams params)
   {
      RmdRenderResult result = params.getResult();
      
      // we're refreshing if the window is up and we're pulling the same
      // output file as the last one
      boolean isRefresh = win != null &&
                          result_ != null && 
                          result_.getOutputFile().equals(
                                result.getOutputFile());

      // if this isn't a refresh but there's a window up, cache the scroll
      // position of the old document before we replace it
      if (!isRefresh && result_ != null && win != null)
      {
         cacheDocPosition(result_, getScrollPosition(win), getAnchor(win));
      }

      // if it is a refresh, use the doc's existing positions
      if (isRefresh)
      {
         params.setScrollPosition(getScrollPosition(win));
         params.setAnchor(getAnchor(win));
      }

      if (win != null && !Desktop.isDesktop() && BrowseCap.isChrome())
      {
         satelliteManager_.forceReopenSatellite(RmdOutputSatellite.NAME, 
                                                params);
      }
      else
      {
         satelliteManager_.openSatellite(RmdOutputSatellite.NAME,     
                                         params,
                                         params.getPreferredSize());   
      }

      // save the result so we know if the next render is a re-render of the
      // same document
      result_ = result;
   }
 
   private final native void exportRmdOutputClosedCallback()/*-{
      var registry = this;     
      $wnd.notifyRmdOutputClosed = $entry(
         function(params) {
            registry.@org.rstudio.studio.client.rmarkdown.RmdOutput::notifyRmdOutputClosed(Lcom/google/gwt/core/client/JavaScriptObject;)(params);
         }
      ); 
   }-*/;
   
   private final native int getScrollPosition(JavaScriptObject win) /*-{
      var scrollPosition;
      try {
         scrollPosition = win.getRstudioFrameScrollPosition();
      }
      catch (e) {
         // fail gracefully with top of document
      }
      if (typeof(scrollPosition) === "undefined")
         scrollPosition = 0;
      return scrollPosition;
   }-*/;
   
   private final native String getAnchor(JavaScriptObject win) /*-{
      var anchor;
      try { 
         anchor = win.getRstudioFrameAnchor();
      }
      catch (e) {
         // fail gracefully with no anchor
      }
      if (typeof(anchor) === "undefined")
         anchor = "";
      return anchor;
   }-*/;
   
   // when the window is closed, remember our position within it
   private void notifyRmdOutputClosed(JavaScriptObject closeParams)
   {
      // save anchor location for presentations and scroll position for 
      // documents
      RmdPreviewParams params = closeParams.cast();
      cacheDocPosition(params.getResult(), params.getScrollPosition(), 
                       params.getAnchor());
      
      // if this is a Shiny document, stop the associated process
      if (params.isShinyDocument() && !restarting_)
      {
         server_.terminateRenderRmd(true, new VoidServerRequestCallback());
      }
      currentShinyFile_ = null;
      currentShinyUrl_ = null;
   }
   
   private void cacheDocPosition(RmdRenderResult result, int scrollPosition, 
                                 String anchor)
   {
      if (result.isHtmlPresentation())
      {
         anchors_.put(keyFromResult(result), anchor);
      }
      else
      {
         scrollPositions_.put(keyFromResult(result), scrollPosition);
      }
   }
   
   // Generates lookup keys from results; used to enforce caching scroll 
   // position and/or anchor by document name and type 
   private String keyFromResult(RmdRenderResult result)
   {
      if (result.isShinyDocument())
         return result.getTargetFile();
      else
         return result.getOutputFile() + "-" + result.getFormatName();
   }

   private final SatelliteManager satelliteManager_;
   private final GlobalDisplay globalDisplay_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final UIPrefs prefs_;
   private final PDFViewer pdfViewer_;
   private final Provider<ViewFilePanel> pViewFilePanel_;
   private final RMarkdownServerOperations server_;
   private final EventBus events_;
   private boolean restarting_ = false;

   // stores the last scroll position of each document we know about: map
   // of path to position
   private final Map<String, Integer> scrollPositions_ = 
         new HashMap<String, Integer>();
   private final Map<String, String> anchors_ = 
         new HashMap<String, String>();
   private RmdRenderResult result_;
   private String currentShinyFile_;
   private String currentShinyUrl_;
   private Operation onRenderCompleted_;
}