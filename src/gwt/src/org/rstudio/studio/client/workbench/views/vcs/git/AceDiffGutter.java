/*
 * AceDiffGutter.java
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
package org.rstudio.studio.client.workbench.views.vcs.git;

import java.util.ArrayList;
import java.util.Set;
import java.util.Map.Entry;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.inject.Inject;

import org.rstudio.core.client.Invalidation;
import org.rstudio.core.client.container.SafeMap;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.vcs.DiffResult;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.common.vcs.GitServerOperations.PatchMode;
import org.rstudio.studio.client.workbench.views.source.events.DocFocusedEvent;
import org.rstudio.studio.client.workbench.views.source.events.SaveFileEvent;
import org.rstudio.studio.client.workbench.views.vcs.ViewVcsConstants;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.DiffChunk;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.Line;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.UnifiedParser;

public class AceDiffGutter implements SaveFileEvent.Handler, GitExpandDiffEvent.Handler, DocFocusedEvent.Handler
{
    
    public AceDiffGutter(AceEditor editor)
    {
        editor_ = editor;
        editor_.getWidget().addGitExpandDiffHandler(this);

        RStudioGinjector.INSTANCE.injectMembers(this);
        activeModifiedLines_ = new SafeMap<>();
    }

    @Inject
    private void initialize(EventBus events, GitServerOperations server, UserPrefs uiPrefs)
    {
        events_ = events;
        server_ = server;
        uiPrefs_ = uiPrefs;

        events_.addHandler(SaveFileEvent.TYPE, this);
        events_.addHandler(DocFocusedEvent.TYPE, this);
    }

    @Override
    public void onExpandDiff(GitExpandDiffEvent event) 
    {
        if (!activeModifiedLines_.containsKey(event.getLineNumber()))
            return;
        
        for (Line line: activeModifiedLines_.get(event.getLineNumber()).getLines()) {
            GWT.log(line.getType().getValue() + " " + line.getText());
        }
    }

    @Override
    public void onSaveFile(SaveFileEvent event) 
    {
        updateDecorations(event.getPath());
    }

    @Override
    public void onDocFocused(DocFocusedEvent event) 
    {
        updateDecorations(event.getPath());
    }

    private void updateDecorations(String path)
    {
        if (path == null)
            return;

        diffInvalidation_.invalidate();
        final Invalidation.Token token = diffInvalidation_.getInvalidationToken();

        server_.gitDiffFile(
            path,
            PatchMode.Working,
            2,
            true,
            uiPrefs_.gitDiffIgnoreWhitespace().getValue(),
            new SimpleRequestCallback<DiffResult>(vcsConstants_.diffError())
            {
                @Override
                public void onResponseReceived(DiffResult diffResult)
                {
                    if (token.isInvalid())
                        return;

                    drawDiffResult(diffResult);
                }
            }
        );
    }

    private void drawDiffResult(DiffResult diffResult) 
    {
        String response = diffResult.getDecodedValue();
        UnifiedParser parser = new UnifiedParser(response);
        parser.nextFilePair();
        
        for (Entry<Integer, DiffChunk> entry : activeModifiedLines_.entrySet())
        {
            int line = entry.getKey();
            editor_.getWidget().getEditor().getRenderer().removeGutterDecoration(line - 1, RES.styles().added());
            editor_.getWidget().getEditor().getRenderer().removeGutterDecoration(line - 1, RES.styles().modified());
        }
        activeModifiedLines_.clear();
        
        for (DiffChunk chunk; null != (chunk = parser.nextChunk());)
        {
            boolean chunkHasDeletion = false;
            for (Line line : chunk.getLines()) 
            {
                if (line.getType() == Line.Type.Deletion)
                {
                    chunkHasDeletion = true;
                    break;
                }
            }

            for (Line line : chunk.getLines())
            {
                if (line.getType() == Line.Type.Insertion) {
                    int newLine = line.getNewLine();
                    activeModifiedLines_.put(newLine, chunk);
                    editor_.getWidget().getEditor().getRenderer().addGutterDecoration(
                        newLine - 1, 
                        chunkHasDeletion ? RES.styles().modified() : RES.styles().added());
                }
            }
                
        }
    }

    AceEditor editor_;
    EventBus events_;
    GitServerOperations server_;
    UserPrefs uiPrefs_;

    private SafeMap<Integer, DiffChunk> activeModifiedLines_;
    
    private final Invalidation diffInvalidation_ = new Invalidation();

    interface Resources extends ClientBundle
    {
        @Source("AceDiffGutter.css")
        Styles styles();
    }

    interface Styles extends CssResource
    {
        public String modified();
        public String added();
    }
    public static Resources RES = GWT.create(Resources.class);
    static {
        RES.styles().ensureInjected();
    }
    private static final ViewVcsConstants vcsConstants_ = GWT.create(ViewVcsConstants.class);
    
}
