# Compiles and runs shipped ranking + update version logic without Android SDK.
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$Scratch = "C:\Users\ADMINI~1\AppData\Local\Temp\grok-goal-4bae35c893bd\implementer"
$JavaHome = Join-Path $Scratch "tools\jdk-17"
if (-not (Test-Path "$JavaHome\bin\java.exe")) {
    $alt = "C:\Users\ADMINI~1\AppData\Local\Temp\grok-goal-d55a1bb11511\implementer\tools\jdk-17"
    if (Test-Path "$alt\bin\java.exe") { $JavaHome = $alt }
}
$Java = Join-Path $JavaHome "bin\java.exe"
$Work = Join-Path $Scratch "ranking-test"
$OutLog = Join-Path $Scratch "language-tests.log"
$UpdateLog = Join-Path $Scratch "update-tests.log"

if (-not (Test-Path $Java)) { throw "JDK not found at $JavaHome" }
New-Item -ItemType Directory -Force -Path $Work, (Join-Path $Work "out") | Out-Null

$KotlincHome = Join-Path $Work "kotlinc"
if (-not (Test-Path (Join-Path $KotlincHome "bin\kotlinc.bat"))) {
    # reuse previous kotlinc if present
    $alt = "C:\Users\ADMINI~1\AppData\Local\Temp\grok-goal-d55a1bb11511\implementer\ranking-test\kotlinc"
    if (Test-Path "$alt\bin\kotlinc.bat") {
        $KotlincHome = $alt
    } else {
        Write-Host "Downloading Kotlin compiler..."
        $KotlincZip = Join-Path $Work "kotlin-compiler.zip"
        Invoke-WebRequest -Uri "https://github.com/JetBrains/kotlin/releases/download/v2.0.21/kotlin-compiler-2.0.21.zip" -OutFile $KotlincZip -UseBasicParsing
        Expand-Archive -Path $KotlincZip -DestinationPath $Work -Force
    }
}

$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;" + $env:Path
$Kotlinc = Join-Path $KotlincHome "bin\kotlinc.bat"

$Sources = @(
    (Join-Path $Root "app\src\main\java\com\zakratv\app\data\model\CoreModels.kt"),
    (Join-Path $Root "app\src\main\java\com\zakratv\app\data\ranking\LanguagePreference.kt"),
    (Join-Path $Root "app\src\main\java\com\zakratv\app\data\ranking\StreamRanker.kt"),
    (Join-Path $Root "app\src\main\java\com\zakratv\app\data\update\UpdateVersionLogic.kt"),
    (Join-Path $Root "app\src\test\java\com\zakratv\app\data\ranking\StandaloneRankingHarness.kt")
)
foreach ($s in $Sources) {
    if (-not (Test-Path $s)) { throw "Missing source: $s" }
}

$Jar = Join-Path $Work "out\ranking-tests.jar"
& $Kotlinc @Sources -include-runtime -d $Jar
if ($LASTEXITCODE -ne 0) { throw "kotlinc failed" }

$run = & $Java -cp $Jar com.zakratv.app.data.ranking.StandaloneRankingHarness 2>&1
$joined = ($run | ForEach-Object { "$_" }) -join "`n"
$joined | Set-Content -Path $OutLog -Encoding UTF8
# same harness covers update tests
$joined | Set-Content -Path $UpdateLog -Encoding UTF8
Write-Host $joined
if ($joined -notmatch "ALL_PASS") { throw "Harness failed" }
Write-Host "Wrote $OutLog and $UpdateLog"
