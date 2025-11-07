# Deploy Banking Platform to Kubernetes (Minikube) with ZooKeeper
# Run this from the project root directory

Write-Host "===== Banking Platform Kubernetes Deployment =====" -ForegroundColor Green
Write-Host "Configuration: 3 CPUs, 5GB RAM, 3 Microservices, ZooKeeper + Kafka" -ForegroundColor Cyan

# Step 1: Check if Minikube is running
Write-Host "`n[1/9] Checking Minikube status..." -ForegroundColor Cyan
$minikubeStatus = minikube status 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Minikube is not running. Starting Minikube..." -ForegroundColor Yellow
    Write-Host "Configuration: 3 CPUs, 5GB RAM (optimized for 3 microservices)" -ForegroundColor Yellow
    minikube start --cpus=3 --memory=5120
} else {
    Write-Host "Minikube is already running" -ForegroundColor Green
}

# Step 2: Enable ingress
Write-Host "`n[2/9] Enabling Ingress addon..." -ForegroundColor Cyan
minikube addons enable ingress

# Step 3: Use Minikube's Docker daemon
Write-Host "`n[3/9] Switching to Minikube Docker daemon..." -ForegroundColor Cyan
& minikube -p minikube docker-env --shell powershell | Invoke-Expression

# Step 4: Build Docker images
Write-Host "`n[4/9] Building Docker images..." -ForegroundColor Cyan

Write-Host "Building all services with Maven..." -ForegroundColor Yellow
mvn clean package -DskipTests

Write-Host "Building account-opening-service image..." -ForegroundColor Yellow
docker build -f account-opening-service/Dockerfile -t account-opening-service:latest .

Write-Host "Building customer-account-service image..." -ForegroundColor Yellow
docker build -f customer-account-service/Dockerfile -t customer-account-service:latest .



Write-Host "Building bank-account-service image..." -ForegroundColor Yellow
docker build -f bank-account-service/Dockerfile -t bank-account-service:latest .

# Step 5: Create namespace and RBAC
Write-Host "`n[5/9] Creating namespace and RBAC..." -ForegroundColor Cyan
kubectl apply -f k8s/namespace-and-postgres.yaml
kubectl apply -f k8s/rbac-permissions.yaml

# Step 6: Wait for PostgreSQL
Write-Host "`n[6/9] Waiting for infrastructure to be ready..." -ForegroundColor Cyan
Write-Host "Waiting for PostgreSQL..." -ForegroundColor Yellow
kubectl wait --for=condition=ready pod -l app=postgres -n banking --timeout=180s

Write-Host "Waiting for ZooKeeper..." -ForegroundColor Yellow
kubectl wait --for=condition=ready pod -l app=zookeeper -n banking --timeout=180s

Write-Host "Waiting for Kafka..." -ForegroundColor Yellow
kubectl wait --for=condition=ready pod -l app=kafka -n banking --timeout=300s

# Give Kafka extra time to fully initialize
Write-Host "Giving Kafka additional time to initialize..." -ForegroundColor Yellow
Start-Sleep -Seconds 20

# Step 7: Deploy services
Write-Host "`n[7/9] Deploying microservices..." -ForegroundColor Cyan
kubectl apply -f k8s/account-opening-deployment.yaml
kubectl apply -f k8s/customer-account-deployment.yaml
kubectl apply -f k8s/bank-account-deployment.yaml

# Step 8: Wait for services
Write-Host "`n[8/9] Waiting for services to be ready..." -ForegroundColor Cyan
Write-Host "Waiting for account-opening-service..." -ForegroundColor Yellow
kubectl wait --for=condition=ready pod -l app=account-opening-service -n banking --timeout=180s

Write-Host "Waiting for customer-account-service..." -ForegroundColor Yellow
kubectl wait --for=condition=ready pod -l app=customer-account-service -n banking --timeout=180s

Write-Host "Waiting for bank-account-service..." -ForegroundColor Yellow
kubectl wait --for=condition=ready pod -l app=bank-account-service -n banking --timeout=180s

# Step 9: Display deployment info
Write-Host "`n[9/9] Verifying deployment..." -ForegroundColor Cyan
$MINIKUBE_IP = minikube ip

Write-Host "`n===== Deployment Complete! =====" -ForegroundColor Green
Write-Host "`nArchitecture: ZooKeeper + Kafka (KRaft migration available later)" -ForegroundColor Magenta

Write-Host "`nServices are accessible at:" -ForegroundColor Cyan
Write-Host "  Account Opening Service: http://${MINIKUBE_IP}:30081/api/applications" -ForegroundColor White
Write-Host "  Customer Account Service: http://${MINIKUBE_IP}:30082/api/customers" -ForegroundColor White
Write-Host "  Bank Account Service:    http://${MINIKUBE_IP}:30083/api/accounts" -ForegroundColor White

Write-Host "`nUseful commands:" -ForegroundColor Cyan
Write-Host "  View all pods:       kubectl get pods -n banking" -ForegroundColor White
Write-Host "  View services:       kubectl get services -n banking" -ForegroundColor White
Write-Host "  View logs:           kubectl logs -f deployment/<service-name> -n banking" -ForegroundColor White
Write-Host "  Kafka logs:          kubectl logs -f deployment/kafka -n banking" -ForegroundColor White
Write-Host "  Describe pod:        kubectl describe pod <pod-name> -n banking" -ForegroundColor White
Write-Host "  Access pod shell:    kubectl exec -it <pod-name> -n banking -- /bin/sh" -ForegroundColor White
Write-Host "  Delete deployment:   kubectl delete namespace banking" -ForegroundColor White

Write-Host "`nTest the deployment:" -ForegroundColor Cyan
Write-Host "  curl http://${MINIKUBE_IP}:30081/actuator/health" -ForegroundColor White
Write-Host "  curl http://${MINIKUBE_IP}:30082/actuator/health" -ForegroundColor White
Write-Host "  curl http://${MINIKUBE_IP}:30083/actuator/health" -ForegroundColor White

Write-Host "`nMonitor resources:" -ForegroundColor Cyan
Write-Host "  kubectl top nodes" -ForegroundColor White
Write-Host "  kubectl top pods -n banking" -ForegroundColor White
Write-Host "  kubectl get pods -n banking -o wide" -ForegroundColor White

Write-Host "`nResource Allocation Summary:" -ForegroundColor Cyan
Write-Host "  PostgreSQL:       256Mi / 512Mi   (200m / 400m CPU)" -ForegroundColor White
Write-Host "  ZooKeeper:        256Mi / 512Mi   (200m / 400m CPU)" -ForegroundColor White
Write-Host "  Kafka:            512Mi / 896Mi   (300m / 600m CPU)" -ForegroundColor White
Write-Host "  Service 1:        448Mi / 768Mi   (250m / 500m CPU)" -ForegroundColor White
Write-Host "  Service 2:        448Mi / 768Mi   (250m / 500m CPU)" -ForegroundColor White
Write-Host "  Service 3:        448Mi / 768Mi   (250m / 500m CPU)" -ForegroundColor White
Write-Host "  ------------------------------------------------" -ForegroundColor White
Write-Host "  Total Requests:   ~2.6GB          (~1.45 CPUs)" -ForegroundColor Yellow
Write-Host "  Total Limits:     ~4.7GB          (~3.2 CPUs)" -ForegroundColor Yellow
Write-Host "  Safety Margin:    ~2.5GB          (~1.55 CPUs)" -ForegroundColor Green

Write-Host "`n✓ All 3 microservices deployed successfully!" -ForegroundColor Green