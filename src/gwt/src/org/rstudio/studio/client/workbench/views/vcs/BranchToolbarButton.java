/*
 * BranchToolbarButton.java
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
package org.rstudio.studio.client.workbench.views.vcs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.inject.Inject;
import com.google.inject.Provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.rstudio.core.client.JsVectorString;
import org.rstudio.core.client.MapUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.WidgetHandlerRegistration;
import org.rstudio.core.client.command.AppCommand;
import com.google.gwt.resources.client.ImageResource;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.CustomMenuItemSeparator;
import org.rstudio.core.client.widget.ScrollableToolbarPopupMenu;
import org.rstudio.core.client.widget.SearchWidget;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.common.vcs.BranchesInfo;
import org.rstudio.studio.client.common.vcs.WorktreeInfo;
import org.rstudio.studio.client.workbench.views.vcs.common.events.BranchCaptionChangedEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.git.model.GitState;

public class BranchToolbarButton extends ToolbarMenuButton
                                 implements HasValueChangeHandlers<String>,
                                            VcsRefreshEvent.Handler
{
   private static final ViewVcsConstants constants_ = GWT.create(ViewVcsConstants.class);
   protected class SwitchBranchCommand implements Command
   {
      public SwitchBranchCommand(String branchLabel, String branchValue)
      {
         branchLabel_ = branchLabel;
         branchValue_ = branchValue;
      }

      @Override
      public void execute()
      {
         setBranchCaption(branchLabel_);
         ValueChangeEvent.fire(BranchToolbarButton.this, branchValue_);
      }

      private final String branchLabel_;
      private final String branchValue_;
   }

   protected class SwitchWorktreeCommand implements Command
   {
      public SwitchWorktreeCommand(WorktreeInfo worktree)
      {
         worktree_ = worktree;
      }

      @Override
      public void execute()
      {
         // set by the click preview handler when the right-hand image was hit
         boolean newSession = openInNewSession_;
         openInNewSession_ = false;
         onWorktreeSelected(worktree_, newSession);
      }

      private final WorktreeInfo worktree_;
   }

   @Inject
   public BranchToolbarButton(final Provider<GitState> pVcsState)
   {
      super(ToolbarButton.NoText,
            constants_.switchBranch(),
            StandardIcons.INSTANCE.empty_command(),
            new ScrollableToolbarPopupMenu());

      pVcsState_ = pVcsState;

      new WidgetHandlerRegistration(this)
      {
         @Override
         protected HandlerRegistration doRegister()
         {
            return pVcsState.get().addVcsRefreshHandler(
                                                BranchToolbarButton.this, true);
         }
      };

      menu_ = getMenu();
      menu_.setAutoHideRedundantSeparators(false);
      menu_.addAttachHandler(new AttachEvent.Handler()
      {
         @Override
         public void onAttachOrDetach(AttachEvent event)
         {
            if (event.isAttached())
            {
               openInNewSession_ = false;
               clickPreviewHandler_ = Event.addNativePreviewHandler(new NativePreviewHandler()
               {
                  @Override
                  public void onPreviewNativeEvent(NativePreviewEvent preview)
                  {
                     if (preview.getTypeInt() != Event.ONCLICK)
                        return;

                     Element targetEl = preview.getNativeEvent().getEventTarget().cast();
                     if (targetEl.hasClassName(ThemeStyles.INSTANCE.menuRightImage()))
                        openInNewSession_ = true;
                  }
               });

               // rebuild the menu if required
               if (menuRebuildRequired_)
                  menuRebuildRequired_ = !rebuildMenu();

               // force a re-draw if necessary
               if (initialBranchMap_ != null)
               {
                  searchWidget_.setValue("");
                  onSearchValueChange();
               }

               Scheduler.get().scheduleDeferred(new ScheduledCommand()
               {
                  @Override
                  public void execute()
                  {
                     focusSearch();
                     Element tableEl = menu_.getMenuTableElement();
                     if (tableEl != null)
                        menuWidth_ = tableEl.getOffsetWidth();
                  }
               });
            }
            else
            {
               if (previewHandler_ != null)
               {
                  previewHandler_.removeHandler();
                  previewHandler_ = null;
               }

               if (clickPreviewHandler_ != null)
               {
                  clickPreviewHandler_.removeHandler();
                  clickPreviewHandler_ = null;
               }
            }
         }
      });

      searchWidget_ = new SearchWidget(constants_.searchByBranchName());

      searchValueChangeTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            onSearchValueChange();
         }
      };
   }

   public void setBranchCaption(String caption)
   {
      if (StringUtil.isNullOrEmpty(caption))
         caption = NO_BRANCH;

      setText(caption);
      fireEvent(new BranchCaptionChangedEvent(caption));
   }

   @Override
   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler)
   {
      return addHandler(handler, ValueChangeEvent.getType());
   }

   public HandlerRegistration addBranchCaptionChangedHandler(BranchCaptionChangedEvent.Handler handler) {
      return addHandler(handler, BranchCaptionChangedEvent.TYPE);
   }

   @Override
   public void onVcsRefresh(VcsRefreshEvent event)
   {
      JsVectorString branches = pVcsState_.get().getBranchInfo().getBranches().cast();
      if (branches.length() != branches_.length())
      {
         branches_ = branches.slice().cast();
         menuRebuildRequired_ = true;
         return;
      }

      for (int i = 0; i < branches.length(); i++)
      {
         if (!StringUtil.equals(branches.get(i), branches_.get(i)))
         {
            branches_ = branches.slice();
            menuRebuildRequired_ = true;
            return;
         }
      }

      String worktreeKey = worktreeKey(pVcsState_.get().getBranchInfo().getWorktrees());
      if (!StringUtil.equals(worktreeKey, worktreeKey_))
      {
         worktreeKey_ = worktreeKey;
         menuRebuildRequired_ = true;
      }
   }

   private static String worktreeKey(JsArray<WorktreeInfo> worktrees)
   {
      StringBuilder key = new StringBuilder();
      for (WorktreeInfo worktree : JsUtil.asIterable(worktrees))
      {
         key.append(worktree.getPath())
            .append('\t')
            .append(worktree.getBranch())
            .append('\t')
            .append(worktree.getHead())
            .append('\n');
      }
      return key.toString();
   }

   // Subclasses that can activate a worktree override these.
   protected boolean showWorktrees()
   {
      return false;
   }

   // The icon shown at the right of a worktree entry to open it in a new
   // session, or null when that isn't available
   protected ImageResource openWorktreeInNewSessionImage()
   {
      return null;
   }

   protected void onWorktreeSelected(WorktreeInfo worktree, boolean newSession)
   {
   }

   // Returns false when branch information isn't available yet (the menu was
   // opened before the first status refresh completed), in which case the
   // caller should try again on the next open.
   private boolean rebuildMenu()
   {
      menu_.clearItems();

      BranchesInfo branchInfo = pVcsState_.get().getBranchInfo();
      if (branchInfo == null)
      {
         onBeforePopulateMenu(menu_);
         populateEmptyMenu(menu_);
         return false;
      }

      JsArrayString branches = branchInfo.getBranches();
      if (branches.length() == 0)
      {
         onBeforePopulateMenu(menu_);
         populateEmptyMenu(menu_);
         return true;
      }

      // separate branches based on remote name
      Map<String, List<String>> branchMap = new LinkedHashMap<>();
      List<String> localBranches = new ArrayList<>();
      branchMap.put(LOCAL_BRANCHES, localBranches);
      for (String branch : JsUtil.asIterable(branches))
      {
         if (branch.startsWith("remotes/"))
         {
            JsArrayString parts = StringUtil.split(branch, "/");
            if (parts.length() > 2)
            {
               String remote = parts.get(1);
               if (!branchMap.containsKey(remote))
                  branchMap.put(remote, new ArrayList<>());
               List<String> remoteBranches = branchMap.get(remote);
               remoteBranches.add(branch);
            }
         }
         else
         {
            localBranches.add(branch);
         }
      }

      // branches checked out in another worktree can't be checked out here;
      // remember where they live so that selecting one opens that worktree
      worktrees_ = new ArrayList<>();
      worktreeBranches_ = new HashMap<>();
      if (showWorktrees())
      {
         for (WorktreeInfo worktree : JsUtil.asIterable(branchInfo.getWorktrees()))
         {
            if (worktree.isBare() || worktree.isPrunable())
               continue;

            worktrees_.add(worktree);
            if (!worktree.isCurrent() && !StringUtil.isNullOrEmpty(worktree.getBranch()))
               worktreeBranches_.put(worktree.getBranch(), worktree);
         }

         // the main worktree alone isn't worth a section
         if (worktrees_.size() < 2)
            worktrees_.clear();
      }

      // record the branches used on first populate
      initialBranchMap_ = branchMap;

      onBeforePopulateMenu(menu_);
      populateMenu(menu_, branchMap);
      return true;
   }

   private void populateWorktrees(final ToolbarPopupMenu menu, final String query)
   {
      List<WorktreeInfo> worktrees = new ArrayList<>();
      for (WorktreeInfo worktree : worktrees_)
      {
         if (query.isEmpty() ||
             worktree.getBranch().indexOf(query) != -1 ||
             worktree.getPath().indexOf(query) != -1)
         {
            worktrees.add(worktree);
         }
      }

      if (worktrees.isEmpty())
         return;

      // worktrees nested inside the main worktree read better relative to it
      String mainPath = null;
      for (WorktreeInfo worktree : worktrees_)
      {
         if (worktree.isMain())
            mainPath = worktree.getPath();
      }

      menu.addSeparator(new CustomMenuItemSeparator()
      {
         @Override
         public Element createMainElement()
         {
            Label label = new Label(WORKTREES);
            label.addStyleName(ThemeStyles.INSTANCE.menuSubheader());
            label.getElement().getStyle().setPaddingLeft(2, Unit.PX);
            boolean useSearch = menu_.getItemCount() == 0;
            return useSearch ? createSearchSeparator(label) : label.getElement();
         }
      });
      menu.addSeparator();

      for (WorktreeInfo worktree : worktrees)
      {
         String name = worktree.isDetached() || StringUtil.isNullOrEmpty(worktree.getBranch())
               ? worktree.getHead()
               : worktree.getBranch();

         // nested worktrees read relative to the main one; others keep only
         // their last two path components, with the full path as a tooltip
         String path = worktree.getPath();
         if (mainPath != null && path.startsWith(mainPath + "/"))
         {
            path = path.substring(mainPath.length() + 1);
         }
         else
         {
            String[] parts = path.split("/");
            if (parts.length > 3)
               path = "\u2026/" + parts[parts.length - 2] + "/" + parts[parts.length - 1];
         }

         String label = SafeHtmlUtils.htmlEscape(name) +
               " <span class=\"" + ThemeStyles.INSTANCE.menuItemSubtitle() + "\"" +
               " title=\"" + SafeHtmlUtils.htmlEscape(worktree.getPath()) + "\">" +
               SafeHtmlUtils.htmlEscape(path) + "</span>";

         ImageResource2x check = worktree.isCurrent()
               ? new ImageResource2x(ThemeResources.INSTANCE.menuCheck2x())
               : null;

         ImageResource newSession = worktree.isCurrent() ? null : openWorktreeInNewSessionImage();

         String html = AppCommand.formatMenuLabel(
               check,
               label,
               true,
               null,
               newSession,
               constants_.openWorktreeInNewSession());

         // the current worktree is shown for orientation only; selecting it
         // just closes the menu
         Command command = worktree.isCurrent()
               ? () -> {}
               : new SwitchWorktreeCommand(worktree);
         menu.addItem(new MenuItem(html, true, command));
      }
   }

   private void populateEmptyMenu(final ToolbarPopupMenu menu)
   {
      menu.addSeparator(new CustomMenuItemSeparator()
      {
         @Override
         public Element createMainElement()
         {
            Label label = new Label(NO_BRANCHES_AVAILABLE);
            label.addStyleName(ThemeStyles.INSTANCE.menuSubheader());
            label.getElement().getStyle().setPaddingLeft(2, Unit.PX);
            return createSearchSeparator(label);
         }
      });
   }

   private void populateMenu(final ToolbarPopupMenu menu, final Map<String, List<String>> branchMap)
   {
      populateWorktrees(menu, StringUtil.notNull(lastSearchValue_).trim());

      if (branchMap.isEmpty())
      {
         if (menu.getItemCount() == 0)
            populateEmptyMenu(menu);
         return;
      }

      MapUtil.forEach(branchMap, new MapUtil.ForEachCommand<String, List<String>>()
      {
         int separatorCount_ = 0;

         @Override
         public void execute(final String caption, final List<String> branches)
         {
            // place commonly-used branches at the top
            Collections.sort(branches, new Comparator<String>()
            {
               private final String[] specialBranches_ = new String[] {
                     "master",
                     "develop",
                     "trunk"
               };

               @Override
               public int compare(String o1, String o2)
               {
                  for (String specialBranch : specialBranches_)
                  {
                     if (o1.endsWith(specialBranch))
                        return -1;
                     else if (o2.endsWith(specialBranch))
                        return 1;
                  }

                  return o1.compareToIgnoreCase(o2);
               }
            });

            menu.addSeparator(new CustomMenuItemSeparator()
            {
               @Override
               public Element createMainElement()
               {
                  String branchLabel = caption == LOCAL_BRANCHES
                        ? LOCAL_BRANCHES
                        : constants_.remoteBranchCaption(caption);
                  Label label = new Label(branchLabel);
                  label.addStyleName(ThemeStyles.INSTANCE.menuSubheader());
                  label.getElement().getStyle().setPaddingLeft(2, Unit.PX);

                  boolean useSearch =
                        (separatorCount_++ == 0) &&
                        (menu_.getItemCount() == 0);

                  Element mainEl = (useSearch)
                        ? createSearchSeparator(label)
                        : label.getElement();
                  return mainEl;
               }
            });
            menu.addSeparator();

            // truncate list when we have too many branches
            int n = 0;
            for (String branch : branches)
            {
               // skip detached branches
               if (branch.contains("HEAD detached at"))
                  continue;

               // skip HEAD branches
               if (branch.contains("HEAD ->"))
                  continue;

               // construct branch label without remotes prefix
               final String branchLabel = branch.replaceAll("^remotes/" + caption + "/", "");
               final String branchValue = branch.replaceAll("\\s+\\-\\>.*", "");

               // a branch checked out in another worktree opens that worktree
               WorktreeInfo worktree = worktreeBranches_.get(branchValue);
               if (worktree != null && caption == LOCAL_BRANCHES)
               {
                  menu.addItem(new MenuItem(
                        constants_.worktreeSuffix(branchLabel),
                        new SwitchWorktreeCommand(worktree)));
               }
               else
               {
                  menu.addItem(new MenuItem(
                        branchLabel,
                        new SwitchBranchCommand(branchLabel, branchValue)));
               }

               // update branch count
               if (n++ > MAX_BRANCHES)
                  break;
            }
         }
      });

      if (menuWidth_ != 0)
      {
         Element tableEl = menu_.getMenuTableElement();
         if (tableEl != null)
            tableEl.getStyle().setWidth(menuWidth_, Unit.PX);
      }

      menu.selectFirst();
   }

   protected void onBeforePopulateMenu(ToolbarPopupMenu rootMenu)
   {
      menu_.clearItems();

      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            focusSearch();
         }
      });
   }

   private Element createSearchSeparator(Label label)
   {
      final Element searchEl = searchWidget_.getElement();
      final Element labelEl = label.getElement();

      labelEl.getStyle().setFloat(Style.Float.LEFT);
      searchEl.getStyle().setFloat(Style.Float.RIGHT);

      if (!searchEl.hasClassName(RES.styles().searchWidget()))
         searchEl.addClassName(RES.styles().searchWidget());

      Element container = DOM.createDiv();
      container.appendChild(labelEl);
      container.appendChild(searchEl);

      // For some reason any attempts to click within the search box (when it
      // lives as a separator in the menu) end up losing focus immediately after
      // clicking, so we use a preview handler just to ensure focus doesn't leave
      // the search widget on click events.

      if (previewHandler_ != null)
      {
         previewHandler_.removeHandler();
         previewHandler_ = null;
      }

      previewHandler_ = Event.addNativePreviewHandler(new NativePreviewHandler()
      {
         @Override
         public void onPreviewNativeEvent(NativePreviewEvent preview)
         {
            switch (preview.getTypeInt())
            {
            case Event.ONKEYDOWN:
            case Event.ONKEYPRESS:
            case Event.ONKEYUP:
               onKeyEvent(preview);
               break;
            }
         }

         private void onKeyEvent(NativePreviewEvent preview)
         {
            NativeEvent event = preview.getNativeEvent();
            Element targetEl = event.getEventTarget().cast();
            if (!DomUtils.isDescendant(targetEl, searchEl))
               return;

            String searchValue = StringUtil.notNull(searchWidget_.getValue());
            if (!searchValue.equals(lastSearchValue_))
               searchValueChangeTimer_.schedule(200);
         }
      });

      return container;
   }

   private void onSearchValueChange()
   {
      lastSearchValue_ = searchWidget_.getValue();

      // fast path -- no query to use
      String query = StringUtil.notNull(lastSearchValue_).trim();
      if (query.isEmpty())
      {
         onBeforePopulateMenu(menu_);
         populateMenu(menu_, initialBranchMap_);
         return;
      }

      // iterate through initial branch map and copy branches
      // matching the current query. TODO: should we re-order
      // based on how 'close' a match we have?
      Map<String, List<String>> branchMap = new HashMap<>();
      for (String key : initialBranchMap_.keySet())
      {
         List<String> filteredBranches = new ArrayList<>();
         List<String> branches = initialBranchMap_.get(key);
         for (String branch : branches)
            if (branch.indexOf(query) != -1)
               filteredBranches.add(branch);

         if (!filteredBranches.isEmpty())
            branchMap.put(key, filteredBranches);
      }

      // re-populate
      menu_.clearItems();
      onBeforePopulateMenu(menu_);
      populateMenu(menu_, branchMap);
   }

   private void focusSearch()
   {
      searchWidget_.getInputElement().focus();
   }

   public interface Styles extends CssResource
   {
      String searchWidget();
   }

   public interface Resources extends ClientBundle
   {
      @Source("BranchToolbarButton.css")
      Styles styles();
   }

   protected final Provider<GitState> pVcsState_;

   private final ToolbarPopupMenu menu_;
   private int menuWidth_ = 0;
   private final SearchWidget searchWidget_;
   private Map<String, List<String>> initialBranchMap_;

   private boolean menuRebuildRequired_ = true;
   private JsVectorString branches_ = JsVectorString.createVector();
   private String worktreeKey_ = "";
   private List<WorktreeInfo> worktrees_ = new ArrayList<>();
   private Map<String, WorktreeInfo> worktreeBranches_ = new HashMap<>();
   private boolean openInNewSession_ = false;

   private HandlerRegistration previewHandler_;
   private HandlerRegistration clickPreviewHandler_;

   private String lastSearchValue_;
   private final Timer searchValueChangeTimer_;

   private static final int MAX_BRANCHES = 100;

   private static final String NO_BRANCH = constants_.noBranchParentheses();
   private static final String NO_BRANCHES_AVAILABLE = constants_.noBranchesAvailableParentheses();
   private static final String LOCAL_BRANCHES = constants_.localBranchesParentheses();
   private static final String WORKTREES = constants_.worktreesParentheses();

   private static Resources RES = GWT.create(Resources.class);
   static
   {
      RES.styles().ensureInjected();
   }
}
