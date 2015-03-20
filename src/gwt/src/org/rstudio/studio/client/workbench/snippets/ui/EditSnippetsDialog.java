/*
 * EditSnippetsPanel.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.snippets.ui;


import java.util.ArrayList;

import javax.inject.Inject;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.dom.DomMetrics;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ModalDialogTracker;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.WidgetListBox;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.workbench.ui.FontSizeManager;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

public class EditSnippetsDialog extends ModalDialogBase implements TextDisplay
{
   public EditSnippetsDialog()
   {
      setText("Edit Snippets");
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      addCancelButton();
      ThemedButton saveButton = new ThemedButton("Save", new ClickHandler() {
         public void onClick(ClickEvent event) 
         {
            attemptSaveAndClose();
         }
      });
      addButton(saveButton);
      
      addLeftWidget(new HelpLink("Using Code Snippets", "code_snippets"));
   }
   
   @Inject
   void initialize(EventBus events, FontSizeManager fontSizeManager)
   {
      events_ = events;
      fontSizeManager_ = fontSizeManager;
   }
   
   @Override
   protected Widget createMainWidget()
   {
      panel_ = new DockLayoutPanel(Unit.PX);
      
      // compute the widget size and set it
      Size size = new Size(900, 900);
      size = DomMetrics.adjustedElementSize(size, 
                                            null, 
                                            70,   // pad
                                            100); // client margin
      panel_.setWidth(size.width + "px");
      panel_.setHeight(size.height + "px");
      
      // snippet types
      snippetTypes_ = new WidgetListBox<SnippetType>();
      snippetTypes_.addChangeHandler(new ChangeHandler() {
         @Override
         public void onChange(ChangeEvent event)
         {
            updateEditor(snippetTypes_.getSelectedItem().getFileType());
         }
      });
      snippetTypes_.addItem(new SnippetType("R", FileTypeRegistry.R));
      snippetTypes_.addItem(new SnippetType(FileTypeRegistry.CPP));
      snippetTypes_.addItem(new SnippetType(FileTypeRegistry.JAVA));
      snippetTypes_.addItem(new SnippetType(FileTypeRegistry.PYTHON));
      snippetTypes_.addItem(new SnippetType(FileTypeRegistry.SQL));
      snippetTypes_.addItem(new SnippetType(FileTypeRegistry.JS));
      snippetTypes_.addItem(new SnippetType(FileTypeRegistry.HTML));
      snippetTypes_.addItem(new SnippetType(FileTypeRegistry.CSS));
     
      panel_.addWest(snippetTypes_, 150);
     
      // editor
      docDisplay_ = new AceEditor();
      docDisplay_.setFileType(FileTypeRegistry.SNIPPETS);
      SimplePanel panel = new SimplePanel();
      panel.addStyleName("EditDialog");
      panel.getElement().getStyle().setMarginLeft(8, Unit.PX);
      panel.setWidget(docDisplay_.asWidget());
      panel_.add(panel);
      
      
      TextEditingTarget.syncFontSize(releaseOnDismiss_, 
            events_, 
            this, 
            fontSizeManager_); 
      
      // default to R
      updateEditor(FileTypeRegistry.R);
      
      return panel_;
   }
   
   @Override
   public void onDialogShown()
   {
      docDisplay_.focus();
   }
   
   private void attemptSaveAndClose()
   {
      closeDialog();
   }
   
   @Override
   public void onActivate()
   {
      docDisplay_.onActivate();
   }

   @Override
   public void adaptToFileType(TextFileType fileType)
   {
      docDisplay_.setFileType(fileType, true);
   }

   @Override
   public void setFontSize(double size)
   {
      docDisplay_.setFontSize(size);
   }
   
   @Override
   public Widget asWidget()
   {
      return this;
   }
   
   @Override
   public void onPreviewNativeEvent(Event.NativePreviewEvent event)
   {
      if (!ModalDialogTracker.isTopMost(this))
         return;

      if (event.getTypeInt() == Event.ONKEYDOWN)
      {
         NativeEvent ne = event.getNativeEvent();
         int mod = KeyboardShortcut.getModifierValue(ne);
         if ((mod == KeyboardShortcut.META || 
             (mod == KeyboardShortcut.CTRL && !BrowseCap.hasMetaKey())))
         {
            if (ne.getKeyCode() == 'S')
            {
               ne.preventDefault();
               ne.stopPropagation();
               event.cancel();
               attemptSaveAndClose();
            }
         }  
      }
      
      super.onPreviewNativeEvent(event);
   }
   
   @Override
   public void onUnload()
   {
      super.onUnload();
      
      while (releaseOnDismiss_.size() > 0)
         releaseOnDismiss_.remove(0).removeHandler();
   }
   
   private void updateEditor(TextFileType fileType)
   { 
      String snippetText = getSnippetText(
                              fileType.getEditorLanguage().getModeName());
      docDisplay_.setCode(snippetText, false);
   }
   
   private static native String getSnippetText(String mode) /*-{
      var snippetId = "ace/snippets/" + mode;
      return $wnd.require(snippetId).snippetText;
   }-*/;
   
   private DockLayoutPanel panel_;
   private WidgetListBox<SnippetType> snippetTypes_;
   private DocDisplay docDisplay_;
   
   private final ArrayList<HandlerRegistration> releaseOnDismiss_ =
         new ArrayList<HandlerRegistration>();
   
   private EventBus events_;
   private FontSizeManager fontSizeManager_;
  

}
