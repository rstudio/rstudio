/*
 * JobsConstants.java
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
package org.rstudio.studio.client.workbench.views.jobs;

public interface JobsConstants extends com.google.gwt.i18n.client.Messages {

    @Key("backgroundJobsRunningCaption")
    String backgroundJobsRunningCaption();

    @Key("backgroundJobsRunningMessage")
    String backgroundJobsRunningMessage(String localJobMessage);

    @Key("backgroundJobsUnfinished")
    String backgroundJobsUnfinished();

    @Key("backgroundJobUnfinished")
    String backgroundJobUnfinished();

    @Key("backgroundJobsTitle")
    String backgroundJobsTitle();

    @Key("cannotRetrieveJobOutputCaption")
    String cannotRetrieveJobOutputCaption();

    @Key("runningState")
    String runningState();

    @Key("idleState")
    String idleState();

    @Key("cancelledState")
    String cancelledState();

    @Key("failedState")
    String failedState();

    @Key("succeededState")
    String succeededState();

    @Key("unknownState")
    String unknownState(int state);

    @Key("removeCompletedBackgroundJobsCaption")
    String removeCompletedBackgroundJobsCaption();

    @Key("removeCompletedBackgroundJobsMessage")
    String removeCompletedBackgroundJobsMessage();

    @Key("removeJobsLabel")
    String removeJobsLabel();

    @Key("cancelLabel")
    String cancelLabel();

    @Key("numJobsLabel")
    String numJobsLabel(int jobs);

    @Key("runSelectionAsBackgroundJobCaption")
    String runSelectionAsBackgroundJobCaption();

    @Key("runScriptAsBackgroundJobCaption")
    String runScriptAsBackgroundJobCaption();

    @Key("stopJobTitle")
    String stopJobTitle();

    @Key("selectJobText")
    String selectJobText();

    @Key("waitingText")
    String waitingText();

    @Key("rScriptLabel")
    String rScriptLabel();

    @Key("workingDirectoryCaption")
    String workingDirectoryCaption();

    @Key("dontCopyText")
    String dontCopyText();

    @Key("toGlobalEnvironmentText")
    String toGlobalEnvironmentText();

    @Key("toResultObjectText")
    String toResultObjectText();

    @Key("startButtonCaption")
    String startButtonCaption();

    @Key("currentSelectionText")
    String currentSelectionText();

    @Key("selectionText")
    String selectionText(String path);

    @Key("replayJobText")
    String replayJobText();

    @Key("jobListLabel")
    String jobListLabel(int count);

    @Key("terminateRunningJobsCaption")
    String terminateRunningJobsCaption();

    @Key("terminateJobsCaption")
    String terminateJobsCaption();

    @Key("backgroundJobsTabLabel")
    String backgroundJobsTabLabel();

    @Key("viewAllJobsTitle")
    String viewAllJobsTitle();

    @Key("workbenchJobsTitle")
    String workbenchJobsTitle();

}
