/*
 * Copyright (C) 2012 - 2012 NHN Corporation
 * All rights reserved.
 *
 * This file is part of The nGrinder software distribution. Refer to
 * the file LICENSE which is part of The nGrinder distribution for
 * licensing details. The nGrinder distribution is available on the
 * Internet at http://nhnopensource.org/ngrinder
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.ngrinder.chart.service;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.math.NumberUtils;
import org.junit.Test;
import org.ngrinder.agent.model.AgentInfo;
import org.ngrinder.chart.AbstractChartTransactionalTest;
import org.ngrinder.common.util.ThreadUtil;
import org.ngrinder.infra.AgentConfig;
import org.ngrinder.monitor.MonitorConstants;
import org.ngrinder.monitor.agent.AgentMonitorServer;
import org.ngrinder.monitor.controller.model.JavaDataModel;
import org.ngrinder.monitor.controller.model.SystemDataModel;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Class description.
 * 
 * @author Tobi
 * @since
 * @date 2012-7-23
 */
public class MonitorAgentServiceTest extends AbstractChartTransactionalTest {

	@Autowired
	private MonitorAgentService monitorDataService;
	
	@Autowired
	private MonitorService monitorService;
		
	private void startMonitorServer(boolean isAgent) {
		AgentConfig agentConfig = new AgentConfig();
		agentConfig.init();

		MonitorConstants.init(agentConfig);
		LOG.info("**************************");
		LOG.info("* Start nGrinder Monitor *");
		LOG.info("**************************");
		LOG.info("* Colllect SYSTEM data. **");
		try {
			Set<String> collector = null;
			if (isAgent) {
				collector = MonitorConstants.AGENT_SERVER_DATA_COLLECTOR;
			}else {
				collector = MonitorConstants.TARGET_SERVER_DATA_COLLECTOR;
			}
			AgentMonitorServer.getInstance().init(MonitorConstants.DEFAULT_AGENT_PORT, collector);
			AgentMonitorServer.getInstance().start();
		} catch (Exception e) {
			LOG.error("ERROR: {}", e.getMessage());
			LOG.debug("Error while starting Monitor", e);
		}
		ThreadUtil.sleep(3000);
	}
	
	private void stopMonitorServer() {
		try {
			AgentMonitorServer.getInstance().stop();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testAddRemoveMonitorTargets() {
		//start monitor worker
		startMonitorServer(false);
		
		long startTime = NumberUtils.toLong(df.format(new Date()));
		Set<AgentInfo> agents = new HashSet<AgentInfo>();
		AgentInfo targetServer = new AgentInfo();
		targetServer.setIp("127.0.0.1");
		targetServer.setPort(MonitorConstants.DEFAULT_AGENT_PORT);
		agents.add(targetServer);
		monitorDataService.addMonitorTarget("127.0.0.1_test", agents);
		ThreadUtil.sleep(3000);
		long endTime = NumberUtils.toLong(df.format(new Date()));
		List<SystemDataModel> infoList = monitorService.getSystemMonitorData("127.0.0.1", startTime, endTime);
		assertThat(infoList.size(), greaterThan(0));
		
		monitorDataService.removeMonitorAgents("127.0.0.1_test");
		
		stopMonitorServer();
	}

	@Test
	public void testAddRemoveMonitorAgents() {
		//start monitor worker
		startMonitorServer(true);
		
		long startTime = NumberUtils.toLong(df.format(new Date()));
		Set<AgentInfo> agents = new HashSet<AgentInfo>();
		AgentInfo targetServer = new AgentInfo();
		targetServer.setIp("127.0.0.1");
		targetServer.setPort(MonitorConstants.DEFAULT_AGENT_PORT);
		agents.add(targetServer);
		monitorDataService.addMonitorAgent("127.0.0.1_test", agents);
		ThreadUtil.sleep(3000);
		long endTime = NumberUtils.toLong(df.format(new Date()));
		List<SystemDataModel> sysInfoList = monitorService.getSystemMonitorData("127.0.0.1", startTime, endTime);
		assertThat(sysInfoList.size(), greaterThan(0));
		List<JavaDataModel> javaInfoList = monitorService.getJavaMonitorData("127.0.0.1", startTime, endTime);
		assertThat(javaInfoList.size(), greaterThan(0));
		
		//try to add monitor again, should be ok.
		monitorDataService.addMonitorTarget("127.0.0.1_test", agents);
		
		monitorDataService.removeMonitorAgents("127.0.0.1_test");
		//try to remove again, should be ok.
		monitorDataService.removeMonitorAgents("127.0.0.1_test");
		
		stopMonitorServer();
	}

}
