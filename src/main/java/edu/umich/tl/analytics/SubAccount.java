package edu.umich.tl.analytics;

public class SubAccount {
	private int subAccountId;
	private String subAccountName;
	private int parentAccountId;
	private int rootAccountId;
	
	public int getSubAccountId() {
		return subAccountId;
	}
	public void setSubAccountId(int subAccountId) {
		this.subAccountId = subAccountId;
	}
	public String getSubAccountName() {
		return subAccountName;
	}
	public void setSubAccountName(String subAccountName) {
		this.subAccountName = subAccountName;
	}
	public int getParentAccountId() {
		return parentAccountId;
	}
	public void setParentAccountId(int parentAccountId) {
		this.parentAccountId = parentAccountId;
	}
	public int getRootAccountId() {
		return rootAccountId;
	}
	public void setRootAccountId(int rootAccountId) {
		this.rootAccountId = rootAccountId;
	}
	public boolean isParentRoot() {
		if(parentAccountId==rootAccountId) {
			return true;
		}
		return false;
		
	}

}
