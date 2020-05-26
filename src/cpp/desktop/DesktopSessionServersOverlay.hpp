/*
 * DesktopSessionServersOverlay.hpp
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

#ifndef DESKTOP_SESSION_SERVERS_OVERLAY_HPP
#define DESKTOP_SESSION_SERVERS_OVERLAY_HPP

#include <QJsonObject>
#include <QJsonValue>
#include <QNetworkCookie>
#include <QObject>

#include <boost/optional.hpp>
#include <boost/signals2.hpp>

#include <shared_core/FilePath.hpp>

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

class SessionServerPathMapping
{
public:
   SessionServerPathMapping(const std::string& localPath,
                            const std::string& remotePath) :
      localPath_(localPath),
      remotePath_(remotePath)
   {
   }

   bool empty() { return localPath_.empty() || remotePath_.empty(); }

   const std::string& localPath() const { return localPath_; }
   const std::string& remotePath() const { return remotePath_; }

   void setLocalPath(const std::string& localPath) { localPath_ = localPath; }
   void setRemotePath(const std::string& remotePath) { remotePath_ = remotePath; }

   QJsonObject toJson() const;
   static SessionServerPathMapping fromJson(const QJsonObject& pathMappingJson);

private:
   SessionServerPathMapping() {}

   std::string localPath_;
   std::string remotePath_;
};

class SessionServer
{
public:
   SessionServer(const std::string& name,
                 const std::string& url,
                 bool isDefault = false,
                 bool allowPathMapping = false,
                 const std::vector<SessionServerPathMapping>& pathMappings = {}) :
       name_(name),
       url_(url),
       isDefault_(isDefault),
       allowPathMapping_(allowPathMapping),
       pathMappings_(pathMappings)
   {
   }

   SessionServer(const SessionServer& other) = default;

   const std::string& name() const { return name_; }
   const std::string& url() const { return url_; }
   const std::string& label() const;
   bool isDefault() const { return isDefault_; }
   bool allowPathMapping() const { return allowPathMapping_; }
   std::vector<SessionServerPathMapping> pathMappings() const { return pathMappings_; }

   QJsonObject toJson() const;
   static SessionServer fromJson(const QJsonObject& sessionServerJson);

   bool cookieBelongs(const QNetworkCookie& cookie) const;

   void setName(const std::string& name) { name_ = name; }
   void setUrl(const std::string& url) { url_ = url; }
   void setIsDefault(bool isDefault) { isDefault_ = isDefault; }
   void setAllowPathMapping(bool allow) { allowPathMapping_ = allow; }
   void setPathMappings(const std::vector<SessionServerPathMapping>& mappings) { pathMappings_ = mappings; }

   core::Error test();

   bool operator==(const SessionServer& other) const
   {
      return name_ == other.name_ &&
             url_ == other.url_;
   }

private:
   SessionServer() :
      isDefault_(false) {}

   std::string name_;
   std::string url_;
   bool isDefault_;
   bool allowPathMapping_;

   std::vector<SessionServerPathMapping> pathMappings_;
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

   void showSessionServerOptionsDialog(QWidget* parent = nullptr);
   LaunchLocationResult showSessionLaunchLocationDialog();

   void setPendingSessionServerReconnect(const SessionServer& server);
   boost::optional<SessionServer> getPendingSessionServerReconnect();

Q_SIGNALS:

public:

private:
   boost::optional<SessionServer> pendingSessionServerReconnect_;
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

   boost::signals2::scoped_connection addSaveHandler(const boost::function<void(void)>& onSave);

private:
   friend SessionServerSettings& sessionServerSettings();
   SessionServerSettings();

   std::vector<SessionServer> servers_;
   SessionLocation sessionLocation_;
   CloseServerSessions closeServerSessionsOnExit_;
   core::FilePath optionsFile_;

   boost::signals2::signal<void()> onSaveSignal_;
};

} // namespace desktop
} // namespace rstudio

#endif // DESKTOP_SESSION_SERVERS_OVERLAY_HPP
