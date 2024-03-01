/*
 * CopilotDiagnosticsDialog.java
 *
 * Copyright (C) 2024 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.copilot.ui;

import org.rstudio.core.client.CoreClientConstants;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.Markdown;
import org.rstudio.core.client.dom.Clipboard;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.workbench.copilot.CopilotUIConstants;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;

public class CopilotDiagnosticsDialog extends ModalDialogBase
{
   public CopilotDiagnosticsDialog(String markdownContent)
   {
      super(Roles.getDialogRole());
      setSize("560px", "460px");
      String htmlContent = Markdown.markdownToHtml(markdownContent);
      
      html_ = new HTML(htmlContent);
      html_.addStyleName(RES.styles().container());
      
      container_ = new ScrollPanel();
      container_.setSize("560px", "460px");
      container_.add(html_);
      
      closeButton_ = new ThemedButton(coreConstants_.closeText());
      closeButton_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            hide();
         }
      });
      
      copyButton_ = new ThemedButton("Copy to Clipboard");
      copyButton_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            if (Desktop.isDesktop())
            {
               Desktop.getFrame().setClipboardText(markdownContent);
            }
            else
            {
               Clipboard.setText(markdownContent);
            }
         }
      });
      
      setText(constants_.copilotDiagnosticsTitle());
      addLeftButton(copyButton_, ElementIds.COPILOT_DIAGNOSTICS_COPY_BUTTON);
      addButton(closeButton_, ElementIds.COPILOT_DIAGNOSTICS_CLOSE_BUTTON);
   }
   
   @Override
   protected Widget createMainWidget()
   {
      return container_;
   }

   private final ScrollPanel container_;
   private final HTML html_;
   private final ThemedButton closeButton_;
   private final ThemedButton copyButton_;
   
   
   // Boilerplate ----
   
   public interface Styles extends CssResource
   {
      String container();
   }

   public interface Resources extends ClientBundle
   {
      @Source("CopilotDiagnosticsDialog.css")
      Styles styles();
   }

   private static Resources RES = GWT.create(Resources.class);
   static
   {
      RES.styles().ensureInjected();
   }

   private static final CoreClientConstants coreConstants_ = GWT.create(CoreClientConstants.class);
   private static final CopilotUIConstants constants_ = GWT.create(CopilotUIConstants.class);
}
