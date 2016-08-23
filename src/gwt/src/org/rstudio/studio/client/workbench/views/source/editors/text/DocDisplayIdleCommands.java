/*
 * DocDisplayIdleCommands.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.MiniPopupPanel;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.views.files.model.FilesServerOperations;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplayIdleMonitor.IdleCommand;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplayIdleMonitor.IdleState;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer.ScreenCoordinates;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Token;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DocDisplayIdleCommands
{
   public DocDisplayIdleCommands()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      PREVIEW_LINK = previewLink();
   }
   
   // Private Methods ----
   
   @Inject
   private void initialize(FilesServerOperations filesServer)
   {
      filesServer_ = filesServer;
   }
   
   private IdleCommand previewLink()
   {
      return new IdleCommand()
      {
         @Override
         public void execute(DocDisplay docDisplay, IdleState idleState)
         {
            onPreviewLink(docDisplay, idleState);
         }
      };
   }
   
   private void onPreviewLink(DocDisplay docDisplay, IdleState idleState)
   {
      Position position = resolvePosition(docDisplay, idleState);
      Token token = docDisplay.getTokenAt(position);
      if (token == null)
         return;

      if (!token.hasAllTypes("href"))
         return;

      String href = token.getValue();
      if (href.endsWith(".png") ||
          href.endsWith(".jpg") ||
          href.endsWith(".jpeg") ||
          href.endsWith(".svg"))
      {
         onPreviewImage(docDisplay, href, position);
      }
   }
   
   private void onPreviewImage(DocDisplay docDisplay,
                               String href,
                               Position position)
   {
      if (href.startsWith("http"))
         onPreviewWebImage(docDisplay, href, position);
      else
         onPreviewServerImage(docDisplay, href, position);
   }
   
   private void onPreviewWebImage(DocDisplay docDisplay,
                                  String href,
                                  Position position)
   {
      ImageElement el = Document.get().createImageElement();
      el.setAttribute("src", href);
      el.getStyle().setProperty("maxWidth", "100px");
      
      ScreenCoordinates coordinates =
            docDisplay.documentPositionToScreenCoordinates(position);
      
      MiniPopupPanel panel = new MiniPopupPanel(true, false);
      panel.getElement().appendChild(el);
      panel.setPopupPosition(coordinates.getPageX(), coordinates.getPageY() + 20);
      panel.show();
   }
   
   private void onPreviewServerImage(DocDisplay docDisplay,
                                     String href,
                                     Position position)
   {
      String path = href.startsWith("~/")
            ? "files/" + href.substring(2)
            : "file_show?path=" + StringUtil.encodeURIComponent(href);
            
      ImageElement el = Document.get().createImageElement();
      el.setAttribute("src", path);
      el.getStyle().setProperty("maxWidth", "100px");
      
      ScreenCoordinates coordinates =
            docDisplay.documentPositionToScreenCoordinates(position);
      
      MiniPopupPanel panel = new MiniPopupPanel(true, false);
      panel.getElement().appendChild(el);
      panel.setPopupPosition(coordinates.getPageX(), coordinates.getPageY() + 20);
      panel.show();
      
   }
   
   private static Position resolvePosition(DocDisplay docDisplay, IdleState state)
   {
      int type = state.getType();
      if (type == IdleState.STATE_CURSOR_IDLE)
         return docDisplay.getCursorPosition();
      else if (type == IdleState.STATE_MOUSE_IDLE)
         return docDisplay.screenCoordinatesToDocumentPosition(state.getMouseX(), state.getMouseY());
      
      assert false : "Unhandled idle state type '" + type + "'";
      return Position.create(0, 0);
   }
   
   public final IdleCommand PREVIEW_LINK;
   
   // Injected ----
   private FilesServerOperations filesServer_;
}
