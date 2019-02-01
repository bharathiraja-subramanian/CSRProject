package com.emc.ideaforce.controller;

import com.emc.ideaforce.model.ChallengeDetail;
import com.emc.ideaforce.model.Story;
import com.emc.ideaforce.model.StoryImage;
import com.emc.ideaforce.model.User;
import com.emc.ideaforce.repository.ChallengerCountProjection;
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
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
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
    private static final String PROFILE_VIEW = "profile";
    private static final String MESSAGE = "message";

    public static final String UNAPPROVED_CHALLENGES = "unapprovedchallenges";

    private final CommonService commonService;

    private final UserService userService;

    @GetMapping("/")
    public ModelAndView index(Principal principal) {
        ModelAndView mv = new ModelAndView(HOME_VIEW);

        int totalChallenges = 30;

        int approvedChallenges = commonService.getApprovedStories(principal.getName()).size();

        int goalStatus = (int) ((approvedChallenges * 100.0f) / totalChallenges);
        mv.addObject("goalStatus", goalStatus);

        int stepsTaken = approvedChallenges * 3000;
        mv.addObject("stepsTaken", stepsTaken);

        long participants = userService.getAllUsers();
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
            story.setUserId(principal.getName());
            story.setChallengeId(challengeId);
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

            return gallery();
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

            // get first image from every story/challenge taken
            List<String> images = latestChallenges.stream()
                    .map(Story::getImages)
                    .filter(image -> !isEmpty(image))
                    .map(image -> image.get(0).getData())
                    .map(image -> Base64.getEncoder().encodeToString(image))
                    .collect(Collectors.toList());

            mv.addObject("latestChallenges", images);
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
        List<ChallengerCountProjection> challengerDetails = commonService.getTopTenChallengers();
        mv.addObject("latestchallenges", latestChallengesUndertaken);
        mv.addObject("topchallengers", challengerDetails);
        return mv;
    }

    @GetMapping("/profile")
    public ModelAndView profile(Principal principal) {
        ModelAndView mv = new ModelAndView(PROFILE_VIEW);

        try {
            User user = userService.getUser(principal.getName());
            mv.addObject("details", user);

            List<Story> stories = commonService.getStories(principal.getName());
            mv.addObject("stories", stories);

            List<Story> unApprovedChallengeDetailList = commonService.findAllByApprovedIsFalse();
            mv.addObject(UNAPPROVED_CHALLENGES, unApprovedChallengeDetailList );
        }
        catch (Exception ex) {
            String errorMsg = "Failed to get profile details";
            LOG.error(errorMsg + " for user {}", principal.getName(), ex);

            mv.addObject(MESSAGE, errorMsg);
            mv.setStatus(INTERNAL_SERVER_ERROR);
        }

        return mv;
    }

    @PostMapping("/profile/set")
    public ModelAndView profile(Principal principal,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String employeeId,
            @RequestParam(required = false) MultipartFile image) {

        ModelAndView mv = new ModelAndView(PROFILE_VIEW);

        try {
            User user = userService.getUser(principal.getName());

            if (user == null) {
                String errorMessage = "User doesn't exist";
                if (LOG.isDebugEnabled()) {
                    LOG.debug(format("%s with ID %s", errorMessage, principal.getName()));
                }

                mv.addObject(MESSAGE, errorMessage);
            }
            else {
                user.setFirstName(firstName);
                user.setLastName(lastName);
                user.setEmployeeId(employeeId);

                if (!image.isEmpty()) {
                    user.setImage(image.getBytes());
                }

                userService.updateProfile(user);

                String successMessage = "Profile updated successfully";
                LOG.info(successMessage);
                mv.addObject(MESSAGE, successMessage);
            }
        }
        catch (Exception ex) {
            String errorMsg = "Failed to update profile";
            LOG.error(errorMsg, ex);

            mv.addObject(MESSAGE, errorMsg);
            mv.setStatus(INTERNAL_SERVER_ERROR);
        }

        return mv;
    }

}
