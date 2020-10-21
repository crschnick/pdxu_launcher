Icon "logo.ico"
Unicode True
SilentInstall silent
RequestExecutionLevel user
ShowInstDetails hide

OutFile "launcher.exe"

Section
  nsExec::Exec 'bin\updater.bat'
SectionEnd