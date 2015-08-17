package edu.umich.tl.analytics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;





public class CanvasCourseReportServlet extends HttpServlet {
	
	private static final String THREAD_ID = "thrdId";
	private static final long serialVersionUID = 8770051932039446255L;
	private static Log M_log = LogFactory.getLog(CanvasCourseReportServlet.class);
	
	private static final String ACTION = "action";
	private static final String TERM = "term";
	private static final String TERM_NAME = "termName";
	private static final String ACTION_GET_COURSES = "getCoursesPublished";
	private static final String ACTION_GET_TERMS = "getEnrollmentTerms";
	private static final String EMAIL_SUFFIX="@umich.edu";
	private static final String ACTION_POLLING = "polling";
	private static final String WORKING = "working";
	private String canvasToken=CanvasCourseReportFilter.canvasToken;
	private String canvasURL=CanvasCourseReportFilter.canvasURL;
	ResourceBundle props = ResourceBundle.getBundle("coursereport");
	ConcurrentHashMap<Thread,CourseReportTask> courseReportReqThreads=new ConcurrentHashMap<Thread,CourseReportTask>();
	
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
		}else if(requestedAction.equals(ACTION_POLLING)) {
			checkIfACourseReportThreadIsDone(request,response);
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

	private void checkIfACourseReportThreadIsDone(HttpServletRequest request,HttpServletResponse response) {
		M_log.debug("Starting polling... number of threads: "+courseReportReqThreads.size());
		try {
			boolean isThreadExist=false;
			String thrdId = request.getParameter(THREAD_ID);
			long threadId = Long.parseLong(thrdId);
			KeySetView<Thread, CourseReportTask> courseReportThreads = courseReportReqThreads.keySet();
			for (Thread aCourseReportThread : courseReportThreads) {
				if(aCourseReportThread.getId()==threadId) {
					checkIfThreadAlive(response, aCourseReportThread);
					isThreadExist=true;
					break;
				}
			}
			if(!isThreadExist){
				OutputStream outputStream = response.getOutputStream();
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				M_log.error("Course report thread is not found: "+thrdId);
				outputStream.write(props.getString("report.generation.err.msg").getBytes());
				outputStream.flush();
				outputStream.close();
			}
		} catch (IOException e) {
			M_log.debug("IOException occoured in checkIfACourseReportThreadIsDone()"+e);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			try {
				response.getWriter().print(props.getString("report.generation.err.msg"));
			} catch (IOException e1) {
				M_log.error("IOException occurred while writing the response out",e1);
			}
		}
	}


	private void checkIfThreadAlive(HttpServletResponse response, Thread aCourseReportThread)
			throws IOException {
		if(aCourseReportThread.isAlive()) {
			whileActionThreadIsRunning(response);
		}else {
			afterActionThreadIsDone(response, aCourseReportThread);
		}
	}


	private void whileActionThreadIsRunning(HttpServletResponse response) throws IOException {
		M_log.debug("Still working on generating the report");
		OutputStream outputStream = response.getOutputStream();
		outputStream.write(WORKING.getBytes());
		outputStream.flush();
		outputStream.close();
	}


	private void afterActionThreadIsDone(HttpServletResponse response, Thread aCourseReportThread) throws IOException {
		OutputStream outputStream = response.getOutputStream();
		CourseReportTask courseReportTask = courseReportReqThreads.get(aCourseReportThread);
		CoursesForSubaccounts cfs = courseReportTask.getCfs();
		if (cfs.subaccountCallHasErr || cfs.courseCallHasErr) {
			courseReportReqThreads.remove(aCourseReportThread);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			M_log.error("An error occurred while getting data, so report couldn't be generated");
			outputStream.write(props.getString("report.generation.err.msg").getBytes());
			outputStream.flush();
			outputStream.close();
			M_log.debug("Ending polling... number of threads: " + courseReportReqThreads.size());
		} else {
			M_log.debug("Generating the report....");
			response.setContentType("text/csv");
			response.setHeader("Content-Disposition", "attachment;filename="+String.format(props.getString("filename.course.report"),courseReportTask.getTermName()));
			String outputResult = generateCSVFile(cfs).toString();
			courseReportReqThreads.remove(aCourseReportThread);
			outputStream.write(outputResult.getBytes());
			outputStream.flush();
			outputStream.close();
			M_log.debug("Ending polling... number of threads: " + courseReportReqThreads.size());
		}
	}
	private void getPublishedCourses(HttpServletRequest request,HttpServletResponse response) {
		M_log.debug("getPublishedCourses(): Called");
		String term = request.getParameter(TERM);
		String termName = request.getParameter(TERM_NAME);
		M_log.info("Course Report request For Term= " +termName);
		CoursesForSubaccounts cfs =new CoursesForSubaccounts();
		CourseReportTask courseReportTask=new CourseReportTask(term, termName,cfs,this);
		Thread courseReportThread=new Thread(courseReportTask);
		courseReportReqThreads.put(courseReportThread, courseReportTask);
		courseReportThread.start();
		PrintWriter out;
		try {
			out = response.getWriter();
			out.print(courseReportThread.getId());
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public CoursesForSubaccounts getPublishedCourseThreadCall(String term, String termName,CoursesForSubaccounts cfs) {
		String subAccountUrl = canvasURL + "/api/v1/accounts/1/sub_accounts?recursive=true&per_page=100";
		String url = canvasURL + "/api/v1/accounts/1/courses?enrollment_term_id=" + term+ "&published=true&per_page=100";

		long startTime = System.currentTimeMillis();
		getSubAccountInfo(subAccountUrl, cfs);
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		M_log.info(String.format("Api call to get all (SubAccounts) info took %ssec",TimeUnit.MILLISECONDS.toSeconds(elapsedTime)));

		if (!cfs.isSubaccountCallHasErr()) {
			startTime = System.currentTimeMillis();
			getThePublishedCourseList(url, cfs);
			stopTime = System.currentTimeMillis();
			elapsedTime = stopTime - startTime;
			M_log.info(String.format("Api call to get all (published Courses) for %s term took %ssec", termName, TimeUnit.MILLISECONDS.toSeconds(elapsedTime)));
		}
		return cfs;
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
		ObjectMapper mapper = new ObjectMapper();
		int statusCode = httpResponse.getStatusLine().getStatusCode();
		if(statusCode!=200) {
			cfs.setCourseCallHasErr(true);
			M_log.error(apiCallErrorHandler(httpResponse,url));
			return;
		}
		
		HttpEntity entity = httpResponse.getEntity();
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
				HashMap<String, String> instructorsForCourses = getInstructorsForCourses(aCourse.getCourseId(),cfs);
				aCourse.setInstructors(instructorsForCourses);
				if(!(aCourse.getAccountId()==1)) {
					getSubaccountInfoForCourse(cfs, aCourse);
				}else {
					aCourse.setAccountName(props.getString("value.root"));
					aCourse.setParentAccountName(props.getString("value.root"));
					aCourse.setParentAccountId(1);
					M_log.debug("The Course belongs to Root account");
				}
				cfs.addCourse(aCourse);
			}
			String nextPageLink = getNextPageLink(httpResponse);
			if(nextPageLink!=null) {
				getThePublishedCourseList(nextPageLink,cfs);
			}

		} catch (JsonParseException e1) {
			M_log.error("JsonParseException occured getThePublishedCourseList() : ",e1);
		} catch(JsonMappingException e1) {
			M_log.error("JsonMappingException occured getThePublishedCourseList() : ",e1);
		}  catch (IOException e) {
			M_log.error("IOException occured getThePublishedCourseList( )for URL:" +url,e);
		}
	}
	

	//{"errors":[{"message":"The specified resource does not exist."}],"error_report_id":479313}

	private String apiCallErrorHandler(HttpResponse httpResponse, String url) {
		ObjectMapper mapper = new ObjectMapper();
		HttpEntity entity = httpResponse.getEntity();
		StringBuilder errMsg = new StringBuilder();
		errMsg.append("Api call ["+url+"] has some errors they are: ");
		try {
			String jsonErrRes = EntityUtils.toString(entity);
			Map<String,Object> map = new HashMap<String,Object>();
			map = mapper.readValue(jsonErrRes,new TypeReference<HashMap<String,Object>>(){});
			ArrayList<HashMap<String, String>> error = (ArrayList<HashMap<String, String>>) map.get("errors");
			for (HashMap<String, String> hashMap : error) {
				errMsg.append((String)hashMap.get("message")+",");
			}
		} catch (JsonParseException e1) {
			M_log.error("JsonParseException occured errorHandler() : ",e1);
		} catch(JsonMappingException e1) {
			M_log.error("JsonMappingException occured errorHandler() : ",e1);
		} catch (IOException e1) {
			M_log.error("IOException occured errorHandler() : ",e1);
		}
		return errMsg.toString();
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
			aCourse.setParentAccountName(props.getString("value.root"));
		}else {
			for (SubAccount sAcct : subAccounts) {
				if(sAcct.getSubAccountId()==aCourse.getParentAccountId()) {
					aCourse.setParentAccountName(sAcct.getSubAccountName());
					break;
				}
			}

		}
	}

	private HashMap<String, String> getInstructorsForCourses(int courseId,CoursesForSubaccounts cfs) {
		M_log.debug("getInstructorsForCourses: Called");
		List<HashMap<String, Object>> instructorEnrollmentList=new ArrayList<HashMap<String, Object>>();
		HashMap<String, String> instructors = new HashMap<String, String>();
		String EnrollmentUrl= canvasURL+"/api/v1/courses/"+courseId+"/enrollments?per_page=100&type=TeacherEnrollment";
		HttpResponse httpResponse = doApiCall(EnrollmentUrl);
		int statusCode = httpResponse.getStatusLine().getStatusCode();
		if(statusCode!=200) {
			M_log.error(apiCallErrorHandler(httpResponse, EnrollmentUrl));
			instructors.put(props.getString("value.error.info"), props.getString("value.error.info"));
			return instructors;
		}
		ObjectMapper mapper = new ObjectMapper();
		HttpEntity entity = httpResponse.getEntity();
		try {
			String jsonResponseString = EntityUtils.toString(entity);
			instructorEnrollmentList=mapper.readValue(jsonResponseString,new TypeReference<List<Object>>(){});
			if(!instructorEnrollmentList.isEmpty()) {
			for (HashMap<String, Object> enrollments : instructorEnrollmentList) {
				HashMap<String, Object> user=(HashMap<String, Object>) enrollments.get("user");
				instructors.put((String)user.get("sis_login_id")+EMAIL_SUFFIX, (String) user.get("name"));
			}
			}else {
				instructors.put(props.getString("value.none"), props.getString("value.none"));
			}
		} catch (JsonParseException e1) {
			M_log.error("JsonParseException occurred getInstructorsForCourses() for courseId : "+courseId,e1);
		} catch(JsonMappingException e1) {
			M_log.error("JsonMappingException occurred getInstructorsForCourses() for courseId : "+courseId,e1);
		} catch (IOException e) {
			M_log.error("IOException occurred getInstructorsForCourses( ) for courseId:" +courseId,e);
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
		int statusCode = httpResponse.getStatusLine().getStatusCode();
		if(statusCode!=200) {
			cfs.setSubaccountCallHasErr(true);
			M_log.error(apiCallErrorHandler(httpResponse, url));
			return;
		}
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
		} catch (JsonParseException e1) {
			M_log.error("JsonParseException occured getSubAccountInfo() for URL: : "+url,e1);
		} catch(JsonMappingException e1) {
			M_log.error("JsonMappingException occured getSubAccountInfo() for URL: : "+url,e1);
		} catch (IOException e) {
			M_log.error("IOException occured getSubAccountInfo( )for URL:" +url,e);
		}

	}
	

	private void enrollmentTermsLogic(HttpServletResponse response)  {
		BufferedReader rd = null;
		PrintWriter out = null;
		try {
			out = response.getWriter();
			String url=canvasURL+"/api/v1/accounts/1/terms?per_page=100";
			HttpResponse httpResponse = doApiCall(url);
			int statusCode = httpResponse.getStatusLine().getStatusCode();
			response.setStatus(statusCode);
			if(statusCode!=200) {
				out.print(props.getString("terms.err.msg"));
				M_log.error(apiCallErrorHandler(httpResponse, url));
				out.flush();
				return;
			}
			long startTime = System.nanoTime();
			rd=new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
			long stopTime = System.nanoTime();
			long elapsedTime = stopTime - startTime;
			M_log.info(String.format("Api call to get[Enrollment terms] took %snano sec",elapsedTime));
			String line = "";
			StringBuilder sb = new StringBuilder();
			while ((line = rd.readLine()) != null) {
				sb.append(line);
			}
			out.print(sb.toString());
			out.flush();
		} catch (IllegalStateException e) {
			M_log.error("IllegalState Exception occured enrollmentTermsLogic( ) ",e);
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
