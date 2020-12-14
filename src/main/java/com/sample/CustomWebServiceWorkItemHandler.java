package com.sample;

import org.kie.api.runtime.KieSession;
import java.io.File;

import java.lang.reflect.Array;

import java.lang.reflect.Field;

import java.lang.reflect.Method;

import java.net.MalformedURLException;

import java.net.URL;

import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;

import java.util.HashMap;

import java.util.HashSet;

import java.util.List;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicReference;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;

import org.apache.cxf.configuration.security.AuthorizationPolicy;

import org.apache.cxf.endpoint.Client;

import org.apache.cxf.endpoint.ClientCallback;

import org.apache.cxf.endpoint.dynamic.DynamicClientFactory;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;

import org.apache.cxf.message.Message;

import org.apache.cxf.transport.http.HTTPConduit;

import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import org.drools.core.process.instance.impl.WorkItemImpl;

import org.jbpm.bpmn2.core.Bpmn2Import;

import org.jbpm.bpmn2.handler.WorkItemHandlerRuntimeException;

import org.jbpm.process.workitem.core.AbstractLogOrThrowWorkItemHandler;

import org.jbpm.process.workitem.core.util.Wid;

import org.jbpm.process.workitem.core.util.WidMavenDepends;

import org.jbpm.process.workitem.core.util.WidParameter;

import org.jbpm.process.workitem.core.util.WidResult;

import org.jbpm.process.workitem.core.util.service.WidAction;

import org.jbpm.process.workitem.core.util.service.WidAuth;

import org.jbpm.process.workitem.core.util.service.WidService;

import org.jbpm.workflow.core.impl.WorkflowProcessImpl;

import org.kie.api.runtime.KieSession;

import org.kie.api.runtime.manager.RuntimeEngine;

import org.kie.api.runtime.manager.RuntimeManager;

import org.kie.api.runtime.process.WorkItem;

import org.kie.api.runtime.process.WorkItemManager;

import org.kie.internal.runtime.Cacheable;

import org.kie.internal.runtime.manager.RuntimeManagerRegistry;

import org.kie.internal.runtime.manager.context.ProcessInstanceIdContext;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

public class CustomWebServiceWorkItemHandler extends AbstractLogOrThrowWorkItemHandler implements Cacheable {

	public static final String WSDL_IMPORT_TYPE = "http://schemas.xmlsoap.org/wsdl/";

	private static Logger logger = LoggerFactory.getLogger(CustomWebServiceWorkItemHandler.class);

	private final Long defaultJbpmCxfClientConnectionTimeout = Long
			.parseLong(System.getProperty("org.jbpm.cxf.client.connectionTimeout", "30000"));

	private final Long defaultJbpmCxfClientReceiveTimeout = Long
			.parseLong(System.getProperty("org.jbpm.cxf.client.receiveTimeout", "60000"));

	private ConcurrentHashMap<String, Client> clients = new ConcurrentHashMap<String, Client>();

	private DynamicClientFactory dcf = null;

	private KieSession ksession;

	private int asyncTimeout = 10;

	private ClassLoader classLoader;

	private String username;

	private String password;

	enum WSMode {

		SYNC,

		ASYNC,

		ONEWAY;

	}

	/**
	 * 
	 * Default constructor - no authentication nor ksession
	 * 
	 */

	public CustomWebServiceWorkItemHandler() {

		this.ksession = null;

		this.username = null;

		this.password = null;

	}

	/**
	 * 
	 * Used when no authentication is required
	 * 
	 * @param ksession - kie session
	 * 
	 */

	public CustomWebServiceWorkItemHandler(KieSession ksession) {

		this(ksession, null, null);

	}

	/**
	 * 
	 * Dedicated constructor when BASIC authentication method shall be used
	 * 
	 * @param kieSession - kie session
	 * 
	 * @param username   - basic auth username
	 * 
	 * @param password   - basic auth password
	 * 
	 */

	public CustomWebServiceWorkItemHandler(KieSession kieSession,

			String username,

			String password) {

		this.ksession = kieSession;

		this.username = username;

		this.password = password;

	}

	/**
	 * 
	 * Used when no authentication is required
	 * 
	 * @param ksession    - kie session
	 * 
	 * @param classloader - classloader to use
	 * 
	 */

	public CustomWebServiceWorkItemHandler(KieSession ksession,

			ClassLoader classloader) {

		this(ksession, classloader, null, null);

	}

	/**
	 * 
	 * Dedicated constructor when BASIC authentication method shall be used
	 * 
	 * @param ksession    - kie session
	 * 
	 * @param classloader - classloader to use
	 * 
	 * @param username    - basic auth username
	 * 
	 * @param password    - basic auth password
	 * 
	 */

	public CustomWebServiceWorkItemHandler(KieSession ksession,

			ClassLoader classloader,

			String username,

			String password) {

		this.ksession = ksession;

		this.classLoader = classloader;

		this.username = username;

		this.password = password;

	}

	/**
	 * 
	 * Used when no authentication is required
	 * 
	 * @param ksession - kie session
	 * 
	 * @param timeout  - connection timeout
	 * 
	 */

	public CustomWebServiceWorkItemHandler(KieSession ksession,

			int timeout) {

		this(ksession, timeout, null, null);

	}

	/**
	 * 
	 * Dedicated constructor when BASIC authentication method shall be used
	 * 
	 * @param ksession - kie session
	 * 
	 * @param timeout  - connection timeout
	 * 
	 * @param username - basic auth username
	 * 
	 * @param password - basic auth password
	 * 
	 */

	public CustomWebServiceWorkItemHandler(KieSession ksession,

			int timeout,

			String username,

			String password) {

		this.ksession = ksession;

		this.asyncTimeout = timeout;

		this.username = username;

		this.password = password;

	}

	/**
	 * 
	 * Used when no authentication is required
	 * 
	 * @param handlingProcessId - process id to handle exception
	 * 
	 * @param handlingStrategy  - strategy to be applied after handling exception
	 *                          process is completed
	 * 
	 * @param ksession          - kie session
	 * 
	 */

	public CustomWebServiceWorkItemHandler(String handlingProcessId,

			String handlingStrategy,

			KieSession ksession) {

		this(ksession, null, null);

		this.handlingProcessId = handlingProcessId;

		this.handlingStrategy = handlingStrategy;

	}

	/**
	 * 
	 * Dedicated constructor when BASIC authentication method shall be used
	 * 
	 * @param handlingProcessId - process id to handle exception
	 * 
	 * @param handlingStrategy  - strategy to be applied after handling exception
	 *                          process is completed
	 * 
	 * @param kieSession        - kie session
	 * 
	 * @param username          - basic auth username
	 * 
	 * @param password          - basic auth password
	 * 
	 */

	public CustomWebServiceWorkItemHandler(String handlingProcessId,

			String handlingStrategy,

			KieSession kieSession,

			String username,

			String password) {

		this.ksession = kieSession;

		this.username = username;

		this.password = password;

		this.handlingProcessId = handlingProcessId;

		this.handlingStrategy = handlingStrategy;

	}

	/**
	 * 
	 * Used when no authentication is required
	 * 
	 * @param handlingProcessId - process id to handle exception
	 * 
	 * @param handlingStrategy  - strategy to be applied after handling exception
	 *                          process is completed
	 * 
	 * @param ksession          - kie session
	 * 
	 * @param classloader       - classloader to use
	 * 
	 */

	public CustomWebServiceWorkItemHandler(String handlingProcessId,

			String handlingStrategy,

			KieSession ksession,

			ClassLoader classloader) {

		this(ksession, classloader, null, null);

		this.handlingProcessId = handlingProcessId;

		this.handlingStrategy = handlingStrategy;

	}

	/**
	 * 
	 * Dedicated constructor when BASIC authentication method shall be used
	 * 
	 * @param handlingProcessId - process id to handle exception
	 * 
	 * @param handlingStrategy  - strategy to be applied after handling exception
	 *                          process is completed
	 * 
	 * @param ksession          - kie session
	 * 
	 * @param classloader       - classloader to use
	 * 
	 * @param username          - basic auth username
	 * 
	 * @param password          - basic auth password
	 * 
	 */

	public CustomWebServiceWorkItemHandler(String handlingProcessId,

			String handlingStrategy,

			KieSession ksession,

			ClassLoader classloader,

			String username,

			String password) {

		this.ksession = ksession;

		this.classLoader = classloader;

		this.username = username;

		this.password = password;

		this.handlingProcessId = handlingProcessId;

		this.handlingStrategy = handlingStrategy;

	}

	/**
	 * 
	 * Used when no authentication is required
	 * 
	 * @param handlingProcessId - process id to handle exception
	 * 
	 * @param handlingStrategy  - strategy to be applied after handling exception
	 *                          process is completed
	 * 
	 * @param ksession          - kie session
	 * 
	 * @param timeout           - connection timeout
	 * 
	 */

	public CustomWebServiceWorkItemHandler(String handlingProcessId,

			String handlingStrategy,

			KieSession ksession,

			int timeout) {

		this(ksession, timeout, null, null);

		this.handlingProcessId = handlingProcessId;

		this.handlingStrategy = handlingStrategy;

	}

	/**
	 * 
	 * Dedicated constructor when BASIC authentication method shall be used
	 * 
	 * @param handlingProcessId - process id to handle exception
	 * 
	 * @param handlingStrategy  - strategy to be applied after handling exception
	 *                          process is completed
	 * 
	 * @param ksession          - kie session
	 * 
	 * @param timeout           - connection timeout
	 * 
	 * @param username          - basic auth username
	 * 
	 * @param password          - basic auth password
	 * 
	 */

	public CustomWebServiceWorkItemHandler(String handlingProcessId,

			String handlingStrategy,

			KieSession ksession,

			int timeout,

			String username,

			String password) {

		this.ksession = ksession;

		this.asyncTimeout = timeout;

		this.username = username;

		this.password = password;

		this.handlingProcessId = handlingProcessId;

		this.handlingStrategy = handlingStrategy;

	}

	public void executeWorkItem(WorkItem workItem,

			final WorkItemManager manager) {

		// since JaxWsDynamicClientFactory will change the TCCL we need to restore it
		// after creating client

		ClassLoader origClassloader = Thread.currentThread().getContextClassLoader();

		Object[] parameters = null;

		String interfaceRef = (String) workItem.getParameter("Interface");

		String operationRef = (String) workItem.getParameter("Operation");

		String endpointAddress = (String) workItem.getParameter("Endpoint");

		if (workItem.getParameter("Parameter") instanceof Object[]) {

			parameters = (Object[]) workItem.getParameter("Parameter");

		} else if (workItem.getParameter("Parameter") != null
				&& workItem.getParameter("Parameter").getClass().isArray()) {

			int length = Array.getLength(workItem.getParameter("Parameter"));

			parameters = new Object[length];

			for (int i = 0; i < length; i++) {

				parameters[i] = Array.get(workItem.getParameter("Parameter"),

						i);

			}

		} else {

			parameters = new Object[] { workItem.getParameter("Parameter") };

		}

		String modeParam = (String) workItem.getParameter("Mode");

		WSMode mode = WSMode.valueOf(modeParam == null ? "SYNC" : modeParam.toUpperCase());

		try {

			Client client = getWSClient(workItem,

					interfaceRef);

			if (client == null) {

				throw new IllegalStateException(
						"Unable to create client for web service " + interfaceRef + " - " + operationRef);

			}

			// Override endpoint address if configured.

			if (endpointAddress != null && !"".equals(endpointAddress)) {

				client.getRequestContext().put(Message.ENDPOINT_ADDRESS,

						endpointAddress);

			}

			// apply authorization if needed

			applyAuthorization(username, password, client);

			switch (mode) {

			case SYNC:

				Object[] result = client.invoke(operationRef,

						parameters);

				Map<String, Object> output = new HashMap<String, Object>();

				if (result == null || result.length == 0) {

					output.put("Result",

							null);

				} else {

					output.put("Result",

							result[0]);

				}

				logger.debug("Received sync response {} completeing work item {}",

						result,

						workItem.getId());

				manager.completeWorkItem(workItem.getId(),

						output);

				break;

			case ASYNC:

				final ClientCallback callback = new ClientCallback();

				final long workItemId = workItem.getId();

				final String deploymentId = nonNull(((WorkItemImpl) workItem).getDeploymentId());

				final long processInstanceId = workItem.getProcessInstanceId();

				client.invoke(callback,

						operationRef,

						parameters);

				new Thread(new Runnable() {

					public void run() {

						try {

							Object[] result = callback.get(asyncTimeout,

									TimeUnit.SECONDS);

							Map<String, Object> output = new HashMap<String, Object>();

							if (callback.isDone()) {

								if (result == null) {

									output.put("Result",

											null);

								} else {

									output.put("Result",

											result[0]);

								}

							}

							logger.debug("Received async response {} completeing work item {}",

									result,

									workItemId);

							RuntimeManager manager = RuntimeManagerRegistry.get().getManager(deploymentId);

							if (manager != null) {

								RuntimeEngine engine = manager
										.getRuntimeEngine(ProcessInstanceIdContext.get(processInstanceId));

								engine.getKieSession().getWorkItemManager().completeWorkItem(workItemId,

										output);

								manager.disposeRuntimeEngine(engine);

							} else {

								// in case there is no RuntimeManager available use available ksession,

								// as it might be used without runtime manager at all

								ksession.getWorkItemManager().completeWorkItem(workItemId,

										output);

							}

						} catch (Exception e) {

							e.printStackTrace();

							throw new RuntimeException("Error encountered while invoking ws operation asynchronously",

									e);

						}

					}

				}).start();

				break;

			case ONEWAY:

				ClientCallback callbackFF = new ClientCallback();

				client.invoke(callbackFF,

						operationRef,

						parameters);

				logger.debug("One way operation, not going to wait for response, completing work item {}",

						workItem.getId());

				manager.completeWorkItem(workItem.getId(),

						new HashMap<String, Object>());

				break;

			default:

				break;

			}

		} catch (Exception e) {

			handleException(e);

		} finally {

			Thread.currentThread().setContextClassLoader(origClassloader);

		}

	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@SuppressWarnings("unchecked")

	protected Client getWSClient(WorkItem workItem,

			String interfaceRef) {

		if (clients.containsKey(interfaceRef)) {

			return clients.get(interfaceRef);

		}

		synchronized (this) {

			if (clients.containsKey(interfaceRef)) {

				return clients.get(interfaceRef);

			}

			String importLocation = (String) workItem.getParameter("Url");

			String importNamespace = (String) workItem.getParameter("Namespace");

			if (importLocation != null && importLocation.trim().length() > 0

					&& importNamespace != null && importNamespace.trim().length() > 0) {

				Client client = getDynamicClientFactory().createClient(importLocation,

						new QName(importNamespace,

								interfaceRef),

						getInternalClassLoader(),

						null);

				setClientTimeout(workItem, client);

				clients.put(interfaceRef,

						client);

				return client;

			}

			long processInstanceId = ((WorkItemImpl) workItem).getProcessInstanceId();

			WorkflowProcessImpl process = ((WorkflowProcessImpl) ksession.getProcessInstance(processInstanceId)
					.getProcess());

			List<Bpmn2Import> typedImports = (List<Bpmn2Import>) process.getMetaData("Bpmn2Imports");

			if (typedImports != null) {

				Client client = null;

				for (Bpmn2Import importObj : typedImports) {

					if (WSDL_IMPORT_TYPE.equalsIgnoreCase(importObj.getType())) {

						try {

							client = getDynamicClientFactory().createClient(importObj.getLocation(),

									new QName(importObj.getNamespace(),

											interfaceRef),

									getInternalClassLoader(),

									null);

							setClientTimeout(workItem, client);

							clients.put(interfaceRef,

									client);

							return client;

						} catch (Exception e) {

							logger.error("Error when creating WS Client",

									e);

							continue;

						}

					}

				}

			}

		}

		return null;

	}

	private void setClientTimeout(WorkItem workItem, Client client) {

		HTTPConduit conduit = (HTTPConduit) client.getConduit();

		HTTPClientPolicy policy = conduit.getClient();

		long connectionTimeout = defaultJbpmCxfClientConnectionTimeout;

		String connectionTimeoutStr = (String) workItem.getParameter("ConnectionTimeout");

		if (connectionTimeoutStr != null && !connectionTimeoutStr.trim().isEmpty()) {

			connectionTimeout = Long.valueOf(connectionTimeoutStr);

		}

		long receiveTimeout = defaultJbpmCxfClientReceiveTimeout;

		String receiveTimeoutStr = (String) workItem.getParameter("ReceiveTimeout");

		if (receiveTimeoutStr != null && !receiveTimeoutStr.trim().isEmpty()) {

			receiveTimeout = Long.valueOf(receiveTimeoutStr);

		}

		logger.debug("connectionTimeout = {}, receiveTimeout = {}", connectionTimeout, receiveTimeout);

		policy.setConnectionTimeout(connectionTimeout);

		policy.setReceiveTimeout(receiveTimeout);

	}

	protected synchronized DynamicClientFactory getDynamicClientFactory() {

		if (this.dcf == null) {

			this.dcf = JaxWsDynamicClientFactory.newInstance();

		}

		return this.dcf;

	}

	public void abortWorkItem(WorkItem workItem,

			WorkItemManager manager) {

		// Do nothing, cannot be aborted

	}

	/*
	 * CXF builds compiler classpath assuming that the hierarchy of ClassLoader is
	 * composed of URLClassLoader instances.
	 * 
	 * Since ModuleClassLoader does not implement URLClassLoader, we need to provide
	 * an alternative way of retrieving these URLS
	 * 
	 * so CXF can build a proper classpath, avoiding the issue mentioned below.
	 * 
	 * @see https://issues.apache.org/jira/browse/CXF-7925
	 * 
	 */

	@SuppressWarnings("squid:S1872")

	private ClassLoader getInternalClassLoader() {

		ClassLoader cl = this.classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader(),
				parent = cl;

		Collection<File> uris = new HashSet<>();

		do {

			if (parent.getClass().getSimpleName().equals("ModuleClassLoader")) {

				try {

					getJarsFromModuleClassLoader(parent, uris);

				} catch (ReflectiveOperationException e) {

					throw new WorkItemHandlerRuntimeException(e,
							"Problem calculating list of URLs from ModuleClassLoader");

				}

			}

			parent = parent.getParent();

		} while (parent != null);

		if (!uris.isEmpty()) {

			cl = new CXFJavaCompileClassLoader(uris, cl);

		}

		return cl;

	}

	private static class CXFJavaCompileClassLoader extends URLClassLoader {

		private URL[] jarUrls;

		public CXFJavaCompileClassLoader(Collection<File> files, ClassLoader parent) {

			super(new URL[0], parent);

			this.jarUrls = files.stream().map(CXFJavaCompileClassLoader::toUrl).toArray(URL[]::new);

		}

		@Override

		public URL[] getURLs() {

			return jarUrls;

		}

		private static URL toUrl(File file) {

			try {

				return file.toURI().toURL();

			} catch (MalformedURLException e) {

				throw new WorkItemHandlerRuntimeException(e, "Problem converting file to URL: " + file);

			}

		}

	}

	/*
	 * This method makes assumptions over the internal structure of
	 * ModuleClassLoader. If this class is changed, this method
	 * 
	 * will need to change accordingly
	 * 
	 */

	@SuppressWarnings({ "squid:S3740", "squid:S3011" })

	private void getJarsFromModuleClassLoader(ClassLoader cl, Collection<File> collector)
			throws ReflectiveOperationException {

		AtomicReference<?> paths = (AtomicReference<?>) getFieldValue(cl, "paths");

		Object sourceList = getFieldValue(paths.get(), "sourceList");

		int size = Array.getLength(sourceList);

		Method getVFSResource = null;

		Method getPhysicalFile = null;

		Field rootField = null;

		Field rootNameField = null;

		for (int i = 0; i < size; i++) {

			Object resource = Array.get(sourceList, i);

			if (getVFSResource == null) {

				getVFSResource = resource.getClass().getDeclaredMethod("getResourceLoader");

				getVFSResource.setAccessible(true);

			}

			resource = getVFSResource.invoke(resource);

			if (rootField == null) {

				Class<?> resourceClass = resource.getClass();

				rootField = resourceClass.getDeclaredField("root");

				rootNameField = resourceClass.getDeclaredField("rootName");

				rootField.setAccessible(true);

				rootNameField.setAccessible(true);

			}

			String rootName = (String) rootNameField.get(resource);

			if (rootName.endsWith("jar")) {

				Object root = rootField.get(resource);

				if (getPhysicalFile == null) {

					getPhysicalFile = root.getClass().getDeclaredMethod("getPhysicalFile");

					getPhysicalFile.setAccessible(true);

				}

				collector.add(new File(((File) getPhysicalFile.invoke(root)).getParentFile(), rootName));

			}

		}

	}

	@SuppressWarnings("squid:S3011")

	private Object getFieldValue(Object container, String fieldName)
			throws NoSuchFieldException, IllegalAccessException {

		Field field = container.getClass().getDeclaredField(fieldName);

		field.setAccessible(true);

		return field.get(container);

	}

	public ClassLoader getClassLoader() {

		return classLoader;

	}

	public void setClassLoader(ClassLoader classLoader) {

		this.classLoader = classLoader;

	}

	protected String nonNull(String value) {

		if (value == null) {

			return "";

		}

		return value;

	}

	@Override

	public void close() {

		if (clients != null) {

			for (Client client : clients.values()) {

				client.destroy();

			}

		}

	}

	public static String ENV="<ContexteSession>&lt;?xml version=\"1.0\" encoding=\"utf-8\"?&gt;&lt;ParametresInfrastructure xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"&gt;&lt;SystemePrincipal&gt;K5&lt;/SystemePrincipal&gt;&lt;CodeUniteTache&gt;K5B01&lt;/CodeUniteTache&gt;&lt;Utilisateur&gt;RROY004&lt;/Utilisateur&gt;&lt;CleSuivi /&gt;&lt;Niveau&gt;U&lt;/Niveau&gt;&lt;Phase&gt;2&lt;/Phase&gt;&lt;GroupeCellule&gt;A&lt;/GroupeCellule&gt;&lt;CelluleBanque&gt;A&lt;/CelluleBanque&gt;&lt;DateProd&gt;2015-10-09T00:00:00-04:00&lt;/DateProd&gt;&lt;TypeUtilisateur&gt;Interne&lt;/TypeUtilisateur&gt;&lt;TypeClientele&gt;EchangesElectroniques&lt;/TypeClientele&gt;&lt;CompteOPS&gt;false&lt;/CompteOPS&gt;&lt;EstTraitementAsynchrone&gt;false&lt;/EstTraitementAsynchrone&gt;&lt;ModeCreation&gt;2&lt;/ModeCreation&gt;&lt;ExecutionViaSQLDesign&gt;false&lt;/ExecutionViaSQLDesign&gt;&lt;TraceOracle&gt;false&lt;/TraceOracle&gt;&lt;GdApp&gt;7038114a-1ac8-43ca-8429-8b24ef71a886&lt;/GdApp&gt;&lt;/ParametresInfrastructure&gt;</ContexteSession>";
	protected void applyAuthorization(String userName, String password, Client client) {

		if (userName != null && password != null) {

			HTTPConduit httpConduit = (HTTPConduit) client.getConduit();

			AuthorizationPolicy authorizationPolicy = new AuthorizationPolicy();

			authorizationPolicy.setUserName("LyesTest");

			authorizationPolicy.setPassword("12345");

			authorizationPolicy.setAuthorizationType("Basic");
			client.getRequestContext().put("ws-security.username", "LyesTest");
			client.getRequestContext().put("ws-security.password", "12345");
			client.getInInterceptors().add(new LoggingInInterceptor());
			client.getOutInterceptors().add(new LoggingOutInterceptor());
			
			List<Header> headers = new ArrayList<Header>();
			Header dummyHeader;
			try {
				dummyHeader = new Header(new QName("xmlns:http://revenu.gouv.qc.ca/HeaderContexteSession", "HeaderContexteSession"), ENV,
						new JAXBDataBinding(String.class));
			    
				headers.add(dummyHeader);

				// server side:
				//context.getMessageContext().put(Header.HEADER_LIST, headers);

				// client side:
				client.getRequestContext().put(Header.HEADER_LIST, headers);
				System.out.println("#########"+client.getRequestContext().toString());
			} catch (JAXBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			//client.getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS, headers);
			httpConduit.setAuthorization(authorizationPolicy);

		} else {

			logger.warn("UserName and Password must be provided to set the authorization policy.");

		}

	}

	public void setClients(ConcurrentHashMap<String, Client> clients) {

		this.clients = clients;

	}

}