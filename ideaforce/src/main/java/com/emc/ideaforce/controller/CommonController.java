package com.emc.ideaforce.controller;

import com.emc.ideaforce.model.ChallengeCount;
import com.emc.ideaforce.model.ChallengeDetail;
import com.emc.ideaforce.model.Story;
import com.emc.ideaforce.model.StoryImage;
import com.emc.ideaforce.model.Event;
import com.emc.ideaforce.model.User;
import com.emc.ideaforce.service.CommonService;
import com.emc.ideaforce.service.UserService;
import com.emc.ideaforce.utils.CommonException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.security.Principal;
import java.text.DecimalFormat;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * Common web controller
 */
@Controller
@RequiredArgsConstructor
public class CommonController {

    private static final Logger LOG = LoggerFactory.getLogger(CommonController.class);

    private static final String CHALLENGES = "challenges";
    private static final String CHALLENGE_DETAIL = "challengedetail";
    private static final String CHALLENGE = "challenge";
    private static final String USER_CHALLENGES = "userchallenges";

    private static final String HOME_VIEW = "index";
    private static final String SUBMIT_STORY_VIEW = "submitstory";
    private static final String GALLERY_VIEW = "gallery";
    private static final String LEADER_BOARD_VIEW = "leaderboard";
    private static final String MESSAGE = "message";

    private final CommonService commonService;

    private final UserService userService;

    @GetMapping("/")
    public ModelAndView index() {
        ModelAndView mv = new ModelAndView(HOME_VIEW);

        long participants = userService.getAllUsers();

        int approvedStories = commonService.getApprovedStoriesCount();

        int totalSteps = 65000000;
        int stepsTaken = approvedStories * 17500;

        int goalStatus = (int) Math.ceil((stepsTaken * 100f) / totalSteps);
        goalStatus = goalStatus > 100 ? 100 : goalStatus;

        mv.addObject("goalStatus", goalStatus);
        mv.addObject("stepsTaken", stepsTaken);
        mv.addObject("participants", participants);

        return mv;
    }

    @GetMapping("/challenges")
    public ModelAndView challengesList() {
        List<ChallengeDetail> challengeDetailList = commonService.getChallengeDetailList();

        ModelAndView mv = new ModelAndView(CHALLENGES);
        mv.addObject(CHALLENGES, challengeDetailList);
        return mv;
    }

    @GetMapping("/challenges/{id}")
    public ModelAndView challengeDetail(@PathVariable String id) {
        ChallengeDetail challengeDetail = commonService.getChallengeDetail(id);

        ModelAndView mv = new ModelAndView(CHALLENGE_DETAIL);
        mv.addObject(CHALLENGE, challengeDetail);
        return mv;
    }

    @GetMapping("/stories")
    public ModelAndView stories(Principal principal) {
        List<Story> entries = commonService.getStories(principal.getName());

        ModelAndView mv = new ModelAndView(USER_CHALLENGES);
        mv.addObject(CHALLENGES, entries);
        return mv;
    }

    @GetMapping("/submitstory/{challengeId}")
    public ModelAndView submitStory(@PathVariable String challengeId) {
        ModelAndView mv = new ModelAndView(SUBMIT_STORY_VIEW);
        mv.addObject("challengeId", challengeId);
        return mv;
    }

    @PostMapping("/submit-story")
    public ModelAndView submitStory(Principal principal,
            @RequestParam String challengeId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam MultipartFile[] images,
            @RequestParam String video) {

        ModelAndView mv = new ModelAndView();

        try {
            if (images.length == 0 || Stream.of(images).anyMatch(MultipartFile::isEmpty)) {
                throw new CommonException("At least 1 image needs to be uploaded");
            }

            Story story = new Story();
            story.setUser(userService.getUser(principal.getName()));
            ChallengeDetail challengeDetail = commonService.getChallengeDetail(challengeId);
            if (challengeDetail == null) {
                throw new RuntimeException("Invalid challenge id");
            }
            story.setChallengeDetail(challengeDetail);
            story.setTitle(title);
            story.setDescription(description);
            story.setVideo(video);

            for (MultipartFile image : images) {
                StoryImage storyImage = new StoryImage();
                storyImage.setData(image.getBytes());
                story.addImage(storyImage);
            }

            commonService.submitStory(story);

            LOG.info("Challenge {} submitted successfully by user {}", challengeId, principal.getName());

            return challengesList();
        }
        catch (Exception ex) {
            String errorMsg = "Failed to submit story";
            LOG.error(errorMsg, ex);

            mv.setViewName(SUBMIT_STORY_VIEW);
            mv.setStatus(INTERNAL_SERVER_ERROR);
            mv.addObject(MESSAGE, errorMsg);
        }

        return mv;
    }

    @ResponseBody
    @GetMapping("/gallery")
    public ModelAndView gallery() {
        ModelAndView mv = new ModelAndView(GALLERY_VIEW);

        try {
            List<Story> latestChallenges = commonService.getLatestChallengesUndertaken();
             mv.addObject("stories", latestChallenges);
            // get first image from every story/challenge taken
            List<String> images = latestChallenges.stream()
                    .map(Story::getImages)
                    .filter(image -> !isEmpty(image))
                    .map(image -> image.get(0).getData())
                    .map(image -> Base64.getEncoder().encodeToString(image))
                    .collect(Collectors.toList());

            mv.addObject("latestChallenges", images);
            List<Event> events = commonService.getEventsList();
            mv.addObject("events", events);
        }
        catch (Exception ex) {
            String errorMsg = "Failed to get latest challenges undertaken";
            LOG.error(errorMsg, ex);

            mv.addObject(MESSAGE, errorMsg);
            mv.setStatus(INTERNAL_SERVER_ERROR);
        }

        return mv;
    }

    @GetMapping("/leaderboard")
    public ModelAndView getLeaderBoardView() {
        ModelAndView mv = new ModelAndView(LEADER_BOARD_VIEW);

        List<Story> latestChallengesUndertaken = commonService.getLatestChallengesUndertaken();
        mv.addObject("latestchallenges", latestChallengesUndertaken);

        List<ChallengeCount> challengerDetails = commonService.getTopTenChallengers();
        mv.addObject("topchallengers", challengerDetails);
        return mv;
    }

}
