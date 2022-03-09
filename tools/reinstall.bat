wmic product where name="Pdx-Unlimiter" call uninstall /nointeractive
cd %~dp0..\
msiexec /i "build\bin\pdxu_installer-windows.msi" /quiet
