# DatStation Development

### ADB

#### Install/Uninstall Apps
```
adb uninstall com.robertotorino.datstation
adb install app\build\outputs\apk\debug\datstation-debug.apk
adb shell pm clear com.robertotorino.datstation
```

### GitHub

```powershell
git pull origin master

git remote add origin git@github.com:RobertoTorino/DatStation.git
git push -u origin master
git fetch --all
git remote -v
```

![qrcode-gh.png](media/qrcode-gh.png)


**[RobertoTorino](https://github.com/RobertoTorino)**        
