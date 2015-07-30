package edu.umich.ctools.analytics;

import java.util.HashMap;

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
	

}
