package com.inzinjeri.rs_projekat.controllers.response;

import lombok.Data;

@Data
public class KomentarResponse {

    private Long id;
    private String komentar;
    private Long radId;
    private String putanjaFajla;
    private Integer brojLinije;
}
