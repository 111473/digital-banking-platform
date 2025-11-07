# Start background jobs for port forwarding
$job1 = Start-Job -ScriptBlock {
    kubectl port-forward service/account-opening-service 8081:8081 -n banking
}

$job2 = Start-Job -ScriptBlock {
    kubectl port-forward service/customer-account-service 8082:8082 -n banking
}

Write-Host "Waiting for port forwards to start..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

Write-Host "`n=== Testing Services ===" -ForegroundColor Green

# Test Account Opening Service
Write-Host "`n1. Account Opening Service Health:" -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8081/actuator/health"
    Write-Host "✅ Status: $($response.status)" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed: $_" -ForegroundColor Red
}

# Test Customer Account Service
Write-Host "`n2. Customer Account Service Health:" -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8082/actuator/health"
    Write-Host "✅ Status: $($response.status)" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed: $_" -ForegroundColor Red
}

Write-Host "`n=== Services are accessible at ===" -ForegroundColor Green
Write-Host "Account Opening: http://localhost:8081" -ForegroundColor White
Write-Host "Customer Account: http://localhost:8082" -ForegroundColor White

Write-Host "`nPress Enter to stop port forwarding and exit..." -ForegroundColor Yellow
Read-Host

# Cleanup
Stop-Job -Job $job1,$job2
Remove-Job -Job $job1,$job2
Write-Host "Port forwarding stopped." -ForegroundColor Green