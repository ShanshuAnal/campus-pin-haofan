package com.campus.pinhaofan.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateGroupOrderEventRequest {

    @NotBlank(message = "eventType cannot be blank")
    @Size(max = 32, message = "eventType length cannot exceed 32")
    private String eventType;

    @Size(max = 32, message = "eventLevel length cannot exceed 32")
    private String eventLevel;

    @JsonAlias("title")
    @NotBlank(message = "eventTitle cannot be blank")
    @Size(max = 100, message = "eventTitle length cannot exceed 100")
    private String eventTitle;

    @JsonAlias("content")
    @Size(max = 500, message = "eventContent length cannot exceed 500")
    private String eventContent;
}
