package com.megthink.gateway.controller;

import java.text.ParseException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import com.megthink.gateway.dao.PrivilegesDao;
import com.megthink.gateway.dao.SubscriberInfoQueryDetailDao;
import com.megthink.gateway.form.SuspensionForm;
import com.megthink.gateway.model.Privileges;
import com.megthink.gateway.model.User;
import com.megthink.gateway.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class SubscriberValidationController {

	@Autowired
	private UserService userService;
	@Autowired
	private PrivilegesDao privilegesDao;
	@Autowired
	private SubscriberInfoQueryDetailDao subscriberInfoQueryDetailDao;

	@RequestMapping(value = { "/subscriber.html" }, method = RequestMethod.GET)
	public ModelAndView getSubscriberDetail(HttpServletRequest request) throws ParseException {
		//String reqType = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
		ModelAndView modelAndView = new ModelAndView();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		List<Privileges> privilegesShownList = privilegesDao.getPrivilegesListByUser(user.getUserId());
		modelAndView.addObject("leftMenuList", privilegesShownList.isEmpty() ? null : privilegesShownList);
		modelAndView.addObject("listofreason", null);
		modelAndView.addObject("listofitem", subscriberInfoQueryDetailDao.getSubscriberInfoQueryDetail());
		modelAndView.addObject("filterForm", new SuspensionForm());
		modelAndView.setViewName("subscriber-validation");
		return modelAndView;
	}

	@RequestMapping(value = { "/subscriber.html" }, method = RequestMethod.POST)
	public ModelAndView getSubscriberDetail(@ModelAttribute SuspensionForm filterForm) throws ParseException {
		//String reqType = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
		ModelAndView modelAndView = new ModelAndView();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		List<Privileges> privilegesShownList = privilegesDao.getPrivilegesListByUser(user.getUserId());
		modelAndView.addObject("listofreason", null);
		modelAndView.addObject("leftMenuList", privilegesShownList.isEmpty() ? null : privilegesShownList);
		modelAndView.addObject("listofitem", subscriberInfoQueryDetailDao.getSubscriberInfoQueryDetail());
		modelAndView.addObject("filterForm", filterForm);
		modelAndView.setViewName("subscriber-validation");
		return modelAndView;
	}
}