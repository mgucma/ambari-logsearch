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

import org.apache.ambari.logsearch.conf.SolrPropsConfig;
import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import java.io.IOException;
import java.util.List;

public class ACLHandler implements SolrZkRequestHandler<Boolean> {

  private static final Logger logger = LogManager.getLogger(ACLHandler.class);
  private static final int ZK_SESSION_TIMEOUT = 30000; // 30 sekund

  @Override
  public Boolean handle(CloudSolrClient solrClient, SolrPropsConfig solrPropsConfig) throws Exception {
    List<ACL> aclsToSetList = solrPropsConfig.getZkAcls();
    if (CollectionUtils.isNotEmpty(aclsToSetList)) {
      logger.info("Setting ACLs for '{}' collection...", solrPropsConfig.getCollection());
      // Używamy z konfiguracji, aby uzyskać adres połączenia z ZooKeeperem
      String zkHost = solrPropsConfig.getZkConnectString();
      ZooKeeper zk = new ZooKeeper(zkHost, ZK_SESSION_TIMEOUT, event -> {
        // Pusta implementacja Watchera
      });
      try {
        String collectionPath = String.format("/collections/%s", solrPropsConfig.getCollection());
        String configsPath = String.format("/configs/%s", solrPropsConfig.getConfigName());
        
        List<ACL> collectionAcls = zk.getACL(collectionPath, new Stat());
        if (isRefreshAclsNeeded(aclsToSetList, collectionAcls)) {
          logger.info("ACLs differ for {}, updating ACLs.", collectionPath);
          setRecursivelyOn(zk, collectionPath, aclsToSetList);
        }
        List<ACL> configsAcls = zk.getACL(configsPath, new Stat());
        if (isRefreshAclsNeeded(aclsToSetList, configsAcls)) {
          logger.info("ACLs differ for {}, updating ACLs.", configsPath);
          setRecursivelyOn(zk, configsPath, aclsToSetList);
        }
      } finally {
        try {
          zk.close();
        } catch (InterruptedException e) {
          logger.error("Error while closing ZooKeeper client", e);
        }
      }
    }
    return true;
  }

  private boolean isRefreshAclsNeeded(List<ACL> expectedAcls, List<ACL> currentAcls) {
    if (expectedAcls == null || currentAcls == null) {
      return false;
    }
    if (expectedAcls.size() != currentAcls.size()) {
      return true;
    }
    return aclDiffers(expectedAcls, currentAcls) || aclDiffers(currentAcls, expectedAcls);
  }

  private boolean aclDiffers(List<ACL> aclList1, List<ACL> aclList2) {
    for (ACL acl : aclList1) {
      for (ACL newAcl : aclList2) {
        if (acl.getId() != null && acl.getId().getId().equals(newAcl.getId().getId())
            && acl.getPerms() != newAcl.getPerms()) {
          logger.info("ACL for '{}' differs: '{}' on znode, should be '{}'",
              acl.getId().getId(), acl.getPerms(), newAcl.getPerms());
          return true;
        }
      }
    }
    return false;
  }

  private void setRecursivelyOn(ZooKeeper zk, String node, List<ACL> acls)
      throws KeeperException, InterruptedException {
    zk.setACL(node, acls, -1);
    for (String child : zk.getChildren(node, false)) {
      String path = node.endsWith("/") ? node + child : node + "/" + child;
      setRecursivelyOn(zk, path, acls);
    }
  }
}