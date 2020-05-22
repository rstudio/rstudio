/*
 * DesktopJobLauncherOverlay.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#ifndef DESKTOP_JOB_LAUNCHER_OVERLAY_HPP
#define DESKTOP_JOB_LAUNCHER_OVERLAY_HPP

#include <string>

#include <QNetworkCookie>

#include <core/http/AsyncClient.hpp>
#include <shared_core/json/Json.hpp>

#include "DesktopMainWindow.hpp"
#include "DesktopSessionServersOverlay.hpp"

namespace rstudio {
namespace desktop {

class JobLauncherImpl;

class JobLauncher
{
public:
    explicit JobLauncher(MainWindow* pMainWindow);

    core::Error initialize();

    void startLauncherJobStatusStream(const std::string& jobId);

    void stopLauncherJobStatusStream(const std::string& jobId);

    void startLauncherJobOutputStream(const std::string& jobId);

    void stopLauncherJobOutputStream(const std::string& jobId);

    void controlLauncherJob(const std::string& jobId,
                            const std::string& operation);

    void submitLauncherJob(const core::json::Object& jobObj);

    void validateJobsConfig();

    void getJobContainerUser();

    bool setSessionServer(const SessionServer& sessionServer);

    void signIn();

    int getProxyPortNumber();

    SessionServer getLauncherServer();

    std::map<std::string, QNetworkCookie> getCookies();

private:
    boost::shared_ptr<JobLauncherImpl> pImpl_;
};


} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_JOB_LAUNCHER_OVERLAY_HPP

