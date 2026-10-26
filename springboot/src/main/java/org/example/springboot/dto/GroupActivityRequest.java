package org.example.springboot.dto;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.example.springboot.entity.GroupBuyActivity;
import java.math.BigDecimal;
import java.sql.Timestamp;

/** Only editable activity fields are accepted; ownership and counters are server controlled. */
public record GroupActivityRequest(
    Long id,
    @NotNull @Min(1) Long productId,
    @NotBlank @Size(max = 100) String activityName,
    @Size(max = 5000) String activityDesc,
    @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal groupPrice,
    @NotNull @Min(2) @Max(100) Integer requiredMembers,
    @NotNull @Min(1) @Max(72) Integer validityHours,
    @NotNull @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") Timestamp startTime,
    @NotNull @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") Timestamp endTime
) {
    public GroupBuyActivity toEntity() {
        GroupBuyActivity a = new GroupBuyActivity();
        a.setId(id); a.setProductId(productId); a.setActivityName(activityName);
        a.setActivityDesc(activityDesc); a.setGroupPrice(groupPrice);
        a.setRequiredMembers(requiredMembers); a.setValidityHours(validityHours);
        a.setStartTime(startTime); a.setEndTime(endTime);
        return a;
    }
}
