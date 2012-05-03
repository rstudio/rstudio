/*
 * Synctex.java
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

package org.rstudio.studio.client.common.synctex;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfCompletedEvent;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfStartedEvent;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.common.synctex.events.SynctexStatusChangedEvent;
import org.rstudio.studio.client.common.synctex.events.SynctexViewPdfEvent;
import org.rstudio.studio.client.common.synctex.events.SynctexEditFileEvent;
import org.rstudio.studio.client.common.synctex.model.PdfLocation;
import org.rstudio.studio.client.common.synctex.model.SourceLocation;
import org.rstudio.studio.client.common.synctex.model.SynctexServerOperations;
import org.rstudio.studio.client.pdfviewer.PDFViewerApplication;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class Synctex implements CompilePdfStartedEvent.Handler,
                                CompilePdfCompletedEvent.Handler,
                                SynctexEditFileEvent.Handler
{
   @Inject
   public Synctex(GlobalDisplay globalDisplay,
                  EventBus eventBus,
                  Commands commands,
                  SynctexServerOperations server,
                  FileTypeRegistry fileTypeRegistry,
                  Session session,
                  UIPrefs prefs,
                  Satellite satellite, 
                  SatelliteManager satelliteManager)
   {
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      commands_ = commands;
      server_ = server;
      fileTypeRegistry_ = fileTypeRegistry;
      session_ = session;
      prefs_ = prefs;
      satellite_ = satellite;
      satelliteManager_ = satelliteManager;
      
      // main window and satellite export callbacks to eachother
      if (!satellite.isCurrentWindowSatellite())
      {
         registerMainWindowCallbacks();
         
         eventBus_.addHandler(SynctexEditFileEvent.TYPE, this);
      }
      else if (isCurrentWindowPdfViewerSatellite())
      {
         registerSatelliteCallbacks();
        
         satellite_.addCloseHandler(new CloseHandler<Satellite>() {
            @Override
            public void onClose(CloseEvent<Satellite> event)
            {
               callNotifyPdfViewerClosed(pdfPath_);
            }
         });
      }
      
      // fixup synctex tooltips for macos
      if (BrowseCap.isMacintosh())
         fixupSynctexCommandDescription(commands_.synctexSearch());
      
      // disable commands at the start
      setNoSynctexStatus();
      
      // subscribe to compile pdf status event so we can update command status
      eventBus_.addHandler(CompilePdfStartedEvent.TYPE, this);
      eventBus_.addHandler(CompilePdfCompletedEvent.TYPE, this);
   }
    
   @Override
   public void onCompilePdfStarted(CompilePdfStartedEvent event)
   {
      setNoSynctexStatus();
   }

   @Override
   public void onCompilePdfCompleted(CompilePdfCompletedEvent event)
   {
      String pdfPreview = prefs_.pdfPreview().getValue();
      
      boolean synctexSupported =
                  // internal previewer
                  (pdfPreview.equals(UIPrefs.PDF_PREVIEW_RSTUDIO) &&
                  session_.getSessionInfo().isInternalPdfPreviewEnabled()) ||
                  // platform-specific desktop previewer
                  (pdfPreview.equals(UIPrefs.PDF_PREVIEW_DESKTOP_SYNCTEX) &&
                   Desktop.isDesktop());
      
      boolean synctexAvailable = synctexSupported && 
                                 event.getResult().isSynctexAvailable();
       
      if (synctexAvailable)
         setSynctexStatus(event.getResult().getTargetFile(),
                          event.getResult().getPdfPath());
      else
         setNoSynctexStatus();
      
      // if this was a desktop synctex preview then invoke it directly
      // (internal previews are handled by the compile pdf window directly)
      if (event.getResult().getSucceeded() && handleDesktopSynctex())
      {
         Desktop.getFrame().externalSynctexPreview(
                                 event.getResult().getPdfPath(), 
                                 event.getResult().getPdfLocation().getPage());
      }
   }
   
   @Override
   public void onSynctexEditFile(SynctexEditFileEvent event)
   {
      goToSourceLocation(event.getSourceLocation());
   }
   
   
   public boolean isSynctexAvailable()
   {
      return pdfPath_ != null;
   }
   
   public void enableCommands(boolean enabled)
   {
      commands_.synctexSearch().setVisible(enabled);
      commands_.synctexSearch().setEnabled(enabled);
   }

   // NOTE: the original design was for a single internal pdf viewer. for
   // that configuration we could keep a global pdfPath_ around and be
   // confident that it was always correct. we were also globally managing
   // the state of the synctex command based on any external viewer closing.
   // now that we optionally support desktop viewers for synctex this 
   // assumption may not hold -- specfically there might be multiple active
   // PDF viewers for different document. we have explicitly chosen to 
   // avoid the complexity of tracking distinct viewer states. if we want
   // to do this we probably should do the following:
   //
   //    - always keep the the syncex command available in all editors
   //      so long as there is at least one preview window alive 
   //
   //    - have the server determine the pdfPath for a target file
   //      (based on parsing magic comments, etc.) which will eliminate
   //      the need for us to maintain the pdfPath_ separately here.
   //
   // In any case, the two issues are command enable/disable and letting
   // the server know which pdfPath to apply synctex/concordance to.
   //
   public boolean forwardSearch(SourceLocation sourceLocation)
   {
      if (handleDesktopSynctex())
      {
         // apply concordane
         final ProgressIndicator indicator = getSyncProgress();  
         server_.applyForwardConcordance(
                                 pdfPath_, 
                                 sourceLocation, 
                                 new ServerRequestCallback<SourceLocation>() {
            @Override
            public void onResponseReceived(SourceLocation sourceLocation)
            {
               indicator.onCompleted();
               
               if (sourceLocation != null)
               {
                  Desktop.getFrame().externalSynctexView(
                                 pdfPath_,
                                 sourceLocation.getFile(),
                                 sourceLocation.getLine(),
                                 sourceLocation.getColumn());    
               }
            }
            
            @Override
            public void onError(ServerError error)
            {
               indicator.onError(error.getUserMessage());
            }
            
         });
         
         return true;
      }
      
      // use internal viewer
      else
      {
         // return false if there is no pdf viewer
         WindowEx window = satelliteManager_.getSatelliteWindowObject(
                                                      PDFViewerApplication.NAME);
         if (window == null)
            return false;
         
         // activate the satellite
         satelliteManager_.activateSatelliteWindow(PDFViewerApplication.NAME);
            
         // execute the forward search
         callForwardSearch(window, targetFile_, sourceLocation);
     
         return true;
      }
   }
   
   public void inverseSearch(PdfLocation pdfLocation)
   {
      // switch back to the main window 
      satellite_.focusMainWindow();
      
      // warn firefox users that this doesn't really work in Firefox
      if (BrowseCap.isFirefox())
         SynctexUtils.maybeShowFirefoxWarning("source editor");
      
      // do the inverse search
      callInverseSearch(pdfLocation);
   }
   
   
   private void doForwardSearch(String rootDocument,
                                JavaScriptObject sourceLocationObject)
   {
      SourceLocation sourceLocation = sourceLocationObject.cast();
      
      final ProgressIndicator indicator = getSyncProgress();
      server_.synctexForwardSearch(
         rootDocument,
         sourceLocation,
         new ServerRequestCallback<PdfLocation>() {

            @Override
            public void onResponseReceived(PdfLocation location)
            {
               indicator.onCompleted();
               
               if (location != null)
                  eventBus_.fireEvent(new SynctexViewPdfEvent(location));
            }
            
            @Override
            public void onError(ServerError error)
            {
               indicator.onError(error.getUserMessage());     
            }
               
       });
   }
   
   private void doInverseSearch(JavaScriptObject pdfLocationObject)
   {
      PdfLocation pdfLocation = pdfLocationObject.cast();
      
      final ProgressIndicator indicator = getSyncProgress();  
      server_.synctexInverseSearch(
          pdfLocation,
          new ServerRequestCallback<SourceLocation>() {

             @Override
             public void onResponseReceived(SourceLocation location)
             {
                indicator.onCompleted();
                
                if (location != null)
                   goToSourceLocation(location);
             }

             @Override
             public void onError(ServerError error)
             {
                indicator.onError(error.getUserMessage());     
             }     
        });   
   }  
   
   private void doDesktopInverseSearch(String file, int line, int column)
   {
      // apply concordance
      final ProgressIndicator indicator = getSyncProgress();  
      server_.applyInverseConcordance(
                              SourceLocation.create(file, line, column, true),
                              new ServerRequestCallback<SourceLocation>() {
         @Override
         public void onResponseReceived(SourceLocation sourceLocation)
         {
            indicator.onCompleted();
            
            if (sourceLocation != null)
               goToSourceLocation(sourceLocation);
         }
         
         @Override
         public void onError(ServerError error)
         {
            indicator.onError(error.getUserMessage());
         }
         
      });
      
   }

   private void goToSourceLocation(SourceLocation location)
   {
       FilePosition position = FilePosition.create(
             location.getLine(), 
             Math.min(1, location.getColumn()));
       
       fileTypeRegistry_.editFile(
                      FileSystemItem.createFile(location.getFile()), 
                      position);
   }
    
   
   private void notifyPdfViewerClosed(String pdfFile)
   {
      setNoSynctexStatus();
   }
   
   private boolean isCurrentWindowPdfViewerSatellite()
   {
      return satellite_.isCurrentWindowSatellite() && 
             satellite_.getSatelliteName().equals(PDFViewerApplication.NAME);
            
   }
   
   private boolean handleDesktopSynctex()
   {
      return Desktop.isDesktop() && 
             !satellite_.isCurrentWindowSatellite() &&
              prefs_.pdfPreview().getValue().equals(
                                   UIPrefs.PDF_PREVIEW_DESKTOP_SYNCTEX);
   }

   private ProgressIndicator getSyncProgress()
   {
      return new GlobalProgressDelayer(globalDisplay_, 
                                       500, 
                                       "Syncing...").getIndicator();
   }
   
   private void setNoSynctexStatus()
   {
      setSynctexStatus(null, null);
   }
   
   private void setSynctexStatus(String targetFile, 
                                 String pdfPath)
   {
      // set flag and fire event
      if (!StringUtil.notNull(pdfPath_).equals(StringUtil.notNull(pdfPath)))
      {
         targetFile_ = targetFile;
         pdfPath_ = pdfPath;
         eventBus_.fireEvent(new SynctexStatusChangedEvent(targetFile, 
                                                           pdfPath));
      }
   }
   
   private void fixupSynctexCommandDescription(AppCommand command)
   {
      String desc = command.getDesc().replace("Ctrl+", "Cmd+");
      command.setDesc(desc);
   }
   
   private native void registerMainWindowCallbacks() /*-{
      var synctex = this;     
      $wnd.synctexInverseSearch = $entry(
         function(pdfLocation) {
            synctex.@org.rstudio.studio.client.common.synctex.Synctex::doInverseSearch(Lcom/google/gwt/core/client/JavaScriptObject;)(pdfLocation);
         }
      ); 
      
      $wnd.desktopSynctexInverseSearch = $entry(
         function(file,line,column) {
            synctex.@org.rstudio.studio.client.common.synctex.Synctex::doDesktopInverseSearch(Ljava/lang/String;II)(file,line,column);
         }
      ); 
      
      $wnd.synctexNotifyPdfViewerClosed = $entry(
         function(pdfPath) {
            synctex.@org.rstudio.studio.client.common.synctex.Synctex::notifyPdfViewerClosed(Ljava/lang/String;)(pdfPath);
         }
      );       
   }-*/;
   
   private final native void callInverseSearch(JavaScriptObject pdfLocation)/*-{
      $wnd.opener.synctexInverseSearch(pdfLocation);
   }-*/;
   
   private final native void callNotifyPdfViewerClosed(String pdfPath) /*-{
      $wnd.opener.synctexNotifyPdfViewerClosed(pdfPath);
   }-*/;
   
   private native void registerSatelliteCallbacks() /*-{
      var synctex = this;     
      $wnd.synctexForwardSearch = $entry(
         function(rootDocument, sourceLocation) {
            synctex.@org.rstudio.studio.client.common.synctex.Synctex::doForwardSearch(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(rootDocument, sourceLocation);
         }
      ); 
   }-*/;
   
   private native void callForwardSearch(JavaScriptObject satellite,
                                         String rootDocument,
                                         JavaScriptObject sourceLocation) /*-{
      satellite.synctexForwardSearch(rootDocument, sourceLocation);
   }-*/;
   
   private final GlobalDisplay globalDisplay_;
   private final EventBus eventBus_;
   private final Commands commands_;
   private final SynctexServerOperations server_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final Session session_;
   private final UIPrefs prefs_;
   private final Satellite satellite_;
   private final SatelliteManager satelliteManager_;
   private String pdfPath_ = null;
   private String targetFile_ = "";
   
 
}
