package com.megthink.gateway.controller;

import java.text.ParseException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import com.megthink.gateway.dao.BillingResolutionDao;
import com.megthink.gateway.dao.PrivilegesDao;
import com.megthink.gateway.form.SuspensionForm;
import com.megthink.gateway.model.Privileges;
import com.megthink.gateway.model.User;
import com.megthink.gateway.service.BillingConstantService;
import com.megthink.gateway.service.UserService;
import com.megthink.gateway.utils.ReadConfigFile;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class SuspensionController {

	private static final Logger _logger = LoggerFactory.getLogger(SuspensionController.class);

	@Autowired
	private UserService userService;
	@Autowired
	private PrivilegesDao privilegesDao;
	@Autowired
	private BillingConstantService billingConstantService;
	@Autowired
	private BillingResolutionDao billingResolutionDao;

	@RequestMapping(value = { "/suspension-request.html" }, method = RequestMethod.GET)
	public ModelAndView getSuspensionRequestPage(HttpServletRequest request) throws ParseException {
		ModelAndView modelAndView = new ModelAndView();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		String sessionId = Long.toString(System.currentTimeMillis());
		_logger.info("[sessionId=" + sessionId
				+ "]: SuspensionController.getSuspensionProcess() - Trying to access the suspension request page using the username : ["
				+ user.getUsername() + "]");
		List<Privileges> privilegesShownList = privilegesDao.getPrivilegesListByUser(user.getUserId());
		modelAndView.addObject("leftMenuList", privilegesShownList.isEmpty() ? null : privilegesShownList);
		modelAndView.setViewName("suspension_request");
		return modelAndView;
	}

	@RequestMapping(value = { "/suspension-cancel.html" }, method = RequestMethod.GET)
	public ModelAndView getSuspensionCancelPage(HttpServletRequest request) throws ParseException {
		ModelAndView modelAndView = new ModelAndView();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		String sessionId = Long.toString(System.currentTimeMillis());
		_logger.info("[sessionId=" + sessionId
				+ "]: SuspensionController.getSuspensionProcess() - Trying to access the suspension cancel page using the username : ["
				+ user.getUsername() + "]");
		List<Privileges> privilegesShownList = privilegesDao.getPrivilegesListByUser(user.getUserId());
		String reqType = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
		modelAndView.addObject("leftMenuList", privilegesShownList.isEmpty() ? null : privilegesShownList);
		modelAndView.addObject("listofreason", billingConstantService.findByRequestType("SUS_DNO_CNL"));
		modelAndView.addObject("listofitem", billingResolutionDao.getBillingResolutionDnoCancel(7, reqType, "", ""));
		modelAndView.addObject("filterForm", new SuspensionForm());
		modelAndView.setViewName("suspension_cancel");
		return modelAndView;
	}

	@RequestMapping(value = { "/suspension-cancel.html" }, method = RequestMethod.POST)
	public ModelAndView getSuspensionDetailsByFilter(@ModelAttribute SuspensionForm filterForm) throws ParseException {
		ModelAndView modelAndView = new ModelAndView();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		List<Privileges> privilegesShownList = privilegesDao.getPrivilegesListByUser(user.getUserId());
		String reqType = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
		modelAndView.addObject("leftMenuList", privilegesShownList.isEmpty() ? null : privilegesShownList);
		modelAndView.addObject("listofreason", billingConstantService.findByRequestType("SUS_DNO_CNL"));
		modelAndView.addObject("listofitem", billingResolutionDao.getBillingResolutionDnoCancel(7, reqType,
				filterForm.getRequestId(), filterForm.getMsisdn()));
		modelAndView.addObject("filterForm", filterForm);
		modelAndView.setViewName("suspension_cancel");
		return modelAndView;
	}

	@RequestMapping(value = { "/suspension-acknowledgment.html" }, method = RequestMethod.GET)
	public ModelAndView getSuspensionDNOAck(HttpServletRequest request) throws ParseException {
		String reqType = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
		ModelAndView modelAndView = new ModelAndView();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		String sessionId = Long.toString(System.currentTimeMillis());
		_logger.info("[sessionId=" + sessionId
				+ "]: SuspensionController.getSuspensionProcess() - Trying to access the suspension acknowledgment page using the username : ["
				+ user.getUsername() + "]");
		List<Privileges> privilegesShownList = privilegesDao.getPrivilegesListByUser(user.getUserId());
		modelAndView.addObject("leftMenuList", privilegesShownList.isEmpty() ? null : privilegesShownList);
		modelAndView.addObject("listofreason", billingConstantService.findByRequestType("SUS_DNO_ACK"));
		modelAndView.addObject("listofitem", billingResolutionDao.getBillingResolutionDnoAck("2", reqType));
		modelAndView.addObject("filterForm", new SuspensionForm());
		modelAndView.setViewName("suspension_acknowledgment");
		return modelAndView;
	}

	@RequestMapping(value = { "/suspension-acknowledgment.html" }, method = RequestMethod.POST)
	public ModelAndView getSuspensionDNOAckByFilter(@ModelAttribute SuspensionForm filterForm) throws ParseException {
		String reqType = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
		ModelAndView modelAndView = new ModelAndView();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		List<Privileges> privilegesShownList = privilegesDao.getPrivilegesListByUser(user.getUserId());
		modelAndView.addObject("listofreason", billingConstantService.findByRequestType("SUS_DNO_ACK"));
		modelAndView.addObject("leftMenuList", privilegesShownList.isEmpty() ? null : privilegesShownList);
		modelAndView.addObject("listofitem", billingResolutionDao.getBillingResolutionDnoAck("2", reqType,
				filterForm.getRequestId(), filterForm.getMsisdn()));
		modelAndView.addObject("filterForm", filterForm);
		modelAndView.setViewName("suspension_acknowledgment");
		return modelAndView;
	}

	@RequestMapping(value = { "/suspension-reacknowledgment.html" }, method = RequestMethod.GET)
	public ModelAndView getReAcknowledgeSuspension(HttpServletRequest request) throws ParseException {
		String reqType = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
		ModelAndView modelAndView = new ModelAndView();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		String sessionId = Long.toString(System.currentTimeMillis());
		_logger.info("[sessionId=" + sessionId
				+ "]: SuspensionController.getSuspensionProcess() - Trying to access the suspension acknowledgment page using the username : ["
				+ user.getUsername() + "]");
		List<Privileges> privilegesShownList = privilegesDao.getPrivilegesListByUser(user.getUserId());
		modelAndView.addObject("leftMenuList", privilegesShownList.isEmpty() ? null : privilegesShownList);
		modelAndView.addObject("listofreason", billingConstantService.findByRequestType("SUS_DNO_ACK"));
		modelAndView.addObject("listofitem", billingResolutionDao.getBillingResolutionDnoAck("4", reqType));
		modelAndView.addObject("filterForm", new SuspensionForm());
		modelAndView.setViewName("suspension_reacknowledgment");
		return modelAndView;
	}

	@RequestMapping(value = { "/suspension-reacknowledgment.html" }, method = RequestMethod.POST)
	public ModelAndView getReAcknowledgeSuspension(@ModelAttribute SuspensionForm filterForm) throws ParseException {
		String reqType = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
		ModelAndView modelAndView = new ModelAndView();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		List<Privileges> privilegesShownList = privilegesDao.getPrivilegesListByUser(user.getUserId());
		modelAndView.addObject("listofreason", billingConstantService.findByRequestType("SUS_DNO_ACK"));
		modelAndView.addObject("leftMenuList", privilegesShownList.isEmpty() ? null : privilegesShownList);
		modelAndView.addObject("listofitem", billingResolutionDao.getBillingResolutionDnoAck("4", reqType,
				filterForm.getRequestId(), filterForm.getMsisdn()));
		modelAndView.addObject("filterForm", filterForm);
		modelAndView.setViewName("suspension_reacknowledgment");
		return modelAndView;
	}

	@RequestMapping(value = { "/barring-receipt-confirmation.html" }, method = RequestMethod.GET)
	public ModelAndView getBarringReceiptConfirmation(HttpServletRequest request) throws ParseException {
		ModelAndView modelAndView = new ModelAndView();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		String sessionId = Long.toString(System.currentTimeMillis());
		_logger.info("[sessionId=" + sessionId
				+ "]: SuspensionController.getSuspensionProcess() - Trying to access the suspension acknowledgment page using the username : ["
				+ user.getUsername() + "]");
		String reqType = ReadConfigFile.getProperties().getProperty("SUS_RECIPIENT_IN");
		List<Privileges> privilegesShownList = privilegesDao.getPrivilegesListByUser(user.getUserId());
		modelAndView.addObject("leftMenuList", privilegesShownList.isEmpty() ? null : privilegesShownList);
		modelAndView.addObject("listofreason", billingConstantService.findByRequestType("SUS_RNO_RECEIPT"));
		modelAndView.addObject("listofitem", billingResolutionDao.getBillingResolutionRnoPayment(1, reqType, "", ""));
		modelAndView.addObject("filterForm", new SuspensionForm());
		modelAndView.setViewName("barring-receipt-confirmation");
		return modelAndView;
	}

	@RequestMapping(value = { "/barring-receipt-confirmation.html" }, method = RequestMethod.POST)
	public ModelAndView getBarringReceiptConfirmationDetails(@ModelAttribute SuspensionForm filterForm)
			throws ParseException {
		ModelAndView modelAndView = new ModelAndView();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		List<Privileges> privilegesShownList = privilegesDao.getPrivilegesListByUser(user.getUserId());
		modelAndView.addObject("leftMenuList", privilegesShownList.isEmpty() ? null : privilegesShownList);
		modelAndView.addObject("listofreason", billingConstantService.findByRequestType("SUS_RNO_RECEIPT"));
		String reqType = ReadConfigFile.getProperties().getProperty("SUS_RECIPIENT_IN");
		modelAndView.addObject("listofitem", billingResolutionDao.getBillingResolutionRnoPayment(1, reqType,
				filterForm.getRequestId(), filterForm.getMsisdn()));
		modelAndView.addObject("filterForm", filterForm);
		modelAndView.setViewName("barring-receipt-confirmation");
		return modelAndView;
	}

	@RequestMapping(value = { "/barring-reconnection.html" }, method = RequestMethod.GET)
	public ModelAndView getBarringReconnection(HttpServletRequest request) throws ParseException {
		ModelAndView modelAndView = new ModelAndView();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		String sessionId = Long.toString(System.currentTimeMillis());
		_logger.info("[sessionId=" + sessionId
				+ "]: SuspensionController.getSuspensionProcess() - Trying to access the suspension acknowledgment page using the username : ["
				+ user.getUsername() + "]");
		String reqType = ReadConfigFile.getProperties().getProperty("SUS_RECIPIENT_IN");
		List<Privileges> privilegesShownList = privilegesDao.getPrivilegesListByUser(user.getUserId());
		modelAndView.addObject("leftMenuList", privilegesShownList.isEmpty() ? null : privilegesShownList);
		modelAndView.addObject("listofreason", billingConstantService.findByRequestType("SUS_RNO_RECEIPT"));
		modelAndView.addObject("listofitem", billingResolutionDao.getBillingResolutionReconnect(reqType, "", ""));
		modelAndView.addObject("filterForm", new SuspensionForm());
		modelAndView.setViewName("barring-reconnection");
		return modelAndView;
	}

	@RequestMapping(value = { "/barring-reconnection.html" }, method = RequestMethod.POST)
	public ModelAndView getBarringReconnection(@ModelAttribute SuspensionForm filterForm) throws ParseException {
		String reqType = ReadConfigFile.getProperties().getProperty("SUS_RECIPIENT_IN");
		ModelAndView modelAndView = new ModelAndView();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		List<Privileges> privilegesShownList = privilegesDao.getPrivilegesListByUser(user.getUserId());
		modelAndView.addObject("leftMenuList", privilegesShownList.isEmpty() ? null : privilegesShownList);
		modelAndView.addObject("listofreason", billingConstantService.findByRequestType("SUS_RNO_RECEIPT"));
		modelAndView.addObject("listofitem", billingResolutionDao.getBillingResolutionReconnect(reqType,
				filterForm.getRequestId(), filterForm.getMsisdn()));
		modelAndView.addObject("filterForm", filterForm);
		modelAndView.setViewName("barring-reconnection");
		return modelAndView;
	}

	@RequestMapping(value = { "/suspension-rno-status.html" }, method = RequestMethod.GET)
	public ModelAndView getSuspensionRNOStatus(HttpServletRequest request) throws ParseException {
		ModelAndView modelAndView = new ModelAndView();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		String reqType = ReadConfigFile.getProperties().getProperty("SUS_RECIPIENT_IN");
		List<Privileges> privilegesShownList = privilegesDao.getPrivilegesListByUser(user.getUserId());
		modelAndView.addObject("leftMenuList", privilegesShownList.isEmpty() ? null : privilegesShownList);
		modelAndView.addObject("listofitem", billingResolutionDao.getBillingResolutionStatus(reqType, "", ""));
		modelAndView.addObject("filterForm", new SuspensionForm());
		modelAndView.setViewName("barring_status");
		return modelAndView;
	}

	@RequestMapping(value = { "/suspension-rno-status.html" }, method = RequestMethod.POST)
	public ModelAndView getSuspensionRNOStatus(@ModelAttribute SuspensionForm filterForm) throws ParseException {
		String reqType = ReadConfigFile.getProperties().getProperty("SUS_RECIPIENT_IN");
		ModelAndView modelAndView = new ModelAndView();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		List<Privileges> privilegesShownList = privilegesDao.getPrivilegesListByUser(user.getUserId());
		modelAndView.addObject("leftMenuList", privilegesShownList.isEmpty() ? null : privilegesShownList);
		modelAndView.addObject("listofitem", billingResolutionDao.getBillingResolutionStatus(reqType,
				filterForm.getRequestId(), filterForm.getMsisdn()));
		modelAndView.addObject("filterForm", filterForm);
		modelAndView.setViewName("barring_status");
		return modelAndView;
	}

	@RequestMapping(value = { "/suspension-dno-status.html" }, method = RequestMethod.GET)
	public ModelAndView getSuspensionDNOStatus(HttpServletRequest request) throws ParseException {
		ModelAndView modelAndView = new ModelAndView();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		String reqType = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
		List<Privileges> privilegesShownList = privilegesDao.getPrivilegesListByUser(user.getUserId());
		modelAndView.addObject("leftMenuList", privilegesShownList.isEmpty() ? null : privilegesShownList);
		modelAndView.addObject("listofitem", billingResolutionDao.getBillingResolutionStatus(reqType, "", ""));
		modelAndView.addObject("filterForm", new SuspensionForm());
		modelAndView.setViewName("suspension_status");
		return modelAndView;
	}

	@RequestMapping(value = { "/suspension-dno-status.html" }, method = RequestMethod.POST)
	public ModelAndView getSuspensionDNOStatus(@ModelAttribute SuspensionForm filterForm) throws ParseException {
		String reqType = ReadConfigFile.getProperties().getProperty("SUS_DONOR_IN");
		ModelAndView modelAndView = new ModelAndView();
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = userService.findUserByUsername(auth.getName());
		List<Privileges> privilegesShownList = privilegesDao.getPrivilegesListByUser(user.getUserId());
		modelAndView.addObject("leftMenuList", privilegesShownList.isEmpty() ? null : privilegesShownList);
		modelAndView.addObject("listofitem", billingResolutionDao.getBillingResolutionStatus(reqType,
				filterForm.getRequestId(), filterForm.getMsisdn()));
		modelAndView.addObject("filterForm", filterForm);
		modelAndView.setViewName("suspension_status");
		return modelAndView;
	}

}