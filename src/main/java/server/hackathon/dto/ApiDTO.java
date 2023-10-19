package server.hackathon.dto;

import lombok.Data;

@Data
public class ApiDTO {

    private String name;
    private String endpoint;
    private String method;

    private String  mock;

    private String body;

    //private String queryPar;

    //private String pathPar;
}
