/**
 *    Copyright 2009-2018 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PluginTest {

  @Test
  public void mapPluginShouldInterceptGet() {
    Map map = new HashMap();
    map = (Map) new AlwaysMapPlugin().plugin(map);
    assertEquals("Always", map.get("Anything"));
  }

  @Test
  public void shouldNotInterceptToString() {
    Map map = new HashMap();
    map = (Map) new AlwaysMapPlugin().plugin(map);
    assertFalse("Always".equals(map.toString()));
  }


  @Intercepts(
      {
          // 可以定义多个@Signature对多个地方拦截，都用这个拦截器
          @Signature(type = Map.class, // 这是指拦截哪个接口
                  method = "get",      // 这个接口内的哪个方法名
                  args = {Object.class} // 这是拦截的方法的入参，按顺序写到这，不要多也不要少，如果方法重载，可是要通过方法名和入参来确定唯一的
          )
      }
  )
  // 通过 @Intercepts 和 @Signature 注解，定义了需要拦截的方法为 Map 类型、方法为 "get" 方法，方法参数为 Object.class 。
  public static class AlwaysMapPlugin implements Interceptor {

    // 在实现方法 #intercept(Invocation invocation) 方法，
    // 直接返回 "Always" 字符串。也就是说，当所有的 target 类型为 Map 类型，
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
      // 可以在此进行增强逻辑

      // 并且调用 Map#get(Object) 方法时，返回的都是 "Always"
      return "Always11";
    }

    @Override
    public Object plugin(Object target) {
      // 在实现方法 #plugin(Object target) 方法内部，
      // 他调用 Plugin#wrap(Object target, Interceptor interceptor) 方法，执行代理对象的创建。
      return Plugin.wrap(target, this);
    }

    @Override
    // 在实现方法 #setProperties(Properties properties) 方法内部，
    // 暂未做任何实现。此处可以实现，若 AlwaysMapPlugin 有属性，可以从 properties 获取一些需要的属性值。
    public void setProperties(Properties properties) {
    }
  }
}
