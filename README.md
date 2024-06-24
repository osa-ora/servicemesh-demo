# Demo For Service Mesh (TBC)


Traditional Way of Deploying Application and Switch Version ... (To be completed)
```
oc new-project dev
oc new-app --name=loyalty-v1 java~https://github.com/osa-ora/ocp-demos--context-dir=backend -e APP_VERSION=1.2
oc new-app --name=loyalty-v2 java~https://github.com/osa-ora/ocp-demos --context-dir=backend -e APP_VERSION=2.1
oc new-app --name=front-app java~https://github.com/osa-ora/ocp-demos --context-dir=frontend -e END_POINT=http://loyalty-v1:8080/loyalty/balance/
oc label deployment/loyalty-v2 app.kubernetes.io/part-of=my-application
oc label deployment/loyalty-v1 app.kubernetes.io/part-of=my-application
oc label deployment/front-app app.kubernetes.io/part-of=my-application
oc expose svc/front-app
//wait for app deployment completed ... then test it using curl ...
curl $(oc get route front-app -o jsonpath='{.spec.host}')/front/test/1999
// outcome: {"Response:":"{\"account\":1999,\"balance\": 3000, \", app-version: 1.2}","Welcome":" guest"}%
oc set env deployment/front-app END_POINT=http://loyalty-v2:8080/loyalty/balance/
//wait for the new version deployment
curl $(oc get route front-app -o jsonpath='{.spec.host}')/front/test/1999
// outcome: {"Response:":"{\"account\":1999,\"balance\": 3000, \", app-version: 2.1}","Welcome":" guest"}
```

# Demo for Skupper (TBC)

## Basic Scenario 1: Connect to a remote service

In this scenario we will use a local service v1 or switch to a remote service v2 (in another OCP cluster). Both will be running and the front end service can connect to the selected configured service.

** Steps:

Login to first first or local cluster and deploy front-end app, backend application v1.
```
oc login //first cluster 
oc new-project dev-local
oc new-app --name=loyalty-v1 java~https://github.com/osa-ora/ocp-demos --context-dir=backend -e APP_VERSION=LOCAL1
oc new-app --name=front-app java~https://github.com/osa-ora/ocp-demos --context-dir=frontend -e END_POINT=http://loyalty-v1:8080/loyalty/balance/
oc label deployment/loyalty-v1 app.kubernetes.io/part-of=my-application
oc label deployment/front-app app.kubernetes.io/part-of=my-application
oc expose svc/front-app
```
Initialize Skupper in the first cluster.
```
skupper init --enable-console --enable-flow-collector --console-auth unsecured
//skupper init --enable-console --enable-flow-collector --console-auth internal --console-user <username> --console-password <password> 
//skupper init --enable-console --enable-flow-collector --console-auth openshift
skupper token create secret_connect.token
skupper status
```
Login to remote cluster and deploy v2 of the backend application
```
oc login //second cluster 
oc new-project dev-remote
//oc new-app --name=loyalty-v2 java~https://github.com/osa-ora/ocp-demos --context-dir=backend -e APP_VERSION=REMOTE2
oc new-app --name=loyalty-v2-remote java~https://github.com/osa-ora/ocp-demos --context-dir=backend -e APP_VERSION=REMOTE2
oc label deployment/loyalty-v2 app.kubernetes.io/part-of=my-application
```
Initialize Skupper in the remote cluster, create a link to the first cluster, and expose the backend service (in remote cluster) to be usable from the first cluster.
```
skupper init
skupper status
skupper link create secret_connect.token --name first-to-second-link
//to delete this link: skupper link delete first-to-second-link
skupper expose service/loyalty-v2-remote --address loyalty-v2 --port 8080
//skupper expose deployment/loyalty-v2-remote --address loyalty-v2  --port 8080
//to delete this service: skupper service delete loyalty-v2
```
Test the application while using v1 (on the same cluster) then v2 (on the remove cluster).
```
curl $(oc get route front-app -o jsonpath='{.spec.host}')/front/test/1999
// outcome: {"Response:":"{\"account\":1999,\"balance\": 3000, \", app-version: LOCAL1}","Welcome":" guest"}%
oc set env deployment/front-app END_POINT=http://loyalty-v2:8080/loyalty/balance/
//wait for the new version deployment
curl $(oc get route front-app -o jsonpath='{.spec.host}')/front/test/1999
// outcome: {"Response:":"{\"account\":1999,\"balance\": 3000, \", app-version: REMOTE2}","Welcome":" guest"}
//end of demo
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

## Basic Scenario 2: Connect to local & remote service.

In this scenario we will use a local service v1 and automatic failover to a remote service v1 (in another OCP cluster). We will simulate this with scale the local service to zero replica.

** Steps:

Login to first first or local cluster and deploy front-end app, backend application v1.

```
oc login //first cluster 
oc new-project dev-local
oc new-app --name=loyalty-local-v1 java~https://github.com/osa-ora/ocp-demos --context-dir=backend -e APP_VERSION=LOCAL1
oc new-app --name=front-app java~https://github.com/osa-ora/ocp-demos --context-dir=frontend -e END_POINT=http://loyalty-v1:8080/loyalty/balance/
oc label deployment/loyalty-v1 app.kubernetes.io/part-of=my-application
oc label deployment/front-app app.kubernetes.io/part-of=my-application
oc expose svc/front-app
```
Initialize Skupper in the first cluster.
```
skupper init --enable-console --enable-flow-collector --console-auth unsecured
//skupper init --enable-console --enable-flow-collector --console-auth internal --console-user <username> --console-password <password> 
//skupper init --enable-console --enable-flow-collector --console-auth openshift
skupper token create secret_connect.token
skupper status
```
Login to remote cluster and deploy v1 of the backend application
```
oc login //second cluster 
oc new-project dev-remote
oc new-app --name=loyalty-remote-v1 java~https://github.com/osa-ora/ocp-demos --context-dir=backend -e APP_VERSION=REMOTE1
oc label deployment/loyalty-v2 app.kubernetes.io/part-of=my-application
```
Initialize Skupper in the remote cluster, create a link to the first cluster, and expose the backend service (in remote cluster) to be usable from the first cluster (by creating a skupper service and bind it to the backend service)
```
skupper init
skupper status
skupper link create secret_connect.token --name first-to-second-link
//to delete this link: skupper link delete first-to-second-link
skupper service create loyalty-v1 8080 --protocol http
skupper service bind loyalty-v1 service loyalty-remote-v1
//to delete this service: skupper service delete loyalty-v1
```
Login to first first and bing the service v1 to the same skupper service so now both can be used for failover and high availability.
```
oc login //first cluster 
oc project dev-local
skupper service bind loyalty-v1 service loyalty-local-v1
```
Test the application while connecting to local backend service v1 (on the same cluster) then test the fail over to the removte backend service v1 (on the remove cluster) by scalling the local service to zero replica.
```
//wait for app deployment completed ... then test it using curl ...
curl $(oc get route front-app -o jsonpath='{.spec.host}')/front/test/1999
// outcome: {"Response:":"{\"account\":1999,\"balance\": 3000, \", app-version: LOCAL1}","Welcome":" guest"}%
oc scale deployment/loyalty-local-v1 --replicas=0 -n dev-local
//test the automatic failover ..
curl $(oc get route front-app -o jsonpath='{.spec.host}')/front/test/1999
// outcome: {"Response:":"{\"account\":1999,\"balance\": 3000, \", app-version: REMOTE1}","Welcome":" guest"}
//end of demo
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


