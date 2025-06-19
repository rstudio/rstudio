/*
 * FindOutputPane.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.views.output.find;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.DebouncedCommand;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.*;
import org.rstudio.core.client.widget.events.SelectionChangedEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.output.OutputConstants;
import org.rstudio.studio.client.workbench.views.output.find.model.FindResult;
import org.rstudio.studio.client.workbench.views.output.find.events.PreviewReplaceEvent;

import java.util.ArrayList;


public class FindOutputPane extends WorkbenchPane
      implements FindOutputPresenter.Display
{
   @Inject
   public FindOutputPane(Commands commands,
                         EventBus eventBus)
   {
      super(constants_.findResultsTitle());
      commands_ = commands;
      eventBus_ = eventBus;
      ensureWidget();
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar(constants_.findOutputTabLabel());

      searchLabel_ = new Label();
      toolbar.addLeftWidget(searchLabel_);

      stopSearch_ = new ToolbarButton(
            ToolbarButton.NoText,
            constants_.stopFindInFilesTitle(),
            commands_.interruptR().getImageResource());
      toolbar.addRightWidget(stopSearch_);

      refreshButton_ = commands_.refreshFindInFiles().createToolbarButton();
      refreshButton_.addStyleName(ThemeStyles.INSTANCE.refreshToolbarButton());
      toolbar.addRightWidget(refreshButton_);
      setStopSearchButtonVisible(false);

      showFindButton_ = new LeftRightToggleButton(constants_.findLabel(), constants_.replaceLabel(), true);
      showFindButton_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            if (!replaceMode_)
               turnOnReplaceMode();
         }
      });
      toolbar.addRightWidget(showFindButton_);

      showReplaceButton_ = new LeftRightToggleButton(constants_.findLabel(), constants_.replaceLabel(), false);
      showReplaceButton_.setVisible(false);
      showReplaceButton_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            if (replaceMode_)
               turnOffReplaceMode();
         }
      });
      toolbar.addRightWidget(showReplaceButton_);

      return toolbar;
   }

   @Override
   protected SecondaryToolbar createSecondaryToolbar()
   {
      SecondaryToolbar replaceToolbar = new SecondaryToolbar(constants_.replaceLabel());
      replaceMode_ = true;

      replaceTextBox_ = new TextBox();
      replaceTextBox_.addKeyUpHandler(new KeyUpHandler()
      {
         public void onKeyUp(KeyUpEvent event)
         {
            displayPreview_.nudge();
         }
      });
      FormLabel replaceLabel = new FormLabel(constants_.replaceWithLabel(), replaceTextBox_);
      replaceToolbar.addLeftWidget(replaceLabel);
      replaceToolbar.addLeftWidget(replaceTextBox_);

      stopReplace_ = new ToolbarButton(
            ToolbarButton.NoText,
            constants_.stopReplaceTitle(),
            commands_.interruptR().getImageResource());
      replaceToolbar.addRightWidget(stopReplace_);
      setStopReplaceButtonVisible(false);

      replaceAllButton_ = new ToolbarButton(constants_.replaceAllText(), constants_.replaceAllText(), null);
      replaceToolbar.addRightWidget(replaceAllButton_);

      replaceProgress_ = new ProgressBar();
      replaceProgress_.setHeight("10px");
      replaceProgress_.setWidth("195px");
      replaceProgress_.setVisible(false);
      replaceToolbar.addLeftWidget(replaceProgress_);

      return replaceToolbar;
   }

   @Override
   protected Widget createMainWidget()
   {
      context_ = new FindResultContext();

      FindOutputResources resources = GWT.create(FindOutputResources.class);
      resources.styles().ensureInjected();

      table_ = new FastSelectTable<>(
            new FindOutputCodec(resources),
            resources.styles().selectedRow(),
            true,
            false,
            constants_.findInFilesResultsTitle());
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

   @Override
   public void setRegexPreviewMode(boolean value)
   {
      regexPreviewMode_ = value;
   }

   @Override
   public void setReplaceMode(boolean value)
   {
      FindOutputResources resources = GWT.create(FindOutputResources.class);
      if (value)
         table_.addStyleName(resources.styles().findOutputReplace());
      else
      {
         table_.removeStyleName(resources.styles().findOutputReplace());
         addReplaceMatches("");
      }
      // this needs to be done after addReplaceMatches is called
      replaceMode_ = value;
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
         statusPanel_.setStatusText(constants_.noResultsFoundText());
   }

   @Override
   public void onSelected()
   {
      super.onSelected();

      if (!regexPreviewMode_)
      {
         table_.focus();
         ArrayList<Integer> indices = table_.getSelectedRowIndexes();
         if (indices.isEmpty())
            table_.selectNextRow();
      }
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
      // only one of the stop search and refresh search buttons should show in the toolbar at a time
      // when search is in progress, stopSearch_ will be visible and refreshButton_ will be hidden
      stopSearch_.setVisible(visible);
      refreshButton_.setVisible(!visible);
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
   public HandlerRegistration addSelectionChangedHandler(SelectionChangedEvent.Handler handler)
   {
      return table_.addSelectionChangedHandler(handler);
   }

   @Override
   public void showOverflow()
   {
      if (overflow_)
         return;
      overflow_ = true;
      ArrayList<FindResult> items = new ArrayList<>();
      items.add(null);
      table_.addItems(items, false);
   }

   @Override
   public void updateSearchLabel(String query, String path, boolean wholeWord)
   {
      String intro = wholeWord ? constants_.resultsForWholeWordText() : constants_.resultsForText();
      SafeHtmlBuilder builder = new SafeHtmlBuilder();
      builder.appendEscaped(intro)
            .appendHtmlConstant("<strong>")
            .appendEscaped(query)
            .appendHtmlConstant("</strong>")
            .appendEscaped(" " + constants_.inText())
            .appendEscaped(path);
      searchLabel_.getElement().setInnerHTML(builder.toSafeHtml().asString());
   }

   @Override
   public void updateSearchLabel(String query, String path, String replace, boolean wholeWord)
   {
      String intro = wholeWord ? constants_.replaceResultsWholeWordText() : constants_.replaceResultsForText();
      SafeHtmlBuilder builder = new SafeHtmlBuilder();
      builder.appendEscaped(intro)
            .appendHtmlConstant("<strong>")
            .appendEscaped(query)
            .appendHtmlConstant("</strong>")
            .appendEscaped(" " + constants_.withText())
            .appendHtmlConstant("<strong>")
            .appendEscaped(replace)
            .appendHtmlConstant("</strong>")
            .appendEscaped(" " + constants_.inText())
            .appendEscaped(path);
      searchLabel_.getElement().setInnerHTML(builder.toSafeHtml().asString());
   }

   @Override
   public void updateSearchLabel(String query, String path, String replace,
                                 boolean wholeWord, int successCount, int errorCount)
   {
      String intro = wholeWord ? constants_.replaceResultsWholeWordText() : constants_.replaceResultsForText();
      SafeHtmlBuilder builder = new SafeHtmlBuilder();
      builder.appendEscaped(intro)
            .appendHtmlConstant("<strong>")
            .appendEscaped(query)
            .appendHtmlConstant("</strong>")
            .appendEscaped(" " + constants_.withText())
            .appendHtmlConstant("<strong>")
            .appendEscaped(replace)
            .appendHtmlConstant("</strong>")
            .appendEscaped(" " + constants_.inText())
            .appendEscaped(path);
      {
         String summary = constants_.summaryLabel(successCount, errorCount);
         builder.appendEscaped(summary);
      }
      searchLabel_.getElement().setInnerHTML(builder.toSafeHtml().asString());
   }

   @Override
   public void clearSearchLabel()
   {
      searchLabel_.setText("");
   }

   @Override
   public HandlerRegistration addSelectionCommitHandler(
      SelectionCommitEvent.Handler<CodeNavigationTarget> handler)
   {
      return addHandler(handler, SelectionCommitEvent.getType());
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
   public HasClickHandlers getStopReplaceButton()
   {
      return stopReplace_;
   }

   @Override
   public void setStopReplaceButtonVisible(boolean visible)
   {
      stopReplace_.setVisible(visible);
      refreshButton_.setVisible(!visible);
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
      replaceTextBox_.setReadOnly(true);
      replaceAllButton_.setEnabled(false);
   }

   @Override
   public void turnOnReplaceMode()
   {
      showFindButton_.setVisible(false);
      showReplaceButton_.setVisible(true);
      setReplaceMode(true);
      setSecondaryToolbarVisible(true);
      if (displayPreview_ == null)
         createDisplayPreview();
      if (!replaceTextBox_.getValue().isEmpty())
         displayPreview_.nudge();
   }

   @Override
   public void turnOffReplaceMode()
   {
      showFindButton_.setVisible(true);
      showReplaceButton_.setVisible(false);
      setSecondaryToolbarVisible(false);
      if (!replaceTextBox_.getValue().isEmpty())
         addReplaceMatches("");
      setReplaceMode(false);
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

   private void createDisplayPreview()
   {
      displayPreview_ = new DebouncedCommand(500)
      {
         @Override
         protected void execute()
         {
            setReplaceMode(true);
            if (getRegexPreviewMode())
               eventBus_.fireEvent(new PreviewReplaceEvent(replaceTextBox_.getValue()));
            else
               addReplaceMatches(replaceTextBox_.getValue());
         }
      };
   }

   private void fireSelectionCommitted()
   {
      ArrayList<CodeNavigationTarget> values = table_.getSelectedValues();
      if (values.size() == 1)
         SelectionCommitEvent.fire(this, values.get(0));
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

   private FastSelectTable<FindResult, CodeNavigationTarget, Object> table_;
   private FindResultContext context_;
   private final Commands commands_;
   private final EventBus eventBus_;
   private Label searchLabel_;
   private ToolbarButton stopSearch_;
   private ToolbarButton refreshButton_;
   private SimplePanel container_;
   private ScrollPanel scrollPanel_;
   private StatusPanel statusPanel_;
   private boolean overflow_ = false;
   private int matchCount_;

   private LeftRightToggleButton showFindButton_;
   private LeftRightToggleButton showReplaceButton_;

   private boolean replaceMode_;
   private boolean regexPreviewMode_;

   private TextBox replaceTextBox_;
   private ToolbarButton replaceAllButton_;

   private ToolbarButton stopReplace_;
   private ProgressBar replaceProgress_;

   private DebouncedCommand displayPreview_;

   // This must be the same as MAX_COUNT in SessionFind.cpp
   private static final int MAX_COUNT = 1000;
   private static final OutputConstants constants_ = GWT.create(OutputConstants.class);
}
