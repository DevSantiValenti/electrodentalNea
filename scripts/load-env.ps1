$envFile = Join-Path (Split-Path -Parent $PSScriptRoot) ".env"

if (-not (Test-Path -LiteralPath $envFile)) {
	Write-Error "No se encontró .env en la raíz del proyecto."
	exit 1
}

Get-Content -LiteralPath $envFile | ForEach-Object {
	$line = $_.Trim()
	if ($line -eq "" -or $line.StartsWith("#")) {
		return
	}
	$parts = $line.Split("=", 2)
	if ($parts.Length -eq 2) {
		[Environment]::SetEnvironmentVariable($parts[0], $parts[1], "Process")
	}
}

Write-Host "Variables de entorno de Mercado Pago cargadas para esta terminal."
