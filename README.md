## Huawei Push Kit 🛠

Hello everyone, Today we will learn Huawei Push Kit.

**Note** : Used within the Huawei Map Kit application. Similar steps exist.

https://github.com/halilozel1903/HuaweiMapKitApp

## What is Huawei Push Kit ⁉️

![Huawei](https://github.com/halilozel1903/HuaweiPushKitApp/blob/master/photos/huawei-push-kit-doc.jpeg)

**HUAWEI Push Kit** is a messaging service provided for you. It establishes a messaging channel from the cloud to devices. By integrating Push Kit, you can send messages to your apps on users' devices in real time. This helps you maintain closer ties with users and increases user awareness of and engagement with your apps. The following figure shows the process of sending messages from the cloud to devices.

Currently, Push Kit has been supported in mobile phones, tablets, HUAWEI Vision, HUAWEI Watch, and Volvo and Dongfeng Sokon head units.

For more : https://developer.huawei.com/consumer/en/doc/development/HMSCore-Guides/android-sdk-introduction-0000001050158633

## How to use Huawei Push Kit 📌

You can follow the steps in this blog post.


The steps we need to integrate for **Push Kit** are as follows.

- We activate Push Kit from the Manage API section

![PushKitIntegration](https://github.com/halilozel1903/HuaweiPushKitApp/blob/master/photos/huawei-push-kit.png)

- We are making Push Kit integrations.

![PushKitEnable](https://github.com/halilozel1903/HuaweiPushKitApp/blob/master/photos/huawei-push-kit-enable.png)

- We choose Germany from the data storage location section.

![PushKitLocation](https://github.com/halilozel1903/HuaweiPushKitApp/blob/master/photos/huawei-push-kit-location.png)

- We fill in the fields below for the notification example.

![PushKitLocation](https://github.com/halilozel1903/HuaweiPushKitApp/blob/master/photos/huawei-push-kit-sample.png)

- When we click the test effect button, it will ask us for tokens.

In this step, we will be able to learn the token value we need when we integrate the Push Kit into the application.

- Push Kit added in **build.gradle(:app)**

 ```kotlin
 implementation 'com.huawei.hms:push:5.0.0.300'
 ```


- We create a function in Main Activity. We define an instance to get the token value.

```kotlin
       private fun getToken() {
           object : Thread() {
               override fun run() {
                   try {
                       val appId = AGConnectOptionsBuilder().build(this@MainActivity)
                           .getString("client/app_id")
                       val token = HmsInstanceId.getInstance(this@MainActivity)
                           .getToken(appId, "HCM")
                       Log.i("PUSH", "getToken() token: $token")
                   } catch (e: ApiException) {
                       Log.e("PUSH", "getToken() failure: ${e.message}")
                   }
               }
           }.start()
       }
```

We run the application. The token value is displayed in the log section.

![HuaweiPushKToken](https://github.com/halilozel1903/HuaweiPushKitApp/blob/master/photos/huaweiPushKitToken.png)


We put this token value in the field in the Push Kit test.

![HuaweiPushTest](https://github.com/halilozel1903/HuaweiPushKitApp/blob/master/photos/huawei-push-kit-test.png)


We will test the application with Cloud Debugging.You can run the application by following the below document.

https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-Guides/agc-clouddebug-realtimedebug


We have done all the necessary actions. Congratulations 🥳 You have developed the first Huawei Push Kit application.

<img src="https://github.com/halilozel1903/HuaweiPushKitApp/blob/master/photos/huawei-push-kit-screen.png" width="300" />


<br>

## Testing with Postman 📨

You can test the application with Postman by following the steps below.

- Download the json file located in the url below.

https://gist.github.com/halilozel1903/a9bc9c09bd00ac490fbb26d4ffeefa38

- Open Postman. Import the file. (Postman -> file -> import)
After you import, we can send 3 requests

<img src="https://github.com/halilozel1903/HuaweiPushKitApp/blob/master/photos/huaweiPushKitPostman.png" width="300" />

- Replace the **app_id** and **app_secret** values in the body with the App ID and App secret values on the application home page of the Huawei Console. Replace the fields in the image below with the values in your own project.

![huaweiPushGallery](https://github.com/halilozel1903/HuaweiPushKitApp/blob/master/photos/huaweiPushGallery.png)


- Change the **app_id** value in the link in the Send notification and Send data message files with the App ID on the application page in the console.

- Open the Send notification and Send data message files and replace the **push_token** part in the Body with the token we get from the application logs.

![PushKitToken](https://github.com/halilozel1903/HuaweiPushKitApp/blob/master/photos/huaweiPushToken.png)

- Open the Get access token file and click the Send button to **get access token**. The access token is valid for 3600 seconds and you need to renew it later.

![huaweiPushAccessToken](https://github.com/halilozel1903/HuaweiPushKitApp/blob/master/photos/huaweiPushAccessToken.png)

- Open the Send notification and Send data message file and replace the access_token part in Authorization with the access token we got in before step. Don't forget to delete this characters \  in the access token.

![huaweiPushSend](https://github.com/halilozel1903/HuaweiPushKitApp/blob/master/photos/huaweiPushSend.png)

When we follow the steps above and make a request, we will have successfully seen our notifications on the screen.

<img src="https://github.com/halilozel1903/HuaweiPushKitApp/blob/master/photos/huaweiPushScreen2.png" width="300" /> <img src="https://github.com/halilozel1903/HuaweiPushKitApp/blob/master/photos/huaweiPushScreen1.png" width="300" />


<br>

## Recourses 📚

1. https://developer.huawei.com/consumer/en/codelab/HMSPushKit/

2. https://developer.huawei.com/consumer/en/hms/huawei-pushkit/

3. https://developer.huawei.com/consumer/en/doc/development/HMS-Guides/push-console

<br>

## License ℹ️
```
MIT License

Copyright (c) 2021 Halil OZEL

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
