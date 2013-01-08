/*
 * DesktopWin32ApplicationLaunch.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <windows.h>

#include <QWidget>

#include "DesktopOptions.hpp"

/*
 This class is implemented using a message-only Win32 window with
 a well-known window title. The message-only window can respond
 to a "get main window handle" message, which will return the HWND
 of the application's main window. It also responds to WM_COPYDATA
 with OPENFILE as dwData, which causes openFileRequest(QString) to
 be signaled with the copied data.

 It also uses a lockfile (%TEMP%\rstudio.lock) to try to ensure two
 copies of the app don't run concurrently even if something goes
 wrong with the message-only window.
 */

namespace desktop {

namespace {

const ULONG_PTR OPENFILE = 1;
const char* WINDOW_TITLE = "RStudio_LaunchWindow_6dd82276-ccc3-4324-839e-4e6bcd5145bd";

UINT wmGetMainWindowHandle()
{
   static UINT msg = 0;
   if (!msg)
      msg = ::RegisterWindowMessage("3dd567f8-7c07-4d9d-934b-dcb922cd737e");
   return msg;
}

void activate(HWND hWnd)
{
   HWND hwndPopup = ::GetLastActivePopup(hWnd);
   if (::IsWindow(hwndPopup))
      hWnd = hwndPopup;
   ::SetForegroundWindow(hWnd);
   if (::IsIconic(hWnd))
      ::ShowWindow(hWnd, SW_RESTORE);
}

} // anonymous namespace

ApplicationLaunch::ApplicationLaunch() :
    QWidget(NULL),
    pMainWindow_(NULL)
{
   setAttribute(Qt::WA_NativeWindow);
   setWindowTitle(QString::fromAscii(WINDOW_TITLE));
   ::SetParent(winId(), HWND_MESSAGE);
}

void ApplicationLaunch::init(QString,
                             int& argc,
                             char* argv[],
                             boost::scoped_ptr<QApplication>* ppApp,
                             boost::scoped_ptr<ApplicationLaunch>* ppAppLaunch)
{
   ppApp->reset(new QApplication(argc, argv));
   ppAppLaunch->reset(new ApplicationLaunch());
}

void ApplicationLaunch::setActivationWindow(QWidget* pWindow)
{
   pMainWindow_ = pWindow;
}

void ApplicationLaunch::activateWindow()
{
   activate(winId());
}

QString ApplicationLaunch::startupOpenFileRequest() const
{
   return QString();
}

namespace {

bool acquireLock()
{
   // The file is implicitly released/deleted when the process exits

   QString lockFilePath = QDir::temp().absoluteFilePath(QString::fromAscii("rstudio.lock"));
   HANDLE hFile = ::CreateFileW(lockFilePath.toStdWString().c_str(),
                                GENERIC_WRITE,
                                0, // exclusive access
                                NULL,
                                OPEN_ALWAYS,
                                FILE_ATTRIBUTE_NORMAL | FILE_FLAG_DELETE_ON_CLOSE,
                                NULL);

   if (hFile == INVALID_HANDLE_VALUE)
   {
      if (::GetLastError() == ERROR_SHARING_VIOLATION)
         return false;
   }

   return true;
}

} // anonymous namespace


void ApplicationLaunch::attemptToRegisterPeer()
{
   acquireLock();
}


bool ApplicationLaunch::sendMessage(QString filename)
{
   if (acquireLock())
      return false;

   HWND hwndAppLaunch = NULL;
   do
   {
      hwndAppLaunch = ::FindWindowEx(HWND_MESSAGE, hwndAppLaunch, NULL, WINDOW_TITLE);
   } while (hwndAppLaunch == winId()); // Ignore ourselves

   if (::IsWindow(hwndAppLaunch))
   {
      HWND hwnd = reinterpret_cast<HWND>(::SendMessage(hwndAppLaunch,
                                                       wmGetMainWindowHandle(),
                                                       NULL,
                                                       NULL));
      if (::IsWindow(hwnd))
      {
         HWND hwndPopup = ::GetLastActivePopup(hwnd);
         if (::IsWindow(hwndPopup))
            hwnd = hwndPopup;
         ::SetForegroundWindow(hwnd);
         if (::IsIconic(hwnd))
            ::ShowWindow(hwnd, SW_RESTORE);

         if (!filename.isEmpty())
         {
            QByteArray data = filename.toUtf8();

            COPYDATASTRUCT copydata;
            copydata.dwData = OPENFILE;
            copydata.lpData = data.data();
            copydata.cbData = data.size();

            HWND sender = winId();

            ::SendMessage(hwndAppLaunch,
                          WM_COPYDATA,
                          reinterpret_cast<WPARAM>(sender),
                          reinterpret_cast<LPARAM>(&copydata));
         }
      }
   }

   return true;
}

bool ApplicationLaunch::winEvent(MSG *message, long *result)
{
   if (message->message == WM_COPYDATA)
   {
      COPYDATASTRUCT* cds = reinterpret_cast<COPYDATASTRUCT*>(message->lParam);
      if (cds->dwData == OPENFILE)
      {
         QString fileName = QString::fromUtf8(
               reinterpret_cast<char*>(cds->lpData),
               cds->cbData);
         openFileRequest(fileName);
         *result = 1;
         return true;
      }
   }
   else if (message->message == wmGetMainWindowHandle())
   {
      if (pMainWindow_)
         *result = reinterpret_cast<LRESULT>(pMainWindow_->winId());
      else
         *result = NULL;
      return true;
   }
   return QWidget::winEvent(message, result);
}

} // namespace desktop
