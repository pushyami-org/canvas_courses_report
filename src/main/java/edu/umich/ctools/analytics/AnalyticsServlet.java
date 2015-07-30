package edu.umich.ctools.analytics;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import au.com.bytecode.opencsv.CSVWriter;


public class AnalyticsServlet extends HttpServlet {
	
	private static final long serialVersionUID = 8770051932039446255L;
	private static Log M_log = LogFactory.getLog(AnalyticsServlet.class);
	private static final String canvasToken="";
	private static final String canvasUrl="https://umich.test.instructure.com";
	public static final String ACTION = "action";
	public static final String TERM = "term";
	public static final String ACTION_GET_COURSES = "getCoursesPublished";
	public static final String ACTION_GET_TERMS = "getEnrollmentTerms";
	
	protected void doGet(HttpServletRequest request,HttpServletResponse response){
		M_log.debug("doGet: Called");
		if(request.getParameter(ACTION).equals(ACTION_GET_COURSES)) {
			getPublishedCourses(request,response);
		}else if(request.getParameter(ACTION).equals(ACTION_GET_TERMS)) {
			enrollmentTermsLogic(response);
		}
		
		// Terms terms = mapper.readValue(responseString, Terms.class);
	}
	private void getPublishedCourses(HttpServletRequest request,HttpServletResponse response) {
		String term = request.getParameter(TERM);
		String subAccountUrl = "https://umich.test.instructure.com/api/v1/accounts/1/sub_accounts?recursive=true&per_page=100";
		String url = "https://umich.test.instructure.com/api/v1/accounts/1/courses?enrollment_term_id="+term+"&published=true&per_page=100";
		CoursesForSubaccounts cfs =new CoursesForSubaccounts();
		getSubAccountInfo(subAccountUrl,cfs);
		getThePublishedCourseList(url,cfs);
		response.setContentType("text/csv");
		response.setHeader("Content-Disposition","attachment;filename=publishedCourseByTerm.csv");
		try {
			OutputStream outputStream = response.getOutputStream();
			String outputResult = generateCSVFile(cfs).toString();
			outputStream.write(outputResult.getBytes());
			outputStream.flush();
			outputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	protected void doPost(HttpServletRequest request,HttpServletResponse response) {
		M_log.debug("doGet: Called");
	}

	private StringBuilder generateCSVFile(CoursesForSubaccounts cfs) {
		M_log.debug("generateCSVFile: Called");
		StringBuilder writer = new StringBuilder();
		ArrayList<Course> courses = cfs.getCourse();
		System.out.println("courseSize: "+courses.size());
		writer.append("COURSE_NAME");writer.append(',');
		writer.append("COURSE_ID");writer.append(',');
		writer.append("COURSE_URL");writer.append(',');
		writer.append("INSTRUCTOR_NAME");writer.append(',');
		writer.append("INSTRUCTOR_UNIQNAME");writer.append(',');
		writer.append("ACCOUNT_NAME");writer.append(',');
		writer.append("PARENT_ACCOUNT_NAME");
		writer.append('\n');
		int count=0;
		for (Course course : courses) {
			writer.append("\"" +course.getCourseName()+ "\"");writer.append(',');
			writer.append(Integer.toString(course.getCourseId()));writer.append(',');
			writer.append(course.getCourseUrl());writer.append(',');
			writer.append(StringUtils.join(course.getInstructors().values(),';'));writer.append(',');
			writer.append(StringUtils.join(course.getInstructors().keySet(),';'));writer.append(',');
			writer.append("\"" +course.getAccountName()+"\"" );writer.append(',');
			writer.append("\"" +course.getParentAccountName()+"\"" );writer.append(',');
			writer.append('\n');
			count++;
			System.out.println(count);
		}
		return writer;
		
	}
	
//	private FileWriter generateCSVFile(String sFileName,CoursesForSubaccounts cfs ) {
//		FileWriter writer = null;
//		try {
//			ArrayList<Course> courses = cfs.getCourse();
//			 writer = new FileWriter(sFileName);
//			writer.append("COURSE_NAME");
//			writer.append(',');
//			writer.append("COURSE_ID");
//			writer.append(',');
//			writer.append("COURSE_URL");
//			writer.append(',');
//			writer.append("INSTRUCTOR_NAME");
//			writer.append(',');
//			writer.append("INSTRUCTOR_UNIQNAME");
//			writer.append(',');
//			writer.append("ACCOUNT_NAME");
//			writer.append(',');
//			writer.append("PARENT_ACCOUNT_NAME");
//			writer.append('\n');
//			for (Course course : courses) {
//				writer.append("\"" +course.getCourseName()+ "\"");
//				writer.append(',');
//				writer.append(Integer.toString(course.getCourseId()));
//				writer.append(',');
//				writer.append(course.getCourseUrl());
//				writer.append(',');
//				writer.append(StringUtils.join(course.getInstructors().values(),';'));
//				writer.append(',');
//				writer.append(StringUtils.join(course.getInstructors().keySet(),';'));
//				writer.append(',');
//				writer.append("\"" +course.getAccountName()+"\"" );
//				writer.append(',');
//				writer.append("\"" +course.getParentAccountName()+"\"" );
//				writer.append(',');
//				writer.append('\n');
//			}
//			writer.flush();
//			writer.close();
////			CSVWriter writer;
////			writer = new CSVWriter(new OutputStreamWriter(
////					new FileOutputStream(sFileName), "UTF-8"),
////					',', '\"');
////			writer.writeNext(new String[] {"COURSE_NAME", "COURSE_ID","COURSE_URL","INSTRUCTOR_NAME","INSTRUCTOR_UNIQNAME","ACCOUNT_NAME","PARENT_ACCOUNT_NAME"});
////			for (Course course : courses) {
////			writer.writeNext(new String[] {course.getCourseName(),Integer.toString(course.getCourseId()),course.getCourseUrl(),
////					StringUtils.join(course.getInstructors().values(),';'),StringUtils.join(course.getInstructors().keySet(),';'),
////					course.getAccountName(),course.getParentAccountName()});
////			}
////			writer.close(); 
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return writer;
//	}

	

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
				 aCourse.setCourseUrl((String)canvasUrl+"/courses/"+course.get("id"));
				 HashMap<String, String> instructorsForCourses = getInstructorsForCourses(aCourse.getCourseId());
				 aCourse.setInstructors(instructorsForCourses);
				 ArrayList<SubAccount> subAccounts = cfs.getSubAccount();
				 for (SubAccount subAccount : subAccounts) {
					if(subAccount.getSubAccountId()==aCourse.getAccountId()) {
						aCourse.setAccountName(subAccount.getSubAccountName());
						aCourse.setParentAccountId(subAccount.getParentAccountId());
                         if(subAccount.isParentRoot()) {
                        	 aCourse.setParentAccountName("root");
                         }else {
						for (SubAccount sAcct : subAccounts) {
							if(sAcct.getSubAccountId()==aCourse.getParentAccountId()) {
								aCourse.setParentAccountName(sAcct.getSubAccountName());
								break;
							}
						}
                        	 
                         }
						break;
					}
				}
				 cfs.addCourse(aCourse);
			}
			 String nextPageLink = getNextPageLink(httpResponse);
			 if(nextPageLink!=null) {
				 getThePublishedCourseList(nextPageLink,cfs);
			 }
				 
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private HashMap<String, String> getInstructorsForCourses(int courseId) {
		M_log.debug("getInstructorsForCourses: Called");
		String EnrollmentUrl= "https://umich.test.instructure.com/api/v1/courses/"+courseId+"/enrollments?per_page=100&type=TeacherEnrollment";
		HttpResponse httpResponse = doApiCall(EnrollmentUrl);
		ObjectMapper mapper = new ObjectMapper();
		HttpEntity entity = httpResponse.getEntity();
		List<HashMap<String, Object>> instructorEnrollmentList=new ArrayList<HashMap<String, Object>>();
		HashMap<String, String> instructors = new HashMap<String, String>();
		try {
			String jsonResponseString = EntityUtils.toString(entity);
			instructorEnrollmentList=mapper.readValue(jsonResponseString,new TypeReference<List<Object>>(){});
			for (HashMap<String, Object> enrollments : instructorEnrollmentList) {
				HashMap<String, Object> user=(HashMap<String, Object>) enrollments.get("user");
				instructors.put((String)user.get("sis_login_id"), (String) user.get("name"));
			}
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return instructors;
		
	}

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
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void enrollmentTermsLogic(HttpServletResponse response)  {
		BufferedReader rd = null;
		PrintWriter out = null;
		String url="https://umich.test.instructure.com/api/v1/accounts/1/terms?per_page=100";
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
		} catch (IllegalStateException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
//		ObjectMapper mapper = new ObjectMapper();
//		Map<String,Object> map = new HashMap<String,Object>();
//		try {
//			map = mapper.readValue(responseString,new TypeReference<HashMap<String,Object>>(){});
//			ArrayList<HashMap<String, Object>> termsList = (ArrayList<HashMap<String, Object>>) map.get("enrollment_terms");
//			for (HashMap<String, Object> eachTerm : termsList) {
//				Term term =new Term();
//				term.setCanvasId((Integer) eachTerm.get("id"));
//				term.setSisTermId((String) eachTerm.get("sis_term_id"));
//				term.setTermName((String) eachTerm.get("name"));
//				System.out.println("canvas_id: "+term.getCanvasId()+" sis_term_id: "+term.getSisTermId() + " termName: "+term.getTermName());
//				cfs.addTerm(term);
//			}
//			ArrayList<Term> terms = cfs.getTerms();
//			for (Term term2 : terms) {
//				System.out.println(term2.termName);
//				
//			}
//		} catch (JsonParseException e) {
//			e.printStackTrace();
//		} catch (JsonMappingException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
//		return cfs;
	}
    

	protected String getNextPageLink(HttpResponse response) {
		M_log.debug("getNextPageLink: Called");
		String result = null;
		if (response.containsHeader("Link")) {
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
		}
		M_log.debug("Returning next page header as: " + (result != null ? result : "NONE"));
		return result;
	}
}
