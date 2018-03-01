
// variant of RReadConsole which reads a script instead of using an interactive console
int RReadScript (const char *pmt,
                 CONSOLE_BUFFER_CHAR* buf,
                 int buflen,
                 int hist)
{
   if (s_runScript.empty())
   {
      // exit after we have consumed the script
      return 0;
   }

   // ensure input fits in buffer; we need two extra bytes -- one for the terminating newline and
   // one for the terminating null
   if (s_runScript.length() > (buflen - 2))
   {
      std::string msg = "Script too long (" +
         safe_convert::numberToString(s_runScript.length()) + "), max is " +
         safe_convert::numberToString(buflen - 2) + " characters)";
      LOG_ERROR_MESSAGE(msg);
      rSuicide(msg);
   }

   // copy input into buffer
   s_runScript.copy(buf, s_runScript.length(), 0);

   // append newline and terminating null
   buf[s_runScript.length()] = '\n';
   buf[s_runScript.length() +1 ] = '\0';

   // remove script
   s_runScript.clear();

   // success
   return 0;
}

// variant of RWriteConsoleEx which writes to standard out
void RWriteStdout (const char *buf, int buflen, int otype)
{
   std::cout << buf;
}


   
