set SIGN_TARGET=%1%

"C:\Program Files\Microsoft SDKs\Windows\v7.1\Bin\signtool" sign /sha1 E5D84022D15FD10154AA7A75EC6CA055EE7E52A7 /v /ac "cert\After_10-10-10_MSCV-VSClass3.cer" /s MY /n "RStudio, Inc." /t http://timestamp.VeriSign.com/scripts/timstamp.dll %SIGN_TARGET%

"C:\Program Files\Microsoft SDKs\Windows\v7.1\Bin\signtool" verify /v /kp %SIGN_TARGET%
 
