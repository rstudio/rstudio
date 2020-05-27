/*
 * ConsoleIOMain.cpp
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
#include <iostream>
#include <string>
#include <vector>
#include <gsl/gsl>

#include <stdio.h>
#include <windows.h>

#include <boost/algorithm/string.hpp>

#define BOOST_THREAD_USE_LIB
#include <core/BoostThread.hpp>
#include <shared_core/Error.hpp>

using namespace rstudio;
using namespace rstudio::core;

HANDLE hSnapshotOutput;
// Use this event to ensure that the transferConsoleOutToStdErr thread gets a
// chance to dump the console one last time before exiting
HANDLE hReadyForExitEvent;

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
   pos += cmd.size();

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
      keyCode = VK_RETURN;
      break;
   case '\n':
      // Skip newlines because Enters come in as \r\n, even though we need to
      // send them to the console as \r lest we get two returns.
      // Note that this introduces a bug if Enter is ever sent as \n, so,
      // don't do that.
      return true;
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

// Grab one row of console output, with up to the specified number of columns
// (or exactly that number of columns if padToColumnWidth is set). The output
// will be appended to pOutput.
BOOL capture_console_output_row(HANDLE hConsoleOut,
                                SHORT row,
                                SHORT columns,
                                bool padToColumnWidth,
                                std::string* pOutput)
{
   static std::vector<CHAR_INFO> buffer;

   if (columns == 0)
      return true;

   COORD targetSize = {columns, 1};
   buffer.resize(targetSize.X * targetSize.Y);

   COORD from = {};

   // left, top, right, bottom
   SMALL_RECT rect = {0, row, columns-1, row};

   if (!::ReadConsoleOutput(hConsoleOut,
                            &(buffer[0]),
                            targetSize,
                            from,
                            &rect))
   {
      return false;
   }

   for (SHORT col = 0; col <= rect.Right; col++)
   {
      CHAR c = buffer[col].Char.AsciiChar;
      pOutput->push_back(c);
   }
   if (padToColumnWidth)
   {
      SHORT extraPadding = columns - rect.Right - 1;
      for (SHORT i = 0; i < extraPadding; i++)
         pOutput->push_back(' ');
   }
   return true;
}

BOOL capture_console_output(HANDLE hConsoleOut, std::string* pOutput)
{
   static std::vector<CHAR_INFO> buffer;

   CONSOLE_SCREEN_BUFFER_INFO csbInfo;
   if (!::GetConsoleScreenBufferInfo(hConsoleOut, &csbInfo))
      return false;

   COORD& cursor = csbInfo.dwCursorPosition;
   COORD& consoleSize = csbInfo.dwSize;

   if (cursor.X == 0 && cursor.Y == 0)
      return true;

   for (SHORT row = 0; row < cursor.Y; row++)
   {
      if (!capture_console_output_row(hConsoleOut, row, consoleSize.Y,
                                      false, pOutput))
      {
         return false;
      }

      // Remove trailing spaces
      std::string::iterator trimPos = pOutput->end();
      while (trimPos != pOutput->begin())
      {
         if (*(trimPos-1) != ' ')
            break;
         trimPos--;
      }
      pOutput->erase(trimPos, pOutput->end());

      pOutput->push_back('\r');
      pOutput->push_back('\n');
   }

   // Now grab the line that contains the cursor
   if (!capture_console_output_row(hConsoleOut, cursor.Y, cursor.X, true, pOutput))
      return false;

   return true;
}

// Dump the entire console buffer (up to the cursor) of hConsole
// and write it to hOutput
BOOL write_to_handle(const std::string& output, HANDLE hOutput)
{
   const CHAR* pData = output.c_str();
   DWORD bytesToWrite = gsl::narrow_cast<DWORD>(output.size());
   while (bytesToWrite > 0)
   {
      DWORD bytesWritten;
      if (!::WriteFile(hOutput,
                       pData,
                       bytesToWrite,
                       &bytesWritten,
                       nullptr))
      {
         return false;
      }
      bytesToWrite -= bytesWritten;
      pData += bytesWritten;
   }
   return true;
}

bool isNotSpace(char c)
{
   return c != ' ';
}

std::string calcDifference(const std::string& current,
                           const std::string& prev)
{
   std::string::const_iterator itCur = current.begin(), itPrev = prev.begin();
   for (;
        itCur != current.end() && itPrev != prev.end() && *itCur == *itPrev;
        itCur++, itPrev++)
   {
   }

   if (itPrev == prev.end())
   {
      // Entire prefix matched--good!
      return std::string(itCur, current.end());
   }

   if (std::find_if(itPrev, prev.end(), isNotSpace) != prev.end())
   {
      // Significant (non-space) part of prev was not found in current.
      // Send \f which causes the screen to clear, then the entire current
      // snapshot.
      return "\f" + current;
   }

   // If we got here, current starts with prev except for the end of prev which
   // consists only of spaces.

   if (itCur != current.end() && (*itCur == '\r' || *itCur == '\n'))
   {
      // We'll accept newline as a substitute for those spaces, this is common
      // due to trimming which occurs on all lines but the last one when
      // capturing console output.
      return std::string(itCur, current.end());
   }

   return "\f" + current;
}

void transferStdInToConsole(HANDLE hConIn)
{
   HANDLE hStdIn = ::GetStdHandle(STD_INPUT_HANDLE);
   std::vector<char> buf(1024);
   DWORD bytesRead;

   while (true)
   {
      if (!::ReadFile(hStdIn, &(buf[0]), gsl::narrow_cast<DWORD>(buf.size()), &bytesRead, nullptr))
         break;

      send_console_input(hConIn, buf.begin(), buf.begin() + bytesRead);
   }
}

void transferConsoleOutToStdErr(HANDLE hConOut)
{
   std::string lastKnownConsoleContents;
   std::string output;
   while (true)
   {
      if (!::SetEvent(hReadyForExitEvent))
      {
         print_error("SetEvent");
      }

      ::Sleep(500);

      output.clear();
      if (!capture_console_output(hConOut, &output))
      {
         print_error("capture_console_output");
         continue;
      }

      std::string valueToWrite = calcDifference(output,
                                                lastKnownConsoleContents);

      if (valueToWrite.empty())
         continue;

      lastKnownConsoleContents = output;

      if (!write_to_handle(valueToWrite, hSnapshotOutput))
      {
         print_error("dump_console_output");
         continue;
      }
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

   // Use cmd.exe to allow shell commands like "dir" to work properly
   cmd = "cmd.exe /s /c \"" + cmd + "\"";
   std::vector<char> cmdBuf(cmd.size() + 1, '\0');
   cmd.copy(&(cmdBuf[0]), cmd.size());

   SECURITY_ATTRIBUTES sa = { sizeof(SECURITY_ATTRIBUTES) };
   sa.bInheritHandle = true;

   HANDLE hConIn = ::CreateFile("CONIN$",
                                GENERIC_READ|GENERIC_WRITE,
                                FILE_SHARE_READ|FILE_SHARE_WRITE,
                                &sa,
                                OPEN_EXISTING,
                                0,
                                nullptr);
   if (hConIn == INVALID_HANDLE_VALUE)
   {
      print_error("CreateFile");
      return 1;
   }

   HANDLE hConOut = ::CreateFile("CONOUT$",
                                 GENERIC_READ|GENERIC_WRITE,
                                 FILE_SHARE_READ|FILE_SHARE_WRITE,
                                 &sa,
                                 OPEN_EXISTING,
                                 0,
                                 nullptr);
   if (hConOut == INVALID_HANDLE_VALUE)
   {
      print_error("CreateFile");
      return 1;
   }

   hSnapshotOutput = ::GetStdHandle(STD_ERROR_HANDLE);
   if (!::SetStdHandle(STD_ERROR_HANDLE, hConOut))
   {
      print_error("SetStdHandle");
      return 1;
   }

   SMALL_RECT screenSize = { 1, 1, 3, 3 };
   if (!::SetConsoleWindowInfo(hConOut, TRUE, &screenSize))
   {
      print_error("SetConsoleWindowInfo");
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

   if (!::CreateProcess(nullptr,
                        &(cmdBuf[0]),
                        nullptr,
                        nullptr,
                        TRUE,
                        0,
                        nullptr,
                        nullptr,
                        &si,
                        &pi))
   {
      print_error("CreateProcess");
      return 1;
   }

   hReadyForExitEvent = ::CreateEvent(nullptr, true, true, nullptr);
   if (hReadyForExitEvent == INVALID_HANDLE_VALUE)
   {
      print_error("CreateEvent");
      return 1;
   }

   boost::thread(&transferStdInToConsole, hConIn);
   boost::thread(&transferConsoleOutToStdErr, hConOut);

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
            if (::ResetEvent(hReadyForExitEvent))
            {
               ::WaitForSingleObject(hReadyForExitEvent, 2000);
            }

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
