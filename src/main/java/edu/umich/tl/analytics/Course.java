package edu.umich.tl.analytics;

import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

public class Course {
	private int accountId;
	private String courseCode;
	private String courseName;
	private int courseId;
	private String accountName;
	private int parentAccountId;
	private String parentAccountName;
	private String courseUrl;
	private HashMap<String,String> instructors;
	
	public int getAccountId() {
		return accountId;
	}
	public void setAccountId(int accountId) {
		this.accountId = accountId;
	}
	public String getCourseCode() {
		return courseCode;
	}
	public void setCourseCode(String courseCode) {
		this.courseCode = courseCode;
	}
	public String getCourseName() {
		return courseName;
	}
	public void setCourseName(String courseName) {
		this.courseName = courseName;
	}
	public int getCourseId() {
		return courseId;
	}
	public void setCourseId(int courseId) {
		this.courseId = courseId;
	}
	public String getAccountName() {
		return accountName;
	}
	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}
	public int getParentAccountId() {
		return parentAccountId;
	}
	public void setParentAccountId(int parentAccountId) {
		this.parentAccountId = parentAccountId;
	}
	public String getParentAccountName() {
		return parentAccountName;
	}
	public void setParentAccountName(String parentAccountName) {
		this.parentAccountName = parentAccountName;
	}
	public String getCourseUrl() {
		return courseUrl;
	}
	public void setCourseUrl(String courseUrl) {
		this.courseUrl = courseUrl;
	}
	public HashMap<String, String> getInstructors() {
		return instructors;
	}
	public void setInstructors(HashMap<String, String> instructors) {
		this.instructors = instructors;
	}
	
	public static String getCourseHeader() {
		StringBuilder courseHeader=new StringBuilder();
		courseHeader.append("COURSE_NAME");courseHeader.append(',');
		courseHeader.append("COURSE_ID");courseHeader.append(',');
		courseHeader.append("COURSE_URL");courseHeader.append(',');
		courseHeader.append("INSTRUCTOR_NAME");courseHeader.append(',');
		courseHeader.append("INSTRUCTOR_EMAIL");courseHeader.append(',');
		courseHeader.append("ACCOUNT_NAME");courseHeader.append(',');
		courseHeader.append("PARENT_ACCOUNT_NAME");courseHeader.append('\n');
		return courseHeader.toString();
	}
	public String getCourseValues() {
		StringBuilder courseValues=new StringBuilder();
		courseValues.append("\"" +getCourseName()+ "\"");courseValues.append(',');
		courseValues.append(Integer.toString(getCourseId()));courseValues.append(',');
		courseValues.append(getCourseUrl());courseValues.append(',');
		courseValues.append(StringUtils.join(getInstructors().values(),';'));courseValues.append(',');
		courseValues.append(StringUtils.join(getInstructors().keySet(),';'));courseValues.append(',');
		courseValues.append("\"" +getAccountName()+"\"" );courseValues.append(',');
		courseValues.append("\"" +getParentAccountName()+"\"" );courseValues.append(',');
		courseValues.append('\n');
		return courseValues.toString();
		
	}
	

}
