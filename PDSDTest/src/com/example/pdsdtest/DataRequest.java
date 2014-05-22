package com.example.pdsdtest;

import java.io.Serializable;

public class DataRequest implements Serializable {
  public static final int CONNECT = 1, MESSAGE = 2, MEETING = 3;

  public int type;
  public String fbid;

  public DataRequest(int type, String fbid) {
    this.type = type;
    this.fbid = fbid;
  }
}
