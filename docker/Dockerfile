# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM rockylinux:9

# Ustawienie hasła root
RUN echo "root:changeme" | chpasswd

# Aktualizacja systemu i instalacja wymaganych pakietów
RUN dnf clean all -y && dnf update -y
RUN dnf -y install --allowerasing firefox xorg-x11-server-Xvfb vim wget rpm-build sudo which telnet tar openssh-server openssh-clients chrony git python3-setuptools python3-devel httpd lsof curl unzip net-tools bind-utils nc

# Ponowna instalacja glibc-common (jeśli to konieczne)
RUN rpm -e --nodeps --justdb glibc-common && dnf -y install glibc-common

# Ustawienie zmiennych środowiskowych
ENV HOME=/root

# Instalacja OpenJDK 11
ENV JAVA_VERSION=11
RUN dnf -y install java-11-openjdk-devel
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk

# Instalacja serwera Selenium
RUN wget --no-check-certificate -O /root/selenium-server-standalone.jar \
    https://selenium-release.storage.googleapis.com/2.53/selenium-server-standalone-2.53.1.jar

# Instalacja Maven
RUN mkdir -p /opt/maven
WORKDIR /opt/maven
RUN wget https://archive.apache.org/dist/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz
RUN tar -xvzf apache-maven-3.8.6-bin.tar.gz
RUN rm -rf apache-maven-3.8.6-bin.tar.gz

ENV M2_HOME=/opt/maven/apache-maven-3.8.6
ENV MAVEN_OPTS=-Xmx2048m
ENV PATH=$PATH:$JAVA_HOME/bin:$M2_HOME/bin

# Konfiguracja SSH
RUN ssh-keygen -f /root/.ssh/id_rsa -t rsa -N ''
RUN cat /root/.ssh/id_rsa.pub > /root/.ssh/authorized_keys
RUN chmod 600 /root/.ssh/authorized_keys
RUN sed -ri 's/^UsePAM yes/UsePAM no/g' /etc/ssh/sshd_config
RUN echo "X11Forwarding yes" >> /etc/ssh/sshd_config
RUN echo "X11DisplayOffset 10" >> /etc/ssh/sshd_config

# Konfiguracja Git (np. dla bower za proxy)
RUN git config --global url."https://".insteadOf git://

# Instalacja Apache Solr
ENV SOLR_VERSION=8.11.4
RUN wget --no-check-certificate -O /root/solr-$SOLR_VERSION.tgz \
    https://archive.apache.org/dist/lucene/solr/$SOLR_VERSION/solr-$SOLR_VERSION.tgz
RUN cd /root && tar -zxvf solr-$SOLR_VERSION.tgz

# Instalacja Knox
WORKDIR /
RUN adduser knox
ENV KNOX_VERSION=1.6.0
RUN wget --no-check-certificate -q -O /knox-${KNOX_VERSION}.zip \
    https://downloads.apache.org/knox/${KNOX_VERSION}/knox-${KNOX_VERSION}.zip && \
    unzip /knox-${KNOX_VERSION}.zip && \
    rm knox-${KNOX_VERSION}.zip && \
    ln -nsf knox-${KNOX_VERSION} knox && \
    chmod +x /knox/bin/*.sh && \
    chown -R knox /knox/

ADD knox/keystores /knox-secrets
RUN cd /knox-secrets && unzip test-secrets.zip
RUN mkdir -p /knox/data/security/keystores
RUN mv /knox-secrets/master /knox/data/security/master
RUN cd /knox-secrets && cp -r * /knox/data/security/keystores/
RUN chown -R knox /knox/data/security

ADD knox/ldap.sh /ldap.sh
ADD knox/gateway.sh /gateway.sh
RUN touch /gateway.out && chown -R knox /gateway.out
RUN touch /ldap.out && chown -R knox /ldap.out

ADD bin/start.sh /root/start.sh
ADD test-config /root/test-config
ADD test-logs /root/test-logs
RUN chmod -R 777 /root/test-config
RUN chmod +x /root/start.sh

ENV SOLR_CONFIG_LOCATION=/root/config/solr
ENV LOGSEARCH_CONF_DIR=/root/config/logsearch
ENV LOGFEEDER_CONF_DIR=/root/config/logfeeder
ENV SOLR_INCLUDE=/root/config/solr/solr-env.sh

RUN mkdir -p /var/run/ambari-logsearch-solr /var/log/ambari-logsearch-solr /var/run/ambari-infra-solr-client \
  /var/log/ambari-infra-solr-client /root/logsearch_solr_index/data \
  /var/run/ambari-logsearch-portal /var/log/ambari-logsearch-portal \
  /var/run/ambari-logsearch-logfeeder /var/log/ambari-logsearch-logfeeder

RUN cp /root/test-config/solr/solr.xml /root/logsearch_solr_index/data
RUN cp /root/test-config/solr/zoo.cfg /root/logsearch_solr_index/data

RUN mkdir -p /root/config
RUN chmod -R 777 /root/config

WORKDIR /root
CMD ["/root/start.sh"]