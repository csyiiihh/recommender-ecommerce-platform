param([switch]$Start, [switch]$EnableBusinessTasks)
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
& node (Join-Path $PSScriptRoot 'preflight.cjs')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if (-not $Start) { Write-Output '前置检查通过；使用 -Start 才会启动隔离后端。'; exit 0 }
$jarPath = Join-Path $projectRoot 'springboot/target/springboot-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jarPath)) { throw '请先构建后端 JAR。' }
$configPath = Join-Path $projectRoot 'springboot/config/application-integration.example.properties'
$configUri = ([System.Uri]::new($configPath)).AbsoluteUri
$uploadPath = Join-Path $projectRoot 'integration-uploads'
$businessTasks = if ($EnableBusinessTasks) { 'true' } else { 'false' }
& java "-Dapp.upload-dir=$uploadPath" -jar $jarPath "--spring.config.additional-location=$configUri" '--spring.profiles.active=integration' '--spring.config.import=' "--group-buy.tasks.enabled=$businessTasks" "--refund.retry.enabled=$businessTasks" '--order.unpaid-expiry.enabled=false' '--inventory.reservation.enabled=false'
exit $LASTEXITCODE
