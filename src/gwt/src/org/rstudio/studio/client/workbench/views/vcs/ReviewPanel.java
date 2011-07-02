/*
 * ReviewPanel.java
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
package org.rstudio.studio.client.workbench.views.vcs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import org.rstudio.core.client.ValueSink;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.workbench.views.vcs.ReviewPresenter.Display;
import org.rstudio.studio.client.workbench.views.vcs.diff.Line;
import org.rstudio.studio.client.workbench.views.vcs.diff.LineTablePresenter;
import org.rstudio.studio.client.workbench.views.vcs.diff.LineTableView;
import org.rstudio.studio.client.workbench.views.vcs.diff.NavGutter;

import java.util.ArrayList;

public class ReviewPanel extends Composite implements Display
{
   private static class ListBoxAdapter implements HasValue<Integer>
   {
      private ListBoxAdapter(ListBox listBox)
      {
         listBox_ = listBox;
         listBox_.addChangeHandler(new ChangeHandler()
         {
            @Override
            public void onChange(ChangeEvent event)
            {
               ValueChangeEvent.fire(ListBoxAdapter.this, getValue());
            }
         });
      }

      @Override
      public Integer getValue()
      {
         return Integer.parseInt(
               listBox_.getValue(listBox_.getSelectedIndex()));
      }

      @Override
      public void setValue(Integer value)
      {
         setValue(value, true);
      }

      @Override
      public void setValue(Integer value, boolean fireEvents)
      {
         String valueStr = value.toString();
         for (int i = 0; i < listBox_.getItemCount(); i++)
         {
            if (listBox_.getValue(i).equals(valueStr))
            {
               listBox_.setSelectedIndex(i);
               break;
            }
         }
      }

      @Override
      public HandlerRegistration addValueChangeHandler(ValueChangeHandler<Integer> handler)
      {
         return handlers_.addHandler(ValueChangeEvent.getType(), handler);
      }

      @Override
      public void fireEvent(GwtEvent<?> event)
      {
         handlers_.fireEvent(event);
      }

      private final ListBox listBox_;
      private final HandlerManager handlers_ = new HandlerManager(this);
   }


   interface Binder extends UiBinder<Widget, ReviewPanel>
   {
   }

   @Inject
   public ReviewPanel(ChangelistTable changelist,
                      LineTableView diffPane)
   {
      stageButton_ = new ThemedButton("Stage");
      discardButton_ = new ThemedButton("Discard");
      unstageButton_ = new ThemedButton("Unstage");
      changelist_ = changelist;
      lines_ = diffPane;

      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));

      listBoxAdapter_ = new ListBoxAdapter(contextLines_);
   }

   @Override
   public HasClickHandlers getStageButton()
   {
      return stageButton_;
   }

   @Override
   public HasClickHandlers getDiscardButton()
   {
      return discardButton_;
   }

   @Override
   public HasClickHandlers getUnstageButton()
   {
      return unstageButton_;
   }

   @Override
   public HasValue<Boolean> getStagedCheckBox()
   {
      return stagedCheckBox_;
   }

   @Override
   public LineTablePresenter.Display getLineTableDisplay()
   {
      return lines_;
   }

   @Override
   public ChangelistTable getChangelistTable()
   {
      return changelist_;
   }

   @Override
   public ValueSink<ArrayList<Line>> getGutter()
   {
      return gutter_;
   }

   @Override
   public HasValue<Integer> getContextLines()
   {
      return listBoxAdapter_;
   }

   @UiField(provided = true)
   ThemedButton stageButton_;
   @UiField(provided = true)
   ThemedButton discardButton_;
   @UiField(provided = true)
   ThemedButton unstageButton_;
   @UiField(provided = true)
   ChangelistTable changelist_;
   @UiField
   CheckBox stagedCheckBox_;
   @UiField(provided = true)
   LineTableView lines_;
   @UiField
   NavGutter gutter_;
   @UiField
   ListBox contextLines_;
   private ListBoxAdapter listBoxAdapter_;
}
