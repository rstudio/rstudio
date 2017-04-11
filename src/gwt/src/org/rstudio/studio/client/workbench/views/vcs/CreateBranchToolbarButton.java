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
import org.rstudio.core.client.Functional;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ProcessExitEvent;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.common.vcs.BranchesInfo;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.common.vcs.RemotesInfo;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.vcs.CreateBranchDialog.Input;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;

import com.google.gwt.core.client.JsArray;
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
      super("New Branch",
            new ImageResource2x(StandardIcons.INSTANCE.mermaid2x()),
            (ClickHandler) null);
      
      globalDisplay_ = globalDisplay;
      gitServer_ = gitServer;
      
      addClickHandler(this);
   }
   
   @Override
   public void onClick(ClickEvent event)
   {
      gitServer_.gitListRemotes(new ServerRequestCallback<JsArray<RemotesInfo>>()
      {
         @Override
         public void onResponseReceived(JsArray<RemotesInfo> remotes)
         {
            onListRemotes(remotes);
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }
   
   private void onListRemotes(final JsArray<RemotesInfo> remotesInfo)
   {
      final OperationWithInput<CreateBranchDialog.Input> onCreateBranch =
            new OperationWithInput<CreateBranchDialog.Input>()
            {
               @Override
               public void execute(Input input)
               {
                  onCreateBranch(input);
               }
            };
            
      final OperationWithInput<AddRemoteDialog.Input> onAddRemote =
            new OperationWithInput<AddRemoteDialog.Input>()
            {
               @Override
               public void execute(AddRemoteDialog.Input input)
               {
                  onAddRemote(input);
               }
            };
            
      createBranchDialog_ = new CreateBranchDialog(
            "New Branch",
            remotesInfo,
            onCreateBranch,
            onAddRemote);
      
      createBranchDialog_.showModal();
   }
   
   private void onAddRemote(final AddRemoteDialog.Input input)
   {
      gitServer_.gitAddRemote(
            input.getName(),
            input.getUrl(),
            new ServerRequestCallback<JsArray<RemotesInfo>>()
            {
               @Override
               public void onResponseReceived(JsArray<RemotesInfo> remotesInfo)
               {
                  if (createBranchDialog_ != null)
                     createBranchDialog_.setRemotes(input.getName(), remotesInfo);
               }
               
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }
   
   private void onCreateBranch(final CreateBranchDialog.Input input)
   {
      gitServer_.gitListBranches(
            new ServerRequestCallback<BranchesInfo>()
            {
               @Override
               public void onResponseReceived(BranchesInfo branchesInfo)
               {
                  onBranchInfoReceived(input, branchesInfo);
               }
               
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }
   
   private void onBranchInfoReceived(final CreateBranchDialog.Input input,
                                     final BranchesInfo branchesInfo)
   {
      // If we have a local branch of this name already, prompt the user and ask
      // whether they'd like to check out a new branch (overwriting the previous
      // one) or if they'd like to just check out that branch.
      if (promptUserRegardingLocalBranchOfSameName(input, branchesInfo))
         return;
      
      // If we have a remote branch of this name already, prompt the user and ask
      // whether they'd like to pull and check out that branch, or generate their
      // own.
      if (promptUserRegardingRemoteBranchOfSameName(input, branchesInfo))
         return;
      
      // We ascertained that creating a branch won't put the user out-of-sync with
      // the declared remote -- go ahead with creation.
      onCreate(input);
   }
   
   private boolean promptUserRegardingLocalBranchOfSameName(
         final CreateBranchDialog.Input input,
         final BranchesInfo branchesInfo)
   {
      boolean hasBranch = false;
      for (String branch : JsUtil.asIterable(branchesInfo.getBranches()))
      {
         if (branch.equals(input.getBranch()))
         {
            hasBranch = true;
            break;
         }
      }
      
      if (hasBranch)
      {
         String message =
               "A local branch named '" + input.getBranch() + "' already exists. " +
               "Would you like to check out that branch, or overwrite it?";
         
         List<String> labels = new ArrayList<String>();
         labels.add("Checkout");
         labels.add("Overwrite");
         labels.add("Cancel");
         
         List<Operation> operations = new ArrayList<Operation>();
         operations.add(new Operation()
         {
            @Override
            public void execute()
            {
               onCheckout(input);
            }
         });
         operations.add(new Operation()
         {
            @Override
            public void execute()
            {
               onCreate(input);
            }
         });
         operations.add(new Operation()
         {
            @Override
            public void execute()
            {
               // no-op
            }
         });
         
         
         globalDisplay_.showGenericDialog(
               MessageDialog.INFO,
               "Local Branch Already Exists",
               message,
               labels,
               operations,
               2);
      }
      
      return hasBranch;
   }
   
   private boolean promptUserRegardingRemoteBranchOfSameName(
         final CreateBranchDialog.Input input,
         final BranchesInfo branchesInfo)
   {
      final String targetBranch = "remotes/" + input.getRemote() + "/" + input.getBranch();
      final String remoteBranch = Functional.find(
            branchesInfo.getBranches(),
            new Functional.Predicate<String>()
            {
               @Override
               public boolean test(String branch)
               {
                  return branch.equals(targetBranch);
               }
            });
      
      if (remoteBranch != null)
      {
         String message =
               "A remote branch named '" + input.getBranch() + "' already exists " +
               "on the remote repository '" + input.getRemote() + "'. Would you like " +
               "to check out that branch?";
         
         List<String> labels = new ArrayList<String>();
         labels.add("Checkout");
         labels.add("Cancel");
         
         List<Operation> operations = new ArrayList<Operation>();
         operations.add(new Operation()
         {
            @Override
            public void execute()
            {
               onCheckoutRemote(input);
            }
         });
         operations.add(new Operation()
         {
            @Override
            public void execute()
            {
               // no-op
            }
         });
         
         globalDisplay_.showGenericDialog(
               MessageDialog.INFO,
               "Remote Branch Already Exists",
               message,
               labels,
               operations,
               1);
      }
      
      return remoteBranch != null;
   }
   
   private void onCreate(final CreateBranchDialog.Input input)
   {
      gitServer_.gitCreateBranch(
            input.getBranch(),
            new ServerRequestCallback<ConsoleProcess>()
            {
               @Override
               public void onResponseReceived(ConsoleProcess process)
               {
                  final ConsoleProgressDialog dialog =
                        new ConsoleProgressDialog(process, gitServer_);
                  dialog.showModal();
                  
                  process.addProcessExitHandler(new ProcessExitEvent.Handler()
                  {
                     @Override
                     public void onProcessExit(ProcessExitEvent event)
                     {
                        onCreateBranchFinished(input, event, dialog);
                     }
                  });
               }
               
               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
               
            });
   }
   
   private void onCreateBranchFinished(final CreateBranchDialog.Input input,
                                       final ProcessExitEvent event,
                                       final ConsoleProgressDialog dialog)
   {
      if (event.getExitCode() == 0 && input.getPush())
      {
         gitServer_.gitPushBranch(
               input.getBranch(),
               input.getRemote(),
               new ServerRequestCallback<ConsoleProcess>()
               {
                  @Override
                  public void onResponseReceived(ConsoleProcess process)
                  {
                     dialog.attachToProcess(process);
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     Debug.logError(error);
                  }
               });
      }
   }
   
   private void onCheckout(final CreateBranchDialog.Input input)
   {
      gitServer_.gitCheckout(
            input.getBranch(),
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
   
   private void onCheckoutRemote(final CreateBranchDialog.Input input)
   {
      gitServer_.gitCheckoutRemote(
            input.getBranch(),
            input.getRemote(),
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
   
   private CreateBranchDialog createBranchDialog_;
   
   private final GlobalDisplay globalDisplay_;
   private final GitServerOperations gitServer_;
}
