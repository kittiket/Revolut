package ev.demo.revolut.db.jdbc;

import java.math.BigDecimal;
import java.time.Instant;

class TestDbEntity {
    private String id;
    private String name;
    private BigDecimal amount;
    private Instant time;

    String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    BigDecimal getAmount() {
        return amount;
    }

    void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    Instant getTime() {
        return time;
    }

    void setTime(Instant time) {
        this.time = time;
    }
}
