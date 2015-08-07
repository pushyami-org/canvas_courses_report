package edu.umich.tl.analytics;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.umich.tl.utils.ReportUtilities;


public class CanvasCourseReportFilter implements Filter {
	private static Log M_log = LogFactory.getLog(CanvasCourseReportFilter.class);
	private static final String OU_GROUPS = "ou=Groups";
	private static final String LDAP_CTX_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
	private static final String SYSTEM_PROPERTY_FILE_PATH = "canvasCourseReportPath";
	protected static Properties appExtPropertiesFile;
	protected static final String PROPERTY_CANVAS_URL = "canvas.url";
	protected static final String PROPERTY_CANVAS_ADMIN="canvas.admin.token";
	protected static final String PROPERTY_USE_TEST_URL = "use.test.url";
	protected static final String PROPERTY_LDAP_SERVER_URL = "ldap.server.url";
	private static final String PROPERTY_AUTH_GROUP = "mcomm.group";
	private String providerURL = null;
	private String mcommunityGroup = null;
	protected static String canvasURL = null;
	protected static String canvasToken = null;
	private boolean isTestUrlEnabled=false;
	private static final String FALSE = "false";
	private static final String TEST_USER = "testUser";

	public void init(FilterConfig arg0) throws ServletException {
		M_log.debug("Filter init(): called");
		getExternalAppProperties();
	}

	private void getExternalAppProperties() {
		M_log.debug("getExternalAppProperties(): called");
		String propertiesFilePath = System.getProperty(SYSTEM_PROPERTY_FILE_PATH);
		if (!ReportUtilities.isEmpty(propertiesFilePath)) {
			appExtPropertiesFile=ReportUtilities.getPropertiesFromURL(propertiesFilePath);
			if(appExtPropertiesFile!=null) {
				isTestUrlEnabled = Boolean.parseBoolean(appExtPropertiesFile.getProperty(PROPERTY_USE_TEST_URL,FALSE));
				providerURL=appExtPropertiesFile.getProperty(PROPERTY_LDAP_SERVER_URL);
				mcommunityGroup=appExtPropertiesFile.getProperty(PROPERTY_AUTH_GROUP);
				canvasURL = appExtPropertiesFile.getProperty(PROPERTY_CANVAS_URL);
				canvasToken = appExtPropertiesFile.getProperty(PROPERTY_CANVAS_ADMIN);
			}else {
				M_log.error("Failed to load application properties from canvasReport.properties");
			}
			
		}else {
			M_log.error("File path for (canvasReport.properties) is not provided");
		}
	}

	public void doFilter(ServletRequest sReq, ServletResponse sRes, FilterChain chain)
			throws IOException, ServletException {
		M_log.debug("doFilter: Called");
		HttpServletRequest useRequest = (HttpServletRequest) sReq;
		HttpServletResponse useResponse=(HttpServletResponse)sRes;
		if(!checkForAuthorization(useRequest)) {
			useResponse.sendError(403);
			return;
		}
		useRequest.setAttribute("canvasURL",canvasURL);
		chain.doFilter(useRequest, useResponse);
		
	}
	private boolean checkForAuthorization(HttpServletRequest request) {
		 M_log.debug("checkLdapForAuthorization(): called");
		 String remoteUser = request.getRemoteUser();
		 String testUser = request.getParameter(TEST_USER);
		 boolean isAuthorized = false;
		 String user=null;

		 String testUserInSession = (String)request.getSession().getAttribute(TEST_USER);
		 String sessionId = request.getSession().getId();

		 if ( isTestUrlEnabled && testUser != null ) { 
			 user=testUser;
			 request.getSession().setAttribute(TEST_USER, testUser);
		 }
		 else if ( isTestUrlEnabled && testUserInSession != null ){
			 user=testUserInSession;
		 } 
		 if  ( !isAuthorized && remoteUser != null ) {
			 user=remoteUser;
			 M_log.info(String.format("The session id \"%s\" of the Person with uniqname  \"%s\" issuing the request" ,sessionId,remoteUser));
		 }
		 isAuthorized=ldapAuthorizationVerification(user); 
		 return isAuthorized;
	}
	
	private boolean ldapAuthorizationVerification(String user)  {
		M_log.debug("ldapAuthorizationVerification(): called");
		boolean isAuthorized = false;
		DirContext dirContext=null;
		NamingEnumeration listOfPeopleInAuthGroup=null;
		NamingEnumeration allSearchResultAttributes=null;
		NamingEnumeration simpleListOfPeople=null;
		Hashtable<String,String> env = new Hashtable<String, String>();
		if(!ReportUtilities.isEmpty(providerURL) && !ReportUtilities.isEmpty(mcommunityGroup)) {
		env.put(Context.INITIAL_CONTEXT_FACTORY, LDAP_CTX_FACTORY);
		env.put(Context.PROVIDER_URL, providerURL);
		}else {
			M_log.error(" [ldap.server.url] or [mcomm.group] properties are not set, review the canvasReport.properties file");
			return isAuthorized;
		}
		try {
			dirContext = new InitialDirContext(env);
			String[] attrIDs = {"member"};
			SearchControls searchControls = new SearchControls();
			searchControls.setReturningAttributes(attrIDs);
			searchControls.setReturningObjFlag(true);
			searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			String searchBase = OU_GROUPS;
			String filter = "(&(cn=" + mcommunityGroup + ") (objectclass=rfc822MailGroup))";
			listOfPeopleInAuthGroup = dirContext.search(searchBase, filter, searchControls);
			String positiveMatch = "uid=" + user + ",";
			outerloop:
			while (listOfPeopleInAuthGroup.hasMore()) {
				SearchResult searchResults = (SearchResult)listOfPeopleInAuthGroup.next();
				allSearchResultAttributes = (searchResults.getAttributes()).getAll();
				while (allSearchResultAttributes.hasMoreElements()){
					Attribute attr = (Attribute) allSearchResultAttributes.nextElement();
					simpleListOfPeople = attr.getAll();
					while (simpleListOfPeople.hasMoreElements()){
						String val = (String) simpleListOfPeople.nextElement();
						if(val.indexOf(positiveMatch) != -1){
							isAuthorized = true;
							break outerloop;
						}
					}
				}
			}
			return isAuthorized;
		} catch (NamingException e) {
			M_log.error("Problem getting attribute:" + e);
			return isAuthorized;
		}
		finally {
			try {
				if(simpleListOfPeople!=null) {
				simpleListOfPeople.close();
				}
			} catch (NamingException e) {
				M_log.error("Problem occurred while closing the NamingEnumeration list \"simpleListOfPeople\" list ",e);
			}
			try {
				if(allSearchResultAttributes!=null) {
				allSearchResultAttributes.close();
				}
			} catch (NamingException e) {
				M_log.error("Problem occurred while closing the NamingEnumeration \"allSearchResultAttributes\" list ",e);
			}
			try {
				if(listOfPeopleInAuthGroup!=null) {
				listOfPeopleInAuthGroup.close();
				}
			} catch (NamingException e) {
				M_log.error("Problem occurred while closing the NamingEnumeration \"listOfPeopleInAuthGroup\" list ",e);
			}
			try {
				if(dirContext!=null) {
				dirContext.close();
				}
			} catch (NamingException e) {
				M_log.error("Problem occurred while closing the  \"dirContext\"  object",e);
			}
		}
		
	}

	public void destroy() {
		
	}


}
