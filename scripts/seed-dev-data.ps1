<#
.SYNOPSIS
  Seed NutriTrack dev accounts with ~30 days of realistic diary data.

.DESCRIPTION
  Talks only to the gateway. Requires AUTH_MODE=dev on auth-service.
  Seeds both frontend Dev login (code "dev") and agent-debug by default.
  Demo foods include full vitamin + mineral panels (catalog codes/units).
  Skips accounts that already have diary + weight history (persistent DB).
  Pass -Force to wipe and reseed (also recreates demo foods with micros).

.EXAMPLE
  ./scripts/seed-dev-data.ps1
  ./scripts/seed-dev-data.ps1 -Force
  ./scripts/seed-dev-data.ps1 -BaseUrl https://gateway-production-777b.up.railway.app
  ./scripts/seed-dev-data.ps1 -Account agent-debug -Days 30
#>
[CmdletBinding()]
param(
  [string]$BaseUrl = "http://localhost:8080",
  [string]$RedirectUri = "",
  [ValidateSet("all", "dev", "agent-debug")]
  [string]$Account = "all",
  [int]$Days = 30,
  [string]$Zone = "",
  [switch]$Force
)

$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd("/")

if (-not $RedirectUri) {
  if ($BaseUrl -match "railway\.app") {
    $RedirectUri = "https://front-end-production-4a95.up.railway.app/auth/callback"
  }
  else {
    $RedirectUri = "http://localhost:5173/auth/callback"
  }
}

if (-not $Zone) {
  # Diary APIs require an IANA zone (e.g. Europe/Berlin), not Windows IDs.
  $Zone = "Europe/Berlin"
}

$Persona = @{
  sex           = "MALE"
  birthDate     = "1992-04-12"
  heightCm      = 178
  weightKg      = 82.0
  activityLevel = "MODERATE"
  objective     = "LOSE"
}

# Catalog default_unit for micros (µg as U+00B5 so the file stays ASCII-safe).
$Ug = ([char]0x00B5).ToString() + "g"
$MicroUnitByCode = [ordered]@{
  vitamin_a   = $Ug
  vitamin_b1  = "mg"
  vitamin_b2  = "mg"
  vitamin_b3  = "mg"
  vitamin_b5  = "mg"
  vitamin_b6  = "mg"
  vitamin_b7  = $Ug
  vitamin_b9  = $Ug
  vitamin_b12 = $Ug
  vitamin_c   = "mg"
  vitamin_d   = $Ug
  vitamin_e   = "mg"
  vitamin_k   = $Ug
  calcium     = "mg"
  iron        = "mg"
  magnesium   = "mg"
  potassium   = "mg"
  sodium      = "mg"
  zinc        = "mg"
  iodine      = $Ug
  selenium    = $Ug
  copper      = "mg"
  manganese   = "mg"
  phosphorus  = "mg"
  chromium    = $Ug
  molybdenum  = $Ug
}
$SeedMicroProbeCodes = @("vitamin_a", "vitamin_c", "calcium", "iron", "potassium", "magnesium")

function New-SeedNutrient([string]$Code, $Amount, [string]$Unit) {
  @{ code = $Code; amountPer100g = [double]$Amount; unit = $Unit }
}

function New-SeedMicros([hashtable]$Amounts) {
  $list = [System.Collections.Generic.List[object]]::new()
  foreach ($code in $MicroUnitByCode.Keys) {
    if (-not $Amounts.ContainsKey($code)) {
      throw "Demo food micros missing required code: $code"
    }
    $amount = [double]$Amounts[$code]
    # CreateSubmissionRequest requires @Positive amountPer100g — omit zeros.
    if ($amount -le 0) { continue }
    $list.Add((New-SeedNutrient $code $amount $MicroUnitByCode[$code])) | Out-Null
  }
  return , $list.ToArray()
}

function Join-SeedNutrients {
  param([object[]]$Macros, [object[]]$Micros)
  return @($Macros) + @($Micros)
}

function Test-HasSeedMicros($FoodOrSubmission) {
  $nutrients = @($FoodOrSubmission.nutrients)
  if ($nutrients.Count -eq 0) { return $false }
  $codes = @($nutrients | ForEach-Object { $_.code })
  $hits = @($SeedMicroProbeCodes | Where-Object { $codes -contains $_ }).Count
  return $hits -ge 4
}

# Well-known OFF barcodes (best effort). Fallbacks are created as submissions.
# Only kept when the catalog product already carries micronutrients.
$BarcodeFoods = @(
  @{ barcode = "3017620422003"; role = "SNACK"; grams = 20 }   # Nutella-ish
  @{ barcode = "5449000000996"; role = "SNACK"; grams = 330 }  # Coke
  @{ barcode = "5000112548167"; role = "BREAKFAST"; grams = 40 } # cereal-ish
)

$DemoFoods = @(
  @{
    name = "Seed Oat Porridge"; brand = "NutriTrack Demo"; role = "BREAKFAST"; grams = 250
    nutrients = Join-SeedNutrients -Macros @(
      (New-SeedNutrient "energy_kcal" 88 "kcal")
      (New-SeedNutrient "protein" 3.5 "g")
      (New-SeedNutrient "fat" 1.8 "g")
      (New-SeedNutrient "carbohydrates" 14.5 "g")
      (New-SeedNutrient "fiber" 2.0 "g")
    ) -Micros (New-SeedMicros @{
      vitamin_a = 2; vitamin_b1 = 0.12; vitamin_b2 = 0.05; vitamin_b3 = 0.4; vitamin_b5 = 0.3
      vitamin_b6 = 0.06; vitamin_b7 = 2; vitamin_b9 = 12; vitamin_b12 = 0; vitamin_c = 0
      vitamin_d = 0; vitamin_e = 0.3; vitamin_k = 1.2
      calcium = 20; iron = 1.2; magnesium = 35; potassium = 120; sodium = 5; zinc = 0.8
      iodine = 1; selenium = 4; copper = 0.12; manganese = 1.1; phosphorus = 110
      chromium = 2; molybdenum = 8
    })
  }
  @{
    name = "Seed Greek Yogurt"; brand = "NutriTrack Demo"; role = "BREAKFAST"; grams = 150
    nutrients = Join-SeedNutrients -Macros @(
      (New-SeedNutrient "energy_kcal" 97 "kcal")
      (New-SeedNutrient "protein" 9.0 "g")
      (New-SeedNutrient "fat" 5.0 "g")
      (New-SeedNutrient "carbohydrates" 3.6 "g")
    ) -Micros (New-SeedMicros @{
      vitamin_a = 45; vitamin_b1 = 0.04; vitamin_b2 = 0.22; vitamin_b3 = 0.2; vitamin_b5 = 0.4
      vitamin_b6 = 0.05; vitamin_b7 = 2.5; vitamin_b9 = 12; vitamin_b12 = 0.6; vitamin_c = 0.5
      vitamin_d = 0.1; vitamin_e = 0.1; vitamin_k = 0.4
      calcium = 120; iron = 0.1; magnesium = 12; potassium = 150; sodium = 45; zinc = 0.5
      iodine = 25; selenium = 6; copper = 0.02; manganese = 0.01; phosphorus = 140
      chromium = 1; molybdenum = 3
    })
  }
  @{
    name = "Seed Scrambled Eggs"; brand = "NutriTrack Demo"; role = "BREAKFAST"; grams = 120
    nutrients = Join-SeedNutrients -Macros @(
      (New-SeedNutrient "energy_kcal" 155 "kcal")
      (New-SeedNutrient "protein" 13 "g")
      (New-SeedNutrient "fat" 11 "g")
      (New-SeedNutrient "carbohydrates" 1.1 "g")
    ) -Micros (New-SeedMicros @{
      vitamin_a = 160; vitamin_b1 = 0.04; vitamin_b2 = 0.45; vitamin_b3 = 0.1; vitamin_b5 = 1.4
      vitamin_b6 = 0.14; vitamin_b7 = 16; vitamin_b9 = 44; vitamin_b12 = 1.1; vitamin_c = 0
      vitamin_d = 2.0; vitamin_e = 1.0; vitamin_k = 0.3
      calcium = 50; iron = 1.8; magnesium = 12; potassium = 140; sodium = 150; zinc = 1.2
      iodine = 25; selenium = 30; copper = 0.07; manganese = 0.03; phosphorus = 200
      chromium = 1; molybdenum = 4
    })
  }
  @{
    name = "Seed Chicken Rice Bowl"; brand = "NutriTrack Demo"; role = "LUNCH"; grams = 380
    nutrients = Join-SeedNutrients -Macros @(
      (New-SeedNutrient "energy_kcal" 142 "kcal")
      (New-SeedNutrient "protein" 12 "g")
      (New-SeedNutrient "fat" 4.2 "g")
      (New-SeedNutrient "carbohydrates" 14 "g")
      (New-SeedNutrient "fiber" 1.5 "g")
    ) -Micros (New-SeedMicros @{
      vitamin_a = 35; vitamin_b1 = 0.08; vitamin_b2 = 0.1; vitamin_b3 = 5.5; vitamin_b5 = 0.9
      vitamin_b6 = 0.35; vitamin_b7 = 3; vitamin_b9 = 18; vitamin_b12 = 0.25; vitamin_c = 4
      vitamin_d = 0.2; vitamin_e = 0.4; vitamin_k = 8
      calcium = 25; iron = 0.9; magnesium = 28; potassium = 220; sodium = 280; zinc = 0.9
      iodine = 8; selenium = 18; copper = 0.08; manganese = 0.3; phosphorus = 130
      chromium = 3; molybdenum = 10
    })
  }
  @{
    name = "Seed Turkey Sandwich"; brand = "NutriTrack Demo"; role = "LUNCH"; grams = 220
    nutrients = Join-SeedNutrients -Macros @(
      (New-SeedNutrient "energy_kcal" 210 "kcal")
      (New-SeedNutrient "protein" 14 "g")
      (New-SeedNutrient "fat" 7 "g")
      (New-SeedNutrient "carbohydrates" 22 "g")
      (New-SeedNutrient "fiber" 2.2 "g")
    ) -Micros (New-SeedMicros @{
      vitamin_a = 25; vitamin_b1 = 0.15; vitamin_b2 = 0.12; vitamin_b3 = 4.2; vitamin_b5 = 0.6
      vitamin_b6 = 0.28; vitamin_b7 = 2; vitamin_b9 = 30; vitamin_b12 = 0.35; vitamin_c = 2
      vitamin_d = 0.1; vitamin_e = 0.6; vitamin_k = 4
      calcium = 60; iron = 1.5; magnesium = 30; potassium = 240; sodium = 420; zinc = 1.4
      iodine = 12; selenium = 16; copper = 0.1; manganese = 0.4; phosphorus = 150
      chromium = 4; molybdenum = 12
    })
  }
  @{
    name = "Seed Salmon Potatoes"; brand = "NutriTrack Demo"; role = "DINNER"; grams = 400
    nutrients = Join-SeedNutrients -Macros @(
      (New-SeedNutrient "energy_kcal" 160 "kcal")
      (New-SeedNutrient "protein" 13 "g")
      (New-SeedNutrient "fat" 7.5 "g")
      (New-SeedNutrient "carbohydrates" 11 "g")
    ) -Micros (New-SeedMicros @{
      vitamin_a = 40; vitamin_b1 = 0.12; vitamin_b2 = 0.18; vitamin_b3 = 6.0; vitamin_b5 = 1.1
      vitamin_b6 = 0.55; vitamin_b7 = 4; vitamin_b9 = 25; vitamin_b12 = 2.4; vitamin_c = 8
      vitamin_d = 8.0; vitamin_e = 1.5; vitamin_k = 3
      calcium = 30; iron = 0.8; magnesium = 32; potassium = 420; sodium = 90; zinc = 0.7
      iodine = 18; selenium = 28; copper = 0.12; manganese = 0.2; phosphorus = 200
      chromium = 2; molybdenum = 6
    })
  }
  @{
    name = "Seed Veggie Pasta"; brand = "NutriTrack Demo"; role = "DINNER"; grams = 350
    nutrients = Join-SeedNutrients -Macros @(
      (New-SeedNutrient "energy_kcal" 130 "kcal")
      (New-SeedNutrient "protein" 5 "g")
      (New-SeedNutrient "fat" 3.5 "g")
      (New-SeedNutrient "carbohydrates" 20 "g")
      (New-SeedNutrient "fiber" 3.0 "g")
    ) -Micros (New-SeedMicros @{
      vitamin_a = 220; vitamin_b1 = 0.1; vitamin_b2 = 0.08; vitamin_b3 = 1.2; vitamin_b5 = 0.4
      vitamin_b6 = 0.15; vitamin_b7 = 2; vitamin_b9 = 55; vitamin_b12 = 0; vitamin_c = 18
      vitamin_d = 0; vitamin_e = 1.2; vitamin_k = 45
      calcium = 45; iron = 1.4; magnesium = 40; potassium = 280; sodium = 210; zinc = 0.8
      iodine = 5; selenium = 4; copper = 0.15; manganese = 0.6; phosphorus = 90
      chromium = 5; molybdenum = 15
    })
  }
  @{
    name = "Seed Banana"; brand = "NutriTrack Demo"; role = "SNACK"; grams = 120
    nutrients = Join-SeedNutrients -Macros @(
      (New-SeedNutrient "energy_kcal" 89 "kcal")
      (New-SeedNutrient "protein" 1.1 "g")
      (New-SeedNutrient "fat" 0.3 "g")
      (New-SeedNutrient "carbohydrates" 23 "g")
      (New-SeedNutrient "fiber" 2.6 "g")
    ) -Micros (New-SeedMicros @{
      vitamin_a = 3; vitamin_b1 = 0.03; vitamin_b2 = 0.07; vitamin_b3 = 0.7; vitamin_b5 = 0.3
      vitamin_b6 = 0.4; vitamin_b7 = 0.5; vitamin_b9 = 20; vitamin_b12 = 0; vitamin_c = 8.7
      vitamin_d = 0; vitamin_e = 0.1; vitamin_k = 0.5
      calcium = 5; iron = 0.3; magnesium = 27; potassium = 358; sodium = 1; zinc = 0.15
      iodine = 1; selenium = 1; copper = 0.08; manganese = 0.3; phosphorus = 22
      chromium = 1; molybdenum = 2
    })
  }
  @{
    name = "Seed Protein Bar"; brand = "NutriTrack Demo"; role = "SNACK"; grams = 60
    nutrients = Join-SeedNutrients -Macros @(
      (New-SeedNutrient "energy_kcal" 380 "kcal")
      (New-SeedNutrient "protein" 30 "g")
      (New-SeedNutrient "fat" 12 "g")
      (New-SeedNutrient "carbohydrates" 35 "g")
      (New-SeedNutrient "fiber" 8 "g")
    ) -Micros (New-SeedMicros @{
      vitamin_a = 200; vitamin_b1 = 0.4; vitamin_b2 = 0.4; vitamin_b3 = 5; vitamin_b5 = 2
      vitamin_b6 = 0.5; vitamin_b7 = 15; vitamin_b9 = 100; vitamin_b12 = 1.0; vitamin_c = 20
      vitamin_d = 2.5; vitamin_e = 4; vitamin_k = 12
      calcium = 200; iron = 4; magnesium = 80; potassium = 250; sodium = 220; zinc = 3
      iodine = 40; selenium = 20; copper = 0.3; manganese = 0.8; phosphorus = 180
      chromium = 15; molybdenum = 20
    })
  }
  @{
    name = "Seed Mixed Nuts"; brand = "NutriTrack Demo"; role = "SNACK"; grams = 40
    nutrients = Join-SeedNutrients -Macros @(
      (New-SeedNutrient "energy_kcal" 607 "kcal")
      (New-SeedNutrient "protein" 20 "g")
      (New-SeedNutrient "fat" 54 "g")
      (New-SeedNutrient "carbohydrates" 15 "g")
      (New-SeedNutrient "fiber" 7 "g")
    ) -Micros (New-SeedMicros @{
      vitamin_a = 1; vitamin_b1 = 0.3; vitamin_b2 = 0.2; vitamin_b3 = 2.5; vitamin_b5 = 0.6
      vitamin_b6 = 0.25; vitamin_b7 = 8; vitamin_b9 = 50; vitamin_b12 = 0; vitamin_c = 0.5
      vitamin_d = 0; vitamin_e = 12; vitamin_k = 8
      calcium = 80; iron = 3.5; magnesium = 180; potassium = 600; sodium = 5; zinc = 3.5
      iodine = 2; selenium = 120; copper = 1.2; manganese = 2.5; phosphorus = 400
      chromium = 8; molybdenum = 25
    })
  }
)

function Write-Info([string]$Message) { Write-Host $Message -ForegroundColor Cyan }
function Write-Ok([string]$Message) { Write-Host $Message -ForegroundColor Green }
function Write-Warn([string]$Message) { Write-Host $Message -ForegroundColor Yellow }

function Invoke-Api {
  param(
    [string]$Method,
    [string]$Path,
    [hashtable]$Headers = @{},
    [object]$Body = $null,
    [switch]$AllowError
  )

  $uri = "$BaseUrl$Path"
  $params = @{
    Method      = $Method
    Uri         = $uri
    Headers     = $Headers
    ContentType = "application/json; charset=utf-8"
  }
  if ($null -ne $Body) {
    $json = ($Body | ConvertTo-Json -Depth 10 -Compress)
    # PowerShell string bodies can mojibake µg; send explicit UTF-8 bytes.
    $params.Body = [System.Text.Encoding]::UTF8.GetBytes($json)
  }

  try {
    return Invoke-RestMethod @params
  }
  catch {
    if ($AllowError) { return $null }
    $status = $null
    $respBody = $null
    if ($_.Exception.Response) {
      $status = [int]$_.Exception.Response.StatusCode
      try {
        $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
        $respBody = $reader.ReadToEnd()
        $reader.Close()
      }
      catch { }
    }
    throw "API $Method $Path failed (status=$status): $($_.Exception.Message) $respBody"
  }
}

function Invoke-ApiRawStatus {
  param(
    [string]$Method,
    [string]$Path,
    [hashtable]$Headers = @{},
    [object]$Body = $null
  )
  $uri = "$BaseUrl$Path"
  $params = @{
    Method  = $Method
    Uri     = $uri
    Headers = $Headers
  }
  if ($null -ne $Body) {
    $params.ContentType = "application/json"
    $params.Body = ($Body | ConvertTo-Json -Depth 10 -Compress)
  }
  try {
    $resp = Invoke-WebRequest @params -UseBasicParsing
    return [int]$resp.StatusCode
  }
  catch {
    if ($_.Exception.Response) {
      return [int]$_.Exception.Response.StatusCode
    }
    throw
  }
}

function Get-AuthHeaders([string]$AccessToken) {
  return @{ Authorization = "Bearer $AccessToken" }
}

function Connect-DevAccount([string]$Code) {
  $tokens = Invoke-Api -Method POST -Path "/api/auth/google/callback" -Body @{
    code        = $Code
    redirectUri = $RedirectUri
  }
  if (-not $tokens.accessToken) {
    throw "Dev login failed for code=$Code. Is AUTH_MODE=dev on auth-service?"
  }
  return $tokens
}

function Update-TokensOn401 {
  param(
    [scriptblock]$Action,
    [ref]$Tokens
  )
  try {
    return & $Action $Tokens.Value.accessToken
  }
  catch {
    if ("$($_.Exception.Message)" -notmatch "401|Unauthorized") { throw }
    Write-Warn "  Token expired — refreshing…"
    $refreshed = Invoke-Api -Method POST -Path "/api/auth/refresh" -Body @{
      refreshToken = $Tokens.Value.refreshToken
    }
    $Tokens.Value = $refreshed
    return & $Action $Tokens.Value.accessToken
  }
}

function Ensure-Persona([ref]$Tokens) {
  $profile = Update-TokensOn401 -Tokens $Tokens -Action {
    param($access)
    Invoke-Api -Method GET -Path "/api/users/me" -Headers (Get-AuthHeaders $access)
  }
  $weights = Update-TokensOn401 -Tokens $Tokens -Action {
    param($access)
    Invoke-Api -Method GET -Path "/api/users/me/weight" -Headers (Get-AuthHeaders $access)
  }

  $needsOnboarding = (
    $null -eq $profile.sex -or
    $null -eq $profile.birthDate -or
    $null -eq $profile.heightCm -or
    $null -eq $profile.activityLevel -or
    ($null -eq $weights -or $weights.Count -eq 0)
  )

  if ($needsOnboarding) {
    Write-Info "  Completing onboarding…"
    Update-TokensOn401 -Tokens $Tokens -Action {
      param($access)
      Invoke-Api -Method POST -Path "/api/users/me/onboarding" -Headers (Get-AuthHeaders $access) -Body $Persona
    } | Out-Null
  }
  else {
    Write-Info "  Updating profile to demo persona…"
    Update-TokensOn401 -Tokens $Tokens -Action {
      param($access)
      Invoke-Api -Method PUT -Path "/api/users/me" -Headers (Get-AuthHeaders $access) -Body @{
        sex           = $Persona.sex
        birthDate     = $Persona.birthDate
        heightCm      = $Persona.heightCm
        activityLevel = $Persona.activityLevel
        objective     = $Persona.objective
      }
    } | Out-Null
    Update-TokensOn401 -Tokens $Tokens -Action {
      param($access)
      Invoke-Api -Method POST -Path "/api/users/me/goals/recalculate?apply=true" -Headers (Get-AuthHeaders $access)
    } | Out-Null
  }
}

function Get-DateList([int]$DayCount) {
  $today = [DateTime]::Today
  $dates = @()
  for ($i = $DayCount - 1; $i -ge 0; $i--) {
    $dates += $today.AddDays(-$i).ToString("yyyy-MM-dd")
  }
  return $dates
}

function Reset-DiaryAndWater {
  param([ref]$Tokens, [string[]]$Dates)

  $deletedEntries = 0
  $deletedWater = 0
  $n = 0
  foreach ($date in $Dates) {
    $n++
    if ($n % 15 -eq 0) { Write-Info "  Clearing diary/water… $n/$($Dates.Count)" }

    $entries = Update-TokensOn401 -Tokens $Tokens -Action {
      param($access)
      Invoke-Api -Method GET -Path "/api/diary/entries?date=$date&zone=$([uri]::EscapeDataString($Zone))" `
        -Headers (Get-AuthHeaders $access)
    }
    if ($null -eq $entries) { $entries = @() }
    elseif ($entries -isnot [System.Array]) { $entries = @($entries) }
    foreach ($entry in $entries) {
      if (-not $entry.id) { continue }
      Update-TokensOn401 -Tokens $Tokens -Action {
        param($access)
        $null = Invoke-ApiRawStatus -Method DELETE -Path "/api/diary/entries/$($entry.id)" `
          -Headers (Get-AuthHeaders $access)
      } | Out-Null
      $deletedEntries++
    }

    $waters = Update-TokensOn401 -Tokens $Tokens -Action {
      param($access)
      Invoke-Api -Method GET -Path "/api/diary/water?date=$date&zone=$([uri]::EscapeDataString($Zone))" `
        -Headers (Get-AuthHeaders $access)
    }
    if ($null -eq $waters) { $waters = @() }
    elseif ($waters -isnot [System.Array]) { $waters = @($waters) }
    foreach ($water in $waters) {
      if (-not $water.id) { continue }
      Update-TokensOn401 -Tokens $Tokens -Action {
        param($access)
        $null = Invoke-ApiRawStatus -Method DELETE -Path "/api/diary/water/$($water.id)" `
          -Headers (Get-AuthHeaders $access)
      } | Out-Null
      $deletedWater++
    }
  }
  return @{ entries = $deletedEntries; water = $deletedWater }
}

function Reset-Weights([ref]$Tokens) {
  $weights = Update-TokensOn401 -Tokens $Tokens -Action {
    param($access)
    Invoke-Api -Method GET -Path "/api/users/me/weight" -Headers (Get-AuthHeaders $access)
  }
  if ($null -eq $weights) { $weights = @() }
  elseif ($weights -isnot [System.Array]) { $weights = @($weights) }
  if ($weights.Count -eq 0) { return 0 }

  $deleted = 0
  $unsupported = $false
  foreach ($w in $weights) {
    if (-not $w.id) { continue }
    $status = Update-TokensOn401 -Tokens $Tokens -Action {
      param($access)
      Invoke-ApiRawStatus -Method DELETE -Path "/api/users/me/weight/$($w.id)" `
        -Headers (Get-AuthHeaders $access)
    }
    if ($status -eq 204) {
      $deleted++
    }
    elseif ($status -eq 404 -or $status -eq 405) {
      $unsupported = $true
      break
    }
    else {
      throw "Unexpected status $status deleting weight $($w.id)"
    }
  }

  if ($unsupported -and $deleted -eq 0) {
    Write-Warn "  Weight DELETE not available on this deployment — skipping weight wipe (new points will still be added). Redeploy user-profile-service for a clean reset."
    return 0
  }
  return $deleted
}

function Resolve-Foods([ref]$Tokens) {
  $foods = [System.Collections.Generic.List[object]]::new()

  foreach ($item in $BarcodeFoods) {
    $product = Update-TokensOn401 -Tokens $Tokens -Action {
      param($access)
      Invoke-Api -Method GET -Path "/api/products/barcode/$($item.barcode)" `
        -Headers (Get-AuthHeaders $access) -AllowError
    }
    if ($product -and $product.id -and (Test-HasSeedMicros $product)) {
      $foods.Add([pscustomobject]@{
          kind   = "product"
          id     = $product.id
          name   = $product.name
          role   = $item.role
          grams  = $item.grams
        }) | Out-Null
      Write-Ok "  Catalog (with micros): $($product.name)"
    }
    elseif ($product -and $product.id) {
      Write-Warn "  Catalog $($product.name) lacks vitamins/minerals — skipping barcode food"
    }
    else {
      Write-Warn "  Barcode $($item.barcode) unavailable — will use demo foods"
    }
  }

  $mine = Update-TokensOn401 -Tokens $Tokens -Action {
    param($access)
    Invoke-Api -Method GET -Path "/api/products/submissions/mine" -Headers (Get-AuthHeaders $access)
  }
  if ($null -eq $mine) { $mine = @() }
  elseif ($mine -isnot [System.Array]) { $mine = @($mine) }
  # Prefer newest submission per name (API returns submittedAt desc).
  $byName = @{}
  foreach ($s in $mine) {
    if ($s.name -and -not $byName.ContainsKey($s.name)) { $byName[$s.name] = $s }
  }

  foreach ($demo in $DemoFoods) {
    $existing = $byName[$demo.name]
    $reuse = $existing -and (-not $Force) -and (Test-HasSeedMicros $existing)
    if ($reuse) {
      Write-Info "  Reusing demo food: $($demo.name)"
    }
    else {
      $created = Update-TokensOn401 -Tokens $Tokens -Action {
        param($access)
        Invoke-Api -Method POST -Path "/api/products/submissions" -Headers (Get-AuthHeaders $access) -Body @{
          name         = $demo.name
          brand        = $demo.brand
          servingSizeG = $demo.grams
          nutrients    = $demo.nutrients
          force        = $true
        }
      }
      $existing = $created
      if ($Force) {
        Write-Ok "  Recreated demo food (force): $($demo.name)"
      }
      elseif ($byName.ContainsKey($demo.name)) {
        Write-Ok "  Recreated demo food (added micros): $($demo.name)"
      }
      else {
        Write-Ok "  Created demo food: $($demo.name)"
      }
    }

    $foods.Add([pscustomobject]@{
        kind  = "submission"
        id    = $existing.id
        name  = $demo.name
        role  = $demo.role
        grams = $demo.grams
      }) | Out-Null
  }

  if ($foods.Count -eq 0) {
    throw "No foods resolved. Need OFF barcode access or product submission API."
  }
  return $foods
}

function Get-FoodsForRole($Foods, [string]$Role) {
  $matched = @($Foods | Where-Object { $_.role -eq $Role })
  if ($matched.Count -eq 0) { return @($Foods) }
  return $matched
}

function Pick-One($Items, [System.Random]$Rng) {
  $arr = @($Items)
  return $arr[$Rng.Next(0, $arr.Count)]
}

function Vary-Grams([double]$Base, [System.Random]$Rng) {
  $factor = 0.85 + ($Rng.NextDouble() * 0.30)
  return [math]::Round($Base * $factor, 0)
}

function Local-Instant([string]$Date, [int]$Hour, [int]$Minute) {
  $local = [DateTime]::ParseExact("$Date $($Hour.ToString('00')):$($Minute.ToString('00')):00", "yyyy-MM-dd HH:mm:ss", $null)
  return ([DateTimeOffset]$local).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
}

function Seed-Days {
  param(
    [ref]$Tokens,
    $Foods,
    [string[]]$Dates,
    [System.Random]$Rng
  )

  $mealCount = 0
  $waterCount = 0
  $weightCount = 0
  $startWeight = [double]$Persona.weightKg
  $dayIndex = 0

  foreach ($date in $Dates) {
    $dayIndex++
    if ($dayIndex % 10 -eq 0 -or $dayIndex -eq 1) {
      Write-Info "  Seeding day $dayIndex/$($Dates.Count) ($date)…"
    }

    $lightDay = ($Rng.NextDouble() -lt 0.14)
    $meals = @("BREAKFAST", "LUNCH", "DINNER")
    if (-not $lightDay -and $Rng.NextDouble() -lt 0.40) {
      $meals += "SNACK"
    }
    if ($lightDay) {
      $meals = @("BREAKFAST", "DINNER")
      if ($Rng.NextDouble() -lt 0.5) { $meals = @("LUNCH", "SNACK") }
    }

    $mealHours = @{
      BREAKFAST = 8
      LUNCH     = 12
      DINNER    = 19
      SNACK     = 16
    }

    foreach ($meal in $meals) {
      $candidates = Get-FoodsForRole $Foods $meal
      $food = Pick-One $candidates $Rng
      $grams = Vary-Grams $food.grams $Rng
      $minute = $Rng.Next(0, 50)
      $body = @{
        weightG    = $grams
        mealType   = $meal
        consumedAt = (Local-Instant $date $mealHours[$meal] $minute)
      }
      if ($food.kind -eq "product") {
        $body.productId = $food.id
      }
      else {
        $body.submissionId = $food.id
      }

      Update-TokensOn401 -Tokens $Tokens -Action {
        param($access)
        Invoke-Api -Method POST -Path "/api/diary/entries" -Headers (Get-AuthHeaders $access) -Body $body
      } | Out-Null
      $mealCount++
    }

    $waterTarget = if ($lightDay) { $Rng.Next(1600, 2200) } else { $Rng.Next(1800, 2800) }
    $remaining = $waterTarget
    $logHour = 8
    while ($remaining -gt 0) {
      $sip = [math]::Min($remaining, $Rng.Next(200, 450))
      Update-TokensOn401 -Tokens $Tokens -Action {
        param($access)
        Invoke-Api -Method POST -Path "/api/diary/water" -Headers (Get-AuthHeaders $access) -Body @{
          amountMl = $sip
          loggedAt = (Local-Instant $date $logHour $Rng.Next(0, 40))
        }
      } | Out-Null
      $waterCount++
      $remaining -= $sip
      $logHour = [math]::Min(21, $logHour + $Rng.Next(2, 4))
    }

    # Weight every 2–3 days with gentle downward drift + noise
    if (($dayIndex % 3) -eq 1 -or $dayIndex -eq $Dates.Count) {
      # Oldest day ~82kg; today ~1.5–2kg lower
      $progress = ($dayIndex - 1) / [math]::Max(1, ($Dates.Count - 1))
      $trend = $startWeight - (1.8 * $progress)
      $noise = ($Rng.NextDouble() - 0.5) * 0.35
      $kg = [math]::Round($trend + $noise, 1)
      Update-TokensOn401 -Tokens $Tokens -Action {
        param($access)
        Invoke-Api -Method POST -Path "/api/users/me/weight" -Headers (Get-AuthHeaders $access) -Body @{
          weightKg   = $kg
          measuredAt = (Local-Instant $date 7 $Rng.Next(0, 30))
        }
      } | Out-Null
      $weightCount++
    }
  }

  return @{ meals = $mealCount; water = $waterCount; weights = $weightCount }
}

function Test-AccountAlreadySeeded {
  param([ref]$Tokens, [int]$DayCount)

  $weights = Update-TokensOn401 -Tokens $Tokens -Action {
    param($access)
    Invoke-Api -Method GET -Path "/api/users/me/weight" -Headers (Get-AuthHeaders $access)
  }
  $weightCount = if ($null -eq $weights) { 0 } else { @($weights).Count }
  $minWeights = [Math]::Min(5, [Math]::Max(1, [Math]::Floor($DayCount / 4)))
  if ($weightCount -lt $minWeights) {
    return $false
  }

  $recentDates = Get-DateList -DayCount ([Math]::Min(3, $DayCount))
  foreach ($date in $recentDates) {
    $entries = Update-TokensOn401 -Tokens $Tokens -Action {
      param($access)
      Invoke-Api -Method GET -Path "/api/diary/entries?date=$date&zone=$([uri]::EscapeDataString($Zone))" `
        -Headers (Get-AuthHeaders $access)
    }
    if ($null -eq $entries) { $entries = @() }
    elseif ($entries -isnot [System.Array]) { $entries = @($entries) }
    if ($entries.Count -gt 0) {
      return $true
    }
  }
  return $false
}

function Seed-Account([string]$Code, [string]$Label) {
  Write-Host ""
  Write-Info "=== Seeding $Label (code=$Code) ==="

  $tokens = Connect-DevAccount $Code
  $tokenRef = [ref]$tokens

  if (-not $Force -and (Test-AccountAlreadySeeded -Tokens $tokenRef -DayCount $Days)) {
    Write-Ok "  Already has weight + diary data — skipping (pass -Force to wipe and reseed)"
    return
  }

  Ensure-Persona -Tokens $tokenRef

  # Clear a wider window than we seed so shorter re-runs wipe leftover history
  # (e.g. previous 90-day seeds when switching to 30 days).
  $clearDays = [Math]::Max($Days, 120)
  $clearDates = Get-DateList -DayCount $clearDays
  $dates = Get-DateList -DayCount $Days
  Write-Info "  Resetting last $clearDays days (will seed $Days)…"
  $cleared = Reset-DiaryAndWater -Tokens $tokenRef -Dates $clearDates
  Write-Ok "  Deleted $($cleared.entries) diary entries, $($cleared.water) water logs"

  $weightDeleted = Reset-Weights -Tokens $tokenRef
  Write-Ok "  Deleted $weightDeleted weight logs"

  Write-Info "  Resolving foods…"
  $foods = Resolve-Foods -Tokens $tokenRef
  Write-Ok "  $($foods.Count) foods ready"

  $rng = [System.Random]::new(42 + $Label.GetHashCode())
  $seeded = Seed-Days -Tokens $tokenRef -Foods $foods -Dates $dates -Rng $rng
  Write-Ok "  Wrote $($seeded.meals) meals, $($seeded.water) water logs, $($seeded.weights) weights"
}

# --- main ---
Write-Info "NutriTrack dev data seeder"
Write-Info "  BaseUrl     = $BaseUrl"
Write-Info "  RedirectUri = $RedirectUri"
Write-Info "  Zone        = $Zone"
Write-Info "  Days        = $Days"
Write-Info "  Account     = $Account"
Write-Info "  Force       = $Force"

# Fail fast if auth is not in dev mode
try {
  $probe = Invoke-Api -Method POST -Path "/api/auth/google/callback" -Body @{
    code        = "dev:seed-probe"
    redirectUri = $RedirectUri
  }
  if (-not $probe.accessToken) { throw "No accessToken" }
}
catch {
  throw "Dev login probe failed. Set AUTH_MODE=dev on auth-service (local compose or temporary Railway), then retry. Details: $($_.Exception.Message)"
}

$accounts = @()
if ($Account -eq "all" -or $Account -eq "dev") {
  $accounts += @{ Code = "dev"; Label = "dev-user@example.com" }
}
if ($Account -eq "all" -or $Account -eq "agent-debug") {
  $accounts += @{ Code = "dev:agent-debug"; Label = "agent-debug@example.com" }
}

foreach ($acct in $accounts) {
  Seed-Account -Code $acct.Code -Label $acct.Label
}

Write-Host ""
Write-Ok "Done. Open the app, Dev login (or agent-debug API), and check Today / Analytics."
