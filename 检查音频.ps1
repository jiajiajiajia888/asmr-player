$ErrorActionPreference='Stop'
$source=Join-Path $PSScriptRoot 'app/src/main/java/com/longjiahao/asmrplayer'
$runDir=Join-Path $PSScriptRoot ('build/audio-checks/'+[guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $runDir -Force | Out-Null
$names=@('SoundEngine','EffectDefaults','AutoChannelFill','CenterChannel','RoomDistance','BinauralOrbit','LeftRightAlternation','KemarData','SwipeSeek','SeekMath','SleepDeadline','VideoFit')
$checks=@('AlternationCheck','AudioQualityCheck','AutoChannelFillCheck','SmoothFillCheck','SoundEngineCheck','SpatialCheck','SeekMathCheck','SleepDeadlineCheck','VideoFitCheck')
$sources=@($names | ForEach-Object {Join-Path $source ($_.ToString()+'.java')})
$sources+=@($checks | ForEach-Object {Join-Path (Join-Path $PSScriptRoot 'checks') ($_.ToString()+'.java')})
$java=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin/java.exe'}else{'java'}
$javac=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin/javac.exe'}else{'javac'}
& $javac -encoding UTF-8 --release 17 -d $runDir @sources
if($LASTEXITCODE -ne 0){throw 'Audio check compilation failed.'}
foreach($check in $checks){
    & $java -cp $runDir $check
    if($LASTEXITCODE -ne 0){throw "Audio check failed: $check"}
}
Write-Output 'AUDIO_CHECK_SUITE_OK'
