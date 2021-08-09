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
package org.apache.ibatis.cache;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * 缓存容器接口。
 * 注意，它是一个容器，有点类似 HashMap ，可以往其中添加各种缓存。
 */
public interface Cache {

  /**
   * @return The identifier of this cache
   */
  String getId();

  /**
   * 添加指定键的值
   */
  void putObject(Object key, Object value);

  /**
   * 获得指定键的值
   */
  Object getObject(Object key);

  /**
   * 移除指定键的值
   * @param key The key
   * @return Not used
   */
  Object removeObject(Object key);

  /**
   * 清空缓存
   */
  void clear();

  /**
   * 获得容器中缓存的数量
   */
  int getSize();

  /**
   * Optional. As of 3.2.6 this method is no longer called by the core.
   *
   * Any locking needed by the cache must be provided internally by the cache provider.
   *
   * @return A ReadWriteLock
   */
  ReadWriteLock getReadWriteLock();

}
