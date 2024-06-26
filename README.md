## Basic Demo for Red Hat Service Interconnect (based on Skupper)

Red Hat Service Interconnect empowers developers to more seamlessly create trusted connections between services, applications and workloads across environments without requiring complex network reconfigurations or elevated security privileges.

This enables the organization to implements truely open hybrid cloud implementations where they can extend their application services across different cloud/on-premise and utilize different services from different cloud vendors.

<img width="796" alt="Screenshot 2024-06-26 at 10 02 19 AM" src="https://github.com/osa-ora/ocp-skupper-demo/assets/18471537/4b5d4463-96cc-4b13-aadc-eaf240aaf4fe">


We will explore 2 different basic scenarios where you can extend them and implement more robust architecture for your applications.

Note: You'll need the following to execute the scenarios:
- Access to 2 different OpenShift clusters.
- OpenShift command line installed (i.e. oc)
- Skupper command line installed which can be installed by running:
  ```
    curl https://skupper.io/install.sh | sh
  ```

### Basic Scenario 1: Extend Services Across 2 Sites: Connect to a remote service
---

In this scenario we will use a local backend loyalty service v1 or switch to a remote backend loyalty service v2 (in another OCP cluster). Both will be running and the front end service/application can select which one to connect to based on the environment variable "END_POINT": 
<img width="1045" alt="Screenshot 2024-06-25 at 12 38 54 PM" src="https://github.com/osa-ora/ocp-demos/assets/18471537/ab6906be-6392-4daf-a4d3-bccc4dac8a14">

Use case diagram: 

<img width="572" alt="Screenshot 2024-06-24 at 6 42 55 PM" src="https://github.com/osa-ora/ocp-demos/assets/18471537/2a85e212-42ed-4c90-83c0-7e934d01ec59">

** Steps:

Login to first/local cluster and deploy the front-end app, and the backend loyalty application v1.
```
oc login //first cluster 
oc new-project dev-local
oc new-app --name=loyalty-v1 java~https://github.com/osa-ora/service-interconnect-demo --context-dir=backend -e APP_VERSION=LOCAL1
oc new-app --name=front-app java~https://github.com/osa-ora/service-interconnect-demo --context-dir=frontend -e END_POINT=http://loyalty-v1:8080/loyalty/balance/
oc label deployment/loyalty-v1 app.kubernetes.io/part-of=my-application
oc label deployment/front-app app.kubernetes.io/part-of=my-application
oc expose svc/front-app
```
Initialize Skupper in the first cluster with skupper console enabled.

Create a secret token to use in the link between both sites/clusters.
```
skupper init --enable-console --enable-flow-collector --console-auth unsecured
//skupper init --enable-console --enable-flow-collector --console-auth internal --console-user <username> --console-password <password> 
//skupper init --enable-console --enable-flow-collector --console-auth openshift
skupper token create secret_connect.token
skupper status
```
Login to the remote cluster and deploy backend application loyalty v2.
```
oc login //second cluster 
oc new-project dev-remote
oc new-app --name=loyalty-v2-remote java~https://github.com/osa-ora/service-interconnect-demo --context-dir=backend -e APP_VERSION=REMOTE2
oc label deployment/loyalty-v2 app.kubernetes.io/part-of=my-application
```
Initialize Skupper in the remote cluster, and create a link to the first cluster/site.
```
skupper init
skupper status
skupper link create secret_connect.token --name first-to-second-link
//to delete this link: skupper link delete first-to-second-link
```
Now, both sites are connected.

Expose the backend loyalty service v2 (in remote cluster) to be usable from the first cluster.
```
skupper expose service/loyalty-v2-remote --address loyalty-v2 --port 8080
//you can also use: skupper expose deployment/loyalty-v2-remote --address loyalty-v2  --port 8080
//to delete this service: skupper service delete loyalty-v2
```
Test the application while using loyalty v1 (on the same first cluster) then loyalty v2 (on the remote cluster).
```
curl $(oc get route front-app -o jsonpath='{.spec.host}')/front/test/1999
// outcome: {"Response:":"{\"account\":1999,\"balance\": 3000, \", app-version: LOCAL1}","Welcome":" guest"}%

//switch frontend app to use loyalty version 2 using END_POINT environment variable
oc set env deployment/front-app END_POINT=http://loyalty-v2:8080/loyalty/balance/

curl $(oc get route front-app -o jsonpath='{.spec.host}')/front/test/1999
// outcome: {"Response:":"{\"account\":1999,\"balance\": 3000, \", app-version: REMOTE2}","Welcome":" guest"}
```
To clean everything ..
```
oc login //first cluster
oc project dev-local
//on both clusters
skupper delete
oc delete project dev-lcoal
oc login //second cluster
oc project dev-remote
skupper delete
oc delete project dev-remote
```
Note: We can also expose a service using the annotation, which enables us to use gitops approach, for example: kubectl annotate service backend "skupper.io/address=van-backend"
"skupper.io/port=8080" "skupper.io/proxy=tcp" "skupper.io/target=backend", same for site creation using yaml directly or by the operator.

### Basic Scenario 2: Transparent Service Failover Across 2 Sites : Connect to a local & remote service for HA
---

In this scenario we will use a local loyalty service v1 and automatic failover to a remote loyalty service v1 (in another/remote OCP cluster). We will simulatea failover by scalling the local loyalty service v1 to a zero replica count.

Use case diagram: 

<img width="558" alt="Screenshot 2024-06-24 at 6 43 33 PM" src="https://github.com/osa-ora/ocp-demos/assets/18471537/35acaa76-1c26-4b9d-8a94-41de6b0bbb65">

** Steps:

Login to first/local cluster and deploy the front-end app, and the backend loyalty application v1.

```
oc login //first cluster 
oc new-project dev-local
oc new-app --name=loyalty-local-v1 java~https://github.com/osa-ora/service-interconnect-demo --context-dir=backend -e APP_VERSION=LOCAL1
oc new-app --name=front-app java~https://github.com/osa-ora/service-interconnect-demo --context-dir=frontend -e END_POINT=http://loyalty-v1:8080/loyalty/balance/
oc label deployment/loyalty-v1 app.kubernetes.io/part-of=my-application
oc label deployment/front-app app.kubernetes.io/part-of=my-application
oc expose svc/front-app
```
Initialize Skupper in the first cluster with skupper console enabled.

Create a secret token to use in the link between both sites/clusters.
```
skupper init --enable-console --enable-flow-collector --console-auth unsecured
//skupper init --enable-console --enable-flow-collector --console-auth internal --console-user <username> --console-password <password> 
//skupper init --enable-console --enable-flow-collector --console-auth openshift
skupper token create secret_connect.token
skupper status
```
Login to remote cluster and deploy loyalty v1 of the backend application
```
oc login //second cluster 
oc new-project dev-remote
oc new-app --name=loyalty-remote-v1 java~https://github.com/osa-ora/service-interconnect-demo --context-dir=backend -e APP_VERSION=REMOTE1
oc label deployment/loyalty-v2 app.kubernetes.io/part-of=my-application
```
Initialize Skupper in the remote cluster, and create a link to the first cluster/site.
```
skupper init
skupper status
skupper link create secret_connect.token --name first-to-second-link
//to delete this link: skupper link delete first-to-second-link
```
Now, both sites are connected.

Login to first/local cluster and create skupper service loyalty-v1 and bind it to the local loyalty service v1.
```
oc login //first cluster 
oc project dev-local
skupper service create loyalty-v1 8080 --protocol http
//to delete this service: skupper service delete loyalty-v1
skupper service bind loyalty-v1 service loyalty-local-v1
```
Login to the second cluster and bind the remote backend loyalty service v1 (in remote cluster) to the skupper loyalty-v1 service, so now both local and remote loyalty v1 are wrabbed by skupper service loyalty-v1 and can be used for automatic failover and high availability.
```
oc login //second cluster 
oc new-project dev-remote
skupper service bind loyalty-v1 service loyalty-remote-v1
```
Test the frontend application while connecting to the local backend loyalty service v1 (on the same cluster) then test the failover to the removte backend loyalty service v1 (on the remote cluster) by scalling the local loyalty service v1 to zero replica count.
```
//wait for app deployment completed ... then test it using curl ...
curl $(oc get route front-app -o jsonpath='{.spec.host}')/front/test/1999
// outcome: {"Response:":"{\"account\":1999,\"balance\": 3000, \", app-version: LOCAL1}","Welcome":" guest"}%

//scale replica to zero to simulate a failure in the service..
oc scale deployment/loyalty-local-v1 --replicas=0 -n dev-local

//test the automatic failover ..
curl $(oc get route front-app -o jsonpath='{.spec.host}')/front/test/1999
// outcome: {"Response:":"{\"account\":1999,\"balance\": 3000, \", app-version: REMOTE1}","Welcome":" guest"}

//you can revert this scalling and see how automatic switch back to the local service:
oc scale deployment/loyalty-local-v1 --replicas=1 -n dev-local

curl $(oc get route front-app -o jsonpath='{.spec.host}')/front/test/1999
// outcome: {"Response:":"{\"account\":1999,\"balance\": 3000, \", app-version: LOCAL1}","Welcome":" guest"}%
```
You can open the skupper console as we enabled it during the "skupper init --enable-console", if you execute the following command, you will get the console URL

```
skupper status      
Skupper is enabled for namespace "dev-local". It is not connected to any other sites. It has 1 exposed service.
The site console url is:  https://skupper-dev-local.......
```
Open the console and check the configurations:

This topology shows the different connected sites/clusters:
<img width="1007" alt="Screenshot 2024-06-25 at 12 35 26 PM" src="https://github.com/osa-ora/ocp-demos/assets/18471537/9359da52-0af7-4de6-bc8a-2b182ebdc242">

This one shows the 2 services that we bound to the loyalty-v1 service in both clusters.
<img width="1472" alt="Screenshot 2024-06-25 at 12 35 59 PM" src="https://github.com/osa-ora/ocp-demos/assets/18471537/238bfdf6-70bb-423a-bf14-6070b4fd447c">

To clean everything ..
```
oc login //first cluster
oc project dev-local
//on both clusters
skupper delete
oc delete project dev-lcoal
oc login //second cluster
oc project dev-remote
skupper delete
oc delete project dev-remote
```

