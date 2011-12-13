/*
 * ConsoleIOMain.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
#include <iostream>
#include <string>
#include <vector>

#include <stdio.h>
#include <windows.h>

#define BOOST_THREAD_USE_LIB
#include <core/BoostThread.hpp>
#include <core/Error.hpp>

using namespace core;

/*
 * ConsoleIO is an Win32 program that allows a console program that uses
 * low-level console input (_getch()) to be fed via stdin redirection.
 *
 * The program that needs to be executed and its arguments, should be
 * passed as arguments to consoleio.
 */

void print_usage()
{
   std::cerr << "Usage: consoleio.exe <program> [program-arguments]"
             << std::endl;
}

inline void print_error(const std::string& label = std::string())
{
   DWORD err = ::GetLastError();
   if (label.empty())
      std::cerr << "Error " << err << std::endl;
   else
      std::cerr << "Error calling " << label << ": " << err << std::endl;
}

std::string removeCommandFromCommandLine(const std::string& cmd,
                                         const std::string& cmdLine)
{
   size_t pos = cmdLine.find(cmd);
   if (pos != 0 && pos != 1)
      return "";

   for ( ; cmdLine[pos] != ' ' && pos < cmdLine.size(); pos++)
   {
   }

   for ( ; cmdLine[pos] == ' ' && pos < cmdLine.size(); pos++)
   {
   }

   return cmdLine.substr(pos);
}

bool send_console_input_char(HANDLE hConsoleIn, char c)
{
   WORD keyCode = 0;
   char keyChar = c;

   switch (c)
   {
   case '\r':
   case '\n':
      keyCode = VK_RETURN;
      break;
   }

   INPUT_RECORD inputRecords[2];
   ZeroMemory(inputRecords, sizeof(inputRecords));

   inputRecords[0].EventType = KEY_EVENT;
   inputRecords[0].Event.KeyEvent.bKeyDown = TRUE;
   inputRecords[0].Event.KeyEvent.wRepeatCount = 1;
   inputRecords[0].Event.KeyEvent.wVirtualKeyCode = keyCode;
   inputRecords[0].Event.KeyEvent.wVirtualScanCode = 0;
   inputRecords[0].Event.KeyEvent.uChar.AsciiChar = keyChar;

   inputRecords[1].EventType = KEY_EVENT;
   inputRecords[1].Event.KeyEvent.bKeyDown = FALSE;
   inputRecords[1].Event.KeyEvent.wRepeatCount = 1;
   inputRecords[1].Event.KeyEvent.wVirtualKeyCode = keyCode;
   inputRecords[1].Event.KeyEvent.wVirtualScanCode = 0;
   inputRecords[1].Event.KeyEvent.uChar.AsciiChar = keyChar;

   DWORD written;
   return ::WriteConsoleInput(hConsoleIn,
                              inputRecords,
                              2,
                              &written);
}

template <class InputIterator>
bool send_console_input(HANDLE hConsoleIn,
                        InputIterator begin,
                        InputIterator end)
{
   for (; begin != end; begin++)
      if (!send_console_input_char(hConsoleIn, *begin))
         return false;
   return true;
}

void transferStdInToConsole(HANDLE hConIn)
{
   HANDLE hStdIn = ::GetStdHandle(STD_INPUT_HANDLE);
   std::vector<char> buf(1024);
   DWORD bytesRead;

   while (true)
   {
      if (!::ReadFile(hStdIn, &(buf[0]), buf.size(), &bytesRead, NULL))
         break;

      send_console_input(hConIn, buf.begin(), buf.begin() + bytesRead);
   }
}

int main(int argc, char** argv)
{
   if (argc < 2)
   {
      std::cerr << "Error: Not enough arguments" << std::endl;
      print_usage();
      return 1;
   }

   std::string cmd = removeCommandFromCommandLine(argv[0],
                                                  ::GetCommandLine());
   std::vector<char> cmdBuf;

   // Use cmd.exe to allow shell commands like "dir" to work properly
   std::string cmdExeC = "cmd.exe /c ";
   cmdBuf.assign(cmdExeC.begin(), cmdExeC.end());

   std::copy(cmd.begin(), cmd.end(), std::back_inserter(cmdBuf));
   cmdBuf.assign(cmd.begin(), cmd.end());
   cmdBuf.push_back('\0');

   SECURITY_ATTRIBUTES sa = { sizeof(SECURITY_ATTRIBUTES) };
   sa.bInheritHandle = true;

   HANDLE hConIn = ::CreateFile("CONIN$",
                                GENERIC_READ|GENERIC_WRITE,
                                FILE_SHARE_READ|FILE_SHARE_WRITE,
                                &sa,
                                OPEN_EXISTING,
                                0,
                                NULL);
   if (hConIn == INVALID_HANDLE_VALUE)
   {
      print_error("CreateFile");
      return 1;
   }

   HANDLE hConOut = ::CreateFile("CONOUT$",
                                 GENERIC_READ|GENERIC_WRITE,
                                 FILE_SHARE_READ|FILE_SHARE_WRITE,
                                 NULL,
                                 OPEN_EXISTING,
                                 0,
                                 NULL);
   if (hConOut == INVALID_HANDLE_VALUE)
   {
      print_error("CreateFile");
      return 1;
   }

   COORD newSize = {80, 160};
   if (!::SetConsoleScreenBufferSize(hConOut, newSize))
   {
      print_error("SetConsoleScreenBufferSize");
      return 1;
   }

   STARTUPINFO si = {0};
   si.cb = sizeof(STARTUPINFO);
   si.dwFlags = STARTF_USESHOWWINDOW | STARTF_USESTDHANDLES;
   si.wShowWindow = SW_HIDE;
   si.hStdInput = hConIn;
   si.hStdOutput = ::GetStdHandle(STD_OUTPUT_HANDLE);
   si.hStdError = ::GetStdHandle(STD_ERROR_HANDLE);


   PROCESS_INFORMATION pi = {0};

   if (!::CreateProcess(NULL,
                        &(cmdBuf[0]),
                        NULL,
                        NULL,
                        FALSE,
                        0,
                        NULL,
                        NULL,
                        &si,
                        &pi))
   {
      print_error("CreateProcess");
      return 1;
   }

   boost::thread(&transferStdInToConsole, hConIn);

   while (true)
   {
      DWORD waitResult = ::WaitForSingleObject(pi.hProcess,
                                               INFINITE);
      if (waitResult == WAIT_OBJECT_0)
      {
         // Process has exited
         DWORD exitCode;
         if (::GetExitCodeProcess(pi.hProcess, &exitCode))
         {
            return exitCode;
         }
         else
         {
            print_error("GetExitCodeProcess");
            return 1;
         }
      }
      else if (waitResult == WAIT_FAILED)
      {
         print_error("WaitForMultipleObjects");
         return 1;
      }
      else
      {
         std::cerr << "Unexpected result from WaitForMultipleObjects: "
                   << waitResult
                   << std::endl;
         return 1;
      }
   }
}
