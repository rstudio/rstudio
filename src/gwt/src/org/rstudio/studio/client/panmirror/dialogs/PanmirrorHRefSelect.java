/*
 * PanmirrorHRefSelect.java
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

import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkCapabilities;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkHeadingTarget;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkTargets;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkType;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;



public class PanmirrorHRefSelect extends Composite implements CanFocus
{
   public PanmirrorHRefSelect(PanmirrorLinkTargets targets, 
                              PanmirrorLinkCapabilities capabilities)
   {
      
      targets_ = targets;
      capabilities_ = capabilities;
       
      controls_ = new HorizontalPanel();
      controls_.setWidth("100%");
      
      type_ = new ListBox();
      type_.addItem("URL", Integer.toString(PanmirrorLinkType.URL));
      
      if (capabilities.headings && targets.headings.length > 0)
         type_.addItem("Heading", Integer.toString(PanmirrorLinkType.Heading));
      type_.addItem("ID",Integer.toString(PanmirrorLinkType.ID));
      controls_.add(type_);
      type_.getElement().getStyle().setMarginRight(6, Unit.PX);
      controls_.setCellHorizontalAlignment(type_, HasHorizontalAlignment.ALIGN_LEFT);
        
      hrefContainer_ = new SimplePanel();
      hrefContainer_.setWidth("100%");
      controls_.add(hrefContainer_);
      controls_.setCellWidth(hrefContainer_, "100%");
      
      href_ = new TextBox();
      href_.setWidth("100%");
      
      headings_ = new ListBox(); 
      headings_.setWidth("100%");
      for (PanmirrorLinkHeadingTarget target : targets_.headings)
      {
         if (target.text.trim().length() > 0)
            headings_.addItem(target.text);
      }
      
      ids_ = new ListBox();
      ids_.setWidth("100%");
      for (String id : targets_.ids)
      {
         ids_.addItem("#" + id);
      }
      
      syncHRefType();
      type_.addChangeHandler((event) -> {
         syncHRefType();
      });
      
      VerticalPanel container = new VerticalPanel();
      container.add(new Label("Link To"));
      container.add(controls_);
      
      initWidget(container);
   }
   
   
   public void setHRef(int type, String href)
   {
      if (type == PanmirrorLinkType.Heading && !capabilities_.headings) {
         type = PanmirrorLinkType.URL;
      }
      
      type_.setSelectedIndex(type);
      syncHRefType();
      
      if (getType() == PanmirrorLinkType.URL) 
      {
         href_.setText(href);
      }
      else if (getType() == PanmirrorLinkType.Heading)
      {
         for (int i = 0; i<targets_.headings.length; i++)
         {
            if (targets_.headings[i].text.equals(href))
            {
               headings_.setSelectedIndex(i);
               break;
            }
         }
      }
      else if (getType() == PanmirrorLinkType.ID)
      {
         for (int i = 0; i<targets_.ids.length; i++)
         {
            if (targets_.ids[i].equals(href))
            {
               ids_.setSelectedIndex(i);
               break;
            }
         }
      }  
   }
   
   public int getType()
   {
      return Integer.parseInt(type_.getSelectedValue());
   }
   
   public String getHRef()
   {
      int type = getType();
      if (type == PanmirrorLinkType.URL) 
      {
         return href_.getText().trim();
      }
      else if (type == PanmirrorLinkType.Heading)
      {
         return headings_.getSelectedItemText();
      }
      else if (type == PanmirrorLinkType.ID)
      {
         return ids_.getSelectedItemText();
      }  
      else
      {
         return "";
      }
   }
   
   
   public HandlerRegistration addTypeChangedHandler(ChangeHandler handler)
   {
      return type_.addChangeHandler(handler);
   }
   
   @Override
   public void focus()
   {
      if (hrefContainer_.getWidget() instanceof Focusable)
         FocusHelper.setFocusDeferred((Focusable)hrefContainer_.getWidget());
   }
   
   private void syncHRefType()
   {
      int type = getType();
      if (type == PanmirrorLinkType.URL) 
      {
         hrefContainer_.setWidget(href_);
      }
      else if (type == PanmirrorLinkType.Heading)
      {
         hrefContainer_.setWidget(headings_);
      }
      else if (type == PanmirrorLinkType.ID)
      {
         hrefContainer_.setWidget(ids_);
      }  
   }
   
 
   private final PanmirrorLinkTargets targets_;
   private final PanmirrorLinkCapabilities capabilities_;
   
   private final HorizontalPanel controls_;
   private final SimplePanel hrefContainer_;
   private final ListBox type_;
   private final TextBox href_; 
   private final ListBox headings_;
   private final ListBox ids_;
   
}
