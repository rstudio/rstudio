#ifndef __wininet_h__
#define __wininet_h__

extern "C"  {

#define INTERNET_OPEN_TYPE_PRECONFIG    0
#define INTERNET_FLAG_PRAGMA_NOCACHE    0x00000100
#define INTERNET_FLAG_RELOAD            0x80000000
#define INTERNET_FLAG_NO_COOKIES        0x00080000
#define INTERNET_FLAG_NO_UI             0x00000200

typedef void* HINTERNET;
HINTERNET STDAPICALLTYPE InternetOpenA(LPCSTR lpszAgent, DWORD dwAccessType, LPCSTR lpszProxy, LPCSTR lpszProxyBypass, DWORD dwFlags);
HINTERNET STDAPICALLTYPE InternetOpenUrlA(HINTERNET hInternet, LPCSTR lpszUrl, LPCSTR lpszHeaders, DWORD dwHeadersLength, DWORD dwFlags, DWORD_PTR dwContext);
BOOL STDAPICALLTYPE InternetReadFile(HINTERNET hFile, void* lpBuffer, DWORD dwNumberOfBytesToRead, LPDWORD lpdwNumberOfBytesRead);
BOOL STDAPICALLTYPE InternetCloseHandle(HINTERNET hInternet);

}

#endif // __wininet_h__
