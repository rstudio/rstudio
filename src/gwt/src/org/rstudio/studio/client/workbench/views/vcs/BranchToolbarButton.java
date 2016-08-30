/*
 * BranchToolbarButton.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.vcs;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.inject.Inject;
import com.google.inject.Provider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.rstudio.core.client.MapUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.WidgetHandlerRegistration;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.widget.ScrollableToolbarPopupMenu;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshHandler;
import org.rstudio.studio.client.workbench.views.vcs.git.model.GitState;

public class BranchToolbarButton extends ToolbarButton
                                 implements HasValueChangeHandlers<String>,
                                            VcsRefreshHandler
{
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

   @Inject
   public BranchToolbarButton(final Provider<GitState> pVcsState)
   {
      super("",
            StandardIcons.INSTANCE.empty_command(),
            new ScrollableToolbarPopupMenu());
      pVcsState_ = pVcsState;

      setTitle("Switch branch");

      new WidgetHandlerRegistration(this)
      {
         @Override
         protected HandlerRegistration doRegister()
         {
            return pVcsState.get().addVcsRefreshHandler(
                                                BranchToolbarButton.this, true);
         }
      };
   }
   
   public void setBranchCaption(String caption)
   {
      if (StringUtil.isNullOrEmpty(caption))
         caption = NO_BRANCH;
      
      setText(caption);
   }

   @Override
   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler)
   {
      return addHandler(handler, ValueChangeEvent.getType());
   }

   @Override
   public void onVcsRefresh(VcsRefreshEvent event)
   {
      ToolbarPopupMenu rootMenu = getMenu();
      rootMenu.setAutoHideRedundantSeparators(false);
      rootMenu.clearItems();
      JsArrayString branches = pVcsState_.get().getBranchInfo().getBranches();
      
      // separate branches based on remote name
      Map<String, List<String>> branchMap = new LinkedHashMap<String, List<String>>();
      List<String> localBranches = new ArrayList<String>();
      branchMap.put("(local branches)", localBranches);
      for (String branch : JsUtil.asIterable(branches))
      {
         if (branch.startsWith("remotes/"))
         {
            JsArrayString parts = StringUtil.split(branch, "/");
            if (parts.length() > 2)
            {
               String remote = parts.get(1);
               if (!branchMap.containsKey(remote))
                  branchMap.put(remote, new ArrayList<String>());
               List<String> remoteBranches = branchMap.get(remote);
               remoteBranches.add(branch);
            }
         }
         else
         {
            localBranches.add(branch);
         }
      }
      
      onBeforePopulateMenu(rootMenu);
      
      // populate local branches
      populateMenu(rootMenu, branchMap);
   }
   
   private void populateMenu(final ToolbarPopupMenu menu, final Map<String, List<String>> branchMap)
   {
      MapUtil.forEach(branchMap, new MapUtil.ForEachCommand<String, List<String>>()
      {
         @Override
         public void execute(String caption, List<String> branches)
         {
            menu.addSeparator(caption, false);
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
               menu.addItem(new MenuItem(
                     branchLabel,
                     new SwitchBranchCommand(branchLabel, branchValue)));
            }
         }
      });
   }

   protected void onBeforePopulateMenu(ToolbarPopupMenu rootMenu)
   {
   }

   protected final Provider<GitState> pVcsState_;

   private static final String NO_BRANCH = "(No branch)";
}
