package edu.umich.tl.analytics;

import java.util.ArrayList;

public class CoursesForSubaccounts {
	
	public ArrayList<Course> course=new ArrayList<Course>();
	
	public ArrayList<SubAccount> subAccount=new ArrayList<SubAccount>();
	
	public boolean subaccountCallHasErr=false;
	public boolean courseCallHasErr=false;


	public ArrayList<Course> getCourse() {
		return course;
	}

	public void setCourse(ArrayList<Course> course) {
		this.course = course;
	}
	
	public void addCourse(Course course){
		this.course.add(course);
	}

	public ArrayList<SubAccount> getSubAccount() {
		return subAccount;
	}

	public void setSubAccount(ArrayList<SubAccount> subAccount) {
		this.subAccount = subAccount;
	}
	
	public void addSubAccount(SubAccount subAccount){
		this.subAccount.add(subAccount);
	}

	public boolean isSubaccountCallHasErr() {
		return subaccountCallHasErr;
	}

	public void setSubaccountCallHasErr(boolean subaccountCallHasErr) {
		this.subaccountCallHasErr = subaccountCallHasErr;
	}

	public boolean isGetCourseErrFlag() {
		return courseCallHasErr;
	}

	public void setCourseCallHasErr(boolean courseCallHasErr) {
		this.courseCallHasErr = courseCallHasErr;
	}

}
