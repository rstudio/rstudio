/*
 * ZoteroLibrariesWidget.java
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
package org.rstudio.studio.client.workbench.prefs.views.zotero;

import java.util.ArrayList;

import org.rstudio.core.client.widget.CheckBoxList;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.panmirror.server.PanmirrorZoteroCollectionSpec;
import org.rstudio.studio.client.panmirror.server.PanmirrorZoteroResult;
import org.rstudio.studio.client.panmirror.server.PanmirrorZoteroServerOperations;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ZoteroLibrariesWidget extends Composite
{
   public ZoteroLibrariesWidget(PanmirrorZoteroServerOperations server)
   {
      this(server, false);
   }
   
   public ZoteroLibrariesWidget(PanmirrorZoteroServerOperations server, boolean includeUseDefault)
   {
      server_ = server;
      
      VerticalPanel panel = new VerticalPanel();
      panel.addStyleName(RES.styles().librariesWidget());
      
      ArrayList<String> options = new ArrayList<String>();
      ArrayList<String> values = new ArrayList<String>();
      
      if (includeUseDefault)
      {
         options.add("(Default)");
         values.add(USE_DEFAULT);
      }
      options.add("All Libraries");
      values.add(ALL_LIBRARIES);
      options.add("Selected Libraries");
      values.add(SELECTED_LIBRARIES);
      
      selectedLibs_ = new SelectWidget(
         "Use libraries:", 
         options.toArray(new String[] {}),
         values.toArray(new String[] {}),
         false,
         true,
         false
      );
      selectedLibs_.addStyleName(RES.styles().librariesSelect());
      panel.add(selectedLibs_);   
        
      libraries_ = new CheckBoxList(selectedLibs_.getLabel());
      libraries_.addStyleName(RES.styles().librariesList());
      panel.add(libraries_);
   
      Roles.getListboxRole().setAriaLabelledbyProperty(libraries_.getElement(),
            Id.of(selectedLibs_.getLabel().getElement()));
      
      manageUI();
      selectedLibs_.addChangeHandler((value) -> {
         manageUI();
      });
        
      initWidget(panel);
   }
   
   public void setLibraries(JsArrayString libraries)
   {
      ArrayList<String> selectedLibraries = new ArrayList<String>();
      if (libraries != null)
      {
         // set select widget based on whether we have a library whitelist
         selectedLibs_.setValue(libraries.length() == 0 ? ALL_LIBRARIES : SELECTED_LIBRARIES);
         manageUI();
         
         // start with selected libraries
         for (int i = 0; i<libraries.length(); i++)
         {
            String library = libraries.get(i);
            selectedLibraries.add(library);
            CheckBox chkLibrary = new CheckBox(library);
            chkLibrary.setValue(true);
            libraries_.addItem(chkLibrary);
         }
      }
      else
      {
         selectedLibs_.setValue(USE_DEFAULT);
      }
      
      // query for additional libraries if we haven't already
      if (!queriedForLibs_)
      {
         queriedForLibs_ = true;
         
         server_.zoteroGetCollectionSpecs(new SimpleRequestCallback<JavaScriptObject>() {
            @Override
            public void onResponseReceived(JavaScriptObject response)
            {     
               PanmirrorZoteroResult result = response.cast();
               if (result.getStatus().equals("ok"))
               {
                  JsArray<PanmirrorZoteroCollectionSpec> specs = result.getMessage().cast();
                  for (int i = 0; i<specs.length(); i++)
                  {
                     String name = specs.get(i).getName();
                     if (!selectedLibraries.contains(name))
                     {
                        CheckBox chkLibrary = new CheckBox(name);
                        libraries_.addItem(chkLibrary);
                     }
                  }
               }
              
            }
         });
      }
   }
   
   public JsArrayString getLibraries()
   {   
      if (!selectedLibs_.getValue().equals(USE_DEFAULT))
      {
         JsArrayString libraries = JsArrayString.createArray().cast();
         if (selectedLibs_.getValue().equals(SELECTED_LIBRARIES))
         {
            for (int i = 0; i<libraries_.getItemCount(); i++)
            {
               CheckBox chkLibrary = libraries_.getItemAtIdx(i);
               if (chkLibrary.getValue())
                  libraries.push(chkLibrary.getText());
            }
         }
         return libraries;
      }
      else
      {
         return null;
      }
   }
   
   private void manageUI()
   {
      libraries_.setVisible(selectedLibs_.getValue().equals(SELECTED_LIBRARIES));
   }
   
   private static ZoteroResources RES = ZoteroResources.INSTANCE;
   
   
   private final static String USE_DEFAULT = "default";
   private final static String ALL_LIBRARIES = "all";
   private final static String SELECTED_LIBRARIES = "selected";
   
   private final SelectWidget selectedLibs_;
   private final CheckBoxList libraries_;
   
   private final PanmirrorZoteroServerOperations server_;
   
   private boolean queriedForLibs_ = false;
}
