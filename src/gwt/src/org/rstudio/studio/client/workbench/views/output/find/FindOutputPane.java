/*
 * FindOutputPane.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.output.find;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.DebouncedCommand;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.widget.*;
import org.rstudio.core.client.widget.events.SelectionChangedHandler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.output.find.model.FindResult;
import org.rstudio.studio.client.workbench.views.output.find.events.PreviewReplaceEvent;

import java.util.ArrayList;


public class FindOutputPane extends WorkbenchPane
      implements FindOutputPresenter.Display,
                 HasSelectionCommitHandlers<CodeNavigationTarget>
{
   @Inject
   public FindOutputPane(Commands commands,
                         EventBus eventBus)
   {
      super("Find Results");
      commands_ = commands;
      eventBus_ = eventBus;
      ensureWidget();
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar("Find Output Tab");

      searchLabel_ = new Label();
      toolbar.addLeftWidget(searchLabel_);

      FindOutputResources resources = GWT.create(FindOutputResources.class);
      viewReplaceButton_ = new ToolbarButton("Replace", "Replace",
                                             resources.expandReplaceIcon());
      toolbar.addRightWidget(viewReplaceButton_);
      viewReplaceButton_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            toggleReplaceToolbar();
            toggleChevron();
            if (replaceMode_ ||
                !replaceTextBox_.getValue().isEmpty())
            {
               if (secondaryToolbar_.isVisible())
               {
                  toggleReplaceMode();
                  addReplaceMatches(replaceTextBox_.getValue());
               }
               else if (replaceMode_)
                  toggleReplaceMode();
               else if (!replaceTextBox_.getValue().isEmpty())
                  addReplaceMatches(new String());
            }
         }
      });

      stopSearch_ = new ToolbarButton(
            ToolbarButton.NoText,
            "Stop find in files",
            commands_.interruptR().getImageResource());
      stopSearch_.setVisible(false);
      toolbar.addRightWidget(stopSearch_);

      return toolbar;
   }

   @Override
   protected SecondaryToolbar createSecondaryToolbar()
   {
      replaceToolbar_ = new SecondaryToolbar("Replace");
      replaceMode_ = true;

      replaceLabel_ = new Label();
      replaceToolbar_.addLeftWidget(replaceLabel_);

      replaceTextBox_ = new TextBoxWithCue("Replace all");
      replaceToolbar_.addLeftWidget(replaceTextBox_);
      replaceTextBox_.addKeyUpHandler(new KeyUpHandler()
      {
         public void onKeyUp(KeyUpEvent event)
         {
            createDisplayPreview();
            displayPreview_.nudge();
         }
      });

      regexCheckbox_ = new CheckBox();
      regexCheckboxLabel_ =
                     new CheckboxLabel(regexCheckbox_, "Regular expression").getLabel();
      regexCheckbox_.getElement().getStyle().setMarginLeft(9, Unit.PX);
      regexCheckbox_.getElement().getStyle().setMarginRight(0, Unit.PX);
      replaceToolbar_.addLeftWidget(regexCheckbox_);
      regexCheckboxLabel_.getElement().getStyle().setMarginRight(9, Unit.PX);
      replaceToolbar_.addLeftWidget(regexCheckboxLabel_);
      regexCheckbox_.addValueChangeHandler(new ValueChangeHandler<Boolean>()
      {
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            if (replaceMode_ && !replaceTextBox_.getValue().isEmpty())
            {
               addReplaceMatches(new String());
               if (regexCheckbox_.getValue())
                  eventBus_.fireEvent(new PreviewReplaceEvent(replaceTextBox_.getValue()));
               else
               {
                  // if we've previously done a regex preview, the display content has been modified
                  // and needs to be regenerated
                  if (getRegexPreviewMode())
                  {
                     toggleRegexPreviewMode();
                     eventBus_.fireEvent(new PreviewReplaceEvent(new String()));
                  }
                  addReplaceMatches(replaceTextBox_.getValue());
               }
            }
         }
      });

      useGitIgnore_ = new CheckBox();
      useGitIgnoreLabel_ =
                     new CheckboxLabel(useGitIgnore_, "Use .gitignore").getLabel();
      useGitIgnore_.getElement().getStyle().setMarginRight(0, Unit.PX);
      replaceToolbar_.addLeftWidget(useGitIgnore_);
      useGitIgnoreLabel_.getElement().getStyle().setMarginRight(9, Unit.PX);
      replaceToolbar_.addLeftWidget(useGitIgnoreLabel_);

      stopReplace_ = new ToolbarButton(
            ToolbarButton.NoText,
            "Stop replace",
            commands_.interruptR().getImageResource());
      stopReplace_.setVisible(false);
      replaceToolbar_.addRightWidget(stopReplace_);

      replaceAllButton_ = new ToolbarButton("Replace All", "Replace All", null);
      replaceToolbar_.addRightWidget(replaceAllButton_);

      // don't adjust width without considering stop button
      replaceProgress_ = new ProgressBar();
      replaceProgress_.setHeight("10px");
      replaceProgress_.setWidth("195px");
      replaceProgress_.setVisible(false);
      replaceToolbar_.addLeftWidget(replaceProgress_);

      return replaceToolbar_;
   }

   @Override
   protected Widget createMainWidget()
   {
      context_ = new FindResultContext();

      FindOutputResources resources = GWT.create(FindOutputResources.class);
      resources.styles().ensureInjected();

      table_ = new FastSelectTable<FindResult, CodeNavigationTarget, Object>(
            new FindOutputCodec(resources),
            resources.styles().selectedRow(),
            true,
            false);
      FontSizer.applyNormalFontSize(table_);
      table_.addStyleName(resources.styles().findOutput());
      table_.addClickHandler(new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            if (event.getNativeButton() != NativeEvent.BUTTON_LEFT)
               return;

            if (dblClick_.checkForDoubleClick(event.getNativeEvent()))
               fireSelectionCommitted();
         }

         private final DoubleClickState dblClick_ = new DoubleClickState();
      });

      table_.addKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
               fireSelectionCommitted();
            event.stopPropagation();
            event.preventDefault();
         }
      });

      replaceMode_ = false;
      setSecondaryToolbarVisible(replaceMode_);

      container_ =  new SimplePanel();
      container_.addStyleName("ace_editor_theme");
      container_.setSize("100%", "100%");
      statusPanel_ = new StatusPanel();
      statusPanel_.setSize("100%", "100%");
      scrollPanel_ = new ScrollPanel(table_);
      scrollPanel_.setSize("100%", "100%");
      container_.setWidget(scrollPanel_);
      return container_;
   }

   private void createDisplayPreview()
   {
      if (displayPreview_ == null)
      {
         displayPreview_ = new DebouncedCommand(50)
         {
            @Override
            protected void execute()
            {
               if (!replaceMode_)
                  toggleReplaceMode();
               // !!! currently the preview re runs the 'find' backend logic, we may
               // want to simplify this to just do the preview without rerunning find
               if (regexCheckbox_.getValue())
                  eventBus_.fireEvent(new PreviewReplaceEvent(replaceTextBox_.getValue()));
               else
                  addReplaceMatches(replaceTextBox_.getValue());
            }
         };
      }
   }

   private void toggleReplaceToolbar()
   {
      boolean isToolbarVisible =  secondaryToolbar_.isVisible();
      setSecondaryToolbarVisible(!isToolbarVisible);
   }

   private void toggleChevron()
   {
     FindOutputResources resources = GWT.create(FindOutputResources.class);
     if (secondaryToolbar_.isVisible())
        viewReplaceButton_.setLeftImage(resources.collapseReplaceIcon());
     else
        viewReplaceButton_.setLeftImage(resources.expandReplaceIcon());
   }

   private void fireSelectionCommitted()
   {
      ArrayList<CodeNavigationTarget> values = table_.getSelectedValues();
      if (values.size() == 1)
         SelectionCommitEvent.fire(this, values.get(0));
   }

   @Override
   public void toggleRegexPreviewMode()
   {
      regexPreviewMode_ = !regexPreviewMode_;
   }

   @Override
   public void toggleReplaceMode()
   {
      replaceMode_ = !replaceMode_;
      FindOutputResources resources = GWT.create(FindOutputResources.class);
      if (replaceMode_)
         table_.addStyleName(resources.styles().findOutputReplace());
      else
      {
         table_.removeStyleName(resources.styles().findOutputReplace());
         addReplaceMatches(new String());
      }
   }

   @Override
   public void addMatches(ArrayList<FindResult> findResults)
   {
      int matchesToAdd = Math.min(findResults.size(), MAX_COUNT - matchCount_);

      if (matchesToAdd > 0)
      {
         matchCount_ += matchesToAdd;
         
         if (matchCount_ > 0 && container_.getWidget() != scrollPanel_)
            container_.setWidget(scrollPanel_);
            
         if (!replaceMode_ || regexPreviewMode_)
            context_.addMatches(findResults.subList(0, matchesToAdd));
         table_.addItems(findResults.subList(0, matchesToAdd), false);
      }
      
      if (matchCount_ >= MAX_COUNT)
         showOverflow();
   }

   public void addReplaceMatches(String value)
   {
      table_.clear();
      matchCount_ = 0;
      context_.updateFileMatches(value);
      addMatches(context_.getFindResults());
   }

   @Override
   public void clearMatches()
   {
      context_.reset();
      table_.clear();
      overflow_ = false;
      matchCount_ = 0;
      statusPanel_.setStatusText("");
      container_.setWidget(statusPanel_);
   }
   
   @Override
   public void showSearchCompleted()
   {
      if (matchCount_ == 0)
         statusPanel_.setStatusText("(No results found)");
   }
   
   @Override
   public void onSelected()
   {
      super.onSelected();
      
      table_.focus();
      ArrayList<Integer> indices = table_.getSelectedRowIndexes();
      if (indices.isEmpty())
         table_.selectNextRow();
   }

   @Override
   public void ensureVisible(boolean activate)
   {
      fireEvent(new EnsureVisibleEvent(activate));
   }

   @Override
   public HasClickHandlers getStopSearchButton()
   {
      return stopSearch_;
   }

   @Override
   public void setStopSearchButtonVisible(boolean visible)
   {
      stopSearch_.setVisible(visible);
   }

   @Override
   public void ensureSelectedRowIsVisible()
   {
      ArrayList<TableRowElement> rows = table_.getSelectedRows();
      if (rows.size() > 0)
      {
         DomUtils.ensureVisibleVert(scrollPanel_.getElement(),
                                    rows.get(0),
                                    20);
      }
   }

   @Override
   public HandlerRegistration addSelectionChangedHandler(SelectionChangedHandler handler)
   {
      return table_.addSelectionChangedHandler(handler);
   }

   @Override
   public void showOverflow()
   {
      if (overflow_)
         return;
      overflow_ = true;
      ArrayList<FindResult> items = new ArrayList<FindResult>();
      items.add(null);
      table_.addItems(items, false);
   }

   @Override
   public void updateSearchLabel(String query, String path)
   {
      SafeHtmlBuilder builder = new SafeHtmlBuilder();
      builder.appendEscaped("Results for ")
            .appendHtmlConstant("<strong>")
            .appendEscaped(query)
            .appendHtmlConstant("</strong>")
            .appendEscaped(" in ")
            .appendEscaped(path);
      searchLabel_.getElement().setInnerHTML(builder.toSafeHtml().asString());
   }

   @Override
   public void updateSearchLabel(String query, String path, String replace)
   {
      SafeHtmlBuilder builder = new SafeHtmlBuilder();
      builder.appendEscaped("Replace results for ")
            .appendHtmlConstant("<strong>")
            .appendEscaped(query)
            .appendHtmlConstant("</strong>")
            .appendEscaped(" with ")
            .appendHtmlConstant("<strong>")
            .appendEscaped(replace)
            .appendHtmlConstant("</strong>")
            .appendEscaped(" in ")
            .appendEscaped(path);
      searchLabel_.getElement().setInnerHTML(builder.toSafeHtml().asString());
   }
      
   @Override
   public void clearSearchLabel()
   {
      searchLabel_.setText("");
   }

   @Override
   public HandlerRegistration addSelectionCommitHandler(SelectionCommitHandler<CodeNavigationTarget> handler)
   {
      return addHandler(handler, SelectionCommitEvent.getType());
   }

   private class StatusPanel extends HorizontalCenterPanel
   {
      public StatusPanel()
      {
         super(new Label(), 50);
         label_ = (Label)getWidget();
         
      }
      
      public void setStatusText(String status)
      {
         label_.setText(status);
      }
      
      
      private final Label label_;
      
      }
   
   @Override
   public boolean getRegexPreviewMode()
   {
      return regexPreviewMode_;
   }

   @Override
   public boolean getReplaceMode()
   {
      return replaceMode_;
   }

   @Override
   public HasClickHandlers getReplaceAllButton()
   {
      return replaceAllButton_;
   }

   @Override
   public String getReplaceText()
   {
      return replaceTextBox_.getValue();
   }

   @Override
   public boolean isReplaceRegex()
   {
      return regexCheckbox_.getValue();
   }

   @Override
   public boolean useGitIgnore()
   {
      return useGitIgnore_.getValue();
   }

   @Override
   public HasClickHandlers getStopReplaceButton()
   {
      return stopReplace_;
   }

   @Override
   public void setStopReplaceButtonVisible(boolean visible)
   {
      stopReplace_.setVisible(visible);
   }

   @Override
   public ProgressBar getProgress()
   {
      return replaceProgress_;
   }

   @Override
   public void enableReplace()
   {
      replaceTextBox_.setReadOnly(false);
      replaceAllButton_.setEnabled(true);
   }

   @Override
   public void disableReplace()
   {
      replaceTextBox_.setValue("");
      replaceTextBox_.setReadOnly(true);
      replaceAllButton_.setEnabled(false);
   }

   @Override
   public void showProgress()
   {
      if (!replaceProgress_.isVisible())
         replaceProgress_.setVisible(true);
   }

   @Override
   public void hideProgress()
   {
      if (replaceProgress_.isVisible())
         replaceProgress_.setVisible(false);
   }
   
   private FastSelectTable<FindResult, CodeNavigationTarget, Object> table_;
   private FindResultContext context_;
   private final Commands commands_;
   private final EventBus eventBus_;
   private Label searchLabel_;
   private ToolbarButton stopSearch_;
   private SimplePanel container_;
   private ScrollPanel scrollPanel_;
   private StatusPanel statusPanel_;
   private boolean overflow_ = false;
   private int matchCount_;

   private SecondaryToolbar replaceToolbar_;
   private ToolbarButton viewReplaceButton_;
   private boolean regexPreviewMode_;
   private boolean replaceMode_;
   private Label replaceLabel_;
   private CheckBox regexCheckbox_;
   private Label regexCheckboxLabel_;
   private CheckBox useGitIgnore_;
   private Label useGitIgnoreLabel_;
   private DebouncedCommand displayPreview_;
   private TextBoxWithCue replaceTextBox_;
   private ToolbarButton replaceAllButton_;
   private ToolbarButton stopReplace_;
   private ProgressBar replaceProgress_;

   // This must be the same as MAX_COUNT in SessionFind.cpp
   private static final int MAX_COUNT = 1000;
}
