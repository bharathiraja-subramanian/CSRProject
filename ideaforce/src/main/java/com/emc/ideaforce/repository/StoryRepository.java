package com.emc.ideaforce.repository;

import com.emc.ideaforce.model.Story;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, String> {

    List<Story> findByApprovedIsFalse();

    List<Story> findTop50ByApprovedIsTrueOrderByLastUpdatedDesc();

    @Query("SELECT s FROM Story s JOIN s.user u WHERE u.email = :email")
    List<Story> findByUserEquals(@Param("email") String userId);

    @Query("SELECT s FROM Story s JOIN s.user u WHERE u.email = :email AND s.approved=true")
    List<Story> findByUserEqualsAndApprovedIsTrue(@Param("email") String userId);

    @Query("SELECT u.email as userId, COUNT(u) as count "
            + "FROM Story s JOIN s.user u "
            + "WHERE s.approved=true "
            + "GROUP BY u.email "
            + "ORDER BY COUNT(u.email) DESC")
    List<ChallengerCountProjection> findUsersWithStoryCount(Pageable page);

    int countStoriesByApprovedIsTrue();

}
