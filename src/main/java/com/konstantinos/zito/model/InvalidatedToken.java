package com.konstantinos.zito.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Date;

@Document(collection = "invalidated_tokens")
public class InvalidatedToken {

    @Id
    private String id;

    // Tokens will automatically be removed from the database 24 hours after being invalidated.
    // This keeps the database collection from growing indefinitely.
    @Indexed(expireAfter = "86400s")
    private Date invalidatedAt = new Date();

    public InvalidatedToken() {
    }

    public InvalidatedToken(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getInvalidatedAt() {
        return invalidatedAt;
    }

    public void setInvalidatedAt(Date invalidatedAt) {
        this.invalidatedAt = invalidatedAt;
    }
}
