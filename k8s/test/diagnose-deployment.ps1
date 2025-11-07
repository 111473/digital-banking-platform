# Diagnose Banking Platform Deployment
# Run this to check what's happening in your cluster

Write-Host "===== Banking Platform Diagnostics =====" -ForegroundColor Green

# Step 1: Check Pod Status
Write-Host "`n[1/6] Pod Status..." -ForegroundColor Cyan
kubectl get pods -n banking -o wide

# Step 2: Check Services
Write-Host "`n[2/6] Service Status..." -ForegroundColor Cyan
kubectl get svc -n banking

# Step 3: Check Recent Logs from Each Service
Write-Host "`n[3/6] Recent Logs..." -ForegroundColor Cyan

Write-Host "`n--- Account Opening Service (last 20 lines) ---" -ForegroundColor Yellow
kubectl logs -n banking deployment/account-opening-service --tail=20

Write-Host "`n--- Customer Account Service (last 20 lines) ---" -ForegroundColor Yellow
kubectl logs -n banking deployment/customer-account-service --tail=20

Write-Host "`n--- Bank Account Service (last 20 lines) ---" -ForegroundColor Yellow
kubectl logs -n banking deployment/bank-account-service --tail=20

# Step 4: Check Kafka
Write-Host "`n[4/6] Kafka Topics..." -ForegroundColor Cyan
$kafkaPod = kubectl get pods -n banking -l app=kafka -o jsonpath='{.items[0].metadata.name}'
if ($kafkaPod) {
    Write-Host "Listing Kafka topics:" -ForegroundColor Yellow
    kubectl exec -n banking $kafkaPod -- kafka-topics --list --bootstrap-server localhost:9092
} else {
    Write-Host "  ✗ Kafka pod not found" -ForegroundColor Red
}

# Step 5: Check Database
Write-Host "`n[5/6] Database Status..." -ForegroundColor Cyan
$postgresPod = kubectl get pods -n banking -l app=postgres -o jsonpath='{.items[0].metadata.name}'
if ($postgresPod) {
    Write-Host "Listing databases:" -ForegroundColor Yellow
    kubectl exec -n banking $postgresPod -- psql -U postgres -c "\l"

    Write-Host "`nChecking bank_account_db tables:" -ForegroundColor Yellow
    kubectl exec -n banking $postgresPod -- psql -U postgres -d bank_account_db -c "\dt"

    Write-Host "`nChecking bank accounts count:" -ForegroundColor Yellow
    kubectl exec -n banking $postgresPod -- psql -U postgres -d bank_account_db -c "SELECT COUNT(*) FROM bank_account;"
} else {
    Write-Host "  ✗ PostgreSQL pod not found" -ForegroundColor Red
}

# Step 6: Check Recent Events
Write-Host "`n[6/6] Recent Kubernetes Events..." -ForegroundColor Cyan
kubectl get events -n banking --sort-by='.lastTimestamp' | Select-Object -Last 10

Write-Host "`n===== Diagnostics Complete =====" -ForegroundColor Green

Write-Host "`nQuick Actions:" -ForegroundColor Cyan
Write-Host "  1. Test account creation:" -ForegroundColor White
Write-Host "     curl -X POST http://localhost:8081/api/applications -H 'Content-Type: application/json' -d '{\"customerName\":\"Test User\",\"email\":\"test@example.com\",\"accountType\":\"SAVINGS\"}'" -ForegroundColor Gray
Write-Host "`n  2. Follow logs in real-time:" -ForegroundColor White
Write-Host "     kubectl logs -f deployment/bank-account-service -n banking" -ForegroundColor Gray
Write-Host "`n  3. Check if Kafka events are being produced:" -ForegroundColor White
Write-Host "     kubectl exec -n banking $kafkaPod -- kafka-console-consumer --bootstrap-server localhost:9092 --topic application-approved --from-beginning --max-messages 5" -ForegroundColor Gray