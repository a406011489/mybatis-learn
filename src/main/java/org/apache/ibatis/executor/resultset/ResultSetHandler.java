/**
 *    Copyright 2009-2015 the original author or authors.
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
package org.apache.ibatis.executor.resultset;

import org.apache.ibatis.cursor.Cursor;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * SQL 执行后，响应的结果集 ResultSet 的处理
 *
 * 负责将JDBC返回的ResultSet结果集对象转换成List类型的集合；
 */
public interface ResultSetHandler {

  /**
   * 处理 {@link java.sql.ResultSet} 成映射的对应的结果
   */
  <E> List<E> handleResultSets(Statement stmt) throws SQLException;

  /**
   * 处理 {@link java.sql.ResultSet} 成 Cursor 对象
   */
  <E> Cursor<E> handleCursorResultSets(Statement stmt) throws SQLException;

  void handleOutputParameters(CallableStatement cs) throws SQLException;

}
