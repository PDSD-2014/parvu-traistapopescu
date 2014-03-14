package com.example.pdsdtest;

public class MessageRequest extends DataRequest {
	public String to, msg;
	public MessageRequest(String fbid, String to, String msg) {
		super(DataRequest.MESSAGE, fbid);
		this.to = to;
		this.msg = msg;
	}
}