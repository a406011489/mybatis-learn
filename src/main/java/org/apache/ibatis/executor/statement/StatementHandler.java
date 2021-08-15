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
package org.apache.ibatis.executor.statement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.ResultHandler;

/**
 * Statement 处理器
 * 其中 Statement 包含 java.sql.Statement、java.sql.PreparedStatement、java.sql.CallableStatement 三种。
 *
 * 封装了JDBC Statement操作，负责对JDBC statement的操作，如设置参数、将Statement结果集转换成List集合。
 *
 * 对于JDBC的PreparedStatement类型的对象，创建的过程中，我们使用的是SQL语句字符串会包含若干个问号(？)占位符，
 * 我们其后再对占位符进行设值。StatementHandler通过parameterize(statement)方法对 Statement 进行设值；
 *
 * StatementHandler 通过 List query(Statement statement, ResultHandler resultHandler)方法来
 * 完成执行Statement，和将Statement对象返回的resultSet封装成List；
 *
 */
public interface StatementHandler {

  /**
   * 准备操作，可以理解成创建 Statement 对象
   */
  Statement prepare(Connection connection, Integer transactionTimeout) throws SQLException;

  /**
   * 设置 Statement 对象的参数
   */
  void parameterize(Statement statement) throws SQLException;

  /**
   * 添加 Statement 对象的批量操作
   */
  void batch(Statement statement) throws SQLException;

  /**
   * 执行写操作
   */
  int update(Statement statement) throws SQLException;

  /**
   * 执行读操作
   * @param resultHandler ResultHandler 对象，处理结果
   */
  <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException;

  /**
   * 执行读操作，返回 Cursor 对象
   */
  <E> Cursor<E> queryCursor(Statement statement) throws SQLException;

  /**
   * @return BoundSql 对象
   */
  BoundSql getBoundSql();

  /**
   * @return ParameterHandler 对象
   */
  ParameterHandler getParameterHandler();

}
