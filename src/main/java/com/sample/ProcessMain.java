/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sample;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.drools.core.process.instance.impl.WorkItemImpl;
import org.kie.api.KieBase;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.RuntimeManagerFactory;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.internal.builder.KnowledgeBuilder;
import org.kie.internal.builder.KnowledgeBuilderFactory;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.runtime.StatefulKnowledgeSession;
import org.kie.internal.runtime.conf.AuditMode;
import org.kie.internal.runtime.conf.DeploymentDescriptor;
import org.kie.internal.runtime.manager.context.EmptyContext;
import org.kie.internal.runtime.manager.deploy.DeploymentDescriptorManager;
//import org.kie.test.util.db.PersistenceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a sample file to launch a process.
 */
public class ProcessMain {

	private static final Logger logger = LoggerFactory
			.getLogger(ProcessMain.class);
	private static final boolean usePersistence = true;

	public static final void main(String[] args) throws Exception {
		// load up the knowledge base
		KieBase kbase = readKnowledgeBase();
		KieSession kiession =kbase.newKieSession();
		CustomWebServiceWorkItemHandler customWebServiceWorkItemHandler=new CustomWebServiceWorkItemHandler();
	customWebServiceWorkItemHandler.setUsername("LyesTest");
	customWebServiceWorkItemHandler.setPassword("12345");
		//kiession, "LyesTest", "12345"
		WorkItemImpl workItemForSoap = new WorkItemImpl();
		workItemForSoap.setParameter("Url", "http://palu-svcbusintgint.prod.mrq:8280/services/MX00.TraiterCasWCF.Proxy?wsdl");
		workItemForSoap.setParameter("Endpoint", "http://palu-svcbusintgint.prod.mrq:8280/services/MX00.TraiterCasWCF.Proxy.MX00.TraiterCasWCF.ProxyHttpSoap11Endpoint");
		workItemForSoap.setParameter("Mode", "SYNC");
		workItemForSoap.setParameter("Namespace", "http://tempuri.org/");
		workItemForSoap.setParameter("Operation", "LancerCas");
		workItemForSoap.setParameter("Interface", "MX00.TraiterCasWCF.Proxy");
		   WorkItemManager managerForSoap = new TestWorkItemManager(workItemForSoap);
		   customWebServiceWorkItemHandler.executeWorkItem(workItemForSoap, managerForSoap);
		   System.out.println(workItemForSoap.getResults());
		  
		   /*StatefulKnowledgeSession ksession = newStatefulKnowledgeSession(kbase);
		
		ksession.startProcess("com.sample.bpmn.hello");
		logger.info("Process started ...");
		CustomRESTWorkItemHandler handler = new CustomRESTWorkItemHandler("LyesTest","12345");
		WorkItemImpl workItem = new WorkItemImpl();

      


        workItem.setParameter( "Url", "http://palu-svcbusintgcharge.prod.mrq:8280/MX00.TraiterCasAPI.api/TraiterCasAPI/LancerCas/3");

        workItem.setParameter( "Method", "GET" );
workItem.setParameter("Username", "LyesTest");
workItem.setParameter("Password", "12345");
workItem.setParameter("cookie", "ParamsInfra=%7B%22CelluleBanque%22%3A%220%22,%22CelluleBanquePreProdK1%22%3Anull,%22CleSuiviAppelService%22%3Anull,%22CodeUniteTache%22%3A%22CUTDefaut%22,%22CodeUniteTacheEncrypte%22%3A%22nfvyD%5C%2FPCx1OTV31ofUKeQNg9tkJdEAOL0z3qAGL9ZSDaJCDWbNhwr6N3oqLTFkEdyoLQWIpIYjpQD9kmzEi+tA%3D%3D%22,%22CompteOPS%22%3Atrue,%22DateProduction%22%3A%22%5C%2FDate(1578546000000-0500)%5C%2F%22,%22EstTraitementAsynchrone%22%3Afalse,%22ExecutionViaSQLDesign%22%3Afalse,%22GroupeCellule%22%3A%220%22,%22GroupeCellulePreProdK1%22%3Anull,%22JetonM2%22%3Anull,%22ModeCreation%22%3A0,%22Niveau%22%3A%22U%22,%22NumeroSandbox%22%3A%22%22,%22ParamsDiffere%22%3Anull,%22Phase%22%3A%221%22,%22SystemeAcesseur%22%3A%22MX%22,%22SystemeCleSuivi%22%3Anull,%22SystemePrincipal%22%3A%22GN%22,%22TraceOracle%22%3Afalse,%22TypeClientele%22%3A%22ClientRiche%22,%22TypeUtilisateur%22%3A%22Interne%22,%22Utilisateur%22%3A%22RDEH003%22,%22UtilisateurEncrypte%22%3A%222HeUg9S18qeqftYfOZgVls2JTHEX0D3Y3X%5C%2FdK3SNTNhQOUc8%5C%2F4%5C%2Fwk1lgGZSZuaQPHmB2iKvW3WSLL19oAi1a%5C%2Fw%3D%3D%22%7D; path=/; domain=\"\"; Expires=Fri, 1 Jan  2021 12:14:07 GMT;");
        

        WorkItemManager manager = new TestWorkItemManager(workItem);

        handler.executeWorkItem(workItem, manager);
      
System.out.println(workItem.getResults());*/

        

        

	}

	private static KieBase readKnowledgeBase() throws Exception {
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory
				.newKnowledgeBuilder();
		kbuilder.add(
				ResourceFactory.newClassPathResource("com/sample/sample.bpmn"),
				ResourceType.BPMN2);
		return kbuilder.newKieBase();
	}

	public static StatefulKnowledgeSession newStatefulKnowledgeSession(
			KieBase kbase) {
		RuntimeEnvironmentBuilder builder = null;
	/*	if (usePersistence) {
			Properties properties = new Properties();
			properties.put("driverClassName", "org.h2.Driver");
			properties.put("className", "org.h2.jdbcx.JdbcDataSource");
			properties.put("user", "sa");
			properties.put("password", "");
			properties.put("url", "jdbc:h2:tcp://localhost/~/jbpm-db");
			properties.put("datasourceName", "jdbc/jbpm-ds");
			//PersistenceUtil.setupPoolingDataSource(properties);
			Map<String, String> map = new HashMap<String, String>();
			map.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
			EntityManagerFactory emf = Persistence
					.createEntityManagerFactory("org.jbpm.persistence.jpa");
			builder = RuntimeEnvironmentBuilder.Factory.get()
					.newDefaultBuilder().entityManagerFactory(emf);
		} else {*/
			builder = RuntimeEnvironmentBuilder.Factory.get()
					.newDefaultInMemoryBuilder();
			DeploymentDescriptor descriptor = new DeploymentDescriptorManager()
					.getDefaultDescriptor().getBuilder()
					.auditMode(AuditMode.NONE).get();
			builder.addEnvironmentEntry("KieDeploymentDescriptor", descriptor);
	//	}
		builder.knowledgeBase(kbase);
		RuntimeManager manager = RuntimeManagerFactory.Factory.get()
				.newSingletonRuntimeManager(builder.get());
		return (StatefulKnowledgeSession) manager.getRuntimeEngine(
				EmptyContext.get()).getKieSession();
	}
}
