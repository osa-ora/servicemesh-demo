## Basic Demo for Red Hat Service Mesh (based on Istio)

Red Hat Service Interconnect provides a uniform way to connect, manage, and observe microservices-based applications. It provides behavioral insight into—and control of—the networked microservices in your service mesh.

Based on the open source Istio project, Red Hat OpenShift Service Mesh provides additional functionality with the inclusion of other open source projects like Kiali (Istio console) and Jaeger (distributed tracing), which supports collaboration with leading members of the Istio community.

In this tutorial, we will demostrate the standard bookinfo sample and then build our own sample project step by step.

Note: You'll need the following to execute the scenarios:
- Access to an OpenShift cluster.
- OpenShift command line installed (i.e. oc or Webterminal operator)
- ServiceMesh Installed using the operators.
- For 2nd Scenario, Builds for Red Hat OpenShift Operator is installed.
  
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

//Get the Gateway route: (Note: change the namespace to the istio control-plane namespace
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

In this scenario we will use our custom resources to build and deploy our application to utilize the service mesh, we have a front app that call backend service (we will have 2 versions of that backend service).

** Steps:

Get the ServiceMesh version from the operator.
Login to OCP cluster and execute the following commands.

```
oc login ... //to the cluster
//create a new project
oc new-project dev

//build container images
//create an openshift project
oc new-project dev

//In Istio Service Mesh Member Rolls add the dev project to the members section ...
Add :
spec:
  members:
  - dev

//Make Sure the Builds for Red Hat OpenShift Operator is installed.

//create shipwright build for our front-app application in the 'dev' project
shp build create front-app-build --strategy-name="source-to-image" --source-url="https://github.com/osa-ora/servicemesh-demo" --source-context-dir="frontend" --output-image="image-registry.openshift-image-registry.svc:5000/dev/front-app" --builder-image="image-registry.openshift-image-registry.svc:5000/openshift/java:openjdk-17-ubi8"

shp build create backend-v1-build --strategy-name="source-to-image" --source-url="https://github.com/osa-ora/servicemesh-demo" --source-context-dir="backend" --output-image="image-registry.openshift-image-registry.svc:5000/dev/backend-v1" --builder-image="image-registry.openshift-image-registry.svc:5000/openshift/java:openjdk-17-ubi8"

shp build create backend-v2-build --strategy-name="source-to-image" --source-url="https://github.com/osa-ora/servicemesh-demo" --source-context-dir="backend" --output-image="image-registry.openshift-image-registry.svc:5000/dev/backend-v2" --builder-image="image-registry.openshift-image-registry.svc:5000/openshift/java:openjdk-17-ubi8"


//start the build and follow the output
shp build run front-app-build --follow
shp build run backend-v1-build --follow
shp build run backend-v2-build --follow

//Deploy the application components (Deployment object)
oc apply -f https://raw.githubusercontent.com/osa-ora/servicemesh-demo/refs/heads/main/gitops/front-deployment.yaml -n dev
oc apply -f https://raw.githubusercontent.com/osa-ora/servicemesh-demo/refs/heads/main/gitops/backend-deployment-v1.yaml -n dev
oc apply -f https://raw.githubusercontent.com/osa-ora/servicemesh-demo/refs/heads/main/gitops/backend-deployment-v2.yaml -n dev

//Create Service Object
oc expose deployment/front-app --port=8080 --target-port=8080 --name=front-app -n dev
oc apply -f https://raw.githubusercontent.com/osa-ora/servicemesh-demo/refs/heads/main/gitops/loyalty-service.yaml -n dev
//oc expose deployment/loyalty-v1 --port=8080 --target-port=8080 --name=loyalty-v1 -n dev
//oc expose deployment/loyalty-v2 --port=8080 --target-port=8080 --name=loyalty-v2 -n dev

//Create Application Gateway
oc apply -f https://raw.githubusercontent.com/osa-ora/servicemesh-demo/refs/heads/main/gitops/app-gateway.yaml -n dev

//Create Virtual Services
oc apply -f https://github.com/osa-ora/servicemesh-demo/blob/main/gitops/front-virtual-service.yaml -n dev
//oc apply -f https://raw.githubusercontent.com/osa-ora/servicemesh-demo/refs/heads/main/gitops/backend-virtual-service.yaml -n dev

//Create Distination rules
oc apply -f https://raw.githubusercontent.com/osa-ora/servicemesh-demo/refs/heads/main/gitops/loyalty-dest-rule.yaml -n dev

//Get the Gateway route: (Note: change the namespace to the istio control-plane namespace
export GATEWAY_URL=$(oc -n user1-istio get route istio-ingressgateway -o jsonpath='{.spec.host}')

//Test the front application
curl "http://$GATEWAY_URL/front/test/1999"

```

<img width="764" alt="Screenshot 2024-10-29 at 9 46 00 PM" src="https://github.com/user-attachments/assets/2a3adcab-2d64-4d8e-81de-fe5525df4609">



