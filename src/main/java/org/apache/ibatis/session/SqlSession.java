/**
 *    Copyright 2009-2016 the original author or authors.
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
package org.apache.ibatis.session;

import java.io.Closeable;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.BatchResult;

/**
 * SQL Session 接口。
 *
 * 该类在 MyBatis 中是非常强大的一个类。
 * 在这里你会看到所有执行语句、
 * 提交或回滚事务和获取映射器实例的方法。
 *
 * 作为MyBatis工作的主要顶层API，表示和数据库交互的会话，完成必要数据库增删改查功能
 */
public interface SqlSession extends Closeable {

  <T> T selectOne(String statement);

  <T> T selectOne(String statement, Object parameter);

  <E> List<E> selectList(String statement);

  <E> List<E> selectList(String statement, Object parameter);

  <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds);

  <K, V> Map<K, V> selectMap(String statement, String mapKey);

  <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey);

  <K, V> Map<K, V> selectMap(String statement, Object parameter, String mapKey, RowBounds rowBounds);

  <T> Cursor<T> selectCursor(String statement);

  <T> Cursor<T> selectCursor(String statement, Object parameter);

  <T> Cursor<T> selectCursor(String statement, Object parameter, RowBounds rowBounds);

  void select(String statement, Object parameter, ResultHandler handler);

  void select(String statement, ResultHandler handler);

  void select(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler);

  int insert(String statement);

  int insert(String statement, Object parameter);

  int update(String statement);

  int update(String statement, Object parameter);

  int delete(String statement);

  int delete(String statement, Object parameter);

  void commit();

  void commit(boolean force);

  void rollback();

  void rollback(boolean force);

  List<BatchResult> flushStatements();

  @Override
  void close();

  void clearCache();

  Configuration getConfiguration();

  <T> T getMapper(Class<T> type);

  Connection getConnection();
}
