package com.sap.cf.f1;

import org.cloudfoundry.identity.uaa.oauth.token.CompositeAccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.cloudfoundry.identity.client.*;
import org.cloudfoundry.identity.client.token.*;

import java.net.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import com.sap.cloud.sdk.cloudplatform.logging.CloudLoggerFactory;

//https://help.sap.com/viewer/cca91383641e40ffbe03bdc78f00f681/Cloud/en-US/313b215066a8400db461b311e01bd99b.html

@WebServlet("/hello")
public class HelloWorldServlet extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(HelloWorldServlet.class);

    @Override
    protected void doGet( final HttpServletRequest request, final HttpServletResponse response )
        throws ServletException, IOException
    {
        response.getWriter().write("<br />I am in!");

        JSONObject jsonObj = new JSONObject(System.getenv("VCAP_SERVICES"));
        JSONArray jsonArr = jsonObj.getJSONArray("connectivity");
        JSONObject credentials = jsonArr.getJSONObject(0).getJSONObject("credentials");

        // get value of "onpremise_proxy_host" and "onpremise_proxy_port" from the environment variables
        // and create on-premise HTTP proxy
        String connProxyHost = credentials.getString("onpremise_proxy_host");
        int connProxyPort = Integer.parseInt(credentials.getString("onpremise_proxy_port"));
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(connProxyHost, connProxyPort));

        response.getWriter().write("<br />ProxyHost = " + connProxyHost + ":" + connProxyPort);
        // create URL to the remote endpoint you like to call:
// virtualhost:1234 is defined as an endpoint in the Cloud Connector, as described in the Required Information section
        URL url = new URL("http://gm4.virtual:44355/sap/opu/odata/sap/ERP_UTILITIES_UMC/$metadata");

        JSONArray UaaJsonArr = jsonObj.getJSONArray("xsuaa");
        JSONObject UaaCredentials = UaaJsonArr.getJSONObject(0).getJSONObject("credentials");

        // get value of "clientid" and "clientsecret" from the environment variables
        String clientid = credentials.getString("clientid");
        String clientsecret = credentials.getString("clientsecret");

// get the URL to xsuaa from the environment variables
        URI xsuaaUrl = null;
        try {
            xsuaaUrl = new URI(UaaCredentials.getString("url"));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

// make request to UAA to retrieve access token
        UaaContextFactory factory = UaaContextFactory.factory(xsuaaUrl).authorizePath("/oauth/authorize").tokenPath("/oauth/token");
        TokenRequest tokenRequest = factory.tokenRequest();
        tokenRequest.setGrantType(GrantType.CLIENT_CREDENTIALS);
        tokenRequest.setClientId(clientid);
        tokenRequest.setClientSecret(clientsecret);
        UaaContext xsuaaContext = factory.authenticate(tokenRequest);
        CompositeAccessToken accessToken = xsuaaContext.getToken();


// create the connection object to the endpoint using the proxy
// this does not open a connection but only creates a connection object, which can be modified later, before actually connecting
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection(proxy);

        // set access token as Proxy-Authorization header in the URL connection
//        urlConnection.setRequestProperty("Proxy-Authorization", "Bearer " + accessToken);
        response.getWriter().write("<br />Access token:" + accessToken.toString());

        urlConnection.connect();
        int backendResponseCode = urlConnection.getResponseCode();

        logger.info("I am running!");
        response.getWriter().write("<br />Connection result = " + backendResponseCode);
    }
}
