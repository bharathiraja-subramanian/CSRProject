package com.emc.ideaforce.controller;

import com.emc.ideaforce.model.Story;
import com.emc.ideaforce.model.StoryComments;
import com.emc.ideaforce.model.StoryImage;
import com.emc.ideaforce.model.User;
import com.emc.ideaforce.service.CommonService;
import com.emc.ideaforce.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.security.RolesAllowed;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static com.emc.ideaforce.utils.Utils.ADMIN_ROLE;

import java.util.stream.Collectors; 
import java.util.stream.Stream;
import java.util.stream.IntStream;

@Controller
@RequiredArgsConstructor
public class AdminController {

    public static final String ADMIN = "admin";
    public static final String UNAPPROVED_CHALLENGES = "unapprovedchallenges";
    public static final String APPROVE_CHALLENGE = "approved";
    public static final String ADD_COMMENTS_VIEW = "addcomments";
    public static final String VIEW_COMMENTS_VIEW = "viewcomments";
    private static final String VIEW_STORY_DETAILS_VIEW = "storydetails";

    private final CommonService commonService;

    @Autowired
    private final UserService userService;

    @RolesAllowed(ADMIN_ROLE)
    @GetMapping(value = "/admin/{pageid}")
    public ModelAndView showAdminPage(@PathVariable int pageid) {
        PageRequest pageable = PageRequest.of(pageid - 1, 1);
        Page<Story> unApprovedChallengeDetailList = commonService.getUnapprovedStories(pageable);
        ModelAndView mv = new ModelAndView(ADMIN);
        int totalPages = unApprovedChallengeDetailList.getTotalPages();
        if(totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1,totalPages).boxed().collect(Collectors.toList());
            mv.addObject("pageNumbers", pageNumbers);
        } 
        mv.addObject(UNAPPROVED_CHALLENGES, unApprovedChallengeDetailList);       
        return mv;
    }

    @RolesAllowed(ADMIN_ROLE)
    @GetMapping(value = "/approve/{id}")
    public String approveStory(@PathVariable String id) {
        commonService.approveStory(id);
        return APPROVE_CHALLENGE;
    }

    @GetMapping(value = "/addcomments/{id}")
    public ModelAndView commentsForStory(@PathVariable String id, ModelMap model) {
        CommentDto commentDto = new CommentDto();
        commentDto.setStoryId(id);
        model.addAttribute("newcomment", commentDto);
        return new ModelAndView(ADD_COMMENTS_VIEW, model);
    }


    @GetMapping(value = "/viewcomments/{id}")
    public ModelAndView viewCommentsForStory(@PathVariable String id) {
        List<StoryComments> storyComments = commonService.getAllCommentsForStory(id);
        ModelAndView mvObject = new ModelAndView(VIEW_COMMENTS_VIEW);
        mvObject.addObject("storyComments", storyComments);
        return mvObject;
    }

    @GetMapping(value = "viewdetails/{id}")
    public ModelAndView getStoryDetails(@PathVariable String id) {
        Story storyObj = commonService.getStoryById(id);
        List<StoryImage> images = storyObj.getImages();
        List<String> encodedImages = new ArrayList<>();
        for (StoryImage image : images) {
            encodedImages.add(Base64.getEncoder().encodeToString(image.getData()));
        }
        ModelAndView mvObject = new ModelAndView(VIEW_STORY_DETAILS_VIEW);
        mvObject.addObject("storyDetails", storyObj);
        mvObject.addObject("storyPics", encodedImages);
        return mvObject;
    }

}
