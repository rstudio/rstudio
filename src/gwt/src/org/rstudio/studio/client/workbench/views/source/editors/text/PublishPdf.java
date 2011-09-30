/*
 * PublishPdf.java
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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.inject.Inject;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.events.OAuthApprovalEvent;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.GoogleDocInfo;
import org.rstudio.studio.client.workbench.views.source.model.PublishPdfResult;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import java.util.Date;
import java.util.HashMap;

public class PublishPdf
{
   public interface Display
   { 
      void showPublishUI(String defaultTitle);
      void showPublishUI(String title, String id, Date lastUpdated);
      
      String getTitle();
      boolean getUpdateExisting();
      
      HasClickHandlers getOkButton();
      
      void dismiss();
   }
   
   @Inject
   public PublishPdf(GlobalDisplay globalDisplay, 
                     EventBus eventBus,
                     SourceServerOperations server)
   {
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      server_ = server;  
   }
   
   public void publish(String id, 
                       DocDisplay docDisplay, 
                       DocUpdateSentinel doc, 
                       Display display)
   {  
      // save references
      id_ = id;
      docDisplay_ = docDisplay;
      doc_ = doc;
      display_ = display;
      
      final GoogleDocInfo docInfo = getGoogleDocInfo();
      if (docInfo != null)
      {
         display_.showPublishUI(docInfo.getTitle(),
                                docInfo.getResourceId(),
                                docInfo.getUpdated());
      }
      else
      {
         display_.showPublishUI(getDefaultTitle());
      }
        
      // subscribe to ok button click to perform the publish
      display_.getOkButton().addClickHandler(new ClickHandler() {
         public void onClick(ClickEvent event)
         {
            publishPdf();
         }
      });
      
      
   }
    
   private String getDefaultTitle()
   {
      // see if we can parse a title out of the doc display
      String code = docDisplay_.getCode();
      Pattern titlePattern = Pattern.create("\\\\title\\{([^\\\\{}]+)\\}");
      Match titleMatch = titlePattern.match(code, 0);
      if (titleMatch != null)
         return titleMatch.getGroup(1);
      else
         return FileSystemItem.createFile(doc_.getPath()).getStem();
   }
    
   private void publishPdf()
   {
      // dismiss the dialog
      display_.dismiss();
      
      globalDisplay_.openProgressWindow("_rstudio_publish_pdf",
                                        "Publishing PDF to Google Docs...", 
                                        new OperationWithInput<WindowEx>() {                                        
         public void execute(final WindowEx window)
         {
            // publish pdf
            server_.publishPdf(id_, 
                               display_.getTitle(), 
                               display_.getUpdateExisting(), 
                               new ServerRequestCallback<PublishPdfResult>() {
              @Override
              public void onResponseReceived(PublishPdfResult result)
              {
                 if (result.getGoogleDocInfo() != null)
                 {
                    // get doc info and save it
                    GoogleDocInfo docInfo = result.getGoogleDocInfo();
                    setGoogleDocInfo(docInfo);
                    
                    // redirect window
                    window.replaceLocationHref(docInfo.getURL());
                 }
                 else
                 {
                    // always close window
                    window.close();
                 
                    // show auth if appropriate
                    if (result.getOAuthApproval() != null)
                    {
                       OAuthApprovalEvent event = new OAuthApprovalEvent(
                                                      result.getOAuthApproval());
                       eventBus_.fireEvent(event);
                    }
                    // show error if appropriate
                    else if (result.getErrorMessage() != null)
                    {
                       globalDisplay_.showErrorMessage("Error Publishing PDF",
                                                       result.getErrorMessage());
                    }
                 }
              }
           
              @Override
              public void onError(ServerError error)
              {
                 // close window on error
                 window.close();
                 
                 // show error message
                 globalDisplay_.showErrorMessage("Error Publishing PDF",
                                                 error.getUserMessage());
              }
         });
            
         }
      });
   }
   
   private GoogleDocInfo getGoogleDocInfo()
   {
      String title = doc_.getProperty("gdocTitle");
      if (title != null)
      {
         String resourceId = doc_.getProperty("gdocResourceId");
         String updated = doc_.getProperty("gdocUpdated");
         String url = doc_.getProperty("gdocURL");
         return GoogleDocInfo.create(title, resourceId, updated, url);
      }
      else
      {
         return null;
      }
   }
   
   private void setGoogleDocInfo(GoogleDocInfo docInfo)
   {
      HashMap<String, String> docProperties = new HashMap<String, String>();
      docProperties.put("gdocTitle", docInfo.getTitle());
      docProperties.put("gdocResourceId", docInfo.getResourceId());
      docProperties.put("gdocUpdated", docInfo.getUpdated().getTime() + "");
      docProperties.put("gdocURL", docInfo.getURL());
      doc_.modifyProperties(docProperties, null);
   }
  
   private String id_;
   private DocDisplay docDisplay_;
   private DocUpdateSentinel doc_;
   private Display display_;
   private GlobalDisplay globalDisplay_;
   private EventBus eventBus_;
   private SourceServerOperations server_;
  
}
