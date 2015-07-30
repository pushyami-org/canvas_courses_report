package edu.umich.ctools.analytics;

import java.util.ArrayList;

public class CoursesForSubaccounts {
	public ArrayList<Term> terms=new ArrayList<Term>();
	
	public ArrayList<Course> course=new ArrayList<Course>();
	
	public ArrayList<SubAccount> subAccount=new ArrayList<SubAccount>();

	public ArrayList<Term> getTerms() {
		return terms;
	}

	public void setTerms(ArrayList<Term> terms) {
		this.terms = terms;
	}
	
	public void addTerm(Term term){
		this.terms.add(term);
	}

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

}
