/*
 * DesktopPosixApplicationLaunch.cpp
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

#include "DesktopApplicationLaunch.hpp"

#include "DesktopPosixApplication.hpp"


namespace desktop {

namespace {


PosixApplication* app()
{
   return qobject_cast<PosixApplication*>(qApp);
}

} // anonymous namespace

ApplicationLaunch::ApplicationLaunch() :
    QWidget(NULL),
    pMainWindow_(NULL)
{
   connect(app(), SIGNAL(messageReceived(QString)),
           this, SIGNAL(openFileRequest(QString)));
}

void ApplicationLaunch::init(QString appName,
                             int& argc,
                             char* argv[],
                             boost::scoped_ptr<QApplication>* ppApp,
                             boost::scoped_ptr<ApplicationLaunch>* ppAppLaunch)
{
   // Immediately stuffed into scoped_ptr
   PosixApplication* pSingleApplication = new PosixApplication(appName,
                                                               argc,
                                                               argv);
   pSingleApplication->setApplicationName(appName);
   ppApp->reset(pSingleApplication);

   ppAppLaunch->reset(new ApplicationLaunch());

   // connect app open file signal to app launch
   connect(app(), SIGNAL(openFileRequest(QString)),
           ppAppLaunch->get(), SIGNAL(openFileRequest(QString)));
}

void ApplicationLaunch::setActivationWindow(QWidget* pWindow)
{
   pMainWindow_ = pWindow;
   app()->setActivationWindow(pWindow, true);
}


void ApplicationLaunch::activateWindow()
{
   app()->activateWindow();
}

void ApplicationLaunch::attemptToRegisterPeer()
{
   // side-effect of is running is to try to register ourselves as a peer
   app()->isRunning();
}


bool ApplicationLaunch::sendMessage(QString filename)
{
   return app()->sendMessage(filename);
}

QString ApplicationLaunch::startupOpenFileRequest() const
{
   return app()->startupOpenFileRequest();
}

} // namespace desktop
