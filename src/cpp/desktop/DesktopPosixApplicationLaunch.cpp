/*
 * DesktopPosixApplicationLaunch.cpp
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

#include "DesktopApplicationLaunch.hpp"
#include "DesktopPosixApplication.hpp"
#include "DesktopOptions.hpp"

#include <core/system/Environment.hpp>
#include <core/r_util/RUserData.hpp>

#include <QKeyEvent>
#include <QMouseEvent>

namespace rstudio {
namespace desktop {

namespace {

PosixApplication* app()
{
   return qobject_cast<PosixApplication*>(qApp);
}

// helper for swapping Ctrl, Meta modifiers
// https://bugreports.qt.io/browse/QTBUG-51293
class MacEventFilter : public QObject
{
public:
   explicit MacEventFilter(QObject* parent)
      : QObject(parent)
   {
   }
   
protected:
   bool eventFilter(QObject* object, QEvent* event) override
   {
      // Fix issue with malformed mouse events that can be emitted when the
      // application is focused through either a very fast mouse click, or a
      // trackpad click. in such cases, the 'primary' mouse button is
      // registered in event->button(), but event->buttons() is Qt::NoButton.
      // when this occurs, Qt fails to delegate the mouse event to the
      // sub-widget hosting the web page, causing the mouse event to
      // effectively be dropped. this leads to a mousedown event without a
      // companion mouseup event, effectively leaving the event handler (often
      // Ace) stuck in a pseudo-drag state, which causes all sorts of funky
      // behavior. see:
      //
      //    https://github.com/rstudio/rstudio/issues/5107
      //    https://bugreports.qt.io/browse/QTBUG-77125
      //
      // for some more details.
      if (event->type() == QEvent::MouseButtonPress ||
          event->type() == QEvent::MouseButtonRelease)
      {
         QMouseEvent* mouseEvent = static_cast<QMouseEvent*>(event);
         if (mouseEvent->button() != Qt::NoButton &&
             mouseEvent->buttons() == Qt::NoButton)
         {
            QMouseEvent* fixedMouseEvent = new QMouseEvent(
                     mouseEvent->type(),
                     mouseEvent->localPos(),
                     mouseEvent->windowPos(),
                     mouseEvent->screenPos(),
                     mouseEvent->button(),
                     mouseEvent->button(),
                     mouseEvent->modifiers());
            QCoreApplication::postEvent(object, fixedMouseEvent);
            return true;
         }
      }
      
      if (event->type() == QEvent::KeyPress)
      {
         auto* keyEvent = static_cast<QKeyEvent*>(event);
         auto modifiers = keyEvent->modifiers();
         
         // Through a series of unfortunate events, Qt fails to translate
         // Ctrl keypresses to Meta, and vice versa. pressing Ctrl, therefore,
         // sends a key event where the 'ctrlKey' property is true, but the
         // associated keyCode is 91, and the key pressed is 'Meta'. Ace fails
         // to discover a printable key with keycode 91, so tries calling
         // String.fromCharCode() to figure out what character should be used
         // instead; this key happens to be '['. This leads to Ace receiving the
         // equivalent of a Ctrl+[ keypress, which Ace then interprets as an
         // outdent request (with the default keybindings).
         //
         // We'll just swallow bare modifier keypresses since we don't actually
         // use these for anything (they're only ever handled in conjunction
         // with other keypresses; ie, as modifiers)
         if (keyEvent->key() == Qt::Key_Meta &&
             modifiers == Qt::META)
         {
            return true;
         }
         else if (keyEvent->key() == Qt::Key_Control &&
                  modifiers == Qt::CTRL)
         {
            return true;
         }
         
         // convert Backtab into Shift+Tab -- this is necessary for focus
         // switching as the default browser behavior doesn't understand
         // the Qt 'Backtab' key event
         if (keyEvent->key() == Qt::Key_Backtab)
         {
            auto* event = new QKeyEvent(
                     QEvent::KeyPress,
                     Qt::Key_Tab,
                     modifiers | Qt::ShiftModifier);
            QCoreApplication::postEvent(object, event);
            return true;
         }
         
      }
      
      return QObject::eventFilter(object, event);
   }
};

} // anonymous namespace

ApplicationLaunch::ApplicationLaunch() :
    QWidget(nullptr),
    pMainWindow_(nullptr)
{
   connect(app(), SIGNAL(messageReceived(QString)),
           this, SIGNAL(openFileRequest(QString)));

   launchEnv_ = QProcessEnvironment::systemEnvironment();
}

void ApplicationLaunch::init(QString appName,
                             int& argc,
                             char* argv[],
                             boost::scoped_ptr<QApplication>* ppApp,
                             boost::scoped_ptr<ApplicationLaunch>* ppAppLaunch)
{
   // Immediately stuffed into scoped_ptr
   auto* pSingleApplication = new PosixApplication(appName, argc, argv);

   // create app launch instance
   PosixApplication::setApplicationName(appName);
   ppApp->reset(pSingleApplication);

   ppAppLaunch->reset(new ApplicationLaunch());
   pSingleApplication->setAppLauncher(ppAppLaunch->get());

   // connect app open file signal to app launch
   connect(app(), SIGNAL(openFileRequest(QString)),
           ppAppLaunch->get(), SIGNAL(openFileRequest(QString)));
   
#ifdef Q_OS_MAC
   pSingleApplication->installEventFilter(new MacEventFilter(pSingleApplication));
#endif
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

void ApplicationLaunch::launchRStudio(const std::vector<std::string>& args,
                                      const std::string& initialDir)
{
   QStringList argList;
   for (const std::string& arg : args)
   {
      argList.append(QString::fromStdString(arg));
   }

   QString exePath = QString::fromUtf8(
      desktop::options().executablePath().getAbsolutePath().c_str());

   // temporarily restore the library path to the one we were launched with
   std::string ldPath = core::system::getenv("LD_LIBRARY_PATH");
   core::system::setenv("LD_LIBRARY_PATH",
      launchEnv_.value(QString::fromUtf8("LD_LIBRARY_PATH")).toStdString());

   // set environment variable indicating initial launch dir
   core::system::setenv(kRStudioInitialWorkingDir, initialDir);

   // start RStudio detached
   QProcess::startDetached(exePath, argList);

   // restore environment
   core::system::setenv("LD_LIBRARY_PATH", ldPath);
   core::system::unsetenv(kRStudioInitialWorkingDir);
}

} // namespace desktop
} // namespace rstudio
