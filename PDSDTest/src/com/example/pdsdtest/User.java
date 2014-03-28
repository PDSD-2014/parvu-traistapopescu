package com.example.pdsdtest;

public class User {
	public String id, name, status;
	
	User(String id, String name) {
		this.id = id;
		this.name = name;
		this.status = "";
	}
	
	public String toString() {
		return name;
	}
}