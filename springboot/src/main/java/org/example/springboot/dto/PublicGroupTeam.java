package org.example.springboot.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.example.springboot.entity.*;
import java.sql.Timestamp;
import java.util.List;

/** Public and member-facing responses never expose another member's order or account details. */
public record PublicGroupTeam(
    Long id, Long activityId, Long leaderUserId, Integer currentPeople, Integer requiredPeople,
    Integer status, String failReason,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") Timestamp expireTime,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") Timestamp successTime,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") Timestamp createdAt,
    GroupBuyActivity activity, Person leader, List<Member> members
) {
    public record Person(Long id, String username, String name) {
        static String displayName(User u) { return u.getName() == null || u.getName().isBlank() ? "书友" + u.getId() : u.getName(); }
        static Person from(User u) { return u == null ? null : new Person(u.getId(), displayName(u), displayName(u)); }
    }
    public record Member(Long id, Long userId, Integer isLeader, Integer status, @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8") Timestamp joinTime, Person user) {}
    public static PublicGroupTeam from(GroupBuyTeam t) {
        List<Member> members = t.getMembers() == null ? List.of() : t.getMembers().stream()
            .map(m -> new Member(m.getId(), m.getUserId(), m.getIsLeader(), m.getStatus(), m.getJoinTime(), Person.from(m.getUser()))).toList();
        return new PublicGroupTeam(t.getId(), t.getActivityId(), t.getLeaderUserId(), t.getCurrentPeople(),
            t.getRequiredPeople(), t.getStatus(), t.getFailReason(), t.getExpireTime(), t.getSuccessTime(),
            t.getCreatedAt(), t.getActivity(), Person.from(t.getLeader()), members);
    }
}
