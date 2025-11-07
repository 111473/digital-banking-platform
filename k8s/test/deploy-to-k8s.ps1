# Deploy Banking Platform to Kubernetes (Minikube)
# Run this from the project root directory

Write-Host "===== Banking Platform Kubernetes Deployment =====" -ForegroundColor Green

# Step 1: Check if Minikube is running
Write-Host "`n[1/8] Checking Minikube status..." -ForegroundColor Cyan
$minikubeStatus = minikube status 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Minikube is not running. Starting Minikube..." -ForegroundColor Yellow
    minikube start --cpus=4 --memory=7168
} else {
    Write-Host "Minikube is already running" -ForegroundColor Green
}

# Step 2: Enable ingress
Write-Host "`n[2/8] Enabling Ingress addon..." -ForegroundColor Cyan
minikube addons enable ingress

# Step 3: Use Minikube's Docker daemon
Write-Host "`n[3/8] Switching to Minikube Docker daemon..." -ForegroundColor Cyan
& minikube -p minikube docker-env --shell powershell | Invoke-Expression

# Step 4: Build Docker images
Write-Host "`n[4/8] Building Docker images..." -ForegroundColor Cyan

Write-Host "Building all services with Maven..." -ForegroundColor Yellow
mvn clean package -DskipTests

Write-Host "Building account-opening-service image..." -ForegroundColor Yellow
docker build -f account-opening-service/Dockerfile -t account-opening-service:latest .

Write-Host "Building customer-account-service image..." -ForegroundColor Yellow
docker build -f customer-account-service/Dockerfile -t customer-account-service:latest .

Write-Host "Building customer-account-service image..." -ForegroundColor Yellow
docker build -f branch-service/Dockerfile -t branch-service:latest .

Write-Host "Building bank-account-service image..." -ForegroundColor Yellow
docker build -f bank-account-service/Dockerfile -t bank-account-service:latest .

# Step 5: Create namespace and RBAC
Write-Host "`n[5/8] Creating namespace and RBAC..." -ForegroundColor Cyan
kubectl apply -f k8s/namespace-and-postgres.yaml
kubectl apply -f k8s/rbac-permissions.yaml

# Step 6: Wait for PostgreSQL and Kafka
Write-Host "`n[6/8] Waiting for PostgreSQL and Kafka to be ready..." -ForegroundColor Cyan
kubectl wait --for=condition=ready pod -l app=postgres -n banking --timeout=180s
kubectl wait --for=condition=ready pod -l app=kafka -n banking --timeout=180s

# Step 7: Deploy services
Write-Host "`n[7/8] Deploying microservices..." -ForegroundColor Cyan
kubectl apply -f k8s/account-opening-deployment.yaml
kubectl apply -f k8s/customer-account-deployment.yaml
kubectl apply -f k8s/branch-deployment.yaml
kubectl apply -f k8s/bank-account-deployment.yaml

# Step 8: Wait for services
Write-Host "`n[8/8] Waiting for services to be ready..." -ForegroundColor Cyan
kubectl wait --for=condition=ready pod -l app=account-opening-service -n banking --timeout=180s
kubectl wait --for=condition=ready pod -l app=customer-account-service -n banking --timeout=180s
kubectl wait --for=condition=ready pod -l app=bank-account-service -n banking --timeout=180s

# Get Minikube IP
$MINIKUBE_IP = minikube ip

Write-Host "`n===== Deployment Complete! =====" -ForegroundColor Green
Write-Host "`nServices are accessible at:" -ForegroundColor Cyan
Write-Host "  Account Opening Service: http://${MINIKUBE_IP}:30081/api/applications" -ForegroundColor White
Write-Host "  Customer Account Service: http://${MINIKUBE_IP}:30082/api/customers" -ForegroundColor White
Write-Host "  Branch Service: http://${MINIKUBE_IP}:30083/api/customers" -ForegroundColor White
Write-Host "  Bank Account Service: http://${MINIKUBE_IP}:30084/api/bank-accounts" -ForegroundColor White

Write-Host "`nUseful commands:" -ForegroundColor Cyan
Write-Host "  View all pods:       kubectl get pods -n banking" -ForegroundColor White
Write-Host "  View services:       kubectl get services -n banking" -ForegroundColor White
Write-Host "  View logs:           kubectl logs -f deployment/account-opening-service -n banking" -ForegroundColor White
Write-Host "  Describe pod:        kubectl describe pod <pod-name> -n banking" -ForegroundColor White
Write-Host "  Access pod shell:    kubectl exec -it <pod-name> -n banking -- /bin/sh" -ForegroundColor White
Write-Host "  Delete deployment:   kubectl delete namespace banking" -ForegroundColor White

Write-Host "`nTest the deployment:" -ForegroundColor Cyan
Write-Host "  curl http://${MINIKUBE_IP}:30081/actuator/health" -ForegroundColor White
Write-Host "  curl http://${MINIKUBE_IP}:30082/actuator/health" -ForegroundColor White
Write-Host "  curl http://${MINIKUBE_IP}:30083/actuator/health" -ForegroundColor White
Write-Host "  curl http://${MINIKUBE_IP}:30084/actuator/health" -ForegroundColor White
