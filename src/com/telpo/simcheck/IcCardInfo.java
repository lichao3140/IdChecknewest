package com.telpo.simcheck;

public class IcCardInfo {
	
	public String atr;
	public String iccid;
	public String resp;
	
	public void setatr(String atr) {
		
		this.atr = atr;
	}
	
	public void seticcid(String iccid) {
		
		this.iccid = iccid;
	}
	
	public void setresp(String resp) {
		
		this.resp = resp;
	}
	
	public String getatr() {
		return atr;
	}
	
	public String geticcid(){
		return iccid;
	}
	
	public String getresp() {
		return resp;
	}
}
