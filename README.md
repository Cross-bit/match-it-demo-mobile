# Frontend for match-it android application

Contains Android studio project for match-it frontend.
Application can be build using provided gradle files (e.g. in Android studio or gradlew command).

## Important notes for testing
Since current infrastructure for server side communication relies on 
FCM ([firebase cloud messaging](https://firebase.google.com/products/cloud-messaging)),
including e.g. session invites, it is required that users are in android loggend into the their google account.

Currently only movies matching is supported.

Links:

[showcase](https://drive.google.com/file/d/13WdtzGqauqGs-amZH_CfoRhOnkDSGJc6/view?usp=drive_link)

[Repository](https://drive.google.com/file/d/13WdtzGqauqGs-amZH_CfoRhOnkDSGJc6/view?usp=drive_link) 
containing all related topics.

## Build and run
For simplicity it is recommended to build and run project using IDE, e.g. android studio.


Before running the app, there needs to be local.properties file created, correctly set build variables.

TODO: add link to these variables. For now (to this point 17.6. 2024) it should be enough to set:

```
# Local API dev settings

DEV_SERVER_IP=192.168.0.4

ACCOUNTS_API_URL=http://192.168.0.4:7050/api/v1/

FRIENDSHIPS_API_URL=http://192.168.0.4:9050/api/v1/

DATA_API_URL=http://192.168.0.4:8050/api/v1/
```





