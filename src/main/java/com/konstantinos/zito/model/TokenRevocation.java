package com.konstantinos.zito.model;

import java.util.Date;
import lombok.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Value
@Document("token_revocations")
public class TokenRevocation {
    @Id String id;
    @Indexed(expireAfter = "0s") Date expiresAt;
}
