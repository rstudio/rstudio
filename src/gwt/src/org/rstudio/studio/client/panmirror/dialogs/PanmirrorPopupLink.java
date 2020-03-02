
/*
 * PanmirrorPopupLink.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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


package org.rstudio.studio.client.panmirror.dialogs;



import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.widget.HyperlinkLabel;
import org.rstudio.core.client.widget.ImageButton;
import org.rstudio.studio.client.RStudioGinjector;

import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class PanmirrorPopupLink extends PanmirrorPopup
{
   public PanmirrorPopupLink(Element parent, String url, int maxWidth, CommandWithArg<String> result)
   {
      // hookup gwt widget to existing html element
      super(parent);
      this.onAttach();
      RootPanel.detachOnWindowClose(this);
      
      
      HorizontalPanel panel = new HorizontalPanel();
      setWidget(panel);
     
      // adjust maxWidth for (estimated) space taken up by padding + buttons 
      maxWidth = maxWidth - 75;
      
      // link label
      HyperlinkLabel urlLabel = new HyperlinkLabel(url);
      urlLabel.addClickHandler((event) -> {
         RStudioGinjector.INSTANCE.getGlobalDisplay().openWindow(url);
      });
      
      /*
      urlLabel.addStyleName(RES.styles().linkLabel());
      urlLabel.getElement().getStyle().setProperty("maxWidth", maxWidth + "px");
      setClickHandler(urlLabel, () -> {
         RStudioGinjector.INSTANCE.getGlobalDisplay().openWindow(url);
      });
      */
      panel.add(urlLabel);
      
      /*
      // edit link button
      ImageButton editButton = createResultButton("Edit link attributes", RES.edit_link(), () -> {
         result.execute(kEditResult);
      });
      panel.add(editButton);
     
      // remove link button
      ImageButton removeButton = createResultButton("Remove link", RES.break_link(), () -> {
         result.execute(kRemoveResult);
      });
      panel.add(removeButton);  
      */
   }
   
   
   private ImageButton createResultButton(String description, ImageResource image, Command onClick)
   {
      ImageButton button = new ImageButton(description, image);
      setClickHandler(button, onClick);
      return button;
   }
   
   private void setClickHandler(Widget widget, Command onClick) 
   {
      Event.sinkEvents(widget.getElement(), Event.ONCLICK);
      Event.setEventListener(widget.getElement(), new EventListener() {
         @Override
         public void onBrowserEvent(Event event)
         {
            if (event.getTypeInt() == Event.ONCLICK)
               onClick.execute();
         }
         
      });
   }
  
   protected static final String kRemoveResult = "remove";
   protected static final String kEditResult = "edit";
   
   private static PanmirrorDialogsResources RES = PanmirrorDialogsResources.INSTANCE;

   
}

