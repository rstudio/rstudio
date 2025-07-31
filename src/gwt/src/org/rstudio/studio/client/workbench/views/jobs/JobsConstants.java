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

    @DefaultMessage("Background Jobs Still Running")
    @Key("backgroundJobsRunningCaption")
    String backgroundJobsRunningCaption();

    @DefaultMessage("The Background Jobs tab cannot be closed while there {0}\\n\\nWait until all background jobs have completed.")
    @Key("backgroundJobsRunningMessage")
    String backgroundJobsRunningMessage(String localJobMessage);

    @DefaultMessage("are unfinished background jobs.")
    @Key("backgroundJobsUnfinished")
    String backgroundJobsUnfinished();

    @DefaultMessage("is an unfinished background job.")
    @Key("backgroundJobUnfinished")
    String backgroundJobUnfinished();

    @DefaultMessage("Background Jobs")
    @Key("backgroundJobsTitle")
    String backgroundJobsTitle();

    @DefaultMessage("Cannot retrieve job output")
    @Key("cannotRetrieveJobOutputCaption")
    String cannotRetrieveJobOutputCaption();

    @DefaultMessage("Running")
    @Key("runningState")
    String runningState();

    @DefaultMessage("Idle")
    @Key("idleState")
    String idleState();

    @DefaultMessage("Cancelled")
    @Key("cancelledState")
    String cancelledState();

    @DefaultMessage("Failed")
    @Key("failedState")
    String failedState();

    @DefaultMessage("Succeeded")
    @Key("succeededState")
    String succeededState();

    @DefaultMessage("Unknown {0}")
    @Key("unknownState")
    String unknownState(int state);

    @DefaultMessage("Remove Completed Background Jobs")
    @Key("removeCompletedBackgroundJobsCaption")
    String removeCompletedBackgroundJobsCaption();

    @DefaultMessage("Are you sure you want to remove completed background jobs from the list of jobs?\\n\\nOnce removed, background jobs cannot be recovered.")
    @Key("removeCompletedBackgroundJobsMessage")
    String removeCompletedBackgroundJobsMessage();

    @DefaultMessage("Remove jobs")
    @Key("removeJobsLabel")
    String removeJobsLabel();

    @DefaultMessage("Cancel")
    @Key("cancelLabel")
    String cancelLabel();

    @DefaultMessage("{0} jobs")
    @Key("numJobsLabel")
    String numJobsLabel(int jobs);

    @DefaultMessage("Run Selection as Background Job")
    @Key("runSelectionAsBackgroundJobCaption")
    String runSelectionAsBackgroundJobCaption();

    @DefaultMessage("Run Script as Background Job")
    @Key("runScriptAsBackgroundJobCaption")
    String runScriptAsBackgroundJobCaption();

    @DefaultMessage("Stop background job")
    @Key("stopJobTitle")
    String stopJobTitle();

    @DefaultMessage("Select Background Job")
    @Key("selectJobText")
    String selectJobText();

    @DefaultMessage("Waiting")
    @Key("waitingText")
    String waitingText();

    @DefaultMessage("R Script")
    @Key("rScriptLabel")
    String rScriptLabel();

    @DefaultMessage("Working Directory")
    @Key("workingDirectoryCaption")
    String workingDirectoryCaption();

    @DefaultMessage("(Don''t copy)")
    @Key("dontCopyText")
    String dontCopyText();

    @DefaultMessage("To global environment")
    @Key("toGlobalEnvironmentText")
    String toGlobalEnvironmentText();

    @DefaultMessage("To results object in global environment")
    @Key("toResultObjectText")
    String toResultObjectText();

    @DefaultMessage("Start")
    @Key("startButtonCaption")
    String startButtonCaption();

    @DefaultMessage("Current selection")
    @Key("currentSelectionText")
    String currentSelectionText();

    @DefaultMessage("{0} selection")
    @Key("selectionText")
    String selectionText(String path);

    @DefaultMessage("Replay job")
    @Key("replayJobText")
    String replayJobText();

    @DefaultMessage("The following {0} jobs are still running.")
    @Key("jobListLabel")
    String jobListLabel(int count);

    @DefaultMessage("Terminate Running Jobs")
    @Key("terminateRunningJobsCaption")
    String terminateRunningJobsCaption();

    @DefaultMessage("Terminate Jobs")
    @Key("terminateJobsCaption")
    String terminateJobsCaption();

    @DefaultMessage("Background Jobs Tab")
    @Key("backgroundJobsTabLabel")
    String backgroundJobsTabLabel();

    @DefaultMessage("View all background jobs")
    @Key("viewAllJobsTitle")
    String viewAllJobsTitle();

    @DefaultMessage("Workbench Jobs")
    @Key("workbenchJobsTitle")
    String workbenchJobsTitle();

}
