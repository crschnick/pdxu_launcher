Icon "logo.ico"
Unicode True
SilentInstall silent
RequestExecutionLevel user
ShowInstDetails hide

!define OutFileSignSHA256 ".\sign\signtool sign /f .\sign\certificate.cer /fd sha256 /tr http://timestamp.comodoca.com?td=sha256 /td sha256 /as /v"

OutFile "launcher.exe"

Section
  nsExec::Exec 'bin\updater.bat'
SectionEnd

!finalize "${OutFileSignSHA256} launcher.exe"