#pragma once

#include "stdafx.h"

class Constants
{
public:
    const static LPOLESTR __gwt_disconnected;
    const static LPOLESTR valueOf;
    const static LPOLESTR Error;
    const static _bstr_t JavaScript;
    const static LPOLESTR __gwt_makeResult;
    const static LPOLESTR __gwt_makeTearOff;
};

__declspec(selectany) const LPOLESTR Constants::__gwt_disconnected = L"__gwt_disconnected";
__declspec(selectany) const LPOLESTR Constants::valueOf = L"valueOf";
__declspec(selectany) const LPOLESTR Constants::Error = L"Error";
__declspec(selectany) const _bstr_t Constants::JavaScript = _bstr_t(L"JavaScript");
__declspec(selectany) const LPOLESTR Constants::__gwt_makeResult = L"__gwt_makeResult";
__declspec(selectany) const LPOLESTR Constants::__gwt_makeTearOff = L"__gwt_makeTearOff";

