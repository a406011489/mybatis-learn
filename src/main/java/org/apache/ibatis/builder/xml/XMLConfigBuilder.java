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
package org.apache.ibatis.builder.xml;

import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;
import javax.sql.DataSource;

import org.apache.ibatis.builder.BaseBuilder;
import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.executor.loader.ProxyFactory;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.parsing.XNode;
import org.apache.ibatis.parsing.XPathParser;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.AutoMappingBehavior;
import org.apache.ibatis.session.AutoMappingUnknownColumnBehavior;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.type.JdbcType;

/**
 * ?????? BaseBuilder ????????????
 * XML????????????????????????????????????mybatis-config.xml???????????????
 * ???????????? mybatis ?????????????????????
 */
public class XMLConfigBuilder extends BaseBuilder {

  /**
   * ???????????????
   */
  private boolean parsed;

  /**
   * ?????? Java XPath ?????????
   */
  private final XPathParser parser;

  /**
   * ??????
   */
  private String environment;

  /**
   * ReflectorFactory ??????
   */
  private final ReflectorFactory localReflectorFactory = new DefaultReflectorFactory();

  public XMLConfigBuilder(Reader reader) {
    this(reader, null, null);
  }

  public XMLConfigBuilder(Reader reader, String environment) {
    this(reader, environment, null);
  }

  public XMLConfigBuilder(Reader reader, String environment, Properties props) {
    this(new XPathParser(reader, true, props, new XMLMapperEntityResolver()), environment, props);
  }

  public XMLConfigBuilder(InputStream inputStream) {
    this(inputStream, null, null);
  }

  public XMLConfigBuilder(InputStream inputStream, String environment) {
    this(inputStream, environment, null);
  }

  public XMLConfigBuilder(InputStream inputStream, String environment, Properties props) {
    this(new XPathParser(inputStream, true, props, new XMLMapperEntityResolver()), environment, props);
  }

  private XMLConfigBuilder(XPathParser parser, String environment, Properties props) {
    // <1> ?????? Configuration ??????
    super(new Configuration());
    ErrorContext.instance().resource("SQL Mapper Configuration");
    // <2> ?????? Configuration ??? variables ??????
    this.configuration.setVariables(props);
    this.parsed = false;
    this.environment = environment;
    this.parser = parser;
  }

  // ?????? XML ??? Configuration ?????????
  public Configuration parse() {
    // ??????????????????????????????
    if (parsed) {
      throw new BuilderException("Each XMLConfigBuilder can only be used once.");
    }
    parsed = true;

    // <2> ?????? XML configuration ??????
    parseConfiguration(parser.evalNode("/configuration"));
    return configuration;
  }

  private void parseConfiguration(XNode root) {
    try {

      // <1> ?????? <properties /> ??????
      propertiesElement(root.evalNode("properties"));

      // <2> ?????? <settings /> ??????
      Properties settings = settingsAsProperties(root.evalNode("settings"));

      // <3> ??????????????? VFS ?????????
      loadCustomVfs(settings);
      loadCustomLogImpl(settings);

      // <4> ?????? <typeAliases /> ??????
      typeAliasesElement(root.evalNode("typeAliases"));

      // <5> ?????? <plugins /> ??????
      pluginElement(root.evalNode("plugins"));

      // <6> ?????? <objectFactory /> ??????
      objectFactoryElement(root.evalNode("objectFactory"));

      // <7> ?????? <objectWrapperFactory /> ??????
      objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));

      // <8> ?????? <reflectorFactory /> ??????
      reflectorFactoryElement(root.evalNode("reflectorFactory"));

      // <9> ?????? <settings /> ??? Configuration ??????
      settingsElement(settings);

      // <10> ?????? <environments /> ??????
      environmentsElement(root.evalNode("environments"));

      // <11> ?????? <databaseIdProvider /> ??????
      databaseIdProviderElement(root.evalNode("databaseIdProvider"));

      // <12> ?????? <typeHandlers /> ??????
      typeHandlerElement(root.evalNode("typeHandlers"));

      // <13> ?????? <mappers /> ??????
      mapperElement(root.evalNode("mappers"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
    }
  }

  private Properties settingsAsProperties(XNode context) {

    if (context == null) {
      return new Properties();
    }

    // ???????????????????????? Properties ??????
    Properties props = context.getChildrenAsProperties();

    // ???????????????????????? Configuration ?????????????????? setting ????????????????????? BuilderException ??????
    MetaClass metaConfig = MetaClass.forClass(Configuration.class, localReflectorFactory);
    for (Object key : props.keySet()) {
      if (!metaConfig.hasSetter(String.valueOf(key))) {
        throw new BuilderException("The setting " + key + " is not known.  Make sure you spelled it correctly (case sensitive).");
      }
    }
    return props;
  }

  private void loadCustomVfs(Properties props) throws ClassNotFoundException {

    // ?????? vfsImpl ??????
    String value = props.getProperty("vfsImpl");
    if (value != null) {

      // ?????? , ???????????????????????? VFS ???????????????
      String[] clazzes = value.split(",");

      // ?????? VFS ???????????????
      for (String clazz : clazzes) {
        if (!clazz.isEmpty()) {
          @SuppressWarnings("unchecked")
          // ?????? VFS ???
          Class<? extends VFS> vfsImpl = (Class<? extends VFS>)Resources.classForName(clazz);

          // ????????? Configuration ???
          configuration.setVfsImpl(vfsImpl);
        }
      }
    }
  }

  private void loadCustomLogImpl(Properties props) {
    Class<? extends Log> logImpl = resolveClass(props.getProperty("logImpl"));
    configuration.setLogImpl(logImpl);
  }


/*  <typeAliases>
  <typeAlias alias="Author" type="domain.blog.Author"/>
  <typeAlias alias="Blog" type="domain.blog.Blog"/>
  <typeAlias alias="Comment" type="domain.blog.Comment"/>
  <typeAlias alias="Post" type="domain.blog.Post"/>
  <typeAlias alias="Section" type="domain.blog.Section"/>
  <typeAlias alias="Tag" type="domain.blog.Tag"/>
</typeAliases>
<typeAliases>
  <package name="domain.blog"/>
</typeAliases>

*/
  private void typeAliasesElement(XNode parent) {
    if (parent != null) {

      // ???????????????
      for (XNode child : parent.getChildren()) {

        // ???????????????????????????????????????????????????
        if ("package".equals(child.getName())) {
          String typeAliasPackage = child.getStringAttribute("name");
          configuration.getTypeAliasRegistry().registerAliases(typeAliasPackage);
        } else { // ???????????????????????????????????????????????????
          String alias = child.getStringAttribute("alias");
          String type = child.getStringAttribute("type");
          try {
            // ?????????????????????
            Class<?> clazz = Resources.classForName(type);

            // ????????? typeAliasRegistry ???
            if (alias == null) {
              typeAliasRegistry.registerAlias(clazz);
            } else {
              typeAliasRegistry.registerAlias(alias, clazz);
            }
          } catch (ClassNotFoundException e) {
            throw new BuilderException("Error registering typeAlias for '" + alias + "'. Cause: " + e, e);
          }
        }
      }
    }
  }

  private void pluginElement(XNode parent) throws Exception {
    if (parent != null) {
      // ?????? <plugins /> ??????
      // ?????? MyBatis ?????????????????????????????????????????????????????????
      // ???????????? Interceptor ??????????????????????????????????????????????????????
      for (XNode child : parent.getChildren()) {
        String interceptor = child.getStringAttribute("interceptor");
        Properties properties = child.getChildrenAsProperties();

        // <1> ?????? Interceptor ????????????????????????
        Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).newInstance();
        interceptorInstance.setProperties(properties);

        // <2> ????????? configuration ???
        configuration.addInterceptor(interceptorInstance);
      }
    }
  }

  private void objectFactoryElement(XNode context) throws Exception {
    if (context != null) {

      // ?????? ObjectFactory ????????????
      String type = context.getStringAttribute("type");

      // ?????? Properties ??????
      Properties properties = context.getChildrenAsProperties();

      // <1> ?????? ObjectFactory ?????????????????? Properties ??????
      ObjectFactory factory = (ObjectFactory) resolveClass(type).newInstance();
      factory.setProperties(properties);

      // <2> ?????? Configuration ??? objectFactory ??????
      configuration.setObjectFactory(factory);
    }
  }

  private void objectWrapperFactoryElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      ObjectWrapperFactory factory = (ObjectWrapperFactory) resolveClass(type).newInstance();
      configuration.setObjectWrapperFactory(factory);
    }
  }

  private void reflectorFactoryElement(XNode context) throws Exception {
    if (context != null) {
      // ?????? ReflectorFactory ????????????
       String type = context.getStringAttribute("type");
      // ?????? ReflectorFactory ??????
       ReflectorFactory factory = (ReflectorFactory) resolveClass(type).newInstance();
      // ?????? Configuration ??? reflectorFactory ??????
       configuration.setReflectorFactory(factory);
    }
  }

  private void propertiesElement(XNode context) throws Exception {
    if (context != null) {

      // ???????????????????????? Properties ??????
      Properties defaults = context.getChildrenAsProperties();

      // ?????? resource ??? url ??????
      String resource = context.getStringAttribute("resource");
      String url = context.getStringAttribute("url");

      // resource ??? url ?????????????????????????????? BuilderException ??????
      if (resource != null && url != null) {
        throw new BuilderException("The properties element cannot specify both a URL and a resource based property file reference.  Please specify one or the other.");
      }

      if (resource != null) {
        // ???????????? Properties ??????????????? defaults ??????
        defaults.putAll(Resources.getResourceAsProperties(resource));
      } else if (url != null) { // ???????????? Properties ??????????????? defaults ??????
        defaults.putAll(Resources.getUrlAsProperties(url));
      }
      // ?????? configuration ?????? Properties ????????? defaults ??????
      Properties vars = configuration.getVariables();
      if (vars != null) {
        defaults.putAll(vars);
      }
      // ?????? defaults ??? parser ??? configuration ??????
      parser.setVariables(defaults);
      configuration.setVariables(defaults);
    }
  }

  private void settingsElement(Properties props) {
    configuration.setAutoMappingBehavior(AutoMappingBehavior.valueOf(props.getProperty("autoMappingBehavior", "PARTIAL")));
    configuration.setAutoMappingUnknownColumnBehavior(AutoMappingUnknownColumnBehavior.valueOf(props.getProperty("autoMappingUnknownColumnBehavior", "NONE")));
    configuration.setCacheEnabled(booleanValueOf(props.getProperty("cacheEnabled"), true));
    configuration.setProxyFactory((ProxyFactory) createInstance(props.getProperty("proxyFactory")));
    configuration.setLazyLoadingEnabled(booleanValueOf(props.getProperty("lazyLoadingEnabled"), false));
    configuration.setAggressiveLazyLoading(booleanValueOf(props.getProperty("aggressiveLazyLoading"), false));
    configuration.setMultipleResultSetsEnabled(booleanValueOf(props.getProperty("multipleResultSetsEnabled"), true));
    configuration.setUseColumnLabel(booleanValueOf(props.getProperty("useColumnLabel"), true));
    configuration.setUseGeneratedKeys(booleanValueOf(props.getProperty("useGeneratedKeys"), false));
    configuration.setDefaultExecutorType(ExecutorType.valueOf(props.getProperty("defaultExecutorType", "SIMPLE")));
    configuration.setDefaultStatementTimeout(integerValueOf(props.getProperty("defaultStatementTimeout"), null));
    configuration.setDefaultFetchSize(integerValueOf(props.getProperty("defaultFetchSize"), null));
    configuration.setMapUnderscoreToCamelCase(booleanValueOf(props.getProperty("mapUnderscoreToCamelCase"), false));
    configuration.setSafeRowBoundsEnabled(booleanValueOf(props.getProperty("safeRowBoundsEnabled"), false));
    configuration.setLocalCacheScope(LocalCacheScope.valueOf(props.getProperty("localCacheScope", "SESSION")));
    configuration.setJdbcTypeForNull(JdbcType.valueOf(props.getProperty("jdbcTypeForNull", "OTHER")));
    configuration.setLazyLoadTriggerMethods(stringSetValueOf(props.getProperty("lazyLoadTriggerMethods"), "equals,clone,hashCode,toString"));
    configuration.setSafeResultHandlerEnabled(booleanValueOf(props.getProperty("safeResultHandlerEnabled"), true));
    configuration.setDefaultScriptingLanguage(resolveClass(props.getProperty("defaultScriptingLanguage")));
    configuration.setDefaultEnumTypeHandler(resolveClass(props.getProperty("defaultEnumTypeHandler")));
    configuration.setCallSettersOnNulls(booleanValueOf(props.getProperty("callSettersOnNulls"), false));
    configuration.setUseActualParamName(booleanValueOf(props.getProperty("useActualParamName"), true));
    configuration.setReturnInstanceForEmptyRow(booleanValueOf(props.getProperty("returnInstanceForEmptyRow"), false));
    configuration.setLogPrefix(props.getProperty("logPrefix"));
    configuration.setConfigurationFactory(resolveClass(props.getProperty("configurationFactory")));
  }

  private void environmentsElement(XNode context) throws Exception {
    if (context != null) {
      // <1> environment ?????????????????? default ????????????
      if (environment == null) {
        environment = context.getStringAttribute("default");
      }
      // ?????? XNode ??????
      for (XNode child : context.getChildren()) {
        String id = child.getStringAttribute("id");
        // <2> ?????? environment ????????????
        if (isSpecifiedEnvironment(id)) {
          // <3> ?????? `<transactionManager />` ??????????????? TransactionFactory ??????
          TransactionFactory txFactory = transactionManagerElement(child.evalNode("transactionManager"));
          // <4> ?????? `<dataSource />` ??????????????? DataSourceFactory ??????
          DataSourceFactory dsFactory = dataSourceElement(child.evalNode("dataSource"));
          DataSource dataSource = dsFactory.getDataSource();

          // <5> ?????? Environment.Builder ??????
          Environment.Builder environmentBuilder = new Environment.Builder(id)
              .transactionFactory(txFactory)
              .dataSource(dataSource);

          // <6> ?????? Environment ????????????????????? configuration ???
          configuration.setEnvironment(environmentBuilder.build());
        }
      }
    }
  }

  private void databaseIdProviderElement(XNode context) throws Exception {
    DatabaseIdProvider databaseIdProvider = null;
    if (context != null) {
      // <1> ?????? DatabaseIdProvider ??????
      String type = context.getStringAttribute("type");
      // awful patch to keep backward compatibility
      if ("VENDOR".equals(type)) {
          type = "DB_VENDOR";
      }
      // <2> ?????? Properties ??????
      Properties properties = context.getChildrenAsProperties();

      // <3> ?????? DatabaseIdProvider ?????????????????????????????????
      databaseIdProvider = (DatabaseIdProvider) resolveClass(type).newInstance();
      databaseIdProvider.setProperties(properties);
    }
    Environment environment = configuration.getEnvironment();
    if (environment != null && databaseIdProvider != null) {
      // <4> ??????????????? databaseId ??????
      String databaseId = databaseIdProvider.getDatabaseId(environment.getDataSource());
      // <5> ????????? configuration ???
      configuration.setDatabaseId(databaseId);
    }
  }

  private TransactionFactory transactionManagerElement(XNode context) throws Exception {
    if (context != null) {
      // ?????? TransactionFactory ??????
      String type = context.getStringAttribute("type");
      Properties props = context.getChildrenAsProperties();
      // ?????? TransactionFactory ????????????????????????
      TransactionFactory factory = (TransactionFactory) resolveClass(type).newInstance();
      factory.setProperties(props);
      return factory;
    }
    throw new BuilderException("Environment declaration requires a TransactionFactory.");
  }

  private DataSourceFactory dataSourceElement(XNode context) throws Exception {
    if (context != null) {
      String type = context.getStringAttribute("type");
      Properties props = context.getChildrenAsProperties();
      DataSourceFactory factory = (DataSourceFactory) resolveClass(type).newInstance();
      factory.setProperties(props);
      return factory;
    }
    throw new BuilderException("Environment declaration requires a DataSourceFactory.");
  }

  private void typeHandlerElement(XNode parent) {
    if (parent != null) {
      for (XNode child : parent.getChildren()) {
        if ("package".equals(child.getName())) {
          String typeHandlerPackage = child.getStringAttribute("name");
          typeHandlerRegistry.register(typeHandlerPackage);
        } else {
          String javaTypeName = child.getStringAttribute("javaType");
          String jdbcTypeName = child.getStringAttribute("jdbcType");
          String handlerTypeName = child.getStringAttribute("handler");
          Class<?> javaTypeClass = resolveClass(javaTypeName);
          JdbcType jdbcType = resolveJdbcType(jdbcTypeName);
          Class<?> typeHandlerClass = resolveClass(handlerTypeName);
          if (javaTypeClass != null) {
            if (jdbcType == null) {
              typeHandlerRegistry.register(javaTypeClass, typeHandlerClass);
            } else {
              typeHandlerRegistry.register(javaTypeClass, jdbcType, typeHandlerClass);
            }
          } else {
            typeHandlerRegistry.register(typeHandlerClass);
          }
        }
      }
    }
  }

  private void mapperElement(XNode parent) throws Exception {
    if (parent != null) {
      //  <0> ???????????????
      for (XNode child : parent.getChildren()) {
        // <1> ????????? package ????????????????????????
        if ("package".equals(child.getName())) {
          // ????????????
          String mapperPackage = child.getStringAttribute("name");
          configuration.addMappers(mapperPackage);
        } else {  // ????????? mapper ?????????

          // ?????? resource???url???class ??????
          String resource = child.getStringAttribute("resource");
          String url = child.getStringAttribute("url");
          String mapperClass = child.getStringAttribute("class");

          // <2> ???????????????????????????????????????
          if (resource != null && url == null && mapperClass == null) {
            ErrorContext.instance().resource(resource);

            // ?????? resource ??? InputStream ??????
            InputStream inputStream = Resources.getResourceAsStream(resource);

            // ?????? XMLMapperBuilder ??????
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());

            // ????????????
            mapperParser.parse();

          } else if (resource == null && url != null && mapperClass == null) {
            // <3> ????????????????????????????????????URL???
            ErrorContext.instance().resource(url);
            InputStream inputStream = Resources.getUrlAsStream(url);
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url, configuration.getSqlFragments());
            mapperParser.parse();
          } else if (resource == null && url == null && mapperClass != null) {
            // <4> ???????????????????????????????????????????????????
            Class<?> mapperInterface = Resources.classForName(mapperClass);
            configuration.addMapper(mapperInterface);
          } else {
            throw new BuilderException("A mapper element may only specify a url, resource or class, but not more than one.");
          }
        }
      }
    }
  }

  private boolean isSpecifiedEnvironment(String id) {
    if (environment == null) {
      throw new BuilderException("No environment specified.");
    } else if (id == null) {
      throw new BuilderException("Environment requires an id attribute.");
    } else if (environment.equals(id)) {
      return true;
    }
    return false;
  }

}
