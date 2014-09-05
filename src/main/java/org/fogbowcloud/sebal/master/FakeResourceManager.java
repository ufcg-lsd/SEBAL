package org.fogbowcloud.sebal.master;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.fogbowcloud.manager.core.plugins.openstack.OpenStackIdentityPlugin;
import org.fogbowcloud.manager.occi.core.OCCIHeaders;
import org.fogbowcloud.manager.occi.core.Token;
import org.fogbowcloud.manager.occi.request.RequestAttribute;
import org.fogbowcloud.manager.occi.request.RequestConstants;
import org.fogbowcloud.manager.occi.request.RequestType;

public class FakeResourceManager {

	private Master master;
	private String url = "http://10.1.0.46";
	private String tokenPort = "5000";
	private String requestPort = "8182";
	private Token token;

	public FakeResourceManager(Master master) {
		this.master = master;
	}

	public void askForResource() throws URISyntaxException, HttpException,
	IOException {
		String createRequestResponse = createRequest();
		System.out.println(createRequestResponse);
		String requestId = getRequestId(createRequestResponse);
		getRequestInfo(requestId);
	}

	private void getRequestInfo(String requestId) throws URISyntaxException,
			HttpException, IOException {
		while(true) {
			String getRequestResponse = getRequest(requestId, token.getAccessId());
			try {
				Thread.sleep(5000);
				String[] split = getRequestResponse.split("=");
				String response = split[split.length - 1];
				response = response.replace("\"", "");
				System.out.println(response);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private String getRequestId(String createRequestResponse) {
		String requestUrl = createRequestResponse.split(":")[2];
		return requestUrl.split("/")[4];
	}

	public void allocateResource() {
		List<Task> tasks = master.getTasks();
		List<Resource> resources = master.getResources();
		for (int i = 0; i < resources.size(); i++) {
			FakeResource resource = (FakeResource) resources.get(i);
			if (master.getResourceState(resource.getId()).equals(
					Resource.State.IDLE)) {
				for (int j = 0; j < tasks.size(); j++) {
					TaskImpl task = (TaskImpl) tasks.get(j);
					if (master.getTaskState(task.getId()).equals(
							Task.State.OPEN)) {
						task.setResource(resource);
						master.setTaskState(task.getId(), Task.State.RUNNING);
						master.setResourceState(resource.getId(),
								Resource.State.BUSY);
						resource.execute(task);
						break;
					}
				}
			}
		}
	}

	private String createRequest() throws URISyntaxException, HttpException,
	IOException {
		String pubKey = IOUtils.toString(new FileInputStream(
				"/home/rafaelc/.ssh/id_rsa.pub"));
		Set<Header> headers = new HashSet<Header>();
		headers.add(new BasicHeader("Category", RequestConstants.TERM + "; scheme=\""
				+ RequestConstants.SCHEME + "\"; class=\"" + RequestConstants.KIND_CLASS
				+ "\""));
		headers.add(new BasicHeader("X-OCCI-Attribute", RequestAttribute.INSTANCE_COUNT
				.getValue() + "=" + 1));
		headers.add(new BasicHeader("X-OCCI-Attribute", RequestAttribute.TYPE.getValue()
				+ "=" + RequestType.PERSISTENT.getValue()));
		headers.add(new BasicHeader("Category", "fogbow_small" + "; scheme=\""
				+ RequestConstants.TEMPLATE_RESOURCE_SCHEME + "\"; class=\""
				+ RequestConstants.MIXIN_CLASS + "\""));
		headers.add(new BasicHeader("Category", "fogbow-linux-x86" + "; scheme=\""
				+ RequestConstants.TEMPLATE_OS_SCHEME + "\"; class=\""
				+ RequestConstants.MIXIN_CLASS + "\""));
		if (pubKey != null && !pubKey.isEmpty()) {

			headers.add(new BasicHeader("Category",
					RequestConstants.PUBLIC_KEY_TERM + "; scheme=\""
							+ RequestConstants.CREDENTIALS_RESOURCE_SCHEME
							+ "\"; class=\"" + RequestConstants.MIXIN_CLASS
							+ "\""));
			headers.add(new BasicHeader("X-OCCI-Attribute",
					RequestAttribute.DATA_PUBLIC_KEY.getValue() + "=" + pubKey));
		}
		OpenStackIdentityPlugin openStackIdentityPlugin = new OpenStackIdentityPlugin(
				new Properties());
		Map<String, String> credentials = new HashMap<String, String>();
		credentials.put("username", "sebal");
		credentials.put("password", "sebal");
		credentials.put("tenantName", "demo");
		credentials.put("authUrl", url + ":" + tokenPort);

		token = openStackIdentityPlugin.createToken(credentials);
		return doRequest("post", url + ":" + requestPort + "/" + RequestConstants.TERM,
				token.getAccessId(), headers);
	}

	private String doRequest(String method, String endpoint, String authToken)
			throws URISyntaxException, HttpException, IOException {
		return doRequest(method, endpoint, authToken, new HashSet<Header>());
	}

	private String doRequest(String method, String endpoint, String authToken,
			Set<Header> additionalHeaders) throws URISyntaxException,
			HttpException, IOException {
		HttpUriRequest request = null;
		if (method.equals("get")) {
			request = new HttpGet(endpoint);
		} else if (method.equals("delete")) {
			request = new HttpDelete(endpoint);
		} else if (method.equals("post")) {
			request = new HttpPost(endpoint);
		}
		request.addHeader(OCCIHeaders.CONTENT_TYPE,
				OCCIHeaders.OCCI_CONTENT_TYPE);
		if (authToken != null) {
			request.addHeader(OCCIHeaders.X_AUTH_TOKEN, authToken);
		}
		for (Header header : additionalHeaders) {
			request.addHeader(header);
		}
		HttpClient client = new DefaultHttpClient();
		HttpParams params = new BasicHttpParams();
		params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION,
				HttpVersion.HTTP_1_1);
		client = new DefaultHttpClient(new ThreadSafeClientConnManager(params,
				client.getConnectionManager().getSchemeRegistry()), params);
		HttpResponse response = null;
		try {
			response = client.execute(request);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			String requestResponse = EntityUtils.toString(response.getEntity());
			return requestResponse;
		} else {
			return response.getStatusLine().toString();
		}
	}

	private String getRequest(String requestId, String authToken)
			throws URISyntaxException, HttpException, IOException {
		if (requestId != null) {
			String endpoint = url + ":" + requestPort + "/" + RequestConstants.TERM + "/"
					+ requestId;
			return doRequest("get", endpoint, authToken);
		} else {
			return doRequest("get", url + "/" + RequestConstants.TERM,
					authToken);
		}
	}
}
