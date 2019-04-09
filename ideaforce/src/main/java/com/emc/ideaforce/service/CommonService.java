package com.emc.ideaforce.service;

import com.emc.ideaforce.controller.CommentDto;
import com.emc.ideaforce.model.ChallengeCount;
import com.emc.ideaforce.model.ChallengeDetail;
import com.emc.ideaforce.model.Story;
import com.emc.ideaforce.model.StoryComments;
import com.emc.ideaforce.model.User;
import com.emc.ideaforce.repository.ChallengeDetailRepository;
import com.emc.ideaforce.repository.ChallengerCountProjection;
import com.emc.ideaforce.repository.StoryCommentRepository;
import com.emc.ideaforce.repository.StoryRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.util.Collections.sort;
import static java.util.Comparator.comparingInt;
import static org.springframework.util.CollectionUtils.isEmpty;

@Service
@RequiredArgsConstructor
public class CommonService {

    private final ChallengeDetailRepository challengeDetailRepository;

    private final StoryRepository storyRepository;

    private final StoryCommentRepository storyCommentRepository;

    private final UserService userService;

    /**
     * Returns the global Challenges list
     */
    public List<ChallengeDetail> getChallengeDetailList() {
        List<ChallengeDetail> challengeDetails = challengeDetailRepository.findAll();
        sort(challengeDetails, comparingInt(o -> Integer.parseInt(o.getId())));
        return challengeDetails;
    }

    /**
     * Return the challenge detail
     *
     * @param id unique identifier for the challenge
     */
    public ChallengeDetail getChallengeDetail(String id) {
        return challengeDetailRepository.getOne(id);
    }

    /**
     * Return the challenges taken by the user
     *
     * @param userId unique identifier for the user
     */
    public List<Story> getStories(String userId) {
        return storyRepository.findByUserEquals(userId);
    }

    /**
     * Submit story taken by the user
     *
     * @param story story taken by user
     */
    public void submitStory(Story story) {
        storyRepository.save(story);
    }

    /**
     * Get the latest/recent challenges taken by users
     */
    public List<Story> getLatestChallengesUndertaken() {
        return storyRepository.findTop20ByApprovedIsTrueOrderByLastUpdatedDesc();
    }

    public List<Story> getApprovedStories(String userId) {
        return storyRepository.findByUserEqualsAndApprovedIsTrue(userId);
    }

    public Page<Story> getUnapprovedStories(Pageable pageable) {
        return storyRepository.findByApprovedIsFalse(pageable);
    }

    public int getApprovedStoriesCount() {
        return storyRepository.countStoriesByApprovedIsTrue();
    }

    public void approveStory(String entryId) {
        Story storyObj = storyRepository.getOne(entryId);
        storyObj.setApproved(true);
        storyRepository.save(storyObj);
    }

    public Story getStoryById(String storyId) {
        return storyRepository.getOne(storyId);
    }

    public void saveStoryComment(CommentDto commentModel, User currentUser) {
        StoryComments storyCommentsEntity = new StoryComments();
        storyCommentsEntity.setStoryId(commentModel.getStoryId());
        storyCommentsEntity.setComment(commentModel.getComment());
        storyCommentsEntity.setUser(currentUser);
        storyCommentsEntity.setCreated(new Date(System.currentTimeMillis()));
        storyCommentsEntity.setLastUpdated(new Date(System.currentTimeMillis()));
        storyCommentRepository.save(storyCommentsEntity);
    }

    public List<StoryComments> getAllCommentsForStory(String id) {
        return storyCommentRepository.findAllByStoryIdEqualsOrderByCreatedDesc(id);
    }

    public List<ChallengeCount> getTopTenChallengers() {
        List<ChallengeCount> challengeCounts = new ArrayList<>();
        List<ChallengerCountProjection> challengers = storyRepository.findUsersWithStoryCount(PageRequest.of(0, 50));
        if (!isEmpty(challengers)) {
            for (ChallengerCountProjection challengerCountProjection : challengers) {
                challengeCounts.add(new ChallengeCount(userService.getUser(challengerCountProjection.getUserId()),
                        challengerCountProjection.getCount()));
            }
        }
        return challengeCounts;
    }
}
