package com.example.llmdatadiscovery.dto;

import lombok.Data;

@Data
public class DBConnectionInput {
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
}