Demo For Service Mesh (TBC)


Traditional Way of Deploying Application and Switch Version ...
```
oc new-project dev
oc new-app --name=loyalty-v1 java~https://github.com/osa-ora/service-mesh-demo --context-dir=backend -e APP_VERSION=1.2
oc new-app --name=loyalty-v2 java~https://github.com/osa-ora/service-mesh-demo --context-dir=backend -e APP_VERSION=2.1
oc new-app --name=front-app java~https://github.com/osa-ora/service-mesh-demo --context-dir=frontend -e END_POINT=http://loyalty-v1:8080/loyalty/balance/
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

