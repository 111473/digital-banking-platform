#!/bin/bash

echo "===== Starting Minikube Deployment ====="

# Start Minikube if not running
echo "Starting Minikube..."
minikube start

# Enable ingress addon
echo "Enabling Ingress addon..."
minikube addons enable ingress

# Use Minikube's Docker daemon
echo "Switching to Minikube Docker daemon..."
eval $(minikube docker-env)

# Build all services
echo "Building Maven projects..."
cd ..
mvn clean package -DskipTests

# Build Docker images
echo "Building Docker images..."

cd account-opening-service
docker build -t account-opening-service:latest .
cd ..

cd customer-account-service
docker build -t customer-account-service:latest .
cd ..

cd bank-account-service
docker build -t bank-account-service:latest .
cd ..

cd transaction-service
docker build -t transaction-service:latest .
cd ..

# Deploy to Kubernetes
echo "Deploying to Kubernetes..."
cd k8s

kubectl apply -f namespace-and-postgres.yaml
echo "Waiting for PostgreSQL to be ready..."
kubectl wait --for=condition=ready pod -l app=postgres -n banking --timeout=120s

kubectl apply -f rbac-permissions.yaml
kubectl apply -f account-opening-deployment.yaml
kubectl apply -f customer-account-deployment.yaml
kubectl apply -f bank-account-deployment.yaml
kubectl apply -f transaction-deployment.yaml
kubectl apply -f ingress-gateway.yaml

echo "Waiting for all services to be ready..."
kubectl wait --for=condition=ready pod -l app=account-opening-service -n banking --timeout=120s
kubectl wait --for=condition=ready pod -l app=customer-account-service -n banking --timeout=120s
kubectl wait --for=condition=ready pod -l app=bank-account-service -n banking --timeout=120s
kubectl wait --for=condition=ready pod -l app=transaction-service -n banking --timeout=120s

echo ""
echo "===== Deployment Complete ====="
echo ""
echo "To view pods:"
echo "  kubectl get pods -n banking"
echo ""
echo "To view services:"
echo "  kubectl get services -n banking"
echo ""
echo "To access the services, add this to /etc/hosts:"
echo "  $(minikube ip) banking.local"
echo ""
echo "Then access services at:"
echo "  http://banking.local/account-opening"
echo "  http://banking.local/customer-account"
echo "  http://banking.local/bank-account"
echo "  http://banking.local/transaction"
echo ""
echo "To view logs of a service:"
echo "  kubectl logs -f deployment/account-opening-service -n banking"
echo ""