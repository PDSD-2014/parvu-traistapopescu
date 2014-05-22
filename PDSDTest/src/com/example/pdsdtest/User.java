package com.example.pdsdtest;

public class User implements Comparable<User> {
  public String id, name, status;

  User(String id, String name) {
    this.id = id;
    this.name = name;
    this.status = "";
  }

  public String toString() {
    return name;
  }

  public int compareTo(User other) {
    return name.compareTo(other.name);
  }
}
