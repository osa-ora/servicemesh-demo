## Basic Demo for Red Hat Service Mesh (based on Istio)

Red Hat Service Interconnect provides a uniform way to connect, manage, and observe microservices-based applications. It provides behavioral insight into—and control of—the networked microservices in your service mesh.

Based on the open source Istio project, Red Hat OpenShift Service Mesh provides additional functionality with the inclusion of other open source projects like Kiali (Istio console) and Jaeger (distributed tracing), which supports collaboration with leading members of the Istio community.

In this tutorial, we will demostrate the standard bookinfo sample and then build our own sample project step by step.

Note: You'll need the following to execute the scenarios:
- Access to an OpenShift cluster.
- OpenShift command line installed (i.e. oc or Webterminal operator)
- ServiceMesh Installed using the operators.

### Basic Scenario 1: Bookinfo Project
---

In this scenario we will use the standard bookinfo sample project.

** Steps:

Get the ServiceMesh version from the operator.
Login to OCP cluster and execute the following commands.

```
oc login ... //to the cluster
//create a new project
oc new-project bookinfo
export MESH_VERSION=2.4.5
//In Istio Service Mesh Member Rolls add the bookinfo project to the members section ...
Add :
spec:
  members:
  - bookinfo

//deploy the application
oc apply -n bookinfo -f https://raw.githubusercontent.com/Maistra/istio/maistra-$MESH_VERSION/samples/bookinfo/platform/kube/bookinfo.yaml

//Create application gateway
oc apply -n bookinfo -f https://raw.githubusercontent.com/Maistra/istio/maistra-$MESH_VERSION/samples/bookinfo/networking/bookinfo-gateway.yaml

//Get the Gateway route:
export GATEWAY_URL=$(oc -n istio-system get route istio-ingressgateway -o jsonpath='{.spec.host}')

//Define Virtual Service and Destination rules:
//If you did not enable mutual TLS:
oc apply -n bookinfo -f https://raw.githubusercontent.com/Maistra/istio/maistra-$MESH_VERSION/samples/bookinfo/networking/destination-rule-all.yaml 
//If you enabled mutual TLS:
oc apply -n bookinfo -f https://raw.githubusercontent.com/Maistra/istio/maistra-$MESH_VERSION/samples/bookinfo/networking/destination-rule-all-mtls.yaml 

//test the application
echo "http://$GATEWAY_URL/productpage"
//Use this URL and refresh the page to see the different reviews versions are being utilized (red stars, black starts and without any stars)
```


### Basic Scenario 2: Custom Project
---

In this scenario we will use our custom resources to build and deploy our application to utilize the service mesh.

** Steps:

Get the ServiceMesh version from the operator.
Login to OCP cluster and execute the following commands.

```
oc login ... //to the cluster
//create a new project
oc new-project dev
```


