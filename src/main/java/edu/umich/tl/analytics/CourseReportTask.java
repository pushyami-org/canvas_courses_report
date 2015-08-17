package edu.umich.tl.analytics;

public class CourseReportTask implements Runnable {
	private String term;
	private String termName;
	private CoursesForSubaccounts cfs;
	private CanvasCourseReportServlet canvasCourseReportServlet;
	
	

	public CourseReportTask(String term, String termName,CoursesForSubaccounts cfs, CanvasCourseReportServlet canvasCourseReportServlet) {
		this.term=term;
		this.termName=termName;
		this.cfs=cfs;
		this.canvasCourseReportServlet=canvasCourseReportServlet;
	}
	
	public String getTerm() {
		return term;
	}

	public void setTerm(String term) {
		this.term = term;
	}

	public String getTermName() {
		return termName;
	}

	public void setTermName(String termName) {
		this.termName = termName;
	}

	public CoursesForSubaccounts getCfs() {
		return cfs;
	}

	public void setCfs(CoursesForSubaccounts cfs) {
		this.cfs = cfs;
	}

	public void run() {
		this.canvasCourseReportServlet.getPublishedCourseThreadCall(term, termName,cfs);
	}

}
