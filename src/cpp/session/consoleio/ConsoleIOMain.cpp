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
#include <sstream>
#include <string>
#include <vector>

#include <stdio.h>
#include <windows.h>

#include <boost/algorithm/string.hpp>
#include <boost/optional.hpp>

#include <core/system/Win32RequestResponsePipe.hpp>
#include <core/Error.hpp>

using namespace core;

/*
 * ConsoleIO is an Win32 program that makes it possible to programatically
 * send input and receive output (actually take snapshots of the output
 * buffer) of Win32 console applications that use low-level console IO.
 * The calling program must set up a Win32RequestResponsePipe in parent
 * mode before launching ConsoleIO. Then use CreateProcess to launch
 * consoleio.exe, making sure that bInheritHandles is true and dwCreationFlags
 * includes CREATE_NEW_CONSOLE.
 *
 * The program that needs to be executed and its arguments, should be
 * passed as arguments to consoleio.
 *
 * ConsoleIO expects two types of requests:
 *
 * "i<input>" - Send the input to the console. Response: empty
 * "o" - Respond with a snapshot of the console output.
 */

void print_usage()
{

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

HANDLE dup(HANDLE handle)
{
   HANDLE hProc = GetCurrentProcess();
   HANDLE hDup;
   if (!::DuplicateHandle(hProc,
                          handle,
                          hProc,
                          &hDup,
                          0,
                          FALSE,
                          DUPLICATE_SAME_ACCESS))
   {
      print_error("DuplicateHandle");
      return INVALID_HANDLE_VALUE;
   }

   return hDup;
}

BOOL attach_console(HANDLE hProc, DWORD dwProcessId)
{
   HANDLE origStdIn = dup(::GetStdHandle(STD_INPUT_HANDLE));
   HANDLE origStdOut = dup(::GetStdHandle(STD_OUTPUT_HANDLE));
   HANDLE origStdErr = dup(::GetStdHandle(STD_ERROR_HANDLE));

   ::FreeConsole();

   while (!::AttachConsole(dwProcessId))
   {
      if (::GetLastError() != ERROR_GEN_FAILURE
          || ::WaitForSingleObject(hProc, 0) != WAIT_TIMEOUT)
      {
         return false;
      }
   }

   SetStdHandle(STD_INPUT_HANDLE, origStdIn);
   SetStdHandle(STD_OUTPUT_HANDLE, origStdOut);
   SetStdHandle(STD_ERROR_HANDLE, origStdErr);
   return true;
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

BOOL capture_console_output(HANDLE hConsoleOut, std::string* pOutput)
{
   static std::vector<CHAR_INFO> buffer;

   CONSOLE_SCREEN_BUFFER_INFO csbInfo;
   if (!::GetConsoleScreenBufferInfo(hConsoleOut, &csbInfo))
      return false;

   COORD targetSize = {csbInfo.dwSize.X, csbInfo.dwCursorPosition.Y};

   DWORD cells = targetSize.X * targetSize.Y;
   buffer.resize(cells);

   COORD from = {0, 0};

   SMALL_RECT rect;
   rect.Top = 0;
   rect.Left = 0;
   rect.Bottom = targetSize.Y-1;
   rect.Right = targetSize.X-1;

   if (!::ReadConsoleOutput(hConsoleOut,
                            &(buffer[0]),
                            targetSize,
                            from,
                            &rect))
   {
      return false;
   }

   for (SHORT row = rect.Top; row <= rect.Bottom; row++)
   {
      size_t lineStart = row * targetSize.X + rect.Left;
      size_t lineEnd = row * targetSize.X + rect.Right;
      while (lineEnd != lineStart)
      {
         if (buffer[lineEnd].Char.AsciiChar == ' ')
            lineEnd--;
         else
            break;
      }

      for (size_t pos = lineStart; pos <= lineEnd; pos++)
      {
         CHAR c = buffer[pos].Char.AsciiChar;
         pOutput->push_back(c);
      }
      pOutput->push_back('\r');
      pOutput->push_back('\n');
   }

   return true;
}

// Dump the entire console buffer (up to the cursor) of hConsole
// and write it to hOutput
BOOL dump_console_output(HANDLE hConsole, HANDLE hOutput)
{
   std::string output;
   if (!capture_console_output(hConsole, &output))
      return false;

   const CHAR* pData = output.c_str();
   DWORD bytesToWrite = output.size();
   while (bytesToWrite > 0)
   {
      DWORD bytesWritten;
      if (!::WriteFile(hOutput,
                       pData,
                       bytesToWrite,
                       &bytesWritten,
                       NULL))
      {
         return false;
      }
      bytesToWrite -= bytesWritten;
      pData += bytesWritten;
   }
   return true;
}

int main(int argc, char** argv)
{
   if (argc < 2)
   {
      std::cerr << "Error: Not enough arguments" << std::endl;
      print_usage();
      return 1;
   }

   core::system::Win32RequestResponsePipe rrpipe;
   Error error = rrpipe.childInit();
   if (error)
   {
      std::cerr << "Couldn't initialize request/response pipe" << std::endl;
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

   // Create a pipe
   HANDLE hRead, hWrite;
   if (!::CreatePipe(&hRead, &hWrite, NULL, 0))
   {
      print_error("CreatePipe");
      return 1;
   }
   if (!::CloseHandle(hWrite))
   {
      print_error("CloseHandle");
      return 1;
   }

   STARTUPINFO si = {0};
   si.cb = sizeof(STARTUPINFO);
   si.dwFlags = STARTF_USESHOWWINDOW | STARTF_USESTDHANDLES;
   si.wShowWindow = SW_HIDE;
   si.hStdInput = hRead;
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

   HANDLE hConIn = ::CreateFile("CONIN$",
                                GENERIC_READ|GENERIC_WRITE,
                                FILE_SHARE_READ|FILE_SHARE_WRITE,
                                NULL,
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

   if (!::SetStdHandle(STD_INPUT_HANDLE, hConIn) ||
       !::SetStdHandle(STD_OUTPUT_HANDLE, hConOut) ||
       !::SetStdHandle(STD_ERROR_HANDLE, hConOut))
   {
      print_error("SetStdHandle");
      return 1;
   }

   COORD newSize = {80, 160};
   if (!::SetConsoleScreenBufferSize(hConOut, newSize))
   {
      print_error("SetConsoleScreenBufferSize");
      return 1;
   }

   std::vector<HANDLE> handles;
   handles.push_back(pi.hProcess);
   handles.push_back(rrpipe.requestEvent());

   std::vector<char> buffer;
   boost::optional<DWORD> optExitCode;

   while (true)
   {
      DWORD waitResult = ::WaitForMultipleObjects(handles.size(),
                                                  &(handles[0]),
                                                  FALSE,
                                                  INFINITE);
      if (waitResult == WAIT_OBJECT_0)
      {
         // Process has exited
         DWORD exitCode;
         if (::GetExitCodeProcess(pi.hProcess, &exitCode))
         {
            *optExitCode = exitCode;
         }
         else
         {
            print_error("GetExitCodeProcess");
            return 1;
         }
      }
      else if (waitResult == WAIT_OBJECT_0 + 1)   // command
      {
         buffer.resize(0);
         error = rrpipe.readRequest(&buffer);
         if (error)
         {
            std::cerr << "Error reading request" << std::endl;
            return 1;
         }

         std::string response;

         if (buffer.empty())
         {
            std::cerr << "Unexpectedly empty request" << std::endl;
            return 1;
         }
         if (buffer[0] == 'o')
         {
            if (!capture_console_output(hConOut, &response))
            {
               print_error("capture_console_output");
               return 1;
            }
         }
         else if (buffer[0] == 'i')
         {
            if (!send_console_input(hConIn, buffer.begin() + 1, buffer.end()))
            {
               print_error("send_console_input");
               return 1;
            }
         }
         else
         {
            std::cerr << "Unexpected request" << std::endl;
            return 1;
         }

         // Even in the case of empty responses, we must send a response so
         // that the parent process can unblock. (Or if we exit the process,
         // that will cause the parent process to unblock as well as the pipe
         // will be broken.)
         error = rrpipe.writeResponse(response);
         if (error)
         {
            std::cerr << "Error writing response" << std::endl;
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
