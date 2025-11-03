# GetLoginToken Example Project

The is a example project for GetLoginToken.

This example **cannot be debugged online**. To debug, you can download it locally and replace the [AK](https://usercenter.console.aliyun.com/#/manage/ak) and parameters before debugging.

## Prerequisites

- Download and extract the code for the required language;


- Obtain your [credentials](https://usercenter.console.aliyun.com/#/manage/ak) from your Alibaba Cloud account and use them to replace the ACCESS_KEY_ID and ACCESS_KEY_SECRET in the downloaded code;

- Execute the build and run commands for the corresponding language.

## Execution Steps

After downloading the code package, and modifying the parameters and AK in the code as needed, you can execute the following steps in the **directory where the code was extracted**:

- *You must use Java 8 or later.*
```sh
mvn clean package
java -jar target/sample-1.0.0-jar-with-dependencies.jar
```
## API Used

-  GetLoginToken: Obtains logon credentials. For more information, you can refer to the [document](https://next.api.aliyun.com/document/ecd/2020-10-02/GetLoginToken)

## API Return Example

*The actual output structure may vary slightly, which is a normal response; the following output values are for reference only and the actual call results shall prevail.*


- JSON format 
```js
{
  "Email": "alice",
  "Secret": "5OCLLKKOJU5HPBX66H3QCTWYI7MH****",
  "RequestId": "1CBAFFAB-B697-4049-A9B1-67E1FC5F****",
  "EndUserId": "alice",
  "LoginToken": "v18101ac6a9e69c66b04a163031680463660b4b216cd758f34b60b9ad6a7c7f7334b83dd8f75eef4209c68f9f1080b****",
  "NextStage": "MFAVerify",
  "QrCodePng": "5OCLLKKOJU5HPBX66H3QCTWY******",
  "Label": "test:wuying",
  "SessionId": "d6ec166d-ab93-4286-bf7f-a18bb929****",
  "Phone": "1381111****",
  "TenantId": 0,
  "KeepAliveToken": "006YwvYMsesWWsDBZnVB+Wq9AvJDVIqOY3YCktvtb7+KxMb3ClnNlV8+l/knhZYrXUmeP06IzkjF+IgcZ3vZKOyMprDyFHjCy1r27FRE/U7+geWCl8iQ+yF8GaCRHfJEkC2+ROs93HkT4tfHxyY1J8W7O7ZQGUC/cdCvm+cCP6FIy73IUuPuVR6PcKYXIpEZPW",
  "Industry": "edu",
  "Props": {
    "key": "{'dingUserName': u'\\u674e\\u66fc', 'role': 'student'}"
  },
  "WindowDisplayMode": "mode",
  "RiskVerifyInfo": {
    "Email": "user@example.com",
    "LastLockDuration": 1713749778,
    "Locked": "true",
    "Phone": "1381111****"
  },
  "Reason": "null",
  "PasswordStrategy": {
    "TenantPasswordLength": "null",
    "TenantAlternativeChars": [
      "null"
    ]
  },
  "NickName": ""
}
```

