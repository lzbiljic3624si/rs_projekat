package com.inzinjeri.rs_projekat.controllers.response;

import lombok.Data;

@Data
public class PrijavljenStudentResponse {
    private Long id;
    private StudentResponse student;
    private GrupaResponse grupa;
}
