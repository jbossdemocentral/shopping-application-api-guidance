package com.redhat.shopping.demo.application.services;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.gdata.util.AuthenticationException;
import com.redhat.shopping.demo.application.beans.User;

@RequestMapping("/")
public class ApplicationController {
	
	private static final Logger LOGGER = Logger.getLogger(ApplicationController.class);
	
	@Value(value="${restservice.url.client}")
	private String restServiceUrl;
	
	@Value(value="${gauth.callback.url}")
	private String callBackUrl;
	
	@Autowired
	private TokenLoginService service;

	@RequestMapping(method = RequestMethod.GET, value = { "buy" })
	public String handleBuy(HttpServletRequest request, Model model)
			throws Exception {
		String productCode = request.getParameter("productCode");
		User userDetails = (User) request.getSession().getAttribute(
				"userDetails");
		String url = createBuyUrl(productCode, userDetails);
		StringBuffer buyResult = fetchRestResponse(getApplicationServiceUrl(request)+url);

		model.addAttribute("buyResult", buyResult.toString());
		return "/homePage.jsp";
	}

	private String createBuyUrl(String productCode, User userDetails)
			throws UnsupportedEncodingException {
		String url = "products/";
		if (validateString(productCode)) {
			url = url.concat(productCode);
		}
		url = url.concat("/buy");
		if (userDetails != null) {
			url = url.concat("?customerDetails="
					+ URLEncoder.encode(userDetails.getEmailId(), "UTF-8"));
		}

		return url;
	}

	private boolean validateString(String str) {
		return str != null && !str.isEmpty();
	}

	@RequestMapping(method = RequestMethod.GET, value = { "authenticate" })
	public String handleAuthenticate(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) throws Exception {
		model.put("message", "No OAuth access token available");
		model.put("callBackUrl", URLEncoder
								.encode(getBaseUrl(request)+callBackUrl,"UTF-8"));
		return "/authorize.jsp";
	}

	@RequestMapping(method = RequestMethod.GET, value = { "showTransactions" })
	public String handlePreviousTransactions(
			HttpServletRequest request, ModelMap model)
			throws Exception {
		User userDetails = (User) request.getSession().getAttribute(
				"userDetails");
		StringBuffer lastTransactions = fetchRestResponse(getApplicationServiceUrl(request)+"showTransactions"
				+ "?customerDetails="
				+ URLEncoder.encode(userDetails.getEmailId(), "UTF-8"));
		model.addAttribute("lastTransactions", lastTransactions.toString());
		return "/homePage.jsp";
	}

	@RequestMapping(method = RequestMethod.GET, value = { "postAuthenticate" })
	public String handlePostAuthenticate(HttpServletRequest request,
			HttpServletResponse response, ModelMap model) throws Exception {
		User userDetails = null;
		try {
			userDetails = service.getContactNames(getAccessToken(request),
					getAccessTokenSecret(request));
		} catch (AuthenticationException e) {
			model.put("message", "OAuth access token invalid");
			model.put("callBackUrl", URLEncoder
					.encode(getBaseUrl(request)+callBackUrl,"UTF-8"));
			return "/authorize.jsp";
		}
		request.getSession().setAttribute("userDetails", userDetails);
		return "/homePage.jsp";
	}

	@RequestMapping(method = RequestMethod.GET, value = { "show" })
	public String handleShow(HttpServletRequest request,ModelMap model) throws Exception {
		String url = getApplicationServiceUrl(request).concat("products/");
		LOGGER.info("URL for show: " + url);
		StringBuffer showProducts = fetchRestResponse(url);
		model.addAttribute("showProducts", showProducts.toString());
		return "/homePage.jsp";
	}

	private  StringBuffer fetchRestResponse(String url)
			throws IOException, ClientProtocolException {
		HttpClient client = new DefaultHttpClient();
		HttpGet request = new HttpGet(url);
		HttpResponse response = client.execute(request);
		StringBuffer responseString = new StringBuffer(IOUtils.toString(response
				.getEntity().getContent()));
		
		return responseString;
	}

	private static String getAccessToken(HttpServletRequest request) {
		return getCookieValue(request.getCookies(), "ACCESS-TOKEN");
	}

	private static String getAccessTokenSecret(HttpServletRequest request) {
		return getCookieValue(request.getCookies(), "ACCESS-TOKEN-SECRET");
	}

	private static String getCookieValue(Cookie[] cookies, String name) {
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (name.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	public String getRestServiceUrl() {
		return restServiceUrl;
	}

	public void setRestServiceUrl(String restServiceUrl) {
		this.restServiceUrl = restServiceUrl;
	}

	public String getApplicationServiceUrl(HttpServletRequest request){
		return getBaseUrl(request).concat(getRestServiceUrl());
	}

	private String getBaseUrl(HttpServletRequest request) {
		String baseUrl = String.format("%s://%s:%d%s",request.getScheme(),  request.getServerName(), request.getServerPort(),request.getContextPath());
		return baseUrl;
	}

	public String getCallBackUrl() {
		return callBackUrl;
	}

	public void setCallBackUrl(String callBackUrl) {
		this.callBackUrl = callBackUrl;
	}

}
