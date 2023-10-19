package server.hackathon.dto;


import lombok.Data;

import java.util.List;

@Data
public class SpaDto {
    private String name;
    private List<RelationDTO> relations;

    private List<ApiDTO> apis;





}
