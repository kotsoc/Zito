package com.konstantinos.zito.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document(collection = "TableLayout")
@Data
public class TableLayout {

    @Id
    private string id;

    @Indexed
    private String tableNumber;

    private int coordx;

    private int coordy;

    private int chairs;

}