/*
 * CreateBranchToolbarButton.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.common.vcs.BranchesInfo;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.inject.Inject;

public class CreateBranchToolbarButton extends ToolbarButton
                                       implements ClickHandler
{
   @Inject
   public CreateBranchToolbarButton(GlobalDisplay globalDisplay,
                                    GitServerOperations gitServer)
   {
      super("Create Branch",
            StandardIcons.INSTANCE.mermaid(),
            (ClickHandler) null);
      
      globalDisplay_ = globalDisplay;
      gitServer_ = gitServer;
      
      addClickHandler(this);
   }
   
   @Override
   public void onClick(ClickEvent event)
   {
      globalDisplay_.promptForText(
            "Create Branch",
            "Create a new branch:",
            "",
            new OperationWithInput<String>()
            {
               @Override
               public void execute(String newBranch)
               {
                  onCreateBranch(newBranch);
               }
            });
   }
   
   private void onCreateBranch(final String newBranch)
   {
      gitServer_.gitListBranches(
            new ServerRequestCallback<BranchesInfo>()
            {
               @Override
               public void onResponseReceived(BranchesInfo branchesInfo)
               {
                  onBranchInfoReceived(newBranch, branchesInfo);
               }
               
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }
   
   private void onBranchInfoReceived(final String newBranch,
                                     final BranchesInfo branchesInfo)
   {
      // If we have a branch of this name already, prompt the
      // user and ask whether they'd like to check out a
      // new branch (overwriting the previous one) or if they'd
      // like to just check out that branch.
      boolean hasBranch = false;
      for (String branch : JsUtil.asIterable(branchesInfo.getBranches()))
      {
         if (branch.equals(newBranch))
         {
            hasBranch = true;
            break;
         }
      }
      
      if (hasBranch)
      {
         String message =
               "A branch named '" + newBranch + "' already exists. Would " +
               "you like to check out that branch, or overwrite that branch?";
         
         List<String> labels = new ArrayList<String>();
         labels.add("Checkout");
         labels.add("Overwrite");
         
         List<Operation> operations = new ArrayList<Operation>();
         operations.add(new Operation()
         {
            @Override
            public void execute()
            {
               onCheckout(newBranch);
            }
         });
         operations.add(new Operation()
         {
            @Override
            public void execute()
            {
               onCreate(newBranch);
            }
         });
         
         globalDisplay_.showGenericDialog(
               MessageDialog.INFO,
               "Branch Already Exists",
               message,
               labels,
               operations,
               1);
      }
      else
      {
         onCreate(newBranch);
      }
   }
   
   private void onCreate(final String newBranch)
   {
      gitServer_.gitCreateBranch(
            newBranch,
            new ServerRequestCallback<ConsoleProcess>()
            {
               @Override
               public void onResponseReceived(ConsoleProcess process)
               {
                  showConsoleProcessDialog(process);
               }
               
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
               
            });
   }
   
   private void onCheckout(final String newBranch)
   {
      gitServer_.gitCheckout(
            newBranch,
            new ServerRequestCallback<ConsoleProcess>()
            {
               @Override
               public void onResponseReceived(ConsoleProcess process)
               {
                  showConsoleProcessDialog(process);
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }
   
   private void showConsoleProcessDialog(ConsoleProcess process)
   {
      new ConsoleProgressDialog(process, gitServer_).showModal();
   }
   
   private final GlobalDisplay globalDisplay_;
   private final GitServerOperations gitServer_;
}
