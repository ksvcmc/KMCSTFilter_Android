# 金山云魔方贴纸API文档
## 项目背景
金山魔方是一个多媒体能力提供平台，通过统一接入API、统一鉴权、统一计费等多种手段，降低客户接入多媒体处理能力的代价，提供多媒体能力供应商的效率。 本文档主要针对统一FILTER功能而说明。
## 效果展示
![Alt text](https://raw.githubusercontent.com/wiki/ksvcmc/KMCSTFilter_iOS/img.jpg)
## 名词解释
**贴纸资源**:下图右侧，选中icon后下载下来并且出现在人脸上的图案为贴纸资源。  
**缩略图**:下图左侧下方十个格子中，每个格子中的的icon即为滤镜缩略图。  
**贴纸特效动作**:贴纸里可能含有点头，摇头，张嘴等触发特效的动作，KMCArMaterial里详细定义了具体的触发动作和tips.  
## 鉴权
SDK在使用时需要用token进行鉴权后方可使用，token申请方式见**接入步骤**部分;  
token与应用包名为一一对应的关系;

## 工程目录:    
**demo**:商汤贴纸示例工程  
**libs**: 魔方sdk包libkmcfilter.jar，以及厂家sdk包  

**注: demo工程使用软链接引用libs目录，对于windows平台做Android开发的用户，需要手动将libs目录拷贝到demo目录下。**

此外，gradle需要依赖libksylive库:    
compile 'com.ksyun.media:libksylive-java:2.3.2'   
compile 'com.ksyun.media:libksylive-arm64:2.3.2'   
compile 'com.ksyun.media:libksylive-armv7a:2.3.2'   
compile 'com.ksyun.media:libksylive-x86:2.3.2'   


## 导入SDK
引入目标库, 将libs目录下的库文件引入到目标工程中并添加依赖。

可参考下述配置方式（以Android Studio为例）：
 +  推荐直接使用gradle方式集成：
  
    ```
       allprojects {
         repositories {
             jcenter()
       }
       
       dependencies {
        compile 'com.ksyun.mc:libkmcfilter_sensetime:1.0.4'
        compile 'com.ksyun.mc:SenseTimeAR:1.0.4'
       }
    ```

 +  手动下载集成
   将libs目录copy到目标工程的根目录下；
   修改目标工程的build.gradle文件，配置jniLibs路径：
   
    ```
    sourceSets {
        main {
            jniLibs.srcDirs = ['libs']
        }
    }
    ```


## SDK包总体介绍
libkmcfilter对外提供统一的接口:  
KMCArMaterial 贴纸素材类，  
KMCFilter为贴纸接口类，可以作为fiter设置到推流SDK中  
KMCAuthManager 提供鉴权功能  
KMCFilterManager 提供贴纸列表查询、贴纸下载等功能  


## SDK使用指南  
目前本sdk集成多家厂家信息,厂家的贴纸信息有的托管在金山的服务器，有的托管在厂家的服务器，具体信息可以咨询商务，本sdk只是提供统一的贴纸鉴权,下载，显示服务。
+ **鉴权**  
  本sdk包采用鉴权加密方式，需要通过商务渠道拿到授权的token信息，方可以使用，具体请咨询商务。
鉴权函数如下，其中auth为ak信息，date为过期时间。  
```java
/**
 * @param context
 * @param auth token
 * @param listener 注册结果的回调
 */
void authorize(Context context, String token, AuthResultListener listener)；
```

+ **上传贴纸**（不在本sdk范围内,请参考控制台文档）   
 客户根据自己选择的厂家，按照厂家要求，自己设计好贴纸，通过金山控制台上传贴纸。
+ **拉取贴纸索引信息**  
 客户可以在控制台把贴纸放入一个group里面，sdk通过groupID进行拉取，相关函数为：
```java
void fetchMaterials(final Context context, final String groupID, final FetchMaterialListener listener)；
```
拉取成功后，资源索引文件，包括贴纸的下载地址，缩略图的下载地址，贴纸的手势ID,手势描述信息等，可以在此处设置UI相关信息。
+ **查询贴纸是否已经下载到本地**  
```java
boolean isMaterialDownloaded(Context context, KMCArMaterial material);
```
+ **下载贴纸**  
贴纸资源大小不固定，大的可能几M，小的可能几十K,相关函数：
```java
void downloadMaterial(final Context context, final KMCArMaterial material, final DownloadMaterialListener listener);
```
+ **显示贴纸**  
  贴纸下载完成后，创建KMCFitler实例，然后将KMCArMaterial设置给filter即可开始显示  
```java
/**
 激活素材
 @param material        需要展示的素材
 */
void startShowingMaterial(KMCArMaterial material);
```
## 接入流程
![金山魔方接入流程](https://raw.githubusercontent.com/wiki/ksvcmc/KMCSTFilter_Android/all.jpg "金山魔方接入流程")
## 接入步骤  
1.登录[金山云控制台]( https://console.ksyun.com)，选择视频服务-金山魔方
![步骤1](https://raw.githubusercontent.com/wiki/ksvcmc/KMCSTFilter_Android/step1.png "接入步骤1")

2.在金山魔方控制台中挑选所需服务。
![步骤2](https://raw.githubusercontent.com/wiki/ksvcmc/KMCSTFilter_Android/step2.png "接入步骤2")

3.点击申请试用，填写申请资料。
![步骤3](https://raw.githubusercontent.com/wiki/ksvcmc/KMCSTFilter_Android/step3.png "接入步骤3")

![步骤4](https://raw.githubusercontent.com/wiki/ksvcmc/KMCSTFilter_Android/step4.png "接入步骤4")

4.待申请审核通过后，金山云注册时的邮箱会收到邮件及试用token。
![步骤5](https://raw.githubusercontent.com/wiki/ksvcmc/KMCSTFilter_Android/step5.png "接入步骤5")

5.下载安卓/iOS版本的SDK集成进项目。
![步骤6](https://raw.githubusercontent.com/wiki/ksvcmc/KMCSTFilter_Android/step6.png "接入步骤6")

6.参照文档和DEMO填写TOKEN，就可以Run通项目了。  
7.试用中或试用结束后，有意愿购买该服务可以与我们的商务人员联系购买。  
（商务Email:KSC-VBU-KMC@kingsoft.com）
## Demo下载
![Alt text](https://raw.githubusercontent.com/wiki/ksvcmc/KMCSTFilter_iOS/code.png)
## 反馈与建议  
主页：[金山魔方](https://docs.ksyun.com/read/latest/142/_book/index.html)  
邮箱：ksc-vbu-kmc-dev@kingsoft.com  
QQ讨论群：574179720 [视频云技术交流群]
