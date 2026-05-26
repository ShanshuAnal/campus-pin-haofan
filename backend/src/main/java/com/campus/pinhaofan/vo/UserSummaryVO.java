package com.campus.pinhaofan.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryVO {

    private Long id;

    private String username;

    private String nickname;

    private String phone;

    private String status;
}
