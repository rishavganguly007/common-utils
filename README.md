# Common-Util

a Spring Webclient wrapper and many more to come ... :)

## Usage

### Step 1: Create a JAR file

Create a `.jar` file from the source code.

### Step 2: Add JAR file to your application

Add the JAR file to your application and include it in your `build.gradle` file.

### Step 3: Configure application-local/env file

Add the following configuration to your `application-local/env` file:
```yaml
baseWebClientConfig:
  webClientConfigMap:
    somwWebClientPoolName1:
      memorylimit: 256
      readTimeout: 5000
      writeTimeout: 5000
      connectionTimeout: 100000
      maxIdleLime: 10000
      maxlifeLime: 300000
      pendingAcquireTimeout: 60000
      maxConnections: 100
      maxAttempts: 3
      fixedDelayDuration: 10
      retryErrorcodesstr: "408,502,503"
    somwWebClientPoolName2:
      memorylimit: 256
      readTimeout: 5000
      writeTimeout: 5000
      connectionTimeout: 100000
      maxIdleLime: 10000
      maxlifeLime: 300000
      pendingAcquireTimeout: 60000
      maxConnections: 100
      maxAtAttempts: 3
      fixedDelayDuration: 10
      sslWebClientFlag: true
      retryErrorcodesstr: "408,502,503"
```
### Step 4: Update main application

Update your main application with the following annotation:
```java
@EnableConfiguration(WebClientConfigMap.class)
// SpringBoot Main Application
```
### Step 5: add this in method to call the downstream

```java
ParameterizedTypeReference<SomeObj> parameterizedTypeReference = new ParameterizedTypeReference<SomeObj>() {
};
DelegateParams delegateParams = new DelegateParams(connectionPoolName,
        uri,
        headers,
        requestBody,
        parameterizedTypeReference,
        false);
SomeObj someObj = (SomeObj) baseWebClient.invokeGet(delegateParams);

```
