/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.conf;

import org.apache.ambari.logsearch.configurer.SslConfigurer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.RequestLogWriter;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

import jakarta.inject.Inject;
import java.nio.file.Paths;
import java.time.Duration;

import static org.apache.ambari.logsearch.common.LogSearchConstants.LOGSEARCH_SESSION_ID;

@Component
public class LogSearchWebServerCustomizer implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {

  @Inject
  private ServerProperties serverProperties;

  @Inject
  private LogSearchHttpConfig logSearchHttpConfig;

  @Inject
  private SslConfigurer sslConfigurer;

  @Override
  public void customize(JettyServletWebServerFactory webServerFactory) {
    serverProperties.getServlet().getSession().setTimeout(Duration.ofMinutes(logSearchHttpConfig.getSessionTimeout()));
    serverProperties.getServlet().getSession().getCookie().setName(LOGSEARCH_SESSION_ID);

    if ("https".equals(logSearchHttpConfig.getProtocol())) {
      sslConfigurer.ensureStorePasswords();
      sslConfigurer.loadKeystore();
      webServerFactory.addServerCustomizers((JettyServerCustomizer) server -> {
        // Use SslContextFactory.Server
        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();

        // Configure SslContextFactory.Server using methods from sslConfigurer
        sslContextFactory.setKeyStorePath(sslConfigurer.getKeyStoreLocation());
        sslContextFactory.setKeyStorePassword(sslConfigurer.getKeyStorePassword());
        // Use keystore password for key manager since a separate method is not provided
        sslContextFactory.setKeyManagerPassword(sslConfigurer.getKeyStorePassword());
        sslContextFactory.setTrustStorePath(sslConfigurer.getTrustStoreLocation());
        sslContextFactory.setTrustStorePassword(sslConfigurer.getTrustStorePassword());

        // Configure HTTP and HTTPS
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setSecureScheme("https");
        httpConfig.setSecurePort(logSearchHttpConfig.getHttpsPort());
        httpConfig.addCustomizer(new SecureRequestCustomizer());

        // Create the HTTP connection factory
        HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory(httpConfig);

        // Create the SSL connection factory
        SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory, "http/1.1");

        // Create the ServerConnector with both HTTP and SSL connection factories
        ServerConnector sslConnector = new ServerConnector(server, sslConnectionFactory, httpConnectionFactory);
        sslConnector.setPort(logSearchHttpConfig.getHttpsPort());

        // Set the connector
        server.setConnectors(new Connector[]{sslConnector});
      });
    } else {
      webServerFactory.setPort(logSearchHttpConfig.getHttpPort());
    }

    if (logSearchHttpConfig.isUseAccessLogs()) {
      webServerFactory.addServerCustomizers((JettyServerCustomizer) server -> {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration configuration = context.getConfiguration();
        String logDir = configuration.getStrSubstitutor().getVariableResolver().lookup("log-path");
        String logFileNameSuffix = "logsearch-jetty-yyyy_mm_dd.request.log";
        String logFileName = logDir == null ? logFileNameSuffix : Paths.get(logDir, logFileNameSuffix).toString();
        RequestLogWriter writer = new RequestLogWriter(logFileName);
        writer.setAppend(true);
        writer.setTimeZone("GMT");
        CustomRequestLog requestLog = new CustomRequestLog(writer, CustomRequestLog.NCSA_FORMAT);
        server.setRequestLog(requestLog);
      });
    }
  }
}
