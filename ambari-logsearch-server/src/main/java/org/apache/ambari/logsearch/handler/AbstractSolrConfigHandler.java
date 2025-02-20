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
import java.io.IOException;
import java.nio.file.FileSystems;

import org.apache.ambari.logsearch.conf.SolrPropsConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public abstract class AbstractSolrConfigHandler implements SolrZkRequestHandler<Boolean> {

  private static final Logger logger = LogManager.getLogger(AbstractSolrConfigHandler.class);
  private static final String CONFIGS_ZKNODE = "/configs";
  private static final int ZK_SESSION_TIMEOUT = 30000; // 30 sekund

  private File configSetFolder;

  public AbstractSolrConfigHandler(File configSetFolder) {
    this.configSetFolder = configSetFolder;
  }

  @Override
  public Boolean handle(CloudSolrClient solrClient, SolrPropsConfig solrPropsConfig) throws Exception {
    boolean reloadCollectionNeeded = false;
    String separator = FileSystems.getDefault().getSeparator();
    solrClient.connect();
    // Uzyskaj adres ZooKeepera z konfiguracji, zamiast solrClient.getZkHost()
    String zkHost = solrPropsConfig.getZkConnectString();
    ZooKeeper zk = new ZooKeeper(zkHost, ZK_SESSION_TIMEOUT, event -> {
      // Pusta implementacja Watchera
    });
    try {
      String configPath = CONFIGS_ZKNODE + "/" + solrPropsConfig.getConfigName();
      Stat stat = zk.exists(configPath, false);
      if (stat != null) {
        // Konfiguracja już istnieje
        uploadMissingConfigFiles(zk, configPath);
        reloadCollectionNeeded = doIfConfigExists(solrPropsConfig, zk, separator);
      } else {
        // Konfiguracja nie istnieje – wykonaj akcje dla nieistniejącego configu
        doIfConfigNotExist(solrPropsConfig, zk);
        uploadMissingConfigFiles(zk, configPath);
      }
    } catch (Exception e) {
      throw new RuntimeException(String.format("Cannot upload configurations to zk. (collection: %s, config set folder: %s)",
        solrPropsConfig.getCollection(), solrPropsConfig.getConfigSetFolder()), e);
    } finally {
      zk.close();
    }
    return reloadCollectionNeeded;
  }

  /**
   * Metoda abstrakcyjna, która aktualizuje plik konfiguracyjny (np. solrconfig.xml) na znode w ZooKeeperze.
   * @param solrPropsConfig globalne ustawienia Solr
   * @param zk instancja ZooKeeper
   * @param file plik, który należy przesłać do ZooKeepera
   * @param separator separator systemowy
   * @param content zawartość pliku
   * @return true, jeśli aktualizacja została wykonana lub można ją pominąć
   * @throws IOException błąd podczas przesyłania pliku
   */
  public abstract boolean updateConfigIfNeeded(SolrPropsConfig solrPropsConfig, ZooKeeper zk, File file,
                                               String separator, byte[] content) throws IOException;

  /**
   * Zwraca nazwę pliku konfiguracyjnego, który powinien być przesłany do ZooKeepera.
   */
  public abstract String getConfigFileName();

  /**
   * Metoda wywoływana, gdy konfiguracja nie istnieje.
   * Domyślnie nie robi nic.
   */
  public void doIfConfigNotExist(SolrPropsConfig solrPropsConfig, ZooKeeper zk) throws IOException {
    // Domyślnie brak akcji
  }

  /**
   * Metoda wywoływana, gdy konfiguracja już istnieje.
   * Domyślnie nie robi nic.
   */
  public void uploadMissingConfigFiles(ZooKeeper zk, String configPath) throws IOException {
    // Domyślnie brak akcji
  }

  public boolean doIfConfigExists(SolrPropsConfig solrPropsConfig, ZooKeeper zk, String separator) throws IOException {
    logger.info("Config set exists for '{}' collection. Refreshing it if needed...", solrPropsConfig.getCollection());
    try {
      File[] listOfFiles = getConfigSetFolder().listFiles();
      if (listOfFiles == null)
        return false;
      String configFilePath = String.format("%s/%s/%s", CONFIGS_ZKNODE, solrPropsConfig.getConfigName(), getConfigFileName());
      byte[] data = zk.getData(configFilePath, false, null);

      for (File file : listOfFiles) {
        if (file.getName().equals(getConfigFileName()) && updateConfigIfNeeded(solrPropsConfig, zk, file, separator, data)) {
          return true;
        }
      }
      return false;
    } catch (KeeperException | InterruptedException e) {
      throw new IOException("Error downloading files from zookeeper path " + solrPropsConfig.getConfigName(), e);
    }
  }

  protected File getConfigSetFolder() {
    return configSetFolder;
  }
}