package com.konstantinos.zito.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document(collection = "tablelayouts")
@Data
@CompoundIndex(name = "uniqueCoordinate", def = "{'coordx': 1, 'coordy': 1}", unique = true)
public class TableLayout {

    @Id
    private String id;

    @Indexed
    private String tableNumber;

    private int coordx;

    private int coordy;

    private int chairs;

}