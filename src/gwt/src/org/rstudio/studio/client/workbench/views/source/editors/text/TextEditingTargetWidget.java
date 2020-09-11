/*
 * TextEditingTargetWidget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.KeyCombination;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.EnsureHeightEvent;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.MouseDragHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.layout.RequiresVisibilityChanged;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.FilePathUtils;
import org.rstudio.studio.client.common.ImageMenuItem;
import org.rstudio.studio.client.common.dependencies.model.Dependency;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.plumber.model.PlumberAPIParams;
import org.rstudio.studio.client.plumber.ui.PlumberViewerTypePopupMenu;
import org.rstudio.studio.client.rmarkdown.events.RenderRmdEvent;
import org.rstudio.studio.client.rmarkdown.events.RmdOutputFormatChangedEvent;
import org.rstudio.studio.client.rsconnect.RSConnect;
import org.rstudio.studio.client.rsconnect.ui.RSConnectPublishButton;
import org.rstudio.studio.client.shiny.model.ShinyApplicationParams;
import org.rstudio.studio.client.shiny.ui.ShinyTestPopupMenu;
import org.rstudio.studio.client.shiny.ui.ShinyViewerTypePopupMenu;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionUtils;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.edit.ui.EditDialog;
import org.rstudio.studio.client.workbench.views.source.DocumentOutlineWidget;
import org.rstudio.studio.client.workbench.views.source.PanelWithToolbars;
import org.rstudio.studio.client.workbench.views.source.SourceColumn;
import org.rstudio.studio.client.workbench.views.source.SourceColumnManager;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTargetToolbar;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget.Display;
import org.rstudio.studio.client.workbench.views.source.editors.text.findreplace.FindReplaceBar;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.TextEditingTargetNotebook;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBar;
import org.rstudio.studio.client.workbench.views.source.editors.text.status.StatusBarWidget;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

public class TextEditingTargetWidget
      extends ResizeComposite
      implements Display, RequiresVisibilityChanged
{
   public TextEditingTargetWidget(final TextEditingTarget target,
                                  DocUpdateSentinel docUpdateSentinel,
                                  Commands commands,
                                  UserPrefs userPrefs,
                                  UserState userState,
                                  FileTypeRegistry fileTypeRegistry,
                                  final DocDisplay editor,
                                  TextFileType fileType,
                                  String extendedType,
                                  EventBus events,
                                  Session session,
                                  SourceColumn column)
   {
      target_ = target;
      docUpdateSentinel_ = docUpdateSentinel;
      commands_ = commands;
      userPrefs_ = userPrefs;
      userState_ = userState;
      session_ = session;
      column_ = column;
      fileTypeRegistry_ = fileTypeRegistry;
      editor_ = editor;
      extendedType_ = extendedType;
      events_ = events;
      sourceOnSave_ = new CheckBox();
      srcOnSaveLabel_ = new CheckboxLabel(sourceOnSave_, "Source on Save").getLabel();
      statusBar_ = new StatusBarWidget();
      shinyViewerMenu_ = RStudioGinjector.INSTANCE.getShinyViewerTypePopupMenu();
      shinyTestMenu_ = RStudioGinjector.INSTANCE.getShinyTestPopupMenu();
      plumberViewerMenu_ = RStudioGinjector.INSTANCE.getPlumberViewerTypePopupMenu();
      handlerManager_ = new HandlerManager(this);

      findReplace_ = new TextEditingTargetFindReplace(
         new TextEditingTargetFindReplace.Container()
         {
            @Override
            public AceEditor getEditor()
            {
               return (AceEditor)editor_;
            }

            @Override
            public void insertFindReplace(FindReplaceBar findReplaceBar)
            {
               Widget beforeWidget = null;
               if (warningBar_ != null && warningBar_.isAttached())
                  beforeWidget = warningBar_;
               panel_.insertNorth(findReplaceBar,
                                  findReplaceBar.getHeight(),
                                  beforeWidget);

            }

            @Override
            public void removeFindReplace(FindReplaceBar findReplaceBar)
            {
               panel_.remove(findReplaceBar);
            }
         });

      editorPanel_ = new DockLayoutPanel(Unit.PX);
      docOutlineWidget_ = new DocumentOutlineWidget(target);

      editorPanel_.addEast(docOutlineWidget_, 0);
      editorPanel_.add(editor.asWidget());


      MouseDragHandler.addHandler(
         docOutlineWidget_.getLeftSeparator(),
         new DockPanelSidebarDragHandler(editorPanel_, docOutlineWidget_) {

            @Override
            public void onResized(boolean visible)
            {
               setDocOutlineLatchState(visible);
               editor_.onResize();
            }

            @Override
            public void onPreferredWidth(double width)
            {
               target_.setPreferredOutlineWidgetSize(width);
            }

            @Override
            public void onPreferredVisibility(boolean visible)
            {
               target_.setPreferredOutlineWidgetVisibility(visible);
            }
         }
      );

      // setup editing container (only activate editor if we are not in visual mode)
      editorContainer_ = new TextEditorContainer(sourceEditor_);
      if (!isVisualMode())
        editorContainer_.activateEditor();

      panel_ = new PanelWithToolbars(
            toolbar_ = createToolbar(fileType),
            editorContainer_,
            statusBar_);

      Roles.getTabpanelRole().set(panel_.getElement());
      setAccessibleName(null);
      adaptToFileType(fileType);

      editor.addFocusHandler(event -> initWidgetSize());

      editor_.setTextInputAriaLabel("Text editor");

      initWidget(panel_);

      // Update wrap mode on the editor when the soft wrap property changes
      docUpdateSentinel_.addPropertyValueChangeHandler(
            TextEditingTarget.SOFT_WRAP_LINES, (newval) ->
            {
               boolean wrap = StringUtil.equals(newval.getValue(),
                     DocUpdateSentinel.PROPERTY_TRUE);
               commands_.toggleSoftWrapMode().setChecked(wrap);
               editor_.setUseWrapMode(wrap);
            });

      docUpdateSentinel_.addPropertyValueChangeHandler(
         TextEditingTarget.USE_RAINBOW_PARENS, (newval) ->
         {
            boolean rainbowParens = StringUtil.equals(newval.getValue(),
               DocUpdateSentinel.PROPERTY_TRUE);
            commands_.toggleRainbowParens().setChecked(rainbowParens);
            editor_.setRainbowParentheses(rainbowParens);
         });

      userPrefs_.autoSaveOnBlur().addValueChangeHandler((evt) ->
      {
         // Re-adapt to file type when this preference changes; it may bring
         // back the Source on Save command in the toolbar
         adaptToFileType(editor_.getFileType());
      });

      userPrefs_.autoSaveOnIdle().addValueChangeHandler((evt) ->
      {
         // Same behavior when modifying auto-save on idle
         adaptToFileType(editor_.getFileType());
      });
   }

   public void initWidgetSize()
   {
      if (target_.getPreferredOutlineWidgetVisibility())
      {
         double editorSize = editorPanel_.getOffsetWidth();
         double widgetSize = target_.getPreferredOutlineWidgetSize();
         double size = Math.min(editorSize, widgetSize);
         editorPanel_.setWidgetSize(docOutlineWidget_, size);
         setDocOutlineLatchState(true);
      }
      else
      {
         editorPanel_.setWidgetSize(docOutlineWidget_, 0);
         setDocOutlineLatchState(false);
      }
   }

   public void toggleSoftWrapMode()
   {
      docUpdateSentinel_.setBoolProperty(
            TextEditingTarget.SOFT_WRAP_LINES, !editor_.getUseWrapMode());
   }

   public void toggleRainbowParens()
   {
      docUpdateSentinel_.setBoolProperty(
         TextEditingTarget.USE_RAINBOW_PARENS, !editor_.getRainbowParentheses());
   }

   public void toggleDocumentOutline()
   {
      if (isVisualMode())
         toggleVisualModeOutlineButton_.click();
      else
         toggleDocOutlineButton_.click();
   }


   @Override
   public void toggleRmdVisualMode()
   {
      toggleRmdVisualModeButton_.click();
   }

   private StatusBarWidget statusBar_;

   private void createTestToolbarButtons(Toolbar toolbar)
   {
      SourceColumnManager mgr = RStudioGinjector.INSTANCE.getSourceColumnManager();
      compareTestButton_ = new ToolbarButton(
            "Compare Results",
            ToolbarButton.NoTitle,
            commands_.shinyCompareTest().getImageResource(),
            event -> {
               mgr.setActive(column_);
               commands_.shinyCompareTest().execute();
            });
      compareTestButton_.setTitle(commands_.shinyCompareTest().getDesc());

      toolbar.addRightWidget(compareTestButton_);
      compareTestButton_.setVisible(false);

      testThatButton_ = new ToolbarButton(
            "Run Tests",
            ToolbarButton.NoTitle,
            commands_.testTestthatFile().getImageResource(),
            event -> {
               mgr.setActive(column_);
               commands_.testTestthatFile().execute();
            });
      testThatButton_.setTitle(commands_.testTestthatFile().getDesc());

      toolbar.addRightWidget(testThatButton_);
      testThatButton_.setVisible(false);

      testShinyButton_ = new ToolbarButton(
            "Run Tests",
            ToolbarButton.NoTitle,
            commands_.testShinytestFile().getImageResource(),
            event -> {
               mgr.setActive(column_);
               commands_.testShinytestFile().execute();  
            });
      testShinyButton_.setTitle(commands_.testShinytestFile().getDesc());

      toolbar.addRightWidget(testShinyButton_);
      testShinyButton_.setVisible(false);
   }

   private Toolbar createToolbar(TextFileType fileType)
   {
      Toolbar toolbar = new EditingTargetToolbar(commands_, true, column_, target_.getId());

      // Buttons are unique to a source column so require SourceAppCommands
      SourceColumnManager mgr = RStudioGinjector.INSTANCE.getSourceColumnManager();

      toolbar.addLeftWidget(
         mgr.getSourceCommand(commands_.saveSourceDoc(), column_).createToolbarButton());
      sourceOnSave_.getElement().getStyle().setMarginRight(0, Unit.PX);
      toolbar.addLeftWidget(sourceOnSave_);
      srcOnSaveLabel_.getElement().getStyle().setMarginRight(9, Unit.PX);
      toolbar.addLeftWidget(srcOnSaveLabel_);

      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(
         mgr.getSourceCommand(commands_.checkSpelling(), column_).createToolbarButton());

      toolbar.addLeftWidget(findReplaceButton_ = findReplace_.createFindReplaceButton());
      toolbar.addLeftWidget(createCodeTransformMenuButton());

      notebookSeparatorWidget_ = toolbar.addLeftSeparator();
      toolbar.addLeftWidget(notebookToolbarButton_ =
         mgr.getSourceCommand(commands_.compileNotebook(), column_).createToolbarButton());

      int mod = BrowseCap.hasMetaKey() ? KeyboardShortcut.META :
         KeyboardShortcut.CTRL;
      String cmdText =
        new KeyCombination("K", 'K', mod + KeyboardShortcut.SHIFT).toString(true);
      cmdText = DomUtils.htmlToText(cmdText);
      notebookToolbarButton_.setTitle("Compile Report (" + cmdText + ")");

      texSeparatorWidget_ = toolbar.addLeftSeparator();
      toolbar.addLeftWidget(texToolbarButton_ = createLatexFormatButton());

      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(previewHTMLButton_ =
         mgr.getSourceCommand(commands_.previewHTML(), column_).createToolbarButton());
      knitDocumentButton_ =
         mgr.getSourceCommand(commands_.knitDocument(), column_).createUnsyncedToolbarButton();
      knitDocumentButton_.getElement().getStyle().setMarginRight(0, Unit.PX);
      toolbar.addLeftWidget(knitDocumentButton_);

      ToolbarPopupMenu shinyTestMenu = shinyTestMenu_;
      if (fileType.canKnitToHTML()) {
         shinyLaunchButton_ = new ToolbarMenuButton(
               ToolbarButton.NoText,
               "Shiny test options",
               shinyTestMenu, true);
         toolbar.addLeftWidget(shinyLaunchButton_);
      }

      toolbar.addLeftWidget(compilePdfButton_ =
         mgr.getSourceCommand(commands_.compilePDF(), column_).createToolbarButton());
      rmdFormatButton_ = new ToolbarPopupMenuButton("Knit options", false, true);
      rmdFormatButton_.getMenu().setAutoOpen(true);
      toolbar.addLeftWidget(rmdFormatButton_);

      runDocumentMenuButton_ = new ToolbarPopupMenuButton("Run document options", false, true);
      addClearKnitrCacheMenu(runDocumentMenuButton_);
      runDocumentMenuButton_.addSeparator();
      runDocumentMenuButton_.addMenuItem(mgr.getSourceCommand(commands_.clearPrerenderedOutput(),
         column_).createMenuItem(), "");
      toolbar.addLeftWidget(runDocumentMenuButton_);
      runDocumentMenuButton_.addSeparator();
      runDocumentMenuButton_.addMenuItem(
         mgr.getSourceCommand(commands_.shinyRecordTest(), column_).createMenuItem(), "");
      runDocumentMenuButton_.addMenuItem(
         mgr.getSourceCommand(commands_.shinyRunAllTests(), column_).createMenuItem(), "");
      runDocumentMenuButton_.setVisible(false);

      ToolbarPopupMenu rmdOptionsMenu = new ToolbarPopupMenu();
      rmdOptionsMenu.addItem(
         mgr.getSourceCommand(commands_.editRmdFormatOptions(), column_).createMenuItem());

      rmdOptionsButton_ = new ToolbarMenuButton(
            ToolbarButton.NoText,
            commands_.editRmdFormatOptions().getTooltip(),
            new ImageResource2x(StandardIcons.INSTANCE.options2x()),
            rmdOptionsMenu,
            false);

      toolbar.addLeftWidget(rmdOptionsButton_);

      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(
         mgr.getSourceCommand(commands_.synctexSearch(), column_).createToolbarButton());

      // create menu of chunk skeletons based on common engine types
      ToolbarPopupMenu insertChunksMenu = new ToolbarPopupMenu();
      insertChunksMenu.addItem(mgr.getSourceCommand(commands_.insertChunkR(), column_).createMenuItem());
      insertChunksMenu.addSeparator();

      insertChunksMenu.addItem(
         mgr.getSourceCommand(commands_.insertChunkBash(), column_).createMenuItem());
      insertChunksMenu.addItem(
         mgr.getSourceCommand(commands_.insertChunkD3(), column_).createMenuItem());
      insertChunksMenu.addItem(
         mgr.getSourceCommand(commands_.insertChunkPython(), column_).createMenuItem());
      insertChunksMenu.addItem(
         mgr.getSourceCommand(commands_.insertChunkRCPP(), column_).createMenuItem());
      insertChunksMenu.addItem(
         mgr.getSourceCommand(commands_.insertChunkSQL(), column_).createMenuItem());
      insertChunksMenu.addItem(
         mgr.getSourceCommand(commands_.insertChunkStan(), column_).createMenuItem());

      insertChunkMenu_ = new ToolbarMenuButton(
                       "",
                       commands_.insertChunk().getTooltip(),
                       commands_.insertChunk().getImageResource(),
                       insertChunksMenu,
                       true);

      toolbar.addRightWidget(insertChunkMenu_);

      // create button that just runs default chunk insertion
      insertChunkButton_ = 
         mgr.getSourceCommand(commands_.insertChunk(), column_).createUnsyncedToolbarButton();
      toolbar.addRightWidget(insertChunkButton_);

      toolbar.addRightWidget(runButton_ = 
         mgr.getSourceCommand(commands_.executeCode(), column_).createUnsyncedToolbarButton());
      toolbar.addRightSeparator();
      toolbar.addRightWidget(runLastButton_ = 
         mgr.getSourceCommand(commands_.executeLastCode(), column_).createUnsyncedToolbarButton());
      toolbar.addRightWidget(goToPrevButton_ = 
         mgr.getSourceCommand(commands_.goToPrevSection(), column_).createUnsyncedToolbarButton());
      toolbar.addRightWidget(goToNextButton_ = 
         mgr.getSourceCommand(commands_.goToNextSection(), column_).createUnsyncedToolbarButton());
      toolbar.addRightSeparator();
      final String SOURCE_BUTTON_TITLE = "Source the active document";
      sourceButton_ = new ToolbarButton(
            "Source",
            SOURCE_BUTTON_TITLE,
            commands_.sourceActiveDocument().getImageResource(),
            event -> {
               mgr.setActive(column_);
               if (userPrefs_.sourceWithEcho().getValue())
                  commands_.sourceActiveDocumentWithEcho().execute();
               else
                  commands_.sourceActiveDocument().execute();
            });
      toolbar.addRightWidget(sourceButton_);

      previewJsButton_ = 
         mgr.getSourceCommand(commands_.previewJS(), column_).createUnsyncedToolbarButton();
      toolbar.addRightWidget(previewJsButton_);

      previewSqlButton_ = 
         mgr.getSourceCommand(commands_.previewSql(), column_).createUnsyncedToolbarButton();
      toolbar.addRightWidget(previewSqlButton_);

      createTestToolbarButtons(toolbar);

      userPrefs_.sourceWithEcho().addValueChangeHandler(
            event -> {
               if (event.getValue())
                  sourceButton_.setTitle(SOURCE_BUTTON_TITLE + " (with echo)");
               else
                  sourceButton_.setTitle(SOURCE_BUTTON_TITLE);
            });

      ToolbarPopupMenu sourceMenu = new ToolbarPopupMenu();
      sourceMenu.addItem(
         mgr.getSourceCommand(commands_.sourceActiveDocument(), column_).createMenuItem());
      sourceMenu.addItem(
         mgr.getSourceCommand(commands_.sourceActiveDocumentWithEcho(), column_).createMenuItem());
      sourceMenu.addSeparator();
      sourceMenu.addItem(
         mgr.getSourceCommand(commands_.sourceAsLauncherJob(), column_).createMenuItem());
      sourceMenu.addItem(
         mgr.getSourceCommand(commands_.sourceAsJob(), column_).createMenuItem());

      sourceMenuButton_ = new ToolbarMenuButton(ToolbarButton.NoText, "Source options", sourceMenu, true);
      toolbar.addRightWidget(sourceMenuButton_);

      //toolbar.addRightSeparator();

      ToolbarPopupMenu chunksMenu = new ToolbarPopupMenu();
      chunksMenu.addItem(
         mgr.getSourceCommand(commands_.executeCode(), column_).createMenuItem());
      chunksMenu.addSeparator();
      chunksMenu.addItem(
         mgr.getSourceCommand(commands_.executeCurrentChunk(), column_).createMenuItem());
      chunksMenu.addItem(
         mgr.getSourceCommand(commands_.executeNextChunk(), column_).createMenuItem());
      chunksMenu.addSeparator();
      chunksMenu.addItem(
         mgr.getSourceCommand(commands_.executeSetupChunk(), column_).createMenuItem());
      chunksMenu.addItem(runSetupChunkOptionMenu_= new UserPrefMenuItem<>(
            userPrefs_.autoRunSetupChunk(), true, "Run Setup Chunk Automatically",
            userPrefs_));
      chunksMenu.addSeparator();
      chunksMenu.addItem(
         mgr.getSourceCommand(commands_.executePreviousChunks(), column_).createMenuItem());
      chunksMenu.addItem(
         mgr.getSourceCommand(commands_.executeSubsequentChunks(), column_).createMenuItem());
      if (userPrefs_.rmdChunkOutputInline().getValue())
      {
         chunksMenu.addSeparator();
         chunksMenu.addItem(
               
         mgr.getSourceCommand(commands_.restartRRunAllChunks(), column_).createMenuItem());
         chunksMenu.addItem(
               
         mgr.getSourceCommand(commands_.restartRClearOutput(), column_).createMenuItem());
      }
      chunksMenu.addSeparator();
      chunksMenu.addItem(
         mgr.getSourceCommand(commands_.executeAllCode(), column_).createMenuItem());
      chunksButton_ = new ToolbarMenuButton(
                       "Run",
                       ToolbarButton.NoTitle,
                       commands_.executeCode().getImageResource(),
                       chunksMenu,
                       true);
      toolbar.addRightWidget(chunksButton_);

      ToolbarPopupMenu shinyLaunchMenu = shinyViewerMenu_;
      if (!fileType.canKnitToHTML()) {
         shinyLaunchButton_ = new ToolbarMenuButton(
               ToolbarButton.NoText,
               "Run app options",
               shinyLaunchMenu,
               true);
         toolbar.addRightWidget(shinyLaunchButton_);
      }
      shinyLaunchButton_.setVisible(false);

      plumberLaunchButton_ = new ToolbarMenuButton(
            ToolbarButton.NoText,
            "Run API options",
            plumberViewerMenu_,
            true);
      toolbar.addRightWidget(plumberLaunchButton_);
      plumberLaunchButton_.setVisible(false);

      if (SessionUtils.showPublishUi(session_, userState_))
      {
         toolbar.addRightSeparator();
         publishButton_ = new RSConnectPublishButton(
               RSConnectPublishButton.HOST_EDITOR,
               RSConnect.CONTENT_TYPE_APP, false, null);
         publishButton_.onPublishInvoked(() ->
         {
            if (!StringUtil.isNullOrEmpty(target_.getPath()))
            {
              target_.save();
            }
         });
         toolbar.addRightWidget(publishButton_);
      }

      toggleDocOutlineButton_ = new LatchingToolbarButton(
            ToolbarButton.NoText,
            ToolbarButton.NoTitle,
            true, /* textIndicatesState */
            new ImageResource2x(StandardIcons.INSTANCE.outline2x()),
            event -> {
               final double initialSize = editorPanel_.getWidgetSize(docOutlineWidget_);

               // Clicking the icon toggles the outline widget's visibility. The
               // 'destination' below is the width we would like to set -- we
               // animate to that position for a slightly nicer visual treatment.
               final double destination = docOutlineWidget_.getOffsetWidth() > 5
                     ? 0
                     : Math.min(editorPanel_.getOffsetWidth(), target_.getPreferredOutlineWidgetSize());

               // Update tooltip ('Show'/'Hide' depending on current visibility)
               String title = toggleDocOutlineButton_.getTitle();
               if (destination != 0)
                  title = title.replace("Show ", "Hide ");
               else
                  title = title.replace("Hide ", "Show ");
               toggleDocOutlineButton_.setTitle(title);

               setDocOutlineLatchState(destination != 0);

               int duration = (userPrefs_.reducedMotion().getValue() ? 0 : 500);
               new Animation()
               {
                  @Override
                  protected void onUpdate(double progress)
                  {
                     double size =
                           destination * progress +
                           initialSize * (1 - progress);
                     editorPanel_.setWidgetSize(docOutlineWidget_, size);
                     editor_.onResize();
                  }

                  @Override
                  protected void onComplete()
                  {
                     editorPanel_.setWidgetSize(docOutlineWidget_, destination);
                     target_.setPreferredOutlineWidgetVisibility(destination != 0);
                     editor_.onResize();
                  }
               }.run(duration);
            });

      toggleDocOutlineButton_.addStyleName("rstudio-themes-inverts");

      // Time-out setting the latch just to ensure the document outline
      // has actually been appropriately rendered.
      new Timer()
      {
         @Override
         public void run()
         {
            mgr.setActive(column_);
            String title = commands_.toggleDocumentOutline().getTooltip();
            title = editorPanel_.getWidgetSize(docOutlineWidget_) > 0
                  ? title.replace("Show ", "Hide ")
                  : title.replace("Hide ", "Show ");
            toggleDocOutlineButton_.setTitle(title);
            setDocOutlineLatchState(docOutlineWidget_.getOffsetWidth() > 0);
         }
      }.schedule(100);

      toolbar.addRightSeparator();
      toolbar.addRightWidget(toggleDocOutlineButton_);
      addVisualModeOutlineButton(toolbar);

      toolbar.addRightSeparator();
      toolbar.addRightWidget(createVisualModeToggleButton());

      showWhitespaceCharactersCheckbox_ = new CheckBox("Show whitespace");
      showWhitespaceCharactersCheckbox_.setVisible(false);
      showWhitespaceCharactersCheckbox_.setValue(userPrefs_.showInvisibles().getValue());
      showWhitespaceCharactersCheckbox_.addValueChangeHandler((ValueChangeEvent<Boolean> event) ->
            editor_.setShowInvisibles(event.getValue()));

      if (docUpdateSentinel_ != null && docUpdateSentinel_.getPath() != null)
      {
         FileSystemItem item = FileSystemItem.createFile(docUpdateSentinel_.getPath());
         String ext = item.getExtension();
         if (".csv".equals(ext) || ".tsv".equals(ext))
            showWhitespaceCharactersCheckbox_.setVisible(true);
      }
      toolbar.addRightSeparator();
      toolbar.addRightWidget(showWhitespaceCharactersCheckbox_);

      return toolbar;
   }

   private ToolbarButton createVisualModeToggleButton()
   {
      toggleRmdVisualModeButton_ = new LatchingToolbarButton(
         ToolbarButton.NoText,
         commands_.toggleRmdVisualMode().getTooltip(),
         false, /* textIndicatesState */
         new ImageResource2x(StandardIcons.INSTANCE.visual_mode2x()), event -> {
            boolean visible = !isVisualMode();
            target_.recordCurrentNavigationPosition();
            docUpdateSentinel_.setBoolProperty(TextEditingTarget.RMD_VISUAL_MODE, visible);
            setToggleRmdVisualModeButtonLatched(visible);
            if (visible)
               onUserSwitchingToVisualMode();
         });
      docUpdateSentinel_.addPropertyValueChangeHandler(TextEditingTarget.RMD_VISUAL_MODE, (value) -> {
         setToggleRmdVisualModeButtonLatched(isVisualMode());
         if (isVisualMode())
            findReplace_.hideFindReplace();
      });
      setToggleRmdVisualModeButtonLatched(isVisualMode());
      toggleRmdVisualModeButton_.addStyleName("rstudio-themes-inverts");
      return toggleRmdVisualModeButton_;
   }
   
   private void setToggleRmdVisualModeButtonLatched(boolean latched)
   {
      toggleRmdVisualModeButton_.setLatched(latched);
      if (!latched)
         toggleRmdVisualModeButton_.setTitle("Switch to visual markdown editor");
      else
         toggleRmdVisualModeButton_.setTitle("Switch to source editor");
   }

   private void addVisualModeOutlineButton(Toolbar toolbar)
   {
      // we add a separate outilne button for the visual mode outline b/c the
      // logic for the standard one is tied up in DOM visibility, and we didn't
      // want to refactor that code in a conservative release (v1.4). it's
      // expected that the whole 'visual mode' concept will go away in v1.5
      toggleVisualModeOutlineButton_ = new LatchingToolbarButton(ToolbarButton.NoText,
            ToolbarButton.NoTitle, true, /* textIndicatesState */
            new ImageResource2x(StandardIcons.INSTANCE.outline2x()), event -> {
               target_.setPreferredOutlineWidgetVisibility(
                     !target_.getPreferredOutlineWidgetVisibility());
            });

      // stay in sync w/ doc property
      syncVisualModeOutlineLatchState();
      docUpdateSentinel_.addPropertyValueChangeHandler(TextEditingTarget.DOC_OUTLINE_VISIBLE,
            (event) -> {
               syncVisualModeOutlineLatchState();
            });

      // add to toolbar
      toggleVisualModeOutlineButton_.addStyleName("rstudio-themes-inverts");
      toolbar.addRightWidget(toggleVisualModeOutlineButton_);
   }

   private void syncVisualModeOutlineLatchState()
   {
      boolean visible = target_.getPreferredOutlineWidgetVisibility();
      toggleVisualModeOutlineButton_.setLatched(visible);
      String title = commands_.toggleDocumentOutline().getTooltip();
      title = visible ? title.replace("Show ", "Hide ") : title.replace("Hide ", "Show ");
      toggleVisualModeOutlineButton_.setTitle(title);
   }

   private void setDocOutlineLatchState(boolean latched)
   {
      toggleDocOutlineButton_.setLatched(latched);
      docOutlineWidget_.setAriaVisible(latched);
      docOutlineWidget_.setTabIndex(latched ? 0 : -1);
   }

   private ToolbarButton createLatexFormatButton()
   {
      ToolbarPopupMenu texMenu = new TextEditingTargetLatexFormatMenu(editor_,
                                                                      userPrefs_);

      ToolbarMenuButton texButton = new ToolbarMenuButton(
                           "Format",
                           ToolbarButton.NoTitle,
                           fileTypeRegistry_.getIconForFilename("foo.tex").getImageResource(),
                           texMenu,
                           false);
      return texButton;
   }

   private Widget createCodeTransformMenuButton()
   {
      if (codeTransform_ == null)
      {
         SourceColumnManager mgr = RStudioGinjector.INSTANCE.getSourceColumnManager();
         ImageResource icon = new ImageResource2x(ThemeResources.INSTANCE.codeTransform2x());

         ToolbarPopupMenu menu = new ToolbarPopupMenu();
         menu.addItem(
         mgr.getSourceCommand(commands_.codeCompletion(), column_).createMenuItem());
         menu.addSeparator();
         menu.addItem(
         mgr.getSourceCommand(commands_.goToHelp(), column_).createMenuItem());
         menu.addItem(
         mgr.getSourceCommand(commands_.goToDefinition(), column_).createMenuItem());
         menu.addItem(
         mgr.getSourceCommand(commands_.findUsages(), column_).createMenuItem());
         menu.addSeparator();
         menu.addItem(
         mgr.getSourceCommand(commands_.extractFunction(), column_).createMenuItem());
         menu.addItem(
         mgr.getSourceCommand(commands_.extractLocalVariable(), column_).createMenuItem());
         menu.addItem(
         mgr.getSourceCommand(commands_.renameInScope(), column_).createMenuItem());
         menu.addSeparator();
         menu.addItem(
         mgr.getSourceCommand(commands_.reflowComment(), column_).createMenuItem());
         menu.addItem(
         mgr.getSourceCommand(commands_.commentUncomment(), column_).createMenuItem());
         menu.addItem(
         mgr.getSourceCommand(commands_.insertRoxygenSkeleton(), column_).createMenuItem());
         menu.addSeparator();
         menu.addItem(
         mgr.getSourceCommand(commands_.reindent(), column_).createMenuItem());
         menu.addItem(
         mgr.getSourceCommand(commands_.reformatCode(), column_).createMenuItem());
         menu.addSeparator();
         menu.addItem(
         mgr.getSourceCommand(commands_.showDiagnosticsActiveDocument(), column_).createMenuItem());
         menu.addItem(
         mgr.getSourceCommand(commands_.showDiagnosticsProject(), column_).createMenuItem());
         menu.addSeparator();
         menu.addItem(
         mgr.getSourceCommand(commands_.profileCode(), column_).createMenuItem());
         codeTransform_ = new ToolbarMenuButton(ToolbarButton.NoText, "Code Tools", icon, menu);
      }

      return codeTransform_;
   }

   public void adaptToExtendedFileType(String extendedType)
   {
      extendedType_ = extendedType;
      adaptToFileType(editor_.getFileType());
   }

   public void adaptToFileType(TextFileType fileType)
   {
      editor_.setFileType(fileType);
      boolean canCompilePdf = fileType.canCompilePDF();
      boolean canKnitToHTML = fileType.canKnitToHTML();
      boolean visualRmdMode = isVisualMode();
      boolean canCompileNotebook = fileType.canCompileNotebook();
      boolean canSource = fileType.canSource();
      boolean canSourceWithEcho = fileType.canSourceWithEcho();
      boolean canSourceOnSave = fileType.canSourceOnSave() &&
            !userPrefs_.autoSaveEnabled();
      if (canSourceOnSave && fileType.isJS())
         canSourceOnSave = (extendedType_.equals(SourceDocument.XT_JS_PREVIEWABLE));
      if (canSourceOnSave && fileType.isSql())
         canSourceOnSave = (extendedType_.equals(SourceDocument.XT_SQL_PREVIEWABLE));
      boolean canExecuteCode = fileType.canExecuteCode();
      boolean canExecuteChunks = fileType.canExecuteChunks();
      boolean isPlainMarkdown = fileType.isPlainMarkdown();
      boolean isCpp = fileType.isCpp();
      boolean isScript = fileType.isScript();
      boolean isRMarkdown2 = extendedType_ != null && 
                             extendedType_.startsWith(SourceDocument.XT_RMARKDOWN_PREFIX);
      boolean isMarkdown = editor_.getFileType().isMarkdown();
      boolean canPreviewFromR = fileType.canPreviewFromR();
      boolean terminalAllowed = session_.getSessionInfo().getAllowShell();

      if (isScript && !terminalAllowed)
      {
         commands_.executeCode().setEnabled(false);
         commands_.executeCodeWithoutFocus().setEnabled(false);
      }

      // don't show the run buttons for cpp files, or R files in Shiny/Tests/Plumber
      runButton_.setVisible(canExecuteCode && !canExecuteChunks && !isCpp &&
            !(isShinyFile() || isTestFile() || isPlumberFile()) && !(isScript && !terminalAllowed));
      runLastButton_.setVisible(runButton_.isVisible() && !canExecuteChunks && !isScript);

      // show insertion options for various knitr engines in rmarkdown v2
      insertChunkMenu_.setVisible(isRMarkdown2);

      // otherwise just show the regular insert chunk button
      insertChunkButton_.setVisible(canExecuteChunks && !isRMarkdown2);

      goToPrevButton_.setVisible(fileType.canGoNextPrevSection());
      goToNextButton_.setVisible(fileType.canGoNextPrevSection());

      sourceOnSave_.setVisible(canSourceOnSave);
      srcOnSaveLabel_.setVisible(canSourceOnSave);
      if (fileType.isRd() || fileType.isJS() || canPreviewFromR || fileType.isSql())
         srcOnSaveLabel_.setText(fileType.getPreviewButtonText() + " on Save");
      else
         srcOnSaveLabel_.setText("Source on Save");
      codeTransform_.setVisible(
            (canExecuteCode && !isScript && !fileType.canAuthorContent()) ||
            fileType.isC() || fileType.isStan());

      previewJsButton_.setVisible(fileType.isJS() && extendedType_.equals(SourceDocument.XT_JS_PREVIEWABLE));
      previewSqlButton_.setVisible(fileType.isSql() && extendedType_.equals(SourceDocument.XT_SQL_PREVIEWABLE));

      sourceButton_.setVisible(canSource && !isPlainMarkdown);
      sourceMenuButton_.setVisible(canSourceWithEcho &&
                                   !isPlainMarkdown &&
                                   !isScript &&
                                   !canPreviewFromR);

      texSeparatorWidget_.setVisible(canCompilePdf);
      texToolbarButton_.setVisible(canCompilePdf);
      compilePdfButton_.setVisible(canCompilePdf);
      chunksButton_.setVisible(canExecuteChunks);

      notebookSeparatorWidget_.setVisible(canCompileNotebook);
      notebookToolbarButton_.setVisible(canCompileNotebook);

      findReplaceButton_.setVisible(!visualRmdMode);


      knitDocumentButton_.setVisible(canKnitToHTML);

      setRmdFormatButtonVisible(isRMarkdown2);
      rmdOptionsButton_.setVisible(isRMarkdown2);
      rmdOptionsButton_.setEnabled(isRMarkdown2);


      commands_.enableProsemirrorDevTools().setVisible(isMarkdown);

      toggleRmdVisualModeButton_.setVisible(isMarkdown);

      if (isShinyFile() || isTestFile() || isPlumberFile())
      {
         sourceOnSave_.setVisible(false);
         srcOnSaveLabel_.setVisible(false);
         runButton_.setVisible(false);
         sourceMenuButton_.setVisible(false);
         chunksButton_.setVisible(false);
      }

      testShinyButton_.setVisible(false);
      testThatButton_.setVisible(false);
      compareTestButton_.setVisible(false);
      if (isShinyFile())
      {
         shinyLaunchButton_.setVisible(true);
         plumberLaunchButton_.setVisible(false);
         setSourceButtonFromShinyState();
      }
      else if (isTestThatFile())
      {
         shinyLaunchButton_.setVisible(false);
         plumberLaunchButton_.setVisible(false);
         sourceButton_.setVisible(false);
         testThatButton_.setVisible(true);
      }
      else if (isShinyTestFile())
      {
         shinyLaunchButton_.setVisible(false);
         plumberLaunchButton_.setVisible(false);
         sourceButton_.setVisible(false);
         testShinyButton_.setVisible(true);
         compareTestButton_.setVisible(true);
      }
      else if (isPlumberFile())
      {
         plumberLaunchButton_.setVisible(true);
         setSourceButtonFromPlumberState();
      }
      else
      {
         shinyLaunchButton_.setVisible(false);
         plumberLaunchButton_.setVisible(false);
         setSourceButtonFromScriptState(fileType,
                                        canPreviewFromR,
                                        fileType.getPreviewButtonText());
      }

      // set the content type based on the extended type
      setPublishPath(extendedType_, publishPath_);

      // make toggle outline visible if we have a scope tree
      toggleDocOutlineButton_.setVisible(fileType.canShowScopeTree() && !visualRmdMode);
      if (!fileType.canShowScopeTree())
      {
         editorPanel_.setWidgetSize(docOutlineWidget_, 0);
         setDocOutlineLatchState(false);
      }

      toggleVisualModeOutlineButton_.setVisible(visualRmdMode);
      
      // update modes for filetype
      syncWrapMode();
      syncRainbowParenMode();

      toolbar_.invalidateSeparators();
   }

   private boolean isShinyFile()
   {
      return extendedType_ != null &&
             extendedType_.startsWith(SourceDocument.XT_SHINY_PREFIX);
   }

   private boolean isVisualMode()
   {
      return docUpdateSentinel_.getBoolProperty(TextEditingTarget.RMD_VISUAL_MODE, false);
   }

   private boolean isPlumberFile()
   {
      return SourceDocument.isPlumberFile(extendedType_);
   }

   private boolean isTestFile()
   {
      return extendedType_ != null &&
             extendedType_.startsWith(SourceDocument.XT_TEST_PREFIX);
   }

   private boolean isTestThatFile()
   {
      return extendedType_ != null &&
             extendedType_.startsWith(SourceDocument.XT_TEST_TESTTHAT);
   }

   private boolean isShinyTestFile()
   {
      return extendedType_ != null &&
             extendedType_.startsWith(SourceDocument.XT_TEST_SHINYTEST);
   }

   @Override
   public void setNotebookUIVisible(boolean visible)
   {
      runSetupChunkOptionMenu_.setVisible(visible);
   }

   @Override
   public void setAccessibleName(String name)
   {
      if (StringUtil.isNullOrEmpty(name))
         name = "Untitled Text editor";
      Roles.getTabpanelRole().setAriaLabelProperty(panel_.getElement(), name);
   }

   public HasValue<Boolean> getSourceOnSave()
   {
      return sourceOnSave_;
   }

   public void ensureVisible()
   {
      fireEvent(new EnsureVisibleEvent());
   }

   @Override
   public void onResize()
   {
      super.onResize();
      manageToolbarSizes();
      ResizeEvent.fire(this, getOffsetWidth(), getOffsetHeight());

   }

   private void manageToolbarSizes()
   {
      // sometimes width is passed in as 0 (not sure why)
      int width = getOffsetWidth();
      if (width == 0)
         return;

      texToolbarButton_.setText(width >= 520, "Format");
      runButton_.setText(((width >= 480) && !isShinyFile()), "Run");
      compilePdfButton_.setText(width >= 450, "Compile PDF");
      previewHTMLButton_.setText(width >= 450, previewCommandText_);
      knitDocumentButton_.setText(width >= 450, knitCommandText_);

      if (editor_.getFileType().isRd() || editor_.getFileType().isJS() ||
          editor_.getFileType().isSql() || editor_.getFileType().canPreviewFromR())
      {
         String preview = editor_.getFileType().getPreviewButtonText();
         srcOnSaveLabel_.setText(width < 450 ? preview : preview + " on Save");
      }
      else
      {
         srcOnSaveLabel_.setText(width < 450 ? "Source" : "Source on Save");
      }

      sourceButton_.setText(width >= 400, sourceCommandText_);
   }


   private void showWarningImpl(final Command command)
   {
      if (warningBar_ == null)
      {
         warningBar_ = new InfoBar(InfoBar.WARNING, event -> hideWarningBar());
      }
      command.execute();
      panel_.insertNorth(warningBar_, warningBar_.getHeight(), null);
   }

   @Override
   public void showReadOnlyWarning(final List<String> alternatives)
   {
      showWarningImpl(() -> warningBar_.showReadOnlyWarning(alternatives));
   }

   @Override
   public void showRequiredPackagesMissingWarning(List<String> packages)
   {
      if (docUpdateSentinel_.hasProperty("disableDependencyDiscovery"))
      {
         String disableDependencyDiscovery = docUpdateSentinel_.getProperty("disableDependencyDiscovery");
         if (StringUtil.equals(disableDependencyDiscovery, "1"))
         {
            return;
         }
      }

      Command onInstall = () -> {
         // Form a list of all the dependencies to install
         ArrayList<Dependency> deps = new ArrayList<Dependency>();
         for (String pkg: packages)
            deps.add(Dependency.cranPackage(pkg));

         String scriptName = StringUtil.isNullOrEmpty(docUpdateSentinel_.getPath()) ?
            "R Script" :
            FilePathUtils.friendlyFileName(docUpdateSentinel_.getPath());

         // Install them using the dependency manager; provide a "prompt"
         // function that just accepts the list (since the user has already been
         // prompted here in the editor)
         RStudioGinjector.INSTANCE.getDependencyManager().withDependencies(
               "Install " + scriptName + " dependencies",
               (String dependencies, CommandWithArg<Boolean> result) -> result.execute(true),
               deps, false, null);
         hideWarningBar();
      };

      Command onDismiss = () -> {
         docUpdateSentinel_.setProperty("disableDependencyDiscovery", "1");
         hideWarningBar();
      };

      showWarningImpl(() -> warningBar_.showRequiredPackagesMissingWarning(packages, onInstall, onDismiss));
   }

   @Override
   public void showTexInstallationMissingWarning(String message)
   {
      final Command install = () -> {
         target_.installTinyTeX();
         hideWarningBar();
      };

      showWarningImpl(() -> {
         warningBar_.showTexInstallationMissingWarning(message, install);
      });
   }


   @Override
   public void showPanmirrorFormatChanged(Command onReload)
   {
      showWarningImpl(() -> {
         warningBar_.showPanmirrorFormatChanged(onReload);
      });
   }

   @Override
   public void showWarningBar(final String warning)
   {
      showWarningImpl(() -> warningBar_.setText(warning));
   }

   @Override
   public void showWarningBar(String warning, String actionLabel, Command command)
   {
      showWarningImpl(() -> warningBar_.setTextWithAction(warning, actionLabel, command));
   }

   public void hideWarningBar()
   {
      if (warningBar_ != null)
      {
         panel_.remove(warningBar_);
      }
   }

   public boolean isFindReplaceShowing()
   {
      return findReplace_.isShowing();
   }

   public void showFindReplace(boolean defaultForward)
   {
      findReplace_.showFindReplace(defaultForward);
   }

   public void hideFindReplace()
   {
      findReplace_.hideFindReplace();
   }

   @Override
   public void findNext()
   {
      findReplace_.findNext();
   }

   @Override
   public void findSelectAll()
   {
      findReplace_.selectAll();
   }

   @Override
   public void findPrevious()
   {
      findReplace_.findPrevious();
   }

   @Override
   public void findFromSelection()
   {
      findReplace_.findFromSelection();
   }

   @Override
   public void findFromSelection(String selectionValue)
   {
      findReplace_.findFromSelection(selectionValue);
   }

   @Override
   public void replaceAndFind()
   {
      findReplace_.replaceAndFind();
   }

   public void onActivate()
   {
      editor_.onActivate();

      // sync the state of the command marking word wrap mode for this document
      syncWrapMode();
      syncRainbowParenMode();

      Scheduler.get().scheduleDeferred(() -> manageToolbarSizes());
   }

   public void setFontSize(double size)
   {
      editor_.setFontSize(size);
   }

   public StatusBar getStatusBar()
   {
      return statusBar_;
   }

   @Override
   public void debug_dumpContents()
   {
      String dump = editor_.debug_getDocumentDump();
      new EditDialog(dump, Roles.getAlertdialogRole(), false, false, (input, indicator) ->
            indicator.onCompleted()).showModal();
   }

   @Override
   public void debug_importDump()
   {
      new EditDialog("", Roles.getAlertdialogRole(), false, false, (input, indicator) -> {
         indicator.onCompleted();
         if (input == null)
            return;

         input = input.replaceAll("[ \\r\\n]+", " ");
         String[] chars = input.split(" ");

         StringBuilder sb = new StringBuilder();
         for (String s : chars)
         {
            if (s == ".")
               sb.append('\n');
            else
               sb.append((char)Integer.parseInt(s));
         }

         editor_.debug_setSessionValueDirectly(sb.toString());
      }).showModal();
   }

   // Called by the owning TextEditingTarget to notify the widget that the
   // Shiny application associated with this widget has changed state.
   @Override
   public void onShinyApplicationStateChanged(String state)
   {
      shinyAppState_ = state;
      setSourceButtonFromShinyState();
   }

   // Called by the owning TextEditingTarget to notify the widget that the
   // Plumber API associated with this widget has changed state.
   @Override
   public void onPlumberAPIStateChanged(String state)
   {
      plumberAPIState_ = state;
      setSourceButtonFromPlumberState();
   }

   @Override
   public void setFormatOptions(TextFileType fileType,
                                boolean showRmdFormatMenu,
                                boolean canEditFormatOptions,
                                List<String> options,
                                List<String> values,
                                List<String> extensions,
                                String selectedOption)
   {
      if (!canEditFormatOptions)
      {
         setFormatText("");
      }

      setRmdFormatButtonVisible(showRmdFormatMenu);
      rmdFormatButton_.setEnabled(showRmdFormatMenu);
      rmdFormatButton_.clearMenu();

      int parenPos = selectedOption.indexOf('(');
      if (parenPos != -1)
          selectedOption = selectedOption.substring(0, parenPos).trim();

      // don't show format text (but leave the code in for now in case
      // we change our mind)
      // setFormatText(selectedOption);
      setFormatText("");

      String prefix = fileType.isPlainMarkdown() ? "Preview " : "Knit to ";

      for (int i = 0; i < Math.min(options.size(), values.size()); i++)
      {
         String ext = extensions.get(i);
         ImageResource img = ext != null ?
               fileTypeRegistry_.getIconForFilename("output." + ext).getImageResource() :
               fileTypeRegistry_.getIconForFilename("Makefile").getImageResource();
         final String valueName = values.get(i);
         ScheduledCommand cmd = () -> handlerManager_.fireEvent(
               new RmdOutputFormatChangedEvent(valueName));
         String text = ext == ".nb.html" ? "Preview Notebook" :
            prefix + options.get(i);

         MenuItem item = ImageMenuItem.create(img, text, cmd, 2);
         rmdFormatButton_.addMenuItem(item, values.get(i));
      }

      if (session_.getSessionInfo().getKnitParamsAvailable())
      {
         final AppCommand knitWithParams = commands_.knitWithParameters();
         rmdFormatButton_.addSeparator();
         ScheduledCommand cmd = () -> knitWithParams.execute();
         MenuItem item = new MenuItem(knitWithParams.getMenuHTML(false),
                                      true,
                                      cmd);
         rmdFormatButton_.addMenuItem(item, knitWithParams.getMenuLabel(false));
      }

      if (session_.getSessionInfo().getKnitWorkingDirAvailable())
      {
         MenuBar knitDirMenu = new MenuBar(true);
         DocPropMenuItem knitInDocDir = new DocShadowPropMenuItem(
               "Document Directory",
               docUpdateSentinel_,
               userPrefs_.knitWorkingDir(),
               RenderRmdEvent.WORKING_DIR_PROP,
               UserPrefs.KNIT_WORKING_DIR_DEFAULT);
         knitDirMenu.addItem(knitInDocDir);
         DocPropMenuItem knitInProjectDir = new DocShadowPropMenuItem(
               "Project Directory",
               docUpdateSentinel_,
               userPrefs_.knitWorkingDir(),
               RenderRmdEvent.WORKING_DIR_PROP,
               UserPrefs.KNIT_WORKING_DIR_PROJECT);
         knitDirMenu.addItem(knitInProjectDir);
         DocPropMenuItem knitInCurrentDir = new DocShadowPropMenuItem(
               "Current Working Directory",
               docUpdateSentinel_,
               userPrefs_.knitWorkingDir(),
               RenderRmdEvent.WORKING_DIR_PROP,
               UserPrefs.KNIT_WORKING_DIR_CURRENT);
         knitDirMenu.addItem(knitInCurrentDir);

         rmdFormatButton_.addSeparator();
         rmdFormatButton_.addMenuItem(knitDirMenu, "Knit Directory");
      }

      addClearKnitrCacheMenu(rmdFormatButton_);

      showRmdViewerMenuItems(true, canEditFormatOptions, fileType.isRmd(), false);

      if (publishButton_ != null)
         publishButton_.setIsStatic(true);
   }

   private void addClearKnitrCacheMenu(ToolbarPopupMenuButton menuButton)
   {
      final AppCommand clearKnitrCache = commands_.clearKnitrCache();
      menuButton.addSeparator();
      ScheduledCommand cmd = () -> clearKnitrCache.execute();
      MenuItem item = new MenuItem(clearKnitrCache.getMenuHTML(false),
                                   true,
                                   cmd);
      menuButton.addMenuItem(item, clearKnitrCache.getMenuLabel(false));
   }

   @Override
   public void setIsShinyFormat(boolean showOutputOptions,
                                boolean isPresentation,
                                boolean isShinyPrerendered)
   {
      setRmdFormatButtonVisible(false);

      showRmdViewerMenuItems(!isPresentation, showOutputOptions, true, true);

      String docType = isPresentation ? "Presentation" : "Document";

      if (!isPresentation && !isShinyPrerendered) {
         shinyLaunchButton_.setVisible(true);
      }

      knitCommandText_ = "Run " + docType;
      knitDocumentButton_.setTitle("View the current " + docType.toLowerCase() +
            " with Shiny (" +
            DomUtils.htmlToText(
                  commands_.knitDocument().getShortcutPrettyHtml()) + ")");
      knitDocumentButton_.setText(knitCommandText_);
      knitDocumentButton_.setLeftImage(new ImageResource2x(StandardIcons.INSTANCE.run2x()));

      runDocumentMenuButton_.setVisible(isShinyPrerendered);
      setKnitDocumentMenuVisible(isShinyPrerendered);

      if (publishButton_ != null)
         publishButton_.setIsStatic(false);
   }

   @Override
   public void setIsNotShinyFormat()
   {
      runDocumentMenuButton_.setVisible(false);
   }

   @Override
   public void setIsNotebookFormat()
   {
      knitCommandText_ = "Preview";
      knitDocumentButton_.setTitle("Preview the notebook (" +
            DomUtils.htmlToText(
                  commands_.knitDocument().getShortcutPrettyHtml()) + ")");
      knitDocumentButton_.setText(knitCommandText_);
      knitDocumentButton_.setLeftImage(
            commands_.newRNotebook().getImageResource());
      setRmdFormatButtonVisible(true);
      showRmdViewerMenuItems(true, true, true, false);
   }

   @Override
   public TextEditorContainer editorContainer()
   {
      return editorContainer_;
   }

   @Override
   public void manageCommandUI()
   {
      adaptToFileType(editor_.getFileType());
      showRmdViewerMenuItems(true, true, true,  isShinyFile());
   }

   private void setRmdFormatButtonVisible(boolean visible)
   {
      rmdFormatButton_.setVisible(visible);
      setKnitDocumentMenuVisible(visible);
   }

   private void setKnitDocumentMenuVisible(boolean visible)
   {
      knitDocumentButton_.getElement().getStyle().setMarginRight(
            visible ? 0 : 8, Unit.PX);
   }

   @Override
   public void setPublishPath(String type, String publishPath)
   {
      publishPath_ = publishPath;
      if (publishButton_ != null)
      {
         if (type == SourceDocument.XT_SHINY_DIR)
         {
            publishButton_.setContentPath(publishPath, "");
            publishButton_.setContentType(RSConnect.CONTENT_TYPE_APP);
         }
         else if (type == SourceDocument.XT_SHINY_SINGLE_FILE)
         {
            publishButton_.setContentPath(publishPath, "");
            publishButton_.setContentType(RSConnect.CONTENT_TYPE_APP_SINGLE);
         }
         else if (type.startsWith(SourceDocument.XT_RMARKDOWN_PREFIX))
         {
            // don't publish markdown docs as static
            publishButton_.setRmd(publishPath,
                  false // not static
                  );
         }
         else if (type == SourceDocument.XT_PLUMBER_API)
         {
            publishButton_.setContentPath(publishPath, "");
            publishButton_.setContentType(RSConnect.CONTENT_TYPE_PLUMBER_API);
         }
         else
         {
            publishButton_.setContentType(RSConnect.CONTENT_TYPE_NONE);
         }
      }
   }

   @Override
   public void invokePublish()
   {
      if (publishButton_ == null)
      {
         // shouldn't happen in practice (we hide the publish button and
         // disable the command when a non-publishable item is showing in the
         // widget) but in case it does let the user know why nothing's
         // happening.
         RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
               "Content not publishable",
               "This item cannot be published.");
      }
      else
      {
         publishButton_.invokePublish();
      }
   }

   private void setFormatText(String text)
   {
      if (text.length() > 0)
         text = " " + text;
      knitCommandText_ = "Knit" + text;
      knitDocumentButton_.setText(knitCommandText_);
      knitDocumentButton_.setLeftImage(
            commands_.knitDocument().getImageResource());
      knitDocumentButton_.setTitle(commands_.knitDocument().getTooltip());
      previewCommandText_ = "Preview" + text;
      previewHTMLButton_.setText(previewCommandText_);
   }

   private void setSourceButtonFromScriptState(TextFileType fileType,
                                               boolean canPreviewFromR,
                                               String previewButtonText)
   {
      sourceCommandText_ = commands_.sourceActiveDocument().getButtonLabel();
      String sourceCommandDesc = commands_.sourceActiveDocument().getDesc();
      if (fileType.isPython())
      {
         sourceCommandText_ = "Source Script";
         sourceCommandDesc = "Save changes and source the current script";
         sourceButton_.setLeftImage(
                           commands_.debugContinue().getImageResource());
      }
      else if (fileType.isScript())
      {
         sourceCommandText_ = "Run Script";
         sourceCommandDesc = "Save changes and run the current script";
         sourceButton_.setLeftImage(
                           commands_.debugContinue().getImageResource());
      }
      else if (canPreviewFromR)
      {
         sourceCommandText_ = previewButtonText;
         sourceCommandDesc = "Save changes and preview";
         sourceButton_.setLeftImage(
                           commands_.debugContinue().getImageResource());
      }

      sourceButton_.setTitle(sourceCommandDesc);
      sourceButton_.setText(sourceCommandText_);
      sourceButton_.setLeftImage(
            commands_.sourceActiveDocument().getImageResource());
   }

   public void setSourceButtonFromShinyState()
   {
      sourceCommandText_ = commands_.sourceActiveDocument().getButtonLabel();
      String sourceCommandDesc = commands_.sourceActiveDocument().getDesc();
      if (isShinyFile())
      {
         if (shinyAppState_ == ShinyApplicationParams.STATE_STARTED)
         {
            sourceCommandText_ = "Reload App";
            sourceCommandDesc = "Save changes and reload the Shiny application";
            sourceButton_.setLeftImage(
                  commands_.reloadShinyApp().getImageResource());
         }
         else if (shinyAppState_ == ShinyApplicationParams.STATE_STOPPED)
         {
            sourceCommandText_ = "Run App";
            sourceCommandDesc = "Run the Shiny application";
            sourceButton_.setLeftImage(
                  commands_.debugContinue().getImageResource());
         }
      }

      sourceButton_.setTitle(sourceCommandDesc);
      sourceButton_.setText(sourceCommandText_);
   }

   public void setSourceButtonFromPlumberState()
   {
      sourceCommandText_ = commands_.sourceActiveDocument().getButtonLabel();
      String sourceCommandDesc = commands_.sourceActiveDocument().getDesc();
      if (isPlumberFile())
      {
         if (plumberAPIState_ == PlumberAPIParams.STATE_STARTED)
         {
            sourceCommandText_ = "Reload API";
            sourceCommandDesc = "Save changes and reload the Plumber API";
            sourceButton_.setLeftImage(commands_.reloadPlumberAPI().getImageResource());
         }
         else if (plumberAPIState_ == PlumberAPIParams.STATE_STOPPED)
         {
            sourceCommandText_ = "Run API";
            sourceCommandDesc = "Run the Plumber API";
            sourceButton_.setLeftImage(commands_.debugContinue().getImageResource());
         }
      }

      sourceButton_.setTitle(sourceCommandDesc);
      sourceButton_.setText(sourceCommandText_);
   }

   @Override
   public void addVisualModeFindReplaceButton(ToolbarButton findReplaceButton)
   {
      toolbar_.insertWidget(findReplaceButton, findReplaceButton_);
   }

   public HandlerRegistration addEnsureVisibleHandler(EnsureVisibleEvent.Handler handler)
   {
      return addHandler(handler, EnsureVisibleEvent.TYPE);
   }

   @Override
   public HandlerRegistration addEnsureHeightHandler(EnsureHeightEvent.Handler handler)
   {
      return addHandler(handler, EnsureHeightEvent.TYPE);
   }

   @Override
   public HandlerRegistration addResizeHandler(ResizeHandler handler)
   {
      return addHandler(handler, ResizeEvent.getType());
   }

   public void onVisibilityChanged(boolean visible)
   {
      editor_.onVisibilityChanged(visible);
   }

   @Override
   public HandlerRegistration addRmdFormatChangedHandler(
         RmdOutputFormatChangedEvent.Handler handler)
   {
      return handlerManager_.addHandler(
            RmdOutputFormatChangedEvent.TYPE, handler);
   }

   private void showRmdViewerMenuItems(boolean show, boolean showOutputOptions,
         boolean isRmd, boolean isShinyFile)
   {
      if (rmdViewerPaneMenuItem_ == null)
         rmdViewerPaneMenuItem_ = new UserPrefMenuItem<>(
            userPrefs_.rmdViewerType(),
            UserPrefs.RMD_VIEWER_TYPE_PANE,
            "Preview in Viewer Pane", userPrefs_);
      if (rmdViewerWindowMenuItem_ == null)
         rmdViewerWindowMenuItem_ = new UserPrefMenuItem<>(
            userPrefs_.rmdViewerType(),
            UserPrefs.RMD_VIEWER_TYPE_WINDOW,
            "Preview in Window", userPrefs_);
      if (rmdViewerNoPreviewMenuItem_ == null)
         rmdViewerNoPreviewMenuItem_ = new UserPrefMenuItem<>(
            userPrefs_.rmdViewerType(),
            UserPrefs.RMD_VIEWER_TYPE_NONE,
            "(No Preview)", userPrefs_);


      ToolbarPopupMenu menu = rmdOptionsButton_.getMenu();
      menu.clearItems();

      boolean visualMode = isVisualMode();
      DocPropMenuItem visualModeMenu = new DocPropMenuItem(
         "Use Visual Editor", true, docUpdateSentinel_,
         visualMode,
         TextEditingTarget.RMD_VISUAL_MODE,
         DocUpdateSentinel.PROPERTY_TRUE
      )
      {
         @Override
         public void onUpdateComplete()
         {
            if (docUpdateSentinel_.getBoolProperty(TextEditingTarget.RMD_VISUAL_MODE, false))
               onUserSwitchingToVisualMode();
         }
        
         @Override
         public String getShortcut()
         {
            return commands_.toggleRmdVisualMode().getShortcutPrettyHtml();
         }

      };
      menu.addItem(visualModeMenu);
      menu.addSeparator();
      


      if (show)
      {
         menu.addItem(rmdViewerWindowMenuItem_);
         menu.addItem(rmdViewerPaneMenuItem_);
         menu.addItem(rmdViewerNoPreviewMenuItem_);
         menu.addSeparator();
      }

      menu.addSeparator();

      if (!visualMode)
      {
         String pref = userPrefs_.latexPreviewOnCursorIdle().getValue();
         menu.addItem(new DocPropMenuItem(
            "Preview Images and Equations", docUpdateSentinel_,
            docUpdateSentinel_.getBoolProperty(
               TextEditingTargetNotebook.CONTENT_PREVIEW_ENABLED,
               pref != UserPrefs.LATEX_PREVIEW_ON_CURSOR_IDLE_NEVER),
            TextEditingTargetNotebook.CONTENT_PREVIEW_ENABLED,
            DocUpdateSentinel.PROPERTY_TRUE));
         menu.addItem(new DocPropMenuItem(
            "Show Previews Inline", docUpdateSentinel_,
            docUpdateSentinel_.getBoolProperty(
               TextEditingTargetNotebook.CONTENT_PREVIEW_INLINE,
               pref == UserPrefs.LATEX_PREVIEW_ON_CURSOR_IDLE_ALWAYS),
            TextEditingTargetNotebook.CONTENT_PREVIEW_INLINE,
            DocUpdateSentinel.PROPERTY_TRUE));
         menu.addSeparator();
      }

      if (!isShinyFile)
      {
         boolean inline = userPrefs_.rmdChunkOutputInline().getValue();
         menu.addItem(new DocPropMenuItem(
            "Chunk Output Inline", docUpdateSentinel_,
            inline,
            TextEditingTargetNotebook.CHUNK_OUTPUT_TYPE,
            TextEditingTargetNotebook.CHUNK_OUTPUT_INLINE));
         menu.addItem(new DocPropMenuItem(
            "Chunk Output in Console", docUpdateSentinel_,
            !inline,
            TextEditingTargetNotebook.CHUNK_OUTPUT_TYPE,
            TextEditingTargetNotebook.CHUNK_OUTPUT_CONSOLE));

         menu.addSeparator();

         SourceColumnManager mgr = RStudioGinjector.INSTANCE.getSourceColumnManager();
         menu.addItem(
            mgr.getSourceCommand(commands_.notebookExpandAllOutput(), column_).createMenuItem());
         menu.addItem(
            mgr.getSourceCommand(commands_.notebookCollapseAllOutput(), column_).createMenuItem());
         menu.addSeparator();
         menu.addItem(
            mgr.getSourceCommand(commands_.notebookClearOutput(), column_).createMenuItem());
         menu.addItem(
            mgr.getSourceCommand(commands_.notebookClearAllOutput(), column_).createMenuItem());
         menu.addSeparator();
      }

      if (showOutputOptions)
      {
         SourceColumnManager mgr = RStudioGinjector.INSTANCE.getSourceColumnManager();
         menu.addItem(
            mgr.getSourceCommand(commands_.editRmdFormatOptions(), column_).createMenuItem());
      }
   }

   @Override
   protected void onAttach()
   {
      super.onAttach();

      ElementIds.assignElementId(sourceButton_, ElementIds.TEXT_SOURCE_BUTTON);
      ElementIds.assignElementId(sourceMenuButton_, ElementIds.TEXT_SOURCE_BUTTON_DROPDOWN);
      ElementIds.assignElementId(sourceOnSave_, ElementIds.CB_SOURCE_ON_SAVE);
      ElementIds.assignElementId(toggleDocOutlineButton_, ElementIds.TOGGLE_DOC_OUTLINE_BUTTON);
   }

   private final TextEditorContainer.Editor sourceEditor_ = new TextEditorContainer.Editor()
   {
      @Override
      public void setVisible(boolean visible)
      {
         editorPanel_.setVisible(visible);
         panel_.showStatusBar(visible);
      }

      @Override
      public boolean isVisible()
      {
         return editorPanel_.isVisible();
      }

      @Override
      public void focus()
      {
         editor_.focus();
         if (activationPending_)
         {
            activationPending_ = false;
            new Timer() {
               @Override
               public void run()
               {
                  editor_.moveCursorNearTop();
               }
            }.schedule(100);
         }
      }

      @Override
      public Widget asWidget()
      {
        return editorPanel_;
      }

      @Override
      public void setCode(String code)
      {
         editor_.setCode(code, true);
      }

      @Override
      public void applyChanges(TextEditorContainer.Changes changes, boolean activatingEditor)
      {
         // apply changes
         editor_.applyChanges(changes.changes);

         // additional actions when activating
         if (activatingEditor)
         {
            // call navigator if if one was provided
            if (changes.navigator != null)
               changes.navigator.onNavigate(editor_);

            // flag activation pending (triggers autoscroll)
            activationPending_ = true;
         }
      }

      @Override
      public String getCode()
      {
         return editor_.getCode();
      }

      private boolean activationPending_ = false;

   };

   private void syncWrapMode()
   {
      // set wrap mode from the file type (unless we have a wrap mode specified
      // explicitly)
      boolean wrapMode = editor_.getFileType().getWordWrap();
      if (docUpdateSentinel_.hasProperty(TextEditingTarget.SOFT_WRAP_LINES))
      {
         wrapMode = docUpdateSentinel_.getBoolProperty(TextEditingTarget.SOFT_WRAP_LINES,
               wrapMode);
      }
      editor_.setUseWrapMode(wrapMode);
      commands_.toggleSoftWrapMode().setChecked(wrapMode);
   }

   private void syncRainbowParenMode()
   {
      boolean rainbowMode = editor_.getRainbowParentheses();
      if (docUpdateSentinel_.hasProperty(TextEditingTarget.USE_RAINBOW_PARENS))
      {
         rainbowMode = docUpdateSentinel_.getBoolProperty(TextEditingTarget.USE_RAINBOW_PARENS, rainbowMode);
      }
      editor_.setRainbowParentheses(rainbowMode);
      commands_.toggleRainbowParens().setChecked(rainbowMode);
   }
   
   private void onUserSwitchingToVisualMode()
   {
      target_.onUserSwitchingToVisualMode();
   }

   private final TextEditingTarget target_;
   private final DocUpdateSentinel docUpdateSentinel_;
   private final Commands commands_;
   @SuppressWarnings("unused")
   private final EventBus events_;
   private final UserPrefs userPrefs_;
   private final UserState userState_;
   private final Session session_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final DocDisplay editor_;
   private final ShinyViewerTypePopupMenu shinyViewerMenu_;
   private final ShinyTestPopupMenu shinyTestMenu_;
   private final PlumberViewerTypePopupMenu plumberViewerMenu_;
   private SourceColumn column_;
   private String extendedType_;
   private String publishPath_;
   private CheckBox sourceOnSave_;
   private TextEditorContainer editorContainer_;
   private DockLayoutPanel editorPanel_;
   private DocumentOutlineWidget docOutlineWidget_;
   private PanelWithToolbars panel_;
   private Toolbar toolbar_;
   private InfoBar warningBar_;
   private final TextEditingTargetFindReplace findReplace_;
   private ToolbarButton findReplaceButton_;
   private ToolbarMenuButton codeTransform_;
   private ToolbarButton compilePdfButton_;
   private ToolbarButton previewHTMLButton_;
   private ToolbarButton knitDocumentButton_;
   private ToolbarMenuButton insertChunkMenu_;
   private ToolbarButton insertChunkButton_;
   private ToolbarButton goToPrevButton_;
   private ToolbarButton goToNextButton_;
   private ToolbarButton runButton_;
   private ToolbarButton runLastButton_;
   private ToolbarButton sourceButton_;
   private ToolbarButton previewJsButton_;
   private ToolbarButton previewSqlButton_;
   private ToolbarButton testThatButton_;
   private ToolbarButton testShinyButton_;
   private ToolbarButton compareTestButton_;
   private ToolbarMenuButton sourceMenuButton_;
   private UserPrefMenuItem<Boolean> runSetupChunkOptionMenu_;
   private ToolbarMenuButton chunksButton_;
   private ToolbarMenuButton shinyLaunchButton_;
   private ToolbarMenuButton plumberLaunchButton_;
   private ToolbarMenuButton rmdOptionsButton_;
   private LatchingToolbarButton toggleRmdVisualModeButton_;
   private LatchingToolbarButton toggleDocOutlineButton_;
   private LatchingToolbarButton toggleVisualModeOutlineButton_;
   private CheckBox showWhitespaceCharactersCheckbox_;
   private ToolbarPopupMenuButton rmdFormatButton_;
   private ToolbarPopupMenuButton runDocumentMenuButton_;
   private RSConnectPublishButton publishButton_;
   private MenuItem rmdViewerPaneMenuItem_;
   private MenuItem rmdViewerWindowMenuItem_;
   private MenuItem rmdViewerNoPreviewMenuItem_;
   private HandlerManager handlerManager_;

   private Widget texSeparatorWidget_;
   private ToolbarButton texToolbarButton_;
   private Widget notebookSeparatorWidget_;
   private ToolbarButton notebookToolbarButton_;
   private Label srcOnSaveLabel_;

   private String shinyAppState_ = ShinyApplicationParams.STATE_STOPPED;
   private String plumberAPIState_ = PlumberAPIParams.STATE_STOPPED;
   private String sourceCommandText_ = "Source";
   private String knitCommandText_ = "Knit";
   private String previewCommandText_ = "Preview";

}
