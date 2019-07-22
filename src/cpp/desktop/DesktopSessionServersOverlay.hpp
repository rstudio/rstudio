/*
 * DesktopSessionServersOverlay.hpp
 *
 * Copyright (C) 2019 by RStudio, Inc.
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

#ifndef DESKTOP_SESSION_SERVERS_OVERLAY_HPP
#define DESKTOP_SESSION_SERVERS_OVERLAY_HPP

#include <boost/optional.hpp>

#include <core/FilePath.hpp>
#include <QObject>

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace desktop {

class DesktopSessionServers;
DesktopSessionServers& sessionServers();

class SessionServerSettings;
SessionServerSettings& sessionServerSettings();

class SessionServer
{
public:
   SessionServer(const std::string& name,
                 const std::string& url,
                 bool isDefault = false) :
       name_(name),
       url_(url),
       isDefault_(isDefault)
   {
   }

   SessionServer(const SessionServer& other) = default;

   const std::string& name() const { return name_; }
   const std::string& url() const { return url_; }
   const std::string& label() const;
   bool isDefault() const { return isDefault_; }

   void setName(const std::string& name) { name_ = name; }
   void setUrl(const std::string& url) { url_ = url; }
   void setIsDefault(bool isDefault) { isDefault_ = isDefault; }

   core::Error test();

   bool operator==(const SessionServer& other) const
   {
      return name_ == other.name_ &&
             url_ == other.url_;
   }

private:
   std::string name_;
   std::string url_;
   bool isDefault_;
};

struct LaunchLocationResult
{
   int dialogResult;
   boost::optional<SessionServer> sessionServer;
};

class DesktopSessionServers : public QObject
{
   Q_OBJECT
public:
   DesktopSessionServers();

   void showSessionServerOptionsDialog();
   LaunchLocationResult showSessionLaunchLocationDialog();

Q_SIGNALS:

public:

private:
};

enum class SessionLocation
{
   Ask,
   Locally,
   Server
};

enum class CloseServerSessions
{
   Ask,
   Always,
   Never
};

enum class ConfigSource
{
   Admin,
   User
};

class SessionServerSettings
{
public:
   const std::vector<SessionServer>& servers() const { return servers_; }
   SessionLocation sessionLocation() const { return sessionLocation_; }
   CloseServerSessions closeServerSessionsOnExit() const { return closeServerSessionsOnExit_; }

   ConfigSource configSource() const;

   void save(const std::vector<SessionServer>& servers,
             SessionLocation sessionLocation,
             CloseServerSessions closeServerSessionsOnExit);

private:
   friend SessionServerSettings& sessionServerSettings();
   SessionServerSettings();

   std::vector<SessionServer> servers_;
   SessionLocation sessionLocation_;
   CloseServerSessions closeServerSessionsOnExit_;
   core::FilePath optionsFile_;
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_SESSION_SERVERS_OVERLAY_HPP
