package com.ft.methodeapi.service.methode;

import com.ft.methodeapi.MethodeApiConfiguration;
import com.ft.methodeapi.MethodeApiService;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.yammer.dropwizard.testing.junit.DropwizardServiceRule;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;

public class MethodeContentRetrievalHealthCheckIT {

	@ClassRule
	public static final DropwizardServiceRule<MethodeApiConfiguration> serviceRule = new DropwizardServiceRule<>(MethodeApiService.class, "methode-api-wrong-nsport.yaml");

	@Test
	public void shouldTimeOutWhenInvalidPort() {
        final Client client = Client.create();
        client.setReadTimeout(10000);
        final URI uri = buildHealthCheckUri();
        final ClientResponse clientResponse = client.resource(uri).get(ClientResponse.class);
        assertThat("response", clientResponse, hasProperty("status", equalTo(500)));
	}

	private URI buildHealthCheckUri() {
		return UriBuilder
				.fromPath("healthcheck")
				.scheme("http")
				.host("localhost")
				.port(adminPort())
				.build();
	}

	private int adminPort() {
		return serviceRule.getConfiguration().getHttpConfiguration().getAdminPort();
	}
}
