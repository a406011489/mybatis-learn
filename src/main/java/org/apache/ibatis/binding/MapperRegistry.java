/**
 *    Copyright 2009-2019 the original author or authors.
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
package org.apache.ibatis.binding;

import org.apache.ibatis.builder.annotation.MapperAnnotationBuilder;
import org.apache.ibatis.io.ResolverUtil;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Mapper 注册表。
 */
public class MapperRegistry {

  /**
   * MyBatis Configuration 对象
   */
  private final Configuration config;

  /**
   * MapperProxyFactory 的映射
   *
   * KEY：Mapper 接口
   */
  private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<>();

  public MapperRegistry(Configuration config) {
    this.config = config;
  }

  @SuppressWarnings("unchecked")
  public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
    // 从 MapperRegistry 中的 HashMap 中拿 MapperProxyFactory
    final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);

    // 不存在，则抛出 BindingException 异常
    if (mapperProxyFactory == null) {
      throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
    }
    try {
      //通过动态代理工厂生成示例。
      return mapperProxyFactory.newInstance(sqlSession);
    } catch (Exception e) {
      throw new BindingException("Error getting mapper instance. Cause: " + e, e);
    }
  }

  // 判断是否有Mapper
  public <T> boolean hasMapper(Class<T> type) {
    return knownMappers.containsKey(type);
  }

  public <T> void addMapper(Class<T> type) {
    // <1> 判断，必须是接口。也就是说Mapper接口。
    if (type.isInterface()) {
      // <2> 已经添加过，则抛出BindingException异常
      if (hasMapper(type)) {
        throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
      }
      boolean loadCompleted = false;
      try {
        // <3> 添加到knownMappers中
        knownMappers.put(type, new MapperProxyFactory<>(type));

        // <4> 创建MapperAnnotationBuilder对象，用于解析Mapper的注解配置
        MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
        parser.parse();

        // <5> 标记加载完成
        loadCompleted = true;
      } finally {
        // <6> 若加载未完成，从 knownMappers 中移除
        if (!loadCompleted) {
          knownMappers.remove(type);
        }
      }
    }
  }

  /**
   * 获得 Mapper Proxy 对象。
   */
  public Collection<Class<?>> getMappers() {
    return Collections.unmodifiableCollection(knownMappers.keySet());
  }

  /**
   * @since 3.2.2
   */
  public void addMappers(String packageName, Class<?> superType) {
    // <1> 扫描指定包下的指定类
    ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<>();
    resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
    Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();

    // <2> 遍历，添加到 knownMappers 中
    for (Class<?> mapperClass : mapperSet) {
      addMapper(mapperClass);
    }
  }

  /**
   * 扫描指定包，并将符合的类，添加到 knownMappers 中。
   */
  public void addMappers(String packageName) {
    addMappers(packageName, Object.class);
  }

}
