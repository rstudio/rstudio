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
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.vcs.GitDiffBaseContents;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.common.vcs.VCSConstants;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.GitDiffGutterNative;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.GitDiffSplitNative;
import org.rstudio.studio.client.workbench.views.source.events.SourceFileSavedEvent;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.vcs.common.events.VcsRefreshEvent;
import org.rstudio.studio.client.workbench.views.vcs.git.model.GitState;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.inject.Inject;
import com.google.inject.Provider;

// Marks lines changed relative to the Git index in a document's gutter. The
// base version is fetched once and whenever the index may have moved; the
// diff against the live buffer is computed client-side as the user types.
// A split editor view gets a gutter of its own, fed the same base, and a
// side-by-side diff view shows the base in a pane of its own, driven by the
// primary gutter's diff.
//
// Only documents on screen fetch: a Git refresh reaches every open document,
// and each fetch runs git on the session's main thread, so a hidden document
// notes that its base may be stale and fetches when it is next shown. A fetch
// names the base it already holds, and comes back empty when the index still
// has that version.
public class TextEditingTargetGitDiffGutter
{
   // closeDiffSplit closes the document's side-by-side view, for when the
   // gutter driving it goes away; see detach().
   public TextEditingTargetGitDiffGutter(DocDisplay docDisplay,
                                         DocUpdateSentinel sentinel,
                                         ArrayList<HandlerRegistration> releaseOnDismiss,
                                         Command closeDiffSplit)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);

      docDisplay_ = docDisplay;
      sentinel_ = sentinel;
      closeDiffSplit_ = closeDiffSplit;

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
      // except that Save As gives the document a new path. A rename (Files
      // pane, tab menu) changes the path without a save of its own and is
      // announced on the bus instead.
      releaseOnDismiss.add(docDisplay_.addSaveCompletedHandler(event -> sync()));
      releaseOnDismiss.add(events_.addHandler(SourceFileSavedEvent.TYPE, event ->
      {
         if (StringUtil.equals(event.getDocId(), sentinel_.getId()))
            sync();
      }));
      releaseOnDismiss.add(prefs_.showGitDiffGutter().addValueChangeHandler(event -> sync()));
      releaseOnDismiss.add(prefs_.gitDiffIgnoreWhitespace().addValueChangeHandler(event ->
      {
         if (gutter_ != null)
            gutter_.setIgnoreWhitespace(event.getValue());
         if (splitGutter_ != null)
            splitGutter_.setIgnoreWhitespace(event.getValue());
      }));
      releaseOnDismiss.add(() ->
      {
         dismissed_ = true;
         setSplitEditor(null);
         detach();
      });

      sync();
   }

   // The split view comes and goes independently of the gutter; it gets its
   // own decorator once both exist.
   public void setSplitEditor(AceEditor editor)
   {
      if (splitGutter_ != null)
      {
         splitGutter_.dispose();
         splitGutter_ = null;
      }

      if (splitFocusRegistration_ != null)
      {
         splitFocusRegistration_.removeHandler();
         splitFocusRegistration_ = null;
      }

      splitEditor_ = editor;
      if (splitEditor_ == null)
         return;

      // the same re-fetch on focus as the primary view: a commit made in the
      // Terminal is picked up by clicking into either view
      splitFocusRegistration_ = splitEditor_.addEditorFocusHandler(event -> scheduleRefresh());
      if (gutter_ != null)
         attachSplit();
   }

   // The side-by-side pane comes and goes with the editor split state; the
   // base editor inside it is created once the gutter exists to drive it.
   public void setDiffSplitWidget(GitDiffSplitWidget widget)
   {
      disposeDiffSplit();

      diffSplitWidget_ = widget;
      if (diffSplitWidget_ != null && gutter_ != null)
         attachDiffSplit();
   }

   // Whether a side-by-side view can be shown for this document at all: the
   // gutter has to be in play (Git project, file on disk, markers enabled).
   public boolean isAvailable()
   {
      return isEnabled();
   }

   // The cursor-driven actions act on the view that holds the cursor.
   public void goToNextChange(DocDisplay display)
   {
      GitDiffGutterNative gutter = gutterFor(display);
      if (gutter != null)
         gutter.goToNextChange();
   }

   public void goToPreviousChange(DocDisplay display)
   {
      GitDiffGutterNative gutter = gutterFor(display);
      if (gutter != null)
         gutter.goToPreviousChange();
   }

   public void revertChangeAtCursor(DocDisplay display)
   {
      GitDiffGutterNative gutter = gutterFor(display);
      if (gutter != null)
         gutter.revertChangeAtCursor();
   }

   public void showChangeAtCursor(DocDisplay display)
   {
      GitDiffGutterNative gutter = gutterFor(display);
      if (gutter != null)
         gutter.showChangeAtCursor();
   }

   // Called as the document becomes and stops being the active one (see
   // TextEditingTarget); a refresh that came in while hidden is run now that
   // the markers are seen.
   public void onActivate()
   {
      active_ = true;
      if (refreshDeferred_)
      {
         refreshDeferred_ = false;
         refresh();
      }
   }

   public void onDeactivate()
   {
      active_ = false;
   }

   // Whether the markers are on screen. The active document is, but so is
   // the document showing in each other source column: a column's document
   // is deactivated when another column becomes active, and stays in view.
   private boolean isShown()
   {
      return active_ || DomUtils.isEffectivelyVisible(docDisplay_.getElement());
   }

   private GitDiffGutterNative gutterFor(DocDisplay display)
   {
      return (display == splitEditor_) ? splitGutter_ : gutter_;
   }

   @Inject
   private void initialize(EventBus events,
                           GitServerOperations server,
                           Provider<GitState> gitState,
                           UserPrefs prefs,
                           Session session)
   {
      events_ = events;
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
      else if (!StringUtil.equals(basePath_, sentinel_.getPath()) ||
               !StringUtil.equals(baseEncoding_, currentEncoding()))
         refresh();
   }

   // Reopen with Encoding replaces the document's contents and encoding
   // without a save, so the base has to be re-fetched (and re-decoded).
   public void documentReloaded()
   {
      sync();
   }

   private String currentEncoding()
   {
      return StringUtil.notNull(sentinel_.getEncoding());
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

         gutter_ = createGutter((AceEditor) docDisplay_);
         if (splitEditor_ != null)
            attachSplit();
         if (diffSplitWidget_ != null)
            attachDiffSplit();
         refresh();
      });
   }

   private void attachDiffSplit()
   {
      diffSplit_ = GitDiffSplitNative.create(gutter_, diffSplitWidget_.getElement());

      // the same re-fetch on focus as the editor views: an index moved from
      // outside (staging in the Terminal) is picked up on a click into the
      // base pane too
      diffSplit_.onFocus(() -> scheduleRefresh());
      diffSplit_.setUntracked(untracked_);
      diffSplitWidget_.setView(diffSplit_);
   }

   private void disposeDiffSplit()
   {
      if (diffSplit_ == null)
         return;

      diffSplitWidget_.setView(null);
      diffSplit_.dispose();
      diffSplit_ = null;
   }

   private void attachSplit()
   {
      splitGutter_ = createGutter(splitEditor_);
      splitGutter_.setBase(baseContents_);
   }

   // A null base comes with whether the document is known to have none
   // (untracked), as opposed to one not at hand -- not fetched yet, or not
   // fetchable -- which the side-by-side pane tells apart.
   private void setBase(String contents, String id, boolean untracked)
   {
      baseContents_ = contents;
      baseId_ = (contents == null) ? null : id;
      untracked_ = untracked;
      if (gutter_ != null)
         gutter_.setBase(contents);
      if (splitGutter_ != null)
         splitGutter_.setBase(contents);
      if (diffSplit_ != null)
         diffSplit_.setUntracked(untracked);
   }

   private GitDiffGutterNative createGutter(AceEditor editor)
   {
      GitDiffGutterNative gutter = GitDiffGutterNative.create(editor.getWidget().getEditor());
      gutter.setIgnoreWhitespace(prefs_.gitDiffIgnoreWhitespace().getValue());
      gutter.setBaseRefresher(this::refreshBase);
      return gutter;
   }

   // A revert first re-fetches the base, so that it restores the index as it
   // is rather than as it was when last fetched (the focus-driven re-fetch is
   // delayed, and the index can move under an open document meanwhile). The
   // callback runs once the fetch has been applied -- or has failed, or the
   // document has moved to another path, in which case the base has been
   // dropped and there is nothing left to revert to.
   private void refreshBase(JavaScriptObject callback)
   {
      baseCallbacks_.add(callback);
      if (gutter_ == null)
         runBaseCallbacks();
      else
         refresh();
   }

   private void runBaseCallbacks()
   {
      ArrayList<JavaScriptObject> callbacks = new ArrayList<>(baseCallbacks_);
      baseCallbacks_.clear();
      for (JavaScriptObject callback : callbacks)
         invoke(callback);
   }

   private static native void invoke(JavaScriptObject callback) /*-{
      callback();
   }-*/;

   private void detach()
   {
      refreshTimer_.cancel();
      refreshDeferred_ = false;
      invalidation_.invalidate();
      basePath_ = null;
      baseContents_ = null;
      baseId_ = null;
      untracked_ = false;

      disposeDiffSplit();

      // Without a gutter there is no diff to show side by side, so an open
      // view closes along with it (e.g. the markers turned off in Global
      // Options) -- the view itself right away, not once the split state
      // has made its round trip to the server (see
      // TextEditingTarget.closeDiffSplit). Not on dismiss, where the split
      // is torn down regardless.
      if (diffSplitWidget_ != null && !dismissed_)
         closeDiffSplit_.execute();

      if (gutter_ != null)
      {
         gutter_.dispose();
         gutter_ = null;
      }

      if (splitGutter_ != null)
      {
         splitGutter_.dispose();
         splitGutter_ = null;
      }

      // a disposed gutter has nothing left to revert, so these are harmless
      runBaseCallbacks();
   }

   // Refresh triggers arrive in bursts (a commit fires several, a focus
   // change follows an attach), and each one costs a round trip per document.
   private void scheduleRefresh()
   {
      if (gutter_ == null)
         return;

      if (!isShown())
         refreshDeferred_ = true;
      else
         refreshTimer_.schedule(REFRESH_DELAY_MS);
   }

   private void refresh()
   {
      if (gutter_ == null)
         return;

      // this fetch covers whatever a pending scheduled one was for
      refreshTimer_.cancel();

      final String path = sentinel_.getPath();
      final String encoding = currentEncoding();
      if (!StringUtil.equals(basePath_, path) || !StringUtil.equals(baseEncoding_, encoding))
      {
         // The old base must not outlive the path (Save As) or the encoding
         // (Reopen with Encoding) it was fetched for: a revert in the gap
         // before the new one arrives would restore the wrong contents.
         basePath_ = path;
         baseEncoding_ = encoding;
         setBase(null, null, false);

         // Nor must a revert still waiting on the old base: it would run
         // against the new path's base, whose chunks can share the old
         // ones' coordinates. Settled now, it finds nothing to revert.
         runBaseCallbacks();
      }

      // A hidden document waits to be shown -- unless a revert is waiting
      // on the fetch, which cannot be left hanging.
      if (!isShown() && baseCallbacks_.isEmpty())
      {
         refreshDeferred_ = true;
         return;
      }

      invalidation_.invalidate();
      final Invalidation.Token token = invalidation_.getInvalidationToken();

      server_.gitDiffBaseContents(
            path,
            encoding,
            StringUtil.notNull(baseId_),
            new ServerRequestCallback<GitDiffBaseContents>()
            {
               @Override
               public void onResponseReceived(GitDiffBaseContents response)
               {
                  if (token.isInvalid() || gutter_ == null)
                     return;

                  // the encoding can change under a request in flight
                  if (!StringUtil.equals(encoding, currentEncoding()))
                  {
                     refresh();
                     return;
                  }

                  // the base held is the one the index still has
                  if (response.isUnchanged() && baseContents_ != null)
                  {
                     runBaseCallbacks();
                     return;
                  }

                  if (response.isTracked())
                     setBase(response.getContents(), response.getId(), false);
                  else
                     setBase(null, null, true);
                  runBaseCallbacks();
               }

               @Override
               public void onError(ServerError error)
               {
                  Debug.logError(error);
                  if (token.isInvalid() || gutter_ == null)
                     return;

                  // A base that could not be re-fetched is not one to keep
                  // showing, let alone revert to; the next refresh (focus,
                  // Git operation) brings the markers back.
                  setBase(null, null, false);
                  runBaseCallbacks();
               }
            });
   }

   private final DocDisplay docDisplay_;
   private final DocUpdateSentinel sentinel_;
   private final Command closeDiffSplit_;

   private GitDiffGutterNative gutter_;
   private GitDiffGutterNative splitGutter_;
   private GitDiffSplitNative diffSplit_;
   private GitDiffSplitWidget diffSplitWidget_;
   private AceEditor splitEditor_;
   private HandlerRegistration splitFocusRegistration_;
   private String basePath_;
   private String baseEncoding_;
   private String baseContents_;
   private String baseId_;
   private boolean untracked_;
   private boolean attaching_;
   private boolean dismissed_;
   private boolean active_;
   private boolean refreshDeferred_;

   private final ArrayList<JavaScriptObject> baseCallbacks_ = new ArrayList<>();
   private final Invalidation invalidation_ = new Invalidation();
   private final Timer refreshTimer_ = new Timer()
   {
      @Override
      public void run()
      {
         refresh();
      }
   };

   private EventBus events_;
   private GitServerOperations server_;
   private Provider<GitState> gitState_;
   private UserPrefs prefs_;
   private Session session_;

   private static final int REFRESH_DELAY_MS = 500;
}
