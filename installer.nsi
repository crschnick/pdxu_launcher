!include LogicLib.nsh
!include WinMessages.nsh
!include FileFunc.nsh

Name "Pdx-Unlimiter"
Icon "logo.ico"
Unicode True
RequestExecutionLevel user
OutFile "pdxu_installer.exe"

VIProductVersion                 "1.0.0.0"
VIAddVersionKey ProductName      "Pdx-Unlimiter"
VIAddVersionKey Comments         "Pdx-Unlimiter installer"
VIAddVersionKey CompanyName      "https://github.com/crschnick/pdx_unlimiter/"
VIAddVersionKey FileDescription  "Pdx-Unlimiter installer"
VIAddVersionKey FileVersion      1
VIAddVersionKey ProductVersion   1
VIAddVersionKey InternalName     "Pdx-Unlimiter"
VIAddVersionKey OriginalFilename "pdxu_installer.exe"

 Function .onInit
  StrCpy $INSTDIR `$PROFILE\pdx_unlimiter`
 FunctionEnd

PageEx license
  LicenseText "GNU General Public License" "Ok"
  LicenseData "license_file.txt"
PageExEnd

Page directory
Page instfiles checkReinstall


Var LAUNCHERDIR

Function checkReinstall
  StrCpy $LAUNCHERDIR `$INSTDIR\launcher`
  ${If} ${FileExists} $LAUNCHERDIR
     MessageBox MB_YESNO "Do you want to reinstall the Pdx-Unlimiter launcher?" IDYES Reinstall
       Abort
     Reinstall:
       RMDir /r $LAUNCHERDIR
       ${If} ${Errors}
         MessageBox MB_OK "Error deleting files"
         Abort
       ${EndIf}
  ${EndIf}
FunctionEnd

Section
    SetOverwrite off
    SetOutPath $LAUNCHERDIR
    File /r "build\image\*"
    File "logo.ico"
    File build\bin\launcher.exe

   ${If} ${Errors}
     MessageBox MB_OK "Error installing files"
     Abort
   ${EndIf}

    CreateShortCut "$INSTDIR\Pdx-Unlimiter.lnk" "$LAUNCHERDIR\launcher.exe" "" `$LAUNCHERDIR\logo.ico` 0

    MessageBox MB_YESNO "Create desktop shortcut?" IDNO No
      CreateShortCut "$DESKTOP\Pdx-Unlimiter.lnk" "$LAUNCHERDIR\launcher.exe" "" `$LAUNCHERDIR\logo.ico` 0
    No:
SectionEnd

Function .onInstSuccess
  SetOutPath $LAUNCHERDIR
  Exec "$LAUNCHERDIR\launcher.exe -installed"
FunctionEnd