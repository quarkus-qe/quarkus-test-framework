#!/bin/bash
# usage:
# ./configure-kind-load-balancer.sh <docker_network>
DOCKER_NETWORK=$1

#Inspect Kube config (only for troubleshooting)
kubectl cluster-info
kubectl get nodes

kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/main/manifests/namespace.yaml
kubectl create secret generic -n metallb-system memberlist --from-literal=secretkey="$(openssl rand -base64 128)"
kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/main/manifests/metallb.yaml
GET_METALLB_PODS="kubectl get pods -n metallb-system"
echo "Waiting for the Metallb Controller"
timeout 240s bash -c "until $GET_METALLB_PODS | grep controller | grep Running; do $GET_METALLB_PODS; kubectl -n metallb-system get events; sleep 10; done"
echo "Waiting for the Metallb Speaker"
timeout 240s bash -c "until $GET_METALLB_PODS | grep speaker | grep Running; do $GET_METALLB_PODS; sleep 10; done"

# Inspect docker network (only for troubleshooting)
docker network inspect -f '{{.IPAM.Config}}' $DOCKER_NETWORK

# Get the IPs ranges
NETWORK_IPS=($(docker network inspect -f '{{.IPAM.Config}}' $DOCKER_NETWORK | grep -oP '[\d]+\.'))
cat <<-EOF | kubectl apply -f -
apiVersion: v1
kind: ConfigMap
metadata:
  namespace: metallb-system
  name: config
data:
  config: |
    address-pools:
    - name: default
      protocol: layer2
      addresses:
      - ${NETWORK_IPS[0]%?}.${NETWORK_IPS[1]%?}.255.200-${NETWORK_IPS[0]%?}.${NETWORK_IPS[1]%?}.255.250
EOF
