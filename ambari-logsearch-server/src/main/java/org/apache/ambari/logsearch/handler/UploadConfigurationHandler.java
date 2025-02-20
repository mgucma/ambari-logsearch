/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ambari.logsearch.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.util.Arrays;

import org.apache.ambari.logsearch.conf.SolrPropsConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;

public class UploadConfigurationHandler extends AbstractSolrConfigHandler {

  private static final Logger logger = LogManager.getLogger(UploadConfigurationHandler.class);

  private static final String SOLR_CONFIG_FILE = "solrconfig.xml";
  private static final String[] configFiles = {
    "admin-extra.html", "admin-extra.menu-bottom.html", "admin-extra.menu-top.html",
    "elevate.xml", "enumsConfig.xml", "managed-schema", "solrconfig.xml"
  };
  private boolean hasEnumConfig;

  public UploadConfigurationHandler(File configSetFolder, boolean hasEnumConfig) {
    super(configSetFolder);
    this.hasEnumConfig = hasEnumConfig;
  }

  @Override
  public boolean updateConfigIfNeeded(SolrPropsConfig solrPropsConfig, ZooKeeper zk, File file,
                                      String separator, byte[] content) throws IOException {
    if (Arrays.equals(FileUtils.readFileToByteArray(file), content)) {
      return false;
    }

    logger.info("Solr config file differs ('{}'), uploading config set to ZooKeeper", file.getName());
    // Upload cały katalog konfiguracyjny
    uploadConfigDir(zk, solrPropsConfig.getConfigName(), getConfigSetFolder());
    String filePath = getConfigSetFolder().getAbsolutePath() + separator + getConfigFileName();
    String configsPath = String.format("/configs/%s/%s", solrPropsConfig.getConfigName(), getConfigFileName());
    uploadFileToZk(zk, filePath, configsPath);
    return true;
  }

  @Override
  public void doIfConfigNotExist(SolrPropsConfig solrPropsConfig, ZooKeeper zk) throws IOException {
    logger.info("Config set does not exist for '{}' collection. Uploading it to ZooKeeper...", solrPropsConfig.getCollection());
    // Upload całego katalogu konfiguracji
    uploadConfigDir(zk, solrPropsConfig.getConfigName(), getConfigSetFolder());
  }

  @Override
  public String getConfigFileName() {
    return SOLR_CONFIG_FILE;
  }

  @Override
  public void uploadMissingConfigFiles(ZooKeeper zk, String configName) throws IOException {
    logger.info("Checking if any config files are missing for config ({})", configName);
    for (String configFile : configFiles) {
      if ("enumsConfig.xml".equals(configFile) && !hasEnumConfig) {
        logger.info("Config file ({}) is not needed for {}", configFile, configName);
        continue;
      }
      String zkPath = String.format("/configs/%s/%s", configName, configFile);
      if (existsInZooKeeper(zk, zkPath)) {
        logger.info("Config file ({}) has already been uploaded properly.", configFile);
      } else {
        logger.info("Config file ({}) is missing. Reuploading...", configFile);
        String localFilePath = getConfigSetFolder().getAbsolutePath() + FileSystems.getDefault().getSeparator() + configFile;
        uploadFileToZk(zk, localFilePath, zkPath);
      }
    }
  }

  private boolean existsInZooKeeper(ZooKeeper zk, String path) throws IOException {
    try {
      return zk.exists(path, false) != null;
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  private void uploadFileToZk(ZooKeeper zk, String filePath, String zkPath) throws FileNotFoundException {
    InputStream is = new FileInputStream(filePath);
    try {
      byte[] data = IOUtils.toByteArray(is);
      if (zk.exists(zkPath, false) != null) {
        zk.setData(zkPath, data, -1);
      } else {
        zk.create(zkPath, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  private void uploadConfigDir(ZooKeeper zk, String configName, File configDir) throws IOException {
    if (!configDir.isDirectory()) {
      throw new IOException("Not a directory: " + configDir);
    }
    // Utwórz główny znode dla konfiguracji, jeśli nie istnieje
    String basePath = "/configs/" + configName;
    try {
      if (zk.exists(basePath, false) == null) {
        zk.create(basePath, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
      }
    } catch (Exception e) {
      throw new IOException(e);
    }
    // Przesyłanie plików rekurencyjnie
    for (File file : configDir.listFiles()) {
      if (file.isDirectory()) {
        String subConfigName = configName + "/" + file.getName();
        uploadConfigDir(zk, subConfigName, file);
      } else {
        String zkFilePath = String.format("/configs/%s/%s", configName, file.getName());
        uploadFileToZk(zk, file.getAbsolutePath(), zkFilePath);
      }
    }
  }
}