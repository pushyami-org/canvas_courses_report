package edu.umich.tl.analytics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;




public class CanvasCourseReportServlet extends HttpServlet {
	
	private static final String ROOT = "root";
	private static final long serialVersionUID = 8770051932039446255L;
	private static Log M_log = LogFactory.getLog(CanvasCourseReportServlet.class);
	
	private static final String ACTION = "action";
	private static final String TERM = "term";
	private static final String TERM_NAME = "termName";
	private static final String ACTION_GET_COURSES = "getCoursesPublished";
	private static final String ACTION_GET_TERMS = "getEnrollmentTerms";
	private static final String NONE = "NONE";
	private static final String EMAIL_SUFFIX="@umich.edu";
	private String canvasToken=CanvasCourseReportFilter.canvasToken;
	private String canvasURL=CanvasCourseReportFilter.canvasURL;
	ResourceBundle props = ResourceBundle.getBundle("coursereport");
	
	public void init() {
		M_log.debug("init(): called");
	}
	

	protected void doGet(HttpServletRequest request,HttpServletResponse response){
		M_log.debug("doGet: Called");
		String requestedAction = request.getParameter(ACTION);
		M_log.debug("Action : "+requestedAction);
		if(requestedAction.equals(ACTION_GET_COURSES)) {
			getPublishedCourses(request,response);
		}else if(requestedAction.equals(ACTION_GET_TERMS)) {
			enrollmentTermsLogic(response);
		}else {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			try {
				response.getWriter().print(props.getString("get.error"));
			} catch (IOException e) {
				M_log.error("Request action unknown: \"" + requestedAction
						+ "\"",e);
			}
		}
		
	}
	private void getPublishedCourses(HttpServletRequest request,HttpServletResponse response) {
		M_log.debug("getPublishedCourses: Called");
		String term = request.getParameter(TERM);
	    String termName = request.getParameter(TERM_NAME);
		String subAccountUrl = canvasURL+"/api/v1/accounts/1/sub_accounts?recursive=true&per_page=100";
		String url = canvasURL+"/api/v1/accounts/1/courses?enrollment_term_id="+term+"&published=true&per_page=100";
		CoursesForSubaccounts cfs =new CoursesForSubaccounts();
		getSubAccountInfo(subAccountUrl,cfs);
		getThePublishedCourseList(url,cfs);
		response.setContentType("text/csv");
		response.setHeader("Content-Disposition","attachment;filename=PublishedCanvasCoursesWithInstructorsFor"+termName+".csv");
		try {
			OutputStream outputStream = response.getOutputStream();
			String outputResult = generateCSVFile(cfs).toString();
			outputStream.write(outputResult.getBytes());
			outputStream.flush();
			outputStream.close();
		} catch (IOException e) {
			M_log.error("Writing the csv file to the has some errror: ",e);
		}
	}

	private StringBuilder generateCSVFile(CoursesForSubaccounts cfs) {
		M_log.debug("generateCSVFile: Called");
		StringBuilder writer = new StringBuilder();
		ArrayList<Course> courses = cfs.getCourse();
		writer.append(Course.getCourseHeader());
		for (Course course : courses) {
			writer.append(course.getCourseValues());
		}
		return writer;
		
	}
	

	private HttpResponse doApiCall(String url) {
		HttpUriRequest clientRequest = null;
		HttpResponse httpResponse=null;
		try {
			clientRequest = new HttpGet(url);
			HttpClient client = new DefaultHttpClient();
			final ArrayList<NameValuePair> nameValues = new ArrayList<NameValuePair>();
			nameValues.add(new BasicNameValuePair("Authorization", "Bearer" + " " + canvasToken));
			nameValues.add(new BasicNameValuePair("content-type", "application/json"));
			for (final NameValuePair h : nameValues) {
				clientRequest.addHeader(h.getName(), h.getValue());
			}
			try {
				 httpResponse = client.execute(clientRequest);

			} catch (IOException e) {
				M_log.error("Canvas API call did not complete successfully", e);
			}
		} catch (Exception e) {
			M_log.error("GET request has some exceptions", e);
		}
		return httpResponse;
	}
	private void getThePublishedCourseList(String url, CoursesForSubaccounts cfs) {
		M_log.debug("getThePublishedCourseList: Called");
		HttpResponse httpResponse = doApiCall(url);
		HttpEntity entity = httpResponse.getEntity();
		ObjectMapper mapper = new ObjectMapper();
		List<HashMap<String, Object>> courseList=new ArrayList<HashMap<String, Object>>();
		try {
			String jsonResponseString = EntityUtils.toString(entity);
			courseList = mapper.readValue(jsonResponseString,new TypeReference<List<Object>>(){});
			for (HashMap<String, Object> course : courseList) {
				Course aCourse=new Course();
				aCourse.setAccountId((Integer)course.get("account_id"));
				aCourse.setCourseCode((String)course.get("course_code"));
				aCourse.setCourseName((String)course.get("name"));
				aCourse.setCourseId((Integer)course.get("id"));
				aCourse.setCourseUrl((String)canvasURL+"/courses/"+course.get("id"));
				HashMap<String, String> instructorsForCourses = getInstructorsForCourses(aCourse.getCourseId());
				aCourse.setInstructors(instructorsForCourses);
				if(!(aCourse.getAccountId()==1)) {
					getSubaccountInfoForCourse(cfs, aCourse);
				}else {
					aCourse.setAccountName(ROOT);
					aCourse.setParentAccountName(ROOT);
					aCourse.setParentAccountId(1);
					M_log.debug("The Course belongs to Root account");
				}
				cfs.addCourse(aCourse);
			}
			String nextPageLink = getNextPageLink(httpResponse);
			if(nextPageLink!=null) {
				getThePublishedCourseList(nextPageLink,cfs);
			}

		} catch (ParseException e) {
			M_log.error("Parse exception occured getThePublishedCourseList( ) for URL:"+url,e);
		} catch (IOException e) {
			M_log.error("IOException occured getThePublishedCourseList( )for URL:" +url,e);
		}
	}

	private void getSubaccountInfoForCourse(CoursesForSubaccounts cfs, Course aCourse) {
		ArrayList<SubAccount> subAccounts = cfs.getSubAccount();
		for (SubAccount subAccount : subAccounts) {
			if(subAccount.getSubAccountId()==aCourse.getAccountId()) {
				aCourse.setAccountName(subAccount.getSubAccountName());
				aCourse.setParentAccountId(subAccount.getParentAccountId());
				getParentSubaccountNameForCourse(aCourse, subAccounts, subAccount);
				break;
			}
		}
	}

	private void getParentSubaccountNameForCourse(Course aCourse, ArrayList<SubAccount> subAccounts,SubAccount subAccount) {
		if(subAccount.isParentRoot()) {
			aCourse.setParentAccountName(ROOT);
		}else {
			for (SubAccount sAcct : subAccounts) {
				if(sAcct.getSubAccountId()==aCourse.getParentAccountId()) {
					aCourse.setParentAccountName(sAcct.getSubAccountName());
					break;
				}
			}

		}
	}

	private HashMap<String, String> getInstructorsForCourses(int courseId) {
		M_log.debug("getInstructorsForCourses: Called");
		String EnrollmentUrl= canvasURL+"/api/v1/courses/"+courseId+"/enrollments?per_page=100&type=TeacherEnrollment";
		HttpResponse httpResponse = doApiCall(EnrollmentUrl);
		ObjectMapper mapper = new ObjectMapper();
		HttpEntity entity = httpResponse.getEntity();
		List<HashMap<String, Object>> instructorEnrollmentList=new ArrayList<HashMap<String, Object>>();
		HashMap<String, String> instructors = new HashMap<String, String>();
		try {
			String jsonResponseString = EntityUtils.toString(entity);
			instructorEnrollmentList=mapper.readValue(jsonResponseString,new TypeReference<List<Object>>(){});
			if(!instructorEnrollmentList.isEmpty()) {
			for (HashMap<String, Object> enrollments : instructorEnrollmentList) {
				HashMap<String, Object> user=(HashMap<String, Object>) enrollments.get("user");
				instructors.put((String)user.get("sis_login_id")+EMAIL_SUFFIX, (String) user.get("name"));
			}
			}else {
				instructors.put(NONE, NONE);
			}
		} catch (ParseException e) {
			M_log.error("Parse exception occured getInstructorsForCourses( ) for courseId:"+courseId,e);
		} catch (IOException e) {
			M_log.error("IOException occured getInstructorsForCourses( )for courseId:" +courseId,e);
		}
		return instructors;
		
	}
     /*
      * The Subaccount call fetches the all the subaccounts that belongs to root, the api call only returns 100 records(that's how much canvas supports)
      * to to fetch more records we need look in the response header for the pagination Link to get more record
      */
	private void getSubAccountInfo(String url, CoursesForSubaccounts cfs) {
		M_log.debug("getSubAccountInfo: Called");
		HttpResponse httpResponse = doApiCall(url);
		ObjectMapper mapper = new ObjectMapper();
		HttpEntity entity = httpResponse.getEntity();
		List<HashMap<String, Object>> subAccounts=new ArrayList<HashMap<String, Object>>();
		try {
			String jsonResponseString = EntityUtils.toString(entity);
			subAccounts=mapper.readValue(jsonResponseString,new TypeReference<List<Object>>(){});
			for (HashMap<String, Object> subAccount : subAccounts) {
				SubAccount aSubAccount = new SubAccount();
				aSubAccount.setSubAccountId((Integer)subAccount.get("id"));
				aSubAccount.setSubAccountName((String)subAccount.get("name"));
				aSubAccount.setParentAccountId((Integer)subAccount.get("parent_account_id"));
				aSubAccount.setRootAccountId((Integer)subAccount.get("root_account_id"));
				cfs.addSubAccount(aSubAccount);
			}
			String nextPageLink = getNextPageLink(httpResponse);
			 if(nextPageLink!=null) {
				 getSubAccountInfo(nextPageLink,cfs);
			 }
		} catch (ParseException e) {
			M_log.error("Parse exception occured getSubAccountInfo( ) for URL:"+url,e);
		} catch (IOException e) {
			M_log.error("IOException occured getSubAccountInfo( )for URL:" +url,e);
		}

	}

	private void enrollmentTermsLogic(HttpServletResponse response)  {
		BufferedReader rd = null;
		PrintWriter out = null;
		String url=canvasURL+"/api/v1/accounts/1/terms?per_page=100";
		HttpResponse httpResponse = doApiCall(url);
		try {
			rd=new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
			String line = "";
			StringBuilder sb = new StringBuilder();
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}
			out = response.getWriter();
			out.print(sb.toString());
			out.flush();
		} catch (IllegalStateException e) {
			M_log.error("Parse exception occured enrollmentTermsLogic( ) ",e);
		} catch (IOException e) {
			M_log.error("IOException occured enrollmentTermsLogic( ) ",e);
		}
		
	}
    
    /* The pagination 'Link' sample from the header 
     * Link → <https://umich.test.instructure.com/api/v1/courses/26164/enrollments?type=TeacherEnrollment&page=1&per_page=100>; rel="current",
     * <https://umich.test.instructure.com/api/v1/courses/26164/enrollments?type=TeacherEnrollment&page=1&per_page=100>; rel="next",
     * <https://umich.test.instructure.com/api/v1/courses/26164/enrollments?type=TeacherEnrollment&page=1&per_page=100>; rel="first",
     * <https://umich.test.instructure.com/api/v1/courses/26164/enrollments?type=TeacherEnrollment&page=1&per_page=100>; rel="last"
     */
	protected String getNextPageLink(HttpResponse response) {
		M_log.debug("getNextPageLink: Called");
		String result = null;
		if (!response.containsHeader("Link")) {
			return result;
		}
			Header[] linkHeaders = response.getHeaders("Link");
			Header linkHeader = linkHeaders[0];
			M_log.debug("Http response contains the following Link headers: " + linkHeader.getValue());
			// look for the 'rel='next'' header value
			String[] links = linkHeader.getValue().split(",");
			for (int i = 0; i < links.length; i++) {
				String[] linkPart = links[i].split(";");
				if (linkPart[1].indexOf("rel=\"next\"") > 0) {
					result = linkPart[0].trim();
					break;
				}
			}
			if (result != null) {
				if (result.startsWith("<")) {
					result = result.substring(1, result.length() - 1);
				}
			}
		M_log.debug("Returning next page header as: " + (result != null ? result : "NONE"));
		return result;
	}
}
