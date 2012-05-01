/*
 * EvinceSynctex.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "EvinceSynctex.hpp"

#include <boost/format.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/DateTime.hpp>

#include <DesktopMainWindow.hpp>

#include "EvinceDaemon.hpp"
#include "EvinceWindow.hpp"

using namespace core;

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

EvinceSynctex::EvinceSynctex(MainWindow* pMainWindow)
   : QObject(pMainWindow),
     pMainWindow_(pMainWindow)
{
   pEvince_ = new EvinceDaemon(this);
}

void EvinceSynctex::syncView(const QString& pdfFile,
                             const QString& srcFile,
                             const QPoint& srcLoc)
{
   if (windows_.contains(pdfFile))
   {
      syncView(windows_.value(pdfFile), srcFile, srcLoc);
   }
   else
   {
      // find the window
      QDBusPendingReply<QString> reply = pEvince_->FindDocument(
                                       QUrl::fromLocalFile(pdfFile).toString(),
                                       true);
      reply.waitForFinished();
      if (reply.isError())
      {
         logDBusError(reply.error(), ERROR_LOCATION);
         return;
      }

      // initialize a connection to it
      EvinceWindow* pWindow = new EvinceWindow(reply.value());
      if (!pWindow->isValid())
      {
         logDBusError(pWindow->lastError(), ERROR_LOCATION);
         return;
      }

      // put it in our map
      windows_.insert(pdfFile, pWindow);

      // TODO: sign up for DocLoaded and Closed and remove from
      // our map when that happens...
      //
      // OR, could we not cache it and just let the connection
      // leak -- or do DBus objects manage their own lifetime?

      // sign up for events
      /*
      QObject::connect(pEvince,
                       SIGNAL(SyncSource(const QString&,const QPoint&,uint)),
                       pMainWindow_,
                       SLOT(onSyncSource(const QString&,const QPoint&,uint)));
      */

      // perform sync
      syncView(pWindow, srcFile, srcLoc);
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

   reply.waitForFinished();

   if (reply.isError())
      logDBusError(reply.error(), ERROR_LOCATION);
}


} // namesapce synctex
} // namespace desktop
