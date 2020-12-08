### 完成的功能点

通过简单的api调用，传输账号密码，通过Instagram的API获取cookie

### 需要注意的地方

```java
// LoginController.java
// 这段代码是cookie存放的地址，后期有需要你可以存入mysql
final Path dir = Paths.get(System.getProperty("user.home")).resolve("work")
                .resolve("ins");

// 这里需要配置代理
defaultHttpConfig.setProxyConfig(new DefaultProxyConfig("127.0.0.1", 58591));

// InsLoginProvider.java
// 这个地方需要增加一个对check_point异常的捕获
// check_point是ins对异地登录的校验，需要输入验证码，这个暂时没有解决
try {
            loginResult = HttpUtils.toString(client, post, context);
        } catch (InvalidStateCodeException ex) {
            if (ex.getContent().contains("\"check_point_required\": true")) {
                String identifier = new JsonExecutor(ex.getContent()).execute("check_point_info->check_point_identifier").getAsString();
//                twoFactorLogin(context, identifier, client);
                return;
            }
            throw new LoginFailException(ex.getContent());
        }


```
### 环境依赖
JSE 15
maven 3.6.3


