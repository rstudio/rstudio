package org.rstudio.studio.client.application.model;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.SessionInfo;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class DummyApplicationServerOperations implements ApplicationServerOperations
{

    @Override
    public void setUserPrefs(JavaScriptObject userPrefs, ServerRequestCallback<Void> requestCallback) {}

    @Override
    public void setUserState(JavaScriptObject userState, ServerRequestCallback<Void> requestCallback) {}

    @Override
    public void editPreferences(ServerRequestCallback<Void> requestCallback) {}

    @Override
    public void clearPreferences(ServerRequestCallback<String> requestCallback) {}

    @Override
    public void viewPreferences(ServerRequestCallback<Void> requestCallback) {}

    @Override
    public void clientInit(String baseURL, SessionInitOptions options,
            ServerRequestCallback<SessionInfo> requestCallback) {}

    @Override
    public void getJobConnectionStatus(ServerRequestCallback<String> requestCallback) {}

    @Override
    public void interrupt(ServerRequestCallback<Boolean> requestCallback) {}

    @Override
    public void abort(String nextSessionProject, ServerRequestCallback<Void> requestCallback) {}

    @Override
    public void suspendSession(boolean force, ServerRequestCallback<Void> requestCallback) {}

    @Override
    public void handleUnsavedChangesCompleted(boolean handled, ServerRequestCallback<Void> requestCallback) {}

    @Override
    public void quitSession(boolean saveWorkspace, String switchToProjectPath, RVersionSpec switchToRVersion,
            String hostPageUrl, ServerRequestCallback<Boolean> requestCallback) {}

    @Override
    public void updateCredentials() {}

    @Override
    public void stopEventListener() {}

    @Override
    public void ensureEventListener() {}

    @Override
    public String getApplicationURL(String pathName) {
        return null;
    }

    @Override
    public String getFileUrl(FileSystemItem file) {
        return null;
    }

    @Override
    public void suspendForRestart(SuspendOptions options, ServerRequestCallback<Void> requestCallback) {}

    @Override
    public void ping(ServerRequestCallback<Void> requestCallback) {}

    @Override
    public void checkForUpdates(boolean manual, ServerRequestCallback<UpdateCheckResult> requestCallback) {}

    @Override
    public void getProductInfo(ServerRequestCallback<ProductInfo> requestCallback) {}

    @Override
    public void getProductNotice(ServerRequestCallback<ProductNotice> requestCallback) {}

    @Override
    public void getNewSessionUrl(String hostPageUrl, boolean isProject, String directory, RVersionSpec rVersion,
            JavaScriptObject launchSpec, ServerRequestCallback<String> callback) {}

    @Override
    public void getActiveSessions(String hostPageUrl, ServerRequestCallback<JsArray<ActiveSession>> callback) {}

    @Override
    public void getAvailableRVersions(ServerRequestCallback<JsArray<RVersionSpec>> callback) {}

    @Override
    public void getProjectRVersion(String projectDir, ServerRequestCallback<RVersionSpec> callback) {}

    @Override
    public void getProjectFilePath(String projectId, ServerRequestCallback<String> callback) {}

    @Override
    public void setSessionLabel(String label, ServerRequestCallback<Void> requestCallback) {}

    @Override
    public void deleteSessionDir(String sessionId, ServerRequestCallback<Void> requestCallback) {}

    @Override
    public void findProjectInFolder(String folder, ServerRequestCallback<String> requestCallback) {}

    @Override
    public void getRVersion(ServerRequestCallback<RVersionSpec> requestCallback) {  }


}
