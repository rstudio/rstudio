/*
 * EvinceSynctex.cpp
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

#include "EvinceSynctex.hpp"

#include <boost/format.hpp>

#include <core/Log.hpp>
#include <shared_core/Error.hpp>
#include <core/DateTime.hpp>
#include <shared_core/SafeConvert.hpp>

#include <DesktopMainWindow.hpp>
#include <DesktopUtils.hpp>

#include "EvinceDaemon.hpp"
#include "EvinceWindow.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace desktop {
namespace synctex {

namespace {

void logDBusError(const QDBusError& error, const ErrorLocation& location)
{
   boost::format fmt("Error %1% (%2%): %3%");
   std::string msg = boost::str(fmt % error.type() %
                                      error.name().toStdString() %
                                      error.message().toStdString());
   core::log::logErrorMessage(msg, location);
}

} // anonymous namespace

EvinceSynctex::EvinceSynctex(const SynctexViewerInfo& viewerInfo,
                             MainWindow* pMainWindow)
   : Synctex(pMainWindow), viewerInfo_(viewerInfo)
{
   pEvince_ = new EvinceDaemon(this);
}

void EvinceSynctex::syncView(const QString& pdfFile,
                             const QString& srcFile,
                             const QPoint& srcLoc)
{
   syncView(SyncRequest(pdfFile, srcFile, srcLoc));
}

void EvinceSynctex::syncView(const QString& pdfFile, int page)
{
   syncView(SyncRequest(pdfFile, page));
}

void EvinceSynctex::syncView(const SyncRequest& syncRequest)
{
   QString pdfFile = syncRequest.pdfFile;
   if (windows_.contains(pdfFile))
   {
      syncView(windows_.value(pdfFile), syncRequest);
   }
   else
   {
      // find the window
      QDBusPendingReply<QString> reply = pEvince_->FindDocument(
                                       QUrl::fromLocalFile(pdfFile).toString(),
                                       true);

      // wait for the results asynchronously
      QDBusPendingCallWatcher* pWatcher = new QDBusPendingCallWatcher(reply,
                                                                      this);
      pendingSyncRequests_.insert(pWatcher, syncRequest);

      QObject::connect(pWatcher,
                       SIGNAL(finished(QDBusPendingCallWatcher*)),
                       this,
                       SLOT(onFindWindowFinished(QDBusPendingCallWatcher*)));
   }
}

void EvinceSynctex::onFindWindowFinished(QDBusPendingCallWatcher* pWatcher)
{
   // get the reply and the sync request params
   QDBusPendingReply<QString> reply = *pWatcher;
   SyncRequest req = pendingSyncRequests_.value(pWatcher);
   pendingSyncRequests_.remove(pWatcher);

   if (reply.isError())
   {
      logDBusError(reply.error(), ERROR_LOCATION);
   }
   else
   {
      // initialize a connection to it
      EvinceWindow* pWindow = new EvinceWindow(viewerInfo_, reply.value());
      if (!pWindow->isValid())
      {
         logDBusError(pWindow->lastError(), ERROR_LOCATION);
         return;
      }

      // put it in our map
      windows_.insert(req.pdfFile, pWindow);

      // sign up for events
      QObject::connect(pWindow,
                       SIGNAL(Closed()),
                       this,
                       SLOT(onClosed()));
      QObject::connect(pWindow,
                       SIGNAL(SyncSource(const QString&,const QPoint&,uint)),
                       this,
                       SLOT(onSyncSource(const QString&,const QPoint&,uint)));

      // perform sync
      syncView(pWindow, req);
   }

   // delete the watcher
   pWatcher->deleteLater();
}

void EvinceSynctex::syncView(EvinceWindow* pWindow, const SyncRequest& req)
{
   if (req.hasSourceLoc())
   {
      syncView(pWindow, req.srcFile, req.srcLoc);
   }
   else
   {
      QStringList args;
      if (req.hasPage())
      {
         args.append(QString::fromUtf8("-i"));
         args.append(QString::fromStdString(
                           safe_convert::numberToString(req.page)));
      }
      args.append(req.pdfFile);
      QProcess::startDetached(QString::fromUtf8("evince"), args);
   }
}

void EvinceSynctex::syncView(EvinceWindow* pWindow,
                             const QString& srcFile,
                             const QPoint& srcLoc)
{
   QDBusPendingReply<> reply = pWindow->SyncView(
                                       srcFile,
                                       srcLoc,
                                       core::date_time::secondsSinceEpoch());

   // wait for the results asynchronously
   QDBusPendingCallWatcher* pWatcher = new QDBusPendingCallWatcher(reply,
                                                                   this);
   QObject::connect(pWatcher,
                    SIGNAL(finished(QDBusPendingCallWatcher*)),
                    this,
                    SLOT(onSyncViewFinished(QDBusPendingCallWatcher*)));
}

void EvinceSynctex::onSyncViewFinished(QDBusPendingCallWatcher* pWatcher)
{
   QDBusPendingReply<QString> reply = *pWatcher;
   if (reply.isError())
      logDBusError(reply.error(), ERROR_LOCATION);

   pWatcher->deleteLater();
}

void EvinceSynctex::onClosed()
{
   // get the window that closed and determine the associated pdf
   EvinceWindow* pWindow = static_cast<EvinceWindow*>(sender());
   QString pdfFile = windows_.key(pWindow);

   // notify base
   Synctex::onClosed(pdfFile);

   // remove window
   windows_.remove(pdfFile);
   pWindow->deleteLater();
}


void EvinceSynctex::onSyncSource(const QString& srcFile,
                                 const QPoint& srcLoc,
                                 uint)
{
   QUrl fileUrl(srcFile);
   Synctex::onSyncSource(fileUrl.toLocalFile(), srcLoc);
}


} // namesapce synctex
} // namespace desktop
} // namespace rstudio
