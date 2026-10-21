/*
 * TextEditingTargetGitDiffGutter.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.vcs.GitDiffBaseContents;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.GitDiffGutterNative;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.git.model.GitState;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.inject.Provider;

// Marks lines changed relative to the Git index in a document's gutter. The
// base version is fetched once and whenever the index may have moved; the
// diff against the live buffer is computed client-side as the user types.
public class TextEditingTargetGitDiffGutter
{
   public TextEditingTargetGitDiffGutter(DocDisplay docDisplay,
                                         DocUpdateSentinel sentinel,
                                         ArrayList<HandlerRegistration> releaseOnDismiss)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);

      docDisplay_ = docDisplay;
      sentinel_ = sentinel;

      if (isGitSession())
      {
         // The base only moves when the index does (commit, stage, checkout);
         // status refreshes driven by file changes leave it alone.
         releaseOnDismiss.add(gitState_.get().addVcsRefreshHandler(event ->
         {
            if (event.getReason() != VcsRefreshEvent.Reason.FileChange)
               scheduleRefresh();
         }, false));
      }

      // Commits made outside the IDE (e.g. in the Terminal) produce no refresh,
      // so re-fetch when the user comes back to the editor.
      releaseOnDismiss.add(docDisplay_.addEditorFocusHandler(event -> scheduleRefresh()));

      // Saving only changes the working copy, so the base is unaffected --
      // except that Save As gives the document a new path.
      releaseOnDismiss.add(docDisplay_.addSaveCompletedHandler(event -> sync()));
      releaseOnDismiss.add(prefs_.showGitDiffGutter().addValueChangeHandler(event -> sync()));
      releaseOnDismiss.add(prefs_.gitDiffIgnoreWhitespace().addValueChangeHandler(event ->
      {
         if (gutter_ != null)
            gutter_.setIgnoreWhitespace(event.getValue());
      }));
      releaseOnDismiss.add(() ->
      {
         dismissed_ = true;
         detach();
      });

      sync();
   }

   public void goToNextChange()
   {
      if (gutter_ != null)
         gutter_.goToNextChange();
   }

   public void goToPreviousChange()
   {
      if (gutter_ != null)
         gutter_.goToPreviousChange();
   }

   public void revertChangeAtCursor()
   {
      if (gutter_ != null)
         gutter_.revertChangeAtCursor();
   }

   @Inject
   private void initialize(GitServerOperations server,
                           Provider<GitState> gitState,
                           UserPrefs prefs,
                           Session session)
   {
      server_ = server;
      gitState_ = gitState;
      prefs_ = prefs;
      session_ = session;
   }

   private boolean isGitSession()
   {
      return VCSConstants.GIT_ID.equals(session_.getSessionInfo().getVcsName());
   }

   private boolean isEnabled()
   {
      return prefs_.showGitDiffGutter().getValue() &&
             isGitSession() &&
             sentinel_.getPath() != null &&
             docDisplay_ instanceof AceEditor;
   }

   private void sync()
   {
      if (!isEnabled())
      {
         detach();
         return;
      }

      if (gutter_ == null)
         attach();
      else if (!StringUtil.equals(basePath_, sentinel_.getPath()))
         refresh();
   }

   private void attach()
   {
      if (attaching_)
         return;

      attaching_ = true;
      AceEditor.loadDiffExtension(() ->
      {
         attaching_ = false;
         if (dismissed_ || gutter_ != null || !isEnabled())
            return;

         AceEditor editor = (AceEditor) docDisplay_;
         gutter_ = GitDiffGutterNative.create(editor.getWidget().getEditor());
         gutter_.setIgnoreWhitespace(prefs_.gitDiffIgnoreWhitespace().getValue());
         refresh();
      });
   }

   private void detach()
   {
      refreshTimer_.cancel();
      invalidation_.invalidate();
      basePath_ = null;

      if (gutter_ != null)
      {
         gutter_.dispose();
         gutter_ = null;
      }
   }

   // Refresh triggers arrive in bursts (a commit fires several, a focus
   // change follows an attach), and each one costs a round trip per document.
   private void scheduleRefresh()
   {
      if (gutter_ != null)
         refreshTimer_.schedule(REFRESH_DELAY_MS);
   }

   private void refresh()
   {
      if (gutter_ == null)
         return;

      final String path = sentinel_.getPath();
      basePath_ = path;

      invalidation_.invalidate();
      final Invalidation.Token token = invalidation_.getInvalidationToken();

      server_.gitDiffBaseContents(
            path,
            StringUtil.notNull(sentinel_.getEncoding()),
            new ServerRequestCallback<GitDiffBaseContents>()
            {
               @Override
               public void onResponseReceived(GitDiffBaseContents response)
               {
                  if (token.isInvalid() || gutter_ == null)
                     return;

                  gutter_.setBase(response.isTracked() ? response.getContents() : null);
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
               }
            });
   }

   private final DocDisplay docDisplay_;
   private final DocUpdateSentinel sentinel_;

   private GitDiffGutterNative gutter_;
   private String basePath_;
   private boolean attaching_;
   private boolean dismissed_;

   private final Invalidation invalidation_ = new Invalidation();
   private final Timer refreshTimer_ = new Timer()
   {
      @Override
      public void run()
      {
         refresh();
      }
   };

   private GitServerOperations server_;
   private Provider<GitState> gitState_;
   private UserPrefs prefs_;
   private Session session_;

   private static final int REFRESH_DELAY_MS = 500;
}
