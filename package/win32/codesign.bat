
set SIGN_TARGET=%1%

"C:\Program Files\Microsoft SDKs\Windows\v7.1\Bin\signtool" sign /v /ac "cert\After_10-10-10_MSCV-VSClass3.cer" /s MY /n "RStudio, Inc." /t http://timestamp.VeriSign.com/scripts/timstamp.dll %SIGN_TARGET%
"C:\Program Files\Microsoft SDKs\Windows\v7.1\Bin\signtool" verify /v /kp %SIGN_TARGET%
 







