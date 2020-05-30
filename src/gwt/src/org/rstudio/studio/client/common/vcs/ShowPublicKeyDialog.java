/*
 * ShowPublicKeyDialog.java
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
package org.rstudio.studio.client.common.vcs;

import com.google.gwt.aria.client.Id;
import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.command.KeyCombination;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ThemedButton;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ShowPublicKeyDialog extends ModalDialogBase
{
   public ShowPublicKeyDialog(String caption, String publicKey)
   {
      super(Roles.getDialogRole());
      publicKey_ = publicKey;
      
      setText(caption);
      
      setButtonAlignment(HasHorizontalAlignment.ALIGN_CENTER);
      
      ThemedButton closeButton = new ThemedButton("Close", event -> closeDialog());
      addOkButton(closeButton);
   }
   
   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel panel = new VerticalPanel();
      
      int mod = BrowseCap.hasMetaKey() ? KeyboardShortcut.META : 
                                         KeyboardShortcut.CTRL;
      String cmdText = new KeyCombination("c", 'C', mod).toString(true);
      HTML label = new HTML("Press " + cmdText + 
                            " to copy the key to the clipboard");
      label.addStyleName(RES.styles().viewPublicKeyLabel());
      panel.add(label);
      ElementIds.assignElementId(label, ElementIds.PUBLIC_KEY_LABEL);
      setARIADescribedBy(label.getElement());

      textArea_ = new TextArea();
      textArea_.setReadOnly(true);
      textArea_.setText(publicKey_);
      textArea_.addStyleName(RES.styles().viewPublicKeyContent());
      textArea_.setSize("400px", "250px");
      DomUtils.disableSpellcheck(textArea_);
      FontSizer.applyNormalFontSize(textArea_.getElement());
      ElementIds.assignElementId(textArea_, ElementIds.PUBLIC_KEY_TEXT);
      Roles.getTextboxRole().setAriaLabelledbyProperty(textArea_.getElement(),
         Id.of(label.getElement()));
      panel.add(textArea_);
      
      return panel;
   }
   
   @Override
   protected void onLoad()
   {
      super.onLoad();
     
      textArea_.selectAll();
   }

   @Override
   protected void focusInitialControl()
   {
      FocusHelper.setFocusDeferred(textArea_);
   }

   static interface Styles extends CssResource
   {
      String viewPublicKeyContent();
      String viewPublicKeyLabel();
   }
  
   static interface Resources extends ClientBundle
   {
      @Source("ShowPublicKeyDialog.css")
      Styles styles();
   }
   
   static Resources RES = GWT.create(Resources.class);
   public static void ensureStylesInjected()
   {
      RES.styles().ensureInjected();
   }
   
   private final String publicKey_;
   private TextArea textArea_;
}
