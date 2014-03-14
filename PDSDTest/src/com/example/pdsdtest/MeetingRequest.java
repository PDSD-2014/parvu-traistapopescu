package com.example.pdsdtest;

public class MeetingRequest extends DataRequest {
	public String users;
	
	public MeetingRequest(String fbid, String users) {
		super(DataRequest.MEETING, fbid);
		
		this.users = users;
	}
}
