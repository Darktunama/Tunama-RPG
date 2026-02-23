$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-25.0.1.8-hotspot"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Set-Location "c:\Users\Kais_\PluginRPG\rpg"
& mvn clean package
