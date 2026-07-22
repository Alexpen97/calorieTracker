<#
.SYNOPSIS
  Seed NutriTrack dev accounts with ~30 days of realistic diary data.

.DESCRIPTION
  Talks only to the gateway. Requires AUTH_MODE=dev on auth-service.
  Seeds both frontend Dev login (code "dev") and agent-debug by default.
  Skips accounts that already have diary + weight history (persistent DB).
  Pass -Force to wipe and reseed.

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

# Well-known OFF barcodes (best effort). Fallbacks are created as submissions.
$BarcodeFoods = @(
  @{ barcode = "3017620422003"; role = "SNACK"; grams = 20 }   # Nutella-ish
  @{ barcode = "5449000000996"; role = "SNACK"; grams = 330 }  # Coke
  @{ barcode = "5000112548167"; role = "BREAKFAST"; grams = 40 } # cereal-ish
)

$DemoFoods = @(
  @{
    name = "Seed Oat Porridge"; brand = "NutriTrack Demo"; role = "BREAKFAST"; grams = 250
    nutrients = @(
      @{ code = "energy_kcal"; amountPer100g = 88; unit = "kcal" }
      @{ code = "protein"; amountPer100g = 3.5; unit = "g" }
      @{ code = "fat"; amountPer100g = 1.8; unit = "g" }
      @{ code = "carbohydrates"; amountPer100g = 14.5; unit = "g" }
      @{ code = "fiber"; amountPer100g = 2.0; unit = "g" }
    )
  }
  @{
    name = "Seed Greek Yogurt"; brand = "NutriTrack Demo"; role = "BREAKFAST"; grams = 150
    nutrients = @(
      @{ code = "energy_kcal"; amountPer100g = 97; unit = "kcal" }
      @{ code = "protein"; amountPer100g = 9.0; unit = "g" }
      @{ code = "fat"; amountPer100g = 5.0; unit = "g" }
      @{ code = "carbohydrates"; amountPer100g = 3.6; unit = "g" }
    )
  }
  @{
    name = "Seed Scrambled Eggs"; brand = "NutriTrack Demo"; role = "BREAKFAST"; grams = 120
    nutrients = @(
      @{ code = "energy_kcal"; amountPer100g = 155; unit = "kcal" }
      @{ code = "protein"; amountPer100g = 13; unit = "g" }
      @{ code = "fat"; amountPer100g = 11; unit = "g" }
      @{ code = "carbohydrates"; amountPer100g = 1.1; unit = "g" }
    )
  }
  @{
    name = "Seed Chicken Rice Bowl"; brand = "NutriTrack Demo"; role = "LUNCH"; grams = 380
    nutrients = @(
      @{ code = "energy_kcal"; amountPer100g = 142; unit = "kcal" }
      @{ code = "protein"; amountPer100g = 12; unit = "g" }
      @{ code = "fat"; amountPer100g = 4.2; unit = "g" }
      @{ code = "carbohydrates"; amountPer100g = 14; unit = "g" }
      @{ code = "fiber"; amountPer100g = 1.5; unit = "g" }
    )
  }
  @{
    name = "Seed Turkey Sandwich"; brand = "NutriTrack Demo"; role = "LUNCH"; grams = 220
    nutrients = @(
      @{ code = "energy_kcal"; amountPer100g = 210; unit = "kcal" }
      @{ code = "protein"; amountPer100g = 14; unit = "g" }
      @{ code = "fat"; amountPer100g = 7; unit = "g" }
      @{ code = "carbohydrates"; amountPer100g = 22; unit = "g" }
      @{ code = "fiber"; amountPer100g = 2.2; unit = "g" }
    )
  }
  @{
    name = "Seed Salmon Potatoes"; brand = "NutriTrack Demo"; role = "DINNER"; grams = 400
    nutrients = @(
      @{ code = "energy_kcal"; amountPer100g = 160; unit = "kcal" }
      @{ code = "protein"; amountPer100g = 13; unit = "g" }
      @{ code = "fat"; amountPer100g = 7.5; unit = "g" }
      @{ code = "carbohydrates"; amountPer100g = 11; unit = "g" }
    )
  }
  @{
    name = "Seed Veggie Pasta"; brand = "NutriTrack Demo"; role = "DINNER"; grams = 350
    nutrients = @(
      @{ code = "energy_kcal"; amountPer100g = 130; unit = "kcal" }
      @{ code = "protein"; amountPer100g = 5; unit = "g" }
      @{ code = "fat"; amountPer100g = 3.5; unit = "g" }
      @{ code = "carbohydrates"; amountPer100g = 20; unit = "g" }
      @{ code = "fiber"; amountPer100g = 3.0; unit = "g" }
    )
  }
  @{
    name = "Seed Banana"; brand = "NutriTrack Demo"; role = "SNACK"; grams = 120
    nutrients = @(
      @{ code = "energy_kcal"; amountPer100g = 89; unit = "kcal" }
      @{ code = "protein"; amountPer100g = 1.1; unit = "g" }
      @{ code = "fat"; amountPer100g = 0.3; unit = "g" }
      @{ code = "carbohydrates"; amountPer100g = 23; unit = "g" }
      @{ code = "fiber"; amountPer100g = 2.6; unit = "g" }
    )
  }
  @{
    name = "Seed Protein Bar"; brand = "NutriTrack Demo"; role = "SNACK"; grams = 60
    nutrients = @(
      @{ code = "energy_kcal"; amountPer100g = 380; unit = "kcal" }
      @{ code = "protein"; amountPer100g = 30; unit = "g" }
      @{ code = "fat"; amountPer100g = 12; unit = "g" }
      @{ code = "carbohydrates"; amountPer100g = 35; unit = "g" }
      @{ code = "fiber"; amountPer100g = 8; unit = "g" }
    )
  }
  @{
    name = "Seed Mixed Nuts"; brand = "NutriTrack Demo"; role = "SNACK"; grams = 40
    nutrients = @(
      @{ code = "energy_kcal"; amountPer100g = 607; unit = "kcal" }
      @{ code = "protein"; amountPer100g = 20; unit = "g" }
      @{ code = "fat"; amountPer100g = 54; unit = "g" }
      @{ code = "carbohydrates"; amountPer100g = 15; unit = "g" }
      @{ code = "fiber"; amountPer100g = 7; unit = "g" }
    )
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
    ContentType = "application/json"
  }
  if ($null -ne $Body) {
    $params.Body = ($Body | ConvertTo-Json -Depth 10 -Compress)
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
    if ($product -and $product.id) {
      $foods.Add([pscustomobject]@{
          kind   = "product"
          id     = $product.id
          name   = $product.name
          role   = $item.role
          grams  = $item.grams
        }) | Out-Null
      Write-Ok "  Catalog: $($product.name)"
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
  $byName = @{}
  foreach ($s in $mine) {
    if ($s.name) { $byName[$s.name] = $s }
  }

  foreach ($demo in $DemoFoods) {
    $existing = $byName[$demo.name]
    if (-not $existing) {
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
      Write-Ok "  Created demo food: $($demo.name)"
    }
    else {
      Write-Info "  Reusing demo food: $($demo.name)"
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
