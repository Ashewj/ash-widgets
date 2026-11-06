$ErrorActionPreference='Stop'
$sdk=Join-Path $PSScriptRoot 'tools/sdk'
$bt=Join-Path $sdk 'build-tools/35.0.0'
$platform=Join-Path $sdk 'platforms/android-35/android.jar'
$java='C:/Program Files/Eclipse Adoptium/jdk-21.0.12.101-hotspot/bin'
$src=Join-Path $PSScriptRoot 'android/app/src/main'
$out=Join-Path $PSScriptRoot 'android/build'
New-Item -ItemType Directory -Force "$out/classes","$out/generated","$out/dex" | Out-Null
function Check { if($LASTEXITCODE -ne 0){throw "Build falhou: $LASTEXITCODE"} }
& "$bt/aapt2.exe" compile --dir "$src/res" -o "$out/res.zip"; Check
& "$bt/aapt2.exe" link -o "$out/resources.apk" -I $platform --manifest "$src/AndroidManifest.xml" --java "$out/generated" "$out/res.zip"; Check
$sources=@(Get-ChildItem "$src/java","$out/generated" -Recurse -Filter '*.java' | ForEach-Object FullName)
& "$java/javac.exe" -encoding UTF-8 --release 8 -classpath $platform -d "$out/classes" @sources; Check
& "$java/jar.exe" cf "$out/classes.jar" -C "$out/classes" .; Check
$env:JAVA_HOME=Split-Path $java
& "$bt/d8.bat" --lib $platform --min-api 26 --output "$out/dex" "$out/classes.jar"; Check
Copy-Item "$out/resources.apk" "$out/unsigned.apk" -Force
& "$java/jar.exe" uf "$out/unsigned.apk" -C "$out/dex" classes.dex; Check
& "$bt/zipalign.exe" -f 4 "$out/unsigned.apk" "$out/aligned.apk"; Check
if(!(Test-Path "$out/debug.keystore")){& "$java/keytool.exe" -genkeypair -keystore "$out/debug.keystore" -storepass android -keypass android -alias androiddebugkey -dname 'CN=Widget POCO' -keyalg RSA -validity 10000; Check}
& "$bt/apksigner.bat" sign --ks "$out/debug.keystore" --ks-pass pass:android --out "$out/ash-widgets.apk" "$out/aligned.apk"; Check
& "$bt/apksigner.bat" verify "$out/ash-widgets.apk"; Check
Write-Output "APK: $out/ash-widgets.apk"
