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

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.inject.Inject;

import org.rstudio.core.client.Invalidation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.vcs.DiffResult;
import org.rstudio.studio.client.common.vcs.GitServerOperations;
import org.rstudio.studio.client.common.vcs.GitServerOperations.PatchMode;
import org.rstudio.studio.client.workbench.views.source.events.SaveFileEvent;
import org.rstudio.studio.client.workbench.views.vcs.ViewVcsConstants;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.DiffChunk;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.Line;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.UnifiedParser;

public class AceDiffGutter implements SaveFileEvent.Handler
{
    
    public AceDiffGutter(AceEditor editor)
    {
        editor_ = editor;
        RStudioGinjector.INSTANCE.injectMembers(this);
    }

    @Inject
    private void initialize(EventBus events, GitServerOperations server, UserPrefs uiPrefs)
    {
        events_ = events;
        server_ = server;
        uiPrefs_ = uiPrefs;
        events_.addHandler(SaveFileEvent.TYPE, this);
    }

    @Override
    public void onSaveFile(SaveFileEvent event) {
        final String path = event.getPath();
        if (path == null)
            return;

        diffInvalidation_.invalidate();
        final Invalidation.Token token = diffInvalidation_.getInvalidationToken();

        server_.gitDiffFile(
            path,
            PatchMode.Working,
            5,
            true,
            uiPrefs_.gitDiffIgnoreWhitespace().getValue(),
            new SimpleRequestCallback<DiffResult>(vcsConstants_.diffError())
            {
                @Override
                public void onResponseReceived(DiffResult diffResult)
                {
                    if (token.isInvalid())
                        return;

                    String response = diffResult.getDecodedValue();
                    UnifiedParser parser = new UnifiedParser(response);
                    parser.nextFilePair();
                    
                    for (Integer newLine: activeModifiedLines_)
                    {
                        editor_.getWidget().getEditor().getRenderer().removeGutterDecoration(newLine - 1, RES.styles().added());
                        editor_.getWidget().getEditor().getRenderer().removeGutterDecoration(newLine - 1, RES.styles().modified());
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
                                activeModifiedLines_.add(newLine);
                                editor_.getWidget().getEditor().getRenderer().addGutterDecoration(
                                    newLine - 1, 
                                    chunkHasDeletion ? RES.styles().modified() : RES.styles().added());
                            }
                        }
                            
                    }
                }
            }
        );
    }

    AceEditor editor_;
    EventBus events_;
    GitServerOperations server_;
    UserPrefs uiPrefs_;

    private ArrayList<Integer> activeModifiedLines_ = new ArrayList<>();

    private final Invalidation diffInvalidation_ = new Invalidation();

    interface Resources extends ClientBundle
    {
        @Source("AceDiffGutter.css")
        Styles styles();
    }

    interface Styles extends CssResource
    {
        String modified();
        String added();
    }
    public static Resources RES = GWT.create(Resources.class);
    static {
        RES.styles().ensureInjected();
    }
    private static final ViewVcsConstants vcsConstants_ = GWT.create(ViewVcsConstants.class);
}
