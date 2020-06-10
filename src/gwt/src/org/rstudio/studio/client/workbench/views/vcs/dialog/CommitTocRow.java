/*
 * CommitTocRow.java
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
package org.rstudio.studio.client.workbench.views.vcs.dialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;

import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.RStudioGinjector;

public class CommitTocRow extends Composite implements HasClickHandlers
{
   public interface MyBinder extends UiBinder<FlowPanel, CommitTocRow>
   {}

   public CommitTocRow(String filename)
   {
      icon_ = RStudioGinjector.INSTANCE.getFileTypeRegistry().getIconForFilename(filename).getImage();
      icon_.addStyleName(ThemeStyles.INSTANCE.handCursor());
      anchor_ = new Anchor(filename);
      anchor_.addStyleName(ThemeStyles.INSTANCE.handCursor());

      initWidget(binder_.createAndBindUi(this));
   }

   @Override
   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return new HandlerRegistrations(
            icon_.addClickHandler(handler),
            anchor_.addClickHandler(handler));
   }


   @UiField(provided = true)
   Image icon_;
   @UiField(provided = true)
   Anchor anchor_;

   private static final MyBinder binder_ = GWT.create(MyBinder.class);
}
