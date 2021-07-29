#!/bin/bash
# usage:
# ./configure-kind-load-balancer.sh <docker_network>
DOCKER_NETWORK=$1
kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/master/manifests/namespace.yaml
kubectl create secret generic -n metallb-system memberlist --from-literal=secretkey="$(openssl rand -base64 128)"
kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/master/manifests/metallb.yaml

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
