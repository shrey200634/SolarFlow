# SolarFlow developer helpers (works in Windows PowerShell 5.1 and PowerShell 7).
#
# Load it in ANY PowerShell window, from any folder:
#     . C:\SolarFlow\scripts\sf.ps1
# (note the dot and the space at the start)
#
# Then:  Connect-SF            logs in the test users (run again after 15 minutes: tokens expire)
#        Test-QuotationGuard   self-checking test, prints PASS / FAIL lines
param(
    [string]$BaseUrl = "",
    [string]$SeedPassword = "ChangeMe123!"
)

# ---------- find the running backend (docker = 8081, bootRun = 8080) ----------
if (-not $BaseUrl) {
    foreach ($root in "http://localhost:8081", "http://localhost:8080") {
        try {
            $health = Invoke-RestMethod -Uri "$root/actuator/health" -TimeoutSec 3
            if ($health.status -eq "UP") { $BaseUrl = "$root/api/v1"; break }
        } catch { }
    }
}
if ($BaseUrl) { Write-Host "Backend: $BaseUrl" -ForegroundColor Green }
else { Write-Host "No backend answering on 8081 or 8080. Start it first (docker compose up -d)." -ForegroundColor Red }

$base   = $BaseUrl
$seedPw = $SeedPassword

# database helpers (container name comes from docker-compose.yml)
$db = "solarflow-db"
$u  = docker exec $db printenv POSTGRES_USER 2>$null
$d  = docker exec $db printenv POSTGRES_DB 2>$null

# endpoint paths
$createPath = "/leads/{0}/quotations"
$getPath    = "/quotations/{0}"
$pdfPath    = "/quotations/{0}/pdf"
$acceptPath = "/quotations/{0}/accept"

$SF_Status = 0
$SF_Quiet  = $false
$SF_Fail   = 0

# ---------- HTTP helper ----------
# Call METHOD /path [token] [body-hashtable]. Returns the parsed JSON, and sets $SF_Status.


function Call($method, $path, $token, $body) {
    $headers = @{}
    if ($token) { $headers["Authorization"] = "Bearer $token" }

    $req = @{ Method = $method; Uri = "$base$path"; Headers = $headers; UseBasicParsing = $true }
    if ($null -ne $body) {
        $req["ContentType"] = "application/json"
        $req["Body"] = ($body | ConvertTo-Json -Depth 8)
    }

    try {
        $r = Invoke-WebRequest @req
        $script:SF_Status = [int]$r.StatusCode
        if (-not $r.Content) { return $null }
        $obj = $r.Content | ConvertFrom-Json
        if ($obj -is [array]) { return ,$obj }
        return $obj
    }
    catch {
        $code = "no response"
        if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
        $script:SF_Status = $code

        $msg = $_.ErrorDetails.Message
        if (-not $msg -and $_.Exception.Response) {
            try {
                $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $msg = $sr.ReadToEnd()
            } catch { }
        }
        if (-not $script:SF_Quiet) { Write-Host "HTTP $code : $msg" -ForegroundColor Yellow }
        return $null
    }
}

function Login($email, $pw) {
    $r = Call POST "/auth/login" $null @{ email = $email; password = $pw }
    if (-not $r) { return $null }
    foreach ($f in "accessToken", "token", "jwt", "access_token") { if ($r.$f) { return $r.$f } }
    Write-Host "Token field not found in the login response." -ForegroundColor Red
}

function Register($name, $email, $phone) {
    $script:SF_Quiet = $true
    Call POST "/auth/register" $null @{ fullName = $name; email = $email; phone = $phone; password = "Test@12345"; consent = $true } | Out-Null
    $script:SF_Quiet = $false
}

# ---------- self-checking helpers ----------
function Expect($label, $actual, $expected) {
    $ok = $false
    $a = [decimal]0; $e = [decimal]0
    if ([decimal]::TryParse("$actual", [ref]$a) -and [decimal]::TryParse("$expected", [ref]$e)) { $ok = ($a -eq $e) }
    else { $ok = ("$actual" -eq "$expected") }

    if ($ok) { Write-Host ("PASS  {0,-52} {1}" -f $label, $actual) -ForegroundColor Green }
    else {
        $script:SF_Fail = $script:SF_Fail + 1
        Write-Host ("FAIL  {0,-52} actual=[{1}] expected=[{2}]" -f $label, $actual, $expected) -ForegroundColor Red
    }
}

function ExpectStatus($label, $expected) { Expect $label $script:SF_Status $expected }

# ---------- setup helpers ----------
function Connect-SF {
    Register "Homeowner A" "homeA@test.com" "9876500001"
    Register "Homeowner B" "homeB@test.com" "9876500002"
    $script:tokS     = Login "sales@solarflow.local" $seedPw
    $script:tokAdmin = Login "admin@solarflow.local" $seedPw
    $script:tokA     = Login "homeA@test.com" "Test@12345"
    $script:tokB     = Login "homeB@test.com" "Test@12345"
    "S: $([bool]$tokS)   Admin: $([bool]$tokAdmin)   A: $([bool]$tokA)   B: $([bool]$tokB)"
}

# Set the seeded admin and sales passwords to $seedPw (use when login says "Invalid Email or Password").
function Reset-StaffPasswords {
    docker exec $db psql -U $u -d $d -c "create extension if not exists pgcrypto;"
    docker exec $db psql -U $u -d $d -c "update users set password_hash = crypt('$seedPw', gen_salt('bf', 12)) where email in ('admin@solarflow.local','sales@solarflow.local');"
}

# Creates a lead, uploads a test bill, waits for extraction, confirms the bill manually.
function RunLead($token, $ownership, $roofType, $roofArea, $bill, $pincode, $phone, $units, $label) {
    Write-Host "`n===== $label =====" -ForegroundColor Cyan

    $lead = Call POST "/leads" $token @{
        ownership = $ownership; roofType = $roofType; roofAreaSqft = $roofArea
        declaredMonthlyBill = $bill; pincode = $pincode; phone = $phone
    }
    $leadId = $lead.id
    if (-not $leadId) { Write-Host "Lead not created." -ForegroundColor Red; return $null }

    # a plain 800x600 test image
    $img = "$env:TEMP\sf-bill.jpg"
    if (-not (Test-Path $img)) {
        Add-Type -AssemblyName System.Drawing
        $bmp = New-Object System.Drawing.Bitmap 800, 600
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.Clear([System.Drawing.Color]::White)
        $g.DrawString("TEST ELECTRICITY BILL", (New-Object System.Drawing.Font("Arial", 32)), [System.Drawing.Brushes]::Black, 50, 250)
        $bmp.Save($img, [System.Drawing.Imaging.ImageFormat]::Jpeg)
        $g.Dispose(); $bmp.Dispose()
    }

    $up = curl.exe -s -X POST "$base/leads/$leadId/documents" -H "Authorization: Bearer $token" -F "file=@$img;type=image/jpeg" | ConvertFrom-Json
    $docId = $up.id
    if (-not $docId) { Write-Host "Upload failed." -ForegroundColor Red; return $null }

    for ($i = 0; $i -lt 30; $i++) {
        $ex = Call GET "/documents/$docId/extraction" $token
        if ($ex.status -notin @("UPLOADED", "EXTRACTING")) { break }
        Start-Sleep -Seconds 2
    }

    Call POST "/documents/$docId/extraction/confirm" $token @{
        consumerNumber = "410012345678"; tariff = "LT-I Residential"; sanctionedLoadKw = 5; monthlyUnits = $units
    } | Out-Null

    $q = Call GET "/leads/$leadId/qualification" $token
    Write-Host ("SCORE: {0}   NEXT ACTION: {1}" -f $q.score, $q.nextAction) -ForegroundColor Green

    return [pscustomobject]@{ LeadId = $leadId; DocId = $docId }
}

# ---------- self-checking test: one accepted quotation per lead, quotation list ----------
function Test-QuotationGuard {
    $script:SF_Fail = 0
    Connect-SF | Out-Null

    $phone = "98" + (Get-Random -Minimum 10000000 -Maximum 99999999)
    $lead = RunLead $tokA "OWNED" "RCC_FLAT" 450 3200 "440010" $phone 340 "GUARD TEST LEAD"
    if (-not $lead) { Write-Host "Setup failed, stopping." -ForegroundColor Red; return }
    $l = $lead.LeadId

    Write-Host "`n--- two quotations before any acceptance" -ForegroundColor Cyan
    $qa = Call POST ($createPath -f $l) $tokS @{ selectedCapacityKw = 2.5 }
    ExpectStatus "create quotation A (2.5 kW)" 200
    $qb = Call POST ($createPath -f $l) $tokS @{ selectedCapacityKw = 1.5 }
    ExpectStatus "create quotation B (1.5 kW)" 200

    Write-Host "`n--- list endpoint" -ForegroundColor Cyan
    $lst = Call GET "/leads/$l/quotations" $tokA
    ExpectStatus "owner lists quotations" 200
    Expect "owner sees 2 quotations" $lst.Count 2
    Expect "newest first (B, 1.5 kW)" $lst[0].selectedCapacityKw 1.5
    $lstS = Call GET "/leads/$l/quotations" $tokS
    Expect "sales sees 2 quotations" $lstS.Count 2
    Call GET "/leads/$l/quotations" $tokB | Out-Null
    ExpectStatus "other homeowner is refused" 404
    Call GET "/leads/$l/quotations" $null | Out-Null
    ExpectStatus "no token is refused" 401

    Write-Host "`n--- project needs an accepted quotation" -ForegroundColor Cyan
    Call POST "/projects" $tokS @{ quotationId = $qb.id; advanceAmount = 10000 } | Out-Null
    ExpectStatus "project from unaccepted quotation" 409

    Write-Host "`n--- one accepted quotation per lead" -ForegroundColor Cyan
    $acc = Call POST ($acceptPath -f $qa.id) $tokA
    Expect "accept A" $acc.status "ACCEPTED"
    Call POST ($acceptPath -f $qb.id) $tokA | Out-Null
    ExpectStatus "accept B as well is blocked" 409
    Call POST ($createPath -f $l) $tokS @{ selectedCapacityKw = 2.5 } | Out-Null
    ExpectStatus "new quotation after acceptance is blocked" 409

    Write-Host "`n--- the bypass: sales resets the lead status to QUOTED" -ForegroundColor Cyan
    $patched = Call PATCH "/leads/$l" $tokS @{ status = "QUOTED" }
    Expect "status was reset to QUOTED" $patched.status "QUOTED"
    Call POST ($createPath -f $l) $tokS @{ selectedCapacityKw = 2.5 } | Out-Null
    ExpectStatus "new quotation still blocked" 409
    Call POST ($acceptPath -f $qb.id) $tokA | Out-Null
    ExpectStatus "accept B still blocked" 409

    Write-Host "`n--- project from the accepted quotation" -ForegroundColor Cyan
    $proj = Call POST "/projects" $tokS @{ quotationId = $qa.id; advanceAmount = 20000 }
    ExpectStatus "create project" 201
    Expect "project starts at ADVANCE_PAID" $proj.status "ADVANCE_PAID"

    Write-Host ""
    if ($SF_Fail -eq 0) { Write-Host "ALL CHECKS PASSED" -ForegroundColor Green }
    else { Write-Host "$SF_Fail CHECK(S) FAILED - paste the FAIL lines" -ForegroundColor Red }
}

Write-Host "Loaded. Next:  Connect-SF   then   Test-QuotationGuard" -ForegroundColor Cyan


function Test-BillCorrectionValidation {
    $script:SF_Fail = 0
    Connect-SF | Out-Null

    if (-not $tokA) {
        Write-Host "Homeowner login failed. Check Connect-SF." -ForegroundColor Red
        return
    }

    Write-Host "`n===== BILL CORRECTION VALIDATION TEST =====" -ForegroundColor Cyan

    # Create a fresh lead with a declared monthly bill of Rs. 6000.
    $phone = "98" + (Get-Random -Minimum 10000000 -Maximum 99999999)

    $lead = Call POST "/leads" $tokA @{
        ownership = "OWNED"
        roofType = "RCC_FLAT"
        roofAreaSqft = 450
        declaredMonthlyBill = 6000
        pincode = "440010"
        phone = $phone
    }

    if (-not $lead.id) {
        Write-Host "Lead creation failed. Stopping." -ForegroundColor Red
        return
    }

    # Create a test image if it does not already exist.
    $img = "$env:TEMP\sf-bill.jpg"

    if (-not (Test-Path $img)) {
        Add-Type -AssemblyName System.Drawing
        $bmp = New-Object System.Drawing.Bitmap 800, 600
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.Clear([System.Drawing.Color]::White)
        $g.DrawString(
            "TEST ELECTRICITY BILL",
            (New-Object System.Drawing.Font("Arial", 32)),
            [System.Drawing.Brushes]::Black,
            50, 250
        )
        $bmp.Save($img, [System.Drawing.Imaging.ImageFormat]::Jpeg)
        $g.Dispose()
        $bmp.Dispose()
    }

    # Upload the document.
    $up = curl.exe -s -X POST "$base/leads/$($lead.id)/documents" `
        -H "Authorization: Bearer $tokA" `
        -F "file=@$img;type=image/jpeg" | ConvertFrom-Json

    if (-not $up.id) {
        Write-Host "Document upload failed. Stopping." -ForegroundColor Red
        return
    }

    $docId = $up.id
    Write-Host "Lead: $($lead.id)"
    Write-Host "Document: $docId"

    # Wait for extraction to finish.
    for ($i = 0; $i -lt 30; $i++) {
        $ex = Call GET "/documents/$docId/extraction" $tokA
        if ($ex.status -notin @("UPLOADED", "EXTRACTING")) { break }
        Start-Sleep -Seconds 2
    }

    Write-Host "Extraction status: $($ex.status)"

    # Each request supplies all required fields so the test isolates
    # the particular invalid value.
    $cases = @(
        @{
            Label = "11-digit consumer number rejected"
            Body = @{
                consumerNumber = "41001234567"
                tariff = "LT-I Residential"
                sanctionedLoadKw = 5
                monthlyUnits = 500
            }
        },
        @{
            Label = "HT tariff rejected"
            Body = @{
                consumerNumber = "410012345678"
                tariff = "HT Commercial"
                sanctionedLoadKw = 5
                monthlyUnits = 500
            }
        },
        @{
            Label = "Load below 0.5 kW rejected"
            Body = @{
                consumerNumber = "410012345678"
                tariff = "LT-I Residential"
                sanctionedLoadKw = 0.2
                monthlyUnits = 500
            }
        },
        @{
            Label = "Load above 50 kW rejected"
            Body = @{
                consumerNumber = "410012345678"
                tariff = "LT-I Residential"
                sanctionedLoadKw = 51
                monthlyUnits = 500
            }
        },
        @{
            Label = "Units above 5000 rejected"
            Body = @{
                consumerNumber = "410012345678"
                tariff = "LT-I Residential"
                sanctionedLoadKw = 5
                monthlyUnits = 5001
            }
        },
        @{
            Label = "Implied rate Rs. 60/unit rejected"
            Body = @{
                consumerNumber = "410012345678"
                tariff = "LT-I Residential"
                sanctionedLoadKw = 5
                monthlyUnits = 100
            }
        }
    )

    Write-Host "`n--- Invalid correction cases ---" -ForegroundColor Cyan

    foreach ($case in $cases) {
        Call POST "/documents/$docId/extraction/confirm" $tokA $case.Body | Out-Null
        ExpectStatus $case.Label 400

        $current = Call GET "/documents/$docId/extraction" $tokA
        $stillUnconfirmed = ($current.status -ne "CONFIRMED")
        Expect "$($case.Label): document remains unconfirmed" $stillUnconfirmed "True"
    }

    Write-Host "`n--- Valid correction after failed attempts ---" -ForegroundColor Cyan

    $valid = @{
        consumerNumber = "410012345678"
        tariff = "LT-I Residential"
        sanctionedLoadKw = 5
        monthlyUnits = 500
    }

    Call POST "/documents/$docId/extraction/confirm" $tokA $valid | Out-Null
    ExpectStatus "Valid correction succeeds (Rs. 12/unit)" 200

    $final = Call GET "/documents/$docId/extraction" $tokA
    Expect "Document is now CONFIRMED" $final.status "CONFIRMED"

    Write-Host ""
    if ($SF_Fail -eq 0) {
        Write-Host "ALL BILL CORRECTION CHECKS PASSED" -ForegroundColor Green
    }
    else {
        Write-Host "$SF_Fail CHECK(S) FAILED - paste the FAIL lines" -ForegroundColor Red
    }
}
